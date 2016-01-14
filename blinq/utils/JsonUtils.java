package com.blinq.utils;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Provide list of utilities to deal with json objects and arrays.
 * <p/>
 * Created by Johan Hansson on 9/28/2014.
 */
public class JsonUtils {


    /**
     * Get a String value for a given child property.
     *
     * @param object - Json object to extract the value from.
     * @param parent - the parent object to get the child value from.
     * @param child  - the property to get the value for.
     * @return String value.
     */
    public static String getPropertyStringInsideProperty(JSONObject object,
                                                         String parent, String child) {
        if (object == null) {
            return StringUtils.EMPTY_STRING;
        }

        JSONObject jsonObject = null;
        try {
            jsonObject = (JSONObject) object.getJSONObject(parent);
        } catch (JSONException e) {
            return StringUtils.EMPTY_STRING;
        }
        if (jsonObject != null) {
            return getPropertyString(jsonObject, child);
        }
        return StringUtils.EMPTY_STRING;
    }

    /**
     * Get integer value for a given child property.
     *
     * @param object - Json object to extract the value from.
     * @param parent - the parent object to get the child value from.
     * @param child  - the property to get the value for.
     * @return Integer value.
     */
    public static Integer getPropertyIntegerInsideProperty(JSONObject object,
                                                           String parent, String child) {
        if (object == null) {
            return Integer.valueOf(0);
        }

        JSONObject jsonObject = null;
        try {
            jsonObject = (JSONObject) object.getJSONObject(parent);
        } catch (JSONException e) {
            return Integer.valueOf(0);
        }
        if (jsonObject != null) {
            return getPropertyInteger(jsonObject, child);
        }
        return Integer.valueOf(0);
    }

    /**
     * Get a double value for a given child property.
     *
     * @param object - Json object to extract the value from.
     * @param parent - the parent object to get the child value from.
     * @param child  - the property to get the value for.
     * @return Double value.
     */
    public static Double getPropertyDoubleInsideProperty(JSONObject object,
                                                         String parent, String child) {
        if (object == null) {
            return Double.valueOf(0);
        }

        JSONObject jsonObject = null;
        try {
            jsonObject = (JSONObject) object.getJSONObject(parent);
        } catch (JSONException e) {
            return Double.valueOf(0);
        }
        if (jsonObject != null) {
            return getPropertyDouble(jsonObject, child);
        }

        return Double.valueOf(0);
    }

    /**
     * Get integer value of a given property.
     *
     * @param object   - json object to extract the value from.
     * @param property - json property to get the value for.
     * @return - Integer object.
     */
    public static Integer getPropertyInteger(JSONObject object, String property) {

        if (object == null || object.isNull(property))
            return Integer.valueOf(0);

        Object value = null;
        try {
            value = object.get(property);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        if (value == null || value.equals(StringUtils.EMPTY_STRING)) {
            return Integer.valueOf(0);
        }

        return Integer.valueOf(String.valueOf(value));
    }

    /**
     * Get long value of a given property.
     *
     * @param object   - json object to extract the value from.
     * @param property - json property to get the value for.
     * @return - Long object.
     */
    public static Long getPropertyLong(JSONObject object, String property) {

        if (object == null || object.isNull(property))
            return Long.valueOf(0);

        Object value = null;
        try {
            value = object.get(property);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        if (value == null || value.equals(StringUtils.EMPTY_STRING)) {
            return Long.valueOf(0);
        }

        return Long.valueOf(String.valueOf(value));
    }

    /**
     * Get Double value of a given property.
     *
     * @param object   - json object to extract the value from.
     * @param property - json property to get the value for.
     * @return - Double object.
     */
    public static double getPropertyDouble(JSONObject object, String property) {

        if (object == null || object.isNull(property))
            return Double.valueOf(0);

        Object value = null;
        try {
            value = object.get(property);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        if (value == null || value.equals(StringUtils.EMPTY_STRING)) {
            return Double.valueOf(0);
        }

        return Double.valueOf(String.valueOf(value));
    }

    /**
     * Get String value of a given property.
     *
     * @param object   - json object to extract the value from.
     * @param property - json property to get the value for.
     * @return - String object.
     */
    public static String getPropertyString(JSONObject object, String property) {

        if (object == null || object.isNull(property))
            return StringUtils.EMPTY_STRING;

        Object value = null;
        try {
            value = object.get(property);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        if (value == null || value.equals(StringUtils.EMPTY_STRING)) {
            return String.valueOf(StringUtils.EMPTY_STRING);
        }

        return String.valueOf(value);
    }
}
