package rtc.utils;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.widget.ImageView;

import androidx.annotation.Nullable;


import java.util.ArrayList;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

import rtc.WKRTCApplication;
import rtc.WKRTCCallType;
import rtc.conference.MeetingActivity;
import rtc.conference.MultiCallWaitingAnswerActivity;
import rtc.inters.IAvatarLoader;
import rtc.inters.IChooseMembers;
import rtc.inters.ILocalListener;
import rtc.inters.ISendAckMsg;
import rtc.inters.ISendMsgListener;
import rtc.inters.ITimerListener;
import rtc.p2p.CallActivity;
import rtc.p2p.P2PVideoCallWaitingAnswerActivity;

/**
 * 5/18/21 11:37 AM
 * 音视频管理
 * 修复线程安全问题，统一多人通话架构
 */
public class WKRTCManager {
    private final String tag = "WKRTCManager";

    private WKRTCManager() {

    }

    // 使用 volatile 确保内存可见性
    public volatile boolean isShowAnimation = false;
    public volatile boolean isCalling = false;

    // 用于线程安全的锁对象
    private final Object callStateLock = new Object();

    private static final class LiMRTCManagerBinder {
        static final WKRTCManager manager = new WKRTCManager();
    }

    public static WKRTCManager getInstance() {
        return LiMRTCManagerBinder.manager;
    }

    /**
     * 创建多人视频通话
     * 线程安全：使用同步锁保护状态变更
     */
    public void createMultiCall(String channelID, byte channelType, String loginUID, ArrayList<String> uids, String token, String roomID) {
        synchronized (callStateLock) {
            if (isCalling) {
                WKLogger.w(tag, "创建多人通话失败：正在通话中");
                return;
            }
            
            // 安全复制列表，避免修改原始数据
            ArrayList<String> safeUids = new ArrayList<>(uids);
            // 移除自己的UID（如果存在），兼容旧版本API
            for (int i = safeUids.size() - 1; i >= 0; i--) {
                if (safeUids.get(i).equals(loginUID)) {
                    safeUids.remove(i);
                }
            }
            // 将自己添加到第一位
            safeUids.add(0, loginUID);
            
            isShowAnimation = true;
            Intent intent = new Intent(WKRTCApplication.getInstance().getContext(), MeetingActivity.class);
            intent.putExtra("token", token);
            intent.putExtra("channelID", channelID);
            intent.putExtra("channelType", channelType);
            intent.putExtra("loginUID", loginUID);
            intent.putExtra("roomID", roomID);
            intent.putStringArrayListExtra("uids", safeUids);
            intent.addFlags(FLAG_ACTIVITY_NEW_TASK);
            WKRTCApplication.getInstance().getContext().startActivity(intent);
            isCalling = true;
            
            WKLogger.i(tag, "创建多人通话成功，参与者数量：" + safeUids.size());
        }
    }

    /**
     * 加入多人视频通话
     * 线程安全：使用同步锁保护状态变更
     */
    public void joinMultiCall(String channelID, byte channelType, String inviterUID, String inviterName, String loginUID, ArrayList<String> uids, String token, String roomID) {
        synchronized (callStateLock) {
            if (isCalling) {
                WKLogger.w(tag, "加入多人通话失败：正在通话中");
                return;
            }
            
            isShowAnimation = false;
            Intent intent = new Intent(WKRTCApplication.getInstance().getContext(), MultiCallWaitingAnswerActivity.class);
            intent.putExtra("token", token);
            intent.putExtra("channelID", channelID);
            intent.putExtra("channelType", channelType);
            intent.putExtra("fromUID", inviterUID);
            intent.putExtra("fromName", inviterName);
            intent.putExtra("loginUID", loginUID);
            intent.putExtra("roomID", roomID);
            intent.putStringArrayListExtra("uids", uids != null ? new ArrayList<>(uids) : new ArrayList<>());
            intent.addFlags(FLAG_ACTIVITY_NEW_TASK);
            WKRTCApplication.getInstance().getContext().startActivity(intent);
            isCalling = true;
            
            WKLogger.i(tag, "加入多人通话，邀请者：" + inviterUID);
        }
    }

