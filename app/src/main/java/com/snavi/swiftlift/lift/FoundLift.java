package com.snavi.swiftlift.lift;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.firebase.firestore.DocumentSnapshot;
import com.snavi.swiftlift.utils.Price;

import java.io.Serializable;
import java.util.Date;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

/**
 * Adds to standard lift information about start stretch and end stretch.
 */
public class FoundLift extends Lift {

    // CONST ///////////////////////////////////////////////////////////////////////////////////////
    // errors
    private static final String PARCLABLE_START_STRETCH_LOAD_ERROR  = "Error occurred during reading from parcelable. You must provide start stretch as Serializable";
    private static final String PARCLEABLE_END_STRETCH_LOAD_ERROR   = "Error occurred during reading from parcelable. You must provide end stretch as Serializable";


    // fields //////////////////////////////////////////////////////////////////////////////////////
    private Stretch m_startStretch;
    private Stretch m_endStretch;



    // Parcelable //////////////////////////////////////////////////////////////////////////////////



    public static final Parcelable.Creator<Lift> CREATOR = new Parcelable.Creator<Lift>()
    {
        @Override
        public FoundLift createFromParcel(Parcel in) {
            return new FoundLift(in);
        }

        @Override
        public FoundLift[] newArray(int size) {
            return new FoundLift[size];
        }
    };



    private FoundLift(Parcel in)
    {
        super(in);

        // m_startStretch
        Serializable pre = in.readSerializable();
        if (pre == null) throw new RuntimeException(PARCLABLE_START_STRETCH_LOAD_ERROR);
        m_startStretch = (Stretch) pre;

        // m_endStretch
        pre = in.readSerializable();
        if (pre == null)
            throw new RuntimeException(PARCLEABLE_END_STRETCH_LOAD_ERROR);
        m_endStretch = (Stretch) pre;
    }



    @Override
    public int describeContents()
    {
        return 0;
    }



    @Override
    public void writeToParcel(Parcel outParcel, int flags)
    {
        super.writeToParcel(outParcel, flags);

        outParcel.writeSerializable(m_startStretch);
        outParcel.writeSerializable(m_endStretch);
    }



    // init ////////////////////////////////////////////////////////////////////////////////////////


    public FoundLift(@NonNull ArrayList<Stretch> stretches, @NonNull Currency currency,
                     @NonNull String id, @Nullable String ownerId, Stretch startStretch,
                     Stretch endStretch)
    {
        super(stretches, currency, id, ownerId);

        init(startStretch, endStretch);
    }



    private FoundLift(@NonNull Lift lift, @NonNull Stretch startStretch, @NonNull Stretch endStretch)
    {
        super(lift.getStretches(), lift.getCurrency(), lift.getId(), lift.getOwnerId());

        init(startStretch, endStretch);
    }



    private void init(Stretch startStretch, Stretch endStretch)
    {
        m_startStretch = startStretch;
        m_endStretch   = endStretch;
    }



    // loading from database ///////////////////////////////////////////////////////////////////////



    @Nullable
    public static FoundLift loadFromDoc(DocumentSnapshot doc, RecyclerView.Adapter adapter,
                                        int positionInAdapter, Stretch startStretch,
                                        Stretch endStretch)
    {
        Lift lift = Lift.loadFromDoc(doc);
        if (lift == null)
            return null;

        FoundLift res = new FoundLift(lift, startStretch, endStretch);

        res.loadStretchesFromDb(adapter, positionInAdapter);

        return res;
    }



    // getters & setters ///////////////////////////////////////////////////////////////////////////



    public Stretch getStartStretch()
    {
        return m_startStretch;
    }

    public void setStartStretch(Stretch startStretch)
    {
        this.m_startStretch = startStretch;
    }

    public Stretch getEndStretch()
    {
        return m_endStretch;
    }

    public void setEndStretch(Stretch endStretch)
    {
        this.m_endStretch = endStretch;
    }

    @Override
    @NonNull
    public String getFrom()
    {
        return m_startStretch.getDepAddrLine();
    }



    @Override
    @NonNull
    public String getTo()
    {
        return m_endStretch.getArrAddrLine();
    }



    @NonNull
    public Date getDepDate()
    {
        return m_startStretch.getDepDate();
    }



    @NonNull
    public Date getArrDate()
    {
        return m_endStretch.getArrDate();
    }
}
