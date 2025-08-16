package com.chat.push;

import android.app.Activity;
import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

import com.chat.base.WKBaseApplication;
import com.chat.base.config.WKConfig;
import com.chat.base.config.WKConstants;
import com.chat.base.endpoint.EndpointCategory;
import com.chat.base.endpoint.EndpointManager;
import com.chat.base.endpoint.entity.LoginMenu;
import com.chat.base.net.HttpResponseCode;
import com.chat.base.ui.Theme;
import com.chat.base.utils.WKDialogUtils;
import com.chat.base.utils.WKToastUtils;
import com.chat.base.utils.systembar.WKOSUtils;
import com.chat.push.OsUtils;
import com.chat.push.service.PushModel;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;
import com.heytap.msp.push.HeytapPushManager;
import com.heytap.msp.push.callback.ICallBackResultService;
import com.huawei.hms.aaid.HmsInstanceId;
import com.huawei.hms.common.ApiException;
import com.vivo.push.PushClient;
import com.vivo.push.util.VivoPushException;
import com.xiaomi.mipush.sdk.MiPushClient;

import java.lang.ref.WeakReference;
import android.app.ActivityManager;
import android.os.Process;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Build;

/**
 * 2020-03-08 22:29
 * 推送管理
 */
public class WKPushApplication {
    private WKPushApplication() {
    }

    private static class PushApplicationBinder {
        static final WKPushApplication push = new WKPushApplication();
    }

    private WeakReference<Context> mContext;
    public String pushBundleID;

    public static WKPushApplication getInstance() {
        return PushApplicationBinder.push;
    }


    //初始化推送服务
    public void init(String pushBundleID, final Context context) {
        this.pushBundleID = pushBundleID;
        this.mContext = new WeakReference<>(context);
        addListener();
        initPush();
        EndpointManager.getInstance().setMethod("", EndpointCategory.loginMenus, object -> new LoginMenu(this::initPush));
    }

    private void initPush() {
        if (mContext == null || mContext.get() == null) return;
        FirebaseApp.initializeApp(mContext.get());
        notifyChannel(WKBaseApplication.getInstance().application);
        getPushToken();

    }

    private void getHuaWeiToken(Context context) {
        try {
            // 输入token标识"HCM"
            String tokenScope = "HCM";
            String token = HmsInstanceId.getInstance(context).getToken(PushKeys.huaweiAPPID, tokenScope);
            // 判断token是否为空
            if (!TextUtils.isEmpty(token)) {
                Log.e("华为推送token", token);
                PushModel.getInstance().registerDeviceToken(token, pushBundleID,"");
            }
        } catch (ApiException e) {
            Log.e("华为推送", "获取token失败: " + e.getStatusCode() + " - " + e.getMessage());
        }
    }

    private void initXiaoMiPush(Context context) {
        // 按照官方demo标准，只在主进程中初始化小米推送
        if (isMainProcess(context)) {
            // 按官方文档建议，检查网络状态（避免70000001错误）
            if (isNetworkAvailable(context)) {
                MiPushClient.registerPush(context, PushKeys.xiaoMiAppID, PushKeys.xiaoMiAppKey);
                Log.d("小米推送", "网络状态正常，开始注册推送");
            } else {
                Log.w("小米推送", "网络不可用，推送注册可能失败");
                // 仍然尝试注册，让SDK自己处理网络重连
                MiPushClient.registerPush(context, PushKeys.xiaoMiAppID, PushKeys.xiaoMiAppKey);
            }
        }
    }
    
    private boolean isMainProcess(Context context) {
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        if (am != null) {
            java.util.List<ActivityManager.RunningAppProcessInfo> processInfos = am.getRunningAppProcesses();
            String mainProcessName = context.getPackageName();
            int myPid = Process.myPid();
            for (ActivityManager.RunningAppProcessInfo info : processInfos) {
                if (info.pid == myPid && mainProcessName.equals(info.processName)) {
                    return true;
                }
            }
        }
        return false;
    }
    