    /**
     * 接听P2P通话
     * 线程安全：使用同步锁保护状态变更
     */
    public void joinP2PCall(String inviterUID, String inviterName, String loginUID, int callType) {
        synchronized (callStateLock) {
            if (isCalling) {
                WKLogger.w(tag, "接听P2P通话失败：正在通话中");
                return;
            }
            
            isCalling = true;
            Intent intent = new Intent(WKRTCApplication.getInstance().getContext(), CallActivity.class);
            intent.putExtra("toUID", inviterUID);
            intent.putExtra("callType", callType);
            intent.putExtra("toName", inviterName);
            intent.putExtra("isCreate", false);
            intent.putExtra("loginUID", loginUID);
            intent.addFlags(FLAG_ACTIVITY_NEW_TASK);
            WKRTCApplication.getInstance().getContext().startActivity(intent);
            
            WKLogger.i(tag, "接听P2P通话：" + inviterUID + "，类型：" + callType);
        }
    }

    /**
     * 创建P2P通话
     * 线程安全：使用同步锁保护状态变更
     */
    public void createP2PCall(String loginUID, String toUID, String toName, int callType) {
        synchronized (callStateLock) {
            if (isCalling) {
                WKLogger.w(tag, "创建P2P通话失败：正在通话中");
                return;
            }
            
            isCalling = true;
            Intent intent = new Intent(WKRTCApplication.getInstance().getContext(), CallActivity.class);
            intent.putExtra("callType", callType);
            intent.putExtra("loginUID", loginUID);
            intent.putExtra("toUID", toUID);
            intent.putExtra("toName", toName);
            intent.putExtra("isCreate", true);
            intent.addFlags(FLAG_ACTIVITY_NEW_TASK);
            WKRTCApplication.getInstance().getContext().startActivity(intent);
            
            WKLogger.i(tag, "创建P2P通话：" + toUID + "，类型：" + callType);
        }
    }


    //    private ISaveMsgListener iSaveMsgListener;
    private ISendMsgListener iSendMsgListener;

    public ISendMsgListener getSendMsgListener() {
        if (iSendMsgListener == null) {
            WKLogger.e(tag, "未注册发送消息回掉");
        }
        return iSendMsgListener;
    }

//    public ISaveMsgListener getSaveMsgListener() {
//        return iSaveMsgListener;
//    }

    public void addOnSendMsgListener(ISendMsgListener iSendMsgListener) {
        this.iSendMsgListener = iSendMsgListener;
    }

//    public void addOnSaveMsgListener(ISaveMsgListener iSaveMsgListener) {
//        this.iSaveMsgListener = iSaveMsgListener;
//    }

    public void sendMsgAck(long clientSeq, boolean isSuccess) {
        if (iSendAckMsg != null) iSendAckMsg.msgAck(clientSeq, isSuccess);
    }

    ISendAckMsg iSendAckMsg;

    public void addMsgAck(ISendAckMsg iSendAckMsg) {
        this.iSendAckMsg = iSendAckMsg;
    }

    private ILocalListener localListener;

    public ILocalListener getIRTCListener() {
        return localListener;
    }

    public void addLocalListener(ILocalListener localListener) {
        this.localListener = localListener;
    }

    public void onAccept(String uid, int callType) {
        if (this.localListener != null)
            this.localListener.onAccept(uid, callType);
    }

    public void receivedRTCMsg(String uid, String message) {
        if (localListener != null) localListener.onReceivedRTCMsg(uid, message);
    }

    public void onHangUp(String channelID, byte channelType, int second) {
        if (localListener != null) localListener.onHangUp(channelID, channelType, second);
        synchronized (callStateLock) {
            isCalling = false;
        }
        WKLogger.i(tag, "通话结束：" + channelID + "，时长：" + second + "秒");
    }

