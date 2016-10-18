package com.byteshaft.sendsms;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.telephony.SmsManager;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.byteshaft.requests.HttpRequest;
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
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements HttpRequest.OnReadyStateChangeListener,
        HttpRequest.OnErrorListener, SmsState {

    private static final int REQUEST_ID_MULTIPLE_PERMISSIONS = 0;
    private HttpRequest mRequest;
    private String currentNumber = "";
    private int smsCounter = 0;
    boolean status = false;
    private JSONArray mJsonArray;
    private SmsState smsState;
    private static final String COLON = ":";
    private static final String SPACE = " ";
    private TextView logTextView;
    private static boolean foreground = false;
    private boolean taskRunning = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        logTextView = (TextView) findViewById(R.id.logs);
        checkAndRequestPermissions();
        smsState = this;
    }

    @Override
    protected void onResume() {
        super.onResume();
        foreground = true;
        loadLogs();
    }

    @Override
    protected void onPause() {
        super.onPause();
        foreground = false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        foreground = false;
    }

    private void checkAndRequestPermissions() {
        int storagePermission = ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE);
        int smsPermission = ContextCompat.checkSelfPermission(this,
                Manifest.permission.SEND_SMS);
        final List<String> listPermissionsNeeded = new ArrayList<>();
        if (storagePermission != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            Log.i("TAG", "WRITE_EXTERNAL_STORAGE");
        }
        if (smsPermission != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.SEND_SMS);
            Log.i("TAG", "SEND_SMS");
        }
        Log.i("TAG", " " + listPermissionsNeeded.size());
        if (!listPermissionsNeeded.isEmpty() && listPermissionsNeeded.size() > 0) {
            android.support.v7.app.AlertDialog.Builder alertDialogBuilder =
                    new android.support.v7.app.AlertDialog.Builder(this);
            alertDialogBuilder.setTitle("Permissions Required");
            alertDialogBuilder.setMessage("In order to use SendSms, you need to grant some permissions.")
                    .setCancelable(false).setPositiveButton("Continue", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    dialog.dismiss();
                    ActivityCompat.requestPermissions(MainActivity.this,
                            listPermissionsNeeded.toArray(new String[listPermissionsNeeded.size()]),
                            REQUEST_ID_MULTIPLE_PERMISSIONS);
                }
            });
            android.support.v7.app.AlertDialog alertDialog = alertDialogBuilder.create();
            alertDialog.show();
        } else {
            // if permissions are granted
            if (!Helpers.getBooleanFromSp(AppGlobals.KEY_FILES_CREATED)) {
                Helpers.createFolderInExternalStorage();
                Helpers.createFileInsideAppFolder();
            }
            getSmsAndSend();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case REQUEST_ID_MULTIPLE_PERMISSIONS: {
                Map<String, Integer> perms = new HashMap<>();
                // Initialize the map with both permissions
                perms.put(Manifest.permission.WRITE_EXTERNAL_STORAGE, PackageManager.PERMISSION_GRANTED);
                perms.put(Manifest.permission.SEND_SMS, PackageManager.PERMISSION_GRANTED);
                // Fill with actual results from user
                if (grantResults.length > 0) {
                    for (int i = 0; i < permissions.length; i++)
                        perms.put(permissions[i], grantResults[i]);
                    // Check for both permissions
                    if (perms.get(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                            perms.get(Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED) {
                        Log.d("MainActivity", "permission granted");
                        // process the normal flow
                        //else any one or both the permissions are not granted
                        if (!Helpers.getBooleanFromSp(AppGlobals.KEY_FILES_CREATED)) {
                            Helpers.createFolderInExternalStorage();
                            Helpers.createFileInsideAppFolder();
                        }
                        getSmsAndSend();
                    } else {
                        Log.d("MainActivity", "Some permissions are not granted ask again ");
                        if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                                Manifest.permission.READ_CONTACTS) || ActivityCompat.
                                shouldShowRequestPermissionRationale(this, Manifest.permission.READ_PHONE_STATE)) {
                            showDialogOK(new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    switch (which) {
                                        case DialogInterface.BUTTON_POSITIVE:
                                            dialog.dismiss();
                                            checkAndRequestPermissions();
                                            break;
                                        case DialogInterface.BUTTON_NEGATIVE:
                                            dialog.dismiss();
                                            // proceed with logic by disabling the related features or quit the app.
                                            break;
                                    }
                                }
                            });
                        } else {
                            Toast.makeText(this, "Go to settings and enable permissions", Toast.LENGTH_LONG)
                                    .show();
                            //                            //proceed with logic by disabling the related features or quit the app.
                        }
                    }
                }
            }
        }

    }

    private void loadLogs() {
        logTextView.setText(null);
        File file = new File(Helpers.getLogFile());
        if (file.exists()) {
            StringBuilder text = new StringBuilder();
            try {
                BufferedReader br = new BufferedReader(new FileReader(file));
                String line;
                while ((line = br.readLine()) != null) {
                    text.append(line);
                    text.append('\n');
                }
                logTextView.setText(text.toString());
                br.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

        private void showDialogOK(DialogInterface.OnClickListener okListener) {
        new AlertDialog.Builder(this)
                .setCancelable(false)
                .setMessage("In order to use Prank Call Application some permissions are necessary. " +
                        "Please, continue and grant them all.")
                .setPositiveButton("Continue", okListener)
                .setNegativeButton("Cancel", okListener)
                .create()
                .show();
    }

    private void getSmsAndSend() {
        taskRunning = true;
        JSONObject jsonObject;
        String url = null;
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
                data.put("command", jsonObject.getString("command"));
                String parameters = jsonObject.getString("parameters").replaceAll("'", "\"");
                String params = ", \"parameters\":" + parameters;
                finalData = data.toString().replace("}", " ") + params + "}";

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
                            if (jsonArray.length() > 0) {
                                mJsonArray = jsonArray;
                                smsCounter = 0;
                                sendSMS();
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
            registerReceiver(new BroadcastReceiver() {

                @Override
                public void onReceive(Context context, Intent intent) {
                    String result = "";
                    Log.i(AppGlobals.getLOGTAG(getClass()), " sms response "+ getResultCode());
                    switch (getResultCode()) {
                        case Activity.RESULT_OK:
                            result = "Successful";
                            Log.i("TAG", "OK");
                            break;
                        case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                            result = "Failed";
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
                    if (smsCounter < mJsonArray.length()) {
                        try {
                            String fullLog = getCurrentLogDetails(currentNumber) + SPACE + result +
                                    " message id " + json.getString("sms_id") + " to " +
                                    json.getString("receiver") + SPACE + "\"" + json.getString("raw_sms") + " \"";
                            Helpers.appendLog(fullLog);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        if (foreground) {
                            loadLogs();
                        }
                        smsCounter = smsCounter+1;
                        Log.i("counter", "count " + smsCounter);
                        smsState.messageState();
                    }
                }

            }, new IntentFilter(SENT));
     /* Register for Delivery event */
            registerReceiver(new BroadcastReceiver() {

                @Override
                public void onReceive(Context context, Intent intent) {
                    Toast.makeText(getApplicationContext(), "Delivered",
                            Toast.LENGTH_LONG).show();
                }

            }, new IntentFilter(DELIVERED));

      /*Send SMS*/
            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage("03006860746", null, json.getString("raw_sms"), sentPI,
                    deliverPI);
            currentNumber = "03006860746";
        } catch (Exception ex) {
            Toast.makeText(getApplicationContext(),
                    ex.getMessage().toString(), Toast.LENGTH_SHORT)
                    .show();
            ex.printStackTrace();
        }
        return status;
    }

    @Override
    public void onError(HttpRequest request, short error, Exception exception) {
        Log.i("TAG", String.valueOf(request.getError()));
        Log.i("TAG", String.valueOf(error));
        Log.i("TAG", String.valueOf(exception.getCause()));
    }

    private String getCurrentLogDetails(String currentNumber) {
        String log = null;
        Uri mSmsSentQueryUri = Uri.parse("content://sms/sent");
        Cursor cursor1 = getContentResolver().query(mSmsSentQueryUri, new String[]{"_id", "thread_id", "address", "person", "date", "body", "type"}, null, null, null);
        startManagingCursor(cursor1);
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
                        Calendar calendar = Calendar.getInstance();
                        calendar.setTimeInMillis(Long.parseLong(date));
                        int mYear = calendar.get(Calendar.YEAR);
                        int mMonth = calendar.get(Calendar.MONTH);
                        int mDay = calendar.get(Calendar.DAY_OF_MONTH);
                        int hours = calendar.get(Calendar.HOUR_OF_DAY);
                        int minutes = calendar.get(Calendar.MINUTE);
                        int seconds = calendar.get(Calendar.SECOND);
                        log = mYear + COLON + mMonth + COLON + mDay + SPACE +
                                hours + COLON + minutes + COLON + seconds;
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
        return log;
    }

    @Override
    public void messageState() {
        if (smsCounter == mJsonArray.length()) {
            Log.e("TAG", "Matched");
            if (!taskRunning) {
                Log.e("TAG", "Task not running");
                new android.os.Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        Log.e("TAG", "Task not running");
                        getSmsAndSend();
                    }
                }, 5000);
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
}
