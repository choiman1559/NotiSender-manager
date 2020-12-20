package com.noti.sender;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

import br.tiagohm.markdownview.MarkdownView;
import br.tiagohm.markdownview.css.styles.Github;

@SuppressLint("StaticFieldLeak")
public class GetReleaseTask extends AsyncTask<Void, Void, JSONArray> {
    Activity context;
    ProgressBar progressBar;
    boolean isNewInstall;

    GetReleaseTask(Activity context, ProgressBar progressBar, boolean value) {
        this.context = context;
        this.progressBar = progressBar;
        this.isNewInstall = value;
    }

    @Override
    protected JSONArray doInBackground(Void... params) {
        String str = "https://api.github.com/repos/choiman1559/NotiSender/releases";
        URLConnection urlConn;
        BufferedReader bufferedReader = null;
        try {
            URL url = new URL(str);
            urlConn = url.openConnection();
            bufferedReader = new BufferedReader(new InputStreamReader(urlConn.getInputStream()));

            StringBuilder stringBuffer = new StringBuilder();
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                stringBuffer.append(line);
            }

            return new JSONArray(stringBuffer.toString());
        } catch (Exception ex) {
            Log.e("App", "yourDataTask", ex);
            return null;
        } finally {
            if (bufferedReader != null) {
                try {
                    bufferedReader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    protected void onPostExecute(JSONArray response) {
        if (response != null) {
            try {
                JSONObject obj = response.getJSONObject(0);
                String latestVersion = obj.getString("tag_name");
                if (isNewInstall) {
                    new AlertDialog.Builder(context)
                            .setCancelable(false)
                            .setTitle(context.getString(R.string.dialog_download_title))
                            .setMessage(context.getString(R.string.dialog_download_message))
                            .setNegativeButton("Cancel",(d,w) -> ExitActivity.exitApplication(context))
                            .setPositiveButton("Download",(d,w) -> {
                                ((TextView) context.findViewById(R.id.status1)).setText(context.getString(R.string.main_download));
                                new DownloadTask(context, progressBar).execute(latestVersion);
                            }).show();
                } else {
                    PackageManager pm = context.getPackageManager();
                    String localVersion = pm.getPackageInfo("com.noti.main", 0).versionName.replace(" Beta", "");
                    Version v1 = new Version(latestVersion);
                    Version v2 = new Version(localVersion);

                    if (v1.compareTo(v2) > 0) {
                        //View Layout = context.getLayoutInflater().inflate(R.layout.dialog_updatelog, null);
                        MarkdownView md = new MarkdownView(context);//Layout.findViewById(R.id.mdView);
                        md.addStyleSheet(new Github());
                        md.setEscapeHtml(false);
                        md.loadMarkdown("### Version : " + latestVersion + "\r\n" + obj.getString("body"));

                        new AlertDialog.Builder(context)
                                .setCancelable(false)
                                .setTitle(context.getString(R.string.dialog_update_title))
                                .setMessage(context.getString(R.string.dialog_update_message))
                                .setNegativeButton("Cancel",(d,w) -> ExitActivity.exitApplication(context))
                                .setNeutralButton("NO THANKS",(d,w) -> {
                                    MainActivity.startMainActivity(context);
                                    ExitActivity.exitApplication(context);
                                })
                                .setPositiveButton("Update",(d,w) -> {
                                    ((TextView) context.findViewById(R.id.status1)).setText(context.getString(R.string.main_updating));
                                    new DownloadTask(context, progressBar).execute(latestVersion);
                                }).setView(md/*Layout*/).show();
                    } else {
                        MainActivity.startMainActivity(context);
                        ExitActivity.exitApplication(context);
                    }
                }
            } catch (JSONException | PackageManager.NameNotFoundException ex) {
                ex.printStackTrace();
            }
        }
    }
}