    public void onMultiRefuse(String roomID, String uid) {
        if (localListener != null) localListener.onMultiRefuse(roomID, uid);
    }

    public void onSwitchAudio(String uid) {
        if (localListener != null) {
            localListener.onSwitchAudio(uid);
            // TODO: 6/11/21
            WKFloatingViewManager.getInstance().onSwitchAudio();
        }
    }

    public void onSwitchVideoRequest(String uid) {
        if (localListener != null) {
            localListener.onRequestSwitchVideo(uid);
            //LiMFloatingViewManager.getInstance().onSwitchAudio();
        }
    }

    public void onSwitchVideoRespond(String uid, int status) {
        if (localListener != null) {
            localListener.onSwitchVideoRespond(uid, status);
        }
    }

    public void onRefuse(String channelID, byte channelType, String uid) {
        RTCAudioPlayer.getInstance().stopPlay();
        if (localListener != null) localListener.onRefuse(channelID, channelType, uid);
        synchronized (callStateLock) {
            isCalling = false;
        }
        WKLogger.i(tag, "通话拒绝：" + channelID + "，用户：" + uid);
    }

    public void onCancel(String uid) {
        if (localListener != null) {
            localListener.onCancel(uid);
        }
        synchronized (callStateLock) {
            isCalling = false;
        }
        WKLogger.i(tag, "通话取消：" + uid);
    }

    /**
     * 线程安全地设置通话状态
     * 避免外部直接访问锁对象
     */
    public void setCallingState(boolean calling) {
        synchronized (callStateLock) {
            isCalling = calling;
        }
    }

    private IAvatarLoader iAvatarLoader;

    public void addOnAvatarLoader(IAvatarLoader iAvatarLoader) {
        this.iAvatarLoader = iAvatarLoader;
    }

    public void showAvatar(@Nullable Context context, String uid, ImageView imageView, int width, boolean isP2PCall) {
        if (iAvatarLoader != null) {
            iAvatarLoader.onAvatarLoader(context, uid, imageView, width, isP2PCall);
        } else {
            WKLogger.e(tag, "未注册加载图片事件");
        }
    }

    private IChooseMembers iChooseMembers;

    public IChooseMembers getIChooseMembers() {
        return iChooseMembers;
    }

    public void addChooseMembers(IChooseMembers iChooseMembers) {
        this.iChooseMembers = iChooseMembers;
    }


    // 公用计时器
    private Timer timer;
    private long totalDuration;

    public void startTimer() {
        totalDuration = 0;
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                totalDuration += 1000;
                getTotalTime(totalDuration);
            }
        }, 0, 1000);
    }

    public void stopTimer() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    private ConcurrentHashMap<String, ITimerListener> timerListenerList;

    public void addTimerListener(String key, ITimerListener timerListener) {
        if (timerListener == null || TextUtils.isEmpty(key)) return;
        if (timerListenerList == null) timerListenerList = new ConcurrentHashMap<>();
        timerListenerList.put(key, timerListener);
    }

    public String getTotalTime(long totalDuration) {
        long minute = totalDuration / 1000 / 60;
        long second = (totalDuration / 1000) % 60;
        String showM = String.valueOf(minute);
        if (minute < 10) {
            showM = "0" + minute;
        }
        String showS = String.valueOf(second);
        if (second < 10) {
            showS = "0" + second;
        }
        String finalShowM = showM;
        String finalShowS = showS;
        String timeStr = String.format("%s:%s", finalShowM, finalShowS);
        if (timerListenerList != null && timerListenerList.size() > 0) {
            for (Map.Entry<String, ITimerListener> entry : timerListenerList.entrySet()) {
                entry.getValue().onTimeChanged(totalDuration, timeStr);
            }
        }
        return timeStr;
    }

    public void removeTimeListener(String key) {
        if (timerListenerList != null && !TextUtils.isEmpty(key)) {
            timerListenerList.remove(key);
        }
    }

}
