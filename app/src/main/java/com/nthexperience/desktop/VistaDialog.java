package com.nthexperience.desktop;

import android.app.AlertDialog;
import android.content.*;
import android.database.DataSetObserver;
import android.graphics.Color;
import android.view.*;
import android.widget.*;

/** Shared Vista caption, command buttons and list surface; Android retains dialog semantics. */
final class VistaDialog extends AlertDialog.Builder {
    private final Context context;
    private CharSequence caption;
    private boolean cancelable=true,customTitle;
    VistaDialog(Context context){super(context);this.context=context;}
    @Override public AlertDialog.Builder setTitle(CharSequence value){caption=value;return super.setTitle(value);}
    @Override public AlertDialog.Builder setTitle(int value){return setTitle(context.getText(value));}
    @Override public AlertDialog.Builder setCancelable(boolean value){cancelable=value;return super.setCancelable(value);}
    @Override public AlertDialog.Builder setCustomTitle(View view){customTitle=true;return super.setCustomTitle(view);}
    @Override public AlertDialog create(){
        final AlertDialog[] owner=new AlertDialog[1];
        if(!customTitle&&caption!=null){
            LinearLayout bar=new LinearLayout(context);bar.setGravity(Gravity.CENTER_VERTICAL);bar.setPadding(Aero.dp(context,12),0,Aero.dp(context,5),0);bar.setContentDescription("Vista dialog caption");
            bar.setBackground(Aero.gloss(context,new int[]{0xffe5f3fa,0xffcfdfeb,0xffbdd2e1,0xffdceaf3},4,0xff92acbf));
            TextView title=Aero.text(context,caption.toString(),14,0xff142c40);Aero.bold(title);title.setMaxLines(2);title.setEllipsize(android.text.TextUtils.TruncateAt.END);bar.addView(title,new LinearLayout.LayoutParams(0,Aero.dp(context,48),1));
            if(cancelable){TextView close=new WindowButton(context,"×",()->{if(owner[0]!=null)owner[0].cancel();});close.setTextSize(22);close.setContentDescription("Close dialog");close.setMinHeight(0);close.setPadding(0,0,0,0);close.setTextColor(Color.WHITE);bar.addView(close,new LinearLayout.LayoutParams(Aero.dp(context,40),Aero.dp(context,32)));}
            super.setCustomTitle(bar);
        }
        AlertDialog dialog=super.create();owner[0]=dialog;
        dialog.setOnShowListener(ignored->{Aero.styleDialog(context,dialog);ListView list=dialog.getListView();if(list!=null&&!(list.getAdapter() instanceof Rows)){int checked=list.getCheckedItemPosition();list.setSelector(Aero.selection(context));list.setDivider(new android.graphics.drawable.ColorDrawable(0xffe3e8ec));list.setDividerHeight(Aero.dp(context,1));list.setBackgroundColor(0xfffafcfd);list.setAdapter(new Rows(context,list.getAdapter(),list.getChoiceMode()));if(checked>=0)list.setItemChecked(checked,true);}View message=dialog.findViewById(android.R.id.message);if(message instanceof TextView){((TextView)message).setTextSize(14);((TextView)message).setTextColor(Aero.INK);((TextView)message).setLineSpacing(Aero.dp(context,3),1);}});
        return dialog;
    }
    private static final class Rows extends BaseAdapter {
        final Context context;final ListAdapter original;final int mode;
        Rows(Context context,ListAdapter original,int mode){this.context=context;this.original=original;this.mode=mode;original.registerDataSetObserver(new DataSetObserver(){public void onChanged(){notifyDataSetChanged();}public void onInvalidated(){notifyDataSetInvalidated();}});}
        public int getCount(){return original.getCount();}public Object getItem(int p){return original.getItem(p);}public long getItemId(int p){return original.getItemId(p);}public boolean hasStableIds(){return original.hasStableIds();}public boolean areAllItemsEnabled(){return original.areAllItemsEnabled();}public boolean isEnabled(int p){return original.isEnabled(p);}public int getItemViewType(int p){return original.getItemViewType(p);}public int getViewTypeCount(){return original.getViewTypeCount();}
        public View getView(int position,View recycled,ViewGroup parent){View view=original.getView(position,recycled,parent);style(view);view.setMinimumHeight(Aero.dp(context,44));view.setPadding(Aero.dp(context,16),Aero.dp(context,10),Aero.dp(context,12),Aero.dp(context,10));return view;}
        private void style(View view){if(view instanceof TextView){TextView label=(TextView)view;label.setTypeface(Aero.face(context));label.setTextSize(14);label.setTextColor(Aero.INK);}if(view instanceof CheckedTextView)((CheckedTextView)view).setCheckMarkDrawable(new AeroCheck(context,mode==ListView.CHOICE_MODE_SINGLE));if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++)style(((ViewGroup)view).getChildAt(i));}
    }
}
