package com.byteshaft.sendsms;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.byteshaft.sendsms.utils.Helpers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_ID_MULTIPLE_PERMISSIONS = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        checkAndRequestPermissions();

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
            Helpers.createFolderInExternalStorage();
            Helpers.createFileInsideAppFolder();
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
                        Helpers.createFolderInExternalStorage();
                        Helpers.createFileInsideAppFolder();
                    }
                    else {
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
