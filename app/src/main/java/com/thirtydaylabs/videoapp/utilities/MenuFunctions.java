package com.thirtydaylabs.videoapp.utilities;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.widget.Toast;

import com.thirtydaylabs.snookertipsandtricks.R;
import com.thirtydaylabs.videoapp.app.AboutActivity;
import com.thirtydaylabs.videoapp.app.CollectionActivity;
import com.thirtydaylabs.videoapp.app.PushActivity;

import java.util.ArrayList;




/**
 * Created by HooMan on 5/12/13.
 */
public class MenuFunctions {


    public static boolean openShare(Context context){
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, "https://play.google.com/store/apps/details?id=" + context.getPackageName());
        context.startActivity(Intent.createChooser(shareIntent, "Share..."));
        return true;
    }


    public static boolean openVideoShare(Context context, String ID){
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, "http://youtu.be/" + ID);
        context.startActivity(Intent.createChooser(shareIntent, "Share..."));
        return true;
    }


    public static boolean openAbout(Context context){
        Intent aboutIntent = new Intent(context , AboutActivity.class);
        context.startActivity(aboutIntent);
        return true;
    }

    public static boolean openRate(Context context){
//        Intent intent = new Intent(context, WebViewActivity.class);
//        intent.putExtra("url", "https://play.google.com/store/apps/details?id=" + context.getResources().getString(R.string.package_name));
//        context.startActivity(intent);
//        return true;

        Intent intent = new Intent(Intent.ACTION_VIEW);
        //Try Google play
        intent.setData(Uri.parse("market://details?id="+context.getPackageName()));
        if (MyStartActivity(intent, context) == false) {
            //Market (Google play) app seems not installed, let's try to open a webbrowser
            intent.setData(Uri.parse("https://play.google.com/store/apps/details?id="+context.getPackageName()));
            if (MyStartActivity(intent, context) == false) {
                //Well if this also fails, we have run out of options, inform the user.
                Toast.makeText(context, "Could not open Android market, please install the market app.", Toast.LENGTH_SHORT).show();
            }
        }
        return true;
    }

    public static boolean openMore(Context context){

        Intent intent = new Intent(Intent.ACTION_VIEW);
        //Try Google play
        intent.setData(Uri.parse("market://developer?id="+context.getResources().getString(R.string.developer)));
        if (MyStartActivity(intent, context) == false) {
            //Market (Google play) app seems not installed, let's try to open a webbrowser
            intent.setData(Uri.parse("https://play.google.com/store/apps/developer?id="+context.getResources().getString(R.string.developer)));
            if (MyStartActivity(intent, context) == false) {
                //Well if this also fails, we have run out of options, inform the user.
                Toast.makeText(context, "Could not open Android market, please install the market app.", Toast.LENGTH_SHORT).show();
            }
        }
        return true;
    }

    private static boolean MyStartActivity(Intent aIntent, Context context) {
        try
        {
            context.startActivity(aIntent);
            return true;
        }
        catch (ActivityNotFoundException e)
        {
            return false;
        }
    }


    public static boolean openDailyVideo(Context context) {

        SharedPreferences prefs = context.getSharedPreferences(CollectionActivity.PREFS_NAME, Context.MODE_PRIVATE);
        String ID = prefs.getString("votd", "");


        if(ID.trim().length() > 0){
            // display youtube player
            Intent aboutIntent = new Intent(context , PushActivity.class);
            context.startActivity(aboutIntent);
        }else{
            AlertDialog alertDialog = new AlertDialog.Builder(context).create();
            alertDialog.setIcon(0);
            alertDialog.setTitle("Hold on...");
            alertDialog.setMessage(context.getString(R.string.today_pick_not_available));
            alertDialog.setButton("OK", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    // here you can add functions
                }
            });
            alertDialog.show();
        }

        return true;


    }



     public static boolean settings(Context context){

        final ArrayList mSelectedItems  = new ArrayList();  // Where we track the selected items
        boolean[] ticked = new boolean[10];
        final SharedPreferences prefs = context.getSharedPreferences(CollectionActivity.PREFS_NAME, Context.MODE_PRIVATE);
        final SharedPreferences.Editor editor = prefs.edit();
        //Check is daily video is turned off
        if(prefs.getBoolean("noDailyAlert", false)){ticked[0]=false;}else{ticked[0]=true;}
        //Create the upgrade dialog
        new AlertDialog.Builder(context)
                .setTitle(context.getString(R.string.settings))
                .setMultiChoiceItems(R.array.settings, ticked,
                        new DialogInterface.OnMultiChoiceClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which,
                                                boolean isChecked) {
                                if (isChecked) {
                                    // If the user checked the item, add it to the selected items
                                    mSelectedItems.add(which);

                                } else if (mSelectedItems.contains(which)) {
                                    // Else, if the item is already in the array, remove it
                                    mSelectedItems.remove(Integer.valueOf(which));
                                }
                            }
                        })
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // continue with upgrade
                        if(mSelectedItems.contains(0)){
                            editor.putBoolean("noDailyAlert", false);
                        }else{
                            editor.putBoolean("noDailyAlert", true);
                        }
                        editor.commit();
                    }
                })
                .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // do nothing
                    }
                })
                .setIcon(R.drawable.ic_action_dark_important)
                .show();


        return true;

    }



}
