package com.snavi.swiftlift.signed_in_fragments;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.text.format.DateFormat;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TimePicker;

import com.google.android.gms.maps.model.LatLng;
import com.snavi.swiftlift.R;
import com.snavi.swiftlift.activities.FoundLiftsActivity;
import com.snavi.swiftlift.utils.InternetUtils;
import com.snavi.swiftlift.utils.Toasts;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;


public class FindLiftFragment extends Fragment implements TimePickerDialog.OnTimeSetListener,
        DatePickerDialog.OnDateSetListener {


    // CONST ///////////////////////////////////////////////////////////////////////////////////////
    // errors
    private static final String NULL_CONTEXT_ERROR = "null context error";
    // formats
    private static final String DATE_FORMAT = "dd/MM/YYYY";
    private static final String TIME_FORMAT = "HH:mm";
    // other
    private static final String   TAG = FindLiftFragment.class.getName();


    // fields //////////////////////////////////////////////////////////////////////////////////////
    private Calendar m_departure;
    private Calendar m_latestDeparture;
    // view
    private EditText    m_etFrom;
    private EditText    m_etFromDistRange;
    private EditText    m_etTo;
    private EditText    m_etToDistRange;
    private EditText    m_etDepartureDate;
    private EditText    m_etDepartureTime;
    private EditText    m_etLatestDepDate;
    private EditText    m_etLatestDepTime;
    private ImageButton m_butSearch;


    public FindLiftFragment()
    {
        // Required empty public constructor
    }



    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        m_departure         = Calendar.getInstance();
        m_latestDeparture   = Calendar.getInstance();
    }



    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view =  inflater.inflate(R.layout.fragment_find_lift, container, false);

        initViews(view);
        setDepDateEtListener();
        setDepTimeEtListener();
        setLatestDepDateEtListener();
        setLatestDepTimeEtListener();
        setSearchButtonListener();

        return view;
    }



    private void initViews(View view)
    {
        m_etFrom          = view.findViewById(R.id.fragment_find_lift_et_from);
        m_etFromDistRange = view.findViewById(R.id.fragment_find_lift_et_dist_from);
        m_etTo            = view.findViewById(R.id.fragment_find_lift_et_to);
        m_etToDistRange   = view.findViewById(R.id.fragment_find_lift_et_dist_to);
        m_etDepartureDate = view.findViewById(R.id.fragment_find_lift_et_earliest_dep_date);
        m_etDepartureTime = view.findViewById(R.id.fragment_find_lift_et_earliest_dep_time);
        m_etLatestDepDate = view.findViewById(R.id.fragment_find_lift_et_latest_dep_date);
        m_etLatestDepTime = view.findViewById(R.id.fragment_find_lift_et_latest_dep_time);
        m_butSearch       = view.findViewById(R.id.fragment_find_lift_but_search);
    }



    // listeners ///////////////////////////////////////////////////////////////////////////////////



    private void setSearchButtonListener()
    {
        m_butSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                Context context = getContext();
                if (context == null)
                {
                    Log.e(TAG, NULL_CONTEXT_ERROR);
                    return;
                }
                if (!validateInputs())
                {
                    Toasts.showInvalidDataToast(context);
                    return;
                }
                if (!InternetUtils.hasInternetConnection(getContext()))
                {
                    Toasts.showNetworkErrorToast(context);
                    return;
                }

                LatLng from       = getCoordinates(m_etFrom.getText().toString());
                if (from == null)
                {
                    Toasts.showCantResolveLocationToast(getContext());
                    return;                                                                         // message already shown in getCoordinates
                }
                int fromDistRange = Integer.parseInt(m_etFromDistRange.getText().toString());
                LatLng to         = getCoordinates(m_etTo.getText().toString());
                if (to == null)
                {
                    Toasts.showCantResolveLocationToast(getContext());
                    return;                                                                         // message already shown in getCoordinates
                }
                int toDistRange   = Integer.parseInt(m_etToDistRange.getText().toString());

                goToFoundLiftsActivity(from, fromDistRange, to, toDistRange);
            }
        });
    }



    private void setDepDateEtListener()
    {
        m_etDepartureDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                if (getContext() == null)
                    Log.e(TAG, NULL_CONTEXT_ERROR);

                Calendar currCal = Calendar.getInstance();
                new DatePickerDialog(
                        getContext(),
                        getDatePickerDialogListener(m_departure, m_etDepartureDate),
                        currCal.get(Calendar.YEAR), currCal.get(Calendar.MONTH),
                        currCal.get(Calendar.DAY_OF_MONTH)).show();
            }
        });
    }



    private void setDepTimeEtListener()
    {
        m_etDepartureTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                if (getContext() == null)
                    Log.e(TAG, NULL_CONTEXT_ERROR);

                new TimePickerDialog(
                        getContext(),
                        getTimePickerDialogListener(m_departure, m_etDepartureTime),
                        m_departure.get(Calendar.HOUR),
                        m_departure.get(Calendar.MINUTE),
                        true).show();
            }
        });
    }



    private void setLatestDepDateEtListener()
    {
        m_etLatestDepDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                if (getContext() == null)
                    Log.e(TAG, NULL_CONTEXT_ERROR);

                new DatePickerDialog(
                        getContext(),
                        getDatePickerDialogListener(m_latestDeparture, m_etLatestDepDate),
                        m_departure.get(Calendar.YEAR), m_departure.get(Calendar.MONTH),
                        m_departure.get(Calendar.DAY_OF_MONTH)).show();
            }
        });
    }



    private void setLatestDepTimeEtListener()
    {
        m_etLatestDepTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                if (getContext() == null)
                    Log.e(TAG, NULL_CONTEXT_ERROR);

                new TimePickerDialog(getContext(),
                        getTimePickerDialogListener(m_latestDeparture, m_etLatestDepTime),
                        m_departure.get(Calendar.HOUR),
                        m_departure.get(Calendar.MINUTE),
                        true).show();
            }
        });
    }



    private DatePickerDialog.OnDateSetListener getDatePickerDialogListener(final Calendar calendar,
                                                                           final EditText et)
    {
        return new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker datePicker, int year, int month, int day)
            {
                calendar.set(Calendar.YEAR, year);
                calendar.set(Calendar.MONTH, month);
                calendar.set(Calendar.DAY_OF_MONTH, day);

                SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT, Locale.getDefault());
                et.setText(sdf.format(calendar.getTime()));
            }
        };
    }



    private TimePickerDialog.OnTimeSetListener getTimePickerDialogListener(final Calendar calendar,
                                                                           final EditText et)
    {
        return new TimePickerDialog.OnTimeSetListener() {
            @Override
            public void onTimeSet(TimePicker timePicker, int hour, int minute)
            {
                calendar.set(Calendar.HOUR_OF_DAY, hour);
                calendar.set(Calendar.MINUTE, minute);

                SimpleDateFormat sdf = new SimpleDateFormat(TIME_FORMAT, Locale.getDefault());
                et.setText(sdf.format(calendar.getTime()));
            }
        };
    }



    // validation //////////////////////////////////////////////////////////////////////////////////



    private boolean validateInputs()
    {
        Context context = getContext();
        if (context == null)
            return false;

        boolean isValid = true;

        if (m_etFrom.getText().toString().isEmpty())
        {
            isValid = false;
            m_etFrom.setError(context.getString(R.string.empty_field_error));
        }
        else
            m_etFrom.setError(null);

        if (m_etFromDistRange.getText().toString().isEmpty())
        {
            isValid = false;
            m_etFromDistRange.setError(context.getString(R.string.empty_field_error));
        }
        else
            m_etFromDistRange.setError(null);

        if (m_etTo.getText().toString().isEmpty())
        {
            isValid = false;
            m_etTo.setError(context.getString(R.string.empty_field_error));
        }
        else
            m_etTo.setError(null);

        if (m_etToDistRange.getText().toString().isEmpty())
        {
            isValid = false;
            m_etToDistRange.setError(context.getString(R.string.empty_field_error));
        }
        else
            m_etToDistRange.setError(null);

        if (m_etDepartureDate.getText().toString().isEmpty())
        {
            isValid = false;
            m_etDepartureDate.setError(context.getString(R.string.empty_field_error));
        }
        else
            m_etDepartureDate.setError(null);

        if (m_etDepartureTime.getText().toString().isEmpty())
        {
            isValid = false;
            m_etDepartureTime.setError(context.getString(R.string.empty_field_error));
        }
        else
            m_etDepartureTime.setError(null);

        return isValid;
    }



    // geocoding ///////////////////////////////////////////////////////////////////////////////////



    private LatLng getCoordinates(String address)
    {
        if (Geocoder.isPresent())
        {
            Geocoder geocoder = new Geocoder(getContext(), Locale.getDefault());
            try
            {
                List<Address> locations = geocoder.getFromLocationName(address, 1);
                if (locations == null || locations.isEmpty())
                {
                    Toasts.showCantResolveLocationToast(getContext());
                    return null;
                }

                Address location = locations.get(0);

                return new LatLng(location.getLatitude(), location.getLongitude());                 // proper result
            }
            catch (IOException e)
            {
                Toasts.showGeocodeErrorToast(getContext());
            }
        }
        else
            Toasts.showGeocoderNotPresentToast(getContext());

        return null;
    }



    // Searching ///////////////////////////////////////////////////////////////////////////////////



    private void goToFoundLiftsActivity(@NonNull LatLng from, int fromRange, @NonNull LatLng to,
                                        int toRange)
    {
        Intent intent = new Intent(getContext(), FoundLiftsActivity.class);
        intent.putExtra(FoundLiftsActivity.FROM_KEY, from);
        intent.putExtra(FoundLiftsActivity.FROM_RANGE_KEY, fromRange);
        intent.putExtra(FoundLiftsActivity.TO_KEY, to);
        intent.putExtra(FoundLiftsActivity.TO_RANGE_KEY, toRange);
        intent.putExtra(FoundLiftsActivity.DEPARTURE_KEY, m_departure.getTime());

        if (!m_etLatestDepTime.getText().toString().isEmpty()
        && !m_etLatestDepDate.getText().toString().isEmpty())
        {
            intent.putExtra(FoundLiftsActivity.MAX_DATE_KEY, m_latestDeparture.getTime());
        }

        startActivity(intent);
    }



    // DatePickerDialog.OnDateSetListener //////////////////////////////////////////////////////////


    @Override
    public void onDateSet(DatePicker datePicker, int year, int month, int day)
    {
        m_departure.set(Calendar.YEAR, year);
        m_departure.set(Calendar.MONTH, month);
        m_departure.set(Calendar.DAY_OF_MONTH, day);

        SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT, Locale.getDefault());
        m_etDepartureDate.setText(sdf.format(m_departure.getTime()));
    }



    // TimePickerDialog.OnTimeSetListener //////////////////////////////////////////////////////////



    @Override
    public void onTimeSet(TimePicker timePicker, int hour, int minute)
    {

        m_departure.set(Calendar.HOUR_OF_DAY, hour);
        m_departure.set(Calendar.MINUTE, minute);

        SimpleDateFormat sdf = new SimpleDateFormat(TIME_FORMAT, Locale.getDefault());
        m_etDepartureTime.setText(sdf.format(m_departure.getTime()));
    }
}
