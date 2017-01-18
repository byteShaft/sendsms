package com.byteshaft.sendsms.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.byteshaft.sendsms.utils.AlarmHelpers;
import com.byteshaft.sendsms.utils.AppGlobals;
import com.byteshaft.sendsms.utils.Helpers;

import org.apache.commons.io.comparator.LastModifiedFileComparator;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;


public class StandardAlarm extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        AppGlobals.sHandlerSet = false;
        Log.i(AppGlobals.getLOGTAG(getClass()), "Alarm Received");
        Helpers.saveFileName(Helpers.getCurrentDateAndTime());
        AlarmHelpers.setAlarmForNewDay(context);
        String path = AppGlobals.sPath + File.separator + "cz.blahovec.communicator";
        System.out.println(path);

        File directory = new File(path);
        File[] files = directory.listFiles();
        Arrays.sort(files, LastModifiedFileComparator.LASTMODIFIED_REVERSE);
        Log.d("Files", "decending " + Arrays.toString(files));
        ArrayList<File> arrayList = new ArrayList<>();
        int counter = 0;
        if (files.length >= 51) {
            for (File file1 : files) {
                Log.e("Files", "File: " + file1.getName());
//                Log.i("added", "file " + file1.getName());
//                Log.i("added", "counter " + counter);
                if (counter >= 51) {
                    if (file1.getName().contains("LOG")) {
                        arrayList.add(file1);
                        Log.i("added", "file " + file1.getName());
                    }
                }
                counter++;
            }
        }
        for (File log : arrayList) {
            if (log.getName().contains("LOG")) {
                Log.i("DELETED", "file " + log.getName());
                log.delete();
            }
        }
    }
}
