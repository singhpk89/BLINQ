package com.blinq.ui.views;

import com.blinq.R;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewPropertyAnimator;
import android.widget.TextView;

/**
 * Custom View to display undo bar.
 */
public class UndoActionView {
    private static final String UNDO_MESSAGE_KEY = "undo_message";
    private static final String UNDO_TOKEN_KEY = "undo_token";
    private View barView;
    private TextView messageView;
    private ViewPropertyAnimator barAnimator;
    private Handler hideHandler = new Handler();

    private UndoListener undoListener;

    private int displayTime;

    // State objects
    private Parcelable undoToken;
    private CharSequence undoMessage;

    /**
     * The undo callback
     */
    public interface UndoListener {
        void onUndo(Parcelable token);
    }

    public UndoActionView(View undoBarView, UndoListener undoListener,
                          int displayTime) {
        barView = undoBarView;
        barAnimator = barView.animate();
        this.undoListener = undoListener;
        this.displayTime = displayTime;

        messageView = (TextView) barView.findViewById(R.id.undobar_message);
        barView.findViewById(R.id.undobar_button).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        hideUndoBar(false);
                        UndoActionView.this.undoListener.onUndo(undoToken);
                    }
                }
        );

        hideUndoBar(true);
    }

    /**
     * Display the undo action view for certain time
     */
    public void showUndoBar(boolean immediate, CharSequence message,
                            Parcelable undoToken) {
        this.undoToken = undoToken;
        undoMessage = message;
        messageView.setText(undoMessage);

        hideHandler.removeCallbacks(hideRunnable);
        hideHandler.postDelayed(hideRunnable, displayTime);

        barView.setVisibility(View.VISIBLE);
        if (immediate) {
            barView.setAlpha(1);
        } else {
            barAnimator.cancel();
            barAnimator
                    .alpha(1)
                    .setDuration(
                            barView.getResources().getInteger(
                                    android.R.integer.config_shortAnimTime)
                    )
                    .setListener(null);
        }
    }

    /**
     * Hide the Undo view.
     */
    public void hideUndoBar(boolean immediate) {
        hideHandler.removeCallbacks(hideRunnable);
        if (immediate) {
            barView.setVisibility(View.GONE);
            barView.setAlpha(0);
            undoMessage = null;
            undoToken = null;

        } else {
            barAnimator.cancel();
            barAnimator
                    .alpha(0)
                    .setDuration(
                            barView.getResources().getInteger(
                                    android.R.integer.config_shortAnimTime)
                    )
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            barView.setVisibility(View.GONE);
                            undoMessage = null;
                            undoToken = null;
                        }
                    });
        }
    }

    public void onSaveInstanceState(Bundle outState) {
        outState.putCharSequence(UNDO_MESSAGE_KEY, undoMessage);
        outState.putParcelable(UNDO_TOKEN_KEY, undoToken);
    }

    public void onRestoreInstanceState(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            undoMessage = savedInstanceState.getCharSequence(UNDO_MESSAGE_KEY);
            undoToken = savedInstanceState.getParcelable(UNDO_TOKEN_KEY);

            if (undoToken != null || !TextUtils.isEmpty(undoMessage)) {
                showUndoBar(true, undoMessage, undoToken);
            }
        }
    }

    /**
     * THe background worker that will hide the undo view.
     */
    private Runnable hideRunnable = new Runnable() {
        @Override
        public void run() {
            hideUndoBar(false);
        }
    };
}
