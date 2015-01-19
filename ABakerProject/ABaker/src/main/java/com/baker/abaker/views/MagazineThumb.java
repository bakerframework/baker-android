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
package com.baker.abaker.views;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.baker.abaker.ABakerApp;
import com.baker.abaker.settings.Configuration;
import com.baker.abaker.GindActivity;
import com.baker.abaker.R;
import com.baker.abaker.client.GindMandator;
import com.baker.abaker.client.PostClientTask;
import com.baker.abaker.model.BookJson;
import com.baker.abaker.model.Magazine;
import com.baker.abaker.workers.BookJsonParserTask;
import com.baker.abaker.workers.CheckInternetTask;
import com.baker.abaker.workers.DownloaderTask;
import com.baker.abaker.workers.MagazineDeleteTask;
import com.baker.abaker.workers.UnzipperTask;

import org.json.JSONException;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;


public class MagazineThumb extends LinearLayout implements GindMandator {

    /**
     * The path to the application files, in the internal memory or SD card.
     */
    private String magazinesDirectory;
    /**
     * The path to the application cache folder.
     */
    private String cachePath;
    /**
     * magazine represents the model of the object read from the list of issues json file
     * to render into the layout.
     * It is passed as param with building the layout so it should have all the
     * required values.
     */
    private Magazine magazine;
    /**
     * book is the model object read from the book.json file that comes into the hpub package.
     * It should have all required properties to load the issue.
     */
    private BookJson book;
    /**
     * packDownloader represents a tasks that should be used to download the issue package
     * when necessary.
     */
    private DownloaderTask packDownloader;
    /**
     * thumbDownloader is a download task to get the cover files of the issue.
     */
    private DownloaderTask thumbDownloader;
    /**
     * unzipper task should be used to unzip the hpub to be able to render it into a WebView.
     */
    private UnzipperTask unzipperTask;
    /**
     * Used to send the report after a magazine has downloaded and unzipped.
     */
    private PostClientTask postClientTask;
    /**
     * Issue readable state
     */
    private boolean readable = false;

    private final int THUMB_DOWNLOAD_VISIBILITY = DownloadManager.Request.VISIBILITY_HIDDEN;
    public final int THUMB_DOWNLOAD_TASK = 0;
    private final int MAGAZINE_DOWNLOAD_TASK = 1;
    private final int MAGAZINE_DOWNLOAD_VISIBILITY = DownloadManager.Request.VISIBILITY_VISIBLE;
    private final int UNZIP_MAGAZINE_TASK = 2;
    private final int MAGAZINE_DELETE_TASK = 3;
    private final int POST_DOWNLOAD_TASK = 4;
    private final int BOOK_JSON_PARSE_TASK = 5;
    private final int CHECK_INTERNET_TASK = 6;

    // This will be used to execute a task after checking internet.
    private enum TASKS_AFTER_CHECK_INTERNET{DOWNLOAD_ISSUE, READ_ONLINE}

    private TASKS_AFTER_CHECK_INTERNET TASK = TASKS_AFTER_CHECK_INTERNET.DOWNLOAD_ISSUE;

    private Activity activity;

    /**
     * Set to true when the user uses the Preview button rather than downloading the package.
     */
    private boolean previewLoaded = false;

    private Handler thumbnailDownloaderHandler;

