package com.luxshare.home.fragment;

import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.alibaba.android.arouter.facade.annotation.Route;
import com.luxshare.configs.PathConfig;
import com.luxshare.home.R;
import com.luxshare.home.adapter.DeviceListAdapter;

import java.util.Objects;

@Route(path = PathConfig.Path_DeviceListFragment)
public class DeviceListFragment extends BaseSpeakerFragment  {

    private DeviceListAdapter deviceListAdapter;
    private RecyclerView list;

    @Override
    protected int getChildLayoutId() {
        return R.layout.fragment_device_list;
    }


    @Override
    protected void initChildView(View view) {
        ImageButton btnOperate = getTitleBar().getBtnOperate();
        btnOperate.setBackgroundResource(R.drawable.ic_refresh);
        btnOperate.setVisibility(View.VISIBLE);
        Button button = view.findViewById(R.id.btn_settings);
        button.setOnClickListener(v -> {
        });
        list = view.findViewById(R.id.list_device);
        DividerItemDecoration itemDecoration = new DividerItemDecoration(getContext(), LinearLayout.VERTICAL);
        itemDecoration.setDrawable(Objects.requireNonNull(ContextCompat.getDrawable(getContext(), R.drawable.devideline)));
        list.addItemDecoration(itemDecoration);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext(), RecyclerView.VERTICAL, false);
        list.setLayoutManager(layoutManager);
        deviceListAdapter = new DeviceListAdapter();
        list.setAdapter(deviceListAdapter);
    }

    @Override
    protected void initData() {
    }

    @Override
    protected CharSequence getBarTitle() {
        return getContext().getString(R.string.connected_devices);
    }

    @Override
    protected boolean isBackVisible() {
        return false;
    }


}
