package com.snavi.swiftlift.lift;

import android.util.Log;

import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.snavi.swiftlift.database_objects.Const;
import com.snavi.swiftlift.utils.Price;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

public class Stretch implements Serializable {

    private String m_liftId;
    private LatLng m_coordFrom;
    private LatLng m_coordTo;
    private String m_addrFrom;
    private String m_addrTo;
    private Date m_depDate;
    private Date m_arrDate;
    private Price m_price;



    public Stretch(DocumentSnapshot doc)
    {
        HashMap<String, Double> coordMap = (HashMap<String, Double>) doc.get(Const.STRETCH_FROM_LOC);
        this.m_coordFrom = new LatLng(coordMap.get("latitude"), coordMap.get("longitude"));
        coordMap = (HashMap<String, Double>) doc.get(Const.STRETCH_TO_LOC);
        this.m_coordTo   = new LatLng(coordMap.get("latitude"), coordMap.get("longitude"));
        this.m_addrFrom  = (String) doc.get(Const.STRETCH_FROM_ADDR);
        this.m_addrTo    = (String) doc.get(Const.STRETCH_TO_ADDR);
        this.m_depDate   = ((Timestamp) doc.get(Const.STRETCH_DEP)).toDate();
        this.m_arrDate   = ((Timestamp) doc.get(Const.STRETCH_ARR)).toDate();
        HashMap<String, Object> priceMap = ((HashMap<String, Object>) doc.get(Const.STRETCH_PRICE));
        Log.d("MY", "" + priceMap);
        long mainPart = (long) priceMap.get(Const.PRICE_MAIN_PART);
        long fracPart = (long) priceMap.get(Const.PRICE_FRAC_PART);
        this.m_price     = new Price((int) mainPart, (int) fracPart,
                (String) priceMap.get(Const.PRICE_CURRENCY_CODE));
    }


    Stretch(LatLng coordFrom, LatLng coordTo, String cityFrom, String cityTo,
                   Calendar depDate, Calendar arrDate, Price price)
    {
        this.m_coordFrom = coordFrom;
        this.m_coordTo   = coordTo;
        this.m_addrFrom  = cityFrom;
        this.m_addrTo    = cityTo;
        this.m_depDate   = new Date();
        this.m_depDate.setTime(depDate.getTimeInMillis());
        this.m_arrDate   = new Date();
        this.m_arrDate.setTime(arrDate.getTimeInMillis());
        this.m_price     = price;
    }



    Stretch(LatLng coordFrom, LatLng coordTo, String cityFrom, String cityTo,
            Date depDate, Date arrDate, Price price)
    {
        this.m_coordFrom = coordFrom;
        this.m_coordTo   = coordTo;
        this.m_addrFrom  = cityFrom;
        this.m_addrTo    = cityTo;
        this.m_depDate   = depDate;
        this.m_arrDate   = arrDate;
        this.m_price     = price;
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

        return res;
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
