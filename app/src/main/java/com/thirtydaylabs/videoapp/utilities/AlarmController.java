package com.thirtydaylabs.videoapp.utilities;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.thirtydaylabs.videoapp.app.CollectionActivity;
import com.thirtydaylabs.videoapp.app.DailyVideoService;

import java.util.Calendar;


/**
 * Created by HooMan on 3/03/14.
 */
public class AlarmController {


    public static final int TIMER_1 = 1;
    public static void dailyVideoAlarm(Context context){

        long _alarmFinal;

        AlarmManager alarmMgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent alarmMainIntent = new Intent(context, DailyVideoService.class);
        PendingIntent pendingAlarmIntent = PendingIntent.getBroadcast(context, TIMER_1, alarmMainIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        // Cancel current alarm
        try {
            alarmMgr.cancel(pendingAlarmIntent);
//            Log.d(CollectionActivity.TAG, "Alarm is deleted");
        } catch (Exception e) {
            Log.e(CollectionActivity.TAG, "AlarmManager update was not canceled. " + e.toString());
        }

        //Set a new alarm
        Calendar alaramCalendar = Calendar.getInstance();
        Calendar nowCalendar = Calendar.getInstance();

        alaramCalendar.setTimeInMillis(System.currentTimeMillis());
        alaramCalendar.set(Calendar.HOUR_OF_DAY, 17);
        alaramCalendar.set(Calendar.MINUTE, 00);

        //Make sure alarm is set for the next day
        if(alaramCalendar.getTimeInMillis() <= nowCalendar.getTimeInMillis())
            _alarmFinal = alaramCalendar.getTimeInMillis() + (AlarmManager.INTERVAL_DAY+1);
        else
            _alarmFinal = alaramCalendar.getTimeInMillis();

        alarmMgr.setRepeating(AlarmManager.RTC_WAKEUP,
                _alarmFinal,
               24*60*60*1000, pendingAlarmIntent);
//            Log.i(CollectionActivity.TAG, "Alarm set");

    }


}
