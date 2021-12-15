/*
 * Copyright (C) 2013 UNCOPT LLC.
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

package com.uncopt.android.widget.text.justify;

import android.content.Context;
import android.graphics.Typeface;
import android.text.Layout;
import android.text.method.LinkMovementMethod;
import android.util.AttributeSet;
import android.widget.TextView;

import org.jetbrains.annotations.NotNull;


/**
 * A TextView with justified text.<br>
 * The TextView has a ScrollingMovementMethod by default. You can change the MovementMethod,
 * but you should not set it to null.
 */
public class JustifiedTextView extends androidx.appcompat.widget.AppCompatTextView implements Justify.Justified {

    @SuppressWarnings("unused")
    public JustifiedTextView(final @NotNull Context context) {
        super(context);
        super.setMovementMethod(new LinkMovementMethod());
    }

    @SuppressWarnings("unused")
    public JustifiedTextView(final @NotNull Context context, final AttributeSet attrs) {
        super(context, attrs);
        if (getMovementMethod() == null) super.setMovementMethod(new LinkMovementMethod());
    }

    @SuppressWarnings("unused")
    public JustifiedTextView(final @NotNull Context context,
                             final AttributeSet attrs, final int defStyle) {
        super(context, attrs, defStyle);
        if (getMovementMethod() == null) super.setMovementMethod(new LinkMovementMethod());
    }

    @Override
    protected void onMeasure(final int widthMeasureSpec, final int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        // Make sure we don't call setupScaleSpans again if the measure was triggered
        // by setupScaleSpans itself.
        if (!mMeasuring) {
            final Typeface typeface = getTypeface();
            final float textSize = getTextSize();
            final float textScaleX = getTextScaleX();
            final boolean fakeBold = getPaint().isFakeBoldText();
            if (mTypeface != typeface ||
                    mTextSize != textSize ||
                    mTextScaleX != textScaleX ||
                    mFakeBold != fakeBold ||
                    invalidate) {
                invalidate = false;
                final int width = MeasureSpec.getSize(widthMeasureSpec);
                if (width > 0 && width != mWidth) {
                    mTypeface = typeface;
                    mTextSize = textSize;
                    mTextScaleX = textScaleX;
                    mFakeBold = fakeBold;
                    mWidth = width;
                    mMeasuring = true;
                    try {
                        // Setup ScaleXSpans on whitespaces to justify the text.
                        Justify.setupScaleSpans(this, mSpanStarts, mSpanEnds, mSpans);
                    } finally {
                        mMeasuring = false;
                    }
                }
            }
        }
    }

    @Override
    protected void onTextChanged(final CharSequence text,
                                 final int start, final int lengthBefore, final int lengthAfter) {
        super.onTextChanged(text, start, lengthBefore, lengthAfter);
        final Layout layout = getLayout();
        if (layout != null) {
            Justify.setupScaleSpans(this, mSpanStarts, mSpanEnds, mSpans);
        }
    }


    @Override
    @NotNull
    public TextView getTextView() {
        return this;
    }

    @Override
    public float getMaxProportion() {
        return Justify.DEFAULT_MAX_PROPORTION;
    }

    @Override
    public boolean getJustifyLastLine() {
        return justifyLastLine;
    }

    public void setJustifyLastLine(boolean b) {
        justifyLastLine = b;
    }

    private static final int MAX_SPANS = 512;

    private boolean mMeasuring = false;

    private Typeface mTypeface = null;
    private float mTextSize = 0f;
    private float mTextScaleX = 0f;
    private boolean mFakeBold = false;
    private int mWidth = 0;

    private int[] mSpanStarts = new int[MAX_SPANS];
    private int[] mSpanEnds = new int[MAX_SPANS];
    private Justify.ScaleSpan[] mSpans = new Justify.ScaleSpan[MAX_SPANS];

    private boolean justifyLastLine = false;
    public boolean invalidate = false;

}
