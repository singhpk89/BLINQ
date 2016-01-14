package com.blinq.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.view.View;

import com.blinq.R;

import java.io.FileOutputStream;
import java.util.List;

/**
 * Image Utils including:
 * Stack Blur Algorithm by Mario Klingemann <mario@quasimondo.com>
 * http://stackoverflow.com/questions/2067955/fast-bitmap-blur-for-android-sdk
 *
 * @author Johan Hansson.
 */
public class ImageUtils {


    private static final int BLUR_RADIUS = 5;

    private static final double IMAGEX2_CROPPED_PERCENTAGE = 0.5;
    private static final double IMAGEX2_CROPPED_MARGIN = 0.25;
    private static final double IMAGEX3_LARGE_IMAGE_PERCENTAGE = 0.58;
    private static final double IMAGEX3_SMALL_IMAGE_PERCENTAGE = 0.42;
    public static final String DRAWABLE_PATH = "drawable://";

    public ImageUtils() {

    }

    /**
     * @param bitmap bitmap to convert to blur.
     * @return blur bitmap.
     */
    public static Bitmap fastBlur(Bitmap bitmap) {

        return fastBlur(bitmap, BLUR_RADIUS);
    }

    /**
     * Convert a given bitmap to a blur bitmap using Gaussian Blur Algorithm.
     *
     * @param bitmap bitmap to convert to blur.
     * @param radius equal 2σ (standard deviation) , used in the Gaussian Blur
     *               Algorithm.
     * @return blur bitmap.
     */
    private static Bitmap fastBlur(Bitmap bitmap, int radius) {

        if (radius < 1) {
            return (null);
        }

        int w = bitmap.getWidth();
        int h = bitmap.getHeight();

        int[] pix = new int[w * h];
        Log.e("pix", w + " " + h + " " + pix.length);
        bitmap.getPixels(pix, 0, w, 0, 0, w, h);

        int wm = w - 1;
        int hm = h - 1;
        int wh = w * h;
        int div = radius + radius + 1;

        int r[] = new int[wh];
        int g[] = new int[wh];
        int b[] = new int[wh];
        int rsum, gsum, bsum, x, y, i, p, yp, yi, yw;
        int vmin[] = new int[Math.max(w, h)];

        int divsum = (div + 1) >> 1;
        divsum *= divsum;
        int dv[] = new int[256 * divsum];
        for (i = 0; i < 256 * divsum; i++) {
            dv[i] = (i / divsum);
        }

        yw = yi = 0;

        int[][] stack = new int[div][3];
        int stackpointer;
        int stackstart;
        int[] sir;
        int rbs;
        int r1 = radius + 1;
        int routsum, goutsum, boutsum;
        int rinsum, ginsum, binsum;

        for (y = 0; y < h; y++) {
            rinsum = ginsum = binsum = routsum = goutsum = boutsum = rsum = gsum = bsum = 0;
            for (i = -radius; i <= radius; i++) {
                p = pix[yi + Math.min(wm, Math.max(i, 0))];
                sir = stack[i + radius];
                sir[0] = (p & 0xff0000) >> 16;
                sir[1] = (p & 0x00ff00) >> 8;
                sir[2] = (p & 0x0000ff);
                rbs = r1 - Math.abs(i);
                rsum += sir[0] * rbs;
                gsum += sir[1] * rbs;
                bsum += sir[2] * rbs;
                if (i > 0) {
                    rinsum += sir[0];
                    ginsum += sir[1];
                    binsum += sir[2];
                } else {
                    routsum += sir[0];
                    goutsum += sir[1];
                    boutsum += sir[2];
                }
            }
            stackpointer = radius;

            for (x = 0; x < w; x++) {

                r[yi] = dv[rsum];
                g[yi] = dv[gsum];
                b[yi] = dv[bsum];

                rsum -= routsum;
                gsum -= goutsum;
                bsum -= boutsum;

                stackstart = stackpointer - radius + div;
                sir = stack[stackstart % div];

                routsum -= sir[0];
                goutsum -= sir[1];
                boutsum -= sir[2];

                if (y == 0) {
                    vmin[x] = Math.min(x + radius + 1, wm);
                }
                p = pix[yw + vmin[x]];

                sir[0] = (p & 0xff0000) >> 16;
                sir[1] = (p & 0x00ff00) >> 8;
                sir[2] = (p & 0x0000ff);

                rinsum += sir[0];
                ginsum += sir[1];
                binsum += sir[2];

                rsum += rinsum;
                gsum += ginsum;
                bsum += binsum;

                stackpointer = (stackpointer + 1) % div;
                sir = stack[(stackpointer) % div];

                routsum += sir[0];
                goutsum += sir[1];
                boutsum += sir[2];

                rinsum -= sir[0];
                ginsum -= sir[1];
                binsum -= sir[2];

                yi++;
            }
            yw += w;
        }
        for (x = 0; x < w; x++) {
            rinsum = ginsum = binsum = routsum = goutsum = boutsum = rsum = gsum = bsum = 0;
            yp = -radius * w;
            for (i = -radius; i <= radius; i++) {
                yi = Math.max(0, yp) + x;

                sir = stack[i + radius];

                sir[0] = r[yi];
                sir[1] = g[yi];
                sir[2] = b[yi];

                rbs = r1 - Math.abs(i);

                rsum += r[yi] * rbs;
                gsum += g[yi] * rbs;
                bsum += b[yi] * rbs;

                if (i > 0) {
                    rinsum += sir[0];
                    ginsum += sir[1];
                    binsum += sir[2];
                } else {
                    routsum += sir[0];
                    goutsum += sir[1];
                    boutsum += sir[2];
                }

                if (i < hm) {
                    yp += w;
                }
            }
            yi = x;
            stackpointer = radius;
            for (y = 0; y < h; y++) {
                // Preserve alpha channel: ( 0xff000000 & pix[yi] )
                pix[yi] = (0xff000000 & pix[yi]) | (dv[rsum] << 16)
                        | (dv[gsum] << 8) | dv[bsum];

                rsum -= routsum;
                gsum -= goutsum;
                bsum -= boutsum;

                stackstart = stackpointer - radius + div;
                sir = stack[stackstart % div];

                routsum -= sir[0];
                goutsum -= sir[1];
                boutsum -= sir[2];

                if (x == 0) {
                    vmin[y] = Math.min(y + r1, hm) * w;
                }
                p = x + vmin[y];

                sir[0] = r[p];
                sir[1] = g[p];
                sir[2] = b[p];

                rinsum += sir[0];
                ginsum += sir[1];
                binsum += sir[2];

                rsum += rinsum;
                gsum += ginsum;
                bsum += binsum;

                stackpointer = (stackpointer + 1) % div;
                sir = stack[stackpointer];

                routsum += sir[0];
                goutsum += sir[1];
                boutsum += sir[2];

                rinsum -= sir[0];
                ginsum -= sir[1];
                binsum -= sir[2];

                yi += w;
            }
        }

        Log.e("pix", w + " " + h + " " + pix.length);
        bitmap.setPixels(pix, 0, w, 0, 0, w, h);

        return (bitmap);
    }

