package com.blinq.ui.views;

import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.blinq.R;
import com.blinq.utils.DialogUtils;

/**
 * Custom dialog builder with editable parts:-
 * <p>
 * <ul>
 * <li>1) Dialog view (all layout)</li>
 * <li>2) Dialog title</li>
 * <li>3) Dialog separator line color (between title & message)</li>
 * <li>4) Dialog message</li>
 * <li>5) Dialog icon</li>
 * </ul>
 * </p>
 *
 * @author Johan Hansson.
 */
public class CustomDialogBuilder extends Builder {

    /**
     * Application context.
     */
    private Context context;

    /**
     * The dialog layout.
     */
    private View dialogView;

    /**
     * Dialog title.
     */
    private TextView title;

    /**
     * Dialog icon.
     */
    private ImageView icon;

    /**
     * Dialog message.
     */
    private TextView message;

    /**
     * Dialog item list.
     */
    private ListView itemListView;

    /**
     * MessageLayout.
     */
    private LinearLayout messageLayout;

    /**
     * Line divider between dialog title & message.
     */
    private View divider;

    private AlertDialog createdDialog;

    private ImageButton rightImageButton;

    private final static float rotationDistance = 10.0f * 360.0f;

    private final static int rotationDuration = 8000;

    public CustomDialogBuilder(Context context, DialogBuilderType dialogType) {
        super(context);

        this.context = context;

        switch (dialogType) {
            case NORMAL:
                initNormalDialogBuilder();
                break;
            default:
                break;

        }

    }


    private void initNormalDialogBuilder() {
        // Build the layout of the dialog.
        dialogView = View.inflate(context, R.layout.waiting_dialog, null);
        setView(dialogView);

        title = (TextView) dialogView.findViewById(R.id.textViewForDialogTitle);
        message = (TextView) dialogView
                .findViewById(R.id.textViewForDialogMessage);
        icon = (ImageView) dialogView
                .findViewById(R.id.imageViewForLoadingIcon);
        divider = dialogView.findViewById(R.id.textViewForDialogSeperator);
        itemListView = (ListView) dialogView.findViewById(R.id.item_list);
        messageLayout = (LinearLayout) dialogView
                .findViewById(R.id.message_layout);

        RotateAnimation anim = new RotateAnimation(0.0f, rotationDistance,
                Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF,
                0.5f);
        anim.setInterpolator(new LinearInterpolator());
        anim.setRepeatCount(Animation.INFINITE);
        anim.setDuration(rotationDuration);

        // Start animating the image
        icon.startAnimation(anim);
    }

    public CustomDialogBuilder(Context context) {
        super(context);

        this.context = context;

        initNormalDialogBuilder();

    }

    public void stopAnimation() {
        if (icon != null)
            icon.setAnimation(null);
    }

    @Override
    public AlertDialog create() {
        createdDialog = super.create();
        return createdDialog;
    }

    /**
     * @param colorString color number in string format like "#FFAABB".
     */
    public CustomDialogBuilder setDividerColor(String colorString) {
        if (divider != null)
            divider.setBackgroundColor(Color.parseColor(colorString));
        return this;
    }

    @Override
    public CustomDialogBuilder setTitle(CharSequence text) {
        if (title != null)
            title.setText(text);
        return this;
    }

    /**
     * @param colorString color number in string format like "#FFAABB".
     */
    public CustomDialogBuilder setTitleColor(String colorString) {
        if (title != null)
            title.setTextColor(Color.parseColor(colorString));
        return this;
    }

    @Override
    public CustomDialogBuilder setMessage(CharSequence text) {
        if (message != null)
            message.setText(text);
        return this;
    }

    @Override
    public CustomDialogBuilder setIcon(int drawableResId) {
        if (icon != null)
            icon.setImageResource(drawableResId);
        return this;
    }

    @Override
    public CustomDialogBuilder setIcon(Drawable icon) {
        if (icon != null)
            this.icon.setImageDrawable(icon);
        return this;
    }

    public Builder setItemListView(List<CharSequence> items,
                                   final OnItemClickListener listener) {

        if (itemListView == null)
            return this;
        ArrayAdapter<CharSequence> itemAdapter = new ArrayAdapter<CharSequence>(
                context, R.layout.dialog_item_layout, items);

        itemListView.setAdapter(itemAdapter);
        itemListView.setVisibility(View.VISIBLE);
        itemListView.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                listener.onItemClick(parent, view, position, id);
                DialogUtils.hideDialog((Activity) context, createdDialog);
            }
        });

        return this;
    }

    /**
     * Control the show/hide of loading icon.
     *
     * @param showIcon true to show loading icon, false to hide it.
     * @return instance of the class.
     */
    public CustomDialogBuilder showIcon(boolean showIcon) {

        if (this.icon == null || messageLayout == null)
            return this;
        if (showIcon) {

            this.icon.setVisibility(View.VISIBLE);
            this.messageLayout.setVisibility(View.VISIBLE);

        } else {
            this.icon.setVisibility(View.GONE);
            if (message.getVisibility() == View.GONE) {
                this.messageLayout.setVisibility(View.GONE);
            }
        }

        return this;
    }

    /**
     * Control the show/hide of message.
     *
     * @param showMessage true to show message, false to hide it.
     * @return instance of the class.
     */
    public CustomDialogBuilder showMessage(boolean showMessage) {

        if (message == null)
            return this;
        if (showMessage) {
            this.message.setVisibility(View.VISIBLE);

            if (messageLayout != null)
                this.messageLayout.setVisibility(View.VISIBLE);

        } else {
            this.message.setVisibility(View.GONE);

            if (icon != null && messageLayout != null
                    && icon.getVisibility() == View.GONE) {
                this.messageLayout.setVisibility(View.GONE);
            }

        }

        return this;
    }


    public Builder setRightImageButton(android.view.View.OnClickListener listener) {

        if (rightImageButton != null) {
            rightImageButton.setOnClickListener(listener);
        }

        return this;
    }


    @Override
    public AlertDialog show() {

        // Check if there is no title, remove the title layout.
        if (title != null && title.getText().equals(""))
            dialogView.findViewById(R.id.textViewForDialogTitle).setVisibility(
                    View.GONE);

        return super.show();
    }

    public enum DialogBuilderType {
        NORMAL, UNDO_MERGE
    }

}
