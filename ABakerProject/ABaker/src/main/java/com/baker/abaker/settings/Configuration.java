/**
 * Copyright (c) 2013-2014. Francisco Contreras, Holland Salazar.
 *
 * Redistribution and use in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of
 * conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of
 * conditions and the following disclaimer in the documentation and/or other materials
 * provided with the distribution.
 *
 * 3. Neither the name of the Baker Framework nor the names of its contributors may be used to
 * endorse or promote products derived from this software without specific prior written
 * permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT
 * SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 **/
package com.baker.abaker.settings;

import android.content.Context;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Environment;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.LinkedHashMap;
import java.util.Map;

public class Configuration {

    public final static String BOOK_JSON_KEY = "com.baker.abaker.BOOK_JSON_KEY";

    public final static String MAGAZINE_NAME = "com.baker.abaker.MAGAZINE_NAME";
    public final static String MAGAZINE_STANDALONE = "com.baker.abaker.STANDALONE";
    public final static String MAGAZINE_RETURN_TO_SHELF = "com.baker.abaker.MAGAZINE_RETURN_TO_SHELF";
    public final static String MAGAZINE_ENABLE_BACK_NEXT_BUTTONS = "com.baker.abaker.MAGAZINE_ENABLE_BACK_NEXT_BUTTONS";
    public final static String MAGAZINE_ENABLE_DOUBLE_TAP = "com.baker.abaker.MAGAZINE_ENABLE_DOUBLE_TAP";
    public final static String MAGAZINE_ENABLE_TUTORIAL = "com.baker.abaker.MAGAZINE_ENABLE_TUTORIAL";

    public final static String PROPERTY_REG_ID = "com.baker.abaker.REGISTRATION_ID";
    public final static String DOWNLOAD_IN_PROGRESS = "com.baker.abaker.DOWNLOAD_ID";
    public final static String PROPERTY_APP_VERSION = "com.baker.abaker.APP_VERSION";
    public final static String EXTRACTION_FINISHED = "com.baker.abaker.EXTRACTION_FINISHED";

    public final static String FIRST_TIME_RUN = "com.baker.abaker.FIRST_TIME_RUN";

    private static final String LOG_TITLE = ">>>CONFIGURATION";

    /**
     * Sets the name of the cache folder to be used by the application.
     */
    public static final String MAGAZINES_FILES_DIR = "magazines";

    /**
     * Key of the setting to receive notification.
     */
    public static final String PREF_RECEIVE_NOTIFICATIONS = "pref_receive_notifications";

    /**
     * Key of the setting to receive automatic downloads notifications.
     */
    public static final String PREF_RECEIVE_NOTIFICATIONS_DOWNLOAD = "pref_receive_notifications_download";

    /**
     * Key of the setting to download content only on Wi-Fi.
     */
    public static final String PREF_RECEIVE_NOTIFICATIONS_DOWNLOAD_ONLY_WIFI = "pref_receive_notifications_download_only_wifi";

    /**
     * Key of the setting to enable Google Analytics.
     */
    public static final String PREF_ENABLE_ANALYTICS = "pref_enable_analytics";

    /**
     * Empty constructor not to be used since the class is utils only.
     */
    private Configuration() {
    }

    // Tries to use external storage, if not available then fallback to internal.
    public static String getFilesDirectory(Context context) {
        String filesPath = "";

        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {

            Log.d(LOG_TITLE, "EXTERNAL STORAGE IS MOUNTED.");

            File externalFilesDirectory = context.getExternalFilesDir("");
            if (null != externalFilesDirectory) {
                Log.d(LOG_TITLE, "USING EXTERNAL STORAGE.");
                filesPath = externalFilesDirectory.getPath();
                Log.d(LOG_TITLE, "EXTERNAL PATH TO USE: " + filesPath);
            } else {
                Log.d(LOG_TITLE, "USING INTERNAL STORAGE.");
                filesPath = context.getFilesDir().getPath();
            }
        } else {
            Log.d(LOG_TITLE, "EXTERNAL STORAGE IS *NOT* MOUNTED.");
            filesPath = context.getFilesDir().getPath();
        }
        return filesPath;
    }

    public static String getMagazinesDirectory(Context context) {
        return getFilesDirectory(context) + File.separator + Configuration.MAGAZINES_FILES_DIR;
    }

