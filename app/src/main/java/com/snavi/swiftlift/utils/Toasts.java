package com.snavi.swiftlift.utils;

import android.content.Context;
import android.widget.Toast;

import com.snavi.swiftlift.R;

public class Toasts {

    public static void showNetworkErrorToast(Context context)
    {
        Toast.makeText(context, context.getResources()
                .getString(R.string.internet_connection_error), Toast.LENGTH_LONG).show();
    }



    public static void showUnknownErrorToast(Context context)
    {
        Toast.makeText(context, context.getResources()
                .getString(R.string.unknown_error), Toast.LENGTH_LONG).show();
    }



    public static void showUserSignedOutToast(Context context)
    {
        Toast.makeText(context, context.getResources()
                .getString(R.string.auth_error), Toast.LENGTH_LONG).show();
    }



    public static void showLiftLoadErrorToast(Context context)
    {
        Toast.makeText(context, context.getResources()
                .getString(R.string.error_during_loading_lift), Toast.LENGTH_LONG).show();
    }



    public static void showStretchDeleteErrorToast(Context context)
    {
        Toast.makeText(context, context.getResources()
                .getString(R.string.stretch_delete_failure), Toast.LENGTH_LONG).show();
    }



    public static void showLiftsLoadErrorToast(Context context)
    {
        Toast.makeText(context, context.getResources()
                .getString(R.string.lifts_load_error), Toast.LENGTH_LONG).show();
    }



    public static void showInvalidDataToast(Context context)
    {
        Toast.makeText(context, context.getResources()
                .getString(R.string.invalid_data), Toast.LENGTH_LONG).show();
    }
}