    /**
     * Creates an instance of MagazineThumb to with an activity activity.
     *
     * @param _activity the parent Activity.
     */
    public MagazineThumb(Activity _activity, Magazine mag, Handler thumbnailDownloaderHandler) {
        super(_activity);

        this.activity = _activity;

        //Paths to the application files
        magazinesDirectory = Configuration.getMagazinesDirectory(_activity);
        cachePath = Configuration.getCacheDirectory(_activity);

        //Set the magazine model to the thumb instance.
        this.magazine = mag;

        // Handler to report to after the download of the thumbnail finishes.
        this.thumbnailDownloaderHandler = thumbnailDownloaderHandler;

        //Thumbnail downloader task initialization.
        thumbDownloader = new DownloaderTask(_activity,
                this,
                this.THUMB_DOWNLOAD_TASK,
                this.magazine.getCover(),
                this.magazine.getName(), //Set the same name as the magazine
                this.magazine.getTitle().concat(" cover"),
                "File to show on the shelf",
                cachePath,
                this.THUMB_DOWNLOAD_VISIBILITY);
        packDownloader = new DownloaderTask(this.activity,
                this,
                this.MAGAZINE_DOWNLOAD_TASK,
                this.magazine.getUrl(),
                this.magazine.getName() + _activity.getString(R.string.package_extension),
                this.magazine.getTitle(),
                this.magazine.getInfo(),
                this.magazinesDirectory,
                this.MAGAZINE_DOWNLOAD_VISIBILITY);

        //Unzipper task initialization
        unzipperTask = new UnzipperTask(_activity, this, UNZIP_MAGAZINE_TASK);

        //Logging initialization
        Log.d(this.getClass().getName(), "Magazines relative dir: " + magazinesDirectory);
    }

    public String getMagazinesDirectory() {
        return this.magazinesDirectory;
    }

    /**
     * Initialize the view
     *
     * @param context Application Context
     * @param attrs   Attributes to pass to the view.
     */
    public void init(final Context context, AttributeSet attrs) {
        setOrientation(LinearLayout.HORIZONTAL);

        //Creating the view from the XML
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.magazine_thumb_options, this, true);

        // Load the cover if the file exists.
        if ((new File(Configuration.getCacheDirectory(this.getContext())
                + File.separator + this.magazine.getName())).exists()) {
            this.renderCover();
        }

        //Set texts and values into the layout
        ((TextView) findViewById(R.id.txtTitle)).setText(this.magazine.getTitle());
        ((TextView) findViewById(R.id.txtInfo)).setText(this.magazine.getInfo());
        ((TextView) findViewById(R.id.txtDate)).setText(this.magazine.getDate());
        if (this.magazine.getSize() == 0) {
            //Hide the size if not set
            findViewById(R.id.txtSize).setVisibility(View.GONE);
        } else {
            //Calculating size in MB
            this.magazine.setSizeMB(this.magazine.getSize() / 1048576);
            ((TextView) findViewById(R.id.txtSize)).setText(this.magazine.getSizeMB() + " MB");
        }
        ((TextView) findViewById(R.id.txtProgress)).setText(
                "0 MB / " + this.magazine.getSizeMB() + " MB");

        //Click on the thumbs image
        findViewById(R.id.imgCover).setOnClickListener(new OnClickListener() {
            public void onClick(View v) {

                if (!readable && MagazineThumb.this.magazine.getLiveUrl() != null) {
                    MagazineThumb.this.checkInternetAccess();
                } else {
                    if (readable && !MagazineThumb.this.isDownloading()) {
                        readIssue();
                    } else if (!MagazineThumb.this.isDownloading()) {
                        TASK = TASKS_AFTER_CHECK_INTERNET.DOWNLOAD_ISSUE;
                        MagazineThumb.this.checkInternetAccess();
                    }
                }

            }
        });

