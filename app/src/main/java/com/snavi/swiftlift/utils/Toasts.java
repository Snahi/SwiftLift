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



    public static void showGeocoderNotPresentToast(Context context)
    {
        Toast.makeText(context, context.getResources()
                .getString(R.string.geocoder_not_present), Toast.LENGTH_LONG).show();
    }



    public static void showGeocodeErrorToast(Context context)
    {
        Toast.makeText(context, context.getResources()
                .getString(R.string.geocoding_exception), Toast.LENGTH_LONG).show();
    }



    public static void showCantResolveLocationToast(Context context)
    {
        Toast.makeText(context, context.getResources()
                .getString(R.string.cant_resolve_location), Toast.LENGTH_LONG).show();
    }



    public static void showSearchErrorToast(Context context)
    {
        Toast.makeText(context, context.getResources()
                .getString(R.string.search_error), Toast.LENGTH_LONG).show();
    }



    public static void showSearchCompletedToast(Context context, int numOfFoundLifts)
    {
        String text = context.getString(R.string.search_completed) + " " + numOfFoundLifts;

        Toast.makeText(context, text, Toast.LENGTH_LONG).show();
    }



    public static void showAuthErrorToast(Context context)
    {
        Toast.makeText(context, R.string.auth_error, Toast.LENGTH_LONG).show();
    }



    public static void showReqSendErrorToast(Context context)
    {
        Toast.makeText(context, R.string.lift_req_send_error, Toast.LENGTH_LONG).show();
    }



    public static void showCantFindOwnerNameToast(Context context)
    {
        Toast.makeText(context, R.string.cant_find_owner_name, Toast.LENGTH_LONG).show();
    }



    public static void showCantFindOwnerSurnameToast(Context context)
    {
        Toast.makeText(context, R.string.cant_find_owner_surname, Toast.LENGTH_LONG).show();
    }



    public static void showCantFindOwnerData(Context context)
    {
        Toast.makeText(context, R.string.cant_find_owner_data, Toast.LENGTH_LONG).show();
    }



    public static void showPhotoUploadError(Context context)
    {
        Toast.makeText(context, R.string.photo_update_failure, Toast.LENGTH_LONG).show();
    }



    public static void showYouMustBeSignedIdToast(Context context)
    {
        Toast.makeText(context, R.string.you_must_be_signed_in, Toast.LENGTH_LONG).show();
    }
}
