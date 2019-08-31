package com.snavi.swiftlift.utils;

import android.content.Context;
import android.view.View;

import com.google.android.material.snackbar.Snackbar;
import com.snavi.swiftlift.R;

public class Snackbars {

    public static void showUnsupportedLocSnackbar(Context context, View view)
    {
        Snackbar.make(view, context.getString(R.string.unsupported_localization),
                Snackbar.LENGTH_INDEFINITE).setAction(R.string.snackbar_ok,
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {}
                });
    }



    public static void showPasswordResetSuccessSnackbar(Context context, View view)
    {
        Snackbar.make(view, context.getString(R.string.password_reset_success),
                Snackbar.LENGTH_INDEFINITE).setAction(R.string.snackbar_ok,
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {}
                }).show();
    }



    public static void showPasswordResetFailureSnackbar(Context context, View view)
    {
        Snackbar.make(view, context.getString(R.string.password_reset_failure),
                Snackbar.LENGTH_INDEFINITE).setAction(R.string.snackbar_ok,
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {}
                }).show();
    }
}
