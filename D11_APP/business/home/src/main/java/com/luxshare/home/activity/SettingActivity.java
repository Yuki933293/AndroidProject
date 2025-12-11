package com.luxshare.home.activity;

import static com.luxshare.configs.Configs.TYPE_SINGLE_CHOICE;
import static com.luxshare.configs.Configs.TYPE_SWITCH;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.alibaba.android.arouter.utils.TextUtils;
import com.luxshare.base.utils.BleCMDUtil;
import com.luxshare.base.utils.MultiLanguageUtils;
import com.luxshare.base.utils.NotificationHelper;
import com.luxshare.base.utils.OTAInfoType;
import com.luxshare.base.view.LuxTitleBar;
import com.luxshare.ble.BleManager;
import com.luxshare.ble.eventbus.EventBusMessage;
import com.luxshare.ble.eventbus.IEventBusMessage;
import com.luxshare.ble.eventbus.message.ConnectState;
import com.luxshare.ble.eventbus.message.LanguageMessage;
import com.luxshare.ble.eventbus.message.OtaMessage;
import com.luxshare.ble.eventbus.message.ResultMessage;
import com.luxshare.configs.Configs;
import com.luxshare.configs.ItemCommandManger;
import com.luxshare.configs.PathConfig;
import com.luxshare.configs.bean.ItemCommand;
import com.luxshare.fastsp.FastSharedPreferences;
import com.luxshare.home.HomeActivity;
import com.luxshare.home.R;
import com.luxshare.home.util.DialogUtil;
import com.luxshare.home.view.CommandItemView;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.Objects;

/**
 * 设置页面
 */
public class SettingActivity extends BaseSpeakerActivity implements CommandItemView.ClickCallback {
    private static final String TAG = "SettingActivity";
    private final CommandItemView[] civs = new CommandItemView[15];
    private ImageView batteryIcon;
    private String offTimeNick;
    /**
     * 电量
     */
    private TextView batteryLevel;
    private final int[] batteryLevelIcon = {R.drawable.ic_battery_percentage_5, R.drawable.ic_battery_percentage_20,
            R.drawable.ic_battery_percentage_40, R.drawable.ic_battery_percentage_60, R.drawable.ic_battery_percentage_100};
    private ImageButton playStateSwith;
    /**
     * find my 一分钟后状态暂停；
     */
    private final Runnable resetPlayStateTask = () -> {
        playStateSwith.setTag(getResources().getString(R.string.audio_play));
        playStateSwith.setImageResource(R.drawable.ic_music_play);
        BleManager.getInstance().write(ItemCommandManger.of().getSendCmd(Configs.CMD_FIND_MY,
                Configs.OPERATE_SWITCH_OFF));
    };
    /**
     * 当前连接的设备地址
     */
    private String deviceAdrress;
    /**
     * 是否手动断开设备连接
     */
    private boolean isDisConnectedManual = false;

