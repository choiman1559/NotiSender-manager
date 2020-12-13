package com.noti.sender;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
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

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !isPermission(this)) {
            this.requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
        } else if (Build.VERSION.SDK_INT >= 26 && !this.getPackageManager().canRequestPackageInstalls()) {
            this.startActivityForResult(new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).setData(Uri.parse("package:" + this.getPackageName())), 2);
        } else init();
    }

    private void init() {
        TextView status1 = findViewById(R.id.status1);
        ProgressBar status2 = findViewById(R.id.status2);

        if (isMainAppInstalled(this)) {
            if (isOnline()) {
                status1.setText(R.string.main_checkupdate);
                status2.setIndeterminate(true);
                new GetReleaseTask(this, status2, false).execute();
            } else {
                startMainActivity(this);
                finish();
            }
        } else {
            new GetReleaseTask(this, status2, true).execute();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private boolean isPermission(Context context) {
        boolean value1 = context.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        boolean value2 = this.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        return value1 || value2;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        for (int i : grantResults) {
            if (i == PackageManager.PERMISSION_DENIED) ExitActivity.exitApplication(this);
        }
        if (Build.VERSION.SDK_INT >= 26 && !this.getPackageManager().canRequestPackageInstalls()) {
            this.startActivityForResult(new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).setData(Uri.parse("package:" + this.getPackageName())), 2);
        } else init();
    }

    public static void startMainActivity(Context context) {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.setComponent(new ComponentName("com.noti.main", "com.noti.main.SettingsActivity"));
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1234 && isMainAppInstalled(getApplicationContext())) {
            startMainActivity(getApplicationContext());
        } else ExitActivity.exitApplication(this);

        if (requestCode == 2) {
            if (Build.VERSION.SDK_INT < 26 || resultCode != Activity.RESULT_OK) {
                ExitActivity.exitApplication(this);
            } else init();
        }
    }

    public static boolean isMainAppInstalled(Context context) {
        PackageManager pm = context.getPackageManager();
        boolean app_installed;
        try {
            pm.getPackageInfo("com.noti.main", PackageManager.GET_ACTIVITIES);
            app_installed = true;
        } catch (PackageManager.NameNotFoundException e) {
            app_installed = false;
        }
        return app_installed;
    }

    public boolean isOnline() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        assert cm != null;
        if (cm.getActiveNetworkInfo() != null) {
            NetworkInfo ni = cm.getActiveNetworkInfo();
            return ni != null && ni.isConnected();
        }
        return false;
    }

    @SuppressLint("StaticFieldLeak")
    protected static class GetReleaseTask extends AsyncTask<Void, Void, JSONArray> {
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
                        AlertDialog.Builder alert = new AlertDialog.Builder(context);
                        alert
                                .setCancelable(false)
                                .setTitle(context.getString(R.string.dialog_download_title))
                                .setMessage(context.getString(R.string.dialog_download_message))
                                .setNegativeButton("Cancel",(d,w) -> ExitActivity.exitApplication(context))
                                .setPositiveButton("Download",(d,w) -> {
                            ((TextView) context.findViewById(R.id.status1)).setText(context.getString(R.string.main_download));
                            new DownloadTask(context, progressBar).execute("https://github.com/choiman1559/NotiSender/releases/download/" + latestVersion + "/app-release.apk");
                        }).show();
                    } else {
                        PackageManager pm = context.getPackageManager();
                        String localVersion = pm.getPackageInfo("com.noti.main", 0).versionName.replace(" Beta", "");
                        Version v1 = new Version(latestVersion);
                        Version v2 = new Version(localVersion);

                        if (v1.compareTo(v2) > 0) {
                            AlertDialog.Builder alert = new AlertDialog.Builder(context);
                            alert
                                    .setCancelable(false)
                                    .setTitle(context.getString(R.string.dialog_update_title))
                                    .setMessage(context.getString(R.string.dialog_update_message))
                                    .setNegativeButton("Cancel",(d,w) -> ExitActivity.exitApplication(context))
                                    .setPositiveButton("Update",(d,w) -> {
                                        ((TextView) context.findViewById(R.id.status1)).setText(context.getString(R.string.main_updating));
                                        new DownloadTask(context, progressBar).execute("https://github.com/choiman1559/NotiSender/releases/download/" + latestVersion + "/app-release.apk");
                                    }).show();
                        } else {
                            startMainActivity(context);
                            ExitActivity.exitApplication(context);
                        }
                    }
                } catch (JSONException | PackageManager.NameNotFoundException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }
}