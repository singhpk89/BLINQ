package com.blinq.ui.animations;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.PorterDuff.Mode;
import android.graphics.drawable.BitmapDrawable;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.ImageView;

/**
 * Apply Wheel-Exit-Animation on given image-view.
 *
 * @author Johan Hansson.
 */
public class WheelExitAnimation extends Animation {

    public static final int CLOCKWISE = 1;
    public static final int COUNTER_CLOCKWISE = 2;

    // Retro dialer animation.
    public static final int RETRO_DIALER_ANIMATION_START_ANGLE = 270;
    public static final int RETRO_DIALER_ANIMATION_ARCANGLE = 360;
    public static final int RETRO_DIALER_ANIMATION_DURATION = 1200;

    // Account flipper dialer animation.
    public static final int ACCOUNT_FLIPPER_ANIMATION_START_ANGLE = 270;
    public static final int ACCOUNT_FLIPEER_ANIMATION_ARCANGLE = 360;
    public static final int ACCOUNT_FLIPEER_ANIMATION_DURATION = 800;

    private int startAngle;
    private int arcAngle;
    int direction;
    private ImageView imageView;

    private Bitmap bitmap;
    private Rect rect;

    /**
     * Constructor with parameters.
     *
     * @param duration   animation time in milliseconds.
     * @param startAngle angle to start animation from.
     * @param arcAngle   angle of the arc related to start angle.
     * @param direction  animation direction (clockwise or counter-clockwise).
     * @param imageView  image view to apply animation on.
     */
    public WheelExitAnimation(int duration, int startAngle, int arcAngle,
                              int direction, ImageView imageView) {

        super();
        this.startAngle = startAngle;
        this.arcAngle = arcAngle;
        this.direction = direction;
        this.imageView = imageView;

        init();

        setDuration(duration);

    }

    /**
     * Initialize needed components.
     */
    private void init() {

        bitmap = ((BitmapDrawable) imageView.getDrawable()).getBitmap();

        if (direction == CLOCKWISE) {

            rect = new Rect(-bitmap.getWidth(), -bitmap.getWidth(),
                    bitmap.getWidth() * 2, bitmap.getHeight() * 2);

        } else {

            rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());

        }
    }

    @Override
    protected void applyTransformation(float interpolatedTime, Transformation t) {

        super.applyTransformation(interpolatedTime, t);

        Bitmap circledBitmap = Bitmap.createBitmap(bitmap.getWidth(),
                bitmap.getHeight(), Bitmap.Config.ARGB_8888);

        Canvas canvas = new Canvas(circledBitmap);

        Paint paint = new Paint();
        paint.setAntiAlias(true);

        if (direction == CLOCKWISE) {

            canvas.drawArc(new RectF(rect), startAngle
                    + (interpolatedTime * arcAngle), arcAngle
                    - (interpolatedTime * arcAngle), true, paint);

        } else {

            canvas.drawArc(new RectF(rect), startAngle, arcAngle
                    - (interpolatedTime * arcAngle), true, paint);
        }

        paint.setXfermode(new PorterDuffXfermode(Mode.SRC_IN));
        canvas.drawBitmap(bitmap, rect, rect, paint);

        imageView.setImageBitmap(circledBitmap);
    }
}
