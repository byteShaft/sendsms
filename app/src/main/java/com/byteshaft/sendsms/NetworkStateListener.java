package com.byteshaft.sendsms;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.byteshaft.sendsms.utils.AppGlobals;
import com.byteshaft.sendsms.utils.Helpers;

public class NetworkStateListener extends BroadcastReceiver {
    @Override
    public void onReceive(final Context context, Intent intent) {
        if (Helpers.getBooleanFromSp(AppGlobals.KEY_SERVICE_STATE)) {
            new android.os.Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (SendSmsService.getInstance() == null) {
                        context.startService(new Intent(context, SendSmsService.class));
                    }
                }
            }, 60000);
        }

    }

}
