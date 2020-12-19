package com.noti.sender;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.os.PowerManager;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

@SuppressLint("StaticFieldLeak")
public class DownloadTask extends AsyncTask<String, Integer, String> {

    private final Activity context;
    private final ProgressBar progressBar;
    private PowerManager.WakeLock mWakeLock;
    private String latestVersion;

    public DownloadTask(Activity context,ProgressBar progressBar) {
        this.context = context;
        this.progressBar = progressBar;
    }

    @SuppressLint("WakelockTimeout")
    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        File apk = new File(Environment.getExternalStorageDirectory().getPath() + "/Download/NotiSender_release.apk");
        if(!apk.getParentFile().exists()) apk.getParentFile().mkdirs();
        if(apk.exists()) apk.delete();

        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, getClass().getName());
        mWakeLock.acquire();
        progressBar.setIndeterminate(false);
        progressBar.setProgress(0);
    }

    @Override
    protected void onProgressUpdate(Integer... progress) {
        super.onProgressUpdate(progress);
        progressBar.setProgress(progress[0]);
    }

    @Override
    protected void onPostExecute(String result) {
        mWakeLock.release();
        progressBar.setProgress(100);
        File apk = new File(Environment.getExternalStorageDirectory().getPath() + "/Download/NotiSender" + latestVersion + "_release.apk");
        if (result != null) {
            if(apk.exists()) apk.delete();
            new AlertDialog.Builder(context)
                    .setTitle("Error!")
                    .setMessage("Error occurred while downloading : " + result)
                    .setPositiveButton("Close",(d,w) -> ExitActivity.exitApplication(context))
                    .show();
        } else {
            ((TextView)context.findViewById(R.id.status1)).setText(context.getString(R.string.main_install));
            Intent intent = new Intent(Intent.ACTION_VIEW);
            Uri uri = Build.VERSION.SDK_INT <= 23 ? Uri.fromFile(apk) : FileProvider.getUriForFile(context, BuildConfig.APPLICATION_ID + ".provider", apk);
            if (Build.VERSION.SDK_INT <= 23) {
                intent.setDataAndType(uri, "application/vnd.android.package-archive");
            } else {
                intent.setData(uri);
            }
            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            context.startActivityForResult(intent,1234);
        }
    }

    @Override
    protected String doInBackground(String... sUrl) {
        InputStream input = null;
        OutputStream output = null;
        HttpURLConnection connection = null;
        try {
            latestVersion = sUrl[0];
            URL url = new URL("https://github.com/choiman1559/NotiSender/releases/download/" + latestVersion + "/app-release.apk");
            connection = (HttpURLConnection) url.openConnection();
            connection.connect();
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                return "Server returned HTTP " + connection.getResponseCode() + " " + connection.getResponseMessage();
            }

            int fileLength = connection.getContentLength();
            input = connection.getInputStream();
            output = new FileOutputStream(Environment.getExternalStorageDirectory().getPath() + "/Download/NotiSender" + latestVersion + "_release.apk");

            byte[] data = new byte[4096];
            long total = 0;
            int count;
            while ((count = input.read(data)) != -1) {
                if (isCancelled()) {
                    input.close();
                    return null;
                }
                total += count;
                if (fileLength > 0)
                    publishProgress((int) (total * 100 / fileLength));
                output.write(data, 0, count);
            }
        } catch (Exception e) {
            return e.toString();
        } finally {
            try {
                if (output != null)
                    output.close();
                if (input != null)
                    input.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (connection != null)
                connection.disconnect();
        }
        return null;
    }
}