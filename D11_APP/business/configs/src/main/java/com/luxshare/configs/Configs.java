package com.luxshare.configs;

import android.content.Context;

import java.util.HashMap;

final public class Configs {
    private static HashMap<Byte, String> nicky2TagMap;
    private HashMap<Integer, String> languageMap;
    private HashMap<Integer, String> offTimeMap;

    public static Configs getInstance() {
        if (instance == null) {
            instance = new Configs();
        }
        return instance;
    }

    private static Configs instance;

    /**
     * 页面路径存储key
     */
    public final static String PAGE_PATH = "path";
    /**
     * 存储Bundle的key
     */
    public final static String BUNDLE = "bundle";
    public final static String KEY_TITLE_SELECTED = "key_title_selected";
    /**
     * 设备名称
     */
    public final static String KEY_DEVICE_NAME = "key_device_name";
    public final static String KEY_DEVICE_ADDRESS = "key_device_address";


    public final static String MESSAGE_TYPE_ITEM = "item";
    public final static String MESSAGE_TYPE_BATTERY = "battery";
    /**
     * 查询电量时间间隔；
     */
    public final static long QUERY_BATTERY_LEVEL_PERIOD = 3 * 60 * 1000;

    /**
     * ItemCommand的type类型
     */
    public final static int TYPE_ALONE = 1;
    public final static int TYPE_SWITCH = 2;
    public final static int TYPE_SINGLE_CHOICE = 3;
    /**
     * item指令key的名称
     */
    //switch
    public final static String KEY_SOUND_ON = "sound_on";
    public final static String KEY_SOUND_OFF = "sound_off";

    public final static String KEY_RECONNECT_ON = "reconnect_on";
    public final static String KEY_RECONNECT_OFF = "reconnect_off";

    public final static String KEY_REMIND_ON = "remind_on";
    public final static String KEY_REMIND_OFF = "remind_off";

    public final static String KEY_LANGUAGE = "language";
    public final static String KEY_LANGUAGE_CN = "language_cn";
    public final static String KEY_LANGUAGE_EN = "language_en";

    public final static String KEY_TIME = "time";
    public final static String KEY_TIME_NAME = "time_name";
    public final static String KEY_TIME_TEN = "time_ten";
    public final static String KEY_TIME_THIRTY = "time_thirty";
    public final static String KEY_TIME_SIXTY = "time_sixty";
    public final static String KEY_TIME_ON = "time_on";

    public final static String KEY_CLEAR_PAIR_LIST = "clear_pair_list";
    public final static String KEY_FACTORY_RESET = "factory_reset";

    public final static String KEY_AUDIO_MODE = "audio_mode";

    public final static String KEY_MUSIC_MODE = "music_mode";

    public final static String KEY_MEETING_MODE = "meeting_mode";

    /**
     * action
     */
    public final static String ACTION_SOUND = "sound";


    /**
     * item功能cmd代号
     */
    public final static byte CMD_BATTERY = 0x11;
    public final static byte CMD_LOCATION = 0x01;
    public final static byte CMD_RECONNECT = 0x02;
    public final static byte CMD_REMIND = 0x03;
    /**
     * 语音提醒语言
     */
    public final static byte CMD_LANGUAGE = 0x04;
    public final static int OPERATE_LANGUAGE_CN = 1;
    public final static int OPERATE_LANGUAGE_EN = 2;
    /**
     * 自动关机
     */
    public final static byte CMD_AUTO_SHUTDOWN = 0x05;
    public final static int OPERATE_TIME_TEN = 1;
    public final static int OPERATE_TIME_THIRTY = 2;
    public final static int OPERATE_TIME_SIXTY = 3;
    public final static int OPERATE_TIME_ON = 4;
    /**
     * 清空蓝牙列表
     */
    public final static byte CMD_CLEAR_LIST = 0x06;
    public final static byte CMD_FACTORY_RESET = 0x07;
    /**
     * 环形灯带
     */
    public final static byte CMD_LIGHT_STRIP = 0x08;
    public final static byte CMD_MUSIC_MODE = 0x09;
    /**
     * 管理通知和提示
     */
    public final static byte CMD_NOTIFICATION_PROMPT = 0x0A;
    public final static byte CMD_UPDATE_FIRMWARE = 0x0B;
    public final static byte CMD_OTA_UPDATE = 0x0C;
    public final static byte CMD_FIND_MY = 0x0D;
    /**
     * 查询
     */
    public final static byte CMD_QUERY = 0x66;
    /**
     * 配网
     */
    public final static byte CMD_CONFIG_NET = 0x20;
    public final static byte CMD_CONFIG_NET_WIFI = 0x21;

