package com.chat.push.push;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.heytap.msp.push.mode.DataMessage;
import com.heytap.msp.push.service.DataMessageCallbackService;
import com.chat.push.service.PushMessageHandler;

public class OPPOAppPushMessageService extends DataMessageCallbackService {

    /**
     * 透传消息处理，应用可以打开页面或者执行命令,如果应用不需要处理透传消息，则不需要重写此方法
     *
     * @param context
     * @param message
     */
    @Override
    public void processMessage(Context context, DataMessage message) {
        super.processMessage(context, message);
        Log.e("收到OPPO App推送消息", message.toString());
        
        if (!TextUtils.isEmpty(message.getContent())) {
            PushMessageHandler.getInstance().handlePushMessage("OPPO App推送", message.getTitle(), message.getContent());
        }
    }

}
