package com.luxshare.base;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Build;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import androidx.annotation.IntDef;
import androidx.annotation.RequiresApi;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.ref.WeakReference;

/**
 * 沉浸式状态栏工具类
 *
 * @author Luxshare
 * @version version
 */
public final class SystemBar {

    private static Activity mActivity;
    //状态栏颜色，默认透明色
    private static int mStatusBarColor = Color.TRANSPARENT;
    //底部虚拟键盘颜色，默认透明色
    private static int mNavigationBarColor = Color.TRANSPARENT;

    /**
     * 不做任何动作
     */
    public static final int NORMAL = 1 << 0;

    /**
     * 修改状态栏颜色
     */
    public static final int STATUS_BAR_COLOR = 1 << 1;

    /**
     * 修改底部虚拟键盘颜色
     */
    public static final int NAVIGATION_BAR_COLOR = 1 << 2;

    /**
     * 去除状态栏
     */
    public static final int REMOVE_STATUS_BAR = 1 << 3;

    /**
     * 去除底部虚拟键盘
     */
    public static final int REMOVE_NAVIGATION_BAR = 1 << 4;

    /**
     * 状态栏小图标置灰
     */
    public static final int DARK_STATUS_ICON = 1 << 5;

    //策略
    @Model private static int mStrategy;

    private SystemBar() {
        mStrategy = NORMAL;
    }

    static class SystemBarHolder {
        private static SystemBar instance = new SystemBar();

        public static SystemBar getInstance() {
            return instance;
        }

    }

    /**
     * flag=true时，参数可以“|”传递多个参数，flag=false时，只能传递一个参数
     */
    @IntDef(flag = true, value = {NORMAL, STATUS_BAR_COLOR, NAVIGATION_BAR_COLOR, REMOVE_STATUS_BAR, REMOVE_NAVIGATION_BAR, DARK_STATUS_ICON})
    // 注解作用域参数、方法、成员变量
    @Target({ElementType.PARAMETER, ElementType.METHOD, ElementType.FIELD})
    // 仅仅在源码阶段有效
    @Retention(RetentionPolicy.SOURCE)
    public @interface Model {

    }

    /**
     * 单例
     *
     * @param activity activity
     * @return with
     */
    public static SystemBar with(Activity activity) {
        mActivity = (Activity) new WeakReference(activity).get();
        return SystemBarHolder.instance;
    }

    /**
     * 设置状态栏颜色（不设置就是透明色）
     *
     * @param statusBarColor statusBarColor
     * @return setStatusBarColor
     */
    public SystemBar setStatusBarColor(int statusBarColor) {
        mStatusBarColor = statusBarColor;
        return SystemBarHolder.instance;
    }

    /**
     * 设置状态栏颜色（不设置就是透明色）
     *
     * @param navigationBarColor navigationBarColor
     * @return setNavigationBarColor
     */
    public SystemBar setNavigationBarColor(int navigationBarColor) {
        mNavigationBarColor = navigationBarColor;
        return SystemBarHolder.instance;
    }

    /**
     * 设置策略
     *
     * @param strategy strategy
     * @return setStrategy
     */
    public SystemBar setStrategy(@Model int strategy) {
        mStrategy = strategy;
        return SystemBarHolder.instance;
    }

    /**
     * 添加策略
     *
     * @param strategy strategy
     * @return addStrategy
     */
    public SystemBar addStrategy(@Model int strategy) {
        mStrategy |= strategy;
        return SystemBarHolder.instance;
    }

    /**
     * 移除策略
     *
     * @param strategy strategy
     * @return removeStrategy
     */
    public SystemBar removeStrategy(@Model int strategy) {
        mStrategy &= ~strategy;
        return SystemBarHolder.instance;
    }

    /**
     * 判断是否含有某种策略
     *
     * @param strategy strategy
     * @return 返回判断是否含有某种策略
     */
    private static boolean isContainStrategy(@Model int strategy) {
        return (mStrategy & strategy) > 0;
    }

    /**
     * build
     *
     * @return SystemBar
     * @throws RuntimeException RuntimeException
     */
    public SystemBar build() {

        if (mActivity == null) {
            throw new RuntimeException("please set a activity content!");
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            buildA();
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            buildB();
        }
        return SystemBarHolder.instance;
    }

    private void buildA() {
        //去除灰色背影
        mActivity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        //移除隐藏状态栏的Flag
        mActivity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        //移除隐藏底部虚拟键盘的Flag
        mActivity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);

        if (isContainStrategy(STATUS_BAR_COLOR)) {
            //设置状态栏颜色
            mActivity.getWindow().setStatusBarColor(mStatusBarColor);
        }

        if (isContainStrategy(NAVIGATION_BAR_COLOR)) {
            //设置虚拟键盘颜色
            mActivity.getWindow().setNavigationBarColor(mNavigationBarColor);
        }

