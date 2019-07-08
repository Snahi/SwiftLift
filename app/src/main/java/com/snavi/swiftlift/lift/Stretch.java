package com.snavi.swiftlift.lift;

import android.util.Log;

import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.snavi.swiftlift.database_objects.Const;
import com.snavi.swiftlift.utils.Price;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Currency;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import androidx.annotation.NonNull;

public class Stretch implements Serializable {

    private String m_liftId;
    private LatLng m_coordFrom;
    private LatLng m_coordTo;
    private String m_addrFrom;
    private String m_addrTo;
    private Date m_depDate;
    private Date m_arrDate;
    private Price m_price;


    Stretch(LatLng coordFrom, LatLng coordTo, String addrFrom, String addrTo,
                   Calendar depDate, Calendar arrDate, Price price, String liftId)
    {
        this.m_coordFrom = coordFrom;
        this.m_coordTo   = coordTo;
        this.m_addrFrom  = addrFrom;
        this.m_addrTo    = addrTo;
        this.m_depDate   = new Date();
        this.m_depDate.setTime(depDate.getTimeInMillis());
        this.m_arrDate   = new Date();
        this.m_arrDate.setTime(arrDate.getTimeInMillis());
        this.m_price     = price;
        this.m_liftId    = liftId;
    }



    Stretch(LatLng coordFrom, LatLng coordTo, String addrFrom, String addrTo,
            Date depDate, Date arrDate, Price price, String liftId)
    {
        this.m_coordFrom = coordFrom;
        this.m_coordTo   = coordTo;
        this.m_addrFrom  = addrFrom;
        this.m_addrTo    = addrTo;
        this.m_depDate   = depDate;
        this.m_arrDate   = arrDate;
        this.m_price     = price;
        this.m_liftId    = liftId;
    }



    public static Stretch loadFromDoc(@NonNull DocumentSnapshot doc)
    {
        LatLng locFrom  = getLoc(doc, Const.STRETCH_FROM_LOC);
        LatLng locTo    = getLoc(doc, Const.STRETCH_TO_LOC);
        String addrFrom = getString(doc, Const.STRETCH_FROM_ADDR);
        String addrTo   = getString(doc, Const.STRETCH_TO_ADDR);
        Date depDate    = getDate(doc, Const.STRETCH_DEP);
        Date arrDate    = getDate(doc, Const.STRETCH_ARR);
        Price price     = getPrice(doc, Const.STRETCH_PRICE);
        String liftId   = getString(doc, Const.STRETCH_LIFT_ID);

        Log.d("MY", "locFrom: " + locFrom +
                "\nlocTo: " + locTo +
                "\naddrFrom: " + addrFrom +
                "\naddrTo: " + addrTo +
                "\ndepDate: " + depDate +
                "\narrDate: " + arrDate +
                "\nprice: " + price +
                "\nliftId: " + liftId);
        if (locFrom == null || locTo == null || addrFrom == null || addrTo == null ||
                depDate == null || arrDate == null || price == null || liftId == null)
            return null;
        else
            return new Stretch(locFrom, locTo, addrFrom, addrTo, depDate, arrDate, price, liftId);
    }



    private static LatLng getLoc(@NonNull DocumentSnapshot doc, @NonNull String key)
    {
        Object preMap = doc.get(key);
        if (!(preMap instanceof Map))
            return null;

        Map map = (Map) preMap;

        if (!map.containsKey(Const.COORDINATE_LATITUDE) ||
                !map.containsKey(Const.COORDINATE_LONGITUDE))
            return null;

        Object latitude  = map.get(Const.COORDINATE_LATITUDE);
        Object longitude = map.get(Const.COORDINATE_LONGITUDE);

        if (latitude instanceof Double && longitude instanceof Double)
            return new LatLng((double) latitude, (double) longitude);
        else
            return null;
    }



    private static String getString(@NonNull DocumentSnapshot doc, @NonNull String key)
    {
        Object res = doc.get(key);
        if (res instanceof String)
            return (String) res;
        else
            return null;
    }



    private static Date getDate(DocumentSnapshot doc, String key)
    {
        Object res = doc.get(key);
        if (res instanceof Timestamp)
            return ((Timestamp) res).toDate();
        else
            return null;
    }



