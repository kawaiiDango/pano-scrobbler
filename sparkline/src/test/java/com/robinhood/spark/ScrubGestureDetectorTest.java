package com.robinhood.spark;

import android.os.Handler;
import android.view.MotionEvent;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class ScrubGestureDetectorTest {
    private static final int TOUCH_SLOP = 8;
    private ScrubGestureDetector scrubGestureDetector;
    private ScrubListener scrubListener;
    private Handler handler;

    @Before
    public void setup() {
        scrubListener = mock(ScrubListener.class);
        handler = mock(Handler.class);
        scrubGestureDetector = new ScrubGestureDetector(scrubListener, handler, TOUCH_SLOP);
        scrubGestureDetector.setEnabled(true);
    }

    @Test
    public void test_disabled() {
        scrubGestureDetector.setEnabled(false);
        final MotionEvent down = getMotionEvent(MotionEvent.ACTION_DOWN, 0, 0, 0, 0);

        // verify all events are rejected at the start of the stream
        assertFalse(scrubGestureDetector.onTouch(null, down));
    }

    @Test
    public void test_moveScrub_success() {
        final long downTime = 0;
        final long moveTime = ScrubGestureDetector.LONG_PRESS_TIMEOUT_MS;
        final float scrubX = TOUCH_SLOP + 1, scrubY = TOUCH_SLOP + 1;
        final MotionEvent down = getMotionEvent(MotionEvent.ACTION_DOWN, 0, 0, 0, 0);
        final MotionEvent move = getMotionEvent(MotionEvent.ACTION_MOVE, scrubX, scrubY, moveTime,
                downTime);
        final MotionEvent up = getMotionEvent(MotionEvent.ACTION_UP, 0, 0, moveTime, downTime);

        assertTrue(scrubGestureDetector.onTouch(null, down));
        assertTrue(scrubGestureDetector.onTouch(null, move));
        assertTrue(scrubGestureDetector.onTouch(null, up));

        // verify scrub then end events
        verify(scrubListener).onScrubbed(scrubX, scrubY);
        verify(scrubListener).onScrubEnded();
        verifyNoMoreInteractions(scrubListener);
    }

    @Test
    public void test_moveScrub_waiting() {
        final long downTime = 0;
        // make just under the timeout
        final long moveTime = ScrubGestureDetector.LONG_PRESS_TIMEOUT_MS - 1;
        // make just under our touch-slop
        final float scrubX = TOUCH_SLOP - 1, scrubY = TOUCH_SLOP - 1;
        final MotionEvent down = getMotionEvent(MotionEvent.ACTION_DOWN, 0, 0, 0, 0);
        final MotionEvent move = getMotionEvent(MotionEvent.ACTION_MOVE, scrubX, scrubY, moveTime,
                downTime);

        assertTrue(scrubGestureDetector.onTouch(null, down));
        assertTrue(scrubGestureDetector.onTouch(null, move));

        // verify no scrub events
        verifyNoMoreInteractions(scrubListener);
    }

    @Test
    public void test_moveScrub_failure() {
        final long downTime = 0;
        // make just under the timeout
        final long moveTime = ScrubGestureDetector.LONG_PRESS_TIMEOUT_MS - 1;
        // make just over the touch-slop
        final float scrubX = 9, scrubY = 9;
        final MotionEvent down = getMotionEvent(MotionEvent.ACTION_DOWN, 0, 0, 0, 0);
        final MotionEvent move = getMotionEvent(MotionEvent.ACTION_MOVE, scrubX, scrubY, moveTime,
                downTime);

        assertTrue(scrubGestureDetector.onTouch(null, down));
        assertFalse(scrubGestureDetector.onTouch(null, move));

        // verify no scrub events
        verifyNoMoreInteractions(scrubListener);
    }

    @Test
    public void test_timeScrub_success() {
        // mock handler runs runnable immediately
        when(handler.postDelayed(any(Runnable.class), anyLong())).then(new Answer<Boolean>() {
            @Override
            public Boolean answer(InvocationOnMock invocation) throws Throwable {
                Runnable r = (Runnable) invocation.getArguments()[0];
                r.run();
                return true;
            }
        });
        float scrubX = 10, scrubY = 10;
        MotionEvent down = getMotionEvent(MotionEvent.ACTION_DOWN, scrubX, scrubY, 0, 0);
        assertTrue(scrubGestureDetector.onTouch(null, down));

        // verify single scrub event from handler/runnable timer
        verify(scrubListener).onScrubbed(scrubX, scrubY);
        verifyNoMoreInteractions(scrubListener);
    }

    private MotionEvent getMotionEvent(int action, float x, float y, long eventTime, long downTime) {
        MotionEvent motionEvent = Mockito.mock(MotionEvent.class);
        when(motionEvent.getX()).thenReturn(x);
        when(motionEvent.getY()).thenReturn(y);
        when(motionEvent.getEventTime()).thenReturn(eventTime);
        when(motionEvent.getDownTime()).thenReturn(downTime);
        when(motionEvent.getActionMasked()).thenReturn(action);
        return motionEvent;
    }
}
