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
package com.baker.abaker;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.graphics.Color;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.baker.abaker.client.GindMandator;
import com.baker.abaker.model.BookJson;
import com.baker.abaker.model.Magazine;
import com.baker.abaker.settings.Configuration;
import com.baker.abaker.settings.SettingsActivity;
import com.baker.abaker.views.FlowLayout;
import com.baker.abaker.views.MagazineThumb;
import com.baker.abaker.workers.BookJsonParserTask;
import com.baker.abaker.workers.CheckInternetTask;
import com.baker.abaker.workers.DownloaderTask;
import com.baker.abaker.workers.GCMRegistrationWorker;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.gcm.GoogleCloudMessaging;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Locale;

public class GindActivity extends Activity implements GindMandator {

    public final static String BOOK_JSON_KEY = "com.giniem.gindpubs.BOOK_JSON_KEY";
    public final static String MAGAZINE_NAME = "com.giniem.gindpubs.MAGAZINE_NAME";
    public final static String MAGAZINE_STANDALONE = "com.giniem.gindpubs.STANDALONE";
    public final static String MAGAZINE_RETURN_TO_SHELF = "com.giniem.gindpubs.MAGAZINE_RETURN_TO_SHELF";
    public static final String PROPERTY_REG_ID = "com.giniem.gindpubs.REGISTRATION_ID";
    public static final String DOWNLOAD_IN_PROGRESS = "com.giniem.gindpubs.DOWNLOAD_ID";
    private static final String PROPERTY_APP_VERSION = "com.giniem.gindpubs.APP_VERSION";

    /**
     * Code used when the MagazineActivity finishes, so we can evaluate if we need to close this
     * activity as well or not.
     */
    public static final int STANDALONE_MAGAZINE_ACTIVITY_FINISH = 1;

    //Shelf file download properties
    private final String shelfFileName = "shelf.json";
    private final String shelfFileTitle = "Shelf Information";
    private final String shelfFileDescription = "JSON Encoded file with the magazines information";
    private final int shelfFileVisibility = DownloadManager.Request.VISIBILITY_HIDDEN;

    private FlowLayout flowLayout;

    //Task to be done by this activity
    private final int DOWNLOAD_SHELF_FILE = 0;
    private final int REGISTRATION_TASK = 1;
    private final int CHECK_INTERNET_TASK = 2;
    private final int BOOK_JSON_PARSE_TASK = 3;

    // For Google Cloud Messaging
    private GoogleCloudMessaging gcm;
    private String registrationId;

    // Used to auto-start download of last content if a notification is received.
    private String startDownload = "";

    // For parsing dates
    SimpleDateFormat sdfInput;
    SimpleDateFormat sdfOutput;

    // Used in device registration process.
    public static String userAccount = "";

    private boolean isLoading = true;

    /**
     * Used when running in standalone mode based on the run_as_standalone setting in booleans.xml.
     */
    private boolean STANDALONE_MODE = false;
    /**
     * When viewing an issue, if this is true the app will return to the shelf after closing it.
     * If running in standalone mode and only one issue is present, this will be set to false, causing the app to finish.
     */
    private boolean RETURN_TO_SHELF = true;

    private GLSurfaceView mGLView;

