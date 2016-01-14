package com.blinq.ui.activities;

import android.app.Activity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;

import com.blinq.BlinqApplication;
import com.blinq.PreferencesManager;
import com.blinq.R;

/**
 * Created by Musab on 11/16/2014.
 */
public class PopupCheckboxFinalMsg extends Activity {

    private static final String TAG = PopupCheckboxFinalMsg.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        showAsPopup(this);

        BlinqApplication.analyticsSender.sendShowFinalMessagePopup();
        setContentView(R.layout.popup_checkbox_final_msg_window);
        this.setFinishOnTouchOutside(true);

        Button gotItButton = (Button) findViewById(R.id.popup_final_msg_window_got_it_button);
        gotItButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        getActionBar().hide();

        PreferencesManager preferencesManager = new PreferencesManager(this);
        preferencesManager.setOnboardingFinalMessageAppearedOnce(true);
    }

    /**
     * To show activity as dialog and dim the background with a specific layout configurations.
     */
    public static void showAsPopup(Activity activity) {

        activity.requestWindowFeature(Window.FEATURE_ACTION_BAR);
        activity.getWindow().setFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND,
                WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        WindowManager.LayoutParams params = activity.getWindow().getAttributes();

        DisplayMetrics displayMetrics = activity.getResources().getDisplayMetrics();

        float dpHeight = displayMetrics.heightPixels / displayMetrics.density;
        float dpWidth = displayMetrics.widthPixels / displayMetrics.density;

        params.height = (int) (dpHeight*displayMetrics.density * 3 / 5) ;
        params.width = (int) (dpWidth*displayMetrics.density * 4.5 / 5) ; //fixed width
        params.alpha = 1.0f;
        params.dimAmount = 0.5f;
        activity.getWindow().setAttributes(params);
    }




    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.new_social_window, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
       finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

}
