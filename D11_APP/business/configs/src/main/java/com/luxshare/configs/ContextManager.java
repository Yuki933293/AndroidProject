package com.luxshare.configs;

import android.content.Context;

/**
 * Created by CaoYanYan
 * Date: 2024/3/12 16:01
 **/
public class ContextManager {
    private ContextManager() {
    }

     static interface Holder {
        public static ContextManager mInstance = new ContextManager();
    }

    public static ContextManager getInstance() {
        return Holder.mInstance;
    }

    private Context context;

    public Context getContext() {
        return context;
    }

    public void setContext(Context context) {
        this.context = context;
    }
}
