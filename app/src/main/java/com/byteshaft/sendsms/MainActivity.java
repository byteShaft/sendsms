package com.byteshaft.sendsms;

import android.Manifest;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.byteshaft.sendsms.utils.AlarmHelpers;
import com.byteshaft.sendsms.utils.AppGlobals;
import com.byteshaft.sendsms.utils.Helpers;
import com.byteshaft.sendsms.utils.Logs;

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
    public static boolean foreground = false;
    public static boolean taskRunning = false;
    private static MainActivity instance;
    private Switch mSwitch;
    private ListView mListView;
    public ArrayList<Logs> arrayList;
    public Adapter arrayAdapter;

    public static MainActivity getInstance() {
        return instance;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        arrayList = new ArrayList<>();
        AlarmHelpers.setAlarmForNewDay(getApplicationContext());
        mListView = (ListView) findViewById(R.id.list_view);
        arrayAdapter = new Adapter(getApplicationContext(), R.layout.list_item,
                arrayList);
        mListView.setAdapter(arrayAdapter);
        instance = this;
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
//                        Helpers.appendLog(SendSmsService.getInstance().getCurrentLogDetails("") +  " Service is started (onCreate)\n");
                    }
                } else {
                    SendSmsService.getInstance().unregisterReceiver();
                    stopService(new Intent(getApplicationContext(), SendSmsService.class));
                    Helpers.saveFileName(Helpers.getCurrentDateAndTime());
                    if (SendSmsService.getInstance() != null) {
                        SendSmsService.getInstance().onDestroy();
                    }
                    Helpers.saveBooleanToSp(AppGlobals.KEY_SERVICE_STATE, false);
                }
            }
        });
        checkAndRequestPermissions();
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        File file = new File(Helpers.getLogFile());
        if (file.exists()) {
            new LoadLogs().execute();
        }
    }

    private List<File> clearLogs(File parentDir) {
        ArrayList<File> inFiles = new ArrayList<>();
        File[] files = parentDir.listFiles();
        for (File file : files) {
            if (file.isFile()) {
                if (file.getName().startsWith("LOG_")) {
                    Log.i("FIlE", file.getName());
                    file.delete();
                }
            }
        }
        arrayList.clear();
        arrayAdapter.notifyDataSetChanged();
        Helpers.saveFileName(Helpers.getCurrentDateAndTime());
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
        arrayAdapter.notifyDataSetChanged();
        Log.i("TAG", "boolean " + Helpers.getBooleanFromSp(AppGlobals.KEY_SERVICE_STATE));
        if (Helpers.getBooleanFromSp(AppGlobals.KEY_SERVICE_STATE)) {
            mSwitch.setChecked(true);
            mSwitch.setText("Service Running");
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

        // call permission
        int outGoingCallPermission = ContextCompat.checkSelfPermission(this,
                Manifest.permission.PROCESS_OUTGOING_CALLS);
        int phoneStatePermision = ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_PHONE_STATE);

        final List<String> listPermissionsNeeded = new ArrayList<>();
        if (storagePermission != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            Log.i("TAG", "WRITE_EXTERNAL_STORAGE");
        }
        if (smsPermission != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.SEND_SMS);
            Log.i("TAG", "SEND_SMS");
        }

        // call permissions

        if (outGoingCallPermission != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.PROCESS_OUTGOING_CALLS);
            Log.i("TAG", "WRITE_EXTERNAL_STORAGE");
        }
        if (phoneStatePermision != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.READ_PHONE_STATE);
            Log.i("TAG", "WRITE_EXTERNAL_STORAGE");
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
//                Helpers.appendLog(SendSmsService.getInstance().getCurrentLogDetails("") +  " Service is running (checkAndRequestPermissions)\n");
            } else {
                Helpers.saveFileName(Helpers.getCurrentDateAndTime());
                mSwitch.setChecked(false);
                mSwitch.setText("Service Stopped");
//                Helpers.appendLog(SendSmsService.getInstance().getCurrentLogDetails("") +  " Service is stopped (checkAndRequestPermissions)\n");
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
                    if (perms.get(Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
                            PackageManager.PERMISSION_GRANTED &&
                            perms.get(Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED &&
                            perms.get(Manifest.permission.PROCESS_OUTGOING_CALLS) ==
                                    PackageManager.PERMISSION_GRANTED &&
                    perms.get(Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
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
//                            Helpers.appendLog(SendSmsService.getInstance().getCurrentLogDetails("") +  " Service is running (onRequestPermissionsResult)\n");
                        } else {
                            Helpers.saveFileName(Helpers.getCurrentDateAndTime());
                            mSwitch.setChecked(false);
                            mSwitch.setText("Service Stopped");
//                            Helpers.appendLog(SendSmsService.getInstance().getCurrentLogDetails("") +  " Service is stopped (onRequestPermissionsResult)\n");
                        }
                    } else {
                        Log.d("MainActivity", "Some permissions are not granted ask again ");
                        if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                                Manifest.permission.SEND_SMS) || ActivityCompat.
                                shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
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
                        } else if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                                Manifest.permission.PROCESS_OUTGOING_CALLS) || ActivityCompat.
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
        if (isMyServiceRunning(SendSmsService.class)) {
            mSwitch.setChecked(true);
            mSwitch.setText("Service Running");
        } else {
            mSwitch.setChecked(false);
            mSwitch.setText("Service Stopped");
        }
        arrayAdapter.notifyDataSetChanged();
        scrollMyListViewToBottom();
    }

    private void scrollMyListViewToBottom() {
        mListView.post(new Runnable() {
            @Override
            public void run() {
                // Select the last row so it will scroll into view...
                mListView.setSelection(arrayAdapter.getCount() - 1);
            }
        });
    }

    private void showDialogOK(DialogInterface.OnClickListener okListener) {
        new AlertDialog.Builder(this)
                .setCancelable(false)
                .setMessage(String.format("In order to use %s Application some permissions are necessary. " +
                        "Please, continue and grant them all.", getString(R.string.app_name)))
                .setPositiveButton("Continue", okListener)
                .setNegativeButton("Cancel", okListener)
                .create()
                .show();
    }

    class LoadLogs extends AsyncTask<String, String, String> {

        private ProgressDialog progressDialog;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog = new ProgressDialog(MainActivity.this);
            progressDialog.setMessage("Loading Logs ...");
            progressDialog.setIndeterminate(true);
            progressDialog.show();
        }

        @Override
        protected String doInBackground(String... strings) {
            File file = new File(Helpers.getLogFile());
            if (file.exists()) {
                try {
                    BufferedReader br = new BufferedReader(new FileReader(file));
                    String line;
                    while ((line = br.readLine()) != null) {
                        Logs log = new Logs();
                        log.setLogs(line);
                        arrayList.add(log);
                        publishProgress();
                    }
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(String... values) {
            super.onProgressUpdate(values);
            arrayAdapter.notifyDataSetChanged();

        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            progressDialog.dismiss();
        }
    }

    class Adapter extends ArrayAdapter<Logs> {

        private ArrayList<Logs> arrayList;
        private ViewHolder viewHolder;

        public Adapter(Context context, int resource, ArrayList<Logs> arrayList) {
            super(context, resource);
            this.arrayList = arrayList;
        }

        @NonNull
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = getLayoutInflater().inflate(R.layout.list_item, parent, false);
                viewHolder = new ViewHolder();
                viewHolder.textView = (TextView) convertView.findViewById(R.id.log_text_view);
                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
            }
            Logs log = arrayList.get(position);
            viewHolder.textView.setText(log.getLogs());
            return convertView;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public int getCount() {
            return arrayList.size();
        }
    }

    static class ViewHolder{
        TextView textView;
    }
}
