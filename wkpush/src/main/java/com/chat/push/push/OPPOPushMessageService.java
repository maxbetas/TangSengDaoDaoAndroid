package com.chat.push.push;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.heytap.msp.push.mode.DataMessage;
import com.heytap.msp.push.service.CompatibleDataMessageCallbackService;
import com.chat.push.service.PushMessageHandler;

public class OPPOPushMessageService extends CompatibleDataMessageCallbackService {
    /**
     * 透传消息处理，应用可以打开页面或者执行命令,如果应用不需要处理透传消息，则不需要重写此方法
     *
     * @param context
     * @param message
     */
    @Override
    public void processMessage(Context context, DataMessage message) {
        super.processMessage(context.getApplicationContext(), message);
        String content = message.getContent();
        Log.e("收到OPPO推送消息", message.toString());
        
        // 统一处理推送消息
        String title = message.getTitle();
        if (!TextUtils.isEmpty(content)) {
            PushMessageHandler.getInstance().handlePushMessage("OPPO推送", title, content);
        }
    }
}
