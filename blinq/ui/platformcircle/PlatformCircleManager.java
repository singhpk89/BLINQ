package com.blinq.ui.platformcircle;

import android.app.Activity;
import android.content.Context;
import android.view.View;

import com.blinq.models.MemberContact;
import com.blinq.models.Platform;
import com.touchmenotapps.widget.radialmenu.menu.v1.RadialMenuItem;
import com.touchmenotapps.widget.radialmenu.menu.v1.RadialMenuItem.RadialMenuItemClickListener;

import java.util.HashMap;
import java.util.List;

/**
 * Responsible for call the Ring Menu and return the selected item index.
 *
 * @author Johan Hansson
 */
public class PlatformCircleManager {

    /**
     * Represents the retro dialer supported platforms mapped with item indices.
     */
    static HashMap<Platform, Integer> SUPPORTED_PLATFORMS_RETRO_INDICES = new HashMap<Platform, Integer>();

    static {

        SUPPORTED_PLATFORMS_RETRO_INDICES = new HashMap<Platform, Integer>();
        SUPPORTED_PLATFORMS_RETRO_INDICES.put(Platform.CALL, PlatformCircleSettings.CALL_INDEX);
        SUPPORTED_PLATFORMS_RETRO_INDICES.put(Platform.FACEBOOK, PlatformCircleSettings.FACEBOOK_INDEX);
        SUPPORTED_PLATFORMS_RETRO_INDICES.put(Platform.HANGOUTS, PlatformCircleSettings.HANGOUTS_INDEX);
        SUPPORTED_PLATFORMS_RETRO_INDICES.put(Platform.WHATSAPP, PlatformCircleSettings.WHATSAPP_INDEX);
        SUPPORTED_PLATFORMS_RETRO_INDICES.put(Platform.SKYPE, PlatformCircleSettings.SKYPE_INDEX);
        SUPPORTED_PLATFORMS_RETRO_INDICES.put(Platform.SMS, PlatformCircleSettings.MESSAGE_INDEX);
        SUPPORTED_PLATFORMS_RETRO_INDICES.put(Platform.EMAIL, PlatformCircleSettings.MAIL_FILLER_INDEX);
    }


    /**
     * Platform Circle menu object.
     */
    public PlatformCircle platformCircle;

    /**
     * Menu Items array.
     */
    private List<RadialMenuItem> menuItems;

    /**
     * Menu Frame Items array.
     */
    private List<RadialMenuItem> menuFrameItems;

    /**
     * Application main context.
     */
    private Activity activity;

    /**
     * Listener for the clicked menu item.
     */
    private RadialMenuItemClickListener radialMenuItemClickListener;

    /**
     * Flag to enable or disable Platform Circle.
     */
    private boolean enable = true;

    private HashMap<Platform, List<MemberContact>> memberContacts;

    /**
     * Response for initialization platform circle.
     *
     * @param context Activity Context.
     */
    public PlatformCircleManager(Activity context,
                                 RadialMenuItemClickListener radialMenuItemClickListener) {

        super();

        this.setActivity(context);
        this.radialMenuItemClickListener = radialMenuItemClickListener;
        initializePlatformCircle(context);

    }

    /**
     * Initialize Platform Circle
     *
     * @param context Activity Context.
     */
    private void initializePlatformCircle(Context context) {

        // Instance of the menu.
        platformCircle = new PlatformCircle(context);

        // Initialize the menu.
        platformCircle.initializePlatformCircle();

        // Get Platform Circle Items.
        menuItems = platformCircle.getMenuItems();

        // Get Platform Circle Frame Items.
        menuFrameItems = platformCircle.getMenuFrameItems();

        // Connect Listeners to menu items.
        applyMenuItemListeners();

        // Apply menu items array list to the ring menu.
        platformCircle.completeInitialization();

    }

    /**
     * Set platform circle items as enabled.
     */
    private void enableFrameItems() {

        if (isEnabled()) {

            for (Platform platform : SUPPORTED_PLATFORMS_RETRO_INDICES.keySet()) {

                //Enable the retro dialer outer frame if we
                //have a merged contact for this platform.
                int itemIndex = SUPPORTED_PLATFORMS_RETRO_INDICES.get(platform);
                boolean frameEnabled = memberContacts.containsKey(platform);
                menuFrameItems.get(itemIndex).setEnable(frameEnabled);
            }
        }

    }


    private void enableMenuItems() {

        initializeMenu();

        if (isEnabled()) {

            for (Platform platform : memberContacts.keySet()) {
                if (SUPPORTED_PLATFORMS_RETRO_INDICES.containsKey(platform)) {
                    int itemIndex = SUPPORTED_PLATFORMS_RETRO_INDICES.get(platform);
                    menuItems.get(itemIndex).setEnable(true);
                }
            }
        }

    }

    private void initializeMenu() {

        for (Platform platform : SUPPORTED_PLATFORMS_RETRO_INDICES.keySet()) {

            int itemIndex = SUPPORTED_PLATFORMS_RETRO_INDICES.get(platform);
            menuItems.get(itemIndex).setEnable(false);
            menuFrameItems.get(itemIndex).setEnable(false);
        }

    }

    /**
     * /** Apply action Listener for menu items.
     */
    private void applyMenuItemListeners() {

        for (RadialMenuItem item : menuItems)
            item.setOnMenuItemPressed(radialMenuItemClickListener);

    }

    /**
     * Show Platform Circle.
     *
     * @param v Menu view.
     */
    public void showPlatformCircle(View v, String phoneNumberToCal,
                                   HashMap<Platform, List<MemberContact>> memberContacts,
                                   boolean enable) {

        this.enable = enable;
        this.memberContacts = memberContacts;

        // Enabling user accessible menu items.
        enableMenuItems();

        // Enabling accessible menu frame.
        enableFrameItems();

        platformCircle.show(v);
    }

    /**
     * @return Main application context
     */
    public Activity getContext() {
        return activity;
    }

    /**
     * @param activity
     */
    public void setActivity(Activity activity) {
        this.activity = activity;
    }

    public HashMap<Platform, List<MemberContact>> getMemberContacts() {
        return memberContacts;
    }

    public void setMemberContacts(
            HashMap<Platform, List<MemberContact>> memberContacts) {
        this.memberContacts = memberContacts;
    }

    public boolean isEnabled() {
        return enable;
    }

    public void setEnable(boolean enable) {
        this.enable = enable;
    }

    public void dismiss() {
        platformCircle.dismiss();

    }

    public boolean isPlatformCircleShown() {
        return platformCircle.isShown();
    }



}