    /**
     * Save bitmap in external files.
     *
     * @param fileName name of saved file.
     * @param bitmap   bitmap to save.
     */
    public static void saveBitmapToFile(Context context, String fileName,
                                        Bitmap bitmap) {

        String path = context.getFilesDir().getAbsolutePath();

        try {

            FileOutputStream out = new FileOutputStream(path + "/" + fileName);
            bitmap.compress(Bitmap.CompressFormat.PNG, 90, out);
            out.close();

        } catch (Exception e) {

            e.printStackTrace();
        }

    }

    /**
     * Get a Bitmap object from external files.
     *
     * @param fileName name of the bitmap file.
     * @return bitmap object from given file name;
     */
    public static Bitmap getBitmapFromFile(Context context, String fileName) {

        String path = context.getFilesDir().getAbsolutePath();

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        Bitmap bitmap = BitmapFactory
                .decodeFile(path + "/" + fileName, options);

        return bitmap;
    }

    /**
     * Take capture of given view and return it as bitmap.
     *
     * @param view to take capture of.
     * @return bitmap for a given view.
     */
    public static Bitmap takeCaptureOfView(View view) {

        view.setDrawingCacheEnabled(true);

        Bitmap bitmap = Bitmap.createBitmap(view.getDrawingCache());

        view.setDrawingCacheEnabled(false);

        return bitmap;

    }

