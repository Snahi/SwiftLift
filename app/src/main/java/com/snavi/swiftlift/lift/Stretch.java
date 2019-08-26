package com.snavi.swiftlift.lift;

import android.location.Address;

import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.snavi.swiftlift.database_objects.Const;
import com.snavi.swiftlift.searching.CellCreator;
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

import androidx.annotation.NonNull;

// TODO address is not serializable
public class Stretch implements Serializable {

    // CONST ///////////////////////////////////////////////////////////////////////////////////////
    private static final String NULL_LATITUDE_OR_LONGITUDE = "Null latitude or longitude";

    // fields //////////////////////////////////////////////////////////////////////////////////////
    private String  m_id;
    private String  m_liftId;
    private transient Address m_depAddr;
    private transient Address m_arrAddr;
    private Price   m_price;
    private Date    m_depDate;
    private Date    m_arrDate;
    private long    m_fromCell;
    private long    m_toCell;


    Stretch(Address addrFrom, Address addrTo, Calendar depDate, Calendar arrDate, Price price,
            String liftId)
    {
        LatLng locFrom = new LatLng(addrFrom.getLatitude(), addrFrom.getLongitude());
        LatLng locTo   = new LatLng(addrTo.getLatitude(), addrTo.getLongitude());

        commonInit(locFrom, locTo);

        this.m_depAddr   = addrFrom;
        this.m_arrAddr   = addrTo;
        this.m_depDate   = new Date();
        this.m_depDate.setTime(depDate.getTimeInMillis());
        this.m_arrDate   = new Date();
        this.m_arrDate.setTime(arrDate.getTimeInMillis());
        this.m_price     = price;
        this.m_liftId    = liftId;
    }



    private Stretch(LatLng coordFrom,
                    LatLng coordTo,
                    String addrLineFrom,
                    String cityFrom,
                    String postCodeFrom,
                    String streetFrom,
                    String streetNumFrom,
                    String addrLineTo,
                    String cityTo,
                    String postCodeTo,
                    String streetTo,
                    String streetNumTo,
                    Date depDate,
                    Date arrDate,
                    Price price,
                    String liftId,
                    String id)
    {
        commonInit(coordFrom, coordTo);
        initFromAddr(coordFrom, addrLineFrom, cityFrom, postCodeFrom, streetFrom, streetNumFrom);
        initToAddr(coordTo, addrLineTo, cityTo, postCodeTo, streetTo, streetNumTo);

        this.m_id        = id;
        this.m_depDate   = depDate;
        this.m_arrDate   = arrDate;
        this.m_price     = price;
        this.m_liftId    = liftId;
    }



    private void commonInit(LatLng coordFrom, LatLng coordTo)
    {
        this.m_fromCell         = CellCreator.assignCell(coordFrom.latitude, coordFrom.longitude);
        this.m_toCell           = CellCreator.assignCell(coordTo.latitude, coordTo.longitude);
    }



    private void initFromAddr(LatLng coordFrom, String addrLineFrom, String cityFrom,
                              String postCodeFrom, String streetFrom, String streetNumFrom)
    {
        this.m_depAddr = createAddress(
                coordFrom.latitude,
                coordFrom.longitude,
                addrLineFrom,
                cityFrom,
                postCodeFrom,
                streetFrom,
                streetNumFrom
        );
    }



    private void initToAddr(LatLng coordTo, String addrLineTo, String cityTo, String postCodeTo,
                            String streetTo, String streetNumTo)
    {
        this.m_arrAddr = createAddress(
                coordTo.latitude,
                coordTo.longitude,
                addrLineTo,
                cityTo,
                postCodeTo,
                streetTo,
                streetNumTo
        );
    }



    // serialization //////////////////////////////////////////////////////////////////////////////



    private void writeObject(ObjectOutputStream out) throws IOException
    {
        out.defaultWriteObject();
        writeDepAddress(out);
        writeArrAddress(out);
    }



    private void writeDepAddress(ObjectOutputStream out) throws IOException
    {
        out.writeDouble(m_depAddr.getLatitude());
        out.writeDouble(m_depAddr.getLongitude());
        out.writeObject(m_depAddr.getAddressLine(0));
        out.writeObject(m_depAddr.getLocality());
        out.writeObject(m_depAddr.getPostalCode());
        out.writeObject(m_depAddr.getThoroughfare());
        out.writeObject(m_depAddr.getSubThoroughfare());
    }



    private void writeArrAddress(ObjectOutputStream out) throws IOException
    {
        out.writeDouble(m_arrAddr.getLatitude());
        out.writeDouble(m_arrAddr.getLongitude());
        out.writeObject(m_arrAddr.getAddressLine(0));
        out.writeObject(m_arrAddr.getLocality());
        out.writeObject(m_arrAddr.getPostalCode());
        out.writeObject(m_arrAddr.getThoroughfare());
        out.writeObject(m_arrAddr.getSubThoroughfare());
    }



    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException
    {
        in.defaultReadObject();
        m_depAddr = readAddress(in);
        m_arrAddr = readAddress(in);
    }



    private Address readAddress(ObjectInputStream in) throws IOException, ClassNotFoundException
    {
        return createAddress(
                in.readDouble(),
                in.readDouble(),
                (String) in.readObject(),
                (String) in.readObject(),
                (String) in.readObject(),
                (String) in.readObject(),
                (String) in.readObject()
        );
    }



    // end of serialization ///////////////////////////////////////////////////////////////////////