    static {
        try {
            System.loadLibrary("JavaScriptCore");
            System.loadLibrary("ejecta");
        } catch (UnsatisfiedLinkError ex) {
            Log.e(GindActivity.class.getName(), "Could not load libraries: " + ex.getMessage());
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        this.getActionBar().hide();

        sdfInput = new SimpleDateFormat(
                getString(R.string.inputDateFormat), Locale.US);
        sdfOutput = new SimpleDateFormat(
                getString(R.string.outputDateFormat), Locale.US);

        Intent intent = this.getIntent();
        if (intent.hasExtra("START_DOWNLOAD")) {
            this.startDownload = intent.getStringExtra("START_DOWNLOAD");
        }

        try {
            loadingScreen();
            STANDALONE_MODE = getResources().getBoolean(R.bool.run_as_standalone);
            //Getting the user main account
            AccountManager manager = AccountManager.get(this);
            Account[] accounts = manager.getAccountsByType("com.google");

            // If we can't get a google account, then we will have to use
            // any account the user have on the phone.
            if (accounts.length == 0) {
                accounts = manager.getAccounts();
            }

            if (accounts.length != 0) {
                // We will use the first account on the list.
                userAccount = accounts[0].type + "_" + accounts[0].name;
            } else {
                // Wow, if we still do not have any working account
                // then we will have to use the ANDROID_ID,
                // Read: http://developer.android.com/reference/android/provider/Settings.Secure.html#ANDROID_ID
                Log.e(this.getClass().toString(), "USER ACCOUNT COULD NOT BE RETRIEVED, WILL USE ANDROID_ID.");
                userAccount = Settings.Secure.getString(this.getContentResolver(), Settings.Secure.ANDROID_ID);
            }
            Log.d(this.getClass().getName(), "APP_ID: " + this.getString(R.string.app_id) + ", USER_ID: " + userAccount);

            if (checkPlayServices()) {
                Log.d(this.getClass().getName(), "Google Play Services enabled.");
                gcm = GoogleCloudMessaging.getInstance(this);
                registrationId = getRegistrationId(this.getApplicationContext());

                Log.d(this.getClass().getName(), "Obtained registration ID: " + registrationId);

                registerInBackground();
            } else {
                Log.e(this.getClass().toString(), "No valid Google Play Services APK found.");
            }
            if (STANDALONE_MODE) {
                activateStandalone();
            } else {
                CheckInternetTask checkInternetTask = new CheckInternetTask(this, this, CHECK_INTERNET_TASK);
                checkInternetTask.execute();
            }

            // Here we register the APP OPEN event on Google Analytics
            if (getResources().getBoolean(R.bool.ga_enable) && getResources().getBoolean(R.bool.ga_register_app_open_event)) {
                ((ABakerApp)this.getApplication()).sendEvent(
                        getString(R.string.application_category),
                        getString(R.string.application_open),
                        getString(R.string.application_open_label));
            }

        } catch (Exception e) {
            e.printStackTrace();
            Log.e(this.getClass().getName(), "Cannot load configuration.");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkPlayServices();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.gind, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final int itemId = item.getItemId();

        if (itemId == R.id.action_info) {
            Intent intent = new Intent(this, InfoActivity.class);
            intent.putExtra(MagazineActivity.MODAL_URL, getString(R.string.infoUrl));
            startActivity(intent);
            return true;
        } else if (itemId == R.id.action_settings) {
            Intent settingsIntent = new Intent(this, SettingsActivity.class);
            startActivity(settingsIntent);
            return true;
        } else {
            return super.onContextItemSelected(item);
        }
    }

    private void activateStandalone() {
        ArrayList<String> issues = this.getValidIssuesAssets();
        Log.d(this.getClass().getName(), "Found " + issues.size() + " issues.");
        if (issues.size() == 1) {
            RETURN_TO_SHELF = false;
            Magazine magazine = new Magazine();
            magazine.setName(issues.get(0));
            magazine.setStandalone(STANDALONE_MODE);
            BookJsonParserTask parser = new BookJsonParserTask(
                    this,
                    magazine,
                    this,
                    BOOK_JSON_PARSE_TASK);
            parser.execute("STANDALONE");
        } else if (issues.size() > 1) {
            JSONArray jsonArray = new JSONArray();
            for (String issue : issues) {
                Log.d(this.getClass().getName(), "The file is: " + issue);
                jsonArray.put(this.getIssueData(issue));
            }
            this.createThumbnails(jsonArray);
        } else {
            Log.e(this.getClass().getName(), "Running standalone but no issues were found.");
            Toast.makeText(this, getString(R.string.sa_no_issues), Toast.LENGTH_LONG).show();
            this.finish();
        }
    }

    private JSONObject getIssueData(final String issueName) {
        final String books = getString(R.string.sa_books_directory);
        final String bookJson = "book.json";
        JSONObject result = new JSONObject();

        BufferedReader reader = null;
        try {
            result.put("name", issueName);

            AssetManager assetManager = getAssets();

            String bookJsonPath = books.concat(File.separator)
                    .concat(issueName).concat(File.separator)
                    .concat(bookJson);
            reader = new BufferedReader(new InputStreamReader(assetManager.open(bookJsonPath)));

            String line = "";
            StringBuilder jsonString = new StringBuilder();
            do {
                jsonString.append(line);
                line = reader.readLine();
            } while (line != null);

            Log.d(this.getClass().getName(), "The book.json read was: " + jsonString.toString());
            JSONObject jsonRaw = new JSONObject(jsonString.toString());
            result.put("title", jsonRaw.getString("title"));
            result.put("url", jsonRaw.getString("url"));
            result.put("info", jsonRaw.getString("title"));
            result.put("cover", jsonRaw.getString("cover"));
            result.put("date", jsonRaw.getString("date"));

        } catch (JSONException ex) {
            Log.e(this.getClass().getName(), "Error getting issue information from " + issueName, ex);
        } catch (IOException ex) {
            Log.e(this.getClass().getName(), "Error getting issue information from " + issueName, ex);
        } finally {
            try {
                reader.close();
            } catch (Exception ex) {
                //
            }
        }

        return result;
    }

    private ArrayList<String> getValidIssuesAssets() {
        final String path = getString(R.string.sa_books_directory);
        ArrayList<String> issues = new ArrayList<String>();
        try {
            AssetManager assetManager = getAssets();
            String assetList[] = assetManager.list(path);
            String fileName;
            for (String asset : assetList) {
                fileName = path.concat(File.separator).concat(asset);
                if (assetManager.list(fileName).length > 0) {
                    if (this.hasBookJson(fileName)) {
                        Log.d(this.getClass().getName(), "Valid issue found: " + fileName);
                        issues.add(asset);
                    }
                }
            }
        } catch (Exception ex) {
            Log.e(this.getClass().getName(), "Error getting issues from assets", ex);
        }
        return issues;
    }

    private boolean hasBookJson(final String issuePath) {
        boolean result = false;
        final String bookJson = "book.json";

        AssetManager assetManager = getAssets();
        InputStream inputStream = null;
        try {
            inputStream = assetManager.open(issuePath.concat(File.separator).concat(bookJson));
            result = true;
        } catch (Exception ex) {
            result = false;
        } finally {
            try {
                inputStream.close();
            } catch (Exception e) {
                Log.e(this.getClass().getName(), "Error opening the book.json for " + issuePath);
            }
        }

        return result;
    }

    public void downloadShelf(final String internetAccess) {
        String cacheShelfPath = Configuration.getCacheDirectory(this) + File.separator + this.getString(R.string.shelf);
        File cachedShelf = new File(cacheShelfPath);
        File backup = new File(cacheShelfPath + ".backup");
        boolean useBackup = false;
        Log.d(this.getClass().toString(), "INTERNET ACCESS RAW: " + internetAccess);
        boolean hasInternetAccess = (internetAccess.equals("TRUE")) ? true : false;

        if (hasInternetAccess) {
            Log.d(this.getClass().toString(), "INTERNET ACCESS AVAILABLE, WILL TRY TO CREATE BACKUP SHELF.");
            // We make a copy of the shelf as backup in case the download fails.
            if (cachedShelf.exists()) {
                try {
                    String contents = Configuration.readFile(cachedShelf);

                    if (cachedShelf.length() == 0L && contents.trim().isEmpty()) {
                        Log.d(this.getClass().toString(), "Cached shelf.json file is empty.");
                        cachedShelf.delete();

                        if (backup.exists()) {
                            Log.d(this.getClass().toString(), "shelf.json backup file found, the system will use it.");
                            Configuration.copyFile(backup, cachedShelf);
                            useBackup = true;
                        }
                    } else {

                        if (backup.exists()) {
                            backup.delete();
                        }

                        Log.d(this.getClass().toString(), "Creating backup for the shelf.json file.");
                        Configuration.copyFile(cachedShelf, new File(cacheShelfPath + ".backup"));
                    }
                } catch (IOException ioe) {
                    Toast.makeText(this, "Cannot download the magazine shelf.",
                            Toast.LENGTH_LONG).show();
                    this.finish();
                }
            }
        } else {
            Log.d(this.getClass().toString(), "NO INTERNET ACCESS, WON'T CREATE BACKUP SHELF.");
        }

        if (hasInternetAccess && !useBackup) {
            // We get the shelf json asynchronously.
            DownloaderTask downloadShelf = new DownloaderTask(
                    this.getApplicationContext(),
                    this,
                    this.DOWNLOAD_SHELF_FILE,
                    getString(R.string.newstand_manifest_url),
                    this.shelfFileName,
                    this.shelfFileTitle,
                    this.shelfFileDescription,
                    Configuration.getCacheDirectory(this),
                    this.shelfFileVisibility);
            downloadShelf.execute();
        } else if (cachedShelf.exists()) {
            this.readShelf(cacheShelfPath);

            if (backup.exists()) {
                Log.d(this.getClass().toString(), "Used shelf.json backup file, deleting backup file.");
                backup.delete();
                useBackup = false;
            }
        } else {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(this.getString(R.string.exit))
                    .setMessage(this.getString(R.string.no_shelf_no_internet))
                    .setPositiveButton(this.getString(android.R.string.ok), new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            GindActivity.this.finish();
                        }
                    })
                    .show();
        }
    }

