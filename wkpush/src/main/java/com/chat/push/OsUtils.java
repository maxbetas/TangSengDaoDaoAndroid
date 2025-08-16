package com.chat.push;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import com.chat.base.utils.WKReader;

// 🗑️ 已删除：不再需要IO相关导入，统一使用Build.MANUFACTURER
import java.util.List;

/**
 * 2020-03-08 22:43
 * 手机型号判断
 */
public class OsUtils {

    private static final String ROM_MIUI = "MIUI";
    private static final String ROM_EMUI = "EMUI";
    private static final String ROM_FLYME = "FLYME";
    private static final String ROM_OPPO = "OPPO";
    private static final String ROM_SMARTISAN = "SMARTISAN";
    private static final String ROM_VIVO = "VIVO";
    private static final String ROM_QIKU = "QIKU";
    private static final String ROM_HONOR = "HONOR";

    // 🗑️ 已删除：不再需要复杂的系统属性检测，统一使用厂商名映射

    private static String sName;
    private static String sVersion;

    public static boolean isEmui() {
        return check(ROM_EMUI);
    }

    public static boolean isMiui() {
        return check(ROM_MIUI);
    }

    public static boolean isVivo() {
        return check(ROM_VIVO);
    }

    public static boolean isOppo() {
        return check(ROM_OPPO);
    }

    public static boolean isHonor() {
        return check(ROM_HONOR);
    }

    public static boolean isFlyme() {
        return check(ROM_FLYME);
    }

    public static boolean is360() {
        return check(ROM_QIKU) || check("360");
    }

    public static boolean isSmartisan() {
        return check(ROM_SMARTISAN);
    }

    public static String getName() {
        if (sName == null) {
            check("");
        }
        return sName;
    }

    public static String getVersion() {
        if (sVersion == null) {
            check("");
        }
        return sVersion;
    }

    private static boolean check(String rom) {
        if (sName != null) {
            return sName.equals(rom);
        }
        
        // 🏆 最佳实践：直接基于厂商名映射（大小写不敏感）
        String manufacturer = Build.MANUFACTURER;
        sName = mapManufacturerToRom(manufacturer);
        sVersion = Build.DISPLAY; // 保留版本信息用于调试
        
        return sName.equals(rom);
    }

    /**
     * 🏆 最佳实践：厂商映射（大小写不敏感，健壮性最强）
     * 
     * 技术要点：
     * 1. 使用equalsIgnoreCase()避免大小写问题（华为可能返回"HUAWEI"或"Huawei"）
     * 2. 支持所有新老系统，无需复杂的系统属性检测
     * 3. 基于Build.MANUFACTURER永远稳定，厂商品牌不会变
     * 
     * @param manufacturer 厂商名（Build.MANUFACTURER的值）
     * @return 对应的ROM名（如MIUI、EMUI等）
     */
    private static String mapManufacturerToRom(String manufacturer) {
        // 🔧 使用equalsIgnoreCase确保大小写兼容性（最佳实践）
        if ("Xiaomi".equalsIgnoreCase(manufacturer)) return ROM_MIUI;      // 小米全系列
        if ("HUAWEI".equalsIgnoreCase(manufacturer)) return ROM_EMUI;      // 华为全系列
        if ("OPPO".equalsIgnoreCase(manufacturer)) return ROM_OPPO;        // OPPO全系列
        if ("vivo".equalsIgnoreCase(manufacturer)) return ROM_VIVO;        // Vivo全系列
        if ("HONOR".equalsIgnoreCase(manufacturer)) return ROM_HONOR;      // 荣耀全系列
        if ("Meizu".equalsIgnoreCase(manufacturer)) return ROM_FLYME;      // 魅族全系列
        if ("smartisan".equalsIgnoreCase(manufacturer)) return ROM_SMARTISAN; // 锤子
        if ("360".equalsIgnoreCase(manufacturer)) return ROM_QIKU;         // 360手机
        
        return manufacturer.toUpperCase(); // 未知厂商统一大写处理
    }

    // 🗑️ 已删除：getProp()方法不再需要，统一使用Build.MANUFACTURER

    static void setBadge(Context context, int number) {
        if (isEmui()) {
            try {
                Bundle bundle = new Bundle();
                bundle.putString("package", WKPushApplication.getInstance().pushBundleID); // com.test.badge is your package name
                bundle.putString("class", WKPushApplication.getInstance().pushBundleID + ".MainActivity"); // com.test. badge.MainActivity is your apk main activity
                bundle.putInt("badgenumber", number);
                context.getContentResolver().call(Uri.parse("content://com.huawei.android.launcher.settings/badge/"), "change_badge", null, bundle);
            } catch (Exception e) {
                Log.e("设置红点", "-->不支持");
            }
        } else if (isVivo()) {
            try {
                Intent intent = new Intent("launcher.action.CHANGE_APPLICATION_NOTIFICATION_NUM");
                intent.putExtra("packageName", context.getPackageName());
                // String launchClassName = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName()).getComponent().getClassName();
                intent.putExtra("className", WKPushApplication.getInstance().pushBundleID + ".MainActivity");
                intent.putExtra("notificationNum", number);
                context.sendBroadcast(intent);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (isOppo()) {
            try {
                if (number == 0) {
                    number = -1;
                }
                Intent intent = new Intent("com.oppo.unsettledevent");
                intent.putExtra("pakeageName", context.getPackageName());
                intent.putExtra("number", number);
                intent.putExtra("upgradeNumber", number);
                if (canResolveBroadcast(context, intent)) {
                    context.sendBroadcast(intent);
                } else {
                    try {
                        Bundle extras = new Bundle();
                        extras.putInt("app_badge_count", number);
                        context.getContentResolver().call(Uri.parse("content://com.android.badge/badge"), "setAppBadgeCount", null, extras);
                    } catch (Throwable t) {
                        t.printStackTrace();
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }

    public static boolean canResolveBroadcast(Context context, Intent intent) {
        PackageManager packageManager = context.getPackageManager();
        List<ResolveInfo> receivers = packageManager.queryBroadcastReceivers(intent, 0);
        return WKReader.isNotEmpty(receivers);
    }
}
