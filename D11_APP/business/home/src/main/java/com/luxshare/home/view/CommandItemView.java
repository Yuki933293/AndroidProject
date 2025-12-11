package com.luxshare.home.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.alibaba.android.arouter.utils.TextUtils;
import com.luxshare.base.R;
import com.luxshare.base.utils.DisplayUtil;
import com.luxshare.base.utils.MultiClickUtil;


/**
 * 自定义titlebar
 *
 * @author CaoYanyan
 */
@SuppressLint("UseSwitchCompatOrMaterialCode")
public class CommandItemView extends FrameLayout {
    private static final String TAG = "CommandItemView";
    private TextView titleTv;
    private TextView sPreTitle;
    /**
     * item类型
     */
    private ItemType itemType;
    private ItemBgType itemBgType;
    /**
     * 描述TextView
     */
    private TextView sTvdes;
    /**
     * 描述信息
     */
    private String describe;
    /**
     * 描述信息字体大小
     */
    private float describe_size;
    /**
     * 描述信息文本颜色
     */
    private int desColor;
    /**
     * 开关
     */
    private Switch itemSwitch;
    /**
     * 用于控制开关指令下发和返回UI刷新；
     */
    private boolean isSwitchCommandActiving = false;

    /**
     * 点击开关时的状态
     */
    private boolean switchState = false;
    private Handler sHandler = new Handler(Looper.getMainLooper());
    /**
     * 频繁点击限制时间
     */
    private int durationTime = 2 * 1000;
    /**
     * 等待刷新时间
     */
    private int delayTime = 3 * 1000;
    private View contentLayout;
    private View line;
    private int sItemBgType;

    private TextView tvTip;

    public CommandItemView(Context context) {
        this(context, null);
    }

