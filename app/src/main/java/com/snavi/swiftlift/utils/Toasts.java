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
}