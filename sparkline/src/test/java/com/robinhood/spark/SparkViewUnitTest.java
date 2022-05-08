package com.robinhood.spark;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static junit.framework.Assert.assertEquals;

public class SparkViewUnitTest {
    List<Float> points = Arrays.asList(0f, 1f, 2f, 3f);

    @Test
    public void test_getNearestIndex_exact() {
        assertEquals(1, SparkView.getNearestIndex(points, 1f));
    }

    @Test
    public void test_getNearestIndex_first() {
        assertEquals(0, SparkView.getNearestIndex(points, -1f));
    }

    @Test
    public void test_getNearestIndex_last() {
        assertEquals(3, SparkView.getNearestIndex(points, 4f));
    }

    @Test
    public void test_getNearestIndex_down() {
        assertEquals(2, SparkView.getNearestIndex(points, 2.1f));
    }

    @Test
    public void test_getNearestIndex_up() {
        assertEquals(3, SparkView.getNearestIndex(points, 2.9f));
    }

    @Test
    public void test_getNearestIndex_halfway() {
        assertEquals(3, SparkView.getNearestIndex(points, 2.5f));
    }
}
