package com.byteshaft.sendsms.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.byteshaft.sendsms.utils.AppGlobals;
import com.byteshaft.sendsms.utils.Helpers;

/**
 * Created by s9iper1 on 11/12/16.
 */

public abstract class Receiver extends BroadcastReceiver {

    private static int lastState = TelephonyManager.CALL_STATE_IDLE;
    private static boolean isIncoming;

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i("TAG", "receiver called");
        if (!Helpers.getBooleanFromSp(AppGlobals.KEY_SERVICE_STATE)) {
            return;
        }

        if (intent.getAction().equals("android.intent.action.NEW_OUTGOING_CALL")) {

        } else{
            String stateStr = intent.getExtras().getString(TelephonyManager.EXTRA_STATE);
            String number = intent.getExtras().getString(TelephonyManager.EXTRA_INCOMING_NUMBER);
            int state = 0;
            if(stateStr.equals(TelephonyManager.EXTRA_STATE_IDLE)){
                state = TelephonyManager.CALL_STATE_IDLE;
            }
            else if(stateStr.equals(TelephonyManager.EXTRA_STATE_OFFHOOK)){
                state = TelephonyManager.CALL_STATE_OFFHOOK;
            }
            else if(stateStr.equals(TelephonyManager.EXTRA_STATE_RINGING)){
                state = TelephonyManager.CALL_STATE_RINGING;
            }

            onCallStateChanged(context, state, number);
        }
    }

    //Derived classes should override these to respond to specific events of interest
    protected abstract void onIncomingCallReceived(Context ctx, String number);
    protected abstract void onOutgoingCallStarted(Context ctx, String number);


    public void onCallStateChanged(Context context, int state, String number) {
        if(lastState == state){
            return;
        }
        switch (state) {
            case TelephonyManager.CALL_STATE_RINGING:
                isIncoming = true;
                onIncomingCallReceived(context, number);
                break;
            case TelephonyManager.CALL_STATE_OFFHOOK:
                Log.i("Receiver", "call state :" + state);
                if(lastState != TelephonyManager.CALL_STATE_RINGING) {
                    isIncoming = false;
                    onOutgoingCallStarted(context, number);
                }
                else
                {
                    isIncoming = true;
                }

                break;
            case TelephonyManager.CALL_STATE_IDLE:
                //Went to idle-  this is the end of a call.  What type depends on previous state(s)
                if(lastState == TelephonyManager.CALL_STATE_RINGING){
                    //Ring but no pickup-  a miss

                }
                else if(isIncoming){

                }
                else{

                }
                break;
        }
        lastState = state;
    }
}