        //Click on download button
        findViewById(R.id.btnDownload).setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                TASK = TASKS_AFTER_CHECK_INTERNET.DOWNLOAD_ISSUE;
                MagazineThumb.this.checkInternetAccess();
            }
        });

        // Click on the Read button.
        findViewById(R.id.btnRead).setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                readIssue();
            }
        });

        // Click on the ARCHIVE button.
        findViewById(R.id.btnArchive).setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                deleteIssue();
            }
        });

        // If there is a liveUrl key on the json we enable the functionality to read the
        // magazine online.
        if (this.magazine.getLiveUrl() != null) {

            // If the space is enable in the booleans.xml we use the spacer.
            if (this.getContext().getResources().getBoolean(R.bool.space_between_preview_and_download)) {
                findViewById(R.id.btnPreviewSpacer).setVisibility(View.VISIBLE);
            }
            findViewById(R.id.btnPreview).setVisibility(View.VISIBLE);
            findViewById(R.id.btnPreview).setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    TASK = TASKS_AFTER_CHECK_INTERNET.READ_ONLINE;
                    MagazineThumb.this.checkInternetAccess();
                }
            });
        }
    }

    private void checkInternetAccess() {
        CheckInternetTask checkInternetTask = new CheckInternetTask(MagazineThumb.this.getContext(),
                MagazineThumb.this,
                CHECK_INTERNET_TASK);
        checkInternetTask.execute();
    }

    /**
     * This method should return the package downloader task to be able to cancel it.
     *
     * @return DownloaderTask
     */
    public DownloaderTask getPackDownloader() {
        return this.packDownloader;
    }

    /**
     * Updates the UI to allow the user read/archive the issue.
     */
    public void enableReadArchiveActions() {
        findViewById(R.id.download_container).setVisibility(View.GONE);
        findViewById(R.id.txtProgress).setVisibility(View.GONE);
        ((TextView) findViewById(R.id.txtProgress)).setText(R.string.downloading);
        findViewById(R.id.progress_ui).setVisibility(View.GONE);
        findViewById(R.id.actions_ui).setVisibility(View.VISIBLE);

        //Issue becomes readable
        readable = true;
    }

    public void enableReadButton() {
        findViewById(R.id.download_container).setVisibility(View.GONE);
        findViewById(R.id.btnArchive).setVisibility(View.GONE);
        findViewById(R.id.actions_ui).setVisibility(View.VISIBLE);

        //Issue becomes readable
        readable = true;
    }

    public void setBookJson(BookJson bookJson) {
        this.book = bookJson;

        if (null != this.book) {
            GindActivity activity = (GindActivity) this.getContext();
            activity.viewMagazine(this.book);
        } else {
            Toast.makeText(this.getContext(), "The book.json was not found!",
                    Toast.LENGTH_LONG).show();
        }

        if (this.previewLoaded) {
            this.previewLoaded = false;
            findViewById(R.id.download_container).setVisibility(View.VISIBLE);
            findViewById(R.id.txtProgress).setVisibility(View.GONE);
        }

        // Here we register the OPEN ISSUE event on Google Analytics
        if (this.activity.getResources().getBoolean(R.bool.ga_enable) && this.activity.getResources().getBoolean(R.bool.ga_register_issue_read_event)) {
            ((ABakerApp)this.activity.getApplication()).sendEvent(
                    this.activity.getString(R.string.issues_category),
                    this.activity.getString(R.string.issue_open),
                    this.magazine.getName());
        }
    }

    public Magazine getMagazine() {
        return magazine;
    }

    /**
     * This should let other instances know if this Magazine Thumb instance is already
     * downloading a package.
     *
     * @return boolean true if downloading false, otherwise.
     */
    public boolean isDownloading() {
        boolean result = false;
        if (null != this.packDownloader) {
            result = this.packDownloader.isDownloading();
        }
        return result;
    }

    /**
     * Deletes and issue from the user device.
     */
    private void deleteIssue() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this.getContext());
        builder.setTitle(R.string.confirmation)
                .setMessage(R.string.archiveConfirm)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        //Setting text and showing progress text
                        ((TextView) findViewById(R.id.txtProgress)).setText(R.string.deleting);
                        findViewById(R.id.txtProgress).setVisibility(View.VISIBLE);

                        MagazineDeleteTask deleter = new MagazineDeleteTask(
                                MagazineThumb.this.getContext(), MagazineThumb.this, MAGAZINE_DELETE_TASK);
                        deleter.execute(magazine.getName());
                    }
                })
                .setNegativeButton(R.string.no, null)
                .show();
    }

    /**
     * Change the views to start reading the issue.
     */
    private void readIssue() {
        enableReadArchiveActions();
        BookJsonParserTask parser = new BookJsonParserTask(
                this.getContext(),
                this.magazine,
                this,
                BOOK_JSON_PARSE_TASK);
        if (magazine.isStandalone()) {
            parser.execute("STANDALONE");
        } else {
            parser.execute(magazine.getName());
        }
    }

    private void readOnline() {
        previewLoaded = true;
        findViewById(R.id.actions_ui).setVisibility(View.GONE);
        findViewById(R.id.download_container).setVisibility(View.GONE);
        findViewById(R.id.progress_ui).setVisibility(View.GONE);
        findViewById(R.id.txtProgress).setVisibility(View.VISIBLE);
        ((TextView) findViewById(R.id.txtProgress)).setText(R.string.loadingPreview);

        BookJsonParserTask parser = new BookJsonParserTask(
                this.getContext(),
                this.magazine,
                this,
                BOOK_JSON_PARSE_TASK);
        parser.execute("ONLINE");
    }

    /**
     * Starts the download of an issue, hides the download controls and shows the progress controls.
     */
    public void startPackageDownload() {

        if (null == this.packDownloader) {

            //Package downloader task initialization.
            packDownloader = new DownloaderTask(this.activity,
                    this,
                    this.MAGAZINE_DOWNLOAD_TASK,
                    this.magazine.getUrl(),
                    this.magazine.getName() + activity.getString(R.string.package_extension),
                    this.magazine.getTitle(),
                    this.magazine.getInfo(),
                    this.magazinesDirectory,
                    this.MAGAZINE_DOWNLOAD_VISIBILITY);
        }

        //If the issue is not downloading we start the download, otherwise, do nothing.
        //if (!isDownloading()) {
        // Hide download button
        findViewById(R.id.download_container).setVisibility(View.GONE);

        // Hide Read and Archive Buttons
        findViewById(R.id.actions_ui).setVisibility(View.GONE);

        //Show progressbar and progress text
        findViewById(R.id.txtProgress).setVisibility(View.VISIBLE);
        ((TextView) findViewById(R.id.txtProgress)).setText(R.string.downloading);
        findViewById(R.id.progress_ui).setVisibility(View.VISIBLE);

        packDownloader.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, "");
        //}
    }

    /**
     * Start the unzipping task for an issue. Also handles the controls update.
     *
     * @param filePath the path where the file to unzip is located.
     * @param name     the name of the file to unzip.
     */
    public void startUnzip(final String filePath, final String name) {

        //Make sure the unzipper gets initialized
        if (null == this.unzipperTask) {
            this.unzipperTask = new UnzipperTask(activity, this, UNZIP_MAGAZINE_TASK);
        }
        //Shows and set the text of progress
        ((TextView) findViewById(R.id.txtProgress)).setText(R.string.unzipping);
        findViewById(R.id.txtProgress).setVisibility(View.VISIBLE);

        findViewById(R.id.download_container).setVisibility(View.GONE);
        findViewById(R.id.actions_ui).setVisibility(View.GONE);
//        findViewById(R.id.btnRead).setVisibility(View.GONE);

        //Starts the unzipping task
        unzipperTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, filePath, name);
    }

    /**
     * Download the cover if the file does not exist.
     */
    public void downloadCover() {
        String coverFile = Configuration.getCacheDirectory(this.getContext())
                .concat(File.separator)
                .concat(this.magazine.getName());
        boolean coverExists = (new File(coverFile).exists());
        if (!coverExists && !this.magazine.isStandalone()) {
            Log.d(this.getClass().getName(), "Will download cover for thumb " + this.getId());
            thumbDownloader.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR);
        } else {
            Log.d(this.getClass().getName(), "Cover for thumb " + this.getId() + " exists. Will not download.");
            this.renderCover();
            // We still need to report to the handler in case there are repeated covers that should be rendered.
            this.thumbnailDownloaderHandler.sendEmptyMessage(1);
        }
    }

    /**
     * Sets an image file as the cover of this instance of an issue.
     */
    private void renderCover() {
        String path;
        Bitmap bmp;
        Log.d(this.getClass().getName(), "Will render cover for magazine " + this.magazine.getName());
        try {
            if (this.magazine.isStandalone()) {
                boolean fromAssets = !(this.getContext().getResources().getBoolean(R.bool.sa_read_from_custom_directory));
                if (fromAssets) {
                    String books = this.getContext().getString(R.string.sa_books_directory);
                    path =  books
                            .concat(File.separator)
                            .concat(this.magazine.getName())
                            .concat(File.separator)
                            .concat(this.magazine.getCover());
                    AssetManager assetManager = this.getContext().getAssets();
                    bmp = BitmapFactory.decodeStream(assetManager.open(path));
                } else {
                    path = Configuration.getMagazinesDirectory(this.getContext())
                            .concat(File.separator)
                            .concat(this.magazine.getName())
                            .concat(File.separator)
                            .concat(this.magazine.getCover());
                    bmp = BitmapFactory.decodeFile(path);
                }
            } else {
                path = Configuration.getCacheDirectory(this.getContext())
                        .concat(File.separator)
                        .concat(this.magazine.getName());
                bmp = BitmapFactory.decodeFile(path);
            }
            ((ImageView) findViewById(R.id.imgCover)).setImageBitmap(bmp);
        } catch (IOException ex) {
            Log.e(this.getClass().getName(), "Could not render cover for " + this.magazine.getName(), ex);
        }
    }

    private void resetUI() {
        findViewById(R.id.download_container).setVisibility(View.VISIBLE);
        findViewById(R.id.txtProgress).setVisibility(View.GONE);
        findViewById(R.id.progress_ui).setVisibility(View.GONE);
        findViewById(R.id.actions_ui).setVisibility(View.GONE);
    }

    /**
     * Updates the UI to let the user download the issue.
     */
    private void enableDownloadAction() {
        findViewById(R.id.download_container).setVisibility(View.VISIBLE);
        findViewById(R.id.actions_ui).setVisibility(View.GONE);
        //findViewById(R.id.btnRead).setVisibility(View.GONE);

        //Issue become non readable
        readable = false;
    }

    private void sendDownloadReport() {
        postClientTask = new PostClientTask(POST_DOWNLOAD_TASK, this);

        String url = this.activity.getString(R.string.download_report_url);
        url = url.replace(":issue_id", this.getMagazine().getName());
        url = url.replace(":device_type", "ANDROID");
        url = url.replace(":user_id", GindActivity.userAccount);

        Log.d(this.getClass().getName(), "URL TO REPORT: " + url);

        postClientTask.setUrl(url);
        postClientTask.execute();
    }

    /**
     * As an instance of GindMandator, this class has to implement updateProgress
     * to update the UI when some of the task change it status.
     *
     * @param taskId   the id of the task that changed status.
     * @param progress the progress indicators.
     */
    @Override
    public void updateProgress(final int taskId, Long... progress) {
        //Update the progress bar only when downloading the magazines
        if (taskId == this.MAGAZINE_DOWNLOAD_TASK) {
            //Calculating progress
            long fileProgress = progress[1] / 1048576;
            long length = progress[2] / 1048576;
            Integer intProgress = (int) (long) progress[0];

            //Updating UI
            ((TextView) findViewById(R.id.txtProgress))
                    .setText(String.valueOf(fileProgress) + " MB (" + intProgress + " %)");
            ((ProgressBar) findViewById(R.id.prgDownload)).setProgress(intProgress);
        }
    }

    /**
     * As an instance of GindMandator, this class should implement a method postExecute
     * to be executed when one of the tasks is done.
     *
     * @param taskId  the id of the task that ended.
     * @param results the results values.
     */
    @Override
    public void postExecute(final int taskId, String... results) {
        //Hide progress
        findViewById(R.id.txtProgress).setVisibility(View.GONE);
        findViewById(R.id.progress_ui).setVisibility(View.GONE);

        //TODO: Handle failures.
        switch (taskId) {
            case MAGAZINE_DOWNLOAD_TASK:
                this.packDownloader = null;
                //When the download task ended successfully we will start unzipping the file.
                Log.d(this.getClass().getName(), "DownloaderTask result: " + results[0]);
                if (results[0].equals("SUCCESS")) {
                    // Here we register the DOWNLOAD ISSUE event on Google Analytics
                    if (this.activity.getResources().getBoolean(R.bool.ga_enable) && this.activity.getResources().getBoolean(R.bool.ga_register_issue_download_event)) {
                        ((ABakerApp)this.activity.getApplication()).sendEvent(
                                this.activity.getString(R.string.issues_category),
                                this.activity.getString(R.string.issue_download),
                                this.magazine.getName());
                    }

                    startUnzip(results[1], this.magazine.getName());
                } else if (results[0].equals("ERROR")) {
                    this.resetUI();
                }
                break;
            case THUMB_DOWNLOAD_TASK:
                //If the thumbnail download ended successfully we will render the cover.
                if (results[0].equals("SUCCESS")) {
                    Log.d(this.getClass().getName(), "Cover download for " + this.magazine.getName() + " finished.");
                    this.renderCover();
                    thumbnailDownloaderHandler.sendEmptyMessage(1);
                }
                break;
            case UNZIP_MAGAZINE_TASK:
                //If the Unzip tasks ended successfully we will update the UI to let the user
                //start reading the issue.
                this.unzipperTask = null;
                if (results[0].equals("SUCCESS")) {
                    this.enableReadArchiveActions();
                    this.sendDownloadReport();
                } else {
                    this.resetUI();
                    Toast.makeText(this.getContext(), "Could not extract the package. Possibly corrupted.",
                            Toast.LENGTH_LONG).show();
                }
                break;
            case MAGAZINE_DELETE_TASK:
                //If the issue files were deleted successfully the UI will be updated to let
                //the user download the issue again.
                if (results[0].equals("SUCCESS")) {
                    // Here we register the DELETE ISSUE event on Google Analytics
                    if (this.activity.getResources().getBoolean(R.bool.ga_enable) && this.activity.getResources().getBoolean(R.bool.ga_register_issue_delete_event)) {
                        ((ABakerApp)this.activity.getApplication()).sendEvent(
                                this.activity.getString(R.string.issues_category),
                                this.activity.getString(R.string.issue_delete),
                                this.magazine.getName());
                    }

                    this.enableDownloadAction();
                }
                break;
            case POST_DOWNLOAD_TASK:
                if (!results[0].equals("ERROR")) {
                }
                break;
            case BOOK_JSON_PARSE_TASK:
                try {
                    BookJson bookJson = new BookJson();

                    final String magazineName = results[0];
                    final String rawJson = results[1];

                    bookJson.setMagazineName(magazineName);
                    bookJson.fromJson(rawJson);

                    setBookJson(bookJson);
                } catch (JSONException ex) {
                    Log.e(this.getClass().getName(), "Error parsing the book.json", ex);
                } catch (ParseException ex) {
                    Log.e(this.getClass().getName(), "Error parsing the book.json", ex);
                }

                break;
            case CHECK_INTERNET_TASK:
                if (results[0].equals("TRUE")) {
                    if (TASK == TASKS_AFTER_CHECK_INTERNET.DOWNLOAD_ISSUE) {
                        this.startPackageDownload();
                    } else {
                        this.readOnline();
                    }
                } else {
                    if (TASK == TASKS_AFTER_CHECK_INTERNET.DOWNLOAD_ISSUE) {
                        Log.e(this.getClass().toString(), "No Internet access to download the issue.");
                        Toast.makeText(this.getContext(), this.getContext().getString(R.string.cannot_download_issue),
                                Toast.LENGTH_LONG).show();
                    } else {
                        Log.e(this.getClass().toString(), "Cannot view issue online. No internet access.");
                        Toast.makeText(this.getContext(), this.getContext().getString(R.string.cannot_view_online),
                                Toast.LENGTH_LONG).show();
                    }
                }

                break;
        }
    }
}
