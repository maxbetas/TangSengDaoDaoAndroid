package com.chat.push.push;

import android.text.TextUtils;
import android.util.Log;
import com.chat.push.WKPushApplication;
import com.chat.push.service.PushModel;
import com.hihonor.push.sdk.HonorMessageService;
import com.hihonor.push.sdk.HonorPushDataMsg;

/**
 * 荣耀推送消息服务
 */
public class HonorPushMessageServiceImpl extends HonorMessageService {
    
    @Override
    public void onNewToken(String token) {
        super.onNewToken(token);
        if (!TextUtils.isEmpty(token)) {
            Log.e("荣耀推送新Token", token);
            PushModel.getInstance().registerDeviceToken(token, WKPushApplication.getInstance().pushBundleID, "HONOR");
        }
    }
    
    @Override
    public void onMessageReceived(HonorPushDataMsg message) {
        super.onMessageReceived(message);
        if (message != null) {
            Log.e("收到荣耀推送消息", message.getData());
        }
    }

}
