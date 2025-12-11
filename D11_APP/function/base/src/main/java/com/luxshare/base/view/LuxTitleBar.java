package com.luxshare.base.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.luxshare.base.R;


/**
 * 自定义titlebar
 *
 * @author CaoYanyan
 */
public class LuxTitleBar extends FrameLayout {

    private TextView titleTv, sPreTitle;
    private ImageButton btnBack;
    private ImageButton btnOperate;

    public LuxTitleBar(Context context) {
        this(context, null);
    }

    public LuxTitleBar(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.LuxTitleBar);
        String title = typedArray.getString(R.styleable.LuxTitleBar_title);
        String preTitle = typedArray.getString(R.styleable.LuxTitleBar_preTitle);
        Drawable drawable = typedArray.getDrawable(R.styleable.LuxTitleBar_rightKey);

        typedArray.recycle();
        LayoutInflater.from(context).inflate(R.layout.layout_titlebar, this);
        btnBack = findViewById(R.id.back);
        btnBack.setOnClickListener(v -> {
            if (backCallback != null) {
                backCallback.back();
            }
        });
        titleTv = findViewById(R.id.title);
        sPreTitle = findViewById(R.id.pre_title);
        titleTv.setText(title);
        sPreTitle.setText(preTitle);
        btnOperate = findViewById(R.id.btn_operate);
        if (drawable instanceof ColorDrawable) {
            ColorDrawable colorDrawable = (ColorDrawable) drawable;
            int color = colorDrawable.getColor();
            btnOperate.setBackgroundColor(color);
        } else if (drawable instanceof BitmapDrawable) {
            btnOperate.setBackground(drawable);
        }
    }

    public TextView getTitleView() {
        return titleTv;
    }

    public TextView getsPreTitle() {
        return sPreTitle;
    }

    public void setsPreTitle(TextView sPreTitle) {
        this.sPreTitle = sPreTitle;
    }

    public String getTitle() {
        return titleTv.getText().toString();
    }

    public void setTitle(CharSequence title) {
        this.titleTv.setText(title);
    }

    public ImageButton getBtnBack() {
        return btnBack;
    }

    public void setBackVisible(boolean isVisible) {
        btnBack.setVisibility(isVisible ? VISIBLE : GONE);
    }

    public ImageButton getBtnOperate() {
        return btnOperate;
    }

    private BackCallback backCallback;

    public void setClickCallback(BackCallback backCallback) {
        this.backCallback = backCallback;
    }

    public static interface BackCallback {
        void back();
    }
}
