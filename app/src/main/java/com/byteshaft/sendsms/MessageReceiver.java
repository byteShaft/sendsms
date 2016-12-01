package com.byteshaft.sendsms;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.util.Log;

import com.byteshaft.sendsms.utils.AppGlobals;
import com.byteshaft.sendsms.utils.Helpers;

import java.util.ArrayList;
import java.util.Map;

import static com.byteshaft.sendsms.SendSmsService.smsTobeUpload;

/**
 * Created by s9iper1 on 10/21/16.
 */

public class MessageReceiver extends BroadcastReceiver {

    private String msg_from;
    private String msgBody;

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i("TAG", "SMS RECEIVER");
        if (SendSmsService.getInstance() == null) {
            context.startService(new Intent(context, SendSmsService.class));
        }
        if (intent.getAction().equals("android.provider.Telephony.SMS_RECEIVED")) {
            Bundle bundle = intent.getExtras();           //---get the SMS message passed in---
            SmsMessage[] msgs;
            if (bundle != null) {
                //---retrieve the SMS message received---
                try {
                    Object[] pdus = (Object[]) bundle.get("pdus");
                    msgs = new SmsMessage[pdus.length];
                    for (int i = 0; i < msgs.length; i++) {
                        msgs[i] = SmsMessage.createFromPdu((byte[]) pdus[i]);
                        msg_from = msgs[i].getOriginatingAddress();
                        String msgBody = msgs[i].getMessageBody();
                        Log.i("TAG", msg_from + " " + msgBody);
                    }
                    SmsMessage sms = msgs[0];
                    try {
                        if (msgs.length == 1 || sms.isReplace()) {
                            msgBody = sms.getDisplayMessageBody();
                        } else {
                            StringBuilder bodyText = new StringBuilder();
                            for (int i = 0; i < msgs.length; i++) {
                                bodyText.append(msgs[i].getMessageBody());
                            }
                            msgBody = bodyText.toString();
                        }
                    } catch (Exception e) {

                    }
//                        if (SendSmsService.getInstance() != null && Helpers.getBooleanFromSp(
//                                AppGlobals.KEY_SERVICE_STATE)) {
////                            Helpers.appendLog(SendSmsService.getInstance().getCurrentLogDetails("")
////                                    + " Received New Sms From " + msg_from + " \"" + msgBody + "\" \n");
//                            Log.i("TAG","Condition");
//                            if (SendSmsService.smsTobeUpload.containsKey(msg_from)) {
//                                Log.i("TAG"," if Condition");
//                                ArrayList<String> array = SendSmsService.smsTobeUpload.get(msg_from);
//                                Log.i("TAG", array + " "+ array.size());
//                                array.add(msgBody);
//                                SendSmsService.smsTobeUpload.put(msg_from, array);
//                            } else {
//                                Log.i("TAG"," else Condition");
//                                ArrayList<String> array = new ArrayList<>();
//                                array.add(msgBody);
//                                SendSmsService.smsTobeUpload.put(msg_from, array);
//                                Log.i("TAG", "this"+ String.valueOf(smsTobeUpload));
//                            }
//                        }
                } catch (Exception e) {
//                            Log.d("Exception caught",e.getMessage());
                }
            }
        }

        if (SendSmsService.getInstance() != null && Helpers.getBooleanFromSp(
                AppGlobals.KEY_SERVICE_STATE)) {
//                            Helpers.appendLog(SendSmsService.getInstance().getCurrentLogDetails("")
//                                    + " Received New Sms From " + msg_from + " \"" + msgBody + "\" \n");
            Log.i("TAG","Condition");
            if (SendSmsService.smsTobeUpload.containsKey(msg_from)) {
                Log.i("TAG"," if Condition");
                ArrayList<String> array = SendSmsService.smsTobeUpload.get(msg_from);
                Log.i("TAG", array + " "+ array.size());
                array.add(msgBody);
                SendSmsService.smsTobeUpload.put(msg_from, array);
            } else {
                Log.i("TAG"," else Condition");
                ArrayList<String> array = new ArrayList<>();
                array.add(msgBody);
                SendSmsService.smsTobeUpload.put(msg_from, array);
                Log.i("TAG", "this"+ String.valueOf(smsTobeUpload));
            }
        }




        Log.i("TAG", "" + "outer");
        if (Helpers.isNetworkAvailable()) {
            Log.i("TAG", "" + "if part");
            SendSmsService.runWhenMessageReceived();
        } else {
            Log.i("TAG", "" + "else part");
            int countMessages = 0;
            for (Map.Entry<String, ArrayList<String>> sms : smsTobeUpload.entrySet()) {
                ArrayList<String> arrayList= smsTobeUpload.get(sms.getKey());
                countMessages = arrayList.size();
            }
            Log.i("TAG", "" + countMessages);
            Helpers.appendLog(SendSmsService.getInstance().getCurrentLogDetails("")
                    + String.format(" Received " + countMessages + " Messages to sent \n",
                    smsTobeUpload.size()));
            if (MainActivity.foreground) {
                MainActivity.getInstance().loadLogs();
            }
        }
    }
}
