package com.byteshaft.sendsms.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.byteshaft.sendsms.SendSmsService;
import com.byteshaft.sendsms.utils.AppGlobals;
import com.byteshaft.sendsms.utils.Helpers;

/**
 * Created by s9iper1 on 10/23/16.
 */

public class AlarmBroadcastReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(AppGlobals.getLOGTAG(getClass()), "Alarm Receiver");
        if (Helpers.getBooleanFromSp(AppGlobals.KEY_SERVICE_STATE)) {
            if (SendSmsService.getInstance() != null) {
                SendSmsService.getInstance().getSmsAndSend();
            }
        }

    }
}
