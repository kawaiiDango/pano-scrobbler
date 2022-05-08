/**
 * Copyright (C) 2016 Robinhood Markets, Inc.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.robinhood.spark;

import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;

/**
 * Exposes simple methods for detecting scrub events.
 */
class ScrubHoverGestureDetector implements View.OnHoverListener {
    private final ScrubListener scrubListener;
    private final float touchSlop;

    private boolean enabled;
    private float downX, downY;

    ScrubHoverGestureDetector(
            @NonNull ScrubListener scrubListener,
            float touchSlop) {
        this.scrubListener = scrubListener;
        this.touchSlop = touchSlop;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public boolean onHover(View view, MotionEvent event) {
        if (!enabled) return false;

        final float x = event.getX();
        final float y = event.getY();
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_HOVER_ENTER:
                downX = x;
                downY = y;
                return true;
            case MotionEvent.ACTION_HOVER_MOVE:
                float deltaX = x - downX;
                float deltaY = y - downY;
                if (deltaX >= touchSlop || deltaY >= touchSlop) {
                    scrubListener.onScrubbed(x, y);
//                        return true;
                }
                return true;
            case MotionEvent.ACTION_HOVER_EXIT:
            case MotionEvent.ACTION_CANCEL:
                scrubListener.onScrubEnded();
                return true;
            default:
                return false;
        }
    }
}

