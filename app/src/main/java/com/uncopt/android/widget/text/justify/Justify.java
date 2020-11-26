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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.text.Layout;
import android.text.Spannable;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.style.MetricAffectingSpan;
import android.text.style.ScaleXSpan;
import android.widget.TextView;


class Justify {

  private static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\s");

  static final float DEFAULT_MAX_PROPORTION = 10f;

  /**
   * Adds ScaleX spans to expand widespaces and justify the lines.
   * @param justified the justified TextView.
   * @param textViewSpanEnds a preallocated array that will hold the span end positions.
   * @param textViewSpanStarts a preallocated array that will hold the span start positions.
   * @param textViewSpans a preallocated array that will hold the spans.
   */
  static void setupScaleSpans(final @NotNull Justified justified,
                              final @NotNull int[] textViewSpanStarts,
                              final @NotNull int[] textViewSpanEnds,
                              final @NotNull ScaleSpan[] textViewSpans) {
    final TextView textView = justified.getTextView();
    final CharSequence text = textView.getText();

    final float zero = textView.isInEditMode() ? 0.0001f : 0f;

    // The text should be a spannable already because we set a movement method.
    if (!(text instanceof Spannable)) return;
    final Spannable spannable = (Spannable)text;
    final int length = spannable.length();
    if (length == 0) return;

    // Remove any existing ScaleXSpan (from a previous pass).
    final ScaleSpan[] scaleSpans = spannable.getSpans(0, spannable.length(), ScaleSpan.class);
    if (scaleSpans != null) {
      for (final ScaleSpan span: scaleSpans) {
        spannable.removeSpan(span);
      }
    }

    // We use the layout to get line widths before justification
    final Layout layout = textView.getLayout();
    assert(layout != null);
    final int count = layout.getLineCount();
    if (count < 2 && !justified.getJustifyLastLine()) return;

    // Layout line widths do not include the padding
    final int want = textView.getMeasuredWidth() -
      textView.getCompoundPaddingLeft() - textView.getCompoundPaddingRight();

    // We won't justify lines if it requires expanding the spaces beyond the maximum proportion.
    final float maxProportion;
    if (textView instanceof Justified) {
      maxProportion = ((Justified)textView).getMaxProportion();
    }
    else {
      maxProportion = DEFAULT_MAX_PROPORTION;
    }

    for (int line=0; line<count; ++line) {

      final int lineStart = layout.getLineStart(line);
      final int lineEnd = line == count - 1 ? length : layout.getLineEnd(line);

      // Don't justify empty lines
      if (lineEnd == lineStart) continue;

      // Don't justify the last line or lines ending with a newline.
      if ((lineEnd == length || spannable.charAt(lineEnd - 1) == '\n') && !justified.getJustifyLastLine()) continue;

      // Don't include the trailing whitespace as an expandable whitespace.
      final int visibleLineEnd = layout.getLineVisibleEnd(line);
//      int visibleLineEnd = lineEnd;
//      while (visibleLineEnd > lineStart &&
//             Character.isWhitespace(spannable.charAt(visibleLineEnd-1))) --visibleLineEnd;

      // Don't justify lines that only contain whitespace
      if (visibleLineEnd == lineStart) continue;
      // Layout line width
//      final float w = layout.getLineWidth(line);    // Works fine, but only for API > 11
//      final float w = layout.getLineMax(line);      // Doesn't work well
      final float w = Layout.getDesiredWidth(spannable, lineStart, lineEnd, layout.getPaint());

      // Remaining space to fill
      int remaining = (int)Math.floor(want - w);

      if (remaining > 0) {
        // Make sure trailing whitespace doesn't use any space by setting its scaleX to 0
        if (visibleLineEnd < lineEnd) {
          spannable.setSpan(
            new ScaleXSpan(zero),
            visibleLineEnd,
            lineEnd,
            Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
        }

        // Line text
        final CharSequence sub = spannable.subSequence(lineStart, visibleLineEnd);

        // Accumulated total whitespace width
        float spaceWidth = 0f;
        // Number of whitespace sections
        int n = 0;

        // Find whitespace sections and store their start and end positions
        final Matcher matcher = WHITESPACE_PATTERN.matcher(sub);
        while (matcher.find()) {
          final int matchStart = matcher.start();
          final int matchEnd = matcher.end();
          // If the line starts with whitespace, it's probably an indentation
          // and we don't want to expand indentation space to preserve alignment
          if (matchStart == 0) continue;
          // skip single thin and hair spaces, as well as a single non breaking space
          if ((matchEnd - matchStart) == 1) {
            final int c = sub.charAt(matchStart);
            if (c == '\u200a' || c == '\u2009' || c == '\u00a0') continue;
          }
          assert(layout.getPaint() != null);
          final float matchWidth =
            layout.getPaint().measureText(spannable, lineStart + matchStart, lineStart + matchEnd);

          spaceWidth += matchWidth;

          textViewSpanStarts[n] = matchStart;
          textViewSpanEnds[n] = matchEnd;
          ++n;
        }
        if (n > textViewSpans.length) {
          n = textViewSpans.length;
        }

        // Excess space is distributed evenly
        // (with the same proportions for all whitespace sections)
        final float proportion = (spaceWidth + remaining) / spaceWidth;

        // Don't justify the line if we can't do it without expanding whitespaces too much.
//        if (proportion > maxProportion) continue;

        // Add ScaleX spans on the whitespace sections we want to expand.
        for (int span=0; span<n; ++span) {
          textViewSpans[span] = new ScaleSpan(proportion);
          spannable.setSpan(
            textViewSpans[span],
            lineStart + textViewSpanStarts[span],
            lineStart + textViewSpanEnds[span],
            Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
        }
        // Compute the excess space.
        int excess = (int)Math.ceil(Layout.getDesiredWidth(spannable,
                                                           lineStart, lineEnd,
                                                           layout.getPaint())) - want;
        // We might have added too much space because of rounding errors and because adding spans
        // can modify the kerning.
        // If that is the case, then we try to reduce the extra space slightly until there's no
        // excess space left.
        int loop = 0;
        while (excess > 0) {
          if (++loop == 4) {
            android.util.Log.e("ERROR",
                               "Could not compensate for excess space (" + excess + "px).");
          }
          // Clear the spans from the previous attempt.
          for (int span=0; span<n; ++span) {
            spannable.removeSpan(textViewSpans[span]);
          }
          // Reduce the remaining space exponentially for each iteration.
          remaining -= (excess + loop * loop);
          // Set the spans with the new proportions.
          final float reducedProportions = (spaceWidth + remaining) / spaceWidth;
          for (int span=0; span<n; ++span) {
            textViewSpans[span] = new ScaleSpan(reducedProportions);
            spannable.setSpan(
              textViewSpans[span],
              lineStart + textViewSpanStarts[span],
              lineStart + textViewSpanEnds[span],
              Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
          }
          // recompute the excess space.
          excess = (int)Math.ceil(Layout.getDesiredWidth(spannable,
                                                         lineStart, lineEnd,
                                                         layout.getPaint())) - want;
        }
      }
    }
  }

  static interface Justified {

    /**
     * Gets the TextView (usually the class implementing this interface).
     * @return the TextView.
     */
    @NotNull
    public TextView getTextView();

    /**
     * Gets the maximum stretching proportion allowed for whitespaces. Lines that require
     * expanding whitespace beyond this proportion to be justified will not be justified.
     * @return the maximum stretching proportion allowed.
     */
    public float getMaxProportion();

    public boolean getJustifyLastLine();

  }

  static class ScaleSpan extends MetricAffectingSpan {

    private final float mProportion;

    public ScaleSpan(final float proportion) {
      mProportion = proportion;
    }

    @Override
    public void updateDrawState(final @NotNull TextPaint ds) {
      ds.setTextScaleX(ds.getTextScaleX() * mProportion);
    }

    @Override
    public void updateMeasureState(final @NotNull TextPaint ds) {
      ds.setTextScaleX(ds.getTextScaleX() * mProportion);
    }

  }

}
