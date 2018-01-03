package com.arn.scrobble.ui;

import android.content.Context;
import android.support.design.widget.AppBarLayout;
import android.util.AttributeSet;

/**
 * Created by arn on 03/01/2018.
 */
// https://gist.github.com/skimarxall/863585dcd7abde8f4153

public class MyAppBarLayout extends AppBarLayout
        implements AppBarLayout.OnOffsetChangedListener {
    State state;
    private OnStateChangeListener onStateChangeListener;

    public MyAppBarLayout(Context context) {
        super(context);
    }

    public MyAppBarLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        addOnOffsetChangedListener(this);
    }

    public boolean isExpanded(){
        return state == State.EXPANDED;
    }

    public boolean isCollapsed(){
        return state == State.COLLAPSED;
    }

    @Override
    public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
        if (verticalOffset == 0) {
            if (onStateChangeListener != null && state != State.EXPANDED) {
                onStateChangeListener.onStateChange(State.EXPANDED);
            }
            state = State.EXPANDED;
        } else if (Math.abs(verticalOffset) >= appBarLayout.getTotalScrollRange()) {
            if (onStateChangeListener != null && state != State.COLLAPSED) {
                onStateChangeListener.onStateChange(State.COLLAPSED);
            }
            state = State.COLLAPSED;
        } else {
            if (onStateChangeListener != null && state != State.IDLE) {
                onStateChangeListener.onStateChange(State.IDLE);
            }
            state = State.IDLE;
        }
    }

    public void setOnStateChangeListener(OnStateChangeListener listener) {
        this.onStateChangeListener = listener;
    }

    public interface OnStateChangeListener {
        void onStateChange(State toolbarChange);
    }

    public enum State {
        COLLAPSED,
        EXPANDED,
        IDLE
    }
}