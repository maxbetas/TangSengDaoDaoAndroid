package com.chat.push.push;

import android.content.Context;
import android.text.TextUtils;

import com.chat.base.utils.WKReader;
import com.chat.push.WKPushApplication;
import com.chat.push.service.PushModel;
import com.chat.push.service.PushMessageHandler;
import com.xiaomi.mipush.sdk.ErrorCode;
import com.xiaomi.mipush.sdk.MiPushClient;
import com.xiaomi.mipush.sdk.MiPushCommandMessage;
import com.xiaomi.mipush.sdk.MiPushMessage;
import com.xiaomi.mipush.sdk.PushMessageReceiver;

import java.util.List;

/**
 * 2020-03-09 11:51
 * 小米推送广播
 */
public class XiaoMiMessageReceiver extends PushMessageReceiver {
    private String mRegId;

    @Override
    public void onReceivePassThroughMessage(Context context, MiPushMessage message) {
        if (message != null && !TextUtils.isEmpty(message.getContent())) {
            PushMessageHandler.getInstance().handlePushMessage("小米推送", message.getTitle(), message.getContent());
        }
    }

    @Override
    public void onNotificationMessageClicked(Context context, MiPushMessage message) {
        // 点击处理由系统PendingIntent自动完成
    }

    @Override
    public void onNotificationMessageArrived(Context context, MiPushMessage message) {
        if (message != null && !TextUtils.isEmpty(message.getContent())) {
            PushMessageHandler.getInstance().handlePushMessage("小米推送", message.getTitle(), message.getContent());
        }
    }

    @Override
    public void onCommandResult(Context context, MiPushCommandMessage message) {
        if (MiPushClient.COMMAND_REGISTER.equals(message.getCommand()) && 
            message.getResultCode() == ErrorCode.SUCCESS) {
            List<String> arguments = message.getCommandArguments();
            if (WKReader.isNotEmpty(arguments)) {
                mRegId = arguments.get(0);
                if (!TextUtils.isEmpty(mRegId) && !TextUtils.isEmpty(WKPushApplication.getInstance().pushBundleID)) {
                    PushModel.getInstance().registerDeviceToken(mRegId, WKPushApplication.getInstance().pushBundleID, "");
                }
            }
        }
    }

    @Override
    public void onReceiveRegisterResult(Context context, MiPushCommandMessage message) {
        if (MiPushClient.COMMAND_REGISTER.equals(message.getCommand()) && 
            message.getResultCode() == ErrorCode.SUCCESS) {
            List<String> arguments = message.getCommandArguments();
            if (WKReader.isNotEmpty(arguments)) {
                mRegId = arguments.get(0);
                if (!TextUtils.isEmpty(mRegId) && !TextUtils.isEmpty(WKPushApplication.getInstance().pushBundleID)) {
                    PushModel.getInstance().registerDeviceToken(mRegId, WKPushApplication.getInstance().pushBundleID, "");
                }
            }
        }
    }
}