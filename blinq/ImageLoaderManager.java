package com.blinq;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.ImageView;

import com.blinq.analytics.BlinqAnalytics;
import com.blinq.models.Contact;
import com.blinq.models.FeedDesign;
import com.blinq.utils.ImageUtils;
import com.blinq.utils.Log;
import com.blinq.utils.StringUtils;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;
import com.nostra13.universalimageloader.core.download.BaseImageDownloader;
import com.nostra13.universalimageloader.core.listener.ImageLoadingListener;

import java.io.InputStream;

import de.hdodenhof.circleimageview.CircleImageView;

/**
 * Centralize loading images operation.
 *
 * @author Johan Hansson;
 */
public class ImageLoaderManager {

    private static final String USER_PROFILE_PHOTO_SUFFIX = "/photo";
    private final String TAG = ImageLoaderManager.class.getSimpleName();

    private DisplayImageOptions displayImageOptions;

    private ImageLoader imageLoader;

    private FeedDesign modeDesign;

    private Context context;
    private BlinqAnalytics analyticsManager;

    public ImageLoaderManager(Context context) {

        this.context = context;

        modeDesign = FeedDesign.getInstance();

        analyticsManager = new BlinqAnalytics(context);


        initializeImageLoaderWithDefaultConfigurations();
    }

    /**
     * Initialize image loader with default configuration as needed for our
     * application.
     */
    public void initializeImageLoaderWithDefaultConfigurations() {
        displayImageOptions = new DisplayImageOptions.Builder()
                .imageScaleType(ImageScaleType.EXACTLY)
                .bitmapConfig(Bitmap.Config.RGB_565)
                .cacheInMemory(true).cacheOnDisk(true).build();

        imageLoader = ImageLoader.getInstance();

    }

    public void loadContactImage(final ImageView imageView, final Uri uri) {

        String convertedUri = uri.toString().replace(USER_PROFILE_PHOTO_SUFFIX, "");
        BitmapDownloaderTask task = new BitmapDownloaderTask(context, imageView, convertedUri);
        task.execute();
    }

    /**
     * Set the Image view using URI.
     */
    public void loadImage(ImageView imageView, Uri uri) {

        try {
            if (uri.toString().length() == 0) {

                // Empty URI, load default image.
                imageView.setImageDrawable(context.getResources().getDrawable(
                        FeedDesign.getInstance().getSenderImageId()));

            } else {
                imageLoader.displayImage(uri.toString(), imageView,
                        displayImageOptions, new ImageLoadingListener() {
                            @Override
                            public void onLoadingStarted(String s, View view) {
                            }

                            @Override
                            public void onLoadingFailed(String s, View view, FailReason failReason) {

                            }

                            @Override
                            public void onLoadingComplete(String s, View view, Bitmap bitmap) {
                                Animation anim = new AlphaAnimation(0, 1);
                                anim.setDuration(300);
                                view.startAnimation(anim);
                            }

                            @Override
                            public void onLoadingCancelled(String s, View view) {

                            }
                        });
            }

        } catch (Exception e) {
            Log.d(TAG, "Invalid image uri: " + uri);
        }

    }


