package com.luxshare.base.fragment;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

/**
 * Created by CaoYanYan
 * Date: 2024/3/12 15:02
 **/
public abstract class BaseFragment extends Fragment {
    private final String BASE_TAG = "BaseFragment";
    protected final String TAG = this.getClass().getSimpleName();

    private boolean isViewCreated = false;
    private View view;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        Log.i(BASE_TAG, "onAttach: " + TAG);

    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(BASE_TAG, "onCreate: " + TAG);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.i(BASE_TAG, "onCreateView: " + TAG);
        view = inflater.inflate(getLayoutResourceId(), container, false);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        if (isViewCreated) {
            return;
        }
        initView(view);
        initData();
        isViewCreated = true;
    }


    protected abstract int getLayoutResourceId();

    protected void initView(View view) {

    }

    protected void initData() {

    }

    private boolean isOnceLoaded = false;

    @Override
    public void onResume() {
        super.onResume();
        Log.d(BASE_TAG, "onResume: " + TAG);
        if (!isOnceLoaded && !isHidden()) {
            onceLoaded();
            isOnceLoaded = true;
        }
    }

    protected void onceLoaded() {
    }


    @Override
    public void onStop() {
        super.onStop();
        Log.d(BASE_TAG, "onStop: " + TAG);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.d(BASE_TAG, "onDestroyView: " + TAG);
        isViewCreated = false;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        Log.d(BASE_TAG, "onDetach: " + TAG);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        isOnceLoaded = false;
        Log.d(BASE_TAG, "onDestroy: " + TAG);
    }
}
