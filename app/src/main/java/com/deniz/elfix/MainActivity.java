package com.deniz.elfix;

import android.Manifest;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private AppLock al;
    private Switch service;
    private Switch screen;
    private Switch wake_screen;
    private SharedPreferences prefs;

    public static String PACKAGE_NAME = "com.deniz.elfix";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        al = new AppLock(this);

        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_NOTIFICATION_POLICY)
                == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_NOTIFICATION_POLICY},
                    1);
        }

        service = findViewById(R.id.switch_service);
        service.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    if (!AppLock.isSetup()) {
                        showInitDialog();
                    } else {
                        if (!NotificationService.checkNotificationEnabled(getApplicationContext())) {
                            Toast.makeText(getApplicationContext(), "Please enable \"Notification Access\" permission for " + al.getAppName(getPackageName()), Toast.LENGTH_LONG).show();
                            startActivityForResult(new Intent(
                                    "android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"), 2);
                        } else {
                            toggleService(true);
                        }
                        updateUI();
                    }
                } else {
                    toggleService(false);
                }
            }
        });

        prefs = getSharedPreferences(PACKAGE_NAME, MODE_PRIVATE);
        screen = findViewById(R.id.switch_screen);
        screen.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                prefs.edit().putBoolean("SCREEN_ON", isChecked).apply();
            }
        });
        wake_screen = findViewById(R.id.wake_screen);
        wake_screen.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                prefs.edit().putBoolean("WAKE_SCREEN", isChecked).apply();
            }
        });

        Button start_other_els = findViewById(R.id.button_start_els);
        start_other_els.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
                mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
                List<ResolveInfo> pkgAppsList = getPackageManager().queryIntentActivities( mainIntent, 0);

                for (ResolveInfo ri : pkgAppsList) {
                    String pkg = ri.activityInfo.packageName;
                    if (pkg.contains("com.deniz.elfix")) {
                        if (!pkg.equals(getPackageName())) {
                            Intent launchIntent = getPackageManager().getLaunchIntentForPackage(pkg);
                            if (!isAppRunning(getApplicationContext(), pkg)) {
                                Toast.makeText(getApplicationContext(), "Launching " + al.getAppName(pkg), Toast.LENGTH_LONG).show();
                                startActivity(launchIntent);
                                break;
                            }
                        }
                    }
                }
            }
        });

        service.setChecked(true);
        screen.setChecked(prefs.getBoolean("SCREEN_ON", false));
        wake_screen.setChecked(prefs.getBoolean("WAKE_SCREEN", false));
    }

    public static boolean isAppRunning(final Context context, final String packageName) {
        final ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        final List<ActivityManager.RunningAppProcessInfo> procInfos = activityManager.getRunningAppProcesses();
        if (procInfos != null) {
            for (final ActivityManager.RunningAppProcessInfo processInfo : procInfos) {
                if (processInfo.processName.equals(packageName)) {
                    return true;
                }
            }
        }
        return false;
    }

    private void toggleService(boolean state) {
        Intent startIntent = new Intent(MainActivity.this, ForegroundService.class);
        if (state)
            startService(startIntent);
        else
            stopService(startIntent);
    }

    private void showInitDialog() {
        final Intent appIntent = new Intent(MainActivity.this, Chooser.class);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Lock the App")
                .setMessage(getString(R.string.dialog_msg))
                .setPositiveButton("Continue", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        appIntent.putExtra("multiple", false);
                        startActivityForResult(appIntent, 1);
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        appIntent.putExtra("multiple", true);
                        startActivityForResult(appIntent, 1);
                    }
                }).setCancelable(AppLock.isSetup());

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void updateUI() {
        TextView name = findViewById(R.id.appName);
        TextView desc = findViewById(R.id.desc);
        ImageView iv = findViewById(R.id.appIcon);
        Button change = findViewById(R.id.button);

        change.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showInitDialog();
            }
        });

        System.out.println(AppLock.isLocked());
        if (!AppLock.isLocked()) {
            int a = AppLock.AVAILABLE_PACKAGE_NAMES.size();
            name.setText(a + " app" + (a > 1 ? "s": "") + " selected");
            String desc_txt = "Selected Apps:\n";
            for (String app : AppLock.AVAILABLE_PACKAGE_NAMES) {
                desc_txt += al.getAppName(app) + "\n";
            }
            desc_txt += "\nStarted service";
            desc.setText(desc_txt);
            iv.setImageResource(R.mipmap.ic_launcher);
            try {
                getActionBar().setTitle("Multiple Mode");
                getSupportActionBar().setTitle("Multiple Mode");
            } catch (NullPointerException ignored) {}
        } else {
            name.setText(al.getAppName(AppLock.LOCK_PACKAGE_NAME));
            desc.setText("Locked and Started service");
            try {
                Drawable icon = getPackageManager().getApplicationIcon(AppLock.LOCK_PACKAGE_NAME);
                iv.setImageDrawable(icon);
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
            try {
                getActionBar().setTitle(getString(R.string.app_name));
                getSupportActionBar().setTitle(getString(R.string.app_name));
            } catch (NullPointerException ignored) {}
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 2) {
            if (NotificationService.checkNotificationEnabled(this)) {
                toggleService(true);
                service.setChecked(true);
            } else {
                System.exit(0);
            }
        } else {
            if (null != data) {
                if (requestCode == 1) {
                    AppLock.LOCK_PACKAGE_NAME = data.getStringExtra("package_name");
                }
            } else {
                AppLock.LOCK_PACKAGE_NAME = null;
            }
            if (AppLock.isSetup()) {
                al.saveDataToSP();
                updateUI();
                if (!NotificationService.checkNotificationEnabled(this)) {
                    Toast.makeText(this, "Please enable \"Notification Access\" permission for " + al.getAppName(getPackageName()), Toast.LENGTH_LONG).show();
                    startActivityForResult(new Intent(
                            "android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"), 2);
                } else {
                    toggleService(true);
                    service.setChecked(true);
                }
            }
        }
    }
}
