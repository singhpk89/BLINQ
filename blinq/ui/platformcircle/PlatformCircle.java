package com.blinq.ui.platformcircle;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.res.TypedArray;
import android.view.View;

import com.blinq.R;
import com.blinq.analytics.AnalyticsConstants;
import com.blinq.analytics.BlinqAnalytics;
import com.touchmenotapps.widget.radialmenu.menu.v1.RadialMenuItem;
import com.touchmenotapps.widget.radialmenu.menu.v1.RadialMenuWidget;

/**
 * Responsible for creation and initialization the platform circle.
 *
 * @author Johan Hansson
 */
public class PlatformCircle {

    /**
     * Number of items in the platform circle.
     */
    private static final int PLATFORM_CIRCLE_ITEMS_NUMBER = 8;

    private static final String TAG = "";

    /**
     * Menu item tag name.
     */
    private final String menuItemName = "%s";

    /**
     * Radial Menu Widget.
     */
    private RadialMenuWidget platformCircle;

    /**
     * Radial Menu frame.
     */
    private RadialMenuWidget platformCircleFrame;

    /**
     * Menu Items array.
     */
    private List<RadialMenuItem> menuItems;

    /**
     * Menu Frame Items array.
     */
    private List<RadialMenuItem> menuFrameItems;

    /**
     * Array of account icon.
     */
    private List<Integer> accountIcons;

    /**
     * Array of account disable icon.
     */
    private List<Integer> accountDisableIcons;

    /**
     * Array of frame colors.
     */
    private List<Integer> platformCircleFrameColors;

    /**
     * Array of frame colors alpha.
     */
    private List<Integer> platformCircleFrameColorsAlpha;

    /**
     * The center menu item.
     */
    private RadialMenuItem centerItem;

    /**
     * Context form the main application.
     */
    private Context context;

    /**
     * Selected item index
     */
    private int selectedItemIndex = -1;

    /**
     * Array of account icon IDs.
     */
    private TypedArray iconsIds;

    /**
     * Array of account disable icon IDs.
     */
    private TypedArray disableIconsIds;

    /**
     * Array of frame colors IDs.
     */
    private TypedArray frameColorsIds;

    /**
     * Array of frame colors alpha IDs.
     */
    private TypedArray frameColorsAlphaIds;

    private BlinqAnalytics analyticsManager;

    /**
     * Response for creating and showing platform circle menu.
     *
     * @param context Activity context.
     */
    public PlatformCircle(Context context) {

        super();
        this.context = context;
        analyticsManager = new BlinqAnalytics(context);
        // Create instance of Platform Circle Frame.
        platformCircleFrame = new RadialMenuWidget(context);

        // Create instance of Platform Circle Menu and attach the frame to it.
        platformCircle = new RadialMenuWidget(context, platformCircleFrame);

        // Initialize the array list of Platform Circle items.
        menuItems = new ArrayList<RadialMenuItem>();

        // Initialize the array list of Platform Circle Frame items.
        menuFrameItems = new ArrayList<RadialMenuItem>();

    }

    /**
     * Initializes platform circle view.
     */
    public void initializePlatformCircle() {

        // Initialize menu center item.
        initializeCenterItem();

        // Initialize menu items icons.
        initializeResourceIds();

        // Initialize menu items.
        createItems();

        // Set menu settings.
        setMenuSettings();

        // Set menu frame settings.
        setMenuFrameSettings();

    }

    /**
     * Initializes platform circle accounts icon id.
     */
    private void initializeResourceIds() {

        accountIcons = new ArrayList<Integer>();
        accountDisableIcons = new ArrayList<Integer>();
        platformCircleFrameColors = new ArrayList<Integer>();
        platformCircleFrameColorsAlpha = new ArrayList<Integer>();

        iconsIds = platformCircle.getResources().obtainTypedArray(
                R.array.platform_circle__icons);
        disableIconsIds = platformCircle.getResources().obtainTypedArray(
                R.array.Platform_circle_disable_icons);
        frameColorsIds = platformCircle.getResources().obtainTypedArray(
                R.array.Platform_circle_frame_colors);
        frameColorsAlphaIds = platformCircle.getResources().obtainTypedArray(
                R.array.Platform_circle_frame_colors_alpha);

        for (int index = 0; index < PLATFORM_CIRCLE_ITEMS_NUMBER; index++) {
            // Get resource id by its index.
            accountIcons.add(iconsIds.getResourceId(index, -1));
            accountDisableIcons.add(disableIconsIds.getResourceId(index, -1));
            platformCircleFrameColors.add(frameColorsIds.getColor(
                    index,
                    platformCircle.getResources().getColor(
                            R.color.default_color)
            ));
            platformCircleFrameColorsAlpha.add(frameColorsAlphaIds.getInteger(
                    index, -1));

        }

        iconsIds.recycle();
        disableIconsIds.recycle();
        frameColorsIds.recycle();
        frameColorsAlphaIds.recycle();

    }

