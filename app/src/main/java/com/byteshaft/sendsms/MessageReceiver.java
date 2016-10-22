package com.byteshaft.sendsms;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.util.Log;

import com.byteshaft.sendsms.utils.AppGlobals;
import com.byteshaft.sendsms.utils.Helpers;

/**
 * Created by s9iper1 on 10/21/16.
 */

public class MessageReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i("TAG", "SMS RECEIVER");
        if(intent.getAction().equals("android.provider.Telephony.SMS_RECEIVED")){
            Bundle bundle = intent.getExtras();           //---get the SMS message passed in---
            SmsMessage[] msgs;
            String msg_from;
            if (bundle != null) {
                //---retrieve the SMS message received---
                try{
                    Object[] pdus = (Object[]) bundle.get("pdus");
                    msgs = new SmsMessage[pdus.length];
                    for(int i=0; i<msgs.length; i++) {
                        msgs[i] = SmsMessage.createFromPdu((byte[])pdus[i]);
                        msg_from = msgs[i].getOriginatingAddress();
                        String msgBody = msgs[i].getMessageBody();
                        Log.i("TAG", msg_from + " "+ msgBody);
                        if (SendSmsService.getInstance() != null && Helpers.getBooleanFromSp(
                                AppGlobals.KEY_SERVICE_STATE)) {
                            Helpers.appendLog(SendSmsService.getInstance().getCurrentLogDetails("")
                                    + " Received New Sms From "+ msg_from +" \""+ msgBody+ "\" \n");
                            SendSmsService.smsTobeUpload.put(msg_from, msgBody);
                            if (MainActivity.foreground) {
                                MainActivity.getInstance().loadLogs();
                            }
                        }
                    }
                    if (Helpers.isNetworkAvailable()) {
                        SendSmsService.runWhenMessageReceived();
                    }
                }catch(Exception e){
//                            Log.d("Exception caught",e.getMessage());
                }
            }
        }

    }
}
