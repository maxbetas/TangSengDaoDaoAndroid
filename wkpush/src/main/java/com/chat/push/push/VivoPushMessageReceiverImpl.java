package com.chat.push.push;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

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
        if (msg != null && !TextUtils.isEmpty(msg.getContent())) {
            Log.e("收到Vivo推送消息", msg.getContent());
            PushMessageHandler.getInstance().handlePushMessage("Vivo推送", msg.getTitle(), msg.getContent());
        }
    }

    @Override
    public void onNotificationMessageClicked(Context context, UPSNotificationMessage msg) {
        super.onNotificationMessageClicked(context, msg);
        // 点击处理由系统PendingIntent自动完成
    }
}
