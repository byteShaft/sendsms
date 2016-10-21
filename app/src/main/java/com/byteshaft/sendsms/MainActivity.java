package com.byteshaft.sendsms;

import android.Manifest;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.CompoundButton;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.byteshaft.sendsms.utils.AppGlobals;
import com.byteshaft.sendsms.utils.Helpers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.byteshaft.sendsms.utils.AppGlobals.sPath;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_ID_MULTIPLE_PERMISSIONS = 0;
    public TextView logTextView;
    public static boolean foreground = false;
    public static boolean taskRunning = false;
    private static MainActivity instance;
    private ScrollView mScrollView;
    private Switch mSwitch;

    public static MainActivity getInstance() {
        return instance;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mScrollView = (ScrollView) findViewById(R.id.scroll_view);
        instance = this;
        logTextView = (TextView) findViewById(R.id.logs);
        mSwitch = (Switch) findViewById(R.id.service_switch);
        mSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b) {
                    Log.i("TAG", "" + isMyServiceRunning(SendSmsService.class));
                    if (!isMyServiceRunning(SendSmsService.class) && !Helpers.getBooleanFromSp(AppGlobals.KEY_SERVICE_STATE)) {
                        Helpers.saveFileName(Helpers.getCurrentDateAndTime());
                        startService(new Intent(getApplicationContext(), SendSmsService.class));
                        Helpers.saveBooleanToSp(AppGlobals.KEY_SERVICE_STATE, true);
                        mSwitch.setChecked(true);
                        mSwitch.setText("Service Running");
                    }
                } else {
                    SendSmsService.getInstance().unregiReceiver();
                    stopService(new Intent(getApplicationContext(), SendSmsService.class));
                    Helpers.saveFileName("");
                    if (SendSmsService.getInstance() != null) {
                        SendSmsService.getInstance().onDestroy();
                    }
                    Helpers.saveBooleanToSp(AppGlobals.KEY_SERVICE_STATE, false);
                }
            }
        });
        checkAndRequestPermissions();
    }

    private List<File> clearLogs(File parentDir) {
        ArrayList<File> inFiles = new ArrayList<>();
        File[] files = parentDir.listFiles();
        for (File file : files) {
            if (file.isFile()) {
                if (file.getName().startsWith("LOG_")) {
                    Log.i("FIle", file.getName());
                    file.delete();
                }
            }
        }
        logTextView.setText(null);
        Helpers.saveFileName("");
        return inFiles;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_clear_logs) {
            clearLogs(new File(sPath + File.separator + AppGlobals.sFolderName));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        foreground = true;
        File file = new File(Helpers.getLogFile());
        if (file.exists()) {
            loadLogs();
        }
        Log.i("TAG", "boolean " + Helpers.getBooleanFromSp(AppGlobals.KEY_SERVICE_STATE));
        if (Helpers.getBooleanFromSp(AppGlobals.KEY_SERVICE_STATE)) {
            mSwitch.setText("Service Running");
            mSwitch.setChecked(true);
        } else {
            mSwitch.setChecked(false);
            mSwitch.setText("Service Stopped");
        }
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
            if (!isMyServiceRunning(SendSmsService.class) && Helpers.getBooleanFromSp(AppGlobals.KEY_SERVICE_STATE)) {
                Helpers.saveFileName(Helpers.getCurrentDateAndTime());
                startService(new Intent(getApplicationContext(), SendSmsService.class));
                mSwitch.setChecked(true);
                mSwitch.setText("Service Running");
            } else {
                Helpers.saveFileName("");
                mSwitch.setChecked(false);
                mSwitch.setText("Service Stopped");
            }
        }
    }

    public boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
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
                        if (!isMyServiceRunning(SendSmsService.class) && Helpers.getBooleanFromSp(AppGlobals.KEY_SERVICE_STATE)) {
                            Helpers.saveFileName(Helpers.getCurrentDateAndTime());
                            startService(new Intent(getApplicationContext(), SendSmsService.class));
                            mSwitch.setChecked(true);
                            mSwitch.setText("Service Running");
                        } else {
                            Helpers.saveFileName("");
                            mSwitch.setChecked(false);
                            mSwitch.setText("Service Stopped");
                        }
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

    public void loadLogs() {
        logTextView.setText(null);
        if (isMyServiceRunning(SendSmsService.class)) {
            mSwitch.setText("Service Running");
            mSwitch.setChecked(true);
        } else {
            mSwitch.setChecked(false);
            mSwitch.setText("Service Stopped");
        }
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
        mScrollView.post(new Runnable() {

            @Override
            public void run() {
                mScrollView.fullScroll(ScrollView.FOCUS_DOWN);
            }
        });
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
}
