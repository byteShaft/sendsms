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
import android.os.PowerManager;
import android.support.v4.app.NotificationCompat;
import android.telephony.SmsManager;
import android.util.Log;
import android.widget.Toast;

import com.byteshaft.requests.HttpRequest;
import com.byteshaft.sendsms.utils.AppGlobals;
import com.byteshaft.sendsms.utils.Helpers;

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

import static android.content.ContentValues.TAG;
import static com.byteshaft.sendsms.MainActivity.foreground;
import static com.byteshaft.sendsms.MainActivity.taskRunning;
import static com.byteshaft.sendsms.utils.AppGlobals.getContext;


public class SendSmsService extends Service implements HttpRequest.OnReadyStateChangeListener,
        HttpRequest.OnErrorListener, SmsState {

    private HttpRequest mRequest;
    private String currentNumber = "";
    boolean status = false;
    private static final String COLON = ":";
    private static final String SPACE = " ";
    private SmsState smsState;
    private final int NOTIFICATION_ID = 10001;
    private Context mContext;
    private static SendSmsService instance;
    public static String api_key = "";
    public static String url = "";
    private BroadcastReceiver sendReceiver;
    private BroadcastReceiver deliverReceiver;
    private boolean serviceRunning = false;
    public static HashMap<String, ArrayList<String>> smsTobeUpload;
    public static int sMinInterval = 0;
    public static int sMaxInterval = 0;
    private boolean successRunning = false;
    private boolean failedRunning = false;
    private int msgParts = 0;
    private BroadcastReceiver longMessageReceiver;
    private final String LONG_MESSAGE_SENT_ACTION = "long_sent";
    private boolean sendingLongSms = false;
    public static String queueName = "";
    private PowerManager.WakeLock wakeLock;

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
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                "service wake lock");
        wakeLock.acquire();
        return START_NOT_STICKY;
    }

    private void startService() {
        Intent notificationIntent = new Intent(this, SendSmsService.class);
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
                JSONObject parameterJosnObject = new JSONObject(parameters);
                queueName = parameterJosnObject.getString("queue_name");
                String params = ", \"parameters\":" + parameters;
                finalData = data.toString().replace("}", " ") + params + "}";
                Log.i("TAG", "data " + finalData);

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
            Helpers.appendLog(getCurrentLogDetails("") + " Requesting new SMS ...\n");
            mRequest.send(finalData);
            Helpers.appendLog(getCurrentLogDetails("") + " Request for new SMS sent.\n");
        } else Log.e(AppGlobals.getLOGTAG(getClass()), "File does not exist");
    }

    private void runWhenFailed(final JSONObject jsonObject, final String result) {
        failedRunning = true;
        JSONObject data = new JSONObject();
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
                                    failedRunning = false;
                                    taskRunning = false;
                                    String response = request.getResponseText();
                                    Log.i(AppGlobals.getLOGTAG(getClass()), " " + response);
                                    processSmsResponse(result, jsonObject);
                                    break;
                                default:
                                    Helpers.appendLog(getCurrentLogDetails("") + " NOT HTTP status for sms_server_mark_message_as_not_sent: " + request.getStatusText() + "\n");
                            }
                    }
                }
            });
            request.setOnErrorListener(new HttpRequest.OnErrorListener() {
                @Override
                public void onError(HttpRequest request, short error, Exception exception) {
                    failedRunning = false;
                    try {
                        Helpers.appendLog(getCurrentLogDetails("") +
                                String.format(" Report unsent SMS id %s failed", jsonObject.getString("sms_id")) + "\n");
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            });
            request.open("POST", url);
            request.setTimeout(20000);
            request.setRequestHeader("Content-Type", "application/json");
            request.send(data.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }


    private void runWhenSuccess(final JSONObject jsonObject, final String result) {
        successRunning = true;
        JSONObject data = new JSONObject();
        HttpRequest request;
        try {
            data.put("api_key", api_key);
            data.put("command", "sms_server_mark_message_as_sent");
            JSONObject params = new JSONObject();
            params.put("sms_id", jsonObject.get("sms_id"));
            data.put("parameters", params);
            Helpers.appendLog(String.format("%s Sending request sms_server_mark_message_as_sent" +
                    " (SMS id %s)...\n", getCurrentLogDetails(""), jsonObject.get("sms_id")));
            request = new HttpRequest(getApplicationContext());
            Helpers.appendLog(String.format("%s Request sms_server_mark_message_as_sent sent" +
                    " (SMS Id %s).\n", getCurrentLogDetails(""), jsonObject.get("sms_id")));

            request.setOnReadyStateChangeListener(new HttpRequest.OnReadyStateChangeListener() {

                @Override
                public void onReadyStateChange(HttpRequest request, int readyState) {
                    switch (readyState) {
                        case HttpRequest.STATE_DONE:
                            Log.i(AppGlobals.getLOGTAG(getClass()), " runWhenSuccess STATE_DONE");
                            switch (request.getStatus()) {
                                case HttpURLConnection.HTTP_OK:
                                    Log.d("Success", "sending message Success response..............");
                                    successRunning = false;
                                    taskRunning = false;
                                    String response = request.getResponseText();
                                    Log.i(AppGlobals.getLOGTAG(getClass()), " " + response);
                                    try {
                                        JSONObject smsSendResponse = new JSONObject(response);
                                        if (smsSendResponse.has("result")) {
                                            if (smsSendResponse.getString("result").equals("TRUE")) {
                                                processSmsResponse(result, jsonObject);
                                            }
                                        } else if (smsSendResponse.has("error")) {
                                            Helpers.appendLog(getCurrentLogDetails("") + smsSendResponse.getString("error") + "\n");
                                        } else
                                            Helpers.appendLog(getCurrentLogDetails("") + " Unknown result." + "\n");
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                    break;
                                default:
                                    Helpers.appendLog(getCurrentLogDetails("") + " NOT HTTP status for sms_server_mark_message_as_sent: " + request.getStatusText() + "\n");

                            }
                            break;
                        default:
                            Helpers.appendLog(getCurrentLogDetails("") + " HTTP status for sms_server_mark_message_as_sent: " + readyState + " | " + request.getStatusText() + "\n");
                    }
                }
            });

            //161505 pablcz BEGIN
            request.setOnErrorListener(new HttpRequest.OnErrorListener() {
                @Override
                public void onError(HttpRequest request, short error, Exception exception) {
                    successRunning = false;
                    try {
                        Helpers.appendLog(getCurrentLogDetails("") +
                                String.format(" Report sent SMS id %s failed", jsonObject.getString("sms_id")) + ". Try again.\n");
                        runWhenSuccess(jsonObject, result);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            });
            //161505 pablcz END

            request.open("POST", url);
            request.setTimeout(20000);
            request.setRequestHeader("Content-Type", "application/json");
            request.send(data.toString());
            Log.d("Success", "sending message Success request..............");
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
                        try {
                            JSONObject jsonObject = new JSONObject(response);
                            if (jsonObject.has("sms_id")) {
                                Helpers.appendLog(getCurrentLogDetails("") + " Received 1 SMS to send.\n");
                                if (foreground) {
                                    MainActivity.getInstance().loadLogs();
                                }
                                sendSMS(jsonObject);
                            } else {
                                Log.e("TAG", "No Sms found");
                                Random rand = new Random();
                                int randomNum = sMinInterval + rand.nextInt((sMaxInterval - sMinInterval) + 1);
                                Helpers.appendLog(getCurrentLogDetails("") + String.format(" No SMS to send. Waiting %s seconds ...", String.valueOf(randomNum)));
                                if (MainActivity.foreground) {
                                    MainActivity.getInstance().loadLogs();
                                }
                                new android.os.Handler().postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        getSmsAndSend();
                                    }
                                }, TimeUnit.SECONDS.toMillis(randomNum));
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                            Helpers.appendLog(getCurrentLogDetails("") + " JSON exception: " + Log.getStackTraceString(e) + "\n");
                            Random rand = new Random();
                            int randomNum = sMinInterval + rand.nextInt((sMaxInterval - sMinInterval) + 1);
                            new android.os.Handler().postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    getSmsAndSend();
                                }
                            }, TimeUnit.SECONDS.toMillis(randomNum));
                        }
                        break;
                    default: {
                        Helpers.appendLog(getCurrentLogDetails("") + " HTTP NOK status: " + request.getStatusText() + "\n");
                        Random rand = new Random();
                        int randomNum = sMinInterval + rand.nextInt((sMaxInterval - sMinInterval) + 1);
                        new android.os.Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                getSmsAndSend();
                            }
                        }, TimeUnit.SECONDS.toMillis(randomNum));

                    }

                }
                break;
        }
    }

    public boolean sendSMS(final JSONObject jsonObject) throws JSONException {
        Log.i("TAG", jsonObject.toString());
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
                    if (sendingLongSms)
                        return;
                    String result = "";
                    Log.i(AppGlobals.getLOGTAG(getClass()), " sms response " + getResultCode());
                    Helpers.appendLog(getCurrentLogDetails("") + String.format(" Sms response %s", getResultCode()) + "\n");
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
                        default:
                            result = "Failed"; //161202 PaBlCz
                    }
                    Log.i("TAG", "Response " + result);
                    if (result.equals("Failed") && !failedRunning) {
                        if (serviceRunning) {
                            Log.d("TAG", "Task not running");
                            try {
                                unregisterReceiver(sendReceiver);
                            } catch (IllegalArgumentException e) {

                            }
                        }
                        Log.e("TAG", "Failed");
                        // run code when failed
                        if (!successRunning) {
                            if (Helpers.isNetworkAvailable()) {
                                runWhenFailed(jsonObject, result + " Error Code " +
                                        SmsManager.RESULT_ERROR_GENERIC_FAILURE);
                            } else {
                                try {
                                    Helpers.appendLog(getCurrentLogDetails("") +
                                            String.format(" Report unsent SMS id %s failed , no internet connection",
                                                    jsonObject.getString("sms_id")) + "\n");
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                            return;
                        }
                    } else if (result.equals("Successfully")) {
                        if (serviceRunning) {
                            Log.e("TAG", "Task not running");
                            try {
                                unregisterReceiver(sendReceiver);
                            } catch (IllegalArgumentException e) {

                            }
                        }
                        if (Helpers.isNetworkAvailable()) {
                            runWhenSuccess(jsonObject, result);
                            return;
                        } else if (!Helpers.isNetworkAvailable()) {
                            try {
                                Helpers.appendLog(getCurrentLogDetails("") + String.format(" Report unsent SMS id %s failed, no internet connection", jsonObject.getString("sms_id")) + "\n");
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    } else if (!successRunning && !failedRunning) {
                        if (serviceRunning) {
                            Log.e("TAG", "Task not running");
                            try {
                                unregisterReceiver(sendReceiver);
                            } catch (IllegalArgumentException e) {

                            }
                            getSmsAndSend();
                        }
                        Log.e("service", "Running outer");
                    }
                    unregisterReceiver();
                }

            };
            deliverReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    Toast.makeText(getApplicationContext(), "Delivered",
                            Toast.LENGTH_LONG).show();
                }

            };
            Log.i("TAG", "length " + jsonObject.getString("raw_sms").length());
            SmsManager smsManager = SmsManager.getDefault();
            if (jsonObject.getString("raw_sms").length() > 160) {
                Log.i("SendSmsService", "Sending long sms");
                Helpers.appendLog(getCurrentLogDetails("") + " Sending long sms.\n"); //20161208 pablcz
                sendingLongSms = true;
                sendLongSms(jsonObject);
                currentNumber = jsonObject.getString("receiver");
//                currentNumber = "03448797786";
            } else {
                Log.i("SendSmsService", "Sending normal sms");
                Helpers.appendLog(getCurrentLogDetails("") + " Sending normal sms.\n"); //20161208 pablcz
                sendingLongSms = false;
                registerReceiver(sendReceiver, new IntentFilter(SENT));
                smsManager.sendTextMessage(jsonObject.getString("receiver"), null, jsonObject.getString("raw_sms"), sentPI,
                        deliverPI);
                currentNumber = jsonObject.getString("receiver");
//                currentNumber = "03448797786";
            }
        } catch (Exception ex) {
            Helpers.appendLog(getCurrentLogDetails("") + " Send SMS failed.\n"); //20161208 pablcz
            Toast.makeText(getApplicationContext(),
                    ex.getMessage().toString(), Toast.LENGTH_SHORT)
                    .show();
            ex.printStackTrace();
        }
        return status;
    }

    private void sendLongSms(final JSONObject jsonObject) throws JSONException {
        SmsManager sms = SmsManager.getDefault();
        ArrayList<String> parts = sms.divideMessage(jsonObject.getString("raw_sms"));
        final int numParts = parts.size();
        ArrayList<PendingIntent> sentIntents = new ArrayList<>();
        longMessageReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.d(TAG, "SMS onReceive intent received.");
                boolean anyError = false;
                switch (getResultCode()) {
                    case Activity.RESULT_OK:
                        break;
                    case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                    case SmsManager.RESULT_ERROR_NO_SERVICE:
                    case SmsManager.RESULT_ERROR_NULL_PDU:
                    case SmsManager.RESULT_ERROR_RADIO_OFF:
                        anyError = true;
                        break;
                }
                msgParts--;
                if (msgParts == 0) {
                    sendingLongSms = false;
                    if (anyError) {
                        Toast.makeText(context,
                                "Failed",
                                Toast.LENGTH_SHORT).show();
                        if (!failedRunning) {
                            Log.e("TAG", "Failed");
                            // run code when failed
                            if (Helpers.isNetworkAvailable() && !successRunning) {
                                runWhenFailed(jsonObject, "Failed" + " Error Code " +
                                        SmsManager.RESULT_ERROR_GENERIC_FAILURE);
                            }
                        }
                    } else {
                        //success
                        sendingLongSms = false;
                        Log.e("SENT", "SENT LONG MESSAGE");
                        if (Helpers.isNetworkAvailable()) {
                            runWhenSuccess(jsonObject, "Successfully");
                        }

                    }

                    unregisterReceiver(longMessageReceiver);
                }

            }
        };
        registerReceiver(longMessageReceiver, new IntentFilter(LONG_MESSAGE_SENT_ACTION));

        for (int i = 0; i < numParts; i++) {
            sentIntents.add(PendingIntent.getBroadcast(this, 0, new Intent(
                    LONG_MESSAGE_SENT_ACTION), 0));
        }

        Helpers.appendLog(getCurrentLogDetails("") + " Sending message " + jsonObject.getString("sms_id") + "...\n");
        sms.sendMultipartTextMessage(jsonObject.getString("receiver"), null, parts, sentIntents, null);
        msgParts = numParts;
    }

    private void processSmsResponse(String result, JSONObject json) {
        try {
            String fullLog = getCurrentLogDetails(currentNumber) + SPACE + result +
                    " SMS Id " + json.getString("sms_id") + " to " +
                    json.getString("receiver") + SPACE + "\"" + json.getString("raw_sms") + "\"" + "\n";
            Helpers.appendLog(fullLog);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Random rand = new Random();
        int randomNum = sMinInterval + rand.nextInt((sMaxInterval - sMinInterval) + 1);
        Log.i("TAG", "Random Number" + randomNum);
        Helpers.appendLog(getCurrentLogDetails("") +
                String.format(" Waiting %s seconds before sending next SMS ...\n", String.valueOf(randomNum)));
        if (foreground) {
            MainActivity.getInstance().loadLogs();
        }
        new android.os.Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (serviceRunning) {
                    smsState.messageState();
                }
            }
        }, TimeUnit.SECONDS.toMillis(randomNum));
    }

    public void unregisterReceiver() {
        try {
            unregisterReceiver(sendReceiver);
            unregisterReceiver(deliverReceiver);

        } catch (IllegalArgumentException e) {

        }

    }

    @Override
    public void onError(HttpRequest request, short error, Exception exception) {
        Helpers.appendLog(getCurrentLogDetails("") + " Check for new SMS failed \n");
        Log.i("TAG", String.valueOf(request.getError()));
        Log.i("TAG", String.valueOf(error));
        Log.i("TAG", String.valueOf(exception.getCause()));
        Random rand = new Random();
        int randomNum = sMinInterval + rand.nextInt((sMaxInterval - sMinInterval) + 1);
        new android.os.Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                getSmsAndSend();
            }
        }, TimeUnit.SECONDS.toMillis(randomNum));
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
                        if (apiVersion < android.os.Build.VERSION_CODES.LOLLIPOP) {
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
        String log;
        Calendar c = Calendar.getInstance();
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        log = df.format(c.getTime());
        String time = getCurrentMinute();
        String[] splitTime = time.split(":");
        int minute = Integer.valueOf(splitTime[0]);
        int seconds = Integer.valueOf(splitTime[1]);
        if (minute > 55 && !AppGlobals.sHandlerSet) {
            int remaining = 60 - minute;
            int secondsLeft = 59 - seconds-10;
            Log.i("TIME", "current "+time + "will be fire after " + remaining + " S " + secondsLeft);
            long milliSeconds = TimeUnit.MINUTES.toMillis(remaining) + TimeUnit.SECONDS.toMillis(secondsLeft);
            new android.os.Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    sendBroadcast(new Intent("com.byteShaft.night_alarm"));
                }
            }, milliSeconds);
            AppGlobals.sHandlerSet = true;
        }
        if (AppGlobals.sHandlerSet && minute == 59 || minute == 00) {
            Helpers.saveFileName(Helpers.getCurrentDateAndTime());
        }
        return log;
    }

    private String getCurrentMinute() {
        String log;
        Calendar c = Calendar.getInstance();
        SimpleDateFormat df = new SimpleDateFormat("mm:ss");
        log = df.format(c.getTime());
        return log;
    }

    @Override
    public void onDestroy() {
        if (wakeLock.isHeld()) {
            wakeLock.release();
        }
        serviceRunning = false;
        unregisterReceiver();
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
        if (!taskRunning) {

            new android.os.Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (serviceRunning) {
                        Log.e("TAG", "Task not running");
                        if (sendReceiver != null) {
                            try {
                                unregisterReceiver(sendReceiver);
                            } catch (IllegalArgumentException e) {

                            }
                        }
                        getSmsAndSend();
                    }
                }
            }, 100);
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
                        request = new HttpRequest(getContext());
                        request.setOnReadyStateChangeListener(new HttpRequest.OnReadyStateChangeListener() {
                            @Override
                            public void onReadyStateChange(HttpRequest request, int readyState) {
                                switch (readyState) {
                                    case HttpRequest.STATE_DONE:
                                        Log.i(AppGlobals.getLOGTAG(getClass()), "STATE_DONE");
                                        switch (request.getStatus()) {
                                            case HttpURLConnection.HTTP_OK:
                                                Log.d("Response", "sending message receiver Response..............");
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