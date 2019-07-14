package com.snavi.swiftlift.utils;

import android.app.Activity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

public class KeyboardUtils {

    public static void hideSoftKeyboard(Activity activity)
    {
        InputMethodManager inputMethodManager =
                (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);

        View viewWithFocus = activity.getCurrentFocus();
        if (viewWithFocus == null)
            return;

        inputMethodManager.hideSoftInputFromWindow(
                viewWithFocus.getWindowToken(), 0);
    }
}
