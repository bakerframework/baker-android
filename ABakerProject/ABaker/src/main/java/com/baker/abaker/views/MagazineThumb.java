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

import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.baker.abaker.Configuration;
import com.baker.abaker.GindActivity;
import com.baker.abaker.R;
import com.baker.abaker.client.GindMandator;
import com.baker.abaker.client.PostClientTask;
import com.baker.abaker.model.BookJson;
import com.baker.abaker.model.Magazine;
import com.baker.abaker.workers.BookJsonParserTask;
import com.baker.abaker.workers.DownloaderTask;
import com.baker.abaker.workers.MagazineDeleteTask;
import com.baker.abaker.workers.UnzipperTask;

import java.io.File;


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

    private final int THUMB_DOWNLOAD_TASK = 0;
    private final int THUMB_DOWNLOAD_VISIBILITY = DownloadManager.Request.VISIBILITY_HIDDEN;
    private final int MAGAZINE_DOWNLOAD_TASK = 1;
    private final int MAGAZINE_DOWNLOAD_VISIBILITY = DownloadManager.Request.VISIBILITY_VISIBLE;
    private final int UNZIP_MAGAZINE_TASK = 2;
    private final int MAGAZINE_DELETE_TASK = 3;
    private final int POST_DOWNLOAD_TASK = 4;

    private Context context;

    /**
     * Set to true when the user uses the Preview button rather than downloding the package.
     */
    private boolean previewLoaded = false;

    /**
     * Creates an instance of MagazineThumb to with an activity context.
     *
     * @param context the parent Activity context.
     */
    public MagazineThumb(Context context, Magazine mag) {
        super(context);

        this.context = context;

        //Paths to the application files
        magazinesDirectory = Configuration.getMagazinesDirectory(context);
        cachePath = Configuration.getCacheDirectory(context);

        //Set the magazine model to the thumb instance.
        this.magazine = mag;

        //Thumbnail downloader task initialization.
        thumbDownloader = new DownloaderTask(context,
                this,
                this.THUMB_DOWNLOAD_TASK,
                this.magazine.getCover(),
                this.magazine.getName(), //Set the same name as the magazine
                this.magazine.getTitle().concat(" cover"),
                "File to show on the shelf",
                cachePath,
                this.THUMB_DOWNLOAD_VISIBILITY);
        packDownloader = new DownloaderTask(this.context,
                this,
                this.MAGAZINE_DOWNLOAD_TASK,
                this.magazine.getUrl(),
                this.magazine.getName() + context.getString(R.string.package_extension),
                this.magazine.getTitle(),
                this.magazine.getInfo(),
                this.magazinesDirectory,
                this.MAGAZINE_DOWNLOAD_VISIBILITY);

        //Unzipper task initialization
        unzipperTask = new UnzipperTask(context, this, UNZIP_MAGAZINE_TASK);

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

        // Download the cover if not exists.
        if (!(new File(Configuration.getCacheDirectory(this.getContext())
                + File.separator + this.magazine.getName())).exists()) {
            thumbDownloader.execute();
        } else {
            this.renderCover(Configuration.getCacheDirectory(
                    this.getContext()) + File.separator + this.magazine.getName());
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
                if (readable && !MagazineThumb.this.isDownloading()) {
                    readIssue();
                } else if (!MagazineThumb.this.isDownloading()) {
                    startPackageDownload();
                }
            }
        });

        //Click on download button
        findViewById(R.id.btnDownload).setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                startPackageDownload();
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
            findViewById(R.id.btnPreview).setVisibility(View.VISIBLE);
            findViewById(R.id.btnPreview).setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    readOnline();
                }
            });
        }
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
            Toast.makeText(this.getContext(), "Not valid book.json found!",
                    Toast.LENGTH_LONG).show();
        }

        if (this.previewLoaded == true) {
            findViewById(R.id.download_container).setVisibility(View.VISIBLE);
            findViewById(R.id.txtProgress).setVisibility(View.GONE);
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
        BookJsonParserTask parser = new BookJsonParserTask(
                MagazineThumb.this);
        parser.execute(magazine.getName());
    }

    private void readOnline() {
        previewLoaded = true;
        findViewById(R.id.download_container).setVisibility(View.GONE);
        findViewById(R.id.txtProgress).setVisibility(View.VISIBLE);
        ((TextView) findViewById(R.id.txtProgress)).setText(R.string.loadingPreview);

        BookJsonParserTask parser = new BookJsonParserTask(
                MagazineThumb.this);
        parser.execute("ONLINE");
    }

    /**
     * Starts the download of an issue, hides the download controls and shows the progress controls.
     */
    public void startPackageDownload() {

        if (null == this.packDownloader) {

            //Package downloader task initialization.
            packDownloader = new DownloaderTask(this.context,
                    this,
                    this.MAGAZINE_DOWNLOAD_TASK,
                    this.magazine.getUrl(),
                    this.magazine.getName() + context.getString(R.string.package_extension),
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
            this.unzipperTask = new UnzipperTask(context, this, UNZIP_MAGAZINE_TASK);
        }

        findViewById(R.id.download_container).setVisibility(View.GONE);
        findViewById(R.id.actions_ui).setVisibility(View.GONE);
//        findViewById(R.id.btnRead).setVisibility(View.GONE);
        //Shows and set the text of progress
        ((TextView) findViewById(R.id.txtProgress)).setText(R.string.unzipping);
        findViewById(R.id.txtProgress).setVisibility(View.VISIBLE);

        //Starts the unzipping task
        unzipperTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, filePath, name);
    }

    /**
     * Sets an image file as the cover of this instance of an issue.
     *
     * @param path the path of the file to render.
     */
    private void renderCover(final String path) {
        Bitmap bmp = BitmapFactory.decodeFile(path);
        ((ImageView) findViewById(R.id.imgCover)).setImageBitmap(bmp);
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

        String url = this.context.getString(R.string.download_report_url);
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
                if (results[0] == "SUCCESS") {
                    startUnzip(results[1], this.magazine.getName());
                }
                break;
            case THUMB_DOWNLOAD_TASK:
                //If the thumbnail download ended successfully we will render the cover.
                if (results[0] == "SUCCESS") {
                    this.renderCover(Configuration.getCacheDirectory(
                            this.getContext()) + File.separator + this.magazine.getName());
                }
                break;
            case UNZIP_MAGAZINE_TASK:
                //If the Unzip tasks ended successfully we will update the UI to let the user
                //start reading the issue.
                this.unzipperTask = null;
                if (results[0] == "SUCCESS") {
                    this.enableReadArchiveActions();
                    this.sendDownloadReport();
                } else {
                    Toast.makeText(this.getContext(), "Could not extract the package. Possibly corrupted.",
                            Toast.LENGTH_LONG).show();
                }
                break;
            case MAGAZINE_DELETE_TASK:
                //If the issue files were deleted successfully the UI will be updated to let
                //the user download the issue again.
                if (results[0] == "SUCCESS") {
                    this.enableDownloadAction();
                }
                break;
            case POST_DOWNLOAD_TASK:
                if (results[0] != "ERROR") {
                }
                break;
        }
    }
}
