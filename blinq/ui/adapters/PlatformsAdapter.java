package com.blinq.ui.adapters;

import java.util.ArrayList;
import java.util.List;

import com.blinq.R;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;

/**
 * Take a list of icons id's and manage the process of displaying them in grid
 * fashion.
 *
 * @author Johan Hansson.
 */
public class PlatformsAdapter extends BaseAdapter {

    private List<Integer> iconsIds;

    public PlatformsAdapter() {

        iconsIds = new ArrayList<Integer>();
    }

    /**
     * @param iconsIds list of icons id's from resources.
     */
    public PlatformsAdapter(List<Integer> iconsIds) {
        this.iconsIds = iconsIds;
    }

    @Override
    public int getCount() {
        return iconsIds.size();
    }

    @Override
    public Object getItem(int index) {
        return iconsIds.get(index);
    }

    @Override
    public long getItemId(int index) {
        return index;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup viewGroup) {

        View rowView = convertView;

        ViewHolder viewHolder;

        Context context = viewGroup.getContext();

        if (rowView == null) {
            rowView = View
                    .inflate(context, R.layout.platform_item_layout, null);

            viewHolder = new ViewHolder();
            viewHolder.platformIcon = (ImageView) rowView
                    .findViewById(R.id.imageViewForPlatformIcon);

            rowView.setTag(viewHolder);

        } else {
            viewHolder = (ViewHolder) rowView.getTag();
        }

        // Handle empty grid items.
        if (iconsIds.get(position) != null) {
            viewHolder.platformIcon.setImageDrawable(context.getResources()
                    .getDrawable(iconsIds.get(position)));
        }

        return rowView;
    }

    /**
     * Hold a reference to view items, used to improve the performance of
     * displaying data.
     *
     * @author Johan Hansson
     */
    static class ViewHolder {
        private ImageView platformIcon;
    }
}
