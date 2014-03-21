package com.thirtydaylabs.videoapp.utilities;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Vibrator;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.thirtydaylabs.myanmarmovies.R;
import com.thirtydaylabs.videoapp.app.PushActivity;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;



/**
 * Created by HooMan on 1/03/14.
 */
public class Notification {

    // Put the message into a notification and post it.
    // This is just one simple example of what you might choose to do with
    // a GCM message.
    public static void sendNotification(Context mContext, Map push) {

        final int NOTIFICATION_ID = 1;
        NotificationManager mNotificationManager;
        final String TAG = "Timer";

        //Dismiss the notifications
        NotificationManager manager = (NotificationManager) mContext.getSystemService(Service.NOTIFICATION_SERVICE);
        manager.cancel(1);

        String title = (String) push.get("title");
        String subtitle = (String) push.get("subtitle");
        String externalIcon = (String) push.get("externalIcon");

        // Get instance of Vibrator from current Context
        Vibrator v = (Vibrator) mContext.getSystemService(Context.VIBRATOR_SERVICE);

        // Vibrate for 400 milliseconds
        v.vibrate(400);

        try {
            Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            Ringtone r = RingtoneManager.getRingtone(mContext.getApplicationContext(), notification);
            r.play();
        } catch (Exception e) {
            e.printStackTrace();
        }


        mNotificationManager = (NotificationManager)
                mContext.getSystemService(Context.NOTIFICATION_SERVICE);

        PendingIntent contentIntent = PendingIntent.getActivity(mContext, 0,
                new Intent(mContext, PushActivity.class), 0);

        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(mContext)
                        .setSmallIcon(R.drawable.ic_notification_icon)
                        .setContentTitle(title)
                        .setContentText(subtitle);

        Log.i(TAG, "image Icon ");

        if(!externalIcon.equals("")){
            Bitmap bitmap = getBitmapFromURL(externalIcon);
            mBuilder.setLargeIcon(bitmap);
        }

        mBuilder.setContentIntent(contentIntent);
        mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());
    }

    public static Bitmap getBitmapFromURL(String strURL) {
        try {
            URL url = new URL(strURL);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoInput(true);
            connection.connect();
            InputStream input = connection.getInputStream();
            Bitmap myBitmap = BitmapFactory.decodeStream(input);
            return myBitmap;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

}
