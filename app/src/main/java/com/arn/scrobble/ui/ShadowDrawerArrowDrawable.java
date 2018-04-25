/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.arn.scrobble.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.support.annotation.FloatRange;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v4.view.ViewCompat;
import android.support.v7.graphics.drawable.DrawerArrowDrawable;

import com.arn.scrobble.Stuff;

/**
 * A drawable that can draw a "Drawer hamburger" menu or an arrow and animate between them.
 * <p>
 * The progress between the two states is controlled via {@link #setProgress(float)}.
 * </p>
 */
public class ShadowDrawerArrowDrawable extends DrawerArrowDrawable {

    private Paint mPaint;

    // The angle in degrees that the arrow head is inclined at.
    private static final float ARROW_HEAD_ANGLE = (float) Math.toRadians(45);
    // The length of top and bottom bars when they merge into an arrow
    private float mArrowHeadLength;
    // The length of middle bar
    private float mBarLength;
    // The length of the middle bar when arrow is shaped
    private float mArrowShaftLength;
    // The space between bars when they are parallel
    private float mBarGap;
    // Whether bars should spin or not during progress
    private boolean mSpin;
    // Use Path instead of canvas operations so that if color has transparency, overlapping sections
    // wont look different
    private final Path mPath = new Path();
    // Whether we should mirror animation when animation is reversed.
    private boolean mVerticalMirror = false;
    // The interpolated version of the original progress
    private float mProgress;
    // the amount that overlaps w/ bar size when rotation is max
    private float mMaxCutForBarSize;
    // The arrow direction
    private int mDirection;

    private int shadowColor = 0xcc000000;
    private int shadowOffset;

    /**
     * @param context used to get the configuration for the drawable from
     */
    public ShadowDrawerArrowDrawable(Context context) {
        super(context);
        shadowOffset = Stuff.INSTANCE.dp2px(1, context);
        mPaint = getPaint();
        mBarGap = getGapSize();
        mDirection = getDirection();
        mBarLength = getBarLength();
        mArrowHeadLength = getArrowHeadLength();
        mArrowShaftLength = getArrowShaftLength();
        mSpin = isSpinEnabled();
        mMaxCutForBarSize = (float) (getBarThickness() / 2 * Math.cos(ARROW_HEAD_ANGLE));
    }

    /**
     * If set, canvas is flipped when progress reached to end and going back to start.
     */
    public void setVerticalMirror(boolean verticalMirror) {
        if (mVerticalMirror != verticalMirror) {
            mVerticalMirror = verticalMirror;
            invalidateSelf();
        }
    }

    @Override
    public void draw(Canvas canvas) {
        Rect bounds = getBounds();

        final boolean flipToPointRight;
        switch (mDirection) {
            case ARROW_DIRECTION_LEFT:
                flipToPointRight = false;
                break;
            case ARROW_DIRECTION_RIGHT:
                flipToPointRight = true;
                break;
            case ARROW_DIRECTION_END:
                flipToPointRight = DrawableCompat.getLayoutDirection(this)
                        == ViewCompat.LAYOUT_DIRECTION_LTR;
                break;
            case ARROW_DIRECTION_START:
            default:
                flipToPointRight = DrawableCompat.getLayoutDirection(this)
                        == ViewCompat.LAYOUT_DIRECTION_RTL;
                break;
        }

        // Interpolated widths of arrow bars

        float arrowHeadBarLength = (float) Math.sqrt(mArrowHeadLength * mArrowHeadLength * 2);
        arrowHeadBarLength = lerp(mBarLength, arrowHeadBarLength, mProgress);
        final float arrowShaftLength = lerp(mBarLength, mArrowShaftLength, mProgress);
        // Interpolated size of middle bar
        final float arrowShaftCut = Math.round(lerp(0, mMaxCutForBarSize, mProgress));
        // The rotation of the top and bottom bars (that make the arrow head)
        final float rotation = lerp(0, ARROW_HEAD_ANGLE, mProgress);

        // The whole canvas rotates as the transition happens
        final float canvasRotate = lerp(flipToPointRight ? 0 : -180,
                flipToPointRight ? 180 : 0, mProgress);

        final float arrowWidth = Math.round(arrowHeadBarLength * Math.cos(rotation));
        final float arrowHeight = Math.round(arrowHeadBarLength * Math.sin(rotation));

        mPath.rewind();
        final float topBottomBarOffset = lerp(mBarGap + mPaint.getStrokeWidth(), -mMaxCutForBarSize,
                mProgress);

        final float arrowEdge = -arrowShaftLength / 2;
        // draw middle bar
        mPath.moveTo(arrowEdge + arrowShaftCut, 0);
        mPath.rLineTo(arrowShaftLength - arrowShaftCut * 2, 0);
        mPath.moveTo(arrowEdge + arrowShaftCut, 0);
        mPath.rLineTo(arrowShaftLength - arrowShaftCut * 2, 0);

        // bottom bar
        mPath.moveTo(arrowEdge, topBottomBarOffset);
        mPath.rLineTo(arrowWidth, arrowHeight);

        // top bar
        mPath.moveTo(arrowEdge, -topBottomBarOffset);
        mPath.rLineTo(arrowWidth, -arrowHeight);

        mPath.close();

        canvas.save();

        // Rotate the whole canvas if spinning, if not, rotate it 180 to get
        // the arrow pointing the other way for RTL.
        final float barThickness = mPaint.getStrokeWidth();
        final int remainingSpace = (int) (bounds.height() - barThickness * 3 - mBarGap * 2);
        float yOffset = (remainingSpace / 4) * 2; // making sure it is a multiple of 2.
        yOffset += barThickness * 1.5f + mBarGap;

        canvas.translate(bounds.centerX(), yOffset);
        if (mSpin) {
            canvas.rotate(canvasRotate * ((mVerticalMirror ^ flipToPointRight) ? -1 : 1));
        } else if (flipToPointRight) {
            canvas.rotate(180);
        }

        //paint the shadow
        final int color = mPaint.getColor();
        mPaint.setColor(shadowColor);
        canvas.drawPath(mPath, mPaint);

        //paint the fg
        mPaint.setColor(color);
        mPath.offset(shadowOffset,shadowOffset);
        canvas.drawPath(mPath, mPaint);


        canvas.restore();
    }

    /**
     * Returns the current progress of the arrow.
     */
    @FloatRange(from = 0.0, to = 1.0)
    public float getProgress() {
        return mProgress;
    }

    /**
     * Set the progress of the arrow.
     *
     * <p>A value of {@code 0.0} indicates that the arrow should be drawn in its starting
     * position. A value of {@code 1.0} indicates that the arrow should be drawn in its ending
     * position.</p>
     */
    public void setProgress(@FloatRange(from = 0.0, to = 1.0) float progress) {
        if (mProgress != progress) {
            mProgress = progress;
            invalidateSelf();
        }
    }

    public void setColors(int fg, int bg) {
        int shadowColor = bg & 0x00ffffff | 0xDC000000;
        if (shadowColor != this.shadowColor || mPaint.getColor() != fg){
            this.shadowColor = shadowColor;
            mPaint.setColor(fg);
            invalidateSelf();
        }
    }

    /**
     * Linear interpolate between a and b with parameter t.
     */
    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }
}