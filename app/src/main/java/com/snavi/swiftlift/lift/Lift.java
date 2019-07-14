package com.snavi.swiftlift.lift;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.snavi.swiftlift.database_objects.Const;
import com.snavi.swiftlift.utils.Price;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Currency;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

// TODO add owner to fields and contructors, load from database method
// TODO remove currency from strtches
public class Lift implements Parcelable {


    // CONST //////////////////////////////////////////////////////////////////////////////////////
    private static final String NULL_STRETCHES_ERROR = "Error during loading from parcelable." +
            "ArrayList<Stretch> was not passed.";
    private static final String NULL_CURRENCY_ERROR = "Error during loading from parcelable." +
            "Currency was not passed";
    private static final String NULL_ID_ERROR = "Error during loading from parcelable. Id was not" +
            "passed";
    private static final String NULL_OWNER_ID = "Error during loading from parcelable. Owner id " +
            "was not passed";


    // fields /////////////////////////////////////////////////////////////////////////////////////
    @NonNull private ArrayList<Stretch> m_stretches;
    @NonNull private Currency m_currency;
    @NonNull private String m_id;
    @Nullable private String m_owner;

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

        // owner
        String ownerId = inParcel.readString();
        if (ownerId == null) throw new RuntimeException(NULL_OWNER_ID);
        m_owner = ownerId;
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
        outParcel.writeString(m_owner);
    }



    public Lift(@NonNull ArrayList<Stretch> stretches, @NonNull Currency currency,
                @NonNull String id, @Nullable String ownerId)
    {
        m_stretches = stretches;
        m_currency = currency;
        m_id = id;
        m_owner = ownerId;
    }



    public HashMap<String, Object> getFirestoreObject()
    {
        HashMap<String, Object> res = new HashMap<>();
        res.put(Const.LIFT_OWNER, m_owner);
        res.put(Const.LIFT_CURRENCY, m_currency.getCurrencyCode());

        return res;
    }



    @Nullable
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

        return new Lift(new ArrayList<Stretch>(), currency, id, owner);
    }



    public void loadStretchesFromDb()
    {
        FirebaseFirestore db         = FirebaseFirestore.getInstance();
        CollectionReference stretchesCol = db.collection(Const.STRETCHES_COLLECTION);

        stretchesCol.whereEqualTo(Const.STRETCH_LIFT_ID, m_id).get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots)
                    {
                        List<DocumentSnapshot> stretchesDocs = queryDocumentSnapshots.getDocuments();
                        for (DocumentSnapshot doc : stretchesDocs)
                        {
                            m_stretches.add(Stretch.loadFromDoc(doc, m_currency));
                        }
                    }
                });
    }



    public void updateInDb()
    {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection(Const.LIFTS_COLLECTION).document(m_id).set(getFirestoreObject());
    }



    public void addStretch(Stretch stretch)
    {
        m_stretches.add(stretch);
    }



    public void setStretches(@NonNull ArrayList<Stretch> stretches)
    {
        m_stretches = stretches;
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
        Log.d("MY", "currency: " + m_currency.getCurrencyCode());
        Price res = new Price(0, 0, m_currency);
        for (Stretch s : m_stretches)
        {
            Log.d("MY", "stretch currency: " + s.getPrice().getCurrency().getCurrencyCode());
            Log.d("MY", "" + res.add(s.getPrice()));
        }

        return res.toString();
    }



    @NonNull
    public Currency getCurrency()
    {
        return m_currency;
    }



    public void setCurrency(@NonNull Currency currency)
    {
        m_currency = currency;
    }



    public String getCurrencyCode()
    {
        return m_currency.getCurrencyCode();
    }



    public String getId()
    {
        return m_id;
    }


}
