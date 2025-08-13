package com.chat.push.push;

import android.text.TextUtils;
import android.util.Log;
import com.chat.push.WKPushApplication;
import com.chat.push.service.PushModel;
import com.hihonor.push.sdk.HonorPushMessageService;
import com.hihonor.push.sdk.HonorPushDataMessage;

/**
 * 荣耀推送消息服务
 * 参考官方文档：https://developer.honor.com/cn/kitdoc?category=base&kitId=11002&navigation=guides&docId=sdk-overview.md
 */
public class HonorPushMessageServiceImpl extends HonorPushMessageService {
    
    @Override
    public void onNewToken(String token) {
        super.onNewToken(token);
        if (!TextUtils.isEmpty(token)) {
            Log.e("荣耀推送新Token", token);
            PushModel.getInstance().registerDeviceToken(token, WKPushApplication.getInstance().pushBundleID, "HONOR");
        }
    }
    
    @Override
    public void onMessageReceived(HonorPushDataMessage message) {
        super.onMessageReceived(message);
        if (message != null) {
            Log.e("收到荣耀推送消息", message.getData());
        }
    }
    

    
    @Override
    public void onTokenError(Exception exception) {
        super.onTokenError(exception);
        Log.e("荣耀推送Token错误", exception.getMessage());
    }
}
