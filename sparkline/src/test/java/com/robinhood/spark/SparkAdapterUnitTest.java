package com.robinhood.spark;

import android.graphics.RectF;

import org.junit.Test;

import static junit.framework.Assert.assertEquals;

public class SparkAdapterUnitTest {

    @Test
    public void test_getDataBounds_withData() {
        TestAdapter testAdapter = new TestAdapter();
        testAdapter.setYData(new float[] {0, 500, 100});
        testAdapter.setXData(new float[] {50, 99, 100});
        RectF bounds = testAdapter.getDataBounds();

        assertEquals(50f, bounds.left);
        assertEquals(100f, bounds.right);
        assertEquals(0f, bounds.top);
        assertEquals(500f, bounds.bottom);
    }

    @Test
    public void test_getDataBounds_withNegativeData() {
        TestAdapter testAdapter = new TestAdapter();
        testAdapter.setYData(new float[] {0, -500, -100});
        testAdapter.setXData(new float[] {-50, -99, -100});
        RectF bounds = testAdapter.getDataBounds();

        assertEquals(-100f, bounds.left);
        assertEquals(-50f, bounds.right);
        assertEquals(-500f, bounds.top);
        assertEquals(0f, bounds.bottom);
    }

    @Test
    public void test_getDataBounds_noData() {
        TestAdapter testAdapter = new TestAdapter();
        RectF bounds = testAdapter.getDataBounds();

        // with no data, we expect the mins to be +MAX_VALUE and maxes to be -MAX_VALUE
        assertEquals(Float.MAX_VALUE, bounds.left);
        assertEquals(-Float.MAX_VALUE, bounds.right);
        assertEquals(Float.MAX_VALUE, bounds.top);
        assertEquals(-Float.MAX_VALUE, bounds.bottom);
    }
}
