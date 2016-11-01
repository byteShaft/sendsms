package com.byteshaft.sendsms;

import android.app.Activity;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.telephony.SmsManager;
import android.util.Log;
import android.widget.Toast;

import com.byteshaft.requests.HttpRequest;
import com.byteshaft.sendsms.utils.AlarmHelpers;
import com.byteshaft.sendsms.utils.AppGlobals;
import com.byteshaft.sendsms.utils.Helpers;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import static com.byteshaft.sendsms.MainActivity.foreground;
import static com.byteshaft.sendsms.MainActivity.taskRunning;


public class SendSmsService extends Service implements HttpRequest.OnReadyStateChangeListener,
        HttpRequest.OnErrorListener, SmsState {

    private HttpRequest mRequest;
    private String currentNumber = "";
    private int smsCounter = 0;
    boolean status = false;
    private static final String COLON = ":";
    private static final String SPACE = " ";
    private JSONArray mJsonArray;
    private SmsState smsState;
    private final int NOTIFICATION_ID = 10001;
    private Context mContext;
    private static SendSmsService instance;
    private static String api_key = "";
    private static String url = "";
    private BroadcastReceiver sendReceiver;
    private BroadcastReceiver deliverReceiver;
    private boolean serviceRunning = false;
    public static HashMap<String, ArrayList<String>> smsTobeUpload;
    public static int sMinInterval = 0;
    public static int sMaxInterval = 0;
    private boolean successRunning = false;
    private boolean failedRunning = false;


    public static SendSmsService getInstance() {
        return instance;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        smsTobeUpload = new HashMap<>();
        smsState = this;
        instance = this;
        startService();
        getSmsAndSend();
        serviceRunning = true;
        mContext = this;
        return START_NOT_STICKY;
    }

    private void startService() {
        Intent notificationIntent = new Intent(this, SendSmsService.class);
        notificationIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getService(this, 0 /* Request code */, notificationIntent,
                0);
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Blahovec Communicator")
                .setContentText("Service Running")
                .setAutoCancel(true)
                .setOngoing(true)
                .setWhen(System.currentTimeMillis())
                .setContentIntent(pendingIntent);
        startForeground(NOTIFICATION_ID, notificationBuilder.build());
    }

    public void getSmsAndSend() {
        taskRunning = true;
        JSONObject jsonObject;
        String finalData = "";
        JSONObject data = new JSONObject();
        File file = new File(Helpers.getConfigFile());
        if (file.exists()) {
            StringBuilder text = new StringBuilder();

            try {
                BufferedReader br = new BufferedReader(new FileReader(file));
                String line;

                while ((line = br.readLine()) != null) {
                    text.append(line);
                    text.append('\n');
                }
                br.close();
                jsonObject = new JSONObject(text.toString());
                url = jsonObject.getString("server_address");
                data.put("api_key", jsonObject.getString("api_key"));
                api_key = jsonObject.getString("api_key");
                sMinInterval = Integer.parseInt(jsonObject.getString("min_sending_interval"));
                sMaxInterval = Integer.parseInt(jsonObject.getString("max_sending_interval"));
                data.put("command", jsonObject.getString("command"));
                String parameters = jsonObject.getString("parameters").replaceAll("'", "\"");
                String params = ", \"parameters\":" + parameters;
                finalData = data.toString().replace("}", " ") + params + "}";
                Log.i("TAG", finalData);

            } catch (IOException e) {
                Log.e(AppGlobals.getLOGTAG(getClass()), "Error reading file");
                //You'll need to add proper error handling here
            } catch (JSONException e) {
                e.printStackTrace();
            }
            mRequest = new HttpRequest(getApplicationContext());
            mRequest.setOnReadyStateChangeListener(this);
            mRequest.setOnErrorListener(this);
            mRequest.open("POST", url);
            mRequest.setTimeout(20000);
            mRequest.setRequestHeader("Content-Type", "application/json");
            mRequest.send(finalData);
        } else Log.e(AppGlobals.getLOGTAG(getClass()), "File does not exist");
    }

    private void runWhenFailed(final JSONObject jsonObject, final String result) {
        failedRunning = true;
        JSONObject data = new JSONObject();
        Log.e("TAG", jsonObject.toString());
        HttpRequest request;
        try {
            data.put("api_key", api_key);
            data.put("command", "sms_server_mark_message_as_not_sent");
            JSONObject params = new JSONObject();
            params.put("sms_id", jsonObject.get("sms_id"));
            params.put("error_text", "Failed send message");
            data.put("parameters", params);
            request = new HttpRequest(getApplicationContext());
            request.setOnReadyStateChangeListener(new HttpRequest.OnReadyStateChangeListener() {
                @Override
                public void onReadyStateChange(HttpRequest request, int readyState) {
                    switch (readyState) {
                        case HttpRequest.STATE_DONE:
                            Log.i(AppGlobals.getLOGTAG(getClass()), "runWhenFailed STATE_DONE");
                            switch (request.getStatus()) {
                                case HttpURLConnection.HTTP_OK:
                                    failedRunning = true;
                                    taskRunning = false;
                                    String response = request.getResponseText();
                                    Log.i(AppGlobals.getLOGTAG(getClass()), " "+ response);
                                    processSmsResponse(result, jsonObject);
                            }
                    }
                }
            });
            request.setOnErrorListener(this);
            request.open("POST", url);
            request.setTimeout(20000);
            request.setRequestHeader("Content-Type", "application/json");
            Log.e("TAG", data.toString());
            request.send(data.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }


    private void runWhenSuccess(final JSONObject jsonObject, final String result) {
        successRunning = true;
        JSONObject data = new JSONObject();
        Log.e("TAG", jsonObject.toString());
        HttpRequest request;
        try {
            data.put("api_key", api_key);
            data.put("command", "sms_server_mark_message_as_sent");
            JSONObject params = new JSONObject();
            params.put("sms_id", jsonObject.get("sms_id"));
            data.put("parameters", params);
            request = new HttpRequest(getApplicationContext());
            request.setOnReadyStateChangeListener(new HttpRequest.OnReadyStateChangeListener() {
                @Override
                public void onReadyStateChange(HttpRequest request, int readyState) {
                    switch (readyState) {
                        case HttpRequest.STATE_DONE:
                            Log.i(AppGlobals.getLOGTAG(getClass()), " runWhenSuccess STATE_DONE");
                            switch (request.getStatus()) {
                                case HttpURLConnection.HTTP_OK:
                                    Log.e("Success", "sending message Success response..............");
                                    successRunning = false;
                                    taskRunning = false;
                                    String response = request.getResponseText();
                                    Log.i(AppGlobals.getLOGTAG(getClass()), " "+ response);
                                    try {
                                        JSONObject smsSendResponse = new JSONObject(response);
                                        if (smsSendResponse.has("result")) {
                                            if (smsSendResponse.getString("result").equals("TRUE")) {
                                                processSmsResponse(result, jsonObject);
                                            }
                                        }
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }

                            }
                    }
                }
            });
            request.setOnErrorListener(this);
            request.open("POST", url);
            request.setTimeout(20000);
            request.setRequestHeader("Content-Type", "application/json");
            request.send(data.toString());
            Log.e("Success", "sending message Success request..............");
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onReadyStateChange(HttpRequest request, int readyState) {
        switch (readyState) {
            case HttpRequest.STATE_DONE:
                Log.i(AppGlobals.getLOGTAG(getClass()), "STATE_DONE");
                switch (request.getStatus()) {
                    case HttpURLConnection.HTTP_OK:
                        taskRunning = false;
                        String response = mRequest.getResponseText();
                        mJsonArray = new JSONArray();
                        try {
                            JSONArray jsonArray = new JSONArray(response);
                            Log.i("TAG", "length "+ jsonArray.length());
                            Log.e("DATA", jsonArray.toString());
                            if (jsonArray.length() > 0) {
                                Helpers.appendLog(getCurrentLogDetails("") +
                                        String.format(" Received %d SMS to send \n", jsonArray.length()));
                                if (foreground) {
                                    MainActivity.getInstance().loadLogs();
                                }
                                mJsonArray = jsonArray;
                                smsCounter = 0;
                                sendSMS();
                            } else {
                                Log.e("TAG", "No Sms found");
                                Helpers.appendLog(getCurrentLogDetails("") +" No SMS to send \n");
                                if (MainActivity.foreground) {
                                    MainActivity.getInstance().loadLogs();
                                }
                                AlarmHelpers.setAlarm();
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                }
        }
    }

    public boolean sendSMS() throws JSONException {
        final JSONObject json = mJsonArray.getJSONObject(smsCounter);
        Log.i("TAG", json.toString());
        try {
            String SENT = "sent";
            String DELIVERED = "delivered";

            Intent sentIntent = new Intent(SENT);
     /*Create Pending Intents*/
            PendingIntent sentPI = PendingIntent.getBroadcast(
                    getApplicationContext(), 0, sentIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT);

            Intent deliveryIntent = new Intent(DELIVERED);

            PendingIntent deliverPI = PendingIntent.getBroadcast(
                    getApplicationContext(), 0, deliveryIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT);
     /* Register for SMS send action */
            sendReceiver = new BroadcastReceiver() {

                @Override
                public void onReceive(Context context, Intent intent) {
                    String result = "";
                    Log.i(AppGlobals.getLOGTAG(getClass()), " sms response "+ getResultCode());
                    switch (getResultCode()) {
                        case Activity.RESULT_OK:
                            result = "Successfully";
                            break;
                        case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                            result = "Failed";
                            Log.i("TAG", "Failed");
                            break;
                        case SmsManager.RESULT_ERROR_RADIO_OFF:
                            result = "Radio off";
                            break;
                        case SmsManager.RESULT_ERROR_NULL_PDU:
                            result = "No PDU defined";
                            break;
                        case SmsManager.RESULT_ERROR_NO_SERVICE:
                            result = "No service";
                            break;
                    }
                    Log.i("TAG", "counter " + smsCounter);
                    Log.i("TAG", "mJsonArray " + mJsonArray.length());
                    if (smsCounter <= mJsonArray.length()) {
                        if (result.equals("Failed") && !failedRunning) {
                            Log.e("TAG", "Failed");
                            // run code when failed
                            if (Helpers.isNetworkAvailable() && !successRunning) {
                                runWhenFailed(json, result + " Error Code " +
                                        SmsManager.RESULT_ERROR_GENERIC_FAILURE);
                                return;
                            }
                        } else if (result.equals("Successfully")) {
                            if (Helpers.isNetworkAvailable()) {
                                runWhenSuccess(json, result);
                                return;
                            }
                        } else if (!successRunning && !failedRunning)
                        Log.e("service", "Running outer");
                        processSmsResponse(result, json);
                    }
                    unregiReceiver();
                }

            };
            deliverReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    Toast.makeText(getApplicationContext(), "Delivered",
                            Toast.LENGTH_LONG).show();
                }

            };
            registerReceiver(sendReceiver, new IntentFilter(SENT));
//            registerReceiver(deliverReceiver, new IntentFilter(DELIVERED));

            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage("03448797786", null, json.getString("raw_sms"), sentPI,
                    deliverPI);
            currentNumber = "03448797786";
        } catch (Exception ex) {
            Toast.makeText(getApplicationContext(),
                    ex.getMessage().toString(), Toast.LENGTH_SHORT)
                    .show();
            ex.printStackTrace();
        }
        return status;
    }

    private void processSmsResponse(String result, JSONObject json) {
        try {
            String fullLog = getCurrentLogDetails(currentNumber) + SPACE + result +
                    " message ID " + json.getString("sms_id") + " to " +
                    json.getString("receiver") + SPACE + "\"" + json.getString("raw_sms") + "\"";
            Helpers.appendLog(fullLog);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        if (foreground) {
            MainActivity.getInstance().loadLogs();
        }
        smsCounter = smsCounter+1;
        Log.i("counter", "count " + smsCounter);
        Random rand = new Random();
        int randomNum = sMinInterval + rand.nextInt((sMaxInterval - sMinInterval) + 1);
        Log.i("TAG", "Random Number" + randomNum);
        new android.os.Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (serviceRunning) {
                    smsState.messageState();
                }
            }
        }, TimeUnit.SECONDS.toMillis(randomNum));
    }

    public void unregiReceiver() {
        try {
            unregisterReceiver(sendReceiver);
            unregisterReceiver(deliverReceiver);

        }catch (IllegalArgumentException e) {

        }

    }

    @Override
    public void onError(HttpRequest request, short error, Exception exception) {
        Log.i("TAG", String.valueOf(request.getError()));
        Log.i("TAG", String.valueOf(error));
        Log.i("TAG", String.valueOf(exception.getCause()));
    }

    public String getCurrentLogDetails(String currentNumber) {
        String log = null;
        Uri mSmsSentQueryUri = Uri.parse("content://sms/sent");
        Cursor cursor1 = getContentResolver().query(mSmsSentQueryUri, new String[]{"_id", "thread_id", "address", "person", "date", "body", "type"}, null, null, null);
//        ((Activity) mContext).startManagingCursor(cursor1);
        String[] columns = new String[]{"address", "person", "date", "body", "type", "_id"};
        if (cursor1.getCount() > 0) {
            int counter = 0;
            while (cursor1.moveToNext()) {
                String address = cursor1.getString(cursor1.getColumnIndex(columns[0]));
                if (counter == 0) {
                    if (address.equalsIgnoreCase(currentNumber)) { //put your number here
                        String name = cursor1.getString(cursor1.getColumnIndex(columns[1]));
                        String date = cursor1.getString(cursor1.getColumnIndex(columns[2]));
                        String body = cursor1.getString(cursor1.getColumnIndex(columns[3]));
                        String type = cursor1.getString(cursor1.getColumnIndex(columns[4]));
                        Log.d("*******", "body=" + body + "name=" + name + "date=" + date);
                        log = getTimeDate();
                        int apiVersion = android.os.Build.VERSION.SDK_INT;
                        if (apiVersion < android.os.Build.VERSION_CODES.LOLLIPOP){
                            getContentResolver().delete(
                                    Uri.parse("content://sms/" + cursor1.getInt(cursor1.
                                            getColumnIndex(columns[5]))), "date=?", null);
                        }
                    }
                } else {
                    break;
                }
                counter++;
            }
        }
        if (log == null) {
            log = getTimeDate();
        }
        return log;
    }

    private String getTimeDate() {
        String log;Calendar c = Calendar.getInstance();
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        log = df.format(c.getTime());
        return log;
    }

    @Override
    public void onDestroy() {
        serviceRunning = false;
        unregiReceiver();
        stopForeground(true);
        stopSelf();
        super.onDestroy();
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        if (!MainActivity.getInstance().isMyServiceRunning(SendSmsService.class)) {
            startService();
        }
    }

    @Override
    public void messageState() {
        if (smsCounter == mJsonArray.length()) {
            Log.e("TAG", "Matched");
            if (!taskRunning) {
                Log.e("TAG", "Task not running");
                Helpers.appendLog(getCurrentLogDetails("") +  " No SMS to send\n");
                if (foreground) {
                    MainActivity.getInstance().loadLogs();
                }
                new android.os.Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (serviceRunning) {
                            Log.e("TAG", "Task not running");
                            try {
                                unregisterReceiver(sendReceiver);
                                unregisterReceiver(deliverReceiver);
                            } catch (IllegalArgumentException e) {

                            }
                            getSmsAndSend();
                        }
                    }
                }, 10000);
            }
        }
        if (smsCounter < mJsonArray.length()) {
            try {
                sendSMS();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    public static void runWhenMessageReceived() {
        JSONObject data = new JSONObject();
        if (smsTobeUpload != null && smsTobeUpload.size() > 0) {
            for (final Map.Entry<String, ArrayList<String>> sms : smsTobeUpload.entrySet()) {
                for (final String message : sms.getValue()) {
                    HttpRequest request;
                    try {
                        data.put("api_key", api_key);
                        data.put("command", "sms_inbox_store_received_message");
                        JSONObject params = new JSONObject();
                        params.put("sender", sms.getKey());
                        params.put("raw_sms", message);
                        data.put("parameters", params);
                        request = new HttpRequest(AppGlobals.getContext());
                        request.setOnReadyStateChangeListener(new HttpRequest.OnReadyStateChangeListener() {
                            @Override
                            public void onReadyStateChange(HttpRequest request, int readyState) {
                                switch (readyState) {
                                    case HttpRequest.STATE_DONE:
                                        Log.i(AppGlobals.getLOGTAG(getClass()), "STATE_DONE");
                                        switch (request.getStatus()) {
                                            case HttpURLConnection.HTTP_OK:
                                                Log.e("Response", "sending message receiver Response..............");
                                                taskRunning = false;
                                                String response = request.getResponseText();
                                                Log.i("TAG", response);
                                                try {
                                                    JSONObject smsSendResponse = new JSONObject(response);
                                                    if (smsSendResponse.has("result")) {
                                                        if (smsSendResponse.getString("result")
                                                                .equals("TRUE")) {
                                                            Helpers.appendLog(SendSmsService.getInstance().getCurrentLogDetails("")
                                                                    + " Received new SMS from " + sms.getKey() + " \"" + message + "\" \n");
                                                        }
                                                    }
                                                } catch (JSONException e) {
                                                    e.printStackTrace();
                                                }

                                        }
                                }
                            }
                        });
                        request.open("POST", url);
                        request.setTimeout(20000);
                        request.setRequestHeader("Content-Type", "application/json");
                        Log.e("TAG", data.toString());
                        request.send(data.toString());
                        Log.e("SEND", "sending message receiver request..............");
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
            smsTobeUpload = new HashMap<>();
        }
    }

}
