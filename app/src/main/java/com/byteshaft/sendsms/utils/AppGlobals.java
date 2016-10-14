package com.byteshaft.sendsms.utils;

import android.app.Application;
import android.content.Context;
import android.os.Environment;

import com.byteshaft.sendsms.R;

public class AppGlobals extends Application {

    private static Context sContext;
    private static final String LOGTAG = "SEND_SMS";
    public static String sPath = Environment.getExternalStorageDirectory().getAbsolutePath();
    public static String sFolderName;
    public static String sConfigFileName = "config.txt";

    @Override
    public void onCreate() {
        super.onCreate();
        sContext = getApplicationContext();
        sFolderName = getResources().getString(R.string.app_name);
    }

    public static Context getContext() {
        return sContext;
    }

    public static String getLOGTAG(Class aclass) {
        return LOGTAG + aclass.getSimpleName();
    }


}
