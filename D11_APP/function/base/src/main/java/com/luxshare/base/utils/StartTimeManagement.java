package com.luxshare.base.utils;

/**
 * 用于获取开机时间
 * Created by CaoYanYan
 * Date: 2024/3/12 20:40
 **/
public class StartTimeManagement {
    private final static int UNKNOWN = -1;
    /**
     * 用于记录初始化开始时间
     */
    private static long STARTTIME = UNKNOWN;
    /**
     * 应用初始化时长
     */
    private static long INIT_DURATION = UNKNOWN;
    /**
     * 用于及时停留页面时长
     */
    private static long DURATION = 2 * 1000;

    private StartTimeManagement() {
    }

    public static void initStartTime() {
        if (STARTTIME == UNKNOWN) {
            STARTTIME = System.currentTimeMillis();
        }

    }

    public static void endTime() {
        if (INIT_DURATION == UNKNOWN) {
            INIT_DURATION = System.currentTimeMillis() - STARTTIME;
        }
    }

    public static long getDuration() {
        return DURATION - INIT_DURATION;
    }
}
