package com.byteshaft.sendsms.utils;

import android.app.Application;
import android.content.Context;
import android.os.Environment;

import java.io.File;

public class AppGlobals extends Application {

    private static Context sContext;
    private static final String LOGTAG = "SEND_SMS";
    public static String sPath = Environment.getExternalStorageDirectory().getAbsolutePath()+ File.separator+"Android/data";
    public static String sFolderName;
    public static String sConfigFileName = "config.txt";
    public static final String KEY_FILES_CREATED = "file_created";
    public static final String KEY_SERVICE_STATE = "service_state";

    @Override
    public void onCreate() {
        super.onCreate();
        sContext = getApplicationContext();
        sFolderName = getPackageName();
    }

    public static Context getContext() {
        return sContext;
    }

    public static String getLOGTAG(Class aclass) {
        return LOGTAG + aclass.getSimpleName() + " ";
    }


}
