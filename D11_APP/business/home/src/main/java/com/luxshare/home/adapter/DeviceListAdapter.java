package com.luxshare.home.adapter;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.luxshare.ble.DeviceState;
import com.luxshare.ble.bean.DeviceInfo;
import com.luxshare.home.R;

import java.util.ArrayList;
import java.util.List;

public class DeviceListAdapter extends RecyclerView.Adapter {
    private static final String TAG = "DeviceListAdapter";
    private List<DeviceInfo> deviceInfos;
    private OnItemClickListener mListener;


    public DeviceListAdapter() {
        this.deviceInfos = new ArrayList<>();
    }

    public List<DeviceInfo> getDatas() {
        return deviceInfos;
    }

    public void setDatas(List<DeviceInfo> items) {
        if (items == null) {
            return;
        }
        Log.i(TAG, "data size:" + items.size());
        this.deviceInfos = items;
        notifyDataSetChanged();
    }


    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_device_list, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        DeviceInfo item = deviceInfos.get(position);
        ViewHolder itemView = (ViewHolder) holder;
        itemView.title.setText(item.getName());
        itemView.address.setText(item.getAddress());
        if (item.getStatus() == DeviceState.STATE_CONNECTING) {
            itemView.deviceStatus.setVisibility(View.GONE);
            itemView.ivConnecting.setVisibility(View.VISIBLE);
        } else {
            itemView.ivConnecting.setVisibility(View.GONE);
            itemView.deviceStatus.setVisibility(View.VISIBLE);
            itemView.deviceStatus.setText(item.getDescription());
        }
    }

    public List<DeviceInfo> getDeviceInfos() {
        return deviceInfos;
    }


    @Override
    public int getItemCount() {
        return deviceInfos.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener {
        TextView title;
        TextView address;
        ImageView ivConnecting;
        TextView deviceStatus;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            itemView.setClickable(true);
            itemView.setOnClickListener(this);
            itemView.setOnLongClickListener(this);
            title = itemView.findViewById(R.id.device_title);
            address = itemView.findViewById(R.id.device_address);
            ivConnecting = itemView.findViewById(R.id.iv_connecting);
            deviceStatus = itemView.findViewById(R.id.device_status);
        }

        @Override
        public void onClick(View v) {
            if (mListener != null) {
                mListener.onItemClick(DeviceListAdapter.this, v, getLayoutPosition());
            }
        }

        @Override
        public boolean onLongClick(View v) {
            if (mListener != null) {
                mListener.onItemLongClick(DeviceListAdapter.this, v, getLayoutPosition());
            }
            return true;
        }
    }

    public void setOnItemListener(OnItemClickListener listener) {
        this.mListener = listener;
    }

    public interface  OnItemClickListener<T> {
        void onItemClick(DeviceListAdapter adapter, View view, int position);
        void onItemLongClick(DeviceListAdapter adapter, View view, int position);
    }

}
