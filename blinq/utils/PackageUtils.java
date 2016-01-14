package com.blinq.utils;

import java.util.Arrays;
import java.util.List;

/**
 * We use this class to know better on what
 * are the different communication channels
 * our users are using to get their notifications
 * it is used along with the analytics class to know
 * if we should log this notification package name of not
 * It reduces the noise in the analytics events we are sending
 *
 * Created by galbracha on 12/22/14.
 */
public class PackageUtils {
    private static List<String> ignoredPackageNames = Arrays.asList(
            "android",
            "com.android.systemui",
            "com.android.bluetooth",
            "com.android.settings",
            "com.android.vending",
            "com.android.providers.downloads",
            "com.android.incallui",
            "com.android.server.telecom",
            "com.android.calendar",
            "com.google.android.apps.maps",
            "com.google.android.music",
            "com.google.android.googlequicksearchbox",
            "com.google.android.deskclock",
            "com.google.android.gms",
            "com.htc.usage",
            "com.htc.calendar",
            "com.htc.htcpowermanager",
            "com.htc.CustomizationSetup",
            "com.htc.android.mail",
            "com.lge.keepscreenon",
            "com.google.android.GoogleCamera",
            "com.lookout",
            "com.facebook.katana",
            "com.cleanmaster.security",
            "com.cleanmaster.mguard",
            "com.waze",
            "com.bitdefender.security",
            "com.sec.android.pagebuddynotisvc",
            "com.goldtouch.ynet"
    );

    public static boolean isIgnorePackageName(String name) {
        return ignoredPackageNames.contains(name);
    }
}
