package com.snavi.swiftlift.lift;

import android.location.Address;
import android.os.Parcel;
import android.os.Parcelable;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.snavi.swiftlift.database_objects.Const;
import com.snavi.swiftlift.signed_in_fragments.DriverFragment;
import com.snavi.swiftlift.utils.Price;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Currency;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

// TODO when stretches are added/loaded change their currency to lift's currency
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
    @NonNull  protected ArrayList<Stretch> m_stretches;
    @NonNull  private Currency m_currency;
    @NonNull  private String m_id;
    @Nullable private String m_owner;



    // parcelable //////////////////////////////////////////////////////////////////////////////////



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
    Lift(Parcel inParcel)
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



    // init ////////////////////////////////////////////////////////////////////////////////////////



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
        res.put(Const.LIFT_DESCRIPTION, m_description);

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



    public void loadStretchesFromDb(final RecyclerView.Adapter adapter, final int liftPos)
    {
        FirebaseFirestore db                = FirebaseFirestore.getInstance();
        final CollectionReference stretchesCol    = db.collection(Const.STRETCHES_COLLECTION);

        stretchesCol.whereEqualTo(Const.STRETCH_LIFT_ID, m_id).get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots)
                    {
                        List<DocumentSnapshot> stretchesDocs = queryDocumentSnapshots.getDocuments();
                        ArrayList<Stretch> loadedStretches = new ArrayList<>(stretchesDocs.size());
                        for (DocumentSnapshot doc : stretchesDocs)
                        {
                            loadedStretches.add(Stretch.loadFromDoc(doc, m_currency, doc.getId()));
                        }
                        addStretchesChronologically(loadedStretches);

                        if (adapter instanceof DriverFragment.LiftsAdapter)
                            ((DriverFragment.LiftsAdapter) adapter).sort();     // adapter notifies about changes itself
                        else
                            adapter.notifyItemChanged(liftPos);
                    }
                });
    }



    private void addStretchesChronologically(ArrayList<Stretch> stretches)
    {
        if (stretches.isEmpty())
            return;

        m_stretches.add(stretches.get(0));
        Stretch foundStretch;
        boolean isLast;

        for (int foundStretchIdx = 1; foundStretchIdx < stretches.size(); foundStretchIdx++)
        {
            foundStretch = stretches.get(foundStretchIdx);
            isLast = true;

            for (int i = 0; i < m_stretches.size(); i++)
            {
                if (foundStretch.getDepDate().before(m_stretches.get(i).getDepDate()))
                {
                    m_stretches.add(i, foundStretch);
                    isLast = false;
                    break;
                }
            }

            if (isLast)
                m_stretches.add(foundStretch);
        }
    }



    public void updateInDb()
    {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection(Const.LIFTS_COLLECTION).document(m_id).set(getFirestoreObject());
    }



    // getters && setters /////////////////////////////////////////////////////////////////////////



    @Nullable
    public LatLng getLastStretchArrCoords()
    {
        if (m_stretches.isEmpty())
            return null;

        return m_stretches.get(m_stretches.size() - 1).getCoordTo();
    }



    @Nullable
    public Address getLastStretchArrAddr()
    {
        if (m_stretches.isEmpty())
            return null;

        return m_stretches.get(m_stretches.size() - 1).getArrAddr();
    }



    @Nullable
    public Date getLastStretchArrDate()
    {
        if (m_stretches.isEmpty())
            return null;

        return m_stretches.get(m_stretches.size() - 1).getArrDate();
    }



    public void addStretch(Stretch stretch)
    {
        m_stretches.add(stretch);
    }



    public void setStretches(@NonNull ArrayList<Stretch> stretches)
    {
        m_stretches = stretches;
    }


    @NonNull
    public ArrayList<Stretch> getStretches()
    {
        return m_stretches;
    }



    @NonNull
    public String getFrom()
    {
        if (m_stretches.isEmpty())
            return "";
        else
            return m_stretches.get(0).getDepAddrLine();
    }



    @NonNull
    public String getTo()
    {
        if (m_stretches.isEmpty())
            return "";
        else
            return m_stretches.get(m_stretches.size() - 1).getArrAddrLine();
    }



    @NonNull
    public String getDepDateString()
    {
        if (m_stretches.isEmpty())
            return "";
        else
            return m_stretches.get(0).depDateDisplay(Locale.getDefault());
    }



    @NonNull
    public String getArrDateString()
    {
        if (m_stretches.isEmpty())
            return "";
        else
            return m_stretches.get(m_stretches.size() - 1).arrDateDisplay(Locale.getDefault());
    }



    @Nullable
    public Date getDepDate()
    {
        if (m_stretches.isEmpty())
            return null;

        return m_stretches.get(0).getDepDate();
    }



    @Nullable
    public Date getArrDate()
    {
        if (m_stretches.isEmpty())
            return null;

        return m_stretches.get(m_stretches.size() - 1).getArrDate();
    }



    @NonNull
    public String getPrice()
    {
        Price res = new Price(0, 0, m_currency);
        for (Stretch s : m_stretches)
            res.add(s.getPrice());

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



    public String getOwnerId()
    {
        return m_owner;
    }



    public void setOwnerId(String ownerId)
    {
        m_owner = ownerId;
    }




    // comparators /////////////////////////////////////////////////////////////////////////////////



    public static class DepDateAscending implements Comparator<Lift>
    {
        // CONST
        private static final int NULL_DATE = 100;

        @Override
        public int compare(Lift lift1, Lift lift2)
        {
            if (lift1.getDepDate() == null || lift2.getDepDate() == null)
                return NULL_DATE;
            else if (lift1.getDepDate().before(lift2.getDepDate()))
                return -1;
            else if (lift1.getDepDate().after(lift2.getDepDate()))
                return 1;
            else
                return 0;
        }
    }
}
