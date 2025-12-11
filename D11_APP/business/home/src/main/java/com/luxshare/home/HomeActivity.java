package com.luxshare.home;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.ScanResult;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.alibaba.android.arouter.facade.annotation.Route;
import com.alibaba.android.arouter.utils.TextUtils;
import com.luxshare.base.utils.IPermissionLisenter;
import com.luxshare.base.utils.MultiLanguageUtils;
import com.luxshare.base.utils.PermissionXUtil;
import com.luxshare.base.utils.StatusBarUtils;
import com.luxshare.base.view.LuxTitleBar;
import com.luxshare.ble.BleManager;
import com.luxshare.ble.DeviceState;
import com.luxshare.ble.bean.DeviceInfo;
import com.luxshare.ble.eventbus.EventBusMessage;
import com.luxshare.ble.eventbus.IEventBusMessage;
import com.luxshare.ble.eventbus.message.ConnectState;
import com.luxshare.ble.eventbus.message.LanguageMessage;
import com.luxshare.ble.util.BleScanHandler;
import com.luxshare.configs.Configs;
import com.luxshare.configs.ContextManager;
import com.luxshare.configs.PathConfig;
import com.luxshare.fastsp.FastSharedPreferences;
import com.luxshare.home.activity.BaseSpeakerActivity;
import com.luxshare.home.activity.SettingActivity;
import com.luxshare.home.adapter.DeviceListAdapter;
import com.luxshare.home.util.DialogUtil;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Route(path = PathConfig.Path_HomeActivity)
public class HomeActivity extends BaseSpeakerActivity implements DeviceListAdapter.OnItemClickListener {
    private static final String TAG = "HomeActivity";
    private LuxTitleBar titleBar;
    private List<String> permissions = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ?
            Arrays.asList(Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_ADVERTISE,
                Manifest.permission.BLUETOOTH_SCAN):

            Arrays.asList(Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_ADVERTISE,
                    Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE);
    private List<String> permissionLow = Arrays.asList(Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN);
    private DeviceListAdapter deviceListAdapter;
    private RecyclerView list;
    private Set<String> addressSet = new HashSet<>();
    private List<DeviceInfo> deviceList = new ArrayList<>();
    /**
     * 是否获取指定的蓝牙操作权限
     */
    private boolean isAllGranted;
    /**
     * 每次进入页面如果蓝牙开关没打开则跳转到设置页面
     */
    private boolean FLAG_FIRS_OPEN_SWITCH = false;
    private boolean isInited = false;
    private RelativeLayout llLoadTip;
    private ImageView ivLoading;
    private TextView tvSearchTip;

    private boolean isStackTop = false;

    public static boolean isShowToast = true;

