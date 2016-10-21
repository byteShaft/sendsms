package com.byteshaft.sendsms;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.byteshaft.sendsms.utils.AppGlobals;
import com.byteshaft.sendsms.utils.Helpers;


public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Helpers.getBooleanFromSp(AppGlobals.KEY_SERVICE_STATE)) {
            context.startService(new Intent(context, SendSmsService.class));
        }
    }
}