    private TextView toast_tip;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);
        initView();
    }

    private void initView() {
        LuxTitleBar titleBar = findViewById(R.id.title_bar);
        titleBar.setClickCallback(this::finish);
        TextView deviceName = findViewById(R.id.device_name);
        batteryIcon = findViewById(R.id.battery_icon);
        batteryLevel = findViewById(R.id.battery_level);
        initItems();
        Intent intent = getIntent();
        if (intent.hasExtra(Configs.KEY_DEVICE_NAME)) {
            String deviceNicky = intent.getStringExtra(Configs.KEY_DEVICE_NAME);
            deviceName.setText(deviceNicky);
            deviceAdrress = intent.getStringExtra(Configs.KEY_DEVICE_ADDRESS);
        }
    }

    private void initItems() {
        civs[0] = findViewById(R.id.civ_location);
        civs[1] = findViewById(R.id.civ_reconnect);
        civs[2] = findViewById(R.id.civ_voice_reminder);
        civs[3] = findViewById(R.id.civ_voice_reminder_language);
        civs[4] = findViewById(R.id.civ_auto_off);
        civs[5] = findViewById(R.id.civ_clear_paired_list);
        civs[6] = findViewById(R.id.civ_factory_reset);
        civs[7] = findViewById(R.id.civ_light_strip_setting);
        civs[8] = findViewById(R.id.civ_music_mode);
        civs[9] = findViewById(R.id.civ_notifications_and_prompts);
        civs[10] = findViewById(R.id.civ_update_firmware);
        civs[11] = findViewById(R.id.civ_ota_update);
        civs[12] = findViewById(R.id.civ_distribution_net);
        civs[13] = findViewById(R.id.civ_disconnect);
        civs[14] = findViewById(R.id.civ_meeting);
        toast_tip = findViewById(R.id.toast_tip);
        for (CommandItemView commandItemView : civs
        ) {
            commandItemView.setClickCallback(this);
        }
        playStateSwith = findViewById(R.id.play_state_switch);
        playStateSwith.setOnClickListener(v -> {
            String tag = playStateSwith.getTag().toString();
            String play = getResources().getString(R.string.audio_play);
            if (play.equals(tag)) {
                playStateSwith.setTag(getResources().getString(R.string.audio_stop));
//                BleManager.getInstance().setConfigedWifi("wifi_setup"+" Luxshare-Guest "+"NJ@luxshare");
                playStateSwith.setImageResource(R.drawable.ic_music_stop);
                BleManager.getInstance().add(ItemCommandManger.of().getSendCmd(Configs.CMD_FIND_MY,
                        Configs.OPERATE_SWITCH_ON));
                mHandler.removeCallbacks(resetPlayStateTask);
                mHandler.postDelayed(resetPlayStateTask, 60 * 1000);
            } else {
                mHandler.removeCallbacks(resetPlayStateTask);
                playStateSwith.setTag(play);
                playStateSwith.setImageResource(R.drawable.ic_music_play);
                BleManager.getInstance().add(ItemCommandManger.of().getSendCmd(Configs.CMD_FIND_MY,
                        Configs.OPERATE_SWITCH_OFF));
            }
        });
    }

    @Override
    public void onItemClick(CommandItemView v) {
        if (civs[0] == v) {
            Intent intent = new Intent(this, DetailActivity.class);
            intent.putExtra(Configs.PAGE_PATH, PathConfig.Path_SoundLocationFragment);
            Bundle bundle = new Bundle();
            bundle.putString(Configs.KEY_TITLE_SELECTED, civs[0].getTitle());
            bundle.putString(Configs.KEY_DEVICE_ADDRESS, deviceAdrress);
            intent.putExtra(Configs.BUNDLE, bundle);
            startActivity(intent);
        } else if (civs[3] == v) {
            Intent intent = new Intent(this, DetailActivity.class);
            intent.putExtra(Configs.PAGE_PATH, PathConfig.Path_LanguageFragment);
            Bundle bundle = new Bundle();
            bundle.putString(Configs.KEY_TITLE_SELECTED, civs[3].getDescribe());
            bundle.putString(Configs.KEY_DEVICE_ADDRESS, deviceAdrress);
            intent.putExtra(Configs.BUNDLE, bundle);
            startActivity(intent);
        } else if (civs[4] == v) {
            Intent intent = new Intent(this, DetailActivity.class);
            intent.putExtra(Configs.PAGE_PATH, PathConfig.Path_OffTimeFragment);
            Bundle bundle = new Bundle();
            bundle.putString(Configs.KEY_TITLE_SELECTED, offTimeNick);
            bundle.putString(Configs.KEY_DEVICE_ADDRESS, deviceAdrress);
            intent.putExtra(Configs.BUNDLE, bundle);
            startActivity(intent);
        } else if (civs[5] == v) {
            DialogUtil.showTipDialog(this, getString(R.string.clear_pair_tip), v1 ->
                    BleManager.getInstance().write(ItemCommandManger.of().getSendCmd(Configs.CMD_CLEAR_LIST,
                    Configs.OPERATE_SWITCH_ON)));
        } else if (civs[6] == v) {
            DialogUtil.showFactoryResetDialog(this, v12 -> {
                isDisConnectedManual = false;
                BleManager.getInstance().write(ItemCommandManger.of().getSendCmd(Configs.CMD_FACTORY_RESET,
                        Configs.OPERATE_SWITCH_ON));
            });
        } else if (civs[8] == v) {
            Intent intent = new Intent(this, DetailActivity.class);
            intent.putExtra(Configs.PAGE_PATH, PathConfig.Path_AudioModeFragment);
            Bundle bundle = new Bundle();
            bundle.putString(Configs.KEY_TITLE_SELECTED, civs[8].getDescribe());
            bundle.putInt("audio_mode", AUDIO_MODE);
            bundle.putString(Configs.KEY_DEVICE_ADDRESS, deviceAdrress);
            intent.putExtra(Configs.BUNDLE, bundle);
            startActivity(intent);
        } else if (civs[11] == v) {
            Intent intent = new Intent(this, DetailActivity.class);
            intent.putExtra(Configs.PAGE_PATH, PathConfig.Path_OtaFragment);
            Bundle bundle = new Bundle();
            bundle.putString(Configs.KEY_DEVICE_ADDRESS, deviceAdrress);
            intent.putExtra(Configs.BUNDLE, bundle);
            startActivity(intent);
        } else if (civs[12] == v) {
            DialogUtil.showConfigNetDialog(this, getString(R.string.distribution_network), (wifiName, pwd) -> {
                Log.i(TAG, "getWIFIConfig: wifiName:" + wifiName + ",pwd:" + pwd);

                toast_tip.setVisibility(View.VISIBLE);
                toast_tip.setText(getString(R.string.distribution_network_start));

                byte[] sendCmdWifi = ItemCommandManger.of().getConfigNetCmd(Configs.CMD_CONFIG_NET_WIFI, wifiName,pwd);
                BleManager.getInstance().write(sendCmdWifi);
            });
        } else if (civs[13] == v) {
            DialogUtil.showTipDialog(this, getString(R.string.disconnect_tip), v1 -> {
                isDisConnectedManual = true;
                Log.i(TAG, "onItemClick: disconnet");
                BleManager.getInstance().disconnect();
                startActivity(new Intent(this, HomeActivity.class));
                finish();
            });
        } else if (civs[14] == v) {
            Intent intent = new Intent(this, DetailActivity.class);
            intent.putExtra(Configs.PAGE_PATH, PathConfig.Path_MeetingFragment);
            Bundle bundle = new Bundle();
            bundle.putString(Configs.KEY_DEVICE_ADDRESS, deviceAdrress);
            intent.putExtra(Configs.BUNDLE, bundle);
            startActivity(intent);
        } else {
            Toast.makeText(getApplication(), getString(R.string.under_development)
                    , Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onSwitchStateChange(Object itemTag, boolean switchSate) {
        if (itemTag instanceof String) {
            String tag = (String) itemTag;
            ItemCommand itemCommand = ItemCommandManger.of().getItemCommandByTag(tag);
            String switchCommandKey = ItemCommandManger.of().getSwitchKeyByState(tag, switchSate);
            int command = ItemCommandManger.of().getCommandBy(itemCommand, switchCommandKey);
            byte[] sendCmd = ItemCommandManger.of().getSendCmd(itemCommand.getNicky(), command);
            ItemCommandManger.of().printByteArray(TAG, sendCmd);
            BleManager.getInstance().add(sendCmd);
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(EventBusMessage msg) {
        String resultType = msg.getTag();
        IEventBusMessage message = msg.getMessage();
        if (message instanceof ResultMessage) {
            ResultMessage result = (ResultMessage) message;
            Log.i(TAG, "onMessageEvent: Nicky:" + result.getNicky());
            if (result.getNicky() == Configs.CMD_BATTERY) {
                int level = result.getResult();
                Log.i(TAG, "receive battery level:" + level);
                batteryLevel.setText(level + "%");
                if (level >= 100) {
                    batteryIcon.setBackgroundResource(batteryLevelIcon[4]);
                } else if (level >= 60) {
                    batteryIcon.setBackgroundResource(batteryLevelIcon[3]);
                } else if (level >= 40) {
                    batteryIcon.setBackgroundResource(batteryLevelIcon[2]);
                } else if (level >= 20) {
                    batteryIcon.setBackgroundResource(batteryLevelIcon[1]);
                } else {
                    batteryIcon.setBackgroundResource(batteryLevelIcon[0]);
                }
                return;
            } else if (result.getNicky() == Configs.CMD_CLEAR_LIST) {
                //收到清空蓝牙列表回来的消息则断开本地蓝牙然后跳转页面
                Log.i(TAG, "receive clear bluetooth list callback");
                isDisConnectedManual = true;
                BleManager.getInstance().disconnect();
                startActivity(new Intent(this, HomeActivity.class));
                finish();
                return;
            } else if (result.getNicky() == Configs.CMD_FACTORY_RESET) {
                Log.i(TAG, "receive FACTORY_RESET");
                BleManager.getInstance().disconnect();
                startActivity(new Intent(this, HomeActivity.class));
                finish();
                return;
            }
            String tag = Configs.getNicky2TagMap().get(result.getNicky());
            if (tag == null) {
                Log.e(TAG, "the tag is mull");
                return;
            }
            CommandItemView itemView = getItemByTag(tag);
            Log.i(TAG, "tag="+tag);
            if (itemView != null) {
                ItemCommand itemCommand = ItemCommandManger.of().getItemCommandByTag(tag);
                Log.i(TAG, "itemCommand="+itemCommand);
                if (itemCommand == null) {
                    return;
                }
                if (itemCommand.getType() == TYPE_SWITCH) {
                    int status = result.getResult();
                    if (itemCommand.getNicky() == Configs.CMD_FIND_MY) {
                        Log.i(TAG, "onMessageEvent: findmy status : " + status);
                    } else {
                        itemView.refeshSwitchState(status == Configs.OPERATE_SWITCH_ON);
                    }
                } else if (itemCommand.getType() == TYPE_SINGLE_CHOICE) {
                    if (result.getNicky() == Configs.CMD_LANGUAGE) {
                        // String language = new Configs().getLanguageMap(appContext).get(result.getResult());
                        String language = "";
                        int tempLanguageTag;

                        if (result.getResult() == Configs.OPERATE_LANGUAGE_EN) {
                            language = getString(R.string.english);
                            tempLanguageTag = Configs.OPERATE_LANGUAGE_EN;
                        } else {
                            language = getString(R.string.chinese);
                            tempLanguageTag = Configs.OPERATE_LANGUAGE_CN;
                        }

                        Log.i(TAG, "language="+language);
                        if (!TextUtils.isEmpty(language)) {
                            civs[3].setTips(language);
                        }

//                        String tempLanguage = Objects.equals(language, getString(R.string.english))?
//                                MultiLanguageUtils.ENGLISH : MultiLanguageUtils.CHINESE;

//                        String savedLanguage = FastSharedPreferences.get(MultiLanguageUtils.LANGUAGE).getString(
//                                MultiLanguageUtils.LANGUAGE,
//                                MultiLanguageUtils.FOLLOW_SYSTEM
//                        );

                        int savedLanguageTag = FastSharedPreferences.get(MultiLanguageUtils.LANGUAGE).getInt(
                                MultiLanguageUtils.LANGUAGE_TAG,
                              0
                        );

                        Log.i(TAG, "tempLanguageTag="+tempLanguageTag+",savedLanguageTag="+savedLanguageTag);
                        // 如果相同则不切
                        if (savedLanguageTag == tempLanguageTag) {
                            Log.i(TAG, "language same");
                            return;
                        }

                        FastSharedPreferences.get(MultiLanguageUtils.LANGUAGE).edit().
                                putInt(MultiLanguageUtils.LANGUAGE_TAG, tempLanguageTag).commit();

                        if (tempLanguageTag == Configs.OPERATE_LANGUAGE_EN) {
                            MultiLanguageUtils.INSTANCE.changeLanguage(
                                    this, MultiLanguageUtils.ENGLISH, MultiLanguageUtils.US
                            );
                        } else  {
                            MultiLanguageUtils.INSTANCE.changeLanguage(
                                    this, MultiLanguageUtils.CHINESE, MultiLanguageUtils.CHINA
                            );
                        }
//                        EventBus.getDefault().post(
//                                new EventBusMessage(
//                                        EventBusMessage.MESSAGE_TYPE_LANGUAGE,
//                                        new LanguageMessage(MultiLanguageUtils.SWITCH_LANGUAGE)
//                                )
//                        );
                        Log.i(TAG, "recreate");
                        mHandler.postDelayed(this::recreate, 120);
                    } else if (result.getNicky() == Configs.CMD_AUTO_SHUTDOWN) {
                        int state = result.getResult();
                        Log.i(TAG, "receive shutdown state: " + state);
                        Context appContext = getApplicationContext();
                        String offTimeTitle = new Configs().getOffTimeMap(appContext).get(state);
                        Log.i(TAG, "offTimeTitle="+offTimeTitle);
                        civs[4].setTips(offTimeTitle);
                        offTimeNick = Configs.getOffTimeNicky(state);
                    } else if (result.getNicky() == Configs.CMD_MUSIC_MODE) {
                        AUDIO_MODE = result.getResult();
                        Log.i(TAG, "audio mode state: " + AUDIO_MODE);
                        Context appContext = getApplicationContext();
                        String des = AUDIO_MODE == Configs.OPERATE_MUSIC_MODE ?
                                appContext.getString(R.string.music_mode) :
                                appContext.getString(R.string.meeting_mode);
                        civs[8].setTips(des);
                    }
                } else  {
                    Log.i(TAG, "nick="+ itemCommand.getNicky());
                    if (itemCommand.getNicky() == Configs.CMD_CONFIG_NET_WIFI) {
                        int status =  result.getResult();
                        String tips = status == 1? getString(R.string.distribution_network_success)
                                : getString(R.string.distribution_network_fail);
                        toast_tip.setVisibility(View.VISIBLE);
                        toast_tip.setText(tips);
                        mHandler.postDelayed(() -> toast_tip.setVisibility(View.GONE),8* 1000);
                        Context appContext = getApplicationContext();
                        civs[12].setTips( status == 1?
                                appContext.getString(R.string.network_connected)
                                : appContext.getString(R.string.network_not_connected));
                    }
                }
            }
        } else if (message instanceof ConnectState) {
            ConnectState connectState = (ConnectState) message;
            Log.i(TAG, "device connectStatus :" + connectState.getStatus());
            if (!connectState.getStatus()) {
                BleManager.getInstance().stopQueryBatteryLevel();
                if (!isDisConnectedManual && connectState.getAddress().equalsIgnoreCase(deviceAdrress)) {
                    clearSp();
                    DialogUtil.showReconnectDialog(this, v1 -> {
                        startActivity(new Intent(this, HomeActivity.class));
                        finish();
                    });
                }
            }
        } else if (message instanceof OtaMessage) {
            OtaMessage otaMessage = (OtaMessage) message;
            if (otaMessage.getTag().equals("localVersion")) {
                civs[11].setTips(otaMessage.getVersion());
            }
            if (otaMessage.getTag().equals("otaInfo")) {
                Context appContext = getApplicationContext();
                if (otaMessage.getInfoType() == OTAInfoType.UPGRADE_NOTIFICATION.getType()) {
                    NotificationHelper notificationHelper = new NotificationHelper(this);
                    notificationHelper.showNotification(
                            getString(R.string.upgrade_notification_title),
                            getString(R.string.upgrade_notification_message));
                } else if (otaMessage.getInfoType() == OTAInfoType.NO_NETWORK.getType()) {
                    civs[12].setTips(appContext.getString(R.string.network_not_connected));
                } else if (otaMessage.getInfoType() == OTAInfoType.WITH_NETWORK.getType()) {
                    civs[12].setTips(appContext.getString(R.string.network_connected));
                }
            }
        }
    }

    int AUDIO_MODE = Configs.OPERATE_MEETING_MODE;

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(Bundle bundle) {
        if (bundle.containsKey(Configs.KEY_LANGUAGE)) {
            String languageName = bundle.getString(Configs.KEY_LANGUAGE);
            String languageNick = Configs.KEY_LANGUAGE_CN;
            if (!languageName.equals(getString(R.string.chinese))) {
                languageNick = Configs.KEY_LANGUAGE_EN;
            }
            sendLanguageCmd(languageNick);
        } else if (bundle.containsKey(Configs.KEY_TIME)) {
            String offTimeNick = bundle.getString(Configs.KEY_TIME);
            Log.i(TAG, " offTimeNick:" + offTimeNick);
            //发送指令等结果回来刷新单选项，如果无返回则还是上次值；
            ItemCommand offCommand = ItemCommandManger.of().getItemCommandByTag(
                    getString(R.string.tag_lauto_off));
            int commandValue = ItemCommandManger.of().getCommandBy(offCommand, offTimeNick);
            byte[] sendOffCmd = ItemCommandManger.of().getSendCmd(Configs.CMD_AUTO_SHUTDOWN, commandValue);
            BleManager.getInstance().add(sendOffCmd);
        } else if (bundle.containsKey(Configs.KEY_AUDIO_MODE)) {
            Context appContext = getApplicationContext();
            AUDIO_MODE = bundle.getInt(Configs.KEY_AUDIO_MODE);
            Log.i(TAG, " audioMode:" + AUDIO_MODE);
            String des = AUDIO_MODE == Configs.OPERATE_MUSIC_MODE ?
                    appContext.getString(R.string.music_mode) : appContext.getString(R.string.meeting_mode);
            civs[8].setTips(des);
            byte[] sendOffCmd = ItemCommandManger.of().getSendCmd(Configs.CMD_MUSIC_MODE, AUDIO_MODE);
            BleManager.getInstance().add(sendOffCmd);
        } else {
            Log.i(TAG, "onMessageEvent: ");
        }
    }

    private void sendLanguageCmd(String languageNick) {
        ItemCommand languageCommand = ItemCommandManger.of().getItemCommandByTag(
                getString(R.string.tag_language));
        int commandValue = ItemCommandManger.of().getCommandBy(languageCommand, languageNick);
        Log.i(TAG, "onMessageEvent: language value:" + commandValue);
        byte[] sendOffCmd = ItemCommandManger.of().getSendCmd(Configs.CMD_LANGUAGE, commandValue);
        BleManager.getInstance().add(sendOffCmd);
    }

    private CommandItemView getItemByTag(String tag) {
        for (CommandItemView itemView : civs) {
            if (tag.equals(itemView.getTag().toString())) {
                return itemView;
            }
        }
        return null;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (BleManager.getInstance().isConnected()) {
            byte[] sendOffCmd = ItemCommandManger.of().getSendCmd(Configs.CMD_QUERY, 0);
            BleManager.getInstance().add(sendOffCmd);
            BleManager.getInstance().add(BleCMDUtil.INSTANCE.queryMessageCMD(BleCMDUtil.OTA_NETWORK_L));
        } else {
            Log.i(TAG, "the device disconnect");
            clearSp();

            DialogUtil.showReconnectDialog(this, v1 -> {
                startActivity(new Intent(this, HomeActivity.class));
                finish();
            });
        }
    }

    private void clearSp() {
//        FastSharedPreferences languageSharedPreferences = FastSharedPreferences.get(MultiLanguageUtils.LANGUAGE);
//        languageSharedPreferences.edit().putString(MultiLanguageUtils.LANGUAGE,
//                MultiLanguageUtils.FOLLOW_SYSTEM).commit();
    }
}
