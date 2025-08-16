package com.chat.push.push;

import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;

import com.chat.push.WKPushApplication;
import com.chat.push.service.PushModel;
import com.chat.push.service.PushMessageHandler;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class WKFirebaseMessagingService extends FirebaseMessagingService {

    //监控令牌的生成
    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Log.e("获取到FCM令牌111", token);
        if (!TextUtils.isEmpty(token)) {
            PushModel.getInstance().registerDeviceToken(token, WKPushApplication.getInstance().pushBundleID, "FIREBASE");
        }
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage msg) {
        super.onMessageReceived(msg);
        Log.e("收到Firebase推送消息", msg.getFrom());
        
        String title = null;
        String content = null;
        
        // 优先处理notification类型消息
        if (msg.getNotification() != null) {
            title = msg.getNotification().getTitle();
            content = msg.getNotification().getBody();
        }
        // 如果没有notification，尝试从data中获取
        else if (msg.getData() != null && !msg.getData().isEmpty()) {
            title = msg.getData().get("title");
            content = msg.getData().get("body");
            if (TextUtils.isEmpty(content)) {
                content = msg.getData().get("message");
            }
        }
        
        if (!TextUtils.isEmpty(content)) {
            PushMessageHandler.getInstance().handlePushMessage("Firebase推送", title, content);
        }
    }

}
