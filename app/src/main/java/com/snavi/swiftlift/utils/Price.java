package com.snavi.swiftlift.utils;

import com.snavi.swiftlift.database_objects.Const;

import java.util.Currency;
import java.util.HashMap;
import java.util.Locale;

public class Price {
    // TODO test too big fractional part

    // CONST //////////////////////////////////////////////////////////////////////////////////////
    public static final Locale DEFAULT_LOCALE = Locale.US;
    public static final int MAX_FRACTIONAL_PART = 99;
    public static final String[] CURRENCIES = {"LUL", "NZD", "BEC", "CUC", "RUR", "BGN", "MOP", "BMD", "DJF", "CYP", "AOK", "TRY", "DOP", "FIM", "INR", "BTN", "XAG", "MZM", "TRL", "FKP", "MDL", "BOL", "XAF", "SIT", "PTE", "ZRZ", "SKK", "NGN", "ISJ", "KHR", "SRD", "GQE", "TZS", "GRD", "BAD", "ALL", "LSL", "CHW", "JOD", "BAN", "BGM", "YUD", "XPD", "ETB", "BOV", "RUB", "SBD", "MAF", "XBC", "GHS", "AZM", "SOS", "LUF", "ZMK", "USD", "NAD", "CSD", "UYU", "SHP", "ADP", "MUR", "GYD", "XTS", "TJR", "BBD", "KPW", "HRK", "BHD", "JPY", "LAK", "UGX", "KYD", "KRO", "MTL", "PGK", "SDP", "DDM", "UYI", "LBP", "NLG", "GBP", "CUP", "SCR", "GEK", "BRN", "ECV", "SUR", "AOA", "VEF", "LYD", "KZT", "MDC", "XXX", "CVE", "ILR", "CHF", "TPE", "MMK", "BRB", "WST", "DZD", "CLF", "IEP", "ZWR", "CRC", "BWP", "TWD", "UGS", "LRD", "SSP", "GNS", "ALK", "GTQ", "XPF", "LTL", "EUR", "ZWD", "AOR", "ESB", "XBD", "YUR", "GWE", "YER", "HUF", "GNF", "BIF", "MCF", "KGS", "MGF", "CAD", "NIC", "PYG", "XSU", "BAM", "AED", "XFU", "AFN", "LVL", "ZMW", "BRL", "VUV", "BEF", "AWG", "RHD", "SGD", "HRD", "MKD", "MXV", "SRG", "ROL", "VND", "COP", "KMF", "SEK", "NIO", "MZN", "PEI", "MAD", "XFO", "PES", "BDT", "PKR", "GWP", "COU", "CDF", "TMM", "MGA", "CHE", "TTD", "LUC", "ARM", "AZN", "SYP", "XRE", "LVR", "HNL", "PAB", "HKD", "MNT", "ESP", "BEL", "PEN", "NOK", "XPT", "USN", "SVC", "XEU", "XDR", "XBA", "BSD", "CLE", "PLN", "ARP", "XBB", "KRW", "BRZ", "ZRN", "CZK", "UAK", "TOP", "UYP", "CNX", "MWK", "XOF", "ZWL", "BZD", "SDG", "MZE", "ARA", "BOP", "BRE", "BGL", "JMD", "SLL", "GHC", "RON", "VNN", "ISK", "BYB", "OMR", "BOB", "ARL", "XAU", "TND", "BND", "USS", "ITL", "EEK", "ARS", "MXP", "XUA", "MKN", "KES", "CSK", "STD", "BUK", "BRR", "UZS", "CNY", "VEB", "AON", "FJD", "MLF", "MRO", "ZAR", "PHP", "SAR", "GMD", "IDR", "RSD", "AUD", "DEM", "ERN", "TJS", "DKK", "CLP", "PLZ", "ILP", "BRC", "XCD", "YUM", "EGP", "RWF", "QAR", "GIP", "KRH", "FRF", "AFA", "GEL", "ECS", "SDD", "MTP", "IQD", "TMT", "NPR", "MVR", "AMD", "ZAL", "YDD", "THB", "ATS", "BGO", "LKR", "ANG", "UAH", "KWD", "MYR", "YUN", "MXN", "HTG", "ILS", "IRR", "BYR", "SZL", "ESA", "LTT"};


    private int m_mainPart;           // if the price is 100.99, then mainPart is 100
    private int m_fractionalPart;     // if the price is 100.99, then fractionalPart is 99
    private Currency m_currency;
    private Locale m_locale;


    public Price(int mainPart, int fractionalPart, Currency currency)
    {
        initValueAndCurrency(mainPart, fractionalPart, currency);
        m_locale = DEFAULT_LOCALE;
    }



    public Price (int mainPart, int fractionalPart, String currencyCode)
    {
        Currency currency = Currency.getInstance(currencyCode);
        initValueAndCurrency(mainPart, fractionalPart, currency);
    }



    public Price(int mainPart, int fractionalPart, Currency currency, Locale locale)
    {
        initValueAndCurrency(mainPart, fractionalPart, currency);
        m_locale = locale;
    }



    private void initValueAndCurrency(int mainPart, int fractionalPart, Currency currency)
    {
        m_mainPart = 0;

        if (fractionalPart > MAX_FRACTIONAL_PART)
        {
            int mains = fractionalPart / (MAX_FRACTIONAL_PART + 1);
            m_mainPart += mains;
            fractionalPart = fractionalPart % (MAX_FRACTIONAL_PART + 1);
        }
        m_mainPart       += mainPart;
        m_fractionalPart = fractionalPart;
        m_currency       = currency;
    }



    // end of init ////////////////////////////////////////////////////////////////////////////////



    @Override
    public String toString()
    {
        return String.format(m_locale,
                "%d.%02d %s",
                m_mainPart,
                m_fractionalPart,
                m_currency.getCurrencyCode());
    }


    /**
     * @param price price which will be added
     * @return false if prices have different currencies, else true
     */
    public boolean add(Price price)
    {
        if (!m_currency.equals(price.getCurrency()))
            return false;

        int mainPart = price.getMainPart();
        int fracPart = price.getFractionalPart();

        m_mainPart += mainPart;
        m_fractionalPart += fracPart;

        if (m_fractionalPart > MAX_FRACTIONAL_PART)
        {
            m_mainPart += m_fractionalPart / (MAX_FRACTIONAL_PART + 1);
            m_fractionalPart = m_fractionalPart % (MAX_FRACTIONAL_PART + 1);
        }

        return true;
    }



    public HashMap<String, Object> getFirestoreObject()
    {
        HashMap<String, Object> res = new HashMap<>();
        res.put(Const.PRICE_MAIN_PART, m_mainPart);
        res.put(Const.PRICE_FRAC_PART, m_fractionalPart);
        res.put(Const.PRICE_CURRENCY_CODE, m_currency.getCurrencyCode());

        return res;
    }



    // getters & setters //////////////////////////////////////////////////////////////////////////



    public Price copy()
    {
        return new Price(m_mainPart, m_fractionalPart, m_currency, m_locale);
    }



    public int getMainPart()
    {
        return m_mainPart;
    }



    public void setMainPart(int mainPart)
    {
        m_mainPart = mainPart;
    }



    public int getFractionalPart()
    {
        return m_fractionalPart;
    }



    public void setFractionalPart(int fractionalPart)
    {
        m_fractionalPart = fractionalPart;
    }



    public Currency getCurrency()
    {
        return m_currency;
    }



    public void setCurrency(Currency currency)
    {
        m_currency = currency;
    }
}
