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
        
        // 按官方文档检查消息大小限制（小米推送最大4KB）
        if (pushService.contains("小米") && !validateMessageSize(title, content)) {
            Log.w(TAG, "小米推送消息过大，可能影响送达率");
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
    
    /**
     * 按官方文档验证消息大小（小米推送限制4KB）
     * @param title 消息标题
     * @param content 消息内容
     * @return true如果消息大小合规
     */
    private boolean validateMessageSize(String title, String content) {
        try {
            // 计算消息总大小（UTF-8编码）
            int titleSize = title != null ? title.getBytes("UTF-8").length : 0;
            int contentSize = content != null ? content.getBytes("UTF-8").length : 0;
            int totalSize = titleSize + contentSize;
            
            // 小米推送限制4KB = 4096字节
            final int MAX_SIZE = 4096;
            
            if (totalSize > MAX_SIZE) {
                Log.w(TAG, "消息大小超限: " + totalSize + "字节 > " + MAX_SIZE + "字节");
                return false;
            }
            
            return true;
        } catch (Exception e) {
            Log.w(TAG, "消息大小检查异常: " + e.getMessage());
            return true; // 异常时不阻止消息处理
        }
    }
}
