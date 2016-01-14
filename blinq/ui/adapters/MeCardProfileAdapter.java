package com.blinq.ui.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;

import com.blinq.R;
import com.blinq.models.social.window.MeCard;

import java.util.ArrayList;

/**
 * Maps a MeCard.SocialProfile to view
 * open external browser on profile click
 *
 * used in the me card
 *
 * Created by galbracha on 12/24/14.
 */
public class MeCardProfileAdapter extends ArrayAdapter<MeCard.SocialProfile> {

    private final Context context;

    public MeCardProfileAdapter(Context context, ArrayList<MeCard.SocialProfile> socialProfiles) {
        super(context, 0, socialProfiles);
        this.context = context;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final MeCard.SocialProfile profile = getItem(position);
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.social_profile_row, parent, false);
        }
        ImageView image = (ImageView) convertView.findViewById(R.id.social_profile_icon);
        image.setImageDrawable(context.getResources().getDrawable(profile.getPhoto()));

        return convertView;
    }
}
