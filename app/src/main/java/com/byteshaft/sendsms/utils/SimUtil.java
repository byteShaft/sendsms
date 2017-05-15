package com.byteshaft.sendsms.utils;

import android.app.PendingIntent;
import android.content.Context;
import android.os.Build;
import android.os.IBinder;
import android.telephony.SmsManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.util.Log;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by s9iper1 on 5/15/17.
 */

public class SimUtil {

    private static final String TAG = "simUtils";


    public static boolean sendSMS(Context ctx, int simID, String toNum, String centerNum,
                                  String smsText, PendingIntent sentIntent, PendingIntent deliveryIntent) {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            ArrayList<Integer> simCardList = new ArrayList<>();
            SubscriptionManager subscriptionManager;
            subscriptionManager = SubscriptionManager.from(ctx);
            final List<SubscriptionInfo> subscriptionInfoList = subscriptionManager
                    .getActiveSubscriptionInfoList();
            for (SubscriptionInfo subscriptionInfo : subscriptionInfoList) {
                int subscriptionId = subscriptionInfo.getSubscriptionId();
                simCardList.add(subscriptionId);
            }
            Log.i(TAG, "id"  + simID);
            int smsToSendFrom = simCardList.get(simID); //assign your desired sim to send sms, or user selected choice
            Log.i(TAG, "send from"  + smsToSendFrom);
            SmsManager.getSmsManagerForSubscriptionId(smsToSendFrom)
                    .sendTextMessage(toNum, centerNum, smsText,sentIntent , deliveryIntent); //use your phone number, message and pending intents
            // only for gingerbread and newer versions
            return true;
        } else {


            String name;

            try {
                if (simID == 0) {
                    name = "isms";
                    // for model : "Philips T939" name = "isms0"
                } else if (simID == 1) {
                    name = "isms2";
                } else {
                    throw new Exception("can not get service which for sim '" + simID + "', only 0,1 accepted as values");
                }
                Method method = Class.forName("android.os.ServiceManager").getDeclaredMethod("getService", String.class);
                method.setAccessible(true);
                Object param = method.invoke(null, name);

                method = Class.forName("com.android.internal.telephony.ISms$Stub").getDeclaredMethod("asInterface", IBinder.class);
                method.setAccessible(true);
                Object stubObj = method.invoke(null, param);
                if (Build.VERSION.SDK_INT < 18) {
                    method = stubObj.getClass().getMethod("sendText", String.class, String.class, String.class, PendingIntent.class, PendingIntent.class);
                    method.invoke(stubObj, toNum, centerNum, smsText, sentIntent, deliveryIntent);
                } else {
                    method = stubObj.getClass().getMethod("sendText", String.class, String.class, String.class, String.class, PendingIntent.class, PendingIntent.class);
                    method.invoke(stubObj, ctx.getPackageName(), toNum, centerNum, smsText, sentIntent, deliveryIntent);
                }

                return true;
            } catch (ClassNotFoundException e) {
                Log.e(TAG, "ClassNotFoundException:" + e.getMessage());
            } catch (NoSuchMethodException e) {
                Log.e(TAG, "NoSuchMethodException:" + e.getMessage());
            } catch (InvocationTargetException e) {
                Log.e(TAG, "InvocationTargetException:" + e.getMessage());
            } catch (IllegalAccessException e) {
                Log.e(TAG, "IllegalAccessException:" + e.getMessage());
            } catch (Exception e) {
                Log.e(TAG, "Exception:" + e.getMessage());
            }
            return false;
        }
    }


    public static boolean sendMultipartTextSMS(Context ctx, int simID, String toNum,
                                               String centerNum, ArrayList<String> smsTextlist,
                                               ArrayList<PendingIntent> sentIntentList,
                                               ArrayList<PendingIntent> deliveryIntentList) {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            ArrayList<Integer> simCardList = new ArrayList<>();
            SubscriptionManager subscriptionManager;
            subscriptionManager = SubscriptionManager.from(ctx);
            final List<SubscriptionInfo> subscriptionInfoList = subscriptionManager
                    .getActiveSubscriptionInfoList();
            for (SubscriptionInfo subscriptionInfo : subscriptionInfoList) {
                int subscriptionId = subscriptionInfo.getSubscriptionId();
                simCardList.add(subscriptionId);
            }
            Log.i(TAG, "id" + simID);
            int smsToSendFrom = simCardList.get(simID); //assign your desired sim to send sms, or user selected choice
            Log.i(TAG, "send from" + smsToSendFrom);
            SmsManager.getSmsManagerForSubscriptionId(smsToSendFrom)
                    .sendMultipartTextMessage(toNum, centerNum, smsTextlist, sentIntentList, deliveryIntentList); //use your phone number, message and pending intents
            // only for gingerbread and newer versions
            return true;
        } else {

            // for previous devices 22
            String name;
            try {
                if (simID == 0) {
                    name = "isms";
                    // for model : "Philips T939" name = "isms0"
                } else if (simID == 1) {
                    name = "isms2";
                } else {
                    throw new Exception("can not get service which for sim '" + simID + "', only 0,1 accepted as values");
                }
                Method method = Class.forName("android.os.ServiceManager").getDeclaredMethod("getService", String.class);
                method.setAccessible(true);
                Object param = method.invoke(null, name);

                method = Class.forName("com.android.internal.telephony.ISms$Stub").getDeclaredMethod("asInterface", IBinder.class);
                method.setAccessible(true);
                Object stubObj = method.invoke(null, param);
                if (Build.VERSION.SDK_INT < 18) {
                    method = stubObj.getClass().getMethod("sendMultipartText", String.class, String.class, List.class, List.class, List.class);
                    method.invoke(stubObj, toNum, centerNum, smsTextlist, sentIntentList, deliveryIntentList);
                } else {
                    method = stubObj.getClass().getMethod("sendMultipartText", String.class, String.class, String.class, List.class, List.class, List.class);
                    method.invoke(stubObj, ctx.getPackageName(), toNum, centerNum, smsTextlist, sentIntentList, deliveryIntentList);
                }
                return true;
            } catch (ClassNotFoundException e) {
                Log.e(TAG, "ClassNotFoundException:" + e.getMessage());
            } catch (NoSuchMethodException e) {
                Log.e(TAG, "NoSuchMethodException:" + e.getMessage());
            } catch (InvocationTargetException e) {
                Log.e(TAG, "InvocationTargetException:" + e.getMessage());
            } catch (IllegalAccessException e) {
                Log.e(TAG, "IllegalAccessException:" + e.getMessage());
            } catch (Exception e) {
                Log.e(TAG, "Exception:" + e.getMessage());
            }
            return false;
        }
    }


}

