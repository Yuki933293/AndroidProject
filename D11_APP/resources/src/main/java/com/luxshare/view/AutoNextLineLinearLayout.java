package com.luxshare.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.util.SparseIntArray;
import android.view.View;
import android.view.ViewGroup;

import com.luxshare.resource.R;

/**
 * @desc 自动换行
 *
 * @author hudebo
 * @date 2024/1/13
 */
public class AutoNextLineLinearLayout extends ViewGroup {
    private int rowHeight;//行高
    private boolean centerVerticalInRow;//是否在行竖直方向上居中
    private boolean hasRowHeight;
    private SparseIntArray rowHeightList = new SparseIntArray();//没有设置行高的话，通过计算保存每行的行高
    private SparseIntArray rowTopList = new SparseIntArray();//没有设置行高的话，每行顶部的高度

    public AutoNextLineLinearLayout(Context context) {
        this(context, null);
    }

    public AutoNextLineLinearLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AutoNextLineLinearLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        TypedArray typedArray = getContext().obtainStyledAttributes(attrs, R.styleable.AutoLineLayout);
        rowHeight = typedArray.getDimensionPixelSize(R.styleable.AutoLineLayout_row_height, 0);
        hasRowHeight = rowHeight > 0;
        centerVerticalInRow = typedArray.getBoolean(R.styleable.AutoLineLayout_center_vertical_in_row, true);
        typedArray.recycle();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec) - getPaddingLeft() - getPaddingRight();//子View的可填充宽度
        int count = getChildCount();
        int row = 0;
        int widthSpace = width;
        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);
            MarginLayoutParams lp = (MarginLayoutParams) child.getLayoutParams();
            int childWidthSpec;
            int childHeightSpec;
            if (lp.width > 0) {//计算子View宽，不应该超过当前可填充宽
                childWidthSpec = MeasureSpec.makeMeasureSpec(Math.min(width, lp.width), MeasureSpec.EXACTLY);
            } else {
                childWidthSpec = MeasureSpec.makeMeasureSpec(width, MeasureSpec.AT_MOST);
            }
            if (hasRowHeight) {
                if (lp.height > 0) {//计算子View高，不应该超过行高
                    childHeightSpec = MeasureSpec.makeMeasureSpec(Math.min(rowHeight, lp.height), MeasureSpec.EXACTLY);
                } else {
                    childHeightSpec = MeasureSpec.makeMeasureSpec(rowHeight, MeasureSpec.AT_MOST);
                }
            } else {
                if (lp.height > 0) {//计算子View高
                    childHeightSpec = MeasureSpec.makeMeasureSpec(lp.height, MeasureSpec.EXACTLY);
                } else {
                    childHeightSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
                }
            }
            child.measure(childWidthSpec, childHeightSpec);
            int childWidth = child.getMeasuredWidth() + lp.leftMargin + lp.rightMargin;
            if (childWidth <= widthSpace) {//根据宽度计算当前子View在哪一行
                child.setTag(row);
                widthSpace -= childWidth;
            } else {
                row++;
                child.setTag(row);
                widthSpace = width - childWidth;
                rowTopList.put(row, rowHeightList.get(row - 1) + rowTopList.get(row - 1));
            }
            int childHeight = child.getMeasuredHeight() + lp.topMargin + lp.bottomMargin;
            if (!hasRowHeight) {//没有设置行高的话，将行高设置为当前行子View最大的高度
                rowHeightList.put(row, Math.max(rowHeightList.get(row), childHeight));
            }
        }
        int heightSpec;
        if (hasRowHeight) {
            heightSpec = MeasureSpec.makeMeasureSpec(rowHeight * (row + 1) + getPaddingTop() + getPaddingBottom(), MeasureSpec.EXACTLY);
        } else {
            int allChildHeight = rowTopList.get(row) + rowHeightList.get(row);
            heightSpec = MeasureSpec.makeMeasureSpec(allChildHeight + getPaddingTop() + getPaddingBottom(), MeasureSpec.EXACTLY);
        }
        super.onMeasure(widthMeasureSpec, heightSpec);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);
            View preChild = getChildAt(i - 1);

            int childRow = (int) child.getTag();
            int currentRowHeight = hasRowHeight ? rowHeight : rowHeightList.get(childRow);

            MarginLayoutParams childLP = (MarginLayoutParams) child.getLayoutParams();

            int childWidth = child.getMeasuredWidth();
            int childHeight = child.getMeasuredHeight();
            int tempTop = centerVerticalInRow ? (currentRowHeight - childHeight) / 2 : childLP.topMargin;

            int left;
            int top;
            int right;
            int bottom;
            if (preChild != null && (int) preChild.getTag() == childRow) {
                MarginLayoutParams preChildLP = (MarginLayoutParams) preChild.getLayoutParams();
                left = preChild.getRight() + preChildLP.rightMargin + childLP.leftMargin;
            } else {
                left = getPaddingLeft() + childLP.leftMargin;
            }

            right = left + childWidth;

            int preRowHeight;//前几行的高度，用来计算当前View的TOP
            if (hasRowHeight) {
                preRowHeight = rowHeight * childRow;
            } else {
                preRowHeight = rowTopList.get(childRow);
            }
            top = getPaddingTop() + preRowHeight  + tempTop;
            bottom = top + childHeight;
            child.layout(left, top, right, bottom);
        }
    }

    @Override
    public LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new MarginLayoutParams(getContext(), attrs);
    }
}

