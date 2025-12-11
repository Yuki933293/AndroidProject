package com.luxshare.ble.interact;


import android.os.Handler;
import android.os.Looper;
import android.os.Process;

public class HandlerThreadEx extends Thread {
    private int mPriority;
    private int mTid = -1;
    private Looper mLooper;
    protected Handler mHandler;
    protected Handler sMainHandler;

    public HandlerThreadEx(String name) {
        super(name);
        mPriority = Process.THREAD_PRIORITY_DEFAULT;
    }

    /**
     * Constructs a HandlerThread.
     *
     * @param name
     * @param priority The priority to run the thread at. The value supplied must be from
     *                 {@link android.os.Process} and not from java.lang.Thread.
     */
    public HandlerThreadEx(String name, int priority) {
        super(name);
        mPriority = priority;
    }

    /**
     * Call back method that can be explicitly overridden if needed to execute some
     * setup before Looper loops.
     */
    protected void onPrepared() {
    }

    @Override
    public void run() {
        mTid = Process.myTid();
        Looper.prepare();
        synchronized (this) {
            mLooper = Looper.myLooper();
            notifyAll();
        }
        Process.setThreadPriority(mPriority);
        getThreadHandler();
        initMainHandler();
        onPrepared();
        Looper.loop();
        mTid = -1;
    }

    public Looper getLooper() {
        if (!isAlive()) {
            return null;
        }
        boolean wasInterrupted = false;
        synchronized (this) {
            while (isAlive() && mLooper == null) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    wasInterrupted = true;
                }
            }
        }
        if (wasInterrupted) {
            Thread.currentThread().interrupt();
        }
        return mLooper;
    }

    private void initMainHandler() {
        if (sMainHandler ==null){
            sMainHandler = new Handler(Looper.getMainLooper());
        }
    }

    public Handler getThreadHandler() {
        if (mHandler == null) {
            mHandler = new Handler(getLooper());
        }
        return mHandler;
    }

    public boolean quit() {
        Looper looper = getLooper();
        if (looper != null) {
            looper.quit();
            return true;
        }
        return false;
    }

    public boolean quitSafely() {
        Looper looper = getLooper();
        if (looper != null) {
            looper.quitSafely();
            return true;
        }
        return false;
    }

    /**
     * Returns the identifier of this thread. See Process.myTid().
     */
    public int getThreadId() {
        return mTid;
    }
}
