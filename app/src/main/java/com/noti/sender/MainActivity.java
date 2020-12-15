package com.noti.sender;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.ProgressBar;
import android.widget.TextView;

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
                ExitActivity.exitApplication(this);
            }
        } else {
            if(isOnline()) new GetReleaseTask(this, status2, true).execute();
            else {
                AlertDialog.Builder alert = new AlertDialog.Builder(this);
                alert.setNegativeButton("Close", (d,w) -> ExitActivity.exitApplication(this));
                alert.setCancelable(false).setTitle("Error").setMessage("please check Internet connection and try again!");
                alert.show();
            }
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
        if (requestCode == 1234) {
            ExitActivity.exitApplication(this);
        }

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
}