    private Address createAddress(double lat, double lon, String addressLine, String city,
                                  String postCode, String street, String houseNumber)
    {
        Address addr = new Address(Locale.getDefault());

        addr.setLatitude(lat);
        addr.setLongitude(lon);
        addr.setAddressLine(0, addressLine);
        addr.setLocality(city);
        addr.setPostalCode(postCode);
        addr.setThoroughfare(street);
        addr.setSubThoroughfare(houseNumber);

        return addr;
    }



    public static Stretch loadFromDoc(@NonNull DocumentSnapshot doc, Currency currency, String id)
    {
        LatLng locFrom          = getLoc(doc, Const.STRETCH_FROM_LAT, Const.STRETCH_FROM_LON);
        LatLng locTo            = getLoc(doc, Const.STRETCH_TO_LAT, Const.STRETCH_TO_LON);
        String addrFrom         = getString(doc, Const.STRETCH_FROM_ADDR);
        String cityFrom         = getString(doc, Const.STRETCH_FROM_CITY);
        String postCodeFrom     = getString(doc, Const.STRETCH_FROM_POST_CODE);
        String streetFrom       = getString(doc, Const.STRETCH_FROM_STREET);
        String streetNumFrom    = getString(doc, Const.STRETCH_FROM_STREET_NUM);
        String addrTo           = getString(doc, Const.STRETCH_TO_ADDR);
        String cityTo           = getString(doc, Const.STRETCH_TO_CITY);
        String postCodeTo       = getString(doc, Const.STRETCH_TO_POST_CODE);
        String streetTo         = getString(doc, Const.STRETCH_TO_STREET);
        String streetNumTo      = getString(doc, Const.STRETCH_TO_STREET_NUM);
        Date depDate            = getDate(doc, Const.STRETCH_DEP);
        Date arrDate            = getDate(doc, Const.STRETCH_ARR);
        Price price             = getPrice(doc, currency);
        String liftId           = getString(doc, Const.STRETCH_LIFT_ID);

        if (addrFrom == null
                || addrTo   == null
                || depDate  == null
                || arrDate  == null
                || liftId   == null
                || cityFrom == null
                || cityTo   == null)
            return null;
        else
            return new Stretch(locFrom, locTo, addrFrom, cityFrom, postCodeFrom, streetFrom,
                    streetNumFrom, addrTo, cityTo, postCodeTo, streetTo, streetNumTo, depDate,
                    arrDate, price, liftId, id);
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
        res.put(Const.STRETCH_FROM_ADDR, m_depAddr.getAddressLine(0));
        res.put(Const.STRETCH_FROM_CITY, m_depAddr.getLocality());
        res.put(Const.STRETCH_FROM_POST_CODE, m_depAddr.getPostalCode());
        res.put(Const.STRETCH_FROM_STREET, m_depAddr.getThoroughfare());
        res.put(Const.STRETCH_TO_ADDR, m_arrAddr.getAddressLine(0));
        res.put(Const.STRETCH_TO_CITY, m_arrAddr.getLocality());
        res.put(Const.STRETCH_TO_POST_CODE, m_arrAddr.getPostalCode());
        res.put(Const.STRETCH_TO_STREET, m_arrAddr.getThoroughfare());
        res.put(Const.STRETCH_FROM_CELL, m_fromCell);
        res.put(Const.STRETCH_TO_CELL, m_toCell);
        res.put(Const.STRETCH_FROM_LAT, m_depAddr.getLatitude());
        res.put(Const.STRETCH_FROM_LON, m_depAddr.getLongitude());
        res.put(Const.STRETCH_TO_LAT, m_arrAddr.getLatitude());
        res.put(Const.STRETCH_TO_LON, m_arrAddr.getLongitude());
        res.put(Const.STRETCH_PRICE_MAIN, m_price.getMainPart());
        res.put(Const.STRETCH_PRICE_FRAC, m_price.getFractionalPart());
        res.put(Const.STRETCH_LIFT_ID, m_liftId);

        String depStreetNum = m_depAddr.getSubThoroughfare();
        if (depStreetNum != null)
            res.put(Const.STRETCH_FROM_STREET_NUM, m_depAddr.getSubThoroughfare());
        else
            res.put(Const.STRETCH_FROM_STREET_NUM, "");

        String toStreetNum = m_arrAddr.getSubThoroughfare();
        if (toStreetNum != null)
            res.put(Const.STRETCH_TO_STREET_NUM, m_arrAddr.getSubThoroughfare());
        else
            res.put(Const.STRETCH_TO_STREET_NUM, "");

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

    public LatLng getCoordFrom()
    {
        return new LatLng(m_depAddr.getLatitude(), m_depAddr.getLongitude());
    }

    public LatLng getCoordTo()
    {
        return new LatLng(m_arrAddr.getLatitude(), m_arrAddr.getLongitude());
    }

    public String getDepAddrLine() {
        return m_depAddr.getAddressLine(0);
    }

    public String getArrAddrLine() {
        return m_arrAddr.getAddressLine(0);
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

    public String getCityFrom()
    {
        return m_depAddr.getLocality();
    }

    public String getCityTo()
    {
        return m_arrAddr.getLocality();
    }

    public String getPostCodeFrom()
    {
        return m_depAddr.getPostalCode();
    }

    public String getPostCodeTo()
    {
        return m_arrAddr.getPostalCode();
    }

    public String getStreetFrom()
    {
        return m_depAddr.getThoroughfare();
    }

    public String getStreetTo()
    {
        return m_arrAddr.getThoroughfare();
    }

    public String getStreetNumFrom()
    {
        return m_depAddr.getSubThoroughfare();
    }

    public String getStreetNumTo()
    {
        return m_arrAddr.getSubThoroughfare();
    }

    public Address getDepAddr()
    {
        return m_depAddr;
    }

    public Address getArrAddr()
    {
        return m_arrAddr;
    }

}
