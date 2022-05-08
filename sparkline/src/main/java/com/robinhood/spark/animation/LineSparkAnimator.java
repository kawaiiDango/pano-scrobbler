package com.robinhood.spark.animation;

import android.animation.Animator;
import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.graphics.Path;
import android.graphics.PathMeasure;
import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.robinhood.spark.SparkView;

/**
 * Animates the sparkline by path-tracing from the first point to the last.
 */
public class LineSparkAnimator extends Animator implements SparkAnimator {

    private final ValueAnimator animator;

    public LineSparkAnimator() {
        animator = ValueAnimator.ofFloat(0, 1);
    }

    @Nullable
    @Override
    public Animator getAnimation(final SparkView sparkView) {
        final Path linePath = sparkView.getSparkLinePath();

        // get path length
        final PathMeasure pathMeasure = new PathMeasure(linePath, false);
        final float endLength = pathMeasure.getLength();

        if (endLength <= 0) {
            return null;
        }

        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float animatedValue = (float) animation.getAnimatedValue();

                float animatedPathLength = animatedValue * endLength;

                linePath.reset();
                pathMeasure.getSegment(0, animatedPathLength, linePath, true);

                // set the updated path for the animation
                sparkView.setAnimationPath(linePath);
            }
        });

        return animator;
    }

    @Override
    public long getStartDelay() {
        return animator.getStartDelay();
    }

    @Override
    public void setStartDelay(@IntRange(from = 0) long startDelay) {
        animator.setStartDelay(startDelay);
    }

    @Override
    public Animator setDuration(@IntRange(from = 0) long duration) {
        return animator.setDuration(duration);
    }

    @Override
    public long getDuration() {
        return animator.getDuration();
    }

    @Override
    public void setInterpolator(@Nullable TimeInterpolator timeInterpolator) {
        animator.setInterpolator(timeInterpolator);
    }

    @Override
    public boolean isRunning() {
        return animator.isRunning();
    }
}
