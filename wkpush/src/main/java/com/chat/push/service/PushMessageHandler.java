package com.chat.push.service;

import android.text.TextUtils;
import android.util.Log;

import com.chat.base.endpoint.EndpointManager;

/**
 * 统一的推送消息处理器
 * 所有推送服务的消息都通过这个类统一处理
 */
public class PushMessageHandler {
    
    private static final String TAG = "PushMessageHandler";
    
    private PushMessageHandler() {}
    
    private static class HandlerHolder {
        static final PushMessageHandler INSTANCE = new PushMessageHandler();
    }
    
    public static PushMessageHandler getInstance() {
        return HandlerHolder.INSTANCE;
    }
    
    /**
     * 统一处理推送消息并显示通知
     * @param pushService 推送服务名称（如"小米推送"、"华为推送"等）
     * @param title 消息标题
     * @param content 消息内容
     */
    public void handlePushMessage(String pushService, String title, String content) {
        Log.d(TAG, "收到" + pushService + "消息: " + content);
        
        if (TextUtils.isEmpty(content)) {
            Log.w(TAG, pushService + "消息内容为空，忽略处理");
            return;
        }
        
        // 标题为空时使用默认标题
        if (TextUtils.isEmpty(title)) {
            title = "新消息";
        }
        
        // 通过模块化方式调用通知显示
        String[] params = new String[]{title, content};
        EndpointManager.getInstance().invoke("show_push_notification", params);
        
        Log.d(TAG, pushService + "消息处理完成");
    }
    
    /**
     * 仅处理消息内容的便捷方法
     * @param pushService 推送服务名称
     * @param content 消息内容
     */
    public void handlePushMessage(String pushService, String content) {
        handlePushMessage(pushService, null, content);
    }
}
