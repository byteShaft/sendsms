package com.byteshaft.sendsms.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.byteshaft.sendsms.utils.AlarmHelpers;
import com.byteshaft.sendsms.utils.AppGlobals;
import com.byteshaft.sendsms.utils.Helpers;


public class StandardAlarm extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(AppGlobals.getLOGTAG(getClass()), "Alarm Received");
        Helpers.saveFileName(Helpers.getCurrentDateAndTime());
        AlarmHelpers.setAlarmForNewDay(context);

    }
}
