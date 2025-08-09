package rtc.utils;

import static org.webrtc.PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY;
import static owt.base.MediaCodecs.AudioCodec.OPUS;
import static owt.base.MediaCodecs.AudioCodec.PCMU;
import static owt.base.MediaCodecs.VideoCodec.H264;
import static owt.base.MediaCodecs.VideoCodec.VP8;
import static owt.base.MediaCodecs.VideoCodec.VP9;

import android.os.CountDownTimer;
import android.text.TextUtils;
import android.util.Log;

import rtc.WKRTCApplication;
import com.yhao.floatwindow.FloatWindow;

import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.PeerConnection;
import org.webrtc.RTCStatsReport;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import owt.base.ActionCallback;
import owt.base.AudioCodecParameters;
import owt.base.LocalStream;
import owt.base.MediaConstraints;
import owt.base.OwtError;
import owt.base.VideoEncodingParameters;
import owt.conference.ConferenceClient;
import owt.conference.ConferenceClientConfiguration;
import owt.conference.ConferenceInfo;
import owt.conference.Participant;
import owt.conference.Publication;
import owt.conference.PublishOptions;
import owt.conference.RemoteMixedStream;
import owt.conference.RemoteStream;
import owt.conference.SubscribeOptions;
import owt.conference.Subscription;
import owt.utils.OwtVideoCapturer;
import rtc.conference.HttpUtils;
import rtc.conference.ItemRemoteStream;
import rtc.conference.WKRTCStatusReport;
import rtc.inters.ILocalListener;

/**
 * 多人会议数据提供者
 * 负责WebRTC会议的生命周期管理、资源清理和状态同步
 * 
 * 架构说明：
 * - 使用 OWT (Open WebRTC Toolkit) 作为 WebRTC 封装层
 * - 统一的资源管理和错误恢复机制
 * - 支持网络异常自动重连（最多3次）
 * - 优化的编解码器配置（H264/VP8 视频，OPUS/PCMU 音频）
 * 
 * 技术栈：
 * - WebRTC: 基于本地 libwebrtc.jar（建议升级到最新版本）
 * - OWT: Intel Open WebRTC Toolkit（已停止维护，建议迁移到标准 WebRTC）
 * - 视频分辨率: 720p@30fps（可根据网络情况自适应）
 * 
 * 后续优化建议：
 * 1. 迁移到 Google 官方 WebRTC Android SDK
 * 2. 添加网络质量监控和自适应码率
 * 3. 支持 VP9/AV1 等新编解码器
 * 4. 实现 SFU 模式以提升多人通话性能
 */
public class MeetingDataProvider implements ConferenceClient.ConferenceClientObserver {
    private final String tag = "MeetingDataProvider";

    private MeetingDataProvider() {
    }

    // 标记是否已初始化，防止重复初始化
    private volatile boolean isInitialized = false;
    // 标记是否正在清理资源，防止重复清理
    private volatile boolean isCleaningUp = false;
    // 重连次数计数
    private int reconnectAttempts = 0;
    // 最大重连次数
    private static final int MAX_RECONNECT_ATTEMPTS = 3;

    @Override
    public void onStreamAdded(RemoteStream remoteStream) {
        addRemoteStream(remoteStream, null);
    }

    @Override
    public void onParticipantJoined(Participant participant) {
        String uid = participant.userId;
        boolean isAdd = true;
        for (int j = 0; j < list.size(); j++) {
            if (!TextUtils.isEmpty(list.get(j).uid)
                    && !TextUtils.isEmpty(uid) && list.get(j).uid.equals(uid)) {
                list.get(j).participant = participant;
                isAdd = false;
                break;
            }
        }
        if (isAdd) {
            list.add(new ItemRemoteStream(uid, null, participant));
            iMeetingListener.addItem(null, uid);
        }
    }

    @Override
    public void onMessageReceived(String message, String from, String to) {
        if (!TextUtils.isEmpty(message)) {
            try {
                JSONObject jsonObject = new JSONObject(message);
                String cmd = "";
                if (jsonObject.has("cmd")) {
                    cmd = jsonObject.optString("cmd");
                }
                if (TextUtils.isEmpty(cmd)) return;
                if (cmd.equals("removeParticipant")) {
                    String uid = jsonObject.optString("uid");
                    for (int i = 0, size = list.size(); i < size; i++) {
                        if (!TextUtils.isEmpty(uid) && !TextUtils.isEmpty(list.get(i).uid) && list.get(i).uid.equals(uid)) {
                            list.remove(i);
                            iMeetingListener.removeView(uid);
                            if (list.size() == 1) {
                                hangUp();
                            }
                            break;
                        }
                    }

                }
            } catch (JSONException e) {
                e.printStackTrace();
            }

        }
    }

