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
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
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
import android.widget.ProgressBar;
import android.widget.TextView;
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
import com.baker.abaker.workers.UnzipperTask;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.vending.expansion.downloader.DownloadProgressInfo;
import com.google.android.vending.expansion.downloader.DownloaderClientMarshaller;
import com.google.android.vending.expansion.downloader.DownloaderServiceMarshaller;
import com.google.android.vending.expansion.downloader.IDownloaderClient;
import com.google.android.vending.expansion.downloader.IDownloaderService;
import com.google.android.vending.expansion.downloader.IStub;

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
import java.util.List;
import java.util.Locale;

public class GindActivity extends Activity implements GindMandator, IDownloaderClient {

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
    private final int UNZIP_CUSTOM_STANDALONE = 4;

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
    private boolean expansionFileDownloadFinished = false;

    private IStub downloaderStub;
    private IDownloaderService downloaderInterface;
    private UnzipperTask standaloneUnzipper;
    private DownloaderTask shelfDownloader;
    private BookJsonParserTask bookJsonParserTask;

    private ArrayList<Integer> thumbnailIds = new ArrayList<>();

    /**
     * Used when running in standalone mode based on the run_as_standalone setting in booleans.xml.
     */
    private boolean STANDALONE_MODE = false;
    /**
     * When viewing an issue, if this is true the app will return to the shelf after closing it.
     * If running in standalone mode and only one issue is present, this will be set to false, causing the app to finish.
     */
    private boolean RETURN_TO_SHELF = true;

