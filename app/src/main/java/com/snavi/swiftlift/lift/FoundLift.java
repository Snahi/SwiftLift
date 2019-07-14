package com.snavi.swiftlift.lift;

import java.util.ArrayList;
import java.util.Currency;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Adds to standard lift information about start stretch and end stretch.
 */
public class FoundLift extends Lift {

    // CONST ///////////////////////////////////////////////////////////////////////////////////////
    // errors
    private static final String BAD_STRETCH_IDX_ERROR = "Fatal error. startStretchIdx or endStretchIdx is not valid (smaller than 0 or out of bounds of m_stretches.size()";

    // fields //////////////////////////////////////////////////////////////////////////////////////
    private int m_startStretchIdx;
    private int m_endStretchIdx;


    public FoundLift(@NonNull ArrayList<Stretch> stretches, @NonNull Currency currency,
                @NonNull String id, @Nullable String ownerId, int startStretchIdx, int endStretchIdx)
    {
        super(stretches, currency, id, ownerId);

        init(startStretchIdx, endStretchIdx);
    }



    public FoundLift(@NonNull Lift lift, int startStretchIdx, int endStretchIdx)
    {
        super(lift.getStretches(), lift.getCurrency(), lift.getId(), lift.getOwnerId());

        init(startStretchIdx, endStretchIdx);
    }



    private void init(int startStretchIdx, int endStretchIdx)
    {
        m_startStretchIdx = startStretchIdx;
        m_endStretchIdx   = endStretchIdx;

        if (!areStretchIdxesOk())
            throw new RuntimeException(BAD_STRETCH_IDX_ERROR);
    }



    private boolean areStretchIdxesOk()
    {
        return m_startStretchIdx < m_stretches.size() && m_endStretchIdx < m_stretches.size()
        && m_startStretchIdx >= 0 && m_endStretchIdx >= 0;
    }



    // getters & setters ///////////////////////////////////////////////////////////////////////////



    public Stretch getStartStretch()
    {
        return m_stretches.get(m_startStretchIdx);
    }

    public void setStartStretchIdx(int startStretchIdx)
    {
        this.m_startStretchIdx = startStretchIdx;
    }

    public Stretch getEndStretch()
    {
        return m_stretches.get(m_endStretchIdx);
    }

    public void setEndStretchIdx(int endStretchIdx)
    {
        this.m_endStretchIdx = endStretchIdx;
    }
}