        //View.SYSTEM_UI_FLAG_LOW_PROFILE：弱化状态栏和导航栏的图标
        //View.SYSTEM_UI_FLAG_FULLSCREEN：全屏，隐藏状态栏，需要从状态栏位置下拉才会出现
        //View.SYSTEM_UI_FLAG_HIDE_NAVIGATION：隐藏导航栏，用户点击屏幕会显示导航栏
        //View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION：拓展布局到导航栏后面
        //View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN：拓展布局到状态栏后面
        //View.SYSTEM_UI_FLAG_LAYOUT_STABLE：稳定的布局，不会随系统栏的隐藏、显示而变化
        //View.SYSTEM_UI_FLAG_IMMERSIVE：沉浸模式，用户可以交互的界面
        //View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY：沉浸模式，用户可以交互的界面。同时，用户上下拉系统栏时，会自动隐藏系统栏
        int systemUiFlag = View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
        if (isContainStrategy(REMOVE_STATUS_BAR)) {
            systemUiFlag |= View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
            //设置状态栏颜色
            mActivity.getWindow().setStatusBarColor(Color.TRANSPARENT);
        }
        if (isContainStrategy(REMOVE_NAVIGATION_BAR)) {
            systemUiFlag |= View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;
            //设置虚拟键盘颜色
            mActivity.getWindow().setNavigationBarColor(Color.TRANSPARENT);
        }
        // Android 6.0的手机支持将状态栏的小图标设置为黑色
        if (isContainStrategy(DARK_STATUS_ICON)) {
            systemUiFlag |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
        }
        mActivity.getWindow().getDecorView().setSystemUiVisibility(systemUiFlag);
    }

    private void buildB() {
        //Android 4.4适配
        if (isContainStrategy(REMOVE_STATUS_BAR)) {
            //移除状态栏
            mActivity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        }
        if (isContainStrategy(REMOVE_NAVIGATION_BAR)) {
            //移除底部虚拟键盘
            mActivity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
        }
    }

    /**
     * 处理顶部view和状态栏重合问题
     *
     * @param topView 返回处理顶部view和状态栏重合问题
     */
    public void dealwithTop(View topView) {
        if (topView != null) {
            int statusHeight = getStatusHeight();
            //这里LayoutParams是ViewGroup类型，具体是什么类型按需求而定
            ViewGroup.LayoutParams layoutParams = topView.getLayoutParams();
            // view的高度， -2：wrap_content   -1：match_parent  具体数值（大于0的数值）
            int viewHeight = layoutParams.height;
            // 只需要考虑具体数值，wrap_content和match_parent的请路不需要考虑
            if (viewHeight > 0) {
                layoutParams.height += statusHeight;
                topView.setLayoutParams(layoutParams);
            }
            topView.setPadding(0, topView.getPaddingTop() + statusHeight, 0, 0);
        }
    }

    /**
     * 处理底部view和状态栏重合问题
     *
     * @param bottomView 返回处理底部view和状态栏重合问题
     */
    public void dealwithBottom(View bottomView) {
        if (bottomView != null && haveNavgtion()) {
            int navigationHeight = getNavigationHeight();
            ViewGroup.LayoutParams layoutParams = bottomView.getLayoutParams();
            // view的高度， -2：wrap_content   -1：match_parent  具体数值（大于0的数值）
            int viewHeight = layoutParams.height;
            // 只需要考虑具体数值，wrap_content和match_parent的请路不需要考虑
            if (viewHeight > 0) {
                layoutParams.height += navigationHeight;
                bottomView.setLayoutParams(layoutParams);
            }
            bottomView.setPadding(0, 0, 0, bottomView.getPaddingBottom() + navigationHeight);
        }
    }


    /**
     * 获取底部虚拟键盘的高度
     *
     * @return 返回底部虚拟键盘的高度
     */
    private int getNavigationHeight() {
        int height = -1;
        try {
            Class<?> clazz = Class.forName("com.android.internal.R$dimen");
            Object object = clazz.newInstance();
            String heightStr = clazz.getField("navigation_bar_height").get(object).toString();
            height = Integer.parseInt(heightStr);
            height = mActivity.getResources().getDimensionPixelSize(height);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
        return height;
    }

    /**
     * 底部是否含有虚拟键盘
     *
     * @return 返回底部是否含有虚拟键盘
     */
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
    private boolean haveNavgtion() {
        //屏幕的高度  真实物理的屏幕
        Display display = mActivity.getWindowManager().getDefaultDisplay();
        DisplayMetrics displayMetrics = new DisplayMetrics();
        display.getRealMetrics(displayMetrics);
        int heightDisplay = displayMetrics.heightPixels;
        //为了防止横屏
        int widthDisplay = displayMetrics.widthPixels;
        DisplayMetrics contentDisplaymetrics = new DisplayMetrics();
        display.getMetrics(contentDisplaymetrics);
        int contentDisplay = contentDisplaymetrics.heightPixels;
        int contentDisplayWidth = contentDisplaymetrics.widthPixels;
        //屏幕内容高度  显示内容的屏幕
        int w = widthDisplay - contentDisplayWidth;
        //哪一方大于0   就有导航栏
        int h = heightDisplay - contentDisplay;
        return w > 0 || h > 0;
    }

    /**
     * 获取状态栏高度
     *
     * @return 返回状态栏高度
     */
    private int getStatusHeight() {
        int height = -1;
        try {
            Class<?> clazz = Class.forName("com.android.internal.R$dimen");
            Object object = clazz.newInstance();
            String heightStr = clazz.getField("status_bar_height").get(object).toString();
            height = Integer.parseInt(heightStr);
            //dp--px
            height = mActivity.getResources().getDimensionPixelSize(height);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            Rect frame = new Rect();
            mActivity.getWindow().getDecorView().getWindowVisibleDisplayFrame(frame);
            height = frame.top;
            if (height == 0) {
                //获取status_bar_height资源的ID
                int resourceId = mActivity.getResources().getIdentifier("status_bar_height", "dimen", "android");
                if (resourceId > 0) {
                    //根据资源ID获取响应的尺寸值
                    height = mActivity.getResources().getDimensionPixelSize(resourceId);
                }
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
        return height;
    }
}
