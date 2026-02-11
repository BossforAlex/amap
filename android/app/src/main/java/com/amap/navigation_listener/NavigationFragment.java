package com.amap.navigation_listener;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

public class NavigationFragment extends Fragment {
    private static final String TAG = "NavigationFragment";

    // UI组件
    private TextView tvRoadName;
    private TextView tvAction;
    private TextView tvDistance;
    private TextView tvRemainingTime;
    private TextView tvSpeed;
    private TextView tvStatus;
    private Button btnClearData;
    private Button btnCopyData;
    private Button btnCacheData;

    // 数据
    private NavigationData navigationData;
    private MainActivity mainActivity;

    // 导航监听服务
    private NavigationListenerService listenerService;
    private boolean isServiceBound = false;

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            NavigationListenerService.LocalBinder binder = (NavigationListenerService.LocalBinder) service;
            listenerService = binder.getService();
            isServiceBound = true;
            listenerService.setOnNavigationDataListener(new NavigationListenerService.OnNavigationDataListener() {
                @Override
                public void onNavigationDataReceived(NavigationData data) {
                    updateNavigationDisplay(data);
                    // 发送到ESP32
                    if (mainActivity != null) {
                        mainActivity.setNavigationData(data);
                    }
                }
            });
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            listenerService = null;
            isServiceBound = false;
        }
    };

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mainActivity = (MainActivity) getActivity();
        navigationData = new NavigationData();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                           @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_navigation, container, false);
        initViews(view);
        setupListeners();
        updateNavigationDisplay(navigationData);
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        // 检查高德地图是否安装
        if (!isAmapInstalled()) {
            Toast.makeText(getContext(), "请先安装高德地图", Toast.LENGTH_LONG).show();
        }

        // 绑定导航监听服务
        bindNavigationService();
    }

    @Override
    public void onPause() {
        super.onPause();
        // 解绑服务
        if (isServiceBound) {
            getActivity().unbindService(serviceConnection);
            isServiceBound = false;
        }
    }

    private void initViews(View view) {
        tvRoadName = view.findViewById(R.id.tvRoadName);
        tvAction = view.findViewById(R.id.tvAction);
        tvDistance = view.findViewById(R.id.tvDistance);
        tvRemainingTime = view.findViewById(R.id.tvRemainingTime);
        tvSpeed = view.findViewById(R.id.tvSpeed);
        tvStatus = view.findViewById(R.id.tvStatus);
        btnClearData = view.findViewById(R.id.btnClearData);
        btnCopyData = view.findViewById(R.id.btnCopyData);
        btnCacheData = view.findViewById(R.id.btnCacheData);
    }

    private void setupListeners() {
        btnClearData.setOnClickListener(v -> clearNavigationData());
        btnCopyData.setOnClickListener(v -> copyNavigationData());
        btnCacheData.setOnClickListener(v -> cacheNavigationData());
    }

    private void bindNavigationService() {
        Intent intent = new Intent(getActivity(), NavigationListenerService.class);
        getActivity().bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    private boolean isAmapInstalled() {
        // 检查是否安装了高德地图
        List<AccessibilityServiceInfo> services = getActivity().getSystemService(AccessibilityService.class)
                .getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK);

        for (AccessibilityServiceInfo info : services) {
            if (info.getResolveInfo().serviceInfo.packageName.equals("com.autonavi.minimap")) {
                return true;
            }
        }
        return false;
    }

    private void updateNavigationDisplay(NavigationData data) {
        navigationData = data;

        getActivity().runOnUiThread(() -> {
            if (data.isActive()) {
                tvRoadName.setText("当前道路: " + data.getRoadName());
                tvAction.setText("下一步: " + data.getAction());
                tvDistance.setText("距离: " + data.getDistance() + " 米");
                tvRemainingTime.setText("剩余时间: " + formatTime(data.getRemainingTime()));
                tvSpeed.setText("当前速度: " + data.getSpeed() + " km/h");
                tvStatus.setText("导航状态: 进行中");
                tvStatus.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
            } else {
                tvRoadName.setText("当前道路: 未开始导航");
                tvAction.setText("下一步: 等待导航开始");
                tvDistance.setText("距离: 0 米");
                tvRemainingTime.setText("剩余时间: 0 秒");
                tvSpeed.setText("当前速度: 0 km/h");
                tvStatus.setText("导航状态: 未开始");
                tvStatus.setTextColor(getResources().getColor(android.R.color.darker_gray));
            }
        });
    }

    private String formatTime(int seconds) {
        if (seconds < 60) {
            return seconds + " 秒";
        } else {
            int minutes = seconds / 60;
            int remainingSeconds = seconds % 60;
            if (remainingSeconds == 0) {
                return minutes + " 分钟";
            } else {
                return minutes + " 分 " + remainingSeconds + " 秒";
            }
        }
    }

    private void clearNavigationData() {
        navigationData = new NavigationData();
        updateNavigationDisplay(navigationData);
        Toast.makeText(getContext(), "数据已清空", Toast.LENGTH_SHORT).show();
    }

    private void copyNavigationData() {
        try {
            JSONObject json = new JSONObject();
            json.put("roadName", navigationData.getRoadName());
            json.put("action", navigationData.getAction());
            json.put("distance", navigationData.getDistance());
            json.put("remainingTime", navigationData.getRemainingTime());
            json.put("speed", navigationData.getSpeed());
            json.put("isActive", navigationData.isActive());

            String data = json.toString(2);

            // 复制到剪贴板
            android.content.ClipboardManager clipboard = (android.content.ClipboardManager)
                getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
            android.content.ClipData clip = android.content.ClipData.newPlainText("导航数据", data);
            clipboard.setPrimaryClip(clip);

            Toast.makeText(getContext(), "数据已复制到剪贴板", Toast.LENGTH_SHORT).show();
        } catch (JSONException e) {
            Log.e(TAG, "复制数据失败", e);
            Toast.makeText(getContext(), "复制数据失败", Toast.LENGTH_SHORT).show();
        }
    }

    private void cacheNavigationData() {
        // 保存到SharedPreferences
        android.content.SharedPreferences prefs = getActivity()
            .getSharedPreferences("NavigationCache", Context.MODE_PRIVATE);
        android.content.SharedPreferences.Editor editor = prefs.edit();

        try {
            JSONObject json = new JSONObject();
            json.put("roadName", navigationData.getRoadName());
            json.put("action", navigationData.getAction());
            json.put("distance", navigationData.getDistance());
            json.put("remainingTime", navigationData.getRemainingTime());
            json.put("speed", navigationData.getSpeed());
            json.put("isActive", navigationData.isActive());
            json.put("timestamp", System.currentTimeMillis());

            editor.putString("cachedData", json.toString());
            editor.apply();

            Toast.makeText(getContext(), "数据已缓存", Toast.LENGTH_SHORT).show();
        } catch (JSONException e) {
            Log.e(TAG, "缓存数据失败", e);
            Toast.makeText(getContext(), "缓存数据失败", Toast.LENGTH_SHORT).show();
        }
    }
}