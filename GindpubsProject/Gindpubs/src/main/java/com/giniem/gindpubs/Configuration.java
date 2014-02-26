package com.giniem.gindpubs;

import android.content.Context;
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

    private static final String LOG_TITLE = ">>>CONFIGURATION";

    /**
     * Sets the name of the cache folder to be used by the application.
     */
    public static final String MAGAZINES_FILES_DIR = "magazines";

    /**
     * Empty constructor not to be used since the class is utils only.
     */
	private Configuration() {}

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
        String[] pairs = query.split("&");
        for (String pair : pairs) {
            int idx = pair.indexOf("=");
            query_pairs.put(URLDecoder.decode(pair.substring(0, idx), "UTF-8"), URLDecoder.decode(pair.substring(idx + 1), "UTF-8"));
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
            return false;
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
            is.close();
            os.close();
        }
    }

    public static String readFile(File file) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(file));
        String line = null;
        StringBuilder stringBuilder = new StringBuilder();
        String ls = System.getProperty("line.separator");

        while((line = reader.readLine()) != null) {
            stringBuilder.append(line);
            stringBuilder.append(ls);
        }

        return stringBuilder.toString();
    }
}
