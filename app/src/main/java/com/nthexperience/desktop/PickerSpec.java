package com.nthexperience.desktop;

import android.content.Intent;
import java.util.*;

/** The narrow external contract: choose readable files, never replace Android's permission picker. */
final class PickerSpec {
    static final String ACTION=BuildConfig.APPLICATION_ID+".action.PICK_FILE";
    final boolean multiple;final List<String> types;
    PickerSpec(Intent intent){String action=intent.getAction();if(!Intent.ACTION_GET_CONTENT.equals(action)&&!ACTION.equals(action))throw new IllegalArgumentException("Unsupported file request");multiple=intent.getBooleanExtra(Intent.EXTRA_ALLOW_MULTIPLE,false);String main=intent.getType();if(main==null)main="*/*";types=new ArrayList<>();String[] extra=intent.getStringArrayExtra(Intent.EXTRA_MIME_TYPES);if(extra!=null&&extra.length>0){if(extra.length>32)throw new IllegalArgumentException("Too many file types");for(String type:extra)if(valid(type))types.add(type.toLowerCase(Locale.ROOT));}else if(valid(main))types.add(main.toLowerCase(Locale.ROOT));if(types.isEmpty())throw new IllegalArgumentException("No supported file types requested");}
    private static boolean valid(String type){return type!=null&&type.length()<=150&&type.matches("(?:[A-Za-z0-9!#$&^_.+\\-]+|\\*)/(?:[A-Za-z0-9!#$&^_.+\\-]+|\\*)");}
    boolean accepts(String type){if(type==null||type.equals(android.provider.DocumentsContract.Document.MIME_TYPE_DIR))return false;String mime=type.toLowerCase(Locale.ROOT);for(String wanted:types)if(wanted.equals("*/*")||wanted.equals(mime)||wanted.endsWith("/*")&&mime.startsWith(wanted.substring(0,wanted.length()-1)))return true;return false;}
}
