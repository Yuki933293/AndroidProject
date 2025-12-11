package com.luxshare.base.fragment;

import android.os.Bundle;
import android.util.Log;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.alibaba.android.arouter.launcher.ARouter;
import com.luxshare.base.R;

import java.util.Queue;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * 用于Fragment切换跳转
 * <p>
 * Created by CaoYanYan
 * Date: 2023/3/9 13:17
 **/
public class FragmentHanler {
    private static final String TAG = "FragmentHanler";
    private static FragmentHanler instance;
    private FragmentManager supportFragmentManager;
    private FragmentActivity activity;




    private FragmentHanler() {
    }

    public void init(FragmentActivity activity) {
        this.activity = activity;
        supportFragmentManager = activity.getSupportFragmentManager();
    }

    public FragmentActivity getActivity() {
        return activity;
    }

    public FragmentManager getSupportFragmentManager() {
        return supportFragmentManager;
    }

    public static FragmentHanler of() {
        if (instance == null) {
            instance = new FragmentHanler();
        }
        return instance;
    }

    public void popBackStack() {
        int count = supportFragmentManager.getBackStackEntryCount();
        Log.i(TAG, "pop: count" + count);
        if (count > 0) {
            supportFragmentManager.popBackStack();
        }
    }

    public void replace(Fragment target, Bundle args, boolean isSupportBack) {
        if (args != null) {
            target.setArguments(args);
        }
        if (supportFragmentManager == null) {
            throw new NullPointerException("please init first");
        }
        FragmentTransaction transaction = supportFragmentManager.beginTransaction();
        transaction.setReorderingAllowed(true).replace(R.id.fragment_root, target);
        if (isSupportBack) {
            transaction.addToBackStack(target.getClass().getSimpleName());
        }
        transaction.commitAllowingStateLoss();
    }

    public void add(Fragment target, Bundle args, boolean isSupportBack) {
        if (args != null) {
            target.setArguments(args);
        }
        if (supportFragmentManager == null) {
            throw new NullPointerException("please init first");
        }
        FragmentTransaction transaction = supportFragmentManager.beginTransaction();
        transaction.setReorderingAllowed(true).add(R.id.fragment_root, target);
        if (isSupportBack) {
            transaction.addToBackStack(target.getClass().getSimpleName());
        }
        transaction.commit();
    }

    public void add(String path, Bundle args, boolean isSupportBack) {
        Fragment target = (Fragment) ARouter.getInstance().build(path).navigation();
        if (args != null) {
            target.setArguments(args);
        }
        if (supportFragmentManager == null) {
            throw new NullPointerException("please init first");
        }
        FragmentTransaction transaction = supportFragmentManager.beginTransaction();
        transaction.setReorderingAllowed(true).add(R.id.fragment_root, target);
        if (isSupportBack) {
            transaction.addToBackStack(target.getClass().getSimpleName());
        }
        transaction.commitAllowingStateLoss();
    }

    public void replace(String target, Bundle args, boolean isSupportBack) {
        Fragment navigation = (Fragment) ARouter.getInstance().build(target).navigation();
        replace(navigation, args, isSupportBack);
    }

    public void startRoot(Fragment target, Bundle args) {
        clearFragmentStack();
        replace(target, args, false);
    }

    public void clearFragmentStack() {
        clearFragmentStack(supportFragmentManager);
    }

    private void clearFragmentStack(FragmentManager fragmentManager) {
        int count = fragmentManager.getBackStackEntryCount();
        if (count > 0) {
            FragmentManager.BackStackEntry entry = fragmentManager.getBackStackEntryAt(0);
            fragmentManager.popBackStack(entry.getName(), FragmentManager.POP_BACK_STACK_INCLUSIVE);
        }
    }


}
