package com.amap.navigation_listener;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NavigationListenerService extends AccessibilityService {
    private static final String TAG = "NavigationListenerService";
    private static final String AMAP_PACKAGE = "com.autonavi.minimap";

    private OnNavigationDataListener listener;
    private NavigationData currentData;

    public interface OnNavigationDataListener {
        void onNavigationDataReceived(NavigationData data);
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        Log.i(TAG, "导航监听服务已连接");

        // 配置监听参数
        AccessibilityServiceInfo info = new AccessibilityServiceInfo();
        info.eventTypes = AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED |
                         AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED |
                         AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED;
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC;
        info.packageNames = new String[]{AMAP_PACKAGE};
        info.notificationTimeout = 100;
        setServiceInfo(info);

        currentData = new NavigationData();
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (!event.getPackageName().equals(AMAP_PACKAGE)) {
            return;
        }

        Log.d(TAG, "收到高德地图事件: " + event.getEventType());

        // 获取根节点
        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
        if (rootNode == null) {
            return;
        }

        // 解析导航信息
        parseNavigationInfo(rootNode);
        rootNode.recycle();
    }

    private void parseNavigationInfo(AccessibilityNodeInfo rootNode) {
        // 查找包含导航信息的节点
        String fullText = getAllText(rootNode);
        Log.d(TAG, "页面文本: " + fullText);

        // 使用正则表达式提取导航信息
        NavigationData newData = extractNavigationData(fullText);

        // 如果数据有变化，通知监听器
        if (!newData.equals(currentData)) {
            currentData = newData;
            if (listener != null) {
                listener.onNavigationDataReceived(currentData);
            }
        }
    }

    private String getAllText(AccessibilityNodeInfo node) {
        if (node == null) {
            return "";
        }

        StringBuilder sb = new StringBuilder();

        // 获取当前节点的文本
        CharSequence text = node.getText();
        if (text != null && text.length() > 0) {
            sb.append(text.toString()).append(" ");
        }

        // 获取内容描述
        CharSequence contentDescription = node.getContentDescription();
        if (contentDescription != null && contentDescription.length() > 0) {
            sb.append(contentDescription.toString()).append(" ");
        }

        // 递归处理子节点
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child != null) {
                sb.append(getAllText(child));
                child.recycle();
            }
        }

        return sb.toString();
    }

    private NavigationData extractNavigationData(String text) {
        NavigationData data = new NavigationData();

        // 解析道路名称
        Pattern roadPattern = Pattern.compile("(沿|在|进入)([\u4e00-\u9fa5\w]+路)");
        Matcher roadMatcher = roadPattern.matcher(text);
        if (roadMatcher.find()) {
            data.setRoadName(roadMatcher.group(2));
        }

        // 解析导航动作
        String[] actions = {"直行", "左转", "右转", "掉头", "靠左", "靠右", "进入环岛", "驶出环岛"};
        for (String action : actions) {
            if (text.contains(action)) {
                data.setAction(action);
                break;
            }
        }

        // 解析距离
        Pattern distancePattern = Pattern.compile("(\\d+)米");
        Matcher distanceMatcher = distancePattern.matcher(text);
        if (distanceMatcher.find()) {
            data.setDistance(Integer.parseInt(distanceMatcher.group(1)));
        }

        // 解析剩余时间
        Pattern timePattern = Pattern.compile("(\\d+)分钟");
        Matcher timeMatcher = timePattern.matcher(text);
        if (timeMatcher.find()) {
            data.setRemainingTime(Integer.parseInt(timeMatcher.group(1)) * 60);
        }

        // 解析速度
        Pattern speedPattern = Pattern.compile("(\\d+)km/h");
        Matcher speedMatcher = speedPattern.matcher(text);
        if (speedMatcher.find()) {
            data.setSpeed(Integer.parseInt(speedMatcher.group(1)));
        }

        // 判断是否处于导航状态
        data.setActive(text.contains("导航") || text.contains("路线") || !text.isEmpty());

        // 如果没有提取到有效信息，设置为默认值
        if (data.getRoadName() == null || data.getRoadName().isEmpty()) {
            data.setRoadName("未识别道路");
        }
        if (data.getAction() == null || data.getAction().isEmpty()) {
            data.setAction("继续行驶");
        }

        return data;
    }

    @Override
    public void onInterrupt() {
        Log.i(TAG, "导航监听服务被中断");
    }

    public void setOnNavigationDataListener(OnNavigationDataListener listener) {
        this.listener = listener;
    }

    public NavigationData getCurrentData() {
        return currentData;
    }

    // 辅助类用于比较NavigationData
    private boolean dataEquals(NavigationData data1, NavigationData data2) {
        if (data1 == null || data2 == null) {
            return data1 == data2;
        }
        return data1.getRoadName().equals(data2.getRoadName()) &&
               data1.getAction().equals(data2.getAction()) &&
               data1.getDistance() == data2.getDistance() &&
               data1.getRemainingTime() == data2.getRemainingTime() &&
               data1.getSpeed() == data2.getSpeed() &&
               data1.isActive() == data2.isActive();
    }
}