    @Override
    public void finishActivity(int requestCode) {
        super.finishActivity(requestCode);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "onCreate");
        Log.i(TAG, "sdk kit"+ Build.VERSION.SDK_INT);
        setContentView(R.layout.activity_home);
        StatusBarUtils.setWindowStatusBarColor(this, R.color.titlebar_color);
        initView();
        initData();
    }

    private void initView() {
        titleBar = findViewById(R.id.title_bar);
        llLoadTip = findViewById(R.id.ll_load_tip);
        tvSearchTip = findViewById(R.id.search_tip);
        ivLoading = findViewById(R.id.iv_loading);
        titleBar.setBackVisible(false);
        ImageButton btnOperate = titleBar.getBtnOperate();
        btnOperate.setVisibility(View.VISIBLE);
        titleBar.getBtnOperate().setOnClickListener(v -> {
            isShowToast = true;
            startScan();
        });
        Button button = findViewById(R.id.btn_settings);
        button.setOnClickListener(v -> {
            Intent intent = new Intent(this, SettingActivity.class);
            startActivity(intent);
        });
        list = findViewById(R.id.list_device);
        DividerItemDecoration itemDecoration = new DividerItemDecoration(getApplication(), LinearLayout.VERTICAL);
        itemDecoration.setDrawable(Objects.requireNonNull(ContextCompat.getDrawable(getApplication(), R.drawable.devideline)));
        list.addItemDecoration(itemDecoration);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getApplication(), RecyclerView.VERTICAL, false);
        list.setLayoutManager(layoutManager);
        deviceListAdapter = new DeviceListAdapter();
        deviceListAdapter.setOnItemListener(this);
        deviceListAdapter.setDatas(deviceList);
        list.setAdapter(deviceListAdapter);
    }

    private void initData() {
        BleScanHandler.getInstance().addBleScanCallback(bleScanCallback);
        BleManager.getInstance().init(ContextManager.getInstance().getContext());
    }

    @Override
    public void onResume() {
        super.onResume();
        isStackTop = true;
        Log.i(TAG, "onResume: ");
        if (!isAllGranted) {
            Log.i(TAG, "current version :" + Build.VERSION.SDK_INT);
            PermissionXUtil.INSTANCE.applyFileManagerPermission(this);
            PermissionXUtil.INSTANCE.applyPermisions(this, permissions,
                    new IPermissionLisenter() {
                        @Override
                        public void allGranted() {
                            isAllGranted = true;
                            Log.i(TAG, "allGranted: ");
                            openBluetootchSwitch();
                        }

                        @Override
                        public void denied(@Nullable List<String> deniedList) {
                            Log.i(TAG, "denied: first deny");
                            mHandler.post(() -> PermissionXUtil.INSTANCE.applyPermisions(HomeActivity.this, permissionLow,
                                    new IPermissionLisenter() {
                                        @Override
                                        public void allGranted() {
                                            Log.i(TAG, "allGranted retry success");
                                            isAllGranted = true;
                                            openBluetootchSwitch();
                                        }

                                        @Override
                                        public void denied(@Nullable List<String> deniedList1) {
                                            isAllGranted = false;
                                            Toast.makeText(HomeActivity.this,
                                                    getString(R.string.bluetooth_permission), Toast.LENGTH_SHORT).show();
                                        }
                                    }));

                        }
                    });
        } else {
            openBluetootchSwitch();
        }

        if (!BleManager.getInstance().isConnected()) {
            deviceList.forEach(deviceInfo-> {
                deviceInfo.setStatus(DeviceState.STATE_DISCONNECTED);
                deviceInfo.setDescription(getString(R.string.connecte));
            });

            deviceListAdapter.notifyDataSetChanged();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        isStackTop = false;
    }

    private void openBluetootchSwitch() {
        if (BleScanHandler.getInstance().isBlueSwitchOpen()) {
            if (!isInited) {
                isInited = true;
                startScan();
            }
        } else {
            if (FLAG_FIRS_OPEN_SWITCH) {
                return;
            }
            FLAG_FIRS_OPEN_SWITCH = true;
            Intent intent = new Intent(Settings.ACTION_BLUETOOTH_SETTINGS);
            startActivity(intent);
        }
    }

    private final BleScanHandler.BleScanCallback bleScanCallback = new BleScanHandler.BleScanCallback() {
        @Override
        public void onScanResultStart() {
            Log.i(TAG, "onScanResultStart isShowToast="+isShowToast);
            if (isShowToast) {
                Toast.makeText(HomeActivity.this, getString(R.string.start_scan),
                        Toast.LENGTH_SHORT).show();
                switchLoadingState(true);
            }
        }

        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            BluetoothDevice device = result.getDevice();
//            Log.i(TAG, "onScanResult: device:" + device);
            if (device != null && filterLuxSpeakerDevice(device)) {
                if (!addressSet.contains(device.getAddress())) {
                    addressSet.add(device.getAddress());
                    DeviceInfo deviceInfo = DeviceInfo.Companion.getDeviceInfo(result);
                    Set<String> connectedAddresses = BleManager.getInstance().getConnectedAddresses();
                    boolean isConnected = isConnected(deviceInfo, connectedAddresses);
                    deviceInfo.setStatus(isConnected ? DeviceState.STATE_CONNECTED :
                            DeviceState.STATE_DISCONNECTED);
                    deviceInfo.setDescription(getString(isConnected ? R.string.connected :
                            R.string.connecte));
                    deviceList.add(deviceInfo);
                    deviceListAdapter.notifyDataSetChanged();
                }
            }
        }

        @Override
        public void onScanFailed(int errorCode) {

        }

        @Override
        public void onScanResultEnd() {
            Log.i(TAG, "onScanResultEnd isShowToast="+isShowToast);
            if (isShowToast) {
                isShowToast = false;
                mHandler.post(() -> {
                    Toast.makeText(HomeActivity.this, getString(R.string.scan_end)
                            , Toast.LENGTH_SHORT).show();
                });
            }
            checkSearchResult();
        }
    };

    private void checkSearchResult() {
        if (deviceList.isEmpty()) {
            ivLoading.clearAnimation();
            ivLoading.setVisibility(View.GONE);
            tvSearchTip.setText(getString(R.string.search_device_no));
        } else {
            switchLoadingState(false);
        }
    }

    @SuppressLint("MissingPermission")
    private boolean filterLuxSpeakerDevice(BluetoothDevice device) {
        return !TextUtils.isEmpty(device.getName());
    }

    private void startScan() {
        Log.i(TAG, "startScan: isAllGranted:" + isAllGranted);
        if (isAllGranted) {
            addressSet.clear();
            deviceList.clear();
            deviceListAdapter.notifyDataSetChanged();
            Set<BluetoothDevice> bondedDevices = BleScanHandler.getInstance().getBondedDevices();
            Log.i(TAG, "startScan: BondedDevice size:" + bondedDevices.size());
            BleScanHandler.getInstance().startScan();
        }
    }

    public boolean isConnected(DeviceInfo deviceInfo, Set<String> addressSet) {
//        Log.i(TAG, "isConnected: addressSet:" + addressSet);
        if (addressSet == null) {
            return false;
        }
        if (!BleManager.getInstance().isConnected()) {
            return false;
        }
        for (String address : addressSet) {
            if (deviceInfo.getAddress().equals(address)) {
                return true;
            }
        }
        return false;
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(EventBusMessage msg) {
        IEventBusMessage message = msg.getMessage();
        if (message instanceof ConnectState) {
            ConnectState connectState = (ConnectState) message;
            Log.i(TAG, "onMessageEvent: connectStatus :" + connectState);
            for (int i = 0; i < deviceList.size(); i++) {
                DeviceInfo deviceInfo = deviceList.get(i);
                if (deviceInfo.getAddress().equals(connectState.getAddress())) {
                    boolean isConnected = connectState.getStatus();
                    deviceInfo.setStatus(isConnected ? DeviceState.STATE_CONNECTED : DeviceState.STATE_DISCONNECTED);
                    deviceInfo.setDescription(getString(isConnected ? R.string.connected :
                            R.string.connecte));
                    deviceListAdapter.notifyItemChanged(i);
                    if (connectState.getStatus()) {
                        mHandler.postDelayed(() -> jump2Settings(deviceInfo), 500);
                    }
                    break;
                }
            }
            if (connectState.getStatus()) {
                BleManager.getInstance().startQueryBatteryLevel();
            } else {
                BleManager.getInstance().stopQueryBatteryLevel();
                if (isStackTop) {
                    DialogUtil.showConnectFailDialog(this,null);
                }
            }
        }
//        if (message instanceof LanguageMessage) {
//            LanguageMessage languageMessage = (LanguageMessage) message;
//            Log.i(TAG,"language="+languageMessage.getLanguage());
//            MultiLanguageUtils.INSTANCE.changeLanguage(this);
//            mHandler.postDelayed(this::recreate, 70);
//        }
    }

    @Override
    public void onItemClick(DeviceListAdapter adapter, View view, int position) {
        if (position >= adapter.getDeviceInfos().size()) {
            return;
        }
        DeviceInfo deviceInfo = adapter.getDeviceInfos().get(position);
        Log.i(TAG, "onItemClick: position:" + position);
        if (!BleManager.getInstance().isConnected()) {
            deviceInfo.setStatus(DeviceState.STATE_CONNECTING);
            adapter.notifyItemChanged(position);
            BleScanHandler.getInstance().stopScan();
            BleManager.getInstance().connect(deviceInfo.getAddress());
            return;
        }
        switch (deviceInfo.getStatus()) {
            case DeviceState.STATE_DISCONNECTED:
                if (isHasDeviceConnecting()) {
                    Toast.makeText(getApplication(), getString(R.string.other_devices_conncect)
                            , Toast.LENGTH_SHORT).show();
                    return;
                }
                deviceInfo.setStatus(DeviceState.STATE_CONNECTING);
                adapter.notifyItemChanged(position);
                BleScanHandler.getInstance().stopScan();
                //发起链接
                BleManager.getInstance().connect(deviceInfo.getAddress());
                break;
            case DeviceState.STATE_CONNECTED:
                //todo跳转到设置页面
                boolean connected = isConnected(deviceInfo, BleManager.getInstance().getConnectedAddresses());
                if (connected) {
                    jump2Settings(deviceInfo);
                }
                break;
            case DeviceState.STATE_CONNECTING:
                Toast.makeText(getApplication(), getString(R.string.connecting), Toast.LENGTH_SHORT).show();
                break;
            default:
                break;
        }
    }

    private void jump2Settings(DeviceInfo deviceInfo) {
        FastSharedPreferences languageSharedPreferences = FastSharedPreferences.get(MultiLanguageUtils.LANGUAGE);
        languageSharedPreferences.edit().putString(MultiLanguageUtils.LANGUAGE,
                MultiLanguageUtils.FOLLOW_SYSTEM).commit();

        languageSharedPreferences.edit().
                putInt(MultiLanguageUtils.LANGUAGE_TAG, 0).commit();

        String name = deviceInfo.getName();
        Intent intent = new Intent(this, SettingActivity.class);
        intent.putExtra(Configs.KEY_DEVICE_NAME, TextUtils.isEmpty(name) ? deviceInfo.getAddress() : name);
        intent.putExtra(Configs.KEY_DEVICE_ADDRESS, deviceInfo.getAddress());
        startActivity(intent);
    }

    /**
     * 判断当前是否有设备正在连接
     *
     * @return
     */
    private boolean isHasDeviceConnecting() {
        for (int i = 0; i < deviceList.size(); i++) {
            DeviceInfo deviceInfo = deviceList.get(i);
            if (deviceInfo.getStatus() == DeviceState.STATE_CONNECTING) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onItemLongClick(DeviceListAdapter adapter, View view, int position) {
        int size = adapter.getDeviceInfos().size();
        if (size == 0 || position >= size) {
            return;
        }
        DeviceInfo deviceInfo = adapter.getDeviceInfos().get(position);
        Log.i(TAG, "onItemClick: position:" + position);
        switch (deviceInfo.getStatus()) {
            case DeviceState.STATE_CONNECTED:
                boolean connected = isConnected(deviceInfo, BleManager.getInstance().getConnectedAddresses());
                if (connected) {
                    BleManager.getInstance().disconnect();
                }
                break;
            default:
                break;
        }
    }

    private void switchLoadingState(boolean state) {
        Log.i(TAG, "switchLoadingState: state:" + state);
        if (state) {
            tvSearchTip.setText(getString(R.string.searching));
            ivLoading.setVisibility(View.VISIBLE);
            llLoadTip.setVisibility(View.VISIBLE);
            ivLoading.startAnimation(getRotateAnimation());
        } else {
            ivLoading.clearAnimation();
            llLoadTip.setVisibility(View.GONE);
        }
    }

    private RotateAnimation getRotateAnimation() {
        RotateAnimation rotateAnimation = new RotateAnimation(0f, 360f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        rotateAnimation.setInterpolator(new LinearInterpolator());
        rotateAnimation.setDuration(2 * 1000);
        rotateAnimation.setRepeatCount(Integer.MAX_VALUE);
        rotateAnimation.setFillAfter(false);
        return rotateAnimation;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "onDestroy: ");
        FLAG_FIRS_OPEN_SWITCH = false;
        if (ivLoading != null) {
            ivLoading.clearAnimation();
        }
        BleScanHandler.getInstance().removeBleScanCallback(bleScanCallback);
    }
}