    /**
     * 发送指令头
     */
    public final static byte CMD_SEND_HEAD = 0x36;
    /**
     * 接收指令头
     */
    public final static byte CMD_RECIEVE_HEAD = 0x63;

    /**
     * 设置模块标识符
     */
    public final static byte CMD_GROUP_SETTING = 0x50;

    /**
     * 开关能力打开
     */
    public final static int OPERATE_SWITCH_ON = 1;
    /**
     * 开关能力关闭
     */
    public final static int OPERATE_SWITCH_OFF = 2;
    /**
     * 无效值
     */
    public final static int OPERATE_INVALID = -1;

    /**
     * 单独功能项执行
     */
    public final static int OPERATE_EXE_ONLY = OPERATE_SWITCH_ON;

    public final static int OPERATE_MUSIC_MODE = 1;
    public final static int OPERATE_MEETING_MODE = 0;

    public final static String OTA_LOCAL_PATH = "ota_local_path";

    public final static String OTA_SERVER_PATH = "ota_server_path";

    public final static String OTA_LOCAL_VERSION = "ota_local_version";

    public final static String OTA_SERVER_VERSION = "ota_server_version";

    private static Context context;

    static {
        context = ContextManager.getInstance().getContext();
        config2CmdNicky();
    }

    public Configs() {

    }

    private static void config2CmdNicky() {
        nicky2TagMap = new HashMap<>();
//        nicky2TagMap.put(Configs.CMD_LOCATION, context.getString(R.string.tag_sound_location));
//        nicky2TagMap.put(Configs.CMD_RECONNECT, context.getString(R.string.tag_reconnect));
//        nicky2TagMap.put(Configs.CMD_REMIND, context.getString(R.string.tag_remind));
        nicky2TagMap.put(Configs.CMD_LANGUAGE, context.getString(R.string.tag_language));
        nicky2TagMap.put(Configs.CMD_AUTO_SHUTDOWN, context.getString(R.string.tag_lauto_off));
        nicky2TagMap.put(Configs.CMD_CONFIG_NET_WIFI, context.getString(R.string.tag_distribution_network));
        nicky2TagMap.put(Configs.CMD_MUSIC_MODE, context.getString(R.string.tag_music_mode));
    }

    public static HashMap<Byte, String> getNicky2TagMap() {
        return nicky2TagMap;
    }

    public HashMap<Integer, String> getLanguageMap(Context context) {
        languageMap = new HashMap<>();
        languageMap.put(OPERATE_LANGUAGE_CN, context.getString(R.string.chinese));
        languageMap.put(OPERATE_LANGUAGE_EN, context.getString(R.string.english));
        return languageMap;
    }

    public HashMap<Integer, String> getOffTimeMap(Context context) {
        offTimeMap = new HashMap<>();
        offTimeMap.put(OPERATE_TIME_TEN, context.getString(R.string.off_time_ten));
        offTimeMap.put(OPERATE_TIME_THIRTY, context.getString(R.string.off_time_thirty));
        offTimeMap.put(OPERATE_TIME_SIXTY, context.getString(R.string.off_time_sixty));
        offTimeMap.put(OPERATE_TIME_ON, context.getString(R.string.off_time_on));
        return offTimeMap;
    }

    public static String getOffTimeNicky(int offValue) {
        String[] keyArray = context.getResources().getStringArray(R.array.off_time_key);
        return keyArray[offValue - 1];
    }
}
