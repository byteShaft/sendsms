package com.byteshaft.sendsms.utils;

import android.content.SharedPreferences;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;

import static com.byteshaft.sendsms.utils.AppGlobals.sPath;

public class Helpers {

    private static ArrayList<String> toBeScanned = new ArrayList<>();

    public static void createFolderInExternalStorage() {
        File file = new File(sPath + File.separator +AppGlobals.sFolderName);
        Log.i("TAG", file.getAbsolutePath());
        if (!file.exists()) {
            file.mkdirs();
        }

    }

    public static void appendLog(String log) {
        String logFile = AppGlobals.sPath + File.separator + AppGlobals.sFolderName + File.separator
                + AppGlobals.sLogFile;
        File file = new File(logFile);
        if (!file.exists()) {
            writeLogs(log);
        } else {
            file.mkdirs();
            writeLogs(log);
        }
    }

    public static String getConfigFile() {
        return sPath + File.separator + AppGlobals.sFolderName + File.separator
                + AppGlobals.sConfigFileName;
    }

    public static String getLogFile() {
        return sPath + File.separator + AppGlobals.sFolderName + File.separator
                + AppGlobals.sLogFile;
    }

    public static void createFileInsideAppFolder() {
        File textFile = new File(AppGlobals.sPath + File.separator +
                AppGlobals.sFolderName);
            FileWriter writer;
            try {
                File file = new File(textFile, AppGlobals.sConfigFileName);
                writer = new FileWriter(file);
                writer.append("{\"server_address\":\"http://test-ws.blahovec.cz/\"," +
                        "\"api_key\":\"JLKNVImNXDPQp5blCQm6iOxfWqSzFSBt\"," +
                        "\"parameters\":\"{\'queue_name\': \'PRAHA\'}\"," +
                        "\"min_sending_interval\": \"10\"," +
                        "\"max_sending_interval\":\"30\"," +
                        "\"command\": \"sms_outbox_unsent_list\""+
                        "}");
                writer.flush();
                writer.close();
                file.setReadable(true);
                file.setWritable(true);
                toBeScanned.add(file.getAbsolutePath());
            } catch (IOException e) {
                e.printStackTrace();
            }
        String[] toBeScannedStr = new String[toBeScanned.size()];
        toBeScannedStr = toBeScanned.toArray(toBeScannedStr);
        MediaScannerConnection.scanFile(AppGlobals.getContext(), toBeScannedStr, null, new MediaScannerConnection.OnScanCompletedListener() {

            @Override
            public void onScanCompleted(String path, Uri uri) {
                System.out.println("SCAN COMPLETED: " + path);

            }
        });
    }


    private static SharedPreferences getPreferenceManager() {
        return PreferenceManager.getDefaultSharedPreferences(AppGlobals.getContext());
    }

    public static void saveBooleanToSp(String key, boolean value) {
        SharedPreferences sharedPreferences = getPreferenceManager();
        sharedPreferences.edit().putBoolean(key, value).apply();
    }

    public static boolean getBooleanFromSp(String key) {
        SharedPreferences sharedPreferences = getPreferenceManager();
        return sharedPreferences.getBoolean(key, false);
    }

    private static void writeLogs(String log) {
        try {
            FileOutputStream writeLogFile = new FileOutputStream(AppGlobals.sPath + File.separator
                    + AppGlobals.sFolderName + File.separator + AppGlobals.sLogFile ,true);
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(writeLogFile);
            outputStreamWriter.append(log +"\n");
            outputStreamWriter.close();
            writeLogFile.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {

        }
    }

}
