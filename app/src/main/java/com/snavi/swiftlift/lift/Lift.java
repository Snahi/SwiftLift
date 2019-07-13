package com.snavi.swiftlift.lift;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.snavi.swiftlift.database_objects.Const;
import com.snavi.swiftlift.utils.Price;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Currency;
import java.util.HashMap;
import java.util.Locale;

import androidx.annotation.NonNull;
// TODO add owner to fields and contructors, load from database method
public class Lift implements Parcelable {


    // CONST //////////////////////////////////////////////////////////////////////////////////////
    private static final String NULL_STRETCHES_ERROR = "Error during loading from parcelable." +
            "ArrayList<Stretch> was not passed.";
    private static final String NULL_CURRENCY_ERROR = "Error during loading from parcelable." +
            "Currency was not passed";
    private static final String NULL_ID_ERROR = "Error during loading from parcelable. Id was not" +
            "passed";


    // fields /////////////////////////////////////////////////////////////////////////////////////
    @NonNull private ArrayList<Stretch> m_stretches;
    @NonNull private Currency m_currency;
    @NonNull private String m_id;
    @NonNull private String m_owner;

    // parcelable
    public static final Parcelable.Creator<Lift> CREATOR = new Parcelable.Creator<Lift>()
    {
        @Override
        public Lift createFromParcel(Parcel in) {
            return new Lift(in);
        }

        @Override
        public Lift[] newArray(int size) {
            return new Lift[size];
        }
    };



    @SuppressWarnings("unchecked")
    private Lift(Parcel inParcel)
    {
        // stretches
        ArrayList<Stretch> stretches = (ArrayList<Stretch>) inParcel.readSerializable();
        if (stretches == null) throw new RuntimeException(NULL_STRETCHES_ERROR);
        m_stretches = stretches;

        // currency
        Currency currency = (Currency) inParcel.readSerializable();
        if (currency == null) throw new RuntimeException(NULL_CURRENCY_ERROR);
        m_currency  = currency;

        // id
        String id = inParcel.readString();
        if (id == null) throw new RuntimeException(NULL_ID_ERROR);
        m_id = id;
    }



    @Override
    public int describeContents()
    {
        return 0;
    }



    @Override
    public void writeToParcel(Parcel outParcel, int flags)
    {
        outParcel.writeSerializable(m_stretches);
        outParcel.writeSerializable(m_currency);
        outParcel.writeString(m_id);
    }



    public Lift(@NonNull ArrayList<Stretch> stretches, @NonNull Currency currency,
                @NonNull String id)
    {
        m_stretches = stretches;
        m_currency = currency;
        m_id = id;
    }



    public HashMap<String, Object> getFirestoreObject()
    {
        HashMap<String, Object> res = new HashMap<>();
        res.put(Const.LIFT_OWNER, m_owner);
        res.put(Const.LIFT_CURRENCY, m_currency.getCurrencyCode());

        return res;
    }



    public static Lift loadFromDoc(DocumentSnapshot doc)
    {
        String id = doc.getId();

        Object pre = doc.get(Const.LIFT_OWNER);
        if (!(pre instanceof String))
            return null;
        String owner = (String) pre;

        pre = doc.get(Const.LIFT_CURRENCY);
        if (!(pre instanceof String))
            return null;

        Currency currency = Currency.getInstance((String) pre);

        return new Lift(new ArrayList<Stretch>(), currency, id);
    }



    public ArrayList<Stretch> getStretches()
    {
        return m_stretches;
    }



    public String getFrom()
    {
        if (m_stretches.isEmpty())
            return "";
        else
            return m_stretches.get(0).getAddrFrom();
    }



    public String getTo()
    {
        if (m_stretches.isEmpty())
            return "";
        else
            return m_stretches.get(m_stretches.size() - 1).getAddrTo();
    }



    public String getDepDate()
    {
        if (m_stretches.isEmpty())
            return "";
        else
            return m_stretches.get(0).depDateDisplay(Locale.getDefault());
    }



    public String getArrDate()
    {
        if (m_stretches.isEmpty())
            return "";
        else
            return m_stretches.get(m_stretches.size() - 1).arrDateDisplay(Locale.getDefault());
    }



    public String getPrice()
    {
        Price res = new Price(0, 0, m_currency);
        for (Stretch s : m_stretches)
        {
            res.add(s.getPrice());
        }

        return res.toString();
    }



    public String getId()
    {
        return m_id;
    }
}
