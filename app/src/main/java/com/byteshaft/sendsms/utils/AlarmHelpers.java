package com.byteshaft.sendsms.utils;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.SystemClock;
import android.util.Log;

import java.util.concurrent.TimeUnit;

/**
 * Created by s9iper1 on 10/23/16.
 */

public class AlarmHelpers  {

    private static AlarmManager mAlarmManager;
    private static PendingIntent mPendingIntent;

    public static void setAlarm() {
        mAlarmManager = getAlarmManager();
        final int ONE_SECOND = 1000;
        final int ONE_MINUTE = ONE_SECOND * 60;
        setAlarm(ONE_MINUTE);
    }

    private static void setAlarm(long time) {
        Log.i("Alarm Helpers",
                String.format("Setting alarm for: %d", TimeUnit.MILLISECONDS.toMinutes(time)));
        Intent intent = new Intent("com.byteshaft.alarm");
        mPendingIntent = PendingIntent.getBroadcast(AppGlobals.getContext(), 0,
                intent, PendingIntent.FLAG_UPDATE_CURRENT);
        if(Build.VERSION.SDK_INT < 23){
            if(Build.VERSION.SDK_INT >= 19){
                mAlarmManager.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() +
                        time, mPendingIntent);
            }
            else{
                mAlarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() +
                        time, mPendingIntent);
            }
        }
        else{
            mAlarmManager.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    SystemClock.elapsedRealtime() + time, mPendingIntent);
        }
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
