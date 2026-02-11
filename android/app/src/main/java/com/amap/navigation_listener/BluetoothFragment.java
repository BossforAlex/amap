package com.amap.navigation_listener;

import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.List;

public class BluetoothFragment extends Fragment {
    private static final String TAG = "BluetoothFragment";

    // UI组件
    private Button btnScanDevices;
    private Button btnDisconnect;
    private ListView lvDevices;
    private TextView tvConnectionStatus;
    private TextView tvDeviceInfo;

    // 设备列表
    private List<BluetoothDevice> deviceList;
    private ArrayAdapter<String> deviceAdapter;
    private List<String> deviceNames;

    private MainActivity mainActivity;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mainActivity = (MainActivity) getActivity();
        deviceList = new ArrayList<>();
        deviceNames = new ArrayList<>();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                           @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_bluetooth, container, false);
        initViews(view);
        setupListeners();
        updateConnectionStatus();
        return view;
    }

    private void initViews(View view) {
        btnScanDevices = view.findViewById(R.id.btnScanDevices);
        btnDisconnect = view.findViewById(R.id.btnDisconnect);
        lvDevices = view.findViewById(R.id.lvDevices);
        tvConnectionStatus = view.findViewById(R.id.tvConnectionStatus);
        tvDeviceInfo = view.findViewById(R.id.tvDeviceInfo);

        // 初始化设备列表适配器
        deviceAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1, deviceNames);
        lvDevices.setAdapter(deviceAdapter);
    }

    private void setupListeners() {
        btnScanDevices.setOnClickListener(v -> scanForDevices());
        btnDisconnect.setOnClickListener(v -> disconnectDevice());

        lvDevices.setOnItemClickListener((parent, view, position, id) -> {
            if (position < deviceList.size()) {
                connectToDevice(deviceList.get(position));
            }
        });
    }

    private void scanForDevices() {
        deviceList.clear();
        deviceNames.clear();
        deviceAdapter.notifyDataSetChanged();

        Toast.makeText(getContext(), "正在扫描设备...", Toast.LENGTH_SHORT).show();

        // 开始扫描（实际扫描逻辑在MainActivity中）
        if (mainActivity != null) {
            mainActivity.scanAndConnect();
        }
    }

    private void connectToDevice(BluetoothDevice device) {
        if (mainActivity != null) {
            mainActivity.connectToDevice(device);
        }
    }

    private void disconnectDevice() {
        if (mainActivity != null) {
            mainActivity.disconnectDevice();
            updateConnectionStatus();
        }
    }

    public void addDevice(BluetoothDevice device) {
        if (!deviceList.contains(device)) {
            deviceList.add(device);
            String deviceName = device.getName();
            if (deviceName == null || deviceName.isEmpty()) {
                deviceName = "未知设备";
            }
            deviceNames.add(deviceName + "\n" + device.getAddress());
            deviceAdapter.notifyDataSetChanged();
        }
    }

    public void clearDevices() {
        deviceList.clear();
        deviceNames.clear();
        deviceAdapter.notifyDataSetChanged();
    }

    public void updateConnectionStatus() {
        if (mainActivity != null && mainActivity.isConnected()) {
            tvConnectionStatus.setText("已连接");
            tvConnectionStatus.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
            btnDisconnect.setEnabled(true);

            // 显示设备信息
            if (mainActivity.getNavigationData() != null) {
                String info = "设备: ESP32导航显示\n" +
                            "服务UUID: " + "4fafc201-1fb5-459e-8fcc-c5c9c331914b\n" +
                            "特征值UUID: " + "beb5483e-36e1-4688-b7f5-ea07361b26a8";
                tvDeviceInfo.setText(info);
            }
        } else {
            tvConnectionStatus.setText("未连接");
            tvConnectionStatus.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
            btnDisconnect.setEnabled(false);
            tvDeviceInfo.setText("");
        }
    }
}