    /**
     * Gets the absolute cache dir for accessing files.
     *
     * @param context
     * @return The absolute cache dir, either on external or internal storage.
     */
    public static String getCacheDirectory(Context context) {
        String cachePath = "";

        if (Environment.MEDIA_MOUNTED.equals(Environment
                .getExternalStorageState())) {

            Log.d(LOG_TITLE, "EXTERNAL STORAGE IS MOUNTED FOR CACHE.");

            File externalCacheDirectory = context.getExternalCacheDir();
            if (null != externalCacheDirectory) {
                Log.d(LOG_TITLE, "USING EXTERNAL STORAGE FOR CACHE.");
                cachePath = externalCacheDirectory.getPath();
                Log.d(LOG_TITLE, "EXTERNAL PATH TO USE FOR CACHE: " + cachePath);
            } else {
                Log.d(LOG_TITLE, "USING INTERNAL STORAGE FOR CACHE.");
                cachePath = context.getCacheDir().getPath();
            }
        } else {
            Log.d(LOG_TITLE, "EXTERNAL STORAGE IS *NOT* MOUNTED FOR CACHE.");
            cachePath = context.getCacheDir().getPath();
        }
        return cachePath;
    }

    public static String getSharedStorageDirectory() {
        String sharedDirectory;
        String state = Environment.getExternalStorageState();

        if (Environment.MEDIA_MOUNTED.equals(state) && !Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            File externalStorageDir = Environment.getExternalStorageDirectory();
            if (null != externalStorageDir) {
                sharedDirectory = externalStorageDir.getPath();
            } else {
                Log.d(LOG_TITLE, "EXTERNAL STORAGE IS *NOT* AVAILABLE.");
                sharedDirectory = "";
            }
        } else {
            Log.d(LOG_TITLE, "EXTERNAL STORAGE IS *NOT* AVAILABLE.");
            sharedDirectory = "";
        }

        return sharedDirectory;
    }

    public static int getApplicationVersionCode(Context context) {
        int versionCode;
        try {
            versionCode = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(LOG_TITLE, "The version code could not be obtained.");
            versionCode = -1;
        }
        return versionCode;
    }

    public static boolean connectionIsWiFi(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

        return networkInfo.isConnected();
    }

    public static boolean hasNetworkConnection(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        if (netInfo != null && netInfo.isConnected()) {
            return true;
        }
        return false;
    }

    public static Map<String, String> splitUrlQueryString(URL url) throws UnsupportedEncodingException {
        Map<String, String> query_pairs = new LinkedHashMap<String, String>();
        String query = url.getQuery();
        Log.d(LOG_TITLE, "URL QUERY RAW: " + query);
        String[] pairs = query.split("&");
        Log.d(LOG_TITLE, "URL QUERY PAIRS COUNT: " + pairs.length);
        if (pairs.length > 0) {
            for (String pair : pairs) {
                Log.d(LOG_TITLE, "SPLITTING URL QUERY PAIR " + pair);
                int idx = pair.indexOf("=");
                if (idx > -1) {
                    query_pairs.put(URLDecoder.decode(pair.substring(0, idx), "UTF-8"), URLDecoder.decode(pair.substring(idx + 1), "UTF-8"));
                }
            }
        }
        return query_pairs;
    }

    public static boolean deleteDirectory(final String path) {
        File directory = new File(path);

        if (directory.exists()) {
            File[] files = directory.listFiles();
            if (files == null) {
                return true;
            }

            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDirectory(file.getPath());
                } else {
                    file.delete();
                }
            }
        } else {
            return true;
        }

        return (directory.delete());
    }

    public static void copyFile(File source, File destination) throws IOException {
        InputStream is = null;
        OutputStream os = null;
        try {
            is = new FileInputStream(source);
            os = new FileOutputStream(destination);
            byte[] buffer = new byte[1024];
            int length;
            while ((length = is.read(buffer)) > 0) {
                os.write(buffer, 0, length);
            }
        } finally {
            if (is != null)
                is.close();
            if (os != null)
                os.close();
        }
    }

    public static String readFile(File file) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(file));
        String line;
        StringBuilder stringBuilder = new StringBuilder();
        String ls = System.getProperty("line.separator");

        while ((line = reader.readLine()) != null) {
            stringBuilder.append(line);
            stringBuilder.append(ls);
        }

        return stringBuilder.toString();
    }
}
