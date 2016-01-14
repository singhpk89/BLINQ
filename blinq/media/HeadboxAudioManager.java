package com.blinq.media;

import android.content.Context;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;

import com.blinq.SettingsManager;
import com.blinq.utils.AppUtils;

/**
 * Handle load/play/stop of application sounds.
 *
 * @author Johan Hansson;
 */
public class HeadboxAudioManager {

    private final String TAG = HeadboxAudioManager.class.getSimpleName();

    private static HeadboxAudioManager audioManager;

    private MediaPlayer mediaPlayer;
    private Context context;
    private SettingsManager settingsManager;

    public static HeadboxAudioManager getInstance(Context context) {

        if (audioManager == null) {

            audioManager = new HeadboxAudioManager();
        }

        audioManager.context = context;
        audioManager.settingsManager = new SettingsManager(context);

        return audioManager;
    }

    /**
     * Create media player object with given sound id.
     *
     * @param resId sound file id from resources.
     */
    public void setSoundFile(int resId) {

        mediaPlayer = MediaPlayer.create(context, resId);
    }

    /**
     * Play loaded sound.
     */
    public void play() {

        if (settingsManager.isSoundsSourceFromHeadbox()
                && AppUtils.getDeviceRingerMode(context) == android.media.AudioManager.RINGER_MODE_NORMAL
                && !AppUtils.isThereACall(context)) {

            mediaPlayer.start();

            mediaPlayer.setOnCompletionListener(new OnCompletionListener() {

                @Override
                public void onCompletion(MediaPlayer mp) {

                    mp.reset();
                    mp.release();

                }
            });

        }
    }

}
