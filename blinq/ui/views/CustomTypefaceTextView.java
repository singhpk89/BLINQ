package com.blinq.ui.views;

import com.blinq.R;
import com.blinq.utils.UIUtils;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.widget.TextView;

/**
 * Custom TextView in order to add a custom typeface from XML layouts using a
 * new attribute <code>customTypeface</code> which take the path of the typeface
 * as <code>String</code>.
 *
 * @author Johan Hansson
 */
public class CustomTypefaceTextView extends TextView {

    public CustomTypefaceTextView(Context context) {
        super(context);
    }

    public CustomTypefaceTextView(Context context, AttributeSet attrs) {
        super(context, attrs);

        if (!isInEditMode()) {
            ApplyActionsForCustomAttributes(context, attrs);
        }
    }

    public CustomTypefaceTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    /**
     * Move over all attributes of the view and apply required actions for each
     * of them if needed.
     *
     * @param context context in which view is appear.
     * @param attrs   attributes set of the view.
     */
    private void ApplyActionsForCustomAttributes(Context context,
                                                 AttributeSet attrs) {

        TypedArray customAttributes = context.obtainStyledAttributes(attrs,
                R.styleable.TextViewWithCustomTypeface);

        for (int index = 0; index < customAttributes.getIndexCount(); index++) {

            int attributeId = customAttributes.getIndex(index);

            switch (attributeId) {

                // Custom typeface attribute
                case R.styleable.TextViewWithCustomTypeface_customTypeFace:

                    setCustomTypeface(context, customAttributes, attributeId);

                    break;

            }

        }

        customAttributes.recycle();
    }

    /**
     * Set the typeface of text view depends on it's name from attribute set.
     *
     * @param context        context in which view is appear.
     * @param viewAttributes attributes set of the view.
     * @param attributeId    id of the custom typeface attribute.
     */
    private void setCustomTypeface(Context context, TypedArray viewAttributes,
                                   int attributeId) {

        String typefacePath = viewAttributes.getString(attributeId);

        Typeface typeface = UIUtils.getFontTypeface(context, typefacePath);

        setTypeface(typeface);
    }

}