    /**
     * 按官方文档检查网络状态（避免70000001错误）
     * 兼容新旧API版本
     * @param context 上下文
     * @return true如果网络可用
     */
    private boolean isNetworkAvailable(Context context) {
        try {
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm == null) return true;
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                // Android 6.0及以上使用新API
                Network network = cm.getActiveNetwork();
                if (network != null) {
                    NetworkCapabilities capabilities = cm.getNetworkCapabilities(network);
                    return capabilities != null && 
                           (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET));
                }
                return false;
            } else {
                // Android 6.0以下使用旧API
                NetworkInfo networkInfo = cm.getActiveNetworkInfo();
                return networkInfo != null && networkInfo.isConnected();
            }
        } catch (Exception e) {
            Log.w("网络检查", "网络状态检查异常: " + e.getMessage());
        }
        return true; // 异常时假设网络可用，让推送SDK自己处理
    }
    
    /**
     * 按官方文档注销所有推送服务（使regID失效）
     */
    private void unregisterAllPushServices() {
        Context context = mContext != null ? mContext.get() : null;
        if (context == null) return;
        
        try {
            // 小米推送注销
            if (OsUtils.isMiui()) {
                MiPushClient.unregisterPush(context);
                Log.d("小米推送", "已调用unregisterPush，regID将失效");
            }
            
            // 其他厂商推送注销可根据需要添加
            // 注意：大部分厂商推送没有明确的unregister方法
            
            Log.d("推送注销", "所有推送服务注销完成");
        } catch (Exception e) {
            Log.e("推送注销", "注销推送服务异常: " + e.getMessage());
        }
    }

    private void initOPPO() {
        Context context = mContext != null ? mContext.get() : null;
        if (context == null) return;
        HeytapPushManager.init(context, true);
        new Thread(() -> HeytapPushManager.register(context, PushKeys.oppoAppKey, PushKeys.oppoAppSecret, new ICallBackResultService() {
            @Override
            public void onRegister(int i, String s) {
                if (i == 0) {
                    // 注册成功
                    Log.e("tu推送ID", HeytapPushManager.getRegisterID());
                    PushModel.getInstance().registerDeviceToken(s, WKPushApplication.getInstance().pushBundleID,"");
                }
            }

            @Override
            public void onUnRegister(int i) {

            }

            @Override
            public void onSetPushTime(int i, String s) {

            }

            @Override
            public void onGetPushStatus(int i, int i1) {

            }

            @Override
            public void onGetNotificationStatus(int i, int i1) {
            }

            @Override
            public void onError(int errorCode, String errorMsg) {
                Log.e("OPPO推送", "注册失败: " + errorCode + " - " + errorMsg);
            }
        })).start();

    }

    private void initVIVO() {
        Context context = mContext != null ? mContext.get() : null;
        if (context == null) return;
        try {
            PushClient.getInstance(context).initialize();
            PushClient.getInstance(context).turnOnPush(state -> {
                // TODO: 开关状态处理， 0代表成功
                String regId = PushClient.getInstance(context).getRegId();
                if (!TextUtils.isEmpty(regId)) {
                    Log.e("获取vivopush", regId);
                    PushModel.getInstance().registerDeviceToken(regId, pushBundleID,"");
                }
            });

        } catch (VivoPushException e) {
            Log.e("Vivo推送", "初始化失败: " + e.getMessage());
        }
    }
    
    private void initHonor() {
        // 荣耀推送SDK自动初始化，无需手动操作
        Log.e("荣耀推送", "荣耀推送SDK已自动初始化");
    }

    private void addListener() {
        EndpointManager.getInstance().setMethod("show_open_notification_dialog", object -> {
            Context context = (Context) object;
            WKDialogUtils.getInstance().showDialog(context, context.getString(R.string.open_notification_title), context.getString(R.string.open_notification_content), true, "", context.getString(R.string.open_setting), 0, Theme.colorAccount, index -> {
                if (index == 1) {
                    WKOSUtils.openChannelSetting(context, WKConstants.newMsgChannelID);
                }
            });
            return null;
        });
        //注销推送
        EndpointManager.getInstance().setMethod("wk_logout", object -> {
            OsUtils.setBadge(WKBaseApplication.getInstance().getContext(), 0);
            // 按官方文档要求，注销时需要unregisterPush（使regID失效）
            unregisterAllPushServices();
            return null;
        });

        //设置桌面红点数量
        EndpointManager.getInstance().setMethod("push_update_device_badge", object -> {
            int num = (int) object;
            PushModel.getInstance().registerBadge(num);
            OsUtils.setBadge(WKBaseApplication.getInstance().getContext(), num);
            return null;
        });
    }

    private void getPushToken() {
        Context context = mContext != null ? mContext.get() : null;
        if (context == null) return;
        // 优先使用厂商推送，确保推送的可靠性和稳定性
        if (OsUtils.isEmui()) {
            new Thread(() -> getHuaWeiToken(context)).start();
        } else if (OsUtils.isMiui()) {
            initXiaoMiPush(context);
        } else if (OsUtils.isOppo()) {
            initOPPO();
        } else if (OsUtils.isVivo()) {
            initVIVO();
        } else if (OsUtils.isHonor()) {
            initHonor();
        } else {
            // 仅在非厂商设备上使用Firebase作为备用推送
            int statusCode = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context);
            Log.e("google play services", statusCode + "");
            if (statusCode == ConnectionResult.SUCCESS) {
                FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task1 -> {
                    if (!task1.isSuccessful()) {
                        Log.e("获取FCM令牌错误", "-->");
                        Log.w("Firebase", "Fetching FCM registration token failed", task1.getException());
                        return;
                    }
                    // Get new FCM registration token
                    String token = task1.getResult();
                    Log.e("获取到FCM令牌", token);
                    PushModel.getInstance().registerDeviceToken(token, pushBundleID,"FIREBASE");
                });
            }
        }
    }

    private static void notifyChannel(Application context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String channelId = WKConstants.newMsgChannelID;
            String channelName = "Default_Channel";
            String channelDescription = "this is default channel!";
            NotificationChannel mNotificationChannel = new NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_DEFAULT);
            mNotificationChannel.setDescription(channelDescription);
            ((NotificationManager) context.getSystemService(Activity.NOTIFICATION_SERVICE)).createNotificationChannel(mNotificationChannel);
        }
    }
}
