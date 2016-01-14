package com.blinq.ui.views;

import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.preference.ListPreference;
import android.text.TextUtils;
import android.util.AttributeSet;

import com.blinq.utils.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ListPreferenceMultiSelect extends ListPreference {
    private static final String separator = "=";
    private static final String TAG = "ListPreferenceMultiSelect";
    private boolean[] mClickedDialogEntryIndices;

    public ListPreferenceMultiSelect(Context context, AttributeSet attrs) {
        super(context, attrs);
        mClickedDialogEntryIndices = new boolean[getEntries().length];
    }

    @Override
    public void setEntries(CharSequence[] entries) {
        super.setEntries(entries);
        mClickedDialogEntryIndices = new boolean[entries.length];
    }

    public ListPreferenceMultiSelect(Context context) {
        this(context, null);
    }

    @Override
    protected void onPrepareDialogBuilder(Builder builder) {
        CharSequence[] entries = getEntries();
        CharSequence[] entryValues = getEntryValues();
        if (entries == null || entryValues == null || entries.length != entryValues.length) {
            throw new IllegalStateException(
                    "ListPreference requires an entries array and an entryValues array which are both the same length");
        }

        restoreCheckedEntries();
        builder.setMultiChoiceItems(entries, mClickedDialogEntryIndices,
                new DialogInterface.OnMultiChoiceClickListener() {
                    public void onClick(DialogInterface dialog, int which, boolean val) {
                        mClickedDialogEntryIndices[which] = val;
                    }
                });
    }

    public String[] parseStoredValue(CharSequence val) {
        return StringUtils.isBlank(val) ? null : val.toString().split(separator);
    }

    private void restoreCheckedEntries() {
        CharSequence[] entryValues = getEntryValues();
        String[] vals = parseStoredValue(getValue());
        if (vals != null) {
            List<String> valuesList = Arrays.asList(vals);
            for (int i = 0; i < entryValues.length; i++) {
                CharSequence entry = entryValues[i];
                if (valuesList.contains(entry)) {
                    mClickedDialogEntryIndices[i] = true;
                }
            }
        }
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        List<String> values = new ArrayList<String>();
        CharSequence[] entryValues = getEntryValues();
        if (positiveResult && entryValues != null) {
            for (int i = 0; i < entryValues.length; i++) {
                if (mClickedDialogEntryIndices[i] == true) {
                    values.add(entryValues[i].toString());
                }
            }
            if (callChangeListener(values)) {
                setValue(TextUtils.join(separator, values));
            }
        }

    }
}
