package com.byteshaft.sendsms;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.byteshaft.sendsms.utils.AppGlobals;
import com.byteshaft.sendsms.utils.Helpers;

public class NetworkStateListener extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i("TAG", "Called");
        if (Helpers.getBooleanFromSp(AppGlobals.KEY_SERVICE_STATE)) {
            SendSmsService.getInstance().getSmsAndSend();
        }

    }

}