    @SuppressLint("ResourceAsColor")
    public CommandItemView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.ItemView);
        String titleText = typedArray.getString(R.styleable.ItemView_itemTitle);
        String tipText = typedArray.getString(R.styleable.ItemView_tip);
        int TipVisible = typedArray.getInt(R.styleable.ItemView_tipVisible, View.GONE);
        String sItemType = typedArray.getString(R.styleable.ItemView_itemType);
        int itemBgType = typedArray.getInt(R.styleable.ItemView_item_bgType, 0);
        Log.i(TAG, "CommandItemView: sItemBgType:" + sItemBgType);
        describe = typedArray.getString(R.styleable.ItemView_describe);
        describe_size = typedArray.getDimension(R.styleable.ItemView_describe_size, -1);
        desColor = typedArray.getColor(R.styleable.ItemView_describe_color, R.color.black_item_tip);

        Log.i(TAG, "CommandItemView: sItemType:" + sItemType);
        itemType = ItemType.getItemType(sItemType);
        this.itemBgType = ItemBgType.getItemBgType(itemBgType);
        typedArray.recycle();
        LayoutInflater.from(context).inflate(R.layout.layout_command_view, this);
        contentLayout = findViewById(R.id.rlyout_content);
        line = findViewById(R.id.line);
        contentLayout.setBackgroundResource(getItemBgResourceId());
        titleTv = findViewById(R.id.title);
        tvTip = findViewById(R.id.tv_tip);
        FrameLayout layoutRight = findViewById(R.id.layout_right);
        setClickable(true);
        getChild(layoutRight);
        titleTv.setText(TextUtils.isEmpty(titleText) ? "" : titleText);
        if (View.VISIBLE == TipVisible) {
            tvTip.setVisibility(View.VISIBLE);
            tvTip.setText(TextUtils.isEmpty(tipText) ? "" : tipText);
        } else {
            tvTip.setVisibility(View.GONE);
        }
        initChildView();
        setClickable(true);
        MultiClickUtil.setOnMultiClick(this, new MultiClickUtil.OnMultiClickListener(getContext()) {
            @Override
            public void onMultiClick(View vew) {
                onItemClick(vew);
            }
        }, durationTime);
    }

    private int getItemBgResourceId() {
        switch (itemBgType) {
            case TYPE_TOP:
                return R.drawable.commandview_item_bg_top;
            case TYPE_BOTTOM:
                return R.drawable.commandview_item_bg_bottom;
            default:
                break;
        }
        return R.drawable.commandview_item_bg_middle;
    }

    private void initChildView() {
        //整体背景和间距
        switch (itemBgType) {
            case TYPE_TOP:
                this.setPadding(0, DisplayUtil.Companion.dip2px(getContext(), 10), 0, 0);
                break;
            case TYPE_BOTTOM:
                line.setVisibility(View.GONE);
                break;
            default:
                break;
        }
        //单个item的布局
        switch (itemType) {
            case TYPE_SWITCH:
                initSwitch();
                break;
            case TYPE_TEXT:
                initTextView();
                break;
            case TYPE_MULTY:
                initMultyView();
                break;
            case TYPE_MONO:
                break;
            default:
                break;
        }
    }

    public ItemType getItemType() {
        return itemType;
    }

    private Runnable switchRefreshTask = () -> {
        if (itemSwitch == null && !isSwitchCommandActiving) {
            return;
        }
        itemSwitch.setChecked(switchState);
        isSwitchCommandActiving = false;
    };

    public void refeshSwitchState(boolean resultState) {
        Log.i(TAG, "refeshSwitchState: result:" + resultState);
        switchState = resultState;
        if (itemSwitch.isChecked() != resultState) {
            itemSwitch.setChecked(switchState);
        }
        sHandler.removeCallbacks(switchRefreshTask);
        isSwitchCommandActiving = false;
    }

    private void initSwitch() {
        itemSwitch = findViewById(R.id.item_switch);
        switchState = itemSwitch.isChecked();
    }

    private void initTextView() {
        initMultyView();
    }

    private void initMultyView() {
        sTvdes = findViewById(R.id.text);
        if (describe_size > 0) {
            sTvdes.setTextSize(TypedValue.COMPLEX_UNIT_PX, describe_size);
        }
        sTvdes.setTextColor(desColor);
        if (!TextUtils.isEmpty(describe)) {
            sTvdes.setText(describe);
        }
    }

    private View getChild(ViewGroup parent) {
        return View.inflate(getContext(), getChildLayout(), parent);
    }

    private int getChildLayout() {
        switch (itemType) {
            case TYPE_SWITCH:
                return R.layout.itemview_switch;
            case TYPE_TEXT:
                return R.layout.itemview_text;
            case TYPE_MULTY:
                return R.layout.itemview_multy;
            case TYPE_MONO:
                return R.layout.itemview_mono;
        }
        return -1;
    }

    public TextView getTitleView() {
        return titleTv;
    }

    public TextView getsPreTitle() {
        return sPreTitle;
    }

    public void setsTvdes(String describe) {
        if (describe != null) {
            sTvdes.setText(describe);
        }
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

    private ClickCallback clickCallback;

    public void setClickCallback(ClickCallback clickCallback) {
        this.clickCallback = clickCallback;
    }

    /**
     * view被点击
     *
     * @param v
     */
    private void onItemClick(View v) {
        if (itemSwitch != null) {
            if (isSwitchCommandActiving) {
                return;
            }
            isSwitchCommandActiving = true;
            switchState = itemSwitch.isChecked();
            itemSwitch.setChecked(!switchState);
            sHandler.removeCallbacks(switchRefreshTask);
            if (clickCallback != null) {
                clickCallback.onSwitchStateChange(getTag(), itemSwitch.isChecked());
            }
            sHandler.postDelayed(switchRefreshTask, delayTime);
            return;
        }
        if (clickCallback != null) {
            clickCallback.onItemClick(this);
        }
    }

    public void setsDescribe(CharSequence describe) {
        if (this.sTvdes != null) {
            this.sTvdes.setText(describe);
        }
    }

    public void setTips(CharSequence describe) {
        if (this.tvTip != null) {
            this.tvTip.setText(describe);
        }
    }

    public String getDescribe() {
        if (this.sTvdes != null) {
            return sTvdes.getText().toString();
        }
        return "";
    }

    public static interface ClickCallback {
        void onItemClick(CommandItemView view);

        default void onSwitchStateChange(Object itemTag, boolean switchSate) {

        }
    }

    static enum ItemType {
        TYPE_SWITCH(0, "type_switch"),
        TYPE_TEXT(1, "type_text"),
        TYPE_MULTY(2, "type_multy"),
        TYPE_MONO(3, "type_mono");
        private String name;
        private int num;

        ItemType(int position, String name) {
            this.name = name;
            this.num = position;
        }

        public String getName() {
            return name;
        }

        public static ItemType getItemType(String name) {
            if (TextUtils.isEmpty(name)) {
                return TYPE_TEXT;
            }
            return ItemType.valueOf(name.toUpperCase());
        }
    }


    /**
     * 用于控制背景图
     */
    static enum ItemBgType {
        TYPE_TOP(1),
        TYPE_MIDDLE(0),
        TYPE_BOTTOM(-1);
        private int num;

        ItemBgType(int position) {
            this.num = position;
        }

        public static ItemBgType getItemBgType(int position) {
            if (position == 1) {
                return TYPE_TOP;
            } else if (position == -1) {
                return TYPE_BOTTOM;
            } else {
                return TYPE_MIDDLE;
            }
        }
    }
}