    private Handler thumbnailDownloaderHandler = new Handler() {

        @Override
        public void handleMessage(Message message) {
            if (message.what == 1) {
                GindActivity.this.downloadNextThumbnail();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        if (this.getActionBar() != null) {
            this.getActionBar().hide();
        }

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

            SharedPreferences sharedPreferences = this.getPreferences();
            if (sharedPreferences.getBoolean(Configuration.FIRST_TIME_RUN, true) && getResources().getBoolean(R.bool.ut_enable_tutorial)) {
                //the app is being launched for first time, do something
                Log.d(this.getClass().getName(), "First time app running, launching tutorial.");

                showAppUsage();

                sharedPreferences.edit().putBoolean(Configuration.FIRST_TIME_RUN, false).apply();
            }

            // Here we register the APP OPEN event on Google Analytics
            if (getResources().getBoolean(R.bool.ga_enable) && getResources().getBoolean(R.bool.ga_register_app_open_event)) {
                ((ABakerApp) this.getApplication()).sendEvent(
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
        if (null != downloaderStub) {
            downloaderStub.connect(this);
        }
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
        ArrayList<String> issues;
        boolean fromAssets = !this.getResources().getBoolean(R.bool.sa_read_from_custom_directory);
        if (fromAssets) {
            issues = this.getValidIssuesAssets();
            this.readStandaloneIssues(issues);
        } else {
            if (this.expansionFileExists()) {
                Log.d(this.getClass().getName(), "The expansion file exists.");
                File directory = new File(Configuration.getMagazinesDirectory(this));
                if (directory.exists() && (directory.list().length > 0)) {
                    if (this.getExtractionFinished()) {
                        Log.d(this.getClass().getName(), "Magazines directory not empty and extraction finished.");
                        this.getValidIssuesFromSharedStorage();
                    } else {
                        Log.d(this.getClass().getName(), "Magazines directory not empty but the extraction did not finished. Trying again.");
                        this.extractFromExpansionFile();
                    }
                } else {
                    Log.d(this.getClass().getName(), "No magazines detected on the magazines directory.");
                    this.saveExtractionFinished(false);
                    this.extractFromExpansionFile();
                }
            } else {
                Log.d(this.getClass().getName(), "The expansion file does not exist.");
                this.downloadExpansionFile();
            }
        }
    }

    private void readStandaloneIssues(final ArrayList<String> issues) {
        Log.d(this.getClass().getName(), "Found " + issues.size() + " issues.");
        if (issues.size() == 1) {
            RETURN_TO_SHELF = false;
            Magazine magazine = new Magazine();
            magazine.setName(issues.get(0));
            magazine.setStandalone(STANDALONE_MODE);
            boolean fromAssets = !this.getResources().getBoolean(R.bool.sa_read_from_custom_directory);
            bookJsonParserTask = new BookJsonParserTask(
                    this,
                    magazine,
                    this,
                    BOOK_JSON_PARSE_TASK);
            bookJsonParserTask.setFromAssets(fromAssets);
            bookJsonParserTask.execute("STANDALONE");
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
        String books;
        boolean fromAssets = !this.getResources().getBoolean(R.bool.sa_read_from_custom_directory);
        final String bookJson = "book.json";
        JSONObject result = new JSONObject();

        BufferedReader reader = null;
        try {
            result.put("name", issueName);

            AssetManager assetManager = getAssets();

            String bookJsonPath = issueName.concat(File.separator)
                    .concat(bookJson);

            if (fromAssets) {
                books = getString(R.string.sa_books_directory).concat(File.separator)
                        .concat(bookJsonPath);
                reader = new BufferedReader(new InputStreamReader(assetManager.open(books)));
            } else {
                books = Configuration.getMagazinesDirectory(this).concat(File.separator)
                        .concat(bookJsonPath);
                reader = new BufferedReader(new InputStreamReader(new FileInputStream(books)));
            }


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

    private void getValidIssuesFromSharedStorage() {
        ArrayList<String> issues = new ArrayList<String>();
        File directory = new File(Configuration.getMagazinesDirectory(this));
        String bookJson;

        if (directory.exists() && directory.isDirectory()) {
            for (String subdir : directory.list()) {
                Log.d(this.getClass().getName(), "Searching for book.json in subdirectory: " + subdir);
                bookJson = directory.getPath().concat(File.separator)
                        .concat(subdir).concat(File.separator).concat("book.json");
                if (new File(bookJson).exists()) {
                    Log.d(this.getClass().getName(), "Detected book.json file in " + subdir);
                    issues.add(new File(subdir).getPath());
                }
            }
        }

        if (issues.isEmpty()) {
            Log.d(this.getClass().getName(), "Issue list read is empty.");
        }

        this.hideDownloadingExtraFiles();
        this.readStandaloneIssues(issues);
    }

    private void extractFromExpansionFile() {
        boolean usingCustomCode = getResources().getBoolean(R.bool.sa_use_expansion_file_custom_version);
        int versionCode;

        if (usingCustomCode) {
            versionCode = getResources().getInteger(R.integer.sa_expansion_file_custom_version);
        } else {
            versionCode = Configuration.getApplicationVersionCode(this);
        }

        final String path = Configuration.getSharedStorageDirectory().concat(File.separator)
                .concat("Android").concat(File.separator)
                .concat("obb").concat(File.separator)
                .concat(getPackageName()).concat(File.separator);
        final String expansionFileNameNoExtension = "main."
                .concat(String.valueOf(versionCode)).concat(".")
                .concat(getPackageName());

        final String expansionFilePath = path + expansionFileNameNoExtension.concat(".obb");

        this.showExtractingExpansion();

        Log.d(this.getClass().getName(), "Initiating extraction of the expansion file...");
        standaloneUnzipper = new UnzipperTask(this, this, UNZIP_CUSTOM_STANDALONE);
        standaloneUnzipper.setAbsoluteOutputDirectory(true);
        standaloneUnzipper.setDeleteZipFile(false);
        standaloneUnzipper.execute(expansionFilePath, Configuration.getMagazinesDirectory(this));
    }

    private boolean expansionFileExists() {
        boolean usingCustomCode = getResources().getBoolean(R.bool.sa_use_expansion_file_custom_version);
        int versionCode;

        if (usingCustomCode) {
            versionCode = getResources().getInteger(R.integer.sa_expansion_file_custom_version);
        } else {
            versionCode = Configuration.getApplicationVersionCode(this);
        }

        final String path = Configuration.getSharedStorageDirectory().concat(File.separator)
                .concat("Android").concat(File.separator)
                .concat("obb").concat(File.separator)
                .concat(getPackageName()).concat(File.separator);
        final String expansionFileNameNoExtension = "main."
                .concat(String.valueOf(versionCode)).concat(".")
                .concat(getPackageName());

        final String expansionFilePath = path + expansionFileNameNoExtension.concat(".obb");

        return (new File(expansionFilePath).exists());
    }

    private void downloadExpansionFile() {
        try {
            Intent notifierIntent = new Intent(this, GindActivity.class);
            notifierIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            PendingIntent pendingIntent = PendingIntent
                    .getActivity(this, 0, notifierIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            int result = DownloaderClientMarshaller
                    .startDownloadServiceIfRequired(this, pendingIntent, ABakerDownloaderService.class);
            if (result != DownloaderClientMarshaller.NO_DOWNLOAD_REQUIRED) {
                Log.d(this.getClass().getName(), "Creating downloader stub.");
                downloaderStub = DownloaderClientMarshaller
                        .CreateStub(this, ABakerDownloaderService.class);
                this.showDownloadingExtraFiles();
            }
        } catch (PackageManager.NameNotFoundException ex) {
            Log.e(this.getClass().getName(), "Failed to download the expansion file: ", ex);
            finish();
        }
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
                if (null != inputStream) {
                    inputStream.close();
                }
            } catch (IOException e) {
                Log.e(this.getClass().getName(), "Error opening the book.json for " + issuePath);
            }
        }

        return result;
    }

    public void downloadNewShelf() {
        shelfDownloader = new DownloaderTask(
                this.getApplicationContext(),
                this,
                this.DOWNLOAD_SHELF_FILE,
                getString(R.string.newstand_manifest_url),
                this.shelfFileName,
                this.shelfFileTitle,
                this.shelfFileDescription,
                Configuration.getCacheDirectory(this),
                this.shelfFileVisibility);
        shelfDownloader.execute();
    }

    public void useBackupShelf() {
        try {
            String cacheShelfPath = Configuration.getCacheDirectory(this)
                    + File.separator
                    + this.getString(R.string.shelf);

            File cachedShelf = new File(cacheShelfPath);
            File backupShelf = new File(cacheShelfPath + ".backup");

            if (backupShelf.exists()) {
                Log.d(this.getClass().toString(), "Backed up shelf exists, the system will use it");
                Configuration.copyFile(backupShelf, cachedShelf);
                this.readShelf(cacheShelfPath);
            } else {
                Log.d(this.getClass().toString(), "The backed up shelf does not exists.");
                Toast.makeText(this, this.getString(R.string.could_not_read_shelf),
                        Toast.LENGTH_LONG).show();
                this.finish();
            }
        } catch (IOException ex) {
            Log.e(this.getClass().getName(), "Exception while trying to use the backup shelf.", ex);
            Toast.makeText(this, this.getString(R.string.could_not_read_shelf),
                    Toast.LENGTH_LONG).show();
            this.finish();
        }
    }

    public void backupCachedShelf() {
        try {
            String cacheShelfPath = Configuration.getCacheDirectory(this)
                    + File.separator
                    + this.getString(R.string.shelf);

            File cachedShelf = new File(cacheShelfPath);
            final String contents = Configuration.readFile(cachedShelf);

            Log.d(this.getClass().toString(), "Checking if the shelf is not empty.");
            final boolean emptyShelf = (cachedShelf.length() == 0L && contents.trim().isEmpty());

            if (emptyShelf) {
                Log.d(this.getClass().toString(), "Shelf is empty, we will delete it.");
                cachedShelf.delete();
            } else {
                Log.d(this.getClass().toString(), "Will create a backup for the shelf.");
                Configuration.copyFile(cachedShelf, new File(cacheShelfPath + ".backup"));
            }
        } catch (IOException ex) {
            Toast.makeText(this, this.getString(R.string.could_not_download_shelf),
                    Toast.LENGTH_LONG).show();
            this.finish();
        }
    }

    public void downloadShelf(final String internetAccess) {
        String cacheShelfPath = Configuration.getCacheDirectory(this) + File.separator + this.getString(R.string.shelf);

        File cachedShelf = new File(cacheShelfPath);
        boolean cachedShelfExists = cachedShelf.exists();
        boolean hasInternetAccess = internetAccess.equals("TRUE");

        if (hasInternetAccess && cachedShelfExists) {
            Log.d(this.getClass().toString(), "Internet access available and shelf exists.");

            this.backupCachedShelf();

            // After creating the backup file, because we have an internet connection,
            // we proceed to try to download the new shelf. If it fails, we will try to use
            // the backed up shelf.
            this.downloadNewShelf();
        } else if (!hasInternetAccess && cachedShelfExists) {
            Log.d(this.getClass().toString(), "No Internet access but shelf exists.");

            // At this point we do not have internet access, so we just backup the existing
            // shelf and try to use that one so we can operate later on the original file.
            this.backupCachedShelf();
            this.useBackupShelf();
        } else if (hasInternetAccess && !cachedShelfExists) {
            Log.d(this.getClass().toString(), "Internet access available but shelf does not exist.");

            this.downloadNewShelf();
        } else {
            Log.d(this.getClass().toString(), "No Internet access and the shelf does not exist.");

            this.useBackupShelf();
        }
    }

    private String getRegistrationId(Context context) {
        final SharedPreferences prefs = getPreferences();
        String regId = prefs.getString(Configuration.PROPERTY_REG_ID, "");
        if (regId.isEmpty()) {
            Log.d(this.getClass().toString(), "Registration ID not found.");
            return "";
        }
        // Check if app was updated; if so, it must clear the registration ID
        // since the existing regID is not guaranteed to work with the new
        // app version.
        int registeredVersion = prefs.getInt(Configuration.PROPERTY_APP_VERSION, Integer.MIN_VALUE);
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
    private SharedPreferences getPreferences() {
        // This sample app persists the registration ID in shared preferences, but
        // how you store the regID in your app is up to you.
        return getSharedPreferences(GindActivity.class.getSimpleName(),
                Context.MODE_PRIVATE);
    }

    private void storeRegistrationId(Context context, String regId) {
        final SharedPreferences prefs = getPreferences();
        int appVersion = getAppVersion(context);
        Log.i(this.getClass().toString(), "Saving regId on app version " + appVersion);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(Configuration.PROPERTY_REG_ID, regId);
        editor.putInt(Configuration.PROPERTY_APP_VERSION, appVersion);
        editor.apply();
    }

    private void saveExtractionFinished(boolean state) {
        final SharedPreferences preferences = this.getPreferences();
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(Configuration.EXTRACTION_FINISHED, state);
        editor.apply();
    }

    private boolean getExtractionFinished() {
        SharedPreferences preferences = this.getPreferences();
        return preferences.getBoolean(Configuration.EXTRACTION_FINISHED, false);
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
        Log.d(this.getClass().getName(), "Showing loading screen.");
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

    private void downloadNextThumbnail() {
        if (!this.thumbnailIds.isEmpty() && this.thumbnailDownloaderHandler != null) {
            int id = this.thumbnailIds.get(0);
            MagazineThumb thumb = (MagazineThumb) findViewById(id);
            thumb.downloadCover();
            this.thumbnailIds.remove(0);
        }
    }

    public void createThumbnails(final JSONArray jsonArray) {
        Log.d(this.getClass().getName(),
                "Shelf json contains " + jsonArray.length() + " elements.");

        JSONObject json;
        try {
            this.setContentView(R.layout.activity_gind);
            loadHeader();
            loadBackground();
            if (this.getActionBar() != null) {
                this.getActionBar().show();

                // Modify the action bar to use a custom layout to center the title.
                this.getActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
                this.getActionBar().setCustomView(R.layout.custom_actionbar);
            }

            flowLayout = (FlowLayout) findViewById(R.id.thumbsContainer);

            int length = jsonArray.length();

            for (int i = 0; i < length; i++) {
                json = new JSONObject(jsonArray.getString(i));
                Log.i(this.getClass().getName(), "Parsing JSON object " + json);

                LinearLayout inner = new LinearLayout(this);
                inner.setLayoutParams(new LinearLayout.LayoutParams(0,
                        LinearLayout.LayoutParams.MATCH_PARENT, 1));
                inner.setGravity(Gravity.CENTER_HORIZONTAL);

                // Building magazine data
                Date date = sdfInput.parse(json.getString("date"));
                String dateString = sdfOutput.format(date);
                int size = 0;
                if (json.has("size")) size = json.getInt("size");

                String encoding = "UTF-8";

                Magazine magazine = new Magazine();
                magazine.setName(new String(json.getString("name").getBytes(encoding), encoding));
                magazine.setTitle(new String(json.getString("title").getBytes(encoding), encoding));
                magazine.setInfo(new String(json.getString("info").getBytes(encoding), encoding));
                magazine.setDate(dateString);
                magazine.setSize(size);
                magazine.setCover(new String(json.getString("cover").getBytes(encoding), encoding));
                magazine.setUrl(new String(json.getString("url").getBytes(encoding), encoding));
                magazine.setStandalone(STANDALONE_MODE);

                if (json.has("liveUrl")) {
                    String liveUrl = new String(json.getString("liveUrl").getBytes(encoding), encoding);
                    liveUrl = liveUrl.replace("/" + this.getString(R.string.book), "");

                    while (liveUrl.endsWith("/")) {
                        liveUrl = liveUrl.substring(0, liveUrl.length() - 1);
                    }

                    magazine.setLiveUrl(liveUrl);

                    Log.d(this.getClass().toString(), "The liveUrl for the magazine "
                            + magazine.getName() + " will be " + liveUrl);
                }

                // Starting the ThumbLayout
                MagazineThumb thumb = new MagazineThumb(this, magazine, thumbnailDownloaderHandler);
                thumb.setId(i + i);
                thumb.init(this, null);
                thumbnailIds.add(thumb.getId());

                if (this.magazineExists(magazine.getName())) {
                    thumb.enableReadArchiveActions();
                } else if (STANDALONE_MODE) {
                    thumb.enableReadButton();
                }

                // Add layout
                flowLayout.addView(thumb);
            }

            // Start downloading the thumbnails.
            this.downloadNextThumbnail();
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
            intent.putExtra(Configuration.BOOK_JSON_KEY, book.toJSON().toString());
            intent.putExtra(Configuration.MAGAZINE_NAME, book.getMagazineName());
            intent.putExtra(Configuration.MAGAZINE_STANDALONE, STANDALONE_MODE);
            intent.putExtra(Configuration.MAGAZINE_RETURN_TO_SHELF, RETURN_TO_SHELF);
            startActivityForResult(intent, STANDALONE_MAGAZINE_ACTIVITY_FINISH);
        } catch (JSONException e) {
            Toast.makeText(this, this.getString(R.string.invalid_book_json),
                    Toast.LENGTH_LONG).show();
        }
    }

    public void showAppUsage() {
        BookJson book = new BookJson();
        book.setMagazineName(this.getString(R.string.ut_directory));

        List<String> contents = new ArrayList<String>();

        String pages[] = this.getString(R.string.ut_pages).split(">");
        for (String page : pages) {
            page = page.trim();
            contents.add(page);
        }

        book.setContents(contents);
        book.setOrientation("portrait");

        Intent intent = new Intent(this, MagazineActivity.class);
        try {
            intent.putExtra(Configuration.BOOK_JSON_KEY, book.toJSON().toString());
            intent.putExtra(Configuration.MAGAZINE_NAME, book.getMagazineName());
            //intent.putExtra(Configuration.MAGAZINE_STANDALONE, true);
            intent.putExtra(Configuration.MAGAZINE_RETURN_TO_SHELF, true);
            intent.putExtra(Configuration.MAGAZINE_ENABLE_DOUBLE_TAP, false);
            intent.putExtra(Configuration.MAGAZINE_ENABLE_BACK_NEXT_BUTTONS, true);
            intent.putExtra(Configuration.MAGAZINE_ENABLE_TUTORIAL, true);
            startActivityForResult(intent, STANDALONE_MAGAZINE_ACTIVITY_FINISH);
        } catch (JSONException e) {
            // Nothing
        }
    }

    private boolean magazineExists(final String name) {
        boolean result;

        File magazineDir = new File(Configuration.getMagazinesDirectory(this) + File.separator + name);
        result = magazineDir.exists() && magazineDir.isDirectory();

        if (result) {
            result = (new File(magazineDir.getPath() + File.separator + this.getString(R.string.book)))
                    .exists();
        }

        return result;
    }

    private boolean magazineZipExists(final String name) {
        boolean result;

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
            Log.e(this.getClass().getName(), "Ups, we collapsed.. :( "
                    + e.getMessage());
            Toast.makeText(this, this.getString(R.string.could_not_read_shelf),
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

    /**
     * This will manage all the task post execute actions
     *
     * @param taskId the id of the task that concluded its work
     */
    public void postExecute(final int taskId, String... results) {
        switch (taskId) {
            //The download of the shelf file has concluded
            case DOWNLOAD_SHELF_FILE:
                Log.d(this.getClass().getName(), "Shelf downloader finished.");
                shelfDownloader = null;
                //Get the results of the download
                String taskStatus = results[0];
                String filePath = results[1];

                if (taskStatus.equals("SUCCESS")) {
                    // After the download is successful, we create a backup for the shelf.
                    this.backupCachedShelf();
                    this.readShelf(filePath);
                } else if ("DIRECTORY_NOT_FOUND".equals(taskStatus) && "".equals(filePath)) {
                    Toast.makeText(this, this.getString(R.string.could_not_save_shelf),
                            Toast.LENGTH_LONG).show();
                    finish();
                } else {
                    Log.d(this.getClass().toString(), "The shelf download failed, we will try to use a backup file.");
                    this.useBackupShelf();
                }
                break;
            case REGISTRATION_TASK:
                if (results[0].equals("SUCCESS")) {
                    this.registrationId = results[1];
                    this.storeRegistrationId(this.getApplicationContext(), results[1]);
                } else {
                    // Could not create registration ID for GCM services.
                    Log.d(this.getClass().toString(), "Could not create registration ID for GCM services");
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
            case UNZIP_CUSTOM_STANDALONE:
                this.saveExtractionFinished(true);
                this.getValidIssuesFromSharedStorage();
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
        if (null != downloaderStub) {
            downloaderStub.disconnect(this);
        }
        super.onStop();
    }

    @Override
    public void onDestroy() {
        Log.d(this.getClass().getName(), "onDestroy method invoked!");
        if (shelfDownloader != null && shelfDownloader.isDownloading()) {
            Log.d(this.getClass().getName(), "Shelf downloader is running, cancelling...");
            shelfDownloader.cancelDownload();
        }
        if (bookJsonParserTask != null && bookJsonParserTask.getStatus() != AsyncTask.Status.RUNNING) {
            Log.d(this.getClass().getName(), "BookJson parser is running, cancelling...");
            bookJsonParserTask.cancel(true);
        }
        shelfDownloader = null;
        bookJsonParserTask = null;
        thumbnailDownloaderHandler = null;
        super.onDestroy();
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

    private void showDownloadingExtraFiles() {
        findViewById(R.id.downloadExtraFiles).setVisibility(View.VISIBLE);
    }

    private void hideDownloadingExtraFiles() {
        findViewById(R.id.downloadExtraFiles).setVisibility(View.GONE);
    }

    private void showExtractingExpansion() {
        ((TextView) findViewById(R.id.expansionTextView)).setText(getString(R.string.sa_extracting_expansion));
        findViewById(R.id.downloadExtraFiles).setVisibility(View.VISIBLE);
        ((ProgressBar) findViewById(R.id.expansionProgressBar)).setProgress(100);

    }

    @Override
    public void onServiceConnected(Messenger m) {
        Log.d(this.getClass().getName(), "Downloader service connected.");
        downloaderInterface = DownloaderServiceMarshaller.CreateProxy(m);
        downloaderInterface.onClientUpdated(downloaderStub.getMessenger());
    }

    @Override
    public void onDownloadStateChanged(int newState) {
        Log.d(this.getClass().getName(), "Expansion file download state changed: " + newState);
        switch (newState) {
            case IDownloaderClient.STATE_PAUSED_ROAMING:
                this.showToast(getString(R.string.sa_download_paused_roaming));
                break;
            case IDownloaderClient.STATE_PAUSED_NEED_WIFI:
                this.showToast(getString(R.string.sa_download_paused_wifi_unavailable));
                break;
            case IDownloaderClient.STATE_PAUSED_NEED_CELLULAR_PERMISSION:
                this.showToast(getString(R.string.sa_download_paused_wifi_unavailable));
                break;
            case IDownloaderClient.STATE_FAILED:
                this.showToast(getString(R.string.sa_download_failed));
                break;
            case IDownloaderClient.STATE_COMPLETED:
                if (!expansionFileDownloadFinished) {
                    expansionFileDownloadFinished = true;
                    this.extractFromExpansionFile();
                }
                break;
            default:
                break;
        }
    }

    @Override
    public void onDownloadProgress(DownloadProgressInfo progress) {
        int percent = ((int)(progress.mOverallProgress * 100 / progress.mOverallTotal));
        Log.d(this.getClass().getName(), "Downloading expansion file, progress: " + percent + " %");
        this.updateDownloadExpansionProgressUI(percent);
    }

    private void updateDownloadExpansionProgressUI(int percent) {
        // Update UI
        TextView expansionTextView = ((TextView) findViewById(R.id.expansionTextView));
        String text = getString(R.string.sa_downloading_expansion);
        expansionTextView.setText(text + " " + percent + " %");
        ((ProgressBar) findViewById(R.id.expansionProgressBar)).setProgress(percent);
    }

    private void showToast(final String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }
}