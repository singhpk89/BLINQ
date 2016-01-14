package com.blinq.ui.recyclers;

import android.content.Context;
import android.view.View;
import android.widget.AbsListView;

import com.blinq.ui.adapters.MessageListAdapter;

/**
 * Created on 9/9/14.
 */
public class RecycleMessageListHolder implements AbsListView.RecyclerListener {
    @Override
    public void onMovedToScrapHeap(final View view) {
        Context context = view.getContext();

        MessageListAdapter.ViewHolder holder = (MessageListAdapter.ViewHolder  ) view.getTag();

        if (holder == null) {
            holder = new MessageListAdapter.ViewHolder();
            view.setTag(holder);
        }

        // Release contactImageView's reference
        if (holder.accountImage != null) {
            holder.accountImage.setImageDrawable(null);
            holder.accountImage.setImageBitmap(null);
        }

        if (holder.bubbleLayout != null){
            holder.bubbleLayout.setBackgroundResource(0);
            ;
        }

        if (holder.messageText !=null){
            holder.messageText.setText(null);
        }

        if (holder.messageTextDate !=null){
            holder.messageTextDate.setText(null);
        }

        if (holder.messageDate !=null){
            holder.messageDate.setText(null);

        }

     }
}
