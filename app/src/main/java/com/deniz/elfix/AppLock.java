package com.deniz.elfix;

import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import static android.content.Context.MODE_PRIVATE;

public class AppLock {

    public static String PACKAGE_NAME = "com.deniz.elfix";
    public static String LOCK_PACKAGE_NAME;

    public static Set<String> AVAILABLE_PACKAGE_NAMES;

    private Context context;
    private SharedPreferences sp;

    AppLock(Context context) {
        this.context = context;
        sp = context.getSharedPreferences(PACKAGE_NAME, MODE_PRIVATE);
        getDataFromSP();
        selfCheck();
    }

    private void selfCheck() {
        if (LOCK_PACKAGE_NAME == null && AVAILABLE_PACKAGE_NAMES == null) {
            String curPackName = context.getPackageName();
            if (!curPackName.equals(PACKAGE_NAME)) {
                String ext_pack = curPackName.split(PACKAGE_NAME)[1].substring(1);
                if (isPackageExisted(ext_pack)) {
                    LOCK_PACKAGE_NAME = ext_pack;
                    saveDataToSP();
                }
            }
        }
    }

    private boolean isPackageExisted(String targetPackage) {
        PackageManager pm = context.getPackageManager();
        try {
            pm.getPackageInfo(targetPackage, PackageManager.GET_META_DATA);
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
        return true;
    }

    private void getDataFromSP() {
        SharedPreferences prefs = context.getSharedPreferences(PACKAGE_NAME, MODE_PRIVATE);
        LOCK_PACKAGE_NAME = prefs.getString("LOCK_PACKAGE_NAME", null);
        AVAILABLE_PACKAGE_NAMES = prefs.getStringSet("AVAILABLE_PACKAGE_NAMES", null);
    }

    public static boolean isLocked() {
        return LOCK_PACKAGE_NAME != null && !LOCK_PACKAGE_NAME.equals("");
    }

    public static boolean isSetup() {
        return isLocked() | AVAILABLE_PACKAGE_NAMES != null;
    }

    public String getAppName(String pack) {
        final PackageManager pm = context.getPackageManager();
        ApplicationInfo ai;
        try {
            ai = pm.getApplicationInfo(pack, 0);
        } catch (final PackageManager.NameNotFoundException e) {
            ai = null;
        }
        return (String) (ai != null ? pm.getApplicationLabel(ai) : "(unknown)");
    }

    public static void setAvailablePackageNames(ArrayList<ResolveInfo> al) {
        AVAILABLE_PACKAGE_NAMES = new HashSet<>();
        for (ResolveInfo ri : al) {
            ActivityInfo activity = ri.activityInfo;
            ComponentName name = new ComponentName(activity.applicationInfo.packageName,
                    activity.name);

            AVAILABLE_PACKAGE_NAMES.add(name.getPackageName());
        }
    }

    public void saveDataToSP() {
        SharedPreferences.Editor editor = sp.edit();
        editor.putString("LOCK_PACKAGE_NAME", LOCK_PACKAGE_NAME);
        if (AVAILABLE_PACKAGE_NAMES != null)
            editor.putStringSet("AVAILABLE_PACKAGE_NAMES", AVAILABLE_PACKAGE_NAMES);
        editor.apply();
    }
}
