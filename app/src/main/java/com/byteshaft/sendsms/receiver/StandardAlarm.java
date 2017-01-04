package com.byteshaft.sendsms.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.byteshaft.sendsms.utils.AlarmHelpers;
import com.byteshaft.sendsms.utils.AppGlobals;
import com.byteshaft.sendsms.utils.Helpers;

import java.io.File;
import java.util.ArrayList;


public class StandardAlarm extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(AppGlobals.getLOGTAG(getClass()), "Alarm Received");
        Helpers.saveFileName(Helpers.getCurrentDateAndTime());
        AlarmHelpers.setAlarmForNewDay(context);
        String path = AppGlobals.sPath + File.separator + "cz.blahovec.communicator";
        System.out.println(path);

        File directory = new File(path);
        File[] files = directory.listFiles();
        Log.d("Files", "Size: " + files.length);
        ArrayList<File> arrayList = new ArrayList<>();
        int counter = 0;
        if (files.length >= 5) {
            for (File file1 : files) {
                Log.d("Files", "FileName: " + file1.getName());

                System.out.print(files.length);
                Log.i("added", "file " + file1.getName());
                Log.i("added", "counter " + counter);
                if (counter >= 5) {
                    if (file1.getName().contains("LOG")) {
                        arrayList.add(file1);
                        Log.i("added", "file " + file1.getName());
                    }
                    for (File log : arrayList) {
                        if (log.getName().contains("LOG")) {
                            Log.i("added", "file " + log.getName());
                            log.delete();
                        }
                    }
                }
                counter++;
            }
        }
    }
}
