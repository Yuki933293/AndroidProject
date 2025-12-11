package com.luxshare.home.fragment;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.LinearLayout;

import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.alibaba.android.arouter.facade.annotation.Route;
import com.luxshare.ble.eventbus.EventBusMessage;
import com.luxshare.configs.Configs;
import com.luxshare.configs.PathConfig;
import com.luxshare.home.R;
import com.luxshare.home.adapter.SingleChoiceListAdapter;
import com.luxshare.home.bean.SingleChoiceItem;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.List;
import java.util.Objects;
import java.util.Set;

@Route(path = PathConfig.Path_OffTimeFragment)
public class OffTimeFragment extends BaseSpeakerFragment implements SingleChoiceListAdapter.OnItemClickListener {
    private RecyclerView list;
    private SingleChoiceListAdapter adapter;
    private String selectedNicky;

    @Override
    protected int getChildLayoutId() {
        return R.layout.fragment_offtime;
    }

    @Override
    protected void initChildView(View view) {
        list = view.findViewById(R.id.list);
        DividerItemDecoration itemDecoration = new DividerItemDecoration(getContext(), LinearLayout.VERTICAL);
        itemDecoration.setDrawable(Objects.requireNonNull(ContextCompat.getDrawable(getContext(), R.drawable.devideline)));
        list.addItemDecoration(itemDecoration);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext(), RecyclerView.VERTICAL, false);
        list.setLayoutManager(layoutManager);

    }

    @Override
    protected void initData() {
        super.initData();
        Bundle arguments = getArguments();
        if (arguments != null) {
            selectedNicky = arguments.getString(Configs.KEY_TITLE_SELECTED, Configs.KEY_TIME_TEN);
        }
        adapter = new SingleChoiceListAdapter();
        adapter.setOnItemListener(this);
        List<SingleChoiceItem> datas = adapter.getDatas();
        String[] keyArray = getResources().getStringArray(R.array.off_time_key);
        String[] ValueArray = getResources().getStringArray(R.array.off_time_value);
        for (int i = 0; i < ValueArray.length; i++) {
            SingleChoiceItem singleChoiceItem = new SingleChoiceItem();
            singleChoiceItem.setName(ValueArray[i]);
            singleChoiceItem.setNicky(keyArray[i]);
            singleChoiceItem.setSelected(selectedNicky.equals(keyArray[i]));
            datas.add(singleChoiceItem);
        }
        list.setAdapter(adapter);
    }

    @Override
    protected CharSequence getBarTitle() {
        return getContext().getString(R.string.automatic_shutdown);
    }

    @Override
    protected boolean isBackVisible() {
        return true;
    }
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void getMessage(EventBusMessage message) {

    }

    @Override
    public void onItemClick(SingleChoiceListAdapter adapter, View view, int position) {
        SingleChoiceItem singleChoiceItem = adapter.getDatas().get(position);
        Bundle bundle = new Bundle();
        bundle.putString(Configs.KEY_TIME, singleChoiceItem.getNicky());
        bundle.putString(Configs.KEY_TIME_NAME, singleChoiceItem.getName());
        EventBus.getDefault().post(bundle);
    }
}
