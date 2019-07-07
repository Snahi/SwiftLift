package com.snavi.swiftlift.database_objects;

public interface Const {

    // user ////////////////////////////////////////////////////////////////////////////////////////
    String USERS_COLLECTION     = "users";
    String USER_NAME            = "name";
    String USER_SURNAME         = "surname";
    String USER_PHONE           = "phone";
    String USER_PHONE_VERIFIED  = "is_phone_verified";

    // lifts ///////////////////////////////////////////////////////////////////////////////////////
    String LIFTS_COLLECTION     = "lifts";
    String LIFT_OWNER           = "owner";
    String STRETCHES_COLLECTION = "stretches";
    String STRETCH_LIFT_ID      = "liftId";
    String STRETCH_FROM_LOC     = "coordFrom";
    String STRETCH_TO_LOC       = "coordTo";
    String STRETCH_FROM_ADDR    = "addrFrom";
    String STRETCH_TO_ADDR      = "addrTo";
    String STRETCH_DEP          = "depDate";
    String STRETCH_ARR          = "arrDate";
    String STRETCH_PRICE        = "price";

    // price ///////////////////////////////////////////////////////////////////////////////////////
    String PRICE_MAIN_PART     = "mainPart";
    String PRICE_FRAC_PART     = "fractionalPart";
    String PRICE_CURRENCY_CODE = "currency";
}
