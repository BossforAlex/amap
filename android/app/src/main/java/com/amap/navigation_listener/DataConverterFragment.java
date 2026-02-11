package com.amap.navigation_listener;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class DataConverterFragment extends Fragment {
    private static final String TAG = "DataConverterFragment";

    // UI组件
    private EditText etInputCode;
    private EditText etInputJson;
    private TextView tvStatusResult;
    private TextView tvJsonResult;
    private Button btnConvertStatus;
    private Button btnConvertJson;
    private Button btnClearAll;

    // 状态码映射
    private Map<Integer, String> statusMap;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initStatusMap();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                           @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_data_converter, container, false);
        initViews(view);
        setupListeners();
        return view;
    }

    private void initStatusMap() {
        statusMap = new HashMap<>();
        // 高德地图导航状态码
        statusMap.put(1, "导航开始");
        statusMap.put(2, "导航进行中");
        statusMap.put(3, "导航暂停");
        statusMap.put(4, "导航恢复");
        statusMap.put(5, "导航结束");
        statusMap.put(6, "导航错误");
        statusMap.put(7, "路线重新规划");
        statusMap.put(8, "偏航重新规划");
        statusMap.put(9, "到达目的地");
        statusMap.put(10, "通过摄像头");
        statusMap.put(11, "进入隧道");
        statusMap.put(12, "驶出隧道");
        statusMap.put(13, "上高架");
        statusMap.put(14, "下高架");
        statusMap.put(15, "进入环岛");
        statusMap.put(16, "驶出环岛");
        statusMap.put(17, "进入高速");
        statusMap.put(18, "驶出高速");
        statusMap.put(19, "进入服务区");
        statusMap.put(20, "驶出服务区");
    }

    private void initViews(View view) {
        etInputCode = view.findViewById(R.id.etInputCode);
        etInputJson = view.findViewById(R.id.etInputJson);
        tvStatusResult = view.findViewById(R.id.tvStatusResult);
        tvJsonResult = view.findViewById(R.id.tvJsonResult);
        btnConvertStatus = view.findViewById(R.id.btnConvertStatus);
        btnConvertJson = view.findViewById(R.id.btnConvertJson);
        btnClearAll = view.findViewById(R.id.btnClearAll);
    }

    private void setupListeners() {
        btnConvertStatus.setOnClickListener(v -> convertStatusCode());
        btnConvertJson.setOnClickListener(v -> convertJsonToText());
        btnClearAll.setOnClickListener(v -> clearAll());
    }

    private void convertStatusCode() {
        String input = etInputCode.getText().toString().trim();

        if (input.isEmpty()) {
            Toast.makeText(getContext(), "请输入状态码", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            int code = Integer.parseInt(input);
            String description = statusMap.get(code);

            if (description != null) {
                tvStatusResult.setText("状态码 " + code + ": " + description);
                tvStatusResult.setTextColor(getResources().getColor(android.R.color.black));
            } else {
                tvStatusResult.setText("状态码 " + code + ": 未知状态");
                tvStatusResult.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
            }
        } catch (NumberFormatException e) {
            tvStatusResult.setText("错误: 请输入有效的数字");
            tvStatusResult.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
        }
    }

    private void convertJsonToText() {
        String input = etInputJson.getText().toString().trim();

        if (input.isEmpty()) {
            Toast.makeText(getContext(), "请输入JSON数据", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            JSONObject json = new JSONObject(input);
            StringBuilder naturalText = new StringBuilder();

            // 解析各个字段
            if (json.has("roadName")) {
                String roadName = json.getString("roadName");
                naturalText.append("当前在").append(roadName).append("上行驶");
            }

            if (json.has("action")) {
                String action = json.getString("action");
                naturalText.append("，").append(action);
            }

            if (json.has("distance")) {
                int distance = json.getInt("distance");
                naturalText.append("，距离").append(distance).append("米");
            }

            if (json.has("remainingTime")) {
                int time = json.getInt("remainingTime");
                naturalText.append("，预计").append(formatTime(time));
            }

            if (json.has("speed")) {
                int speed = json.getInt("speed");
                naturalText.append("，当前车速").append(speed).append("公里每小时");
            }

            if (json.has("isActive")) {
                boolean isActive = json.getBoolean("isActive");
                if (!isActive) {
                    naturalText.append("【导航未开始】");
                }
            }

            tvJsonResult.setText(naturalText.toString());
            tvJsonResult.setTextColor(getResources().getColor(android.R.color.black));

        } catch (JSONException e) {
            tvJsonResult.setText("JSON解析错误: " + e.getMessage());
            tvJsonResult.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
        }
    }

    private String formatTime(int seconds) {
        if (seconds < 60) {
            return seconds + "秒";
        } else {
            int minutes = seconds / 60;
            int remainingSeconds = seconds % 60;
            if (remainingSeconds == 0) {
                return minutes + "分钟";
            } else {
                return minutes + "分钟" + remainingSeconds + "秒";
            }
        }
    }

    private void clearAll() {
        etInputCode.setText("");
        etInputJson.setText("");
        tvStatusResult.setText("");
        tvJsonResult.setText("");
        Toast.makeText(getContext(), "已清空所有内容", Toast.LENGTH_SHORT).show();
    }

    // 专业术语转通俗中文
    public static String convertTechnicalTerms(String technicalText) {
        String commonText = technicalText;

        // 替换专业术语
        Map<String, String> termMap = new HashMap<>();
        termMap.put("GPS信号弱", "卫星定位信号不好");
        termMap.put("偏航", "偏离了规划路线");
        termMap.put("重新规划", "正在重新计算路线");
        termMap.put("前方拥堵", "前面堵车了");
        termMap.put("限速", "速度限制");
        termMap.put("违章拍照", "有摄像头拍照");
        termMap.put("区间测速", "这一段路测平均速度");
        termMap.put("应急车道", "紧急情况下用的车道");
        termMap.put("匝道", "上下高速的连接路");
        termMap.put("并线", "需要变换车道");
        termMap.put("主路", "主要道路");
        termMap.put("辅路", "辅助道路");
        termMap.put("调头", "掉头往回走");
        termMap.put("靠左", "往左边车道走");
        termMap.put("靠右", "往右边车道走");

        for (Map.Entry<String, String> entry : termMap.entrySet()) {
            commonText = commonText.replace(entry.getKey(), entry.getValue());
        }

        return commonText;
    }
}