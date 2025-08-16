package com.chat.push.push;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import com.chat.base.WKBaseApplication;
import com.chat.base.utils.WKDeviceUtils;
import com.chat.push.WKPushApplication;
import com.chat.push.service.PushModel;
import com.chat.push.service.PushMessageHandler;
import com.huawei.hms.push.HmsMessageService;
import com.huawei.hms.push.RemoteMessage;

/**
 * 2020-03-08 22:10
 * 华为推送服务
 */
public class HuaweiHmsMessageService extends HmsMessageService {
    @Override
    public void onNewToken(String s, Bundle bundle) {
        super.onNewToken(s, bundle);
        if (!TextUtils.isEmpty(s)) {
            PushModel.getInstance().registerDeviceToken(s, WKPushApplication.getInstance().pushBundleID,"");
        }
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        Log.e("收到华为推送消息",remoteMessage.getData());
        
        // 统一处理推送消息
        String title = null;
        String content = null;
        
        // 优先处理notification类型消息
        if (remoteMessage.getNotification() != null) {
            title = remoteMessage.getNotification().getTitle();
            content = remoteMessage.getNotification().getBody();
        }
        // 如果没有notification，尝试从data中获取
        else if (remoteMessage.getDataOfMap() != null && !remoteMessage.getDataOfMap().isEmpty()) {
            title = remoteMessage.getDataOfMap().get("title");
            content = remoteMessage.getDataOfMap().get("body");
            if (TextUtils.isEmpty(content)) {
                content = remoteMessage.getDataOfMap().get("message");
            }
        }
        
        if (!TextUtils.isEmpty(content)) {
            PushMessageHandler.getInstance().handlePushMessage("华为推送", title, content);
        }
    }
}
