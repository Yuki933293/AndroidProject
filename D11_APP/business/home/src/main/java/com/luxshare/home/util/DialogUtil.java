package com.luxshare.home.util;

import android.app.Dialog;
import android.content.Context;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.alibaba.android.arouter.utils.TextUtils;
import com.luxshare.home.R;

/**
 * 弹窗工具类
 */
public class DialogUtil {
    /**
     * 蓝牙设备连接失败
     *
     * @param context
     * @param onClickListener
     */
    public static void showConnectFailDialog(Context context, View.OnClickListener onClickListener) {
        Dialog dialog = new Dialog(context, R.style.custom_dialog);
        dialog.setContentView(R.layout.dialog_connect_fail);
        Button confirm = dialog.findViewById(R.id.btn_confirm);
        confirm.setOnClickListener(v -> {
            dialog.dismiss();
            if (onClickListener != null) {
                onClickListener.onClick(v);
            }
        });
        dialog.show();
    }

    /**
     * 恢复出厂设置提示弹窗
     *
     * @param context
     * @param onClickListener
     */
    public static void showFactoryResetDialog(Context context, View.OnClickListener onClickListener) {
        Dialog dialog = new Dialog(context, R.style.custom_dialog);
        dialog.setContentView(R.layout.dialog_factory_reset_tip);
        Button confirm = dialog.findViewById(R.id.btn_confirm);
        Button cancel = dialog.findViewById(R.id.btn_cancel);
        cancel.setOnClickListener(v -> dialog.dismiss());
        confirm.setOnClickListener(v -> {
            dialog.dismiss();
            onClickListener.onClick(v);
        });
        dialog.setCancelable(false);
        dialog.show();
    }

    static Dialog reconnecDialog;
    /**
     * 断开重连提示
     *
     * @param context
     * @param onClickListener
     */
    public static void showReconnectDialog(Context context, View.OnClickListener onClickListener) {
        if (reconnecDialog != null) {
            if (reconnecDialog.isShowing()) {
                reconnecDialog.dismiss();
            }
        }
        reconnecDialog = new Dialog(context, R.style.custom_dialog);
        reconnecDialog.setContentView(R.layout.dialog_reconnect_tip);
        Button confirm = reconnecDialog.findViewById(R.id.btn_confirm);
        Button cancel = reconnecDialog.findViewById(R.id.btn_cancel);
        cancel.setOnClickListener(v -> reconnecDialog.dismiss());
        confirm.setOnClickListener(v -> {
            reconnecDialog.dismiss();
            onClickListener.onClick(v);
        });
        reconnecDialog.setCancelable(false);
        reconnecDialog.show();
    }

    public static void showConfigNetDialog(Context context, String content, WifiConfigCallback wifiConfigCallback) {
        Dialog dialog = new Dialog(context, R.style.custom_dialog);
        dialog.setContentView(R.layout.dialog_config_net);
        TextView tvTitle = dialog.findViewById(R.id.tv_tip);
        if (!TextUtils.isEmpty(content)) {
            tvTitle.setText(content);
        }
        Button confirm = dialog.findViewById(R.id.btn_confirm);
        Button cancel = dialog.findViewById(R.id.btn_cancel);
        EditText etWifiName = dialog.findViewById(R.id.et_wifi_name);
        EditText etWifiPwd = dialog.findViewById(R.id.et_wifi_pwd);
        cancel.setOnClickListener(v -> dialog.dismiss());
        confirm.setOnClickListener(v -> {
            String wifiName = etWifiName.getText().toString();
            String wifiPwd = etWifiPwd.getText().toString();
            if (TextUtils.isEmpty(wifiName) || TextUtils.isEmpty(wifiPwd)) {
                Toast.makeText(context, context.getString(R.string.connect_net_tip), Toast.LENGTH_SHORT).show();
                return;
            }
            wifiConfigCallback.getWIFIConfig(wifiName, wifiPwd);
            dialog.dismiss();
        });
        dialog.setCancelable(false);
        dialog.show();
    }

    /**
     * 清空配对重连提示弹窗
     *
     * @param context
     * @param onClickListener
     */
    public static void showTipDialog(Context context, String content,View.OnClickListener onClickListener) {
        Dialog dialog = new Dialog(context, R.style.custom_dialog);
        dialog.setContentView(R.layout.dialog_clear_pair_tip);
        TextView tvTitle = dialog.findViewById(R.id.tv_tip);
        if (!TextUtils.isEmpty(content)){
            tvTitle.setText(content);
        }
        Button confirm = dialog.findViewById(R.id.btn_confirm);
        Button cancel = dialog.findViewById(R.id.btn_cancel);
        cancel.setOnClickListener(v -> dialog.dismiss());
        confirm.setOnClickListener(v -> {
            dialog.dismiss();
            onClickListener.onClick(v);
        });
        dialog.setCancelable(false);
        dialog.show();
    }

    /**
     * 断开重连提示
     *
     * @param context
     * @param onClickListener
     */
    public static void showDeleteDialog(Context context, View.OnClickListener onClickListener) {
        Dialog dialog = new Dialog(context, R.style.custom_dialog);
        dialog.setContentView(R.layout.dialog_delete_tip);
        Button confirm = dialog.findViewById(R.id.btn_confirm);
        Button cancel = dialog.findViewById(R.id.btn_cancel);
        cancel.setOnClickListener(v -> dialog.dismiss());
        confirm.setOnClickListener(v -> {
            dialog.dismiss();
            onClickListener.onClick(v);
        });
        dialog.setCancelable(false);
        dialog.show();
    }

     public static interface WifiConfigCallback{
         void getWIFIConfig(String wifiName,String pwd);
    }
}
