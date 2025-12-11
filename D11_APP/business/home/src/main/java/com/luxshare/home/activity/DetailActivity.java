package com.luxshare.home.activity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;

import com.elvishew.xlog.XLog;
import com.luxshare.base.fragment.FragmentHanler;
import com.luxshare.configs.Configs;
import com.luxshare.home.R;

public class DetailActivity extends BaseSpeakerActivity {
    private static final String TAG = "DetailActivity";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);
        FragmentHanler.of().init(this);
        Intent intent = getIntent();
        String path = intent.getStringExtra(Configs.PAGE_PATH);
        Bundle bundle = intent.getBundleExtra(Configs.BUNDLE);
        String name = bundle.getString(Configs.KEY_TITLE_SELECTED, "");
        Log.i(TAG, "onCreate: name:"+name);
        FragmentHanler.of().add(path, bundle, false);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
