package com.luxshare.home.fragment;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import com.luxshare.base.fragment.BaseFragment;
import com.luxshare.base.view.LuxTitleBar;
import com.luxshare.ble.eventbus.EventBusMessage;
import com.luxshare.ble.eventbus.IEventBusMessage;
import com.luxshare.ble.eventbus.message.ConnectState;
import com.luxshare.configs.Configs;
import com.luxshare.home.HomeActivity;
import com.luxshare.home.R;
import com.luxshare.home.activity.DetailActivity;
import com.luxshare.home.util.DialogUtil;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

public abstract class BaseSpeakerFragment extends BaseFragment implements LuxTitleBar.BackCallback {

    private FragmentActivity activity;
    protected LuxTitleBar titleBar;
    private FragmentManager fragmentManager;
    private String deviceAddress = "";

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        activity = getActivity();
        fragmentManager = getActivity().getSupportFragmentManager();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this);
        }
    }

    @Override
    protected void initView(View view) {
        super.initView(view);
        titleBar = view.findViewById(R.id.title_bar);
        titleBar.setBackVisible(isBackVisible());
        titleBar.setTitle(getBarTitle());
        titleBar.setClickCallback(this);
        initBaseView(view);
        Bundle arguments = getArguments();
        if (arguments != null) {
            deviceAddress = arguments.getString(Configs.KEY_DEVICE_ADDRESS, "");
        }
        Log.i(TAG, "deviceAddress="+deviceAddress);
    }

    private void initBaseView(View view) {
        ViewGroup contentLayout = view.findViewById(R.id.content);
        View childView = LayoutInflater.from(requireContext()).inflate(getChildLayoutId(), contentLayout, true);
        initChildView(childView);
    }

    @Override
    protected int getLayoutResourceId() {
        return R.layout.fragment_item_base;
    }

    protected abstract int getChildLayoutId();

    protected abstract void initChildView(View view);

    protected CharSequence getBarTitle() {
        return "";
    }

    protected boolean isBackVisible() {
        return true;
    }

    public LuxTitleBar getTitleBar() {
        return titleBar;
    }

    @Override
    public void back() {
        onPopBack();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(EventBusMessage msg) {
        IEventBusMessage message = msg.getMessage();
        if (message instanceof ConnectState) {
            ConnectState connectState = (ConnectState) message;
            Log.i(TAG, "device connectStatus :" + connectState.getStatus());
            if (!connectState.getStatus()) {
                if (connectState.getAddress().equalsIgnoreCase(deviceAddress)) {
                    DialogUtil.showReconnectDialog(requireContext(), v1 -> {
                        startActivity(new Intent(requireContext(), HomeActivity.class));
                        back();
                    });
                }
            }
        }
    }

    /**
     * 退出当前的fragment
     */
    protected void onPopBack() {
        requireActivity().finish();
        int backStackEntryCount = fragmentManager.getBackStackEntryCount();
        Log.i(TAG, "onPopBack: count: " + backStackEntryCount);
        if (backStackEntryCount > 1 && getActivity() instanceof DetailActivity) {
            fragmentManager.popBackStack();
        } else {
            requireActivity().finish();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        FragmenTrack.getInstance().take(this);
        if (EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().unregister(this);
        }
    }
}
