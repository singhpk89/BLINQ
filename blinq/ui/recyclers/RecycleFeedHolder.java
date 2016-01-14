package com.blinq.ui.recyclers;

import android.content.Context;
import android.view.View;
import android.widget.AbsListView;

import com.blinq.ui.adapters.FeedAdapter;

/**
 * Created on 9/9/14.
 */
public class RecycleFeedHolder implements AbsListView.RecyclerListener {
    @Override
    public void onMovedToScrapHeap(final View view) {
        Context context = view.getContext();

        FeedAdapter.ViewHolder holder = (FeedAdapter.ViewHolder ) view.getTag();


        if (holder == null) {
            holder = new FeedAdapter.ViewHolder();
            view.setTag(holder);
        }

        // Release contactImageView's reference
        if (holder.senderImageView != null) {

            holder.senderImageView.setImageDrawable(null);
            holder.senderImageView.setImageBitmap(null);
            holder.senderImageView.setImageResource(0);
        }

        if (holder.platformsIconsViews !=null && holder.platformsIconsViews.length >0){
            for(int i=0; i < holder.platformsIconsViews.length; i++){
                if (holder.platformsIconsViews[i].platformIconImageView != null){
                    holder.platformsIconsViews[i].platformIconImageView.setImageDrawable(null);
                    holder.platformsIconsViews[i].platformIconImageView.setImageBitmap(null);
                    holder.platformsIconsViews[i].platformIconImageView.setImageResource(0);
                }
            }
        }


        if (holder.senderNameTextView != null) {
            holder.senderNameTextView.setText(null);
        }
        if (holder.messageSnippetTextView != null) {
            holder.messageSnippetTextView.setText(null);
        }
        if (holder.messageTimeTextView != null) {
            holder.messageTimeTextView.setText(null);
        }
        if (holder.feedSeparatorTextView != null) {
            holder.feedSeparatorTextView.setText(null);
        }

    }
}
