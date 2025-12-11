package com.luxshare.base.xlog;

import com.elvishew.xlog.LogLevel;
import com.elvishew.xlog.flattener.Flattener;
import com.elvishew.xlog.flattener.Flattener2;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Created by CaoYanYan
 * Date: 2023/11/30 10:05
 **/
public class FactoryToolFlattener implements Flattener, Flattener2 {

    ThreadLocal<SimpleDateFormat> mLocalDateFormat = new ThreadLocal<SimpleDateFormat>() {

        @Override
        protected SimpleDateFormat initialValue() {
            return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US);
        }
    };

    @Override
    public CharSequence flatten(int logLevel, String tag, String message) {
        return flatten(System.currentTimeMillis(), logLevel, tag, message);
    }

    @Override
    public CharSequence flatten(long timeMillis, int logLevel, String tag, String message) {
        SimpleDateFormat simpleDateFormat = mLocalDateFormat.get();
        String time = simpleDateFormat.format(new Date(timeMillis));
        return time + '|' + LogLevel.getShortLevelName(logLevel) + '|' + tag + '|' + message;
    }
}