    /**
     * Initializes the center item of platform circle.
     */
    private void initializeCenterItem() {

        String centerName = "Center";
        // Center item creation.
        centerItem = new RadialMenuItem(centerName, TAG);

        // Center item click listener.
        centerItem
                .setOnMenuItemPressed(new RadialMenuItem.RadialMenuItemClickListener() {

                    @Override
                    public void execute(RadialMenuItem radialMenuItem) {

                        selectedItemIndex = -1;
                        platformCircle.dismiss();
                        analyticsManager.sendEvent(AnalyticsConstants.CLICKED_ON_RETRO_CENTER_EVENT, false, AnalyticsConstants.ACTION_CATEGORY);


                    }

                    @Override
                    public void executeLongClick(RadialMenuItem arg0) {
                        selectedItemIndex = -1;
                        platformCircle.dismiss();
                    }
                });

    }

    /**
     * Set platform circle settings.
     */
    private void setMenuSettings() {

        platformCircle
                .setAnimationSpeed(PlatformCircleSettings.ANIMATION_SPEED);
        platformCircle.setIconSize(PlatformCircleSettings.MIN_ICON_SIZE,
                PlatformCircleSettings.MAX_ICON_SIZE);
        platformCircle.setTextSize(PlatformCircleSettings.TEXT_SIZE);
        platformCircle.setTextColor(
                context.getResources().getColor(
                        R.color.platform_circle_text_color),
                PlatformCircleSettings.TEXT_COLOR_ALPHA
        );
        platformCircle.setOutlineColor(
                context.getResources().getColor(
                        R.color.outline_platform_circle_color),
                PlatformCircleSettings.OUTLINE_COLOR_ALPHA
        );
        platformCircle.setInnerRingColor(
                context.getResources().getColor(
                        R.color.inner_platform_circle_color),
                PlatformCircleSettings.INNER_RING_COLOR_ALPHA
        );
        platformCircle.setCenterCircle(centerItem);
        platformCircle.setInnerRingRadius(PlatformCircleSettings.INNER_RADIUS,
                PlatformCircleSettings.OUTER_RADIUS);
        platformCircle
                .setCenterCircleRadius(PlatformCircleSettings.CENTER_CERCLE_RADIUS);
        platformCircle.setSelectedColor(
                context.getResources().getColor(
                        R.color.platform_circle_selected_item_color),
                PlatformCircleSettings.SELECTED_ITEM_COLOR_ALPHA
        );

        platformCircle.setCenterAlpha(PlatformCircleSettings.CENTER_ALPHA);

    }

    /**
     * Set platform circle frame settings.
     */
    private void setMenuFrameSettings() {

        platformCircleFrame
                .setAnimationSpeed(PlatformCircleSettings.FRAME_ANIMATION_SPEED);
        platformCircleFrame.setOutlineColor(
                context.getResources().getColor(
                        R.color.outline_platform_circle_color),
                PlatformCircleSettings.FRAME_OUTLINE_COLOR_ALPHA
        );
        platformCircleFrame.setInnerRingColor(
                context.getResources().getColor(
                        R.color.inner_platform_circle_color),
                PlatformCircleSettings.FRAME_INNER_RING_COLOR_ALPHA
        );
        platformCircleFrame.setCenterCircle(centerItem);
        platformCircleFrame.setInnerRingRadius(
                PlatformCircleSettings.FRAME_INNER_RADIUS,
                PlatformCircleSettings.FRAME_OUTER_RADIUS);
        platformCircleFrame
                .setCenterCircleRadius(PlatformCircleSettings.FRAME_CENTER_CERCLE_RADIUS);
        platformCircleFrame.setSelectedColor(
                context.getResources().getColor(
                        R.color.platform_circle_selected_item_color),
                PlatformCircleSettings.FRAME_SELECTED_ITEM_COLOR_ALPHA
        );

    }

