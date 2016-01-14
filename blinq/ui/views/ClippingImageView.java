package com.blinq.ui.views;


import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.ImageView;

/**
 * Taken from:
 * http://stackoverflow.com/questions/18305219/how-to-animate-a-matrix-to-crop-out-an-image
 * To solve the moving animated social window
 */
public class ClippingImageView extends ImageView {

    private final Rect mClipRect = new Rect();

    public ClippingImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initClip();
    }

    public ClippingImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initClip();
    }

    public ClippingImageView(Context context) {
        super(context);
        initClip();
    }

    private void initClip() {
        // post to message queue, so it gets run after measuring & layout
        // Starts hidden
        post(new Runnable() {
            @Override public void run() {
                setImageCrop(0);
            }
        });
    }

    @Override protected void onDraw(Canvas canvas) {
        // clip if needed and let super take care of the drawing
        if (clip()) canvas.clipRect(mClipRect);
        super.onDraw(canvas);
    }

    private boolean clip() {
        // true if clip bounds have been set aren't equal to the view's bounds
        return !mClipRect.isEmpty() && !clipEqualsBounds();
    }

    private boolean clipEqualsBounds() {
        final int width = getWidth();
        final int height = getHeight();
        // whether the clip bounds are identical to this view's bounds (which effectively means no clip)
        return mClipRect.width() == width && mClipRect.height() == height;
    }

    public void toggle() {
        // toggle between [0...0.5] and [0.5...0]
        final float[] values = clipEqualsBounds() ? new float[] { 0.5f, 0f } : new float[] { 0, 0.5f };
        ObjectAnimator.ofFloat(this, "imageCrop", values).start();
    }

    public void setImageCrop(float value) {
        // nothing to do if there's no drawable set
        final Drawable drawable = getDrawable();
        if (drawable == null) return;

        // nothing to do if no dimensions are known yet
        final int width = getWidth();
        final int height = getHeight();
        if (width <= 0 || height <= 0) return;

        // construct the clip bounds based on the supplied 'value' (which is assumed to be within the range [0...1])
        final int clipWidth = (int) (value * width);
        final int clipHeight = (int) (value * height);
        final int left = clipWidth / 2;
        final int top = clipHeight / 2;
        final int right = width - left;
        final int bottom = height - top;

        // set clipping bounds
        mClipRect.set(left, top, right, bottom);
        // schedule a draw pass for the new clipping bounds to take effect visually
        invalidate();
    }

}