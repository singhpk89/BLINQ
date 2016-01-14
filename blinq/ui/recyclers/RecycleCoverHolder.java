package com.blinq.ui.recyclers;

import android.content.Context;
import android.view.View;
import android.widget.AbsListView;

import com.blinq.ui.adapters.PlatformCoverListAdapter;

/**
 * Created on 9/8/14.
 */
public class RecycleCoverHolder implements AbsListView.RecyclerListener {

    @Override
    public void onMovedToScrapHeap(final View view) {
        Context context = view.getContext();

        PlatformCoverListAdapter.ViewHolder holder = (PlatformCoverListAdapter.ViewHolder) view.getTag();
        if (holder == null) {
            holder = new PlatformCoverListAdapter.ViewHolder();
            view.setTag(holder);
        }

        // Release mBackground's reference
        if (holder.coverImage != null) {
            holder.coverImage.setImageDrawable(null);
            holder.coverImage.setImageBitmap(null);
        }


    }
}
