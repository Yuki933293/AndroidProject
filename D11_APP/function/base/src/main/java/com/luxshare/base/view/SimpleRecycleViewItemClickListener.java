package com.luxshare.base.view;

import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import androidx.core.view.GestureDetectorCompat;
import androidx.recyclerview.widget.RecyclerView;

/**
 * RecyclerView的Item点击事件监听
 */
public class SimpleRecycleViewItemClickListener extends RecyclerView.SimpleOnItemTouchListener {

    private OnItemClickListener mListener;
    private GestureDetectorCompat mGestureDetector;

    public SimpleRecycleViewItemClickListener(OnItemClickListener listener) {
        this.mListener = listener;
    }

    @Override
    public boolean onInterceptTouchEvent(RecyclerView rv, MotionEvent e) {
        if (mGestureDetector == null) {
            initGestureDetector(rv);
        }
        if (mGestureDetector.onTouchEvent(e)) { // 把事件交给GestureDetector处理
            return true;
        } else {
            return false;
        }
    }

    /**
     * 初始化GestureDetector
     */
    private void initGestureDetector(final RecyclerView recyclerView) {
        mGestureDetector = new GestureDetectorCompat(recyclerView.getContext(), new GestureDetector.SimpleOnGestureListener() { // 这里选择SimpleOnGestureListener实现类，可以根据需要选择重写的方法

            /**
             * 单击事件
             */
            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                View childView = recyclerView.findChildViewUnder(e.getX(), e.getY());
                if (childView != null && mListener != null) {
                    mListener.onItemClick(childView, recyclerView.getChildLayoutPosition(childView));
                    return true;
                }
                return false;
            }

            /**
             * 长按事件
             */
            @Override
            public void onLongPress(MotionEvent e) {
                View childView = recyclerView.findChildViewUnder(e.getX(), e.getY());
                if (childView != null && mListener != null) {
                    mListener.onItemLongClick(childView, recyclerView.getChildLayoutPosition(childView));
                }
            }

            /**
             * 双击事件
             */
            @Override
            public boolean onDoubleTapEvent(MotionEvent e) {
                int action = e.getAction();
                if (action == MotionEvent.ACTION_UP) {
                    View childView = recyclerView.findChildViewUnder(e.getX(), e.getY());
                    if (childView != null && mListener != null) {
                        mListener.onItemDoubleClick(childView, recyclerView.getChildLayoutPosition(childView));
                        return true;
                    }
                }
                return false;
            }

        });

    }

    /**
     * RecyclerView的Item点击事件监听接口
     *
     * @author liyunlong
     * @date 2016/11/21 9:43
     */
    public interface OnItemClickListener {

        /**
         * 当ItemView的单击事件触发时调用
         */
        void onItemClick(View view, int position);

        /**
         * 当ItemView的长按事件触发时调用
         */
        default void onItemLongClick(View view, int position) {
        }

        /**
         * 当ItemView的双击事件触发时调用
         */
        default void onItemDoubleClick(View view, int position) {
        }
    }
}
