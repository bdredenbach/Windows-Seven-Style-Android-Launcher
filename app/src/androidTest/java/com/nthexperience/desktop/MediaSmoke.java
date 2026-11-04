package com.nthexperience.desktop;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
import android.provider.DocumentsContract;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;

@UnstableApi
public final class MediaSmoke extends Instrumentation {
    private MainActivity home;
    private View root;
    @Override public void onCreate(Bundle arguments) { super.onCreate(arguments); start(); }
    @Override public void onStart() {
        Bundle result = new Bundle();
        Uri folder = null;
        int outcome = Activity.RESULT_CANCELED;
        List<MediaLibrary.Entry> files = new ArrayList<>();
        try {
            DesktopStore store = new DesktopStore(getTargetContext());
            store.prefs.edit().putInt("startup_mode", 2).putBoolean("welcome_on_start", false).commit();
            getTargetContext().getSharedPreferences(StorageAccess.PREFS, 0).edit().putBoolean("seen", true).commit();
            getTargetContext().getSharedPreferences(StartupExperience.SESSION, 0).edit()
                    .putBoolean("seen", true).putBoolean("welcome_seen", true)
                    .putBoolean(StartupExperience.HOME_OFFERED, true).commit();
            Uri downloads = StorageAccess.document("Download");
            folder = DocumentOps.createFolder(getTargetContext(), downloads, "Nth Media Test " + SystemClock.uptimeMillis(), new DocumentOps.Cancel());
            for (String name : Arrays.asList("Tone.mp3", "Scene.mp4", "Tone.m4a", "Tone.wav", "Tone.flac", "Scene.webm")) {
                String type = FileLaunch.mime(name, null);
                String display = name.equals("Tone.mp3") ? "01 Tone.mp3" : name;
                Uri uri = DocumentsContract.createDocument(getTargetContext().getContentResolver(), folder, type, display);
                try (InputStream in = getContext().getAssets().open("media/" + name);
                     OutputStream out = getTargetContext().getContentResolver().openOutputStream(uri)) {
                    byte[] buffer = new byte[8192];
                    for (int n; (n = in.read(buffer)) != -1;) out.write(buffer, 0, n);
                }
                MediaLibrary.Entry item = MediaLibrary.entry(uri, display, type);
                check(item != null, "Media type recognized: " + name);
                files.add(item);
            }
            home = (MainActivity) startActivitySync(new Intent(Intent.ACTION_MAIN)
                    .setComponent(new ComponentName(getTargetContext(), MainActivity.class))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            root = home.getWindow().getDecorView();
            await(home::hasWindowFocus, "Desktop gains focus");
            check(StartSearch.find(store, Collections.emptyList(), "nth media").size() >= 2, "Both media tools are searchable");
            String tree = StorageAccess.tree().toString();
            String folderId = DocumentsContract.getDocumentId(folder);
            String page = DocumentFolders.childPage(DocumentFolders.rootPage(tree, "Internal storage"),
                    new DocumentFolders.Item(folderId, "Nth Media", DocumentsContract.Document.MIME_TYPE_DIR, 0, 0));
            runOnMainSync(() -> home.open("@files"));
            await(() -> find(root, view -> view instanceof VistaExplorer) != null, "Explorer opens");
            runOnMainSync(() -> ((VistaExplorer)find(root, view -> view instanceof VistaExplorer)).navigate(page));
            await(() -> find(root, view -> "01 Tone.mp3".contentEquals(view.getContentDescription() == null ? "" : view.getContentDescription())) != null,
                    "Explorer displays the real MP3");
            runOnMainSync(() -> find(root, view -> "01 Tone.mp3".equals(view.getContentDescription())).performClick());
            await(() -> home.mediaPlayback().current() != null && home.mediaPlayback().player.getCurrentPosition() > 200,
                    "Explorer opens and decodes MP3 in the Player");
            MediaPlayback playback = home.mediaPlayback();
            await(() -> {
                for (float sample : playback.signal.samples()) if (Math.abs(sample) > .01f) return true;
                return false;
            }, "Visualizer receives real decoded audio");
            MediaPanel player = (MediaPanel)find(root, view -> view instanceof MediaPanel);
            await(() -> find(player, view -> view instanceof MediaTransportButton
                    && "Pause".equals(view.getContentDescription())) != null, "Playing glyph has an accessible Pause action");
            shot("nth-media-player.png");
            runOnMainSync(() -> {
                home.mediaLibrary().add(files);
                playback.player.pause();
                playback.player.seekTo(3100);
            });
            await(() -> playback.player.getCurrentPosition() >= 3000, "MP3 seeks");
            clickTab(player, "Library");
            await(() -> find(player, view -> "Media library".equals(view.getContentDescription())) != null, "Light Library opens independently");
            clickTab(player, "Now Playing");
            runOnMainSync(() -> home.open(MediaLibrary.CENTER));
            await(() -> find(root, view -> MediaLibrary.CENTER_TITLE.equals(view.getContentDescription())) != null,
                    "Media Center opens fullscreen");
            await(() -> !playback.player.getPlayWhenReady() && Math.abs(playback.player.getCurrentPosition() - 3100) < 300,
                    "Center keeps the exact playback session");
            shot("nth-media-center.png");
            View center = find(root, view -> MediaLibrary.CENTER_TITLE.equals(view.getContentDescription()));
            clickTab(center, "Start");
            await(() -> find(center, view -> "Music".equals(view.getContentDescription())) != null, "Media Center Start exposes its Music hub");
            shot("nth-media-center-start.png");
            runOnMainSync(() -> find(center, view -> "Music".equals(view.getContentDescription())).performClick());
            await(() -> find(center, view -> "Play 01 Tone.mp3".equals(view.getContentDescription())) != null,
                    "Music opens the shared library without changing playback");
            clickTab(center, "Now Playing");
            await(() -> Math.abs(playback.player.getCurrentPosition() - 3100) < 300, "Changing skins preserves playback position");

            AtomicInteger frames = new AtomicInteger();
            runOnMainSync(() -> playback.player.addListener(new Player.Listener() {
                @Override public void onRenderedFirstFrame() { frames.incrementAndGet(); }
            }));
            for (int index = 1; index < files.size(); index++) {
                MediaLibrary.Entry entry = files.get(index);
                int oldFrames = frames.get();
                runOnMainSync(() -> playback.play(Collections.singletonList(entry), 0));
                await(() -> playback.player.getPlaybackState() == Player.STATE_READY
                        && playback.player.getCurrentPosition() > 150
                        && (!entry.video() || frames.get() > oldFrames), "Decodes " + entry.name);
                runOnMainSync(() -> { playback.player.pause(); playback.player.seekTo(1600); });
                await(() -> playback.player.isCurrentMediaItemSeekable()
                        && Math.abs(playback.player.getCurrentPosition() - 1600) < 250, "Seeks " + entry.name);
                if (index == 1) shot("nth-media-video.png");
            }
            runOnMainSync(() -> home.onBackPressed());
            await(() -> find(root, view -> MediaLibrary.CENTER_TITLE.equals(view.getContentDescription())) == null
                    && playback.player.getCurrentPosition() > 1300, "Back returns to the same playback session");
            check(new MediaLibrary(store).items().size() == files.size(), "Library survives reload");
            result.putString("stream", "PASS: Explorer routes MP3 internally; real PCM drives the visualizer; "
                    + "MP3, H.264/AAC MP4, AAC M4A, PCM WAV, FLAC and VP8/Opus WebM play and seek; "
                    + "video renders; both views share position; library reloads.\n");
            outcome = Activity.RESULT_OK;
        } catch (Throwable failure) {
            try { shot("nth-media-failure.png"); } catch (Exception ignored) {}
            StringWriter details = new StringWriter();
            failure.printStackTrace(new PrintWriter(details));
            result.putString("stream", "FAIL: " + details);

        } finally {
            if (home != null) runOnMainSync(() -> {
                home.mediaPlayback().player.pause();
                for (MediaLibrary.Entry item : files) home.mediaLibrary().remove(item);
            });
            if (folder != null) try { DocumentsContract.deleteDocument(getTargetContext().getContentResolver(), folder); }
            catch (Exception ignored) {}
        }
        finish(outcome, result);
    }
    private void clickTab(View panel, String label) {
        runOnMainSync(() -> {
            View tab = find(panel, view -> view instanceof TextView && view.isClickable()
                    && label.contentEquals(((TextView)view).getText()));
            check(tab != null, "Media tab is available: " + label);
            tab.performClick();
        });
    }
    private void await(BooleanSupplier ready, String description) throws Exception {
        long deadline = SystemClock.uptimeMillis() + 12000;
        while (SystemClock.uptimeMillis() < deadline) {
            boolean[] value = {false};
            runOnMainSync(() -> value[0] = ready.getAsBoolean());
            if (value[0]) return;
            Thread.sleep(60);
        }
        throw new AssertionError(description);
    }
    private View find(View view, Predicate<View> predicate) {
        if (view == null || !view.isShown()) return null;
        if (predicate.test(view)) return view;
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup)view;
            for (int i = 0; i < group.getChildCount(); i++) {
                View result = find(group.getChildAt(i), predicate);
                if (result != null) return result;
            }
        }
        return null;
    }
    private void shot(String filename) throws Exception {
        Bitmap image = getUiAutomation().takeScreenshot();
        if (image == null) return;
        try (OutputStream out = getTargetContext().openFileOutput(filename, 0)) {
            image.compress(Bitmap.CompressFormat.PNG, 100, out);
        } finally { image.recycle(); }
    }
    private static void check(boolean valid, String message) {
        if (!valid) throw new AssertionError(message);
    }
}
