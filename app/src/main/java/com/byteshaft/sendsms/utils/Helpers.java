package com.byteshaft.sendsms.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.MediaScannerConnection;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

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
        Log.i("TAG", getLogFile());
        File file = new File(getLogFile());
        if (!file.exists()) {
            Log.e("if", "File not exist");
            ifFileNotExist(log);
        } else {
            Log.e("if", "File exist");
            writeLogs(log);
        }
    }

    public static String getConfigFile() {
        return sPath + File.separator + AppGlobals.sFolderName + File.separator
                + AppGlobals.sConfigFileName;
    }

    public static String getLogFile() {
        return sPath + File.separator + AppGlobals.sFolderName + File.separator
                + getFileNameSP()+".txt";
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
                        "\"command\": \"sms_outbox_unsent\""+
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
        saveBooleanToSp(AppGlobals.KEY_FILES_CREATED, true);
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

    public static void saveFileName(String value) {
        SharedPreferences sharedPreferences = getPreferenceManager();
        sharedPreferences.edit().putString("log_file", "LOG_"+value).commit();
    }

    public static String getFileNameSP() {
        SharedPreferences sharedPreferences = getPreferenceManager();
        return sharedPreferences.getString("log_file", "");
    }

    private static void writeLogs(String log) {
        try {
            FileOutputStream writeLogFile = new FileOutputStream(Helpers.getLogFile(), true);
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(writeLogFile);
//            outputStreamWriter.append(log +"\n");
            outputStreamWriter.append(log);
            outputStreamWriter.close();
            writeLogFile.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {

        }
    }

    private static void ifFileNotExist(String log){
        FileWriter writer;
         String path = sPath + File.separator + AppGlobals.sFolderName ;
        try {
            File file = new File(path, getFileNameSP()+".txt");
            writer = new FileWriter(file);
            writer.append(log);
            writer.flush();
            writer.close();
            file.setReadable(true);
            file.setWritable(true);
            toBeScanned.add(file.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String getCurrentDateAndTime() {
        Calendar c = Calendar.getInstance();
//pablcz        SimpleDateFormat df = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss");
        SimpleDateFormat df = new SimpleDateFormat("yyyy_MM_dd");
        return df.format(c.getTime());

    }

    public static boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager)
                AppGlobals.getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = cm.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnected();
    }

}
