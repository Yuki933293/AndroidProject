package com.luxshare.welcom;


import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.Nullable;

import com.alibaba.android.arouter.launcher.ARouter;
import com.luxshare.base.activity.BaseActivity;
import com.luxshare.base.utils.StartTimeManagement;
import com.luxshare.configs.PathConfig;
import com.luxshare.home.HomeActivity;

public class SplashActivity extends BaseActivity {
    private static final String TAG = "SplashActivity";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        StartTimeManagement.endTime();
        Log.i(TAG, "onCreate");
        HomeActivity.isShowToast = true;

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            ARouter.getInstance().build(PathConfig.Path_HomeActivity).navigation();
            finish();
        }, StartTimeManagement.getDuration());
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i(TAG, "onResume: time:"+StartTimeManagement.getDuration());
    }
}