    private static Price getPrice(DocumentSnapshot doc, String key)
    {
        Object res = doc.get(key);
        if (!(res instanceof Map))
            return null;
        Log.d("MY", "is map");
        // check if all fields are present
        Map resMap = (Map) res;
        if (!(
                resMap.containsKey(Const.PRICE_MAIN_PART) &&
                resMap.containsKey(Const.PRICE_FRAC_PART) &&
                resMap.containsKey(Const.PRICE_CURRENCY_CODE)
        ))
            return null;
        Log.d("MY", "keys exists");
        Object mainPart = resMap.get(Const.PRICE_MAIN_PART);
        Object fracPart = resMap.get(Const.PRICE_FRAC_PART);
        Object currency = resMap.get(Const.PRICE_CURRENCY_CODE);

        // check if all fields has proper type
        if (!(
                mainPart instanceof Long &&
                fracPart instanceof Long &&
                currency instanceof String
                ))
            return null;
        Log.d("MY", "all ok");

        return new Price(((Long) mainPart).intValue(), ((Long) fracPart).intValue(), Currency.getInstance((String) currency));
    }



    public HashMap<String, Object> getFirestoreObject()
    {
        HashMap<String, Object> res = new HashMap<>();
        res.put(Const.STRETCH_ARR, m_arrDate);
        res.put(Const.STRETCH_DEP, m_depDate);
        res.put(Const.STRETCH_FROM_ADDR, m_addrFrom);
        res.put(Const.STRETCH_TO_ADDR, m_addrTo);
        res.put(Const.STRETCH_FROM_LOC, m_coordFrom);
        res.put(Const.STRETCH_TO_LOC, m_coordTo);
        res.put(Const.STRETCH_PRICE, m_price.getFirestoreObject());
        res.put(Const.STRETCH_LIFT_ID, m_liftId);

        return res;
    }



    private String dateDisplay(Date date, Locale locale)
    {
        SimpleDateFormat format = new SimpleDateFormat("dd-MM-YYYY HH:mm", locale);
        return format.format(date);
    }



    public String depDateDisplay(Locale locale)
    {
        return dateDisplay(m_depDate, locale);
    }



    public String arrDateDisplay(Locale locale)
    {
        return dateDisplay(m_arrDate, locale);
    }



    // getters & setters ///////////////////////////////////////////////////////////////////////////

    ////////////////////////////////////////////////////////////////////////////////////
    //    Very Important!     Very Important!     Very Important!     Very Important! //
    //    Very Important!     Very Important!     Very Important!     Very Important! //
    ////////////////////////////////////////////////////////////////////////////////////
    //////////// Under no circumstances change getters & setters name //////////////////
    ////////////////////////////////////////////////////////////////////////////////////
    // reason: the methods are providing name for fields in database, but they are    //
    // also available via Cont interface constants and those constants won't change   //
    // when one changes getters && setters names.                                     //
    ////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////

    public LatLng getCoordFrom() {
        return m_coordFrom;
    }

    public void setCoordFrom(LatLng m_coordFrom) {
        this.m_coordFrom = m_coordFrom;
    }

    public LatLng getCoordTo() {
        return m_coordTo;
    }

    public String getAddrFrom() {
        return m_addrFrom;
    }

    public void setAddrFrom(String m_cityFrom) {
        this.m_addrFrom = m_cityFrom;
    }

    public String getAddrTo() {
        return m_addrTo;
    }

    public void setAddrTo(String m_cityTo) {
        this.m_addrTo = m_cityTo;
    }

    public Date getDepDate() {
        return m_depDate;
    }

    public void setDepDate(Date depDate) {
        m_depDate = depDate;
    }

    public Date getArrDate()
    {
        return m_arrDate;
    }

    public void setArrDate(Date m_arrDate) {
        this.m_arrDate = m_arrDate;
    }

    public Price getPrice() {
        return m_price;
    }

    public void setPrice(Price m_price) {
        this.m_price = m_price;
    }

    public void setLiftId(String liftId)
    {
        this.m_liftId = liftId;
    }

    public String getLiftId()
    {
        return m_liftId;
    }

}