    /**
     * Combine multiple bitmaps (2 or 3) into one bitmap.
     * <p>
     * <ul>
     * <li>- If 2 bitmaps , separate the space equally between them.
     * <li>- If 3 bitmaps , the first bitmap take
     * {@link ImageUtils#IMAGEX3_LARGE_IMAGE_PERCENTAGE} of the space & the
     * other two divide the {@link ImageUtils#IMAGEX3_SMALL_IMAGE_PERCENTAGE}
     * space vertically.
     * </ul>
     * </p>
     *
     * @param bitmaps list of bitmaps to combine.
     * @return combined bitmap.
     */
    public static Bitmap combineImages(List<Bitmap> bitmaps) {

        Bitmap combinedBitmap = null;

        if (bitmaps != null && bitmaps.size() > 1) {

            int width = bitmaps.get(0).getWidth();
            int height = bitmaps.get(0).getHeight();

            combinedBitmap = Bitmap.createBitmap(width, height,
                    Bitmap.Config.ARGB_8888);

            Canvas comboImage = new Canvas(combinedBitmap);

            // Check number of bitmaps.
            if (bitmaps.size() == 2) {

                // Two bitmaps, divide the image equally between both.
                Bitmap first = bitmaps.get(0);
                Bitmap second = bitmaps.get(1);

                // Get scaled bitmaps of the given bitmaps
                Bitmap firstCropped = Bitmap.createBitmap(first,
                        (int) (IMAGEX2_CROPPED_MARGIN * width), 0,
                        (int) (IMAGEX2_CROPPED_PERCENTAGE * width), height);

                Bitmap secondCropped = Bitmap.createBitmap(second,
                        (int) (IMAGEX2_CROPPED_MARGIN * second.getWidth()), 0,
                        (int) (IMAGEX2_CROPPED_PERCENTAGE * second.getWidth()),
                        second.getHeight());

                // Draw scaled bitmaps on one bitmap.
                comboImage.drawBitmap(firstCropped, 0f, 0f, null);
                comboImage.drawBitmap(secondCropped,
                        firstCropped.getWidth() + 1, 0f, null);

            } else {

                // More than two bitmaps, take the first 3 bitmaps and combine
                // them together.
                Bitmap first = bitmaps.get(0);
                Bitmap second = bitmaps.get(1);
                Bitmap third = bitmaps.get(2);

                Bitmap firstCropped = Bitmap.createBitmap(first, 0, 0,
                        (int) (IMAGEX3_LARGE_IMAGE_PERCENTAGE * first
                                .getWidth()), first.getHeight()
                );

                Bitmap secondScalled = scaleBitmap(second,
                        (int) (IMAGEX3_SMALL_IMAGE_PERCENTAGE * first
                                .getWidth()), firstCropped.getHeight() / 2
                );

                Bitmap thirdScalled = scaleBitmap(third,
                        (int) (IMAGEX3_SMALL_IMAGE_PERCENTAGE * first
                                .getWidth()), firstCropped.getHeight() / 2
                );

                // Draw scaled bitmaps on one bitmap.
                comboImage.drawBitmap(firstCropped, 0f, 0f, null);

                comboImage.drawBitmap(secondScalled,
                        firstCropped.getWidth() + 1, 0f, null);

                comboImage.drawBitmap(thirdScalled,
                        firstCropped.getWidth() + 1,
                        secondScalled.getHeight() + 1, null);
            }
        }

        return combinedBitmap;
    }

    /**
     * Scale bitmap to specific dimensions.
     *
     * @param bitmap    bitmap to be scaled.
     * @param newWidth  new bitmap width
     * @param newHeight new bitmap height
     * @return scaled bitmap
     */
    public static Bitmap scaleBitmap(Bitmap bitmap, int newWidth, int newHeight) {

        Bitmap scaledBitmap = Bitmap.createBitmap(newWidth, newHeight,
                Config.ARGB_8888);

        float ratioX = newWidth / (float) bitmap.getWidth();
        float ratioY = newHeight / (float) bitmap.getHeight();
        float middleX = newWidth / 2.0f;
        float middleY = newHeight / 2.0f;

        Matrix scaleMatrix = new Matrix();
        scaleMatrix.setScale(ratioX, ratioY, middleX, middleY);

        Canvas canvas = new Canvas(scaledBitmap);
        canvas.setMatrix(scaleMatrix);
        canvas.drawBitmap(bitmap, middleX - bitmap.getWidth() / 2, middleY
                - bitmap.getHeight() / 2, new Paint(Paint.FILTER_BITMAP_FLAG));

        return scaledBitmap;
    }

    /**
     * Return bitmap object from resource id.
     *
     * @param context    context of application
     * @param resourceID Id for given resource.
     * @return bitmap object for given id.
     */
    public static Bitmap getResourceBitmap(Context context, int resourceID) {

        Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(),
                resourceID);

        return bitmap;
    }

    public static Dimensions getResourceImageDimensions(Context context, int resourceID) {
        BitmapFactory.Options dimensions = new BitmapFactory.Options();
        dimensions.inJustDecodeBounds = true;
        Bitmap mBitmap = BitmapFactory.decodeResource(context.getResources(), resourceID, dimensions);
        int height = dimensions.outHeight;
        int width =  dimensions.outWidth;
        return new Dimensions(height, width);
    }

    public static class Dimensions {
        Dimensions(int height, int width) {
            this.height = height;
            this.width = width;
        }
        public int height;
        public int width;
    }
}
