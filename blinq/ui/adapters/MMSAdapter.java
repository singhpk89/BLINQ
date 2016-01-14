package com.blinq.ui.adapters;

import java.util.List;

import android.content.Context;
import android.net.Uri;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;

import com.blinq.R;
import com.blinq.ImageLoaderManager;

/**
 * Adapter to display MMS messages.
 *
 * @author Johan Hansson.
 */
public class MMSAdapter extends BaseAdapter {

    private final String TAG = MMSAdapter.class.getSimpleName();

    private List<Uri> imageUris;

    private Context context;

    private ImageLoaderManager imageLoaderManager;

    public MMSAdapter(Context context, List<Uri> imageUris) {

        this.context = context;
        this.imageUris = imageUris;

        imageLoaderManager = new ImageLoaderManager(context);
    }

    @Override
    public int getCount() {
        return imageUris.size();
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        ViewHolder holder;
        View view = convertView;

        if (view == null) {

            view = View.inflate(context, R.layout.mms_image_item, null);

            holder = new ViewHolder();

            if (view != null) {

                holder.imageView = (ImageView) view
                        .findViewById(R.id.imageViewMMSItem);
                view.setTag(holder);
            }

        } else {
            holder = (ViewHolder) view.getTag();
        }

        imageLoaderManager.loadImage(holder.imageView, imageUris.get(position));

        return view;
    }

    /**
     * Inner class to hold views of adapter item.
     *
     * @author Johan Hansson.
     */
    static class ViewHolder {
        ImageView imageView;
    }
}
