package com.blinq.ui.adapters;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;

import com.blinq.ImageLoaderManager;
import com.blinq.R;
import com.blinq.models.social.window.MeCard;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;

import java.util.ArrayList;

/**
 * Maps a MeCard.SocialProfile to view
 * open external browser on profile click
 *
 * used in the me card
 *
 * Created by galbracha on 12/24/14.
 */
public class MeCardMutualFriendsAdapter extends ArrayAdapter<MeCard.MutualFriend> {

    private ImageLoaderManager imageLoaderManager;
    private DisplayImageOptions displayImageOptions;


    public MeCardMutualFriendsAdapter(Context context, ArrayList<MeCard.MutualFriend> photos) {
        super(context, 0, photos);
        imageLoaderManager = new ImageLoaderManager(context);

        displayImageOptions = new DisplayImageOptions.Builder()
                .cacheOnDisk(true)
                .cacheInMemory(false)
                .showImageOnLoading(R.drawable.white_bitmap)
                .imageScaleType(ImageScaleType.EXACTLY)
                .bitmapConfig(Bitmap.Config.RGB_565)
                .build();

        imageLoaderManager.setDisplayImageOptions(displayImageOptions);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final String photo = getItem(position).getPhotoUrl();
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.mutual_friend_row, parent, false);
        }
        ImageView imageView = (ImageView) convertView.findViewById(R.id.mutual_friend_photo);
        imageLoaderManager.loadImage(imageView, Uri.parse(photo));

        return convertView;
    }
}
