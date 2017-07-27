package com.arn.ytscrobble.ui;

/**
 * Created by arn on 25/07/2017.
 */

/**
 *   https://gist.github.com/Kevinrob/09742d9069e4e4e4ab66
 *   */

import android.content.Context;
import android.content.res.Resources;
import android.text.Layout;
import android.text.Layout.Alignment;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.widget.TextView;


public class FixedLineTextView extends android.support.v7.widget.AppCompatTextView {

    /**
     * Original textSize in PX
     */
    protected Float mOriginalSize;
    // Minimum text size for this text view
    public static final float MIN_TEXT_SIZE = 30;

    public FixedLineTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setEllipsize(TextUtils.TruncateAt.END);
    }

    public FixedLineTextView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FixedLineTextView(Context context) {
        this(context, null);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        final Layout layout = getLayout();
        if (layout != null) {
            final int lineCount = layout.getLineCount();
            if (lineCount > 0) {
                final int ellipsisCount = layout.getEllipsisCount(lineCount - 1);
                if (ellipsisCount > 0) {

                    final float textSize = getTextSize();
                    if (textSize <= MIN_TEXT_SIZE)
                        return;

                    if (mOriginalSize == null) {
                        mOriginalSize = textSize;
                    }

                    float before = mOriginalSize;

                    // textSize is already expressed in pixels
                    setTextSize(TypedValue.COMPLEX_UNIT_PX, (textSize - 1));

                    mOriginalSize = before;

                    // recursion
                    measure(widthMeasureSpec, heightMeasureSpec);
                }
            }
        }
    }

    @Override
    public void setTextSize(int unit, float size) {
        super.setTextSize(unit, size);

        Context c = getContext();
        Resources r;

        if (c == null)
            r = Resources.getSystem();
        else
            r = c.getResources();

        mOriginalSize = TypedValue.applyDimension(unit, size, r.getDisplayMetrics());
    }

    @Override
    public void setText(CharSequence text, BufferType type) {
        if (mOriginalSize != null) {
            setTextSize(TypedValue.COMPLEX_UNIT_PX, mOriginalSize);
        }

        super.setText(text, type);

        requestLayout();
    }
}