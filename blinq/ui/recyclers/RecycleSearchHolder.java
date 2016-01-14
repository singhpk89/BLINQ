package com.blinq.ui.recyclers;

import android.content.Context;
import android.view.View;
import android.widget.AbsListView;

import com.blinq.ui.adapters.SearchAdapter;

/**
 * Created on 9/8/14.
 */
public class RecycleSearchHolder  implements AbsListView.RecyclerListener  {

    @Override
    public void onMovedToScrapHeap(final View view) {
        Context context = view.getContext();

        SearchAdapter.ViewHolder holder = (SearchAdapter.ViewHolder  ) view.getTag();

        if (holder == null) {
            holder = new SearchAdapter.ViewHolder();
            view.setTag(holder);
        }

        // Release contactImageView's reference
        if (holder.contactImageView != null) {
            holder.contactImageView.setImageDrawable(null);
            holder.contactImageView.setImageBitmap(null);

        }

        // Release lastUsedPlatformImageView's reference
        if (holder.lastUsedPlatformImageView != null) {
            holder.lastUsedPlatformImageView.setImageDrawable(null);
            holder.lastUsedPlatformImageView.setImageBitmap(null);
        }

        if (holder.contactNameTextView != null) {
            holder.contactNameTextView.setText(null);
        }

        if (holder.messageTimeTextView != null) {
            holder.messageTimeTextView.setText(null);
        }

        if (holder.searchSeparatorTextView != null) {
            holder.searchSeparatorTextView.setText(null);
        }

    }
}
