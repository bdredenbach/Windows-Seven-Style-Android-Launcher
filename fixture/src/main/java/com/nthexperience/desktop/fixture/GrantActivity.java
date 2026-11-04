package com.nthexperience.desktop.fixture;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.provider.DocumentsContract;
import java.io.*;

public final class GrantActivity extends Activity {
    @Override public void onCreate(Bundle state){super.onCreate(state);try{if(getIntent().getBooleanExtra("revoke",false)){revokeUriPermission(DocumentsContract.buildTreeDocumentUri(FixtureProvider.AUTHORITY,"root:"),Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION);receipt();finish();return;}File root=new File(getFilesDir(),"root"),readonly=new File(getFilesDir(),"readonly");if(getIntent().getBooleanExtra("reset",false)){if(root.exists())FixtureProvider.remove(root);if(readonly.exists())FixtureProvider.remove(readonly);}root.mkdirs();readonly.mkdirs();new File(root,"Nested").mkdirs();new File(root,"Target").mkdirs();
        write(new File(root,"Source.txt"),"Nth file operations — café 🌲".getBytes(java.nio.charset.StandardCharsets.UTF_8));write(new File(root,"Unselected.txt"),"must stay private".getBytes());write(new File(root,"Chosen.txt"),"chosen by the user".getBytes());write(new File(root,"Other.txt"),"not selected".getBytes());write(new File(root,"ReadOnly.txt"),"read only".getBytes());write(new File(root,"Nested/Inside.txt"),"nested content".getBytes());write(new File(readonly,"Readonly.txt"),"readonly tree".getBytes());write(new File(root,"Broken.bin"),new byte[65536]);write(new File(root,"Slow.bin"),new byte[2*1024*1024]);
        int read=Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION|Intent.FLAG_GRANT_PREFIX_URI_PERMISSION;
        grantUriPermission(BuildConfig.LAUNCHER_APPLICATION_ID,DocumentsContract.buildTreeDocumentUri(FixtureProvider.AUTHORITY,"root:"),read|Intent.FLAG_GRANT_WRITE_URI_PERMISSION);grantUriPermission(BuildConfig.LAUNCHER_APPLICATION_ID,DocumentsContract.buildTreeDocumentUri(FixtureProvider.AUTHORITY,"readonly:"),read);
        receipt();
    }catch(Exception ex){android.util.Log.e("NthFixture","Fixture failed",ex);}finish();}
    private void receipt()throws IOException{String token=getIntent().getStringExtra("receipt");if(token!=null)write(new File(getFilesDir(),"grant-receipt.txt"),token.getBytes(java.nio.charset.StandardCharsets.UTF_8));}
    private static void write(File file,byte[] bytes)throws IOException{try(FileOutputStream out=new FileOutputStream(file)){out.write(bytes);}}
}
