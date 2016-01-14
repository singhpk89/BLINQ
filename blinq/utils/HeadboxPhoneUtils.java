package com.blinq.utils;

import android.content.Context;

import com.blinq.R;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Built over the libPhoneNumber library to parse the phone numbers.
 *
 * @author Johan Hansson.
 */
public class HeadboxPhoneUtils {

    private static HeadboxPhoneUtils instance;
    private Map<String, String> prefixByCountry;

    public static HeadboxPhoneUtils getInstance(Context context) {
        if (instance == null) {
            instance = new HeadboxPhoneUtils(context);
        }
        return instance;
    }

    private HeadboxPhoneUtils(Context context) {
        prefixByCountry = new HashMap<String, String>();
        String[] countiesCodes = context.getResources().getStringArray(R.array.counties_codes);
        for (String countryCode : countiesCodes) {
            String[] prefixAndName = countryCode.split(",");
            prefixByCountry.put(prefixAndName[1], prefixAndName[0]);
        }
    }

    public static String getPhoneNumber(String number) {

        String strippedPhoneNumber = number.replaceAll("\\D", "");
        if (Pattern.matches("\\d+", strippedPhoneNumber)) {
            return strippedPhoneNumber.replaceFirst("^0", "");
        }
        return number;
    }

    public static boolean isPhoneNumber(String value) {
        return Pattern.matches("[\\d\\s\\-\\+\\(\\)]+", value);
    }

    public String getCountryPrefix(String country) {
        return prefixByCountry.get(country);
    }

}
