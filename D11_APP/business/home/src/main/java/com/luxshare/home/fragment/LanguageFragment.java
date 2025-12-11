package com.luxshare.home.fragment;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.LinearLayout;

import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.alibaba.android.arouter.facade.annotation.Route;
import com.luxshare.base.utils.MultiLanguageUtils;
import com.luxshare.ble.eventbus.EventBusMessage;
import com.luxshare.configs.Configs;
import com.luxshare.configs.PathConfig;
import com.luxshare.fastsp.FastSharedPreferences;
import com.luxshare.home.R;
import com.luxshare.home.adapter.SingleChoiceListAdapter;
import com.luxshare.home.bean.SingleChoiceItem;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.List;
import java.util.Objects;

@Route(path = PathConfig.Path_LanguageFragment)
public class LanguageFragment extends BaseSpeakerFragment implements SingleChoiceListAdapter.OnItemClickListener {
    private RecyclerView list;
    private SingleChoiceListAdapter adapter;
    private String selectedTitle;

    @Override
    protected int getChildLayoutId() {
        return R.layout.fragment_language;
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
//        Bundle arguments = getArguments();
//        if (arguments != null) {
//            selectedTitle = arguments.getString(Configs.KEY_TITLE_SELECTED, "");
//        }

//        String language = FastSharedPreferences.get(MultiLanguageUtils.LANGUAGE).getString(
//                MultiLanguageUtils.LANGUAGE,
//                MultiLanguageUtils.FOLLOW_SYSTEM
//        );
//        if (language.isEmpty() || language.equals(MultiLanguageUtils.CHINESE)) {
//            selectedTitle = new Configs().getLanguageMap(requireContext()).get(1);
//        } else  {
//            selectedTitle = new Configs().getLanguageMap(requireContext()).get(2);
//        }

        int savedLanguageTag = FastSharedPreferences.get(MultiLanguageUtils.LANGUAGE).getInt(
                MultiLanguageUtils.LANGUAGE_TAG,
                Configs.OPERATE_LANGUAGE_CN
        );
        if (savedLanguageTag == Configs.OPERATE_LANGUAGE_EN) {
            selectedTitle = getString(R.string.english);
        } else {
            selectedTitle = getString(R.string.chinese);
        }

        adapter = new SingleChoiceListAdapter();
        adapter.setOnItemListener(this);
        List<SingleChoiceItem> datas = adapter.getDatas();
        String[] names = getResources().getStringArray(R.array.device_language);
        for (String name : names) {
            SingleChoiceItem singleChoiceItem = new SingleChoiceItem();
            singleChoiceItem.setName(name);
            singleChoiceItem.setSelected(selectedTitle.equals(name));
            datas.add(singleChoiceItem);
        }
        list.setAdapter(adapter);
    }

    @Override
    protected CharSequence getBarTitle() {
        return getContext().getString(R.string.voice_reminder_language);
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
        bundle.putString(Configs.KEY_LANGUAGE, singleChoiceItem.getName());
        EventBus.getDefault().post(bundle);
        back();
    }
}
