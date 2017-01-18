package com.byteshaft.sendsms.utils;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.util.Log;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * Created by s9iper1 on 10/23/16.
 */

public class AlarmHelpers  {

    private static AlarmManager mAlarmManager;
    private static PendingIntent mPendingIntent;
    private  static PendingIntent mPIntent;

//    public static void setAlarm() {
//        mAlarmManager = getAlarmManager();
//        final int ONE_SECOND = 1000;
//        final int ONE_MINUTE = ONE_SECOND * 90;
//        setAlarm(ONE_MINUTE);
//    }
//
//    private static void setAlarm(long time) {
//        Log.i("Alarm Helpers",
//                String.format("Setting alarm for: %d", TimeUnit.MILLISECONDS.toMinutes(time)));
//        Intent intent = new Intent("com.byteshaft.alarm");
//        mPendingIntent = PendingIntent.getBroadcast(AppGlobals.getContext(), 0,
//                intent, PendingIntent.FLAG_UPDATE_CURRENT);
//        if(Build.VERSION.SDK_INT < 23){
//            if(Build.VERSION.SDK_INT >= 19){
//                mAlarmManager.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() +
//                        time, mPendingIntent);
//            }
//            else{
//                mAlarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() +
//                        time, mPendingIntent);
//            }
//        }
//        else{
//            mAlarmManager.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP,
//                    SystemClock.elapsedRealtime() + time, mPendingIntent);
//        }
//    }

    private static int getNextHour(int time) {
        if (time == 23) {
            return 1;
        } else {
            return time+1;
        }
    }

    public static void setAlarmForNewDay(Context context) {
        mAlarmManager = getAlarmManager();
        Intent intent = new Intent("com.byteShaft.night_alarm");
        mPIntent = PendingIntent.getBroadcast(context, 1, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        Calendar timeOff = Calendar.getInstance();
        Date currentLocalTime = timeOff.getTime();
        DateFormat date = new SimpleDateFormat("HH");
        int localTime = Integer.parseInt(date.format(currentLocalTime));
        System.out.print("time"+localTime);
        timeOff.set(Calendar.HOUR_OF_DAY, getNextHour(localTime));
        mAlarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP,
                timeOff.getTimeInMillis() , AlarmManager.INTERVAL_DAY, mPIntent);
        Log.i("TAG", " alarm time " + timeOff.getTimeInMillis());
        Log.i("TAG", " alarm time "+ SystemClock.elapsedRealtime() +  timeOff.getTimeInMillis());
    }

    private static int oneHour() {
        int ONE_SECOND = 1000;
        int ONE_MIN = ONE_SECOND * 60;
        return  ONE_MIN * 60;
    }

    private static AlarmManager getAlarmManager() {
        return (AlarmManager) AppGlobals.getContext().getSystemService(Context.ALARM_SERVICE);
    }

    void removePreviousAlarams() {
        try {
            if (mPendingIntent != null) {
                Log.i("NAMAZ_TIME", "removing namaz Alarm");
                mAlarmManager.cancel(mPendingIntent);
            }
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }
}
