package com.robinhood.spark;

import android.graphics.RectF;

import org.junit.Before;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;

public class ScaleHelperUnitTest {
    private RectF contentRect;
    private TestAdapter testAdapter;

    @Before
    public void setup() {
        // by default, all tests are on a canvas of 100 x 100
        contentRect = TestAdapter.createMockRectF(0, 0, 100, 100);
        testAdapter = new TestAdapter();
    }

    @Test
    public void testScaleTwoPoints() {
        testAdapter.setYData(new float[] {0, 1});
        SparkView.ScaleHelper scaleHelper = new SparkView.ScaleHelper(testAdapter, contentRect, 0,
                false);

        // assert point 0 is bottom left (0, 100)
        float x0 = scaleHelper.getX(testAdapter.getX(0));
        float y0 = scaleHelper.getY(testAdapter.getY(0));
        assertEquals(0f, x0);
        assertEquals(100f, y0);

        // assert point 1 is top right (100, 0)
        float x1 = scaleHelper.getX(testAdapter.getX(1));
        float y1 = scaleHelper.getY(testAdapter.getY(1));
        assertEquals(100f, x1);
        assertEquals(0f, y1);
    }

    @Test
    public void testScaleThreePoints() {
        testAdapter.setYData(new float[] {0, 1, 0});
        SparkView.ScaleHelper scaleHelper = new SparkView.ScaleHelper(testAdapter, contentRect, 0,
                false);

        // assert point 0 is bottom left (0, 100)
        float x0 = scaleHelper.getX(testAdapter.getX(0));
        float y0 = scaleHelper.getY(testAdapter.getY(0));
        assertEquals(0f, x0);
        assertEquals(100f, y0);

        // assert point 1 is top middle (50, 0)
        float x1 = scaleHelper.getX(testAdapter.getX(1));
        float y1 = scaleHelper.getY(testAdapter.getY(1));
        assertEquals(50f, x1);
        assertEquals(0f, y1);

        // assert point 2 is bottom right (100, 100)
        float x2 = scaleHelper.getX(testAdapter.getX(2));
        float y2 = scaleHelper.getY(testAdapter.getY(2));
        assertEquals(100f, x2);
        assertEquals(100f, y2);
    }

    @Test
    public void testLineWidthPadding() {
        testAdapter.setYData(new float[] {0, 1});
        float lineWidth = 10;
        SparkView.ScaleHelper scaleHelper = new SparkView.ScaleHelper(testAdapter, contentRect,
                lineWidth, false);

        // point 0 should be bottom-left, but offset for the supplied line-width
        float x0 = scaleHelper.getX(testAdapter.getX(0));
        float y0 = scaleHelper.getY(testAdapter.getY(0));
        assertEquals(lineWidth/2, x0);
        assertEquals(100f - (lineWidth/2), y0);

        // point 1 should be top-right, but offset for the supplied line-width
        float x1 = scaleHelper.getX(testAdapter.getX(1));
        float y1 = scaleHelper.getY(testAdapter.getY(1));
        assertEquals(100f - (lineWidth/2), x1);
        assertEquals(lineWidth/2, y1);
    }

    @Test
    public void testNonuniformXPoints() {
        testAdapter.setYData(new float[] {0, 1, 2, 3, 4});
        testAdapter.setXData(new float[] {0, 1, 2, 3, 100});
        SparkView.ScaleHelper scaleHelper = new SparkView.ScaleHelper(testAdapter, contentRect, 0,
                false);

        // point 0 is bottom left
        float x0 = scaleHelper.getX(testAdapter.getX(0));
        float y0 = scaleHelper.getY(testAdapter.getY(0));
        assertEquals(0f, x0);
        assertEquals(100f, y0);

        // point 1 is 1 px to the right and 1/4 of the way up
        float x1 = scaleHelper.getX(testAdapter.getX(1));
        float y1 = scaleHelper.getY(testAdapter.getY(1));
        assertEquals(1f, x1);
        assertEquals(75f, y1);

        // point 2 is 1 px to the right and another 1/4 of the way up
        float x2 = scaleHelper.getX(testAdapter.getX(2));
        float y2 = scaleHelper.getY(testAdapter.getY(2));
        assertEquals(2f, x2);
        assertEquals(50f, y2);

        // point 3 is 1 px to the right and another 1/4 of the way up
        float x3 = scaleHelper.getX(testAdapter.getX(3));
        float y3 = scaleHelper.getY(testAdapter.getY(3));
        assertEquals(3f, x3);
        assertEquals(25f, y3);

        // point 4 is top-right
        float x4 = scaleHelper.getX(testAdapter.getX(4));
        float y4 = scaleHelper.getY(testAdapter.getY(4));
        assertEquals(100f, x4);
        assertEquals(0f, y4);
    }

