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
    private String mTopic;
    private String mAlias;
    private String mUserAccount;

    @Override
    public void onReceivePassThroughMessage(Context context, MiPushMessage message) {
        if (message != null && !TextUtils.isEmpty(message.getContent())) {
            // 按官方文档标准记录topic、alias、userAccount信息
            updateMessageInfo(message);
            PushMessageHandler.getInstance().handlePushMessage("小米推送", message.getTitle(), message.getContent());
        }
    }

    @Override
    public void onNotificationMessageClicked(Context context, MiPushMessage message) {
        // 按官方文档标准记录消息信息
        if (message != null) {
            updateMessageInfo(message);
            android.util.Log.d("小米推送", "通知消息被点击: " + message.getContent());
        }
        // 点击处理由系统PendingIntent自动完成
    }

    @Override
    public void onNotificationMessageArrived(Context context, MiPushMessage message) {
        // 通知消息到达回调：虽然系统会自动显示通知，但我们仍需处理自定义逻辑
        if (message != null && !TextUtils.isEmpty(message.getContent())) {
            // 按官方文档标准记录消息信息
            updateMessageInfo(message);
            android.util.Log.d("小米推送", "通知消息已到达: " + message.getContent());
            // 保持与其他厂商推送一致的处理逻辑，确保统一的用户体验
            PushMessageHandler.getInstance().handlePushMessage("小米推送", message.getTitle(), message.getContent());
        }
    }

    @Override
    public void onCommandResult(Context context, MiPushCommandMessage message) {
        String command = message.getCommand();
        List<String> arguments = message.getCommandArguments();
        String cmdArg1 = (WKReader.isNotEmpty(arguments) ? arguments.get(0) : null);
        
        if (MiPushClient.COMMAND_REGISTER.equals(command)) {
            if (message.getResultCode() == ErrorCode.SUCCESS) {
                mRegId = cmdArg1;
                android.util.Log.d("小米推送", "注册成功，RegId: " + mRegId);
                if (!TextUtils.isEmpty(mRegId) && !TextUtils.isEmpty(WKPushApplication.getInstance().pushBundleID)) {
                    PushModel.getInstance().registerDeviceToken(mRegId, WKPushApplication.getInstance().pushBundleID, "");
                }
            } else {
                // 按官方文档处理常见错误
                handleRegistrationError(message.getResultCode(), message.getReason());
            }
        }
        // 可以在这里处理其他命令，如设置别名、标签等
    }

    @Override
    public void onReceiveRegisterResult(Context context, MiPushCommandMessage message) {
        // 专门的注册结果回调，主要用于日志记录
        // 实际的token注册已在onCommandResult中处理，避免重复
        if (MiPushClient.COMMAND_REGISTER.equals(message.getCommand())) {
            if (message.getResultCode() == ErrorCode.SUCCESS) {
                android.util.Log.d("小米推送", "注册结果回调成功");
            } else {
                android.util.Log.e("小米推送", "注册结果回调失败: " + message.getReason());
            }
        }
    }
    
    /**
     * 按官方文档标准更新消息相关信息
     * @param message 推送消息对象
     */
    private void updateMessageInfo(MiPushMessage message) {
        if (message == null) return;
        
        // 按官方文档示例，优先级：topic > alias > userAccount
        if (!TextUtils.isEmpty(message.getTopic())) {
            mTopic = message.getTopic();
        } else if (!TextUtils.isEmpty(message.getAlias())) {
            mAlias = message.getAlias();
        } else if (!TextUtils.isEmpty(message.getUserAccount())) {
            mUserAccount = message.getUserAccount();
        }
    }
    
    /**
     * 按官方文档处理注册错误
     * @param resultCode 错误码
     * @param reason 错误原因
     */
    private void handleRegistrationError(long resultCode, String reason) {
        if (resultCode == 70000001) {
            // 按官方文档：错误码70000001一般是由于网络问题导致的
            android.util.Log.e("小米推送", "注册失败 - 网络连接问题 (70000001): " + reason);
            android.util.Log.e("小米推送", "请检查：1.设备网络是否正常 2.是否使用了代理 3.系统时间是否正确");
            
            // 可以考虑延迟重试注册
            // TODO: 可根据业务需求添加重试机制
        } else {
            android.util.Log.e("小米推送", "注册失败 - 错误码: " + resultCode + ", 原因: " + reason);
        }
    }
}