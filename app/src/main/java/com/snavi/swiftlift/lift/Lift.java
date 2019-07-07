package com.snavi.swiftlift.lift;

import com.snavi.swiftlift.utils.Price;

import java.util.ArrayList;
import java.util.Currency;
import java.util.Locale;

import androidx.annotation.NonNull;

public class Lift {

    @NonNull private ArrayList<Stretch> m_stretches;
    @NonNull private Currency m_currency;


    public Lift(@NonNull ArrayList<Stretch> stretches, @NonNull Currency currency)
    {
        m_stretches = stretches;
        m_currency = currency;
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
}
