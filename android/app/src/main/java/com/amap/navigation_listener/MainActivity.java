package com.amap.navigation_listener;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "NavigationListener";
    private static final int REQUEST_PERMISSIONS = 1001;
    private static final int REQUEST_ENABLE_BT = 1002;

    // BLE UUIDs
    private static final String SERVICE_UUID = "4fafc201-1fb5-459e-8fcc-c5c9c331914b";
    private static final String CHARACTERISTIC_UUID = "beb5483e-36e1-4688-b7f5-ea07361b26a8";

    // UI组件
    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private Button btnStartService;
    private TextView tvStatus;

    // 蓝牙相关
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothGatt bluetoothGatt;
    private BluetoothDevice connectedDevice;
    private boolean isConnected = false;

    // 页面适配器
    private ViewPagerAdapter pagerAdapter;

    // 导航数据
    private NavigationData navigationData;

    // 需要的权限
    private String[] permissions = {
        Manifest.permission.BLUETOOTH,
        Manifest.permission.BLUETOOTH_ADMIN,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.BLUETOOTH_SCAN,
        Manifest.permission.BLUETOOTH_CONNECT
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        initBluetooth();
        checkPermissions();
        setupViewPager();
    }

    private void initViews() {
        tabLayout = findViewById(R.id.tabLayout);
        viewPager = findViewById(R.id.viewPager);
        btnStartService = findViewById(R.id.btnStartService);
        tvStatus = findViewById(R.id.tvStatus);

        btnStartService.setOnClickListener(v -> toggleNavigationService());
    }

    private void initBluetooth() {
        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();

        if (bluetoothAdapter == null) {
            Toast.makeText(this, "设备不支持蓝牙", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
    }

    private void checkPermissions() {
        List<String> permissionsNeeded = new ArrayList<>();

        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(permission);
            }
        }

        if (!permissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this,
                permissionsNeeded.toArray(new String[0]),
                REQUEST_PERMISSIONS);
        }
    }

    private void setupViewPager() {
        pagerAdapter = new ViewPagerAdapter(this);
        viewPager.setAdapter(pagerAdapter);

        new TabLayoutMediator(tabLayout, viewPager,
            (tab, position) -> {
                switch (position) {
                    case 0:
                        tab.setText("导航监听");
                        break;
                    case 1:
                        tab.setText("蓝牙控制");
                        break;
                    case 2:
                        tab.setText("数据转换");
                        break;
                }
            }
        ).attach();
    }

    private void toggleNavigationService() {
        if (!isConnected) {
            scanAndConnect();
        } else {
            disconnectDevice();
        }
    }

    @SuppressLint("MissingPermission")
    private void scanAndConnect() {
        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            return;
        }

        tvStatus.setText("正在扫描设备...");

        // 扫描设备
        bluetoothAdapter.startLeScan(leScanCallback);

        // 10秒后停止扫描
        new Handler().postDelayed(() -> {
            bluetoothAdapter.stopLeScan(leScanCallback);
            if (!isConnected) {
                tvStatus.setText("未找到设备");
            }
        }, 10000);
    }

    private BluetoothAdapter.LeScanCallback leScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
            runOnUiThread(() -> {
                String deviceName = device.getName();
                if (deviceName != null && deviceName.contains("ESP32_Navigation")) {
                    bluetoothAdapter.stopLeScan(this);
                    connectToDevice(device);
                }
            });
        }
    };

    @SuppressLint("MissingPermission")
    private void connectToDevice(BluetoothDevice device) {
        tvStatus.setText("正在连接设备...");
        connectedDevice = device;
        bluetoothGatt = device.connectGatt(this, false, gattCallback);
    }

    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.i(TAG, "已连接到GATT服务器");
                isConnected = true;
                runOnUiThread(() -> {
                    tvStatus.setText("已连接: " + connectedDevice.getName());
                    btnStartService.setText("断开连接");
                });
                gatt.discoverServices();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.i(TAG, "已断开GATT服务器连接");
                isConnected = false;
                runOnUiThread(() -> {
                    tvStatus.setText("未连接");
                    btnStartService.setText("连接设备");
                });
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                BluetoothGattService service = gatt.getService(UUID.fromString(SERVICE_UUID));
                if (service != null) {
                    BluetoothGattCharacteristic characteristic = service.getCharacteristic(
                        UUID.fromString(CHARACTERISTIC_UUID));
                    if (characteristic != null) {
                        gatt.setCharacteristicNotification(characteristic, true);
                    }
                }
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i(TAG, "数据发送成功");
            }
        }
    };

    @SuppressLint("MissingPermission")
    private void disconnectDevice() {
        if (bluetoothGatt != null) {
            bluetoothGatt.disconnect();
            bluetoothGatt.close();
            bluetoothGatt = null;
            isConnected = false;
        }
    }

    // 发送导航数据到ESP32
    public void sendNavigationData(NavigationData data) {
        if (isConnected && bluetoothGatt != null) {
            try {
                JSONObject json = new JSONObject();
                json.put("roadName", data.getRoadName());
                json.put("action", data.getAction());
                json.put("distance", data.getDistance());
                json.put("remainingTime", data.getRemainingTime());
                json.put("speed", data.getSpeed());
                json.put("isActive", data.isActive());

                String jsonStr = json.toString() + "\n";

                BluetoothGattService service = bluetoothGatt.getService(UUID.fromString(SERVICE_UUID));
                if (service != null) {
                    BluetoothGattCharacteristic characteristic = service.getCharacteristic(
                        UUID.fromString(CHARACTERISTIC_UUID));
                    if (characteristic != null) {
                        characteristic.setValue(jsonStr.getBytes());
                        bluetoothGatt.writeCharacteristic(characteristic);
                    }
                }
            } catch (JSONException e) {
                Log.e(TAG, "JSON转换错误", e);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                         @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSIONS) {
            for (int i = 0; i < grantResults.length; i++) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "需要权限才能使用蓝牙功能", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_OK) {
                scanAndConnect();
            } else {
                Toast.makeText(this, "需要启用蓝牙", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        disconnectDevice();
    }

    // Getter方法
    public boolean isConnected() {
        return isConnected;
    }

    public NavigationData getNavigationData() {
        return navigationData;
    }

    public void setNavigationData(NavigationData data) {
        this.navigationData = data;
        sendNavigationData(data);
    }
}