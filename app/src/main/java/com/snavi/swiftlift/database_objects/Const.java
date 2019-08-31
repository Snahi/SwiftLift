package com.snavi.swiftlift.database_objects;

public interface Const {

    // user ////////////////////////////////////////////////////////////////////////////////////////
    String USERS_COLLECTION     = "users";
    String USER_NAME            = "name";
    String USER_SURNAME         = "surname";
    String USER_PHONE           = "phone";
    String USER_PHONE_VERIFIED  = "is_phone_verified";
    String USER_EMAIL           = "email";

    // lifts ///////////////////////////////////////////////////////////////////////////////////////
    String LIFTS_COLLECTION             = "lifts";
    String LIFT_OWNER                   = "owner";
    String LIFT_CURRENCY                = "currency";
    String LIFT_DESCRIPTION             = "desc";
    String STRETCHES_COLLECTION         = "stretches";
    String STRETCH_LIFT_ID              = "liftId";
    String STRETCH_FROM_LAT             = "flat";
    String STRETCH_FROM_LON             = "flon";
    String STRETCH_TO_LAT               = "tlat";
    String STRETCH_TO_LON               = "tlon";
    String STRETCH_FROM_ADDR            = "addrFrom";
    String STRETCH_FROM_CITY            = "cityFrom";
    String STRETCH_FROM_POST_CODE       = "postCodeFrom";
    String STRETCH_FROM_STREET          = "streetFrom";
    String STRETCH_FROM_STREET_NUM      = "streetNumFrom";
    String STRETCH_TO_ADDR              = "addrTo";
    String STRETCH_TO_CITY              = "cityTo";
    String STRETCH_TO_POST_CODE         = "postCodeTo";
    String STRETCH_TO_STREET            = "streetTo";
    String STRETCH_TO_STREET_NUM        = "streetNumTo";
    String STRETCH_DEP                  = "depDate";
    String STRETCH_ARR                  = "arrDate";
    String STRETCH_PRICE_MAIN           = "price_main";
    String STRETCH_PRICE_FRAC           = "price_frac";
    String STRETCH_FROM_CELL            = "from_cell";
    String STRETCH_TO_CELL              = "to_cell";

    // price ///////////////////////////////////////////////////////////////////////////////////////
    String PRICE_MAIN_PART     = "mainPart";
    String PRICE_FRAC_PART     = "fractionalPart";
    String PRICE_CURRENCY_CODE = "currency";

    // firebase cloud messaging ////////////////////////////////////////////////////////////////////
    String FCM_TOKENS_COLLECTION = "tokens_collection";
    String FCM_TOKEN_OWNER       = "owner";
    String FCM_TOKEN             = "token";

    // lift requests ///////////////////////////////////////////////////////////////////////////////
//    String REQ_COLLECTION       = "req_collection";
//    String REQ_SENDER           = "sender";
//    String REQ_LIFT             = "lift";
//    String REQ_LIFT_OWNER_TOKEN = "lift_owner_token";
//    String REQ_FROM_STRETCH     = "from";
//    String REQ_TO_STRETCH       = "to";
}
