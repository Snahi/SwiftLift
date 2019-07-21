package com.snavi.swiftlift.lift;

import com.google.firebase.firestore.DocumentSnapshot;
import com.snavi.swiftlift.lift.Lift;
import com.snavi.swiftlift.lift.Stretch;

import java.util.ArrayList;
import java.util.Currency;
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
    private static final String BAD_STRETCH_IDX_ERROR = "Fatal error. startStretchIdx or endStretchIdx is not valid (smaller than 0 or out of bounds of m_stretches.size()";

    // fields //////////////////////////////////////////////////////////////////////////////////////
    private Stretch m_startStretch;
    private Stretch m_endStretch;


    public FoundLift(@NonNull ArrayList<Stretch> stretches, @NonNull Currency currency,
                     @NonNull String id, @Nullable String ownerId, Stretch startStretch,
                     Stretch endStretch)
    {
        super(stretches, currency, id, ownerId);

        init(startStretch, endStretch);
    }



    public FoundLift(@NonNull Lift lift, @NonNull Stretch startStretch, @NonNull Stretch endStretch)
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
        return m_startStretch.getAddrFrom();
    }



    @Override
    @NonNull
    public String getTo()
    {
        return m_endStretch.getAddrTo();
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
