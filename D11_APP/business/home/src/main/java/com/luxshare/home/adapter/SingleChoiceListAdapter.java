package com.luxshare.home.adapter;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.luxshare.home.R;
import com.luxshare.home.bean.SingleChoiceItem;

import java.util.ArrayList;
import java.util.List;

public class SingleChoiceListAdapter extends RecyclerView.Adapter {
    private static final String TAG = "SingleChoiceListAdapter";
    private List<SingleChoiceItem> items = new ArrayList<>();
    private int selectedPosition = -1;

    private SingleChoiceListAdapter.OnItemClickListener mListener;


    public SingleChoiceListAdapter() {
    }

    public List<SingleChoiceItem> getDatas() {
        return items;
    }

    public void setDatas(List<SingleChoiceItem> items) {
        if (items == null) {
            return;
        }
        Log.i(TAG, "data size:" + items.size());
        this.items = items;
        notifyDataSetChanged();
    }


    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_single_choice_list, parent, false);
        return new SingleChoiceListAdapter.ViewHolder(view);
    }


    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        SingleChoiceItem item = items.get(position);
        SingleChoiceListAdapter.ViewHolder itemView = (SingleChoiceListAdapter.ViewHolder) holder;
        Log.i(TAG, "onBindViewHolder: name:" + item.getName() + ",state:" + item.isSelected() + ",position:" + position);
        itemView.tvName.setText(item.getName());
        itemView.selectIcon.setVisibility(item.isSelected() ? View.VISIBLE : View.INVISIBLE);
        if (item.isSelected()) {
            selectedPosition = position;
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public int getSelectedPosition() {
        return selectedPosition;
    }

    class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        TextView tvName;
        ImageView selectIcon;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            itemView.setClickable(true);
            itemView.setOnClickListener(this);
            tvName = itemView.findViewById(R.id.name);
            selectIcon = itemView.findViewById(R.id.select_state);
        }

        @Override
        public void onClick(View v) {
            int layoutPosition = getLayoutPosition();
            setSelectedPosition(layoutPosition);
            if (mListener != null) {
                mListener.onItemClick(SingleChoiceListAdapter.this, v, layoutPosition);
            }
        }
    }

    public void setSelectedPosition(int position) {
        Log.i(TAG, "setSelectedPosition: position:" + position + ",selectedPosition:" + selectedPosition);
        if (position == selectedPosition) {
            return;
        }
        if (selectedPosition != -1) {
            items.get(selectedPosition).setSelected(false);
        }
        if (position != -1) {
            items.get(position).setSelected(true);
        }
        selectedPosition = position;
        for (int i = 0; i < items.size(); i++) {
            SingleChoiceItem item = items.get(i);
            Log.i(TAG, "setSelectedPosition: name:" + item.getName() + ",state:" + item.isSelected());
        }
        notifyDataSetChanged();

    }

    public void setOnItemListener(SingleChoiceListAdapter.OnItemClickListener listener) {
        this.mListener = listener;
    }

    public interface OnItemClickListener<T> {
        void onItemClick(SingleChoiceListAdapter adapter, View view, int position);
    }
}
