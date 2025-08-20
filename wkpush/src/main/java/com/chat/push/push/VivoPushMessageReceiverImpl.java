package com.chat.push.push;

import android.content.Context;
import android.util.Log;

import com.chat.base.utils.WKDeviceUtils;
import com.chat.push.WKPushApplication;
import com.chat.push.service.PushModel;
import com.vivo.push.model.UPSNotificationMessage;
import com.vivo.push.sdk.OpenClientPushMessageReceiver;

public class VivoPushMessageReceiverImpl extends OpenClientPushMessageReceiver {

    @Override
    public void onReceiveRegId(Context context, String regId) {
        super.onReceiveRegId(context, regId);
        Log.d("VivoPush", "VIVO推送注册成功，RegId: " + regId);
        if (!android.text.TextUtils.isEmpty(regId)) {
            PushModel.getInstance().registerDeviceToken(regId, WKPushApplication.getInstance().pushBundleID,"");
        } else {
            Log.e("VivoPush", "VIVO推送RegId为空");
        }
    }

    @Override
    public void onNotificationMessageClicked(Context context, UPSNotificationMessage msg) {
        super.onNotificationMessageClicked(context, msg);
        Log.d("VivoPush", "VIVO推送通知被点击: " + (msg != null ? msg.toString() : "null"));
    }
    
    @Override
    public void onNotificationMessageArrived(Context context, UPSNotificationMessage msg) {
        super.onNotificationMessageArrived(context, msg);
        Log.d("VivoPush", "VIVO推送通知到达: " + (msg != null ? msg.toString() : "null"));
    }
    
    @Override
    public void onReceiveRegId(Context context, String regId, String packageName, String sdkVersion) {
        super.onReceiveRegId(context, regId, packageName, sdkVersion);
        Log.d("VivoPush", "VIVO推送注册(详细): RegId=" + regId + ", Package=" + packageName + ", SDK=" + sdkVersion);
        if (!android.text.TextUtils.isEmpty(regId)) {
            PushModel.getInstance().registerDeviceToken(regId, WKPushApplication.getInstance().pushBundleID,"");
        }
    }
}
