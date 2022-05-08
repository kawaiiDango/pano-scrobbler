package com.robinhood.spark;

import android.graphics.RectF;
import android.support.annotation.NonNull;

import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import static org.mockito.Matchers.anyFloat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

public class TestAdapter extends SparkAdapter {
    private float[] yData, xData;
    private RectF dataBounds;

    public void setYData(float[] yData) {
        this.yData = yData;
    }

    public void setXData(float[] xData) {
        this.xData = xData;
    }

    public void setDataBounds(float left, float top, float right, float bottom) {
        this.dataBounds = createMockRectF(left, top, right, bottom);
    }

    @Override
    public int getCount() {
        return yData == null ? 0 : yData.length;
    }

    @NonNull
    @Override
    public Object getItem(int index) {
        return yData[index];
    }

    @Override
    public float getY(int index) {
        return yData[index];
    }

    @Override
    public float getX(int index) {
        return  xData == null
                ? super.getX(index)
                : xData[index];
    }

    @Override
    public RectF getDataBounds() {
        return dataBounds == null
                ? super.getDataBounds()
                : dataBounds;
    }

    @Override
    RectF createRectF(float left, float top, float right, float bottom) {
        return createMockRectF(left, top, right, bottom);
    }

    public static RectF createMockRectF(float left, float top, float right, float bottom) {
        final RectF rectF = mock(RectF.class);
        rectF.left = left;
        rectF.top = top;
        rectF.right = right;
        rectF.bottom = bottom;
        when(rectF.width()).thenReturn(rectF.right - rectF.left);
        when(rectF.height()).thenReturn(rectF.bottom - rectF.top);

        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                reset(rectF);
                Object[] args = invocation.getArguments();
                float dx = (float) args[0];
                float dy = (float) args[1];
                rectF.left += dx;
                rectF.top += dy;
                rectF.right -= dx;
                rectF.bottom -= dy;
                return null;
            }
        }).when(rectF).inset(anyFloat(), anyFloat());

        return rectF;
    }
}