    @Override
    public void onServerDisconnected() {
        WKLogger.i(tag, "服务器断开连接，开始清理资源");
        cleanupResources();
        if (iMeetingListener != null) {
            iMeetingListener.hangup();
        }
    }

    /**
     * 统一的资源清理方法
     * 线程安全，防止重复清理
     */
    private void cleanupResources() {
        synchronized (this) {
            if (isCleaningUp) {
                WKLogger.w(tag, "正在清理资源，跳过重复清理");
                return;
            }
            isCleaningUp = true;
        }

        try {
            // 停止计时器
            WKRTCManager.getInstance().stopTimer();
            
            // 清理状态报告定时器
            if (checkRTCStatusReportTimer != null) {
                checkRTCStatusReportTimer.cancel();
                checkRTCStatusReportTimer = null;
            }

            // 销毁悬浮窗
            try {
                FloatWindow.destroy();
            } catch (Exception e) {
                WKLogger.e(tag, "销毁悬浮窗失败", e);
            }

            // 清理本地媒体资源
            cleanupLocalMedia();

            // 清理远程流资源
            cleanupRemoteStreams();

            // 清理计时器
            cleanupTimers();

            // 清理会议客户端
            cleanupConferenceClient();

            // 重置状态
            isInitialized = false;

            WKLogger.i(tag, "资源清理完成");
        } catch (Exception e) {
            WKLogger.e(tag, "资源清理过程中发生异常", e);
        } finally {
            isCleaningUp = false;
        }
    }

    /**
     * 清理本地媒体资源
     */
    private void cleanupLocalMedia() {
        if (capturer != null) {
            try {
                capturer.stopCapture();
                capturer.dispose();
            } catch (Exception e) {
                WKLogger.e(tag, "清理摄像头失败", e);
            } finally {
                capturer = null;
            }
        }

        if (localStream != null) {
            try {
                localStream.dispose();
            } catch (Exception e) {
                WKLogger.e(tag, "清理本地流失败", e);
            } finally {
                localStream = null;
            }
        }

        if (publication != null) {
            try {
                publication.stop();
            } catch (Exception e) {
                WKLogger.e(tag, "停止发布失败", e);
            } finally {
                publication = null;
            }
        }
    }

    /**
     * 清理远程流资源
     */
    private void cleanupRemoteStreams() {
        if (list != null && !list.isEmpty()) {
            for (ItemRemoteStream item : list) {
                if (item.remoteStream != null) {
                    try {
                        item.remoteStream.disableAudio();
                    } catch (Exception e) {
                        WKLogger.e(tag, "清理远程流失败", e);
                    } finally {
                        item.remoteStream = null;
                    }
                }
            }
            list.clear();
        }

        if (bigRemoteStream != null) {
            try {
                bigRemoteStream.disableAudio();
            } catch (Exception e) {
                WKLogger.e(tag, "清理大视图远程流失败", e);
            } finally {
                bigRemoteStream = null;
            }
        }
    }

    /**
     * 清理所有计时器
     */
    private void cleanupTimers() {
        if (timerMap != null && !timerMap.isEmpty()) {
            for (Map.Entry<String, CountDownTimer> entry : timerMap.entrySet()) {
                if (entry.getValue() != null) {
                    try {
                        entry.getValue().cancel();
                    } catch (Exception e) {
                        WKLogger.e(tag, "取消计时器失败: " + entry.getKey(), e);
                    }
                }
            }
            timerMap.clear();
        }
    }

    /**
     * 清理会议客户端资源
     */
    private void cleanupConferenceClient() {
        if (conferenceClient != null) {
            try {
                conferenceClient.removeObserver(this);
                // 注意：不要调用 conferenceClient.leave()，因为这里是断开连接的回调
            } catch (Exception e) {
                WKLogger.e(tag, "清理会议客户端失败", e);
            } finally {
                conferenceClient = null;
                conferenceInfo = null;
            }
        }
    }

    /**
     * 判断是否为网络相关错误，支持自动重连
     */
    private boolean isNetworkError(OwtError error) {
        if (error == null || error.errorMessage == null) {
            return false;
        }
        
        String errorMsg = error.errorMessage.toLowerCase();
        return errorMsg.contains("network") || 
               errorMsg.contains("connection") || 
               errorMsg.contains("timeout") ||
               errorMsg.contains("socket") ||
               errorMsg.contains("disconnect");
    }

    private static class MultiDataProviderBinder {
        final static MeetingDataProvider provider = new MeetingDataProvider();
    }

    public static MeetingDataProvider getInstance() {
        return MultiDataProviderBinder.provider;
    }

    private IMeetingListener iMeetingListener;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private ConferenceClient conferenceClient;
    private ConferenceInfo conferenceInfo;
    public String roomID, token, loginUID, channelID;
    public byte channelType = 2;
    public LocalStream localStream;
    public OwtVideoCapturer capturer;
    public Publication publication;
    public List<ItemRemoteStream> list;
    public RemoteStream bigRemoteStream;
    private ConcurrentHashMap<String, WKRTCStatusReport> rtcStatusMap;
    //未进入通话的倒计时
    private ConcurrentHashMap<String, CountDownTimer> timerMap;
    private Timer checkRTCStatusReportTimer;

    private void startCheckRTCStatusReportTimer() {
        if (rtcStatusMap != null && list != null && rtcStatusMap.size() > 0 && list.size() > 0) {
            if (checkRTCStatusReportTimer == null) {
                checkRTCStatusReportTimer = new Timer();
            }
            checkRTCStatusReportTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    iMeetingListener.checkRTCStatusReport(rtcStatusMap, list);
                }
            }, 1000, 1000);
        }
    }

    public void addMeetingListener(IMeetingListener iMeetingListener) {
        this.iMeetingListener = iMeetingListener;
    }

    public void init(String loginUID, String channelID, byte channelType, String roomID, String token) {
        this.loginUID = loginUID;
        this.channelID = channelID;
        this.channelType = channelType;
        this.roomID = roomID;
        this.token = token;
    }

    private void initConferenceClient() {
        PeerConnection.IceServer iceServer = PeerConnection.IceServer.builder(
                "turn:" + WKRTCApplication.getInstance().turnIP + ":3478?transport=udp").setUsername("user").setPassword(
                "passwd").createIceServer();
        PeerConnection.IceServer iceServer1 = PeerConnection.IceServer.builder(
                "stun:stun1.l.google.com:19302").createIceServer();
        PeerConnection.IceServer iceServer2 = PeerConnection.IceServer.builder(
                "stun:stun2.l.google.com:19302").createIceServer();
        PeerConnection.IceServer iceServer3 = PeerConnection.IceServer.builder(
                "stun:stunserver.org").createIceServer();
        List<PeerConnection.IceServer> iceServers = new ArrayList<>();
        iceServers.add(iceServer);
        iceServers.add(iceServer1);
        iceServers.add(iceServer2);
        iceServers.add(iceServer3);
        PeerConnection.RTCConfiguration rtcConfiguration = new PeerConnection.RTCConfiguration(
                iceServers);
        HttpUtils.setUpINSECURESSLContext();
        rtcConfiguration.continualGatheringPolicy = GATHER_CONTINUALLY;
        ConferenceClientConfiguration configuration
                = ConferenceClientConfiguration.builder()
                .setHostnameVerifier(HttpUtils.hostnameVerifier)
                .setSSLContext(HttpUtils.sslContext)
                .setRTCConfiguration(rtcConfiguration)
                .build();
        conferenceClient = new ConferenceClient(configuration);
        conferenceClient.addObserver(this);
    }

    public void start() {
        initConferenceClient();
        initListener();
        joinRoom();
    }

    private void initListener() {
        WKRTCManager.getInstance().addLocalListener(new ILocalListener() {

            @Override
            public void onReceivedRTCMsg(String uid, String message) {

            }

            @Override
            public void onHangUp(String channelID, byte channelType, int second) {

            }

            @Override
            public void onRefuse(String channelID, byte channelType, String uid) {
                if (channelID.equals(MeetingDataProvider.this.channelID) && channelType == MeetingDataProvider.this.channelType && !TextUtils.isEmpty(uid)) {
                    removeUser(uid);
                }
            }

            @Override
            public void onSwitchVideoRespond(String uid, int status) {

            }

            @Override
            public void onCancel(String uid) {

            }

            @Override
            public void onPublish() {

            }

            @Override
            public void onMultiRefuse(String roomID, String uid) {
                removeUser(uid);
            }
        });
    }


    private void joinRoomSuccess() {

        executor.execute(() -> {
            if (conferenceClient != null && conferenceClient.info() != null && conferenceClient.info().getRemoteStreams().size() > 0) {

                for (int i = 0; i < conferenceClient.info().getRemoteStreams().size(); i++) {
                    RemoteStream remoteStream = conferenceClient.info().getRemoteStreams().get(i);
                    if (remoteStream instanceof RemoteMixedStream
                            && ((RemoteMixedStream) remoteStream).view.equals("common")) {
                        continue;
                    }

                    String uid = "";
                    if (remoteStream.getAttributes() != null) {
                        uid = remoteStream.getAttributes().get("from");
                    }
                    Participant participant = null;
                    conferenceClient.info().getParticipants();
                    for (Participant participant1 : conferenceClient.info().getParticipants()) {
                        if (participant1.userId.equals(uid)) {
                            participant = participant1;
                            break;
                        }
                    }
                    addRemoteStream(remoteStream, participant);
                }

            } else {
                iMeetingListener.addView(list);
            }
        });
        //推流
        publish();
    }


    /**
     * 发布本地音视频流
     * 优化了编解码器配置和视频质量参数
     */
    private void publish() {
        executor.execute(() -> {
            try {
                if (capturer == null) {
                    // 使用更合理的视频分辨率 720p 30fps，支持前后摄像头切换
                    capturer = OwtVideoCapturer.create(1280, 720, 30, true, true);
                    localStream = new LocalStream(capturer, new MediaConstraints.AudioTrackConstraints());
                }
                
                // 默认状态：音频开启，视频关闭（节省带宽）
                localStream.enableAudio();
                localStream.disableVideo();
                
                // 优化编解码器配置：优先使用硬件加速的 H264，备选 VP8
                PublishOptions options = createOptimizedPublishOptions();
                
                ActionCallback<Publication> callback = new ActionCallback<Publication>() {
                @Override
                public void onSuccess(final Publication result) {
                    publication = result;
                    // 通知其他参与者已成功加入会议
                    if (WKRTCManager.getInstance().getSendMsgListener() != null && roomID != null) {
                        WKRTCManager.getInstance().getSendMsgListener().sendMultiJoined(roomID);
                    }
                    WKLogger.i(tag, "本地流发布成功，房间ID: " + roomID);
                }

                @Override
                public void onFailure(final OwtError error) {
                    WKLogger.e(tag, "发布本地流失败: " + error.errorMessage);
                    // 通知UI发布失败
                    if (iMeetingListener != null) {
                        iMeetingListener.hangup();
                    }
                }
                };
                
                // 设置本地流属性
                HashMap<String, String> hashMap = new HashMap<>();
                hashMap.put("from", loginUID);
                localStream.setAttributes(hashMap);
                
                // 发布本地流到会议
                conferenceClient.publish(localStream, options, callback);
                
            } catch (Exception e) {
                WKLogger.e(tag, "发布本地流异常", e);
                // 发布失败时清理资源
                cleanupLocalMedia();
                if (iMeetingListener != null) {
                    iMeetingListener.hangup();
                }
            }
        });
    }

    /**
     * 创建优化的发布选项
     * 优先 H264（硬件加速），备选 VP8（网络适应性）
     */
    private PublishOptions createOptimizedPublishOptions() {
        return PublishOptions.builder()
                .addVideoParameter(new VideoEncodingParameters(H264)) // 优先硬件加速
                .addVideoParameter(new VideoEncodingParameters(VP8))  // 备选方案
                .build();
    }

    public void joinRoom() {
        executor.execute(() -> conferenceClient.join(token, new ActionCallback<ConferenceInfo>() {
            @Override
            public void onSuccess(ConferenceInfo conferenceInfo) {
                WKLogger.i(tag, "成功加入房间");
                reconnectAttempts = 0; // 重置重连计数
                joinRoomSuccess();
            }

            @Override
            public void onFailure(OwtError e) {
                WKLogger.e(tag, "加入房间失败: " + e.errorMessage + ", 重连次数: " + reconnectAttempts);
                
                // 网络错误且未超过重连次数，尝试重连
                if (isNetworkError(e) && reconnectAttempts < MAX_RECONNECT_ATTEMPTS) {
                    reconnectAttempts++;
                    WKLogger.i(tag, "尝试重连房间，第" + reconnectAttempts + "次");
                    
                    // 延迟重连
                    new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                        joinRoom();
                    }, 2000 * reconnectAttempts); // 逐渐增加重连间隔
                } else {
                    // 重连失败或非网络错误，清理资源并通知UI
                    WKLogger.e(tag, "加入房间最终失败，停止重连");
                    cleanupResources();
                    if (iMeetingListener != null) {
                        iMeetingListener.hangup();
                    }
                }
            }
        }));

    }

    /**
     * 主动挂断通话
     * 发送挂断消息并清理资源
     */
    public void hangUp() {
        WKLogger.i(tag, "主动挂断通话");
        
        // 先发送挂断消息
        if (WKRTCManager.getInstance().getSendMsgListener() != null && roomID != null) {
            WKRTCManager.getInstance().getSendMsgListener().sendMultiHangup(roomID);
        }
        
        // 离开会议室
        if (conferenceClient != null) {
            executor.execute(() -> {
                try {
                    conferenceClient.leave();
                } catch (Exception e) {
                    WKLogger.e(tag, "离开会议室失败", e);
                }
            });
        }
        
        // 更新通话状态
        WKRTCManager.getInstance().setCallingState(false);
        
        // 清理资源
        cleanupResources();
        
        // 通知UI
        if (iMeetingListener != null) {
            iMeetingListener.hangup();
        }
    }

    /**
     * @deprecated 使用 {@link #cleanupLocalMedia()} 替代
     * 此方法已整合到统一的资源清理流程中
     */
    @Deprecated
    private void unPublish() {
        WKLogger.w(tag, "unPublish方法已废弃，使用cleanupLocalMedia替代");
        cleanupLocalMedia();
    }

    public void startCountDownTimer(String uid) {

        CountDownTimer downTimer = new CountDownTimer(30 * 1000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {

            }

            @Override
            public void onFinish() {
                removeUser(uid);
            }
        };
        downTimer.start();
        if (timerMap == null) timerMap = new ConcurrentHashMap<>();
        timerMap.put(uid, downTimer);

    }

    private void removeUser(String uid) {
        if (timerMap.containsKey(uid)) {
            CountDownTimer timer = timerMap.get(uid);
            if (timer != null) {
                timer.cancel();
            }
        }
        timerMap.remove(uid);
        if (list != null && list.size() > 0) {
            for (int i = 0; i < list.size(); i++) {
                if (!TextUtils.isEmpty(list.get(i).uid) && list.get(i).uid.equals(uid)) {
                    list.remove(i);
                    iMeetingListener.removeView(uid);
//                    allView.removeViewAt(i);
                    if (list.size() == 1) {
                        hangUp();
                    }
                    break;
                }
            }
        }
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("uid", uid);
            jsonObject.put("cmd", "removeParticipant");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        if (conferenceClient != null)
            conferenceClient.send(jsonObject.toString(), new ActionCallback<Void>() {
                @Override
                public void onSuccess(Void result) {

                }

                @Override
                public void onFailure(OwtError error) {

                }
            });
    }


    /**
     * 添加远程流并订阅
     * 优化了音视频订阅参数和错误处理
     */
    private void addRemoteStream(RemoteStream remoteStream, Participant participant) {
        String uid = "";
        HashMap<String, String> hashMap = remoteStream.getAttributes();
        if (hashMap != null && hashMap.containsKey("from")) {
            uid = hashMap.get("from");
        }
        String finalUid = uid;
        
        if (TextUtils.isEmpty(finalUid)) {
            WKLogger.w(tag, "远程流缺少用户ID信息，跳过订阅");
            return;
        }
        
        executor.execute(() -> {
            try {
                // 创建优化的订阅选项
                SubscribeOptions options = createOptimizedSubscribeOptions();
                
                WKLogger.i(tag, "开始订阅远程流，用户: " + finalUid);
                
                // 添加远程流观察者
                remoteStream.addObserver(new owt.base.RemoteStream.StreamObserver() {
                @Override
                public void onEnded() {

                    for (int i = 0, size = list.size(); i < size; i++) {
                        if (!TextUtils.isEmpty(list.get(i).uid) && !TextUtils.isEmpty(finalUid) && list.get(i).uid.equals(finalUid)) {
                            list.remove(i);
                            iMeetingListener.removeView(finalUid);
                            //allView.removeViewAt(i);
                            break;
                        }
                    }
                    if (list.size() <= 1) {
                        hangUp();
                    }


                }

                @Override
                public void onUpdated() {
                }
            });

            conferenceClient.subscribe(remoteStream, options, new ActionCallback<Subscription>() {
                @Override
                public void onSuccess(Subscription result) {

                    if (!TextUtils.isEmpty(finalUid) && !TextUtils.isEmpty(loginUID)) {
                        if (loginUID.equals(finalUid)) {
                            remoteStream.disableAudio();
                        }
                        if (!loginUID.equals(finalUid)) {
                            WKRTCManager.getInstance().startTimer();
                        }
                    }
                    if (!TextUtils.isEmpty(finalUid) && timerMap != null && timerMap.size() > 0) {
                        CountDownTimer countDownTimer = timerMap.get(finalUid);
                        if (countDownTimer != null) countDownTimer.cancel();
                        timerMap.remove(finalUid);
                    }

                    result.getStats(new ActionCallback<>() {
                        @Override
                        public void onSuccess(RTCStatsReport result) {
                            if (rtcStatusMap == null) rtcStatusMap = new ConcurrentHashMap<>();
                            if (!TextUtils.isEmpty(finalUid)) {
                                rtcStatusMap.put(finalUid, new WKRTCStatusReport(result, 0f));
                            }
                            //startCheckRTCStatusReportTimer();
                        }

                        @Override
                        public void onFailure(OwtError error) {

                        }
                    });
                    result.addObserver(new Subscription.SubscriptionObserver() {
                        @Override
                        public void onEnded() {
                        }

                        @Override
                        public void onError(OwtError error) {

                        }

                        @Override
                        public void onMute(MediaConstraints.TrackKind trackKind) {
                            if (!TextUtils.isEmpty(finalUid)) {
                                iMeetingListener.onMute(list, finalUid, trackKind);
                            }


                        }

                        @Override
                        public void onUnmute(MediaConstraints.TrackKind trackKind) {
                            if (!TextUtils.isEmpty(finalUid)) {
                                iMeetingListener.onUnmute(list, finalUid, trackKind);
                            }


                        }
                    });
                    iMeetingListener.resetView(list, finalUid, participant, remoteStream);
                }

                @Override
                public void onFailure(OwtError error) {
                    WKLogger.e(tag, "订阅远程流失败: " + error.errorMessage + ", 用户: " + finalUid);
                    // 订阅失败，从列表中移除该用户
                    if (!TextUtils.isEmpty(finalUid)) {
                        for (int i = 0; i < list.size(); i++) {
                            if (list.get(i).uid.equals(finalUid)) {
                                list.remove(i);
                                if (iMeetingListener != null) {
                                    iMeetingListener.removeView(finalUid);
                                }
                                break;
                            }
                        }
                    }
                }
            });
            
            } catch (Exception e) {
                WKLogger.e(tag, "订阅远程流异常，用户: " + finalUid, e);
                // 订阅异常时从列表中移除该用户
                for (int i = 0; i < list.size(); i++) {
                    if (list.get(i).uid.equals(finalUid)) {
                        list.remove(i);
                        if (iMeetingListener != null) {
                            iMeetingListener.removeView(finalUid);
                        }
                        break;
                    }
                }
            }
        });
    }

    /**
     * 创建优化的订阅选项
     * 音频：OPUS + PCMU，视频：自适应
     */
    private SubscribeOptions createOptimizedSubscribeOptions() {
        SubscribeOptions.AudioSubscriptionConstraints audioOption =
                SubscribeOptions.AudioSubscriptionConstraints.builder()
                        .addCodec(new AudioCodecParameters(OPUS))  // 高质量
                        .addCodec(new AudioCodecParameters(PCMU))  // 兼容性
                        .build();

        SubscribeOptions.VideoSubscriptionConstraints videoOption =
                SubscribeOptions.VideoSubscriptionConstraints.builder().build();

        return SubscribeOptions.builder(true, true)
                .setAudioOption(audioOption)
                .setVideoOption(videoOption)
                .build();
    }


    public interface IMeetingListener {
        void removeView(String uid);

        void onUnmute(List<ItemRemoteStream> list, String uid, MediaConstraints.TrackKind trackKind);

        void onMute(List<ItemRemoteStream> list, String uid, MediaConstraints.TrackKind trackKind);

        void resetView(List<ItemRemoteStream> list, String uid, Participant participant, RemoteStream remoteStream);

        void hangup();

        void addView(List<ItemRemoteStream> list);

        void addItem(RemoteStream remoteStream, String uid);

        void checkRTCStatusReport(ConcurrentHashMap<String, WKRTCStatusReport> rtcStatusMap, List<ItemRemoteStream> list);
    }
}