    @Test
    public void testNonWrappingDataBounds() {
        testAdapter.setYData(new float[] {0, 50, 100});
        // set bounds to 'zoom in' on the first two points
        testAdapter.setDataBounds(0, 0, 1, 50);
        SparkView.ScaleHelper scaleHelper = new SparkView.ScaleHelper(testAdapter, contentRect, 0,
                false);

        // assert point 0 is bottom left (0, 100)
        float x0 = scaleHelper.getX(testAdapter.getX(0));
        float y0 = scaleHelper.getY(testAdapter.getY(0));
        assertEquals(0f, x0);
        assertEquals(100f, y0);

        // assert point 1 is top right (100, 0)
        float x1 = scaleHelper.getX(testAdapter.getX(1));
        float y1 = scaleHelper.getY(testAdapter.getY(1));
        assertEquals(100f, x1);
        assertEquals(0f, y1);

        // assert point 2 is outside our content rect, far top right (200, -100)
        float x2 = scaleHelper.getX(testAdapter.getX(2));
        float y2 = scaleHelper.getY(testAdapter.getY(2));
        assertEquals(200f, x2);
        assertEquals(-100f, y2);
    }

    @Test
    public void testHorizontalLine() {
        testAdapter.setYData(new float[] {25, 25, 25});
        SparkView.ScaleHelper scaleHelper = new SparkView.ScaleHelper(testAdapter, contentRect, 0,
                false);

        // assert point 0 is left middle
        float x0 = scaleHelper.getX(testAdapter.getX(0));
        float y0 = scaleHelper.getY(testAdapter.getY(0));
        assertEquals(0f, x0);
        assertEquals(50f, y0);

        // assert point 1 is middle middle
        float x1 = scaleHelper.getX(testAdapter.getX(1));
        float y1 = scaleHelper.getY(testAdapter.getY(1));
        assertEquals(50f, x1);
        assertEquals(50f, y1);

        // assert point 2 is right middle
        float x2 = scaleHelper.getX(testAdapter.getX(2));
        float y2 = scaleHelper.getY(testAdapter.getY(2));
        assertEquals(100f, x2);
        assertEquals(50f, y2);
    }

    @Test
    public void testVerticalLine() {
        testAdapter.setYData(new float[] {0, 50, 100});
        testAdapter.setXData(new float[] {25, 25, 25});
        SparkView.ScaleHelper scaleHelper = new SparkView.ScaleHelper(testAdapter, contentRect, 0,
                false);

        // assert point 0 is middle bottom
        float x0 = scaleHelper.getX(testAdapter.getX(0));
        float y0 = scaleHelper.getY(testAdapter.getY(0));
        assertEquals(50f, x0);
        assertEquals(100f, y0);

        // assert point 1 is middle middle
        float x1 = scaleHelper.getX(testAdapter.getX(1));
        float y1 = scaleHelper.getY(testAdapter.getY(1));
        assertEquals(50f, x1);
        assertEquals(50f, y1);

        // assert point 2 is middle top
        float x2 = scaleHelper.getX(testAdapter.getX(2));
        float y2 = scaleHelper.getY(testAdapter.getY(2));
        assertEquals(50f, x2);
        assertEquals(0f, y2);
    }
}
