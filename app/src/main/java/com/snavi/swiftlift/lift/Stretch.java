package com.snavi.swiftlift.lift;

import android.util.Log;

import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.snavi.swiftlift.database_objects.Const;
import com.snavi.swiftlift.utils.Price;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
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

    // CONST ///////////////////////////////////////////////////////////////////////////////////////
    private static final String NULL_LATITUDE_OR_LONGITUDE = "Null latitude or longitude";

    // fields //////////////////////////////////////////////////////////////////////////////////////
    private String m_id;
    private String m_liftId;
    private String m_addrFrom;
    private String m_addrTo;
    private Price  m_price;
    private Date   m_depDate;
    private Date   m_arrDate;
    private transient LatLng m_coordFrom;   // transient means skip it during defaultWriteObject()/defaultReadObject()
    private transient LatLng m_coordTo;     // transient means skip it during defaultWriteObject()/defaultReadObject()


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



    private Stretch(LatLng coordFrom, LatLng coordTo, String addrFrom, String addrTo,
            Date depDate, Date arrDate, Price price, String liftId, String id)
    {
        this.m_id        = id;
        this.m_coordFrom = coordFrom;
        this.m_coordTo   = coordTo;
        this.m_addrFrom  = addrFrom;
        this.m_addrTo    = addrTo;
        this.m_depDate   = depDate;
        this.m_arrDate   = arrDate;
        this.m_price     = price;
        this.m_liftId    = liftId;
    }



    // serialization //////////////////////////////////////////////////////////////////////////////



    private void writeObject(ObjectOutputStream out) throws IOException
    {
        out.defaultWriteObject();
        out.writeDouble(m_coordFrom.latitude);
        out.writeDouble(m_coordFrom.longitude);
        out.writeDouble(m_coordTo.latitude);
        out.writeDouble(m_coordTo.longitude);
    }



    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException
    {
        in.defaultReadObject();
        m_coordFrom = new LatLng(in.readDouble(), in.readDouble());
        m_coordTo   = new LatLng(in.readDouble(), in.readDouble());
    }



    // end of serialization ///////////////////////////////////////////////////////////////////////



    static Stretch loadFromDoc(@NonNull DocumentSnapshot doc, Currency currency, String id)
    {
        LatLng locFrom  = getLoc(doc, Const.STRETCH_FROM_LAT, Const.STRETCH_FROM_LON);
        LatLng locTo    = getLoc(doc, Const.STRETCH_TO_LAT, Const.STRETCH_TO_LON);
        String addrFrom = getString(doc, Const.STRETCH_FROM_ADDR);
        String addrTo   = getString(doc, Const.STRETCH_TO_ADDR);
        Date depDate    = getDate(doc, Const.STRETCH_DEP);
        Date arrDate    = getDate(doc, Const.STRETCH_ARR);
        Price price     = getPrice(doc, currency);
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
                depDate == null || arrDate == null || liftId == null)
            return null;
        else
            return new Stretch(locFrom, locTo, addrFrom, addrTo, depDate, arrDate, price, liftId,
                    id);
    }



    private static LatLng getLoc(@NonNull DocumentSnapshot doc, @NonNull String latKey,
                                 @NonNull String lonKey)
    {
        Double latitude  = doc.getDouble(latKey);
        Double longitude = doc.getDouble(lonKey);

        if (latitude == null || longitude == null)
            throw new RuntimeException(NULL_LATITUDE_OR_LONGITUDE);

        return new LatLng(latitude, longitude);
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



    private static Price getPrice(DocumentSnapshot doc, Currency currency)
    {
        Object mainPartObj = doc.get(Const.STRETCH_PRICE_MAIN);
        Object fracPartObj = doc.get(Const.STRETCH_PRICE_FRAC);

        int mainPart;
        int fracPart;
        try
        {
            mainPart = mainPartObj == null ? 0 : ((Long) mainPartObj).intValue();
            fracPart = fracPartObj == null ? 0 : ((Long) fracPartObj).intValue();
        }
        catch (NumberFormatException e)
        {
            mainPart = 0;
            fracPart = 0;
        }

        return new Price(mainPart, fracPart, currency);
    }



    public HashMap<String, Object> getFirestoreObject()
    {
        HashMap<String, Object> res = new HashMap<>();
        res.put(Const.STRETCH_ARR, m_arrDate);
        res.put(Const.STRETCH_DEP, m_depDate);
        res.put(Const.STRETCH_FROM_ADDR, m_addrFrom);
        res.put(Const.STRETCH_TO_ADDR, m_addrTo);
        res.put(Const.STRETCH_FROM_LAT, m_coordFrom.latitude);
        res.put(Const.STRETCH_FROM_LON, m_coordFrom.longitude);
        res.put(Const.STRETCH_TO_LAT, m_coordTo.latitude);
        res.put(Const.STRETCH_TO_LON, m_coordTo.longitude);
        res.put(Const.STRETCH_PRICE_MAIN, m_price.getMainPart());
        res.put(Const.STRETCH_PRICE_FRAC, m_price.getFractionalPart());
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

    public void setId(String id)
    {
        m_id = id;
    }

    public String getId()
    {
        return m_id;
    }

}