    /**
     * Set the Image view using URI. In case failed occur in loading the image dummy avatar will displayed.
     *
     * @param imageView image view to load image in.
     * @param contact   contact to load avatar for.
     */
    public void loadContactAvatarImage(final ImageView imageView, final Contact contact, final boolean isCalledFromFeed) {

        if (contact.isKnownContact()) {

            // Remove border for known contacts.
            if (imageView instanceof CircleImageView) {
                ((CircleImageView) imageView).setBorderWidth(0);
            }

            Uri photoUri = contact.getPhotoUri();

            // Use image loader
            if (photoUri == null || StringUtils.isBlank(photoUri.toString())) {

                // Empty URI, load default image.
                imageView.setImageResource(contact.getDummyAvatarImage());
                if (!isCalledFromFeed)
                    return;

            } else if (photoUri.toString().startsWith(ImageUtils.DRAWABLE_PATH)) {
                imageView.setImageResource(Integer.valueOf(photoUri.toString().substring(ImageUtils.DRAWABLE_PATH.length())));
            } else {

                imageLoader.displayImage(photoUri.toString(), imageView, displayImageOptions, new ImageLoadingListener() {

                    @Override
                    public void onLoadingStarted(String s, View view) {

                    }

                    @Override
                    public void onLoadingFailed(String s, View view, FailReason failReason) {

                        imageView.setImageResource(contact.getDummyAvatarImage());
                        if (!isCalledFromFeed)
                            return;
                    }


                    @Override
                    public void onLoadingComplete(String s, View view, Bitmap bitmap) {

                        if (contact.getContactType() == null || !isCalledFromFeed)
                            return;
                    }

                    @Override
                    public void onLoadingCancelled(String s, View view) {

                        imageView.setImageResource(contact.getDummyAvatarImage());
                    }
                });
            }

        } else {

            // Set empty image with border for un-known contacts.
            if (imageView instanceof CircleImageView) {
                ((CircleImageView) imageView).setBorderWidth((int) context.getResources().getDimension(R.dimen.unknown_contact_boarder_width));
                ((CircleImageView) imageView).setBorderColor(modeDesign.getFeedSeparatorColor());
                imageView.setImageResource(android.R.color.transparent);
            }
        }

    }

    private static Bitmap getBitmapFromContent(Context context, String imageUri) throws Exception {

        BaseImageDownloader downloader = new BaseImageDownloader(context);
        InputStream inputStream = downloader.getStream(imageUri, null);
        if (inputStream == null) {
            return null;
        }
        return BitmapFactory.decodeStream(inputStream);

    }

    /**
     * Load image synchronously, used in loading notification image before showing it.
     *
     * @param uri image URI to load.
     * @return bitmap of loaded image or default icon when failed.
     */
    public Bitmap loadImageSync(Uri uri) {

        Bitmap bmp;

        if (uri != null && !StringUtils.isBlank(uri.toString())) {

            // Load image, decode it to Bitmap and return Bitmap synchronously
            bmp = imageLoader.loadImageSync(uri.toString());

            if (bmp == null) {

                // Failed to load bitmap, load default icon from drawable.
                bmp = BitmapFactory.decodeResource(context.getResources(), R.drawable.statusbar_notifications_body_headbox);
            }

        } else {

            // Not correct URI provided, load default icon from drawable.
            bmp = BitmapFactory.decodeResource(context.getResources(), R.drawable.statusbar_notifications_body_headbox);
        }

        return bmp;
    }

    /**
     * Return current display image options of image loader.
     *
     * @return the display image options of image loader.
     */
    public DisplayImageOptions getDisplayImageOptions() {
        return displayImageOptions;
    }

    /**
     * Set custom display image options for image loader.
     *
     * @param displayImageOptions the display image options of image loader.
     */
    public void setDisplayImageOptions(DisplayImageOptions displayImageOptions) {
        this.displayImageOptions = displayImageOptions;
    }

    /**
     * Inner class to be use while displaying user photo.
     */
    private static class BitmapDownloaderTask extends AsyncTask<String, Void, Bitmap> {

        /**
         * Photo Url.
         */
        private String url;
        /**
         * Week reference to a target image view.
         */
        private final ImageView imageViewReference;
        private final Context context;

        public BitmapDownloaderTask(Context context, ImageView imageView, String url) {
            this.url = url;
            this.context = context;
            this.imageViewReference = imageView;
        }

        @Override
        // Actual download method, run in the task thread
        protected Bitmap doInBackground(String... params) {
            // params comes from the execute() call: params[0] is the url.
            try {
                return getBitmapFromContent(context, url);
            } catch (Exception e) {
                return null;
            }
        }

        @Override
        // Once the image is downloaded, associates it to the imageView
        protected void onPostExecute(Bitmap bitmap) {

            if (isCancelled()) {
                bitmap = null;
            }
            if (imageViewReference != null) {
                if (imageViewReference != null) {
                    imageViewReference.setImageBitmap(bitmap);
                }
            }
        }
    }
}