    private String getRegistrationId(Context context) {
        final SharedPreferences prefs = getGCMPreferences(context);
        String regId = prefs.getString(PROPERTY_REG_ID, "");
        if (regId.isEmpty()) {
            Log.d(this.getClass().toString(), "Registration ID not found.");
            return "";
        }
        // Check if app was updated; if so, it must clear the registration ID
        // since the existing regID is not guaranteed to work with the new
        // app version.
        int registeredVersion = prefs.getInt(PROPERTY_APP_VERSION, Integer.MIN_VALUE);
        int currentVersion = getAppVersion(context);
        if (registeredVersion != currentVersion) {
            Log.d(this.getClass().toString(), "App version changed.");
            return "";
        }
        return regId;
    }

    /**
     * @return Application's version code from the {@code PackageManager}.
     */
    private static int getAppVersion(Context context) {
        try {
            PackageInfo packageInfo = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0);
            return packageInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            // should never happen
            throw new RuntimeException("Could not get package name: " + e);
        }
    }

    private void registerInBackground() {
        GCMRegistrationWorker registrationWorker = new GCMRegistrationWorker(this.getApplicationContext(),
                this.gcm, this.REGISTRATION_TASK, this);
        registrationWorker.execute();
    }

    /**
     * @return Application's {@code SharedPreferences}.
     */
    private SharedPreferences getGCMPreferences(Context context) {
        // This sample app persists the registration ID in shared preferences, but
        // how you store the regID in your app is up to you.
        return getSharedPreferences(GindActivity.class.getSimpleName(),
                Context.MODE_PRIVATE);
    }

    private void storeRegistrationId(Context context, String regId) {
        final SharedPreferences prefs = getGCMPreferences(context);
        int appVersion = getAppVersion(context);
        Log.i(this.getClass().toString(), "Saving regId on app version " + appVersion);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(PROPERTY_REG_ID, regId);
        editor.putInt(PROPERTY_APP_VERSION, appVersion);
        editor.commit();
    }

    private void loadBackground() {
        WebView webview = (WebView) findViewById(R.id.backgroundWebView);
        webview.getSettings().setJavaScriptEnabled(true);
        webview.setBackgroundColor(Color.TRANSPARENT);
        webview.setWebViewClient(new WebViewClient() {

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);
                return true;
            }
        });
        webview.loadUrl(getString(R.string.backgroundUrl));
    }

    private void loadingScreen() {
        setContentView(R.layout.loading);
        WebView webview = (WebView) findViewById(R.id.loadingWebView);
        webview.getSettings().setJavaScriptEnabled(true);
        webview.getSettings().setUseWideViewPort(true);
        webview.getSettings().setLoadWithOverviewMode(true);
        webview.setBackgroundColor(Color.TRANSPARENT);
        webview.setWebViewClient(new WebViewClient() {

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);
                return true;
            }
        });
        webview.loadUrl(getString(R.string.loadingUrl));
    }

    private void loadHeader() {
        WebView webview = (WebView) findViewById(R.id.headerView);
        webview.getSettings().setJavaScriptEnabled(true);
        webview.getSettings().setUseWideViewPort(true);
        //webview.getSettings().setLoadWithOverviewMode(true);
        webview.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                return true;
            }
        });
        webview.setBackgroundColor(Color.TRANSPARENT);
        webview.setWebChromeClient(new WebChromeClient());
        webview.loadUrl(getString(R.string.headerUrl));
    }

    public void createThumbnails(final JSONArray jsonArray) {
        Log.d(this.getClass().getName(),
                "Shelf json contains " + jsonArray.length() + " elements.");

        JSONObject json;
        try {
            this.setContentView(R.layout.activity_gind);
            loadHeader();
            loadBackground();
            this.getActionBar().show();

            // Modify the action bar to use a custom layout to center the title.
            this.getActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
            this.getActionBar().setCustomView(R.layout.custom_actionbar);

            flowLayout = (FlowLayout) findViewById(R.id.thumbsContainer);

            int length = jsonArray.length();

            for (int i = 0; i < length; i++) {
                json = new JSONObject(jsonArray.getString(i));
                Log.i(this.getClass().getName(), "Parsing JSON object " + json);

                LinearLayout inner = new LinearLayout(this);
                inner.setLayoutParams(new LinearLayout.LayoutParams(0,
                        LinearLayout.LayoutParams.MATCH_PARENT, 1));
                inner.setGravity(Gravity.CENTER_HORIZONTAL);

                //Building magazine data
                Date date = sdfInput.parse(json.getString("date"));
                String dateString = sdfOutput.format(date);
                int size = 0;
                if (json.has("size")) size = json.getInt("size");

                String encoding = "UTF-8";

                Magazine mag = new Magazine();
                mag.setName(new String(json.getString("name").getBytes(encoding), encoding));
                mag.setTitle(new String(json.getString("title").getBytes(encoding), encoding));
                mag.setInfo(new String(json.getString("info").getBytes(encoding), encoding));
                mag.setDate(dateString);
                mag.setSize(size);
                mag.setCover(new String(json.getString("cover").getBytes(encoding), encoding));
                mag.setUrl(new String(json.getString("url").getBytes(encoding), encoding));
                mag.setStandalone(STANDALONE_MODE);

                if (json.has("liveUrl")) {
                    String liveUrl = new String(json.getString("liveUrl").getBytes(encoding), encoding);
                    liveUrl = liveUrl.replace("/" + this.getString(R.string.book), "");
                    liveUrl = liveUrl.replace("/" + mag.getName(), "");
                    mag.setLiveUrl(liveUrl);
                }

                //Starting the ThumbLayout
                MagazineThumb thumb = new MagazineThumb(this, mag);
                thumb.init(this, null);
                if (this.magazineExists(mag.getName())) {
                    thumb.enableReadArchiveActions();
                } else if (STANDALONE_MODE) {
                    thumb.enableReadButton();
                }

                //Add layout
                flowLayout.addView(thumb);
            }

            isLoading = false;
        } catch (Exception e) {
            Log.e(this.getClass().getName(), "Error loading the shelf elements.", e);
            //TODO: Notify the user about the issue.
            e.printStackTrace();
        }
    }

    public void viewMagazine(final BookJson book) {
        Intent intent = new Intent(this, MagazineActivity.class);
        try {
            intent.putExtra(BOOK_JSON_KEY, book.toJSON().toString());
            intent.putExtra(MAGAZINE_NAME, book.getMagazineName());
            intent.putExtra(MAGAZINE_STANDALONE, STANDALONE_MODE);
            intent.putExtra(MAGAZINE_RETURN_TO_SHELF, RETURN_TO_SHELF);
            startActivityForResult(intent, STANDALONE_MAGAZINE_ACTIVITY_FINISH);
        } catch (JSONException e) {
            Toast.makeText(this, "The book.json is invalid.",
                    Toast.LENGTH_LONG).show();
        }
    }

    private boolean magazineExists(final String name) {
        boolean result = false;

        File magazineDir = new File(Configuration.getMagazinesDirectory(this) + File.separator + name);
        result = magazineDir.exists() && magazineDir.isDirectory();

        if (result) {
            result = (new File(magazineDir.getPath() + File.separator + this.getString(R.string.book)))
                    .exists();
        }

        return result;
    }

    private boolean magazineZipExists(final String name) {
        boolean result = false;

        File magazine = new File(Configuration.getMagazinesDirectory(this) + File.separator + name);
        result = magazine.exists() && !magazine.isDirectory();

        return result;
    }

    private void readShelf(final String path) {
        try {
            //Read the shelf file
            File input = new File(path);
            FileInputStream in = new FileInputStream(input);
            byte[] buffer = new byte[1024];
            StringBuffer rawData = new StringBuffer("");

            while (in.read(buffer) != -1) {
                rawData.append(new String(buffer));
            }
            in.close();

            //Parse the shelf file
            JSONArray json = new JSONArray(rawData.toString());

            //Create thumbs
            this.createThumbnails(json);
            if (this.startDownload.equals("latest")) {
                this.startDownloadLastContent(json);
            } else if (!this.startDownload.isEmpty()) {
                this.startDownloadByName(this.startDownload);
            }

            // We try to unzip any pending packages.
            this.unzipPendingPackages();
        } catch (Exception e) {
            Log.e(this.getClass().getName(), "Upss, we colapsed.. :( "
                    + e.getMessage());
            Toast.makeText(this, "Sorry, we could not read the shelf file :(",
                    Toast.LENGTH_LONG).show();
            this.finish();
        }
    }

    /**
     * Since the only file that is downloaded on this activity is the
     * shelf.json we don't need to show the user any progress right now.
     *
     * @param taskId
     * @param progress
     */
    public void updateProgress(final int taskId, Long... progress) {
    }

    ;

    /**
     * This will manage all the task post execute actions
     *
     * @param taskId the id of the task that concluded its work
     */
    public void postExecute(final int taskId, String... results) {
        switch (taskId) {
            //The download of the shelf file has concluded
            case DOWNLOAD_SHELF_FILE:
                //Get the results of the download
                String taskStatus = results[0];
                String filePath = results[1];

                if (taskStatus.equals("SUCCESS")) {
                    this.readShelf(filePath);
                } else if ("DIRECTORY_NOT_FOUND".equals(taskStatus) && "".equals(filePath)) {
                    Toast.makeText(this, "Please insert an SD card to use the app.",
                            Toast.LENGTH_LONG).show();
                    finish();
                } else {
                    Toast.makeText(this, "Cannot download the magazine shelf.",
                            Toast.LENGTH_LONG).show();
                    this.finish();
                }
                break;
            case REGISTRATION_TASK:
                if (results[0].equals("SUCCESS")) {
                    this.registrationId = results[1];
                    this.storeRegistrationId(this.getApplicationContext(), results[1]);
                } else {
                    // Could not create registration ID for GCM services.
                }
                break;
            case CHECK_INTERNET_TASK:
                Log.d(this.getClass().toString(), "FINISHED TESTING INTERNET CONNECTION, CHECKING SHELF...");
                this.downloadShelf(results[0]);
                break;
            case BOOK_JSON_PARSE_TASK:
                try {
                    BookJson bookJson = new BookJson();

                    final String magazineName = results[0];
                    final String rawJson = results[1];

                    bookJson.setMagazineName(magazineName);
                    bookJson.fromJson(rawJson);

                    this.viewMagazine(bookJson);
                } catch (JSONException ex) {
                    Log.e(this.getClass().getName(), "Error parsing the book.json", ex);
                } catch (ParseException ex) {
                    Log.e(this.getClass().getName(), "Error parsing the book.json", ex);
                }

                break;
        }
    }

    private boolean checkPlayServices() {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
//                GooglePlayServicesUtil.getErrorDialog(resultCode, this,
//                        PLAY_SERVICES_RESOLUTION_REQUEST).show();
            } else {
                Log.e(this.getClass().toString(), "This device does not support Google Play Services.");
                finish();
            }
            return false;
        }
        return true;
    }

    private void startDownloadLastContent(final JSONArray jsonArray) {
        try {
            ArrayList<Date> list = new ArrayList<Date>();
            JSONObject jsonObject;
            Date date;

            for (int i = 0; i < jsonArray.length(); i++) {
                jsonObject = new JSONObject(jsonArray.getString(i));

                String rawDate = jsonObject.getString("date");
                date = sdfInput.parse(rawDate);

                list.add(date);
            }

            Collections.sort(list, new Comparator<Date>() {

                @Override
                public int compare(Date s, Date s2) {
                    return s2.compareTo(s);
                }
            });

            for (int i = 0; i < flowLayout.getChildCount(); i++) {
                MagazineThumb thumb = (MagazineThumb) flowLayout.getChildAt(i);

                String dateString = sdfOutput.format(list.get(0));

                if (thumb.getMagazine().getDate().equals(dateString)) {
                    if (!this.magazineExists(thumb.getMagazine().getName())) {
                        Log.d(this.getClass().toString(), "Automatically starting download of " + thumb.getMagazine().getName());
                        thumb.startPackageDownload();
                    } else {
                        Log.d(this.getClass().toString(), "The magazine with name '" + thumb.getMagazine().getName() + "' already exists.");
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void startDownloadByName(final String name) {
        for (int i = 0; i < flowLayout.getChildCount(); i++) {
            MagazineThumb thumb = (MagazineThumb) flowLayout.getChildAt(i);

            if (thumb.getMagazine().getName().equals(name)) {
                Log.d("Automatically starting download of ", thumb.getMagazine().getName());
                thumb.startPackageDownload();
            }
        }
    }

    private void unzipPendingPackages() {
        for (int i = 0; i < flowLayout.getChildCount(); i++) {
            MagazineThumb thumb = (MagazineThumb) flowLayout.getChildAt(i);
            String zipName = thumb.getMagazine().getName().concat(this.getString(R.string.package_extension));

            if (this.magazineZipExists(zipName) && !thumb.isDownloading()) {
                Log.d(this.getClass().toString(), "Continue unzip of " + thumb.getMagazine().getName());
                String filepath = Configuration.getMagazinesDirectory(this.getApplicationContext()) + File.separator + zipName;

                thumb.startUnzip(filepath, thumb.getMagazine().getName());
            } else if (thumb.isDownloading()) {

                Log.d(this.getClass().getName(), "Continue download of: " + thumb.getMagazine().getName());

                // We continue the download.
                thumb.startPackageDownload();
            }
        }
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onBackPressed() {
        if (!this.isLoading) {
            boolean downloading = false;
            final ArrayList<Integer> downloadingThumbs = new ArrayList<Integer>();
            for (int i = 0; i < flowLayout.getChildCount(); i++) {
                MagazineThumb thumb = (MagazineThumb) flowLayout.getChildAt(i);
                if (thumb.isDownloading()) {
                    downloadingThumbs.add(i);
                    downloading = true;
                }
            }

            if (downloading) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder
                        .setTitle(this.getString(R.string.exit))
                        .setMessage(this.getString(R.string.closing_app))
                        .setPositiveButton(this.getString(R.string.yes), new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {

                                GindActivity.this.terminateDownloads(downloadingThumbs);
                                GindActivity.super.onBackPressed();
                            }
                        })
                        .setNegativeButton(this.getString(R.string.no), null)
                        .show();
            } else {
                super.onBackPressed();
            }
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(this.getClass().getName(), "MagazineActivity finished, resultCode: " + resultCode);
        if (resultCode == STANDALONE_MAGAZINE_ACTIVITY_FINISH) {
            this.finish();
        }
    }

    private void terminateDownloads(final ArrayList<Integer> downloadingThumbs) {
        for (Integer id : downloadingThumbs) {
            MagazineThumb thumb = (MagazineThumb) flowLayout.getChildAt(id);
            thumb.getPackDownloader().cancelDownload();
        }
    }
}