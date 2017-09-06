package com.arn.scrobble.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;

import com.squareup.picasso.Transformation;

public class BlurTransform implements Transformation {

    private RenderScript rs;
    public BlurTransform(Context context) {
        super();
        rs = RenderScript.create(context);
    }


    public Bitmap transform(Bitmap bitmap, int w, int h) {
        // Create another bitmap that will hold the results of the filter.
        Bitmap blurredBitmap = Bitmap.createBitmap(bitmap);
/*
        if (bitmap.getWidth() >= h){

            blurredBitmap = Bitmap.createBitmap(
                    bitmap,
                    bitmap.getWidth()/2 - h/2,
                    0,
                    w,
                    h
            );

        }else{
            blurredBitmap = Bitmap.createBitmap(
                    bitmap,
                    0,
                    bitmap.getHeight()/2 - w/2,
                    w,
                    h
            );
        }
*/
        // Allocate memory for Renderscript to work with
        Allocation input = Allocation.createFromBitmap(rs, bitmap, Allocation.MipmapControl.MIPMAP_FULL, Allocation.USAGE_SHARED);
        Allocation output = Allocation.createTyped(rs, input.getType());

        // Load up an instance of the specific script that we want to use.
        ScriptIntrinsicBlur script = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs));
        script.setInput(input);

        // Set the blur radius
        script.setRadius(25);

        // Start the ScriptIntrinisicBlur
        script.forEach(output);

        // Copy the output to the blurred bitmap
        output.copyTo(blurredBitmap);

        return blurredBitmap;
    }
    @Override
    public Bitmap transform(Bitmap bitmap) {
        return transform(bitmap, bitmap.getWidth(), bitmap.getHeight());
    }
    @Override
    public String key() {
        return "blur";
    }

}