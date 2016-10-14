package com.byteshaft.sendsms.utils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class Helpers {

    public static void createFolderInExternalStorage() {
        File file = new File(AppGlobals.sPath + File.separator + AppGlobals.sFolderName);
        if (!file.exists()) {
            file.mkdirs();
        }
    }

    public static String getAppFolder() {
        return AppGlobals.sPath + File.separator + AppGlobals.sFolderName;
    }

    public static void createFileInsideAppFolder() {
        File root = new File(AppGlobals.sPath + File.separator +
                AppGlobals.sFolderName);
        File textFile = new File(root, AppGlobals.sConfigFileName);
        FileWriter writer;
        try {
            writer = new FileWriter(textFile);
            writer.append("Configuration File created");
            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