    /**
     * Create platform circle items.
     */
    private void createItems() {

        String itemName;
        for (int index = 0; index < PLATFORM_CIRCLE_ITEMS_NUMBER; index++) {

            RadialMenuItem menuItem = new RadialMenuItem(String.format(
                    menuItemName, index), null);
            menuItem.setDisplayIcon(accountIcons.get(index));
            menuItem.setDisableMenuIcon(accountDisableIcons.get(index));
            menuItem.setEnable(false);
            menuItems.add(menuItem);
            itemName = String.format(menuItemName, index);
            RadialMenuItem menuFrameItem = new RadialMenuItem(itemName, TAG);
            menuFrameItem.setEnable(false);
            menuFrameItems.add(menuFrameItem);

        }

        platformCircleFrame.setBackgroundColors(platformCircleFrameColors);
        platformCircleFrame
                .setBackgroundColorsAlpha(platformCircleFrameColorsAlpha);

    }

    /**
     * Add new empty item to the menu at index i.
     */
    public void addEmptyItem() {

        String itemName = String.format(menuItemName, 0);
        RadialMenuItem menuItem = new RadialMenuItem(itemName, TAG);
        menuItems.add(menuItem);

    }

    /**
     * Add new item to the menu at index i.
     *
     * @param index         Item index.
     * @param accountIconId Account icon id.
     */
    public void addItem(int index, int accountIconId) {

        RadialMenuItem menuItem = new RadialMenuItem(String.format(
                menuItemName, index), null);
        menuItem.setDisplayIcon(accountIconId);

        menuItems.add(menuItem);

    }

    /**
     * Assign the array list to the menu.
     */
    public void completeInitialization() {

        platformCircleFrame.addMenuEntry(menuFrameItems);
        platformCircle.addMenuEntry(menuItems);

    }

    /**
     * Show the platform circle.
     *
     * @param v platform circle view.
     */
    public void show(View v) {

        int yPosition = ((context.getResources().getDisplayMetrics().heightPixels) / 2);

        platformCircleFrame.show(v, 0, yPosition, false);
        platformCircle.show(v, 0, yPosition, false);

    }

    /**
     * Hide the platform circle.
     */
    public void dismiss() {

        platformCircle.dismiss();

    }

    /**
     * Set select menu item.
     *
     * @param selectedItemIndex Index of selected menu item
     */
    public void setSelectedItemIndex(int selectedItemIndex) {

        this.selectedItemIndex = selectedItemIndex;

    }

    /**
     * @return Instance of the platform circle.
     */
    public RadialMenuWidget getPlatformCircle() {

        return platformCircle;

    }

    /**
     * @return Selected menu item index.
     */
    public int getSelectedItemIndex() {

        return selectedItemIndex;

    }

    /**
     * Set platform circle instance.
     *
     * @param platformCircle platform circle instance.
     */
    public void setPlatformCircle(RadialMenuWidget platformCircle) {

        this.platformCircle = platformCircle;

    }

    /**
     * @return Array of all menu items.
     */
    public List<RadialMenuItem> getMenuItems() {

        return menuItems;

    }

    /**
     * @return Array of all menu frame items.
     */
    public List<RadialMenuItem> getMenuFrameItems() {

        return menuFrameItems;

    }

    /**
     * Set the array of items for the platform circle.
     *
     * @param menuItems Array of platform circle items.
     */
    public void setMenuItems(List<RadialMenuItem> menuItems) {

        this.menuItems = menuItems;

    }

    /**
     * @return Application context.
     */
    public Context getContext() {

        return context;

    }

    /**
     * Set the application context.
     *
     * @param context Application context.
     */
    public void setContext(Context context) {

        this.context = context;

    }

    /**
     * Return menu item at the given index.
     *
     * @param index Item index.
     * @return Menu item.
     */
    public RadialMenuItem getItemAt(int index) {

        return menuItems.get(index);

    }

    public boolean isShown() {
        return platformCircle.isShown() && platformCircleFrame.isShown();
    }

}
