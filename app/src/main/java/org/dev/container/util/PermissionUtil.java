package org.dev.container.util;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;
import androidx.activity.result.ActivityResultLauncher;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

/**
 * 权限管理工具类
 */
public final class PermissionUtil {

    private PermissionUtil() {}

    /**
     * 检测存储权限：
     * 1. 常规读写权限
     * 2. Android 11+ 全文件访问权限
     */
    public static boolean hasStoragePermission(Context context) {
        boolean hasNormalStorage = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return hasNormalStorage && Environment.isExternalStorageManager();
        } else {
            return hasNormalStorage && (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);
        }
    }

    /**
     * 检测安装权限
     */
    public static boolean hasInstallPermission(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return context.getPackageManager().canRequestPackageInstalls();
        }
        return true;
    }

    /**
     * 检测通知权限（通过 NotificationManagerCompat 检测真实的系统级通知总开关）
     */
    public static boolean hasNotificationPermission(Context context) {
        return NotificationManagerCompat.from(context).areNotificationsEnabled();
    }

    /**
     * 检测所有必须权限
     */
    public static boolean isAllPermissionsGranted(Context context) {
        return hasStoragePermission(context) && hasInstallPermission(context) && hasNotificationPermission(context);
    }

    /**
     * 申请通知权限
     */
    public static void requestNotificationPermission(Activity activity, ActivityResultLauncher<Intent> appSettingsLauncher, ActivityResultLauncher<String[]> runtimeLauncher) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && activity.getApplicationInfo().targetSdkVersion >= Build.VERSION_CODES.TIRAMISU) {
            runtimeLauncher.launch(new String[]{Manifest.permission.POST_NOTIFICATIONS});
            return;
        }

        // targetSdk < 33 或低版本系统直接跳转应用通知设置页
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Intent intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
                intent.putExtra(Settings.EXTRA_APP_PACKAGE, activity.getPackageName());
                appSettingsLauncher.launch(intent);
                return;
            }
        } catch (Exception ignored) {}

        openAppDetailsSettings(activity, appSettingsLauncher);
    }

    /**
     * 申请存储与文件管理权限
     */
    public static void requestStoragePermission(Activity activity, ActivityResultLauncher<Intent> manageLauncher, ActivityResultLauncher<String[]> runtimeLauncher) {
        // 先检查是否缺常规读写权限
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            runtimeLauncher.launch(new String[]{
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
            });
            return;
        }

        // Android 11+ 全文件访问
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                try {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                    intent.setData(Uri.parse("package:" + activity.getPackageName()));
                    manageLauncher.launch(intent);
                    return;
                } catch (Exception e) {
                    try {
                        Intent fallbackIntent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                        manageLauncher.launch(fallbackIntent);
                        return;
                    } catch (Exception ex) {
                        openAppDetailsSettings(activity, manageLauncher);
                        return;
                    }
                }
            }
        }

        // 低版本常规写权限
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            runtimeLauncher.launch(new String[]{
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
            });
        }
    }

    /**
     * 申请 APK 安装未知来源权限
     */
    public static void requestInstallPermission(Activity activity, ActivityResultLauncher<Intent> installLauncher) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!activity.getPackageManager().canRequestPackageInstalls()) {
                try {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES);
                    intent.setData(Uri.parse("package:" + activity.getPackageName()));
                    installLauncher.launch(intent);
                } catch (Exception e) {
                    openAppDetailsSettings(activity, installLauncher);
                }
            }
        }
    }

    /**
     * 跳转至应用自身详情设置页（终极兜底）
     */
    public static void openAppDetailsSettings(Activity activity, ActivityResultLauncher<Intent> launcher) {
        try {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.parse("package:" + activity.getPackageName()));
            launcher.launch(intent);
        } catch (Exception ignored) {}
    }
}