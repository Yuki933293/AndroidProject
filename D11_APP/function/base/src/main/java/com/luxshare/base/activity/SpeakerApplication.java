package com.luxshare.base.activity;

import android.app.Application;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import androidx.multidex.MultiDex;

import com.alibaba.android.arouter.launcher.ARouter;
import com.elvishew.xlog.LogConfiguration;
import com.elvishew.xlog.LogLevel;
import com.elvishew.xlog.XLog;
import com.elvishew.xlog.printer.AndroidPrinter;
import com.elvishew.xlog.printer.ConsolePrinter;
import com.elvishew.xlog.printer.Printer;
import com.elvishew.xlog.printer.file.FilePrinter;
import com.elvishew.xlog.printer.file.backup.NeverBackupStrategy;
import com.elvishew.xlog.printer.file.clean.FileLastModifiedCleanStrategy;
import com.luxshare.base.BuildConfig;
import com.luxshare.base.utils.FileUtils;
import com.luxshare.base.utils.StartTimeManagement;
import com.luxshare.base.xlog.DateFileNameExtendGenerator;
import com.luxshare.base.xlog.FactoryToolFlattener;
import com.luxshare.base.xlog.XLogExt;
import com.luxshare.configs.ContextManager;
import com.luxshare.fastsp.FastSharedPreferences;
import java.io.File;


/**
 * @author CaoYanyan
 */
public class SpeakerApplication extends Application {
    private static final String TAG = "SpeakerApplication";
    private String appId = "84d7459b9b";
    private static final String LogTag = "LuxSpeaker";
    /**
     * 本地保存日志的时长
     */
    private static final long maxTimeMillis = 12 * 60 * 60 * 1000;

    @Override
    public void onCreate() {
        super.onCreate();
        StartTimeManagement.initStartTime();
        ContextManager.getInstance().setContext(this.getApplicationContext());
        MultiDex.install(this);
        if (isDebugTest()) {
            ARouter.openLog();
            ARouter.openDebug();
        }
        ARouter.init(this);
        FastSharedPreferences.init(this);
        //Bugly用于异常上报
        /*CrashReport.UserStrategy strategy = new CrashReport.UserStrategy(getApplicationContext());
        strategy.setAppChannel("debug");
        strategy.setAppPackageName(getPackageName());
        strategy.setAppVersion(getVersion());
        CrashReport.initCrashReport(getApplicationContext(), appId, true, strategy);*/
        //XLog日志打印、本地日志保存
        LogConfiguration config = new LogConfiguration.Builder()
                .logLevel(BuildConfig.DEBUG ? LogLevel.ALL : LogLevel.NONE)
                .tag(LogTag)
                .build();
        Printer androidPrinter = new AndroidPrinter(true);
        Printer consolePrinter = new ConsolePrinter(new FactoryToolFlattener());
        File file = new File(FileUtils.getDiskCacheDir(this) + File.separator + "log");
        if (!file.exists()) {
            file.mkdir();
        }
        String logDirPath = file.getAbsolutePath();
        Log.i(LogTag, "onCreate logDirPath:" + logDirPath);
        Printer filePrinter = new FilePrinter.Builder(logDirPath)
                .fileNameGenerator(new DateFileNameExtendGenerator())
                .backupStrategy(new NeverBackupStrategy())
                .flattener(new FactoryToolFlattener())
                .cleanStrategy(new FileLastModifiedCleanStrategy(maxTimeMillis))
                .build();
        XLog.init(config, androidPrinter, consolePrinter, filePrinter);
        XLogExt.INSTANCE.d(TAG, "onCreate");
//        EventBus.builder().addIndex(new MyEventBusIndex()).installDefaultEventBus();
    }


    /**
     * 默认开启debug模式
     *
     * @return
     */
    private boolean isDebugTest() {
        return true;
    }

    private String getVersion() {
        PackageInfo packageInfo = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            try {
                packageInfo = getPackageManager().getPackageInfo(getPackageName(), PackageManager.PackageInfoFlags.of(0));
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
        } else {
            try {
                packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
        }
        if (packageInfo != null) {
            return packageInfo.versionName;
        }
        return "";
    }
}
