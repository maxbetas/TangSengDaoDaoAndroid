package com.chat.push.push;

import android.content.Context;
import android.util.Log;
import android.text.TextUtils;

import com.chat.base.utils.WKDeviceUtils;
import com.chat.push.WKPushApplication;
import com.chat.push.service.PushModel;
import com.chat.push.service.PushMessageHandler;
import com.vivo.push.model.UPSNotificationMessage;
import com.vivo.push.sdk.OpenClientPushMessageReceiver;

public class VivoPushMessageReceiverImpl extends OpenClientPushMessageReceiver {

    @Override
    public void onReceiveRegId(Context context, String regId) {
        super.onReceiveRegId(context, regId);
        Log.e("注册vivo推送",regId);
        PushModel.getInstance().registerDeviceToken(regId, WKPushApplication.getInstance().pushBundleID,"");
    }

    @Override
    public void onNotificationMessageArrived(Context context, UPSNotificationMessage msg) {
        super.onNotificationMessageArrived(context, msg);
        if (msg != null) {
            Log.e("收到Vivo推送消息", msg.getContent());
            
            // 统一处理推送消息
            String title = msg.getTitle();
            String content = msg.getContent();
            if (!TextUtils.isEmpty(content)) {
                PushMessageHandler.getInstance().handlePushMessage("Vivo推送", title, content);
            }
        }
    }

    @Override
    public void onNotificationMessageClicked(Context context, UPSNotificationMessage msg) {
        super.onNotificationMessageClicked(context, msg);
        if (msg != null) {
            Log.e("点击Vivo推送消息", msg.getContent());
            // 点击事件处理，通知已在onNotificationMessageArrived中显示
        }
    }
}
