package com.snavi.swiftlift.lift;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import java.util.Currency;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;
import com.google.android.gms.maps.model.LatLng;
import com.snavi.swiftlift.R;
import com.snavi.swiftlift.activities.LiftPointsPickActivity;
import com.snavi.swiftlift.utils.InternetUtils;
import com.snavi.swiftlift.utils.Price;
import com.snavi.swiftlift.utils.Toasts;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

// TODO bug, when adding next stretch map is not moved
// TODO can't click on edittext error because of focusable='false'
// TODO (not very important) add previously selected points on map
public class AddStretchDialogFragment extends DialogFragment {

    // CONST ///////////////////////////////////////////////////////////////////////////////////////
    // keys
    public static final String INIT_COORDINATES_KEY = "initial_coordinates";
    public static final String CURRENCY_KEY = "currency";
    public static final String DEP_COORDS_KEY = "depCoords";
    public static final String DEP_ADDR_KEY = "depAddr";
    public static final String DEP_DATE_KEY = "depDate";
    public static final String LIFT_ID_KEY = "l_id";
    // request codes
    private static final int FROM_COORDS_REQ_CODE = 7771;
    private static final int TO_COORDS_REQ_CODE   = 7772;
    // errors
    private static final String IOEXCEPTION_GEOCODING = "io exception occured during geocoding";
    private static final String NULL_CONTEXT_ERROR = "null context error";
    private static final String LIFT_BUNDLE_EXCEPTION = "You must pass bundle with lift id!";
    private static final String NO_CURRENCY_PASSED_EXCEPTION = "Currency wasn't passed in arguments. It's obligatory";
    // formats
    private static final String DATE_FORMAT = "dd/MM/YYYY";
    private static final String TIME_FORMAT = "HH:mm";
    //other
    private static final String TAG = AddStretchDialogFragment.class.getName();


    // fields //////////////////////////////////////////////////////////////////////////////////////
    private OnFragmentInteractionListener m_listener;
    private Currency m_currency;
    private LatLng   m_initCoords;
    private String   m_liftId;
    private Bundle   m_arguments;

    // views
    private ImageButton m_fromCoordsBut;
    private TextView    m_depAddrTV;
    private EditText    m_depDateET;
    private EditText    m_depTimeET;

    // result
    private Calendar m_depDate;
    private Calendar m_arrDate;
    private LatLng   m_depCoords;
    private LatLng   m_arrCoords;
    private String   m_depAddr;
    private String   m_arrAddr;
    private Price    m_price;



    public AddStretchDialogFragment() {
        // Required empty public constructor
    }



    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        m_depDate   = Calendar.getInstance();
        m_arrDate   = Calendar.getInstance();
        m_arguments = getNonNullArguments();

        setupLiftId();
        setupCurrency();
    }



    /**
     * @exception RuntimeException if programmer didn't passed arguments
     */
    private Bundle getNonNullArguments()
    {
        Bundle bun = getArguments();
        if (bun == null)
            throw new RuntimeException(LIFT_BUNDLE_EXCEPTION);

        return bun;
    }


    /**
     * @exception RuntimeException when lift id is not in arguments
     */
    private void setupLiftId()
    {
        m_liftId = m_arguments.getString(LIFT_ID_KEY);

        if (m_liftId == null)
            throw new RuntimeException(LIFT_BUNDLE_EXCEPTION);
    }


    /**
     * @exception RuntimeException when currency isn't in arguments
     */
    private void setupCurrency()
    {
        m_currency = (Currency) m_arguments.getSerializable(CURRENCY_KEY);
        if (m_currency == null)
            throw new RuntimeException(NO_CURRENCY_PASSED_EXCEPTION);
    }


    /**
     * method that sets up departure location and date if at least one Stretch was added previously.
     * Also blocks the data, so that user can't change it, because it could cause discontinuity in
     * stretches.
     */
    private void setupDerivedFromPreviousStretch()
    {
        setupDepLoc();
        setupDepDate();
    }



    private void setupDepLoc()
    {
        m_depCoords = m_arguments.getParcelable(DEP_COORDS_KEY);
        if (m_depCoords == null)
            return;

        m_depAddr = m_arguments.getString(DEP_ADDR_KEY);
        if (m_depAddr == null)
            return;

        m_depAddrTV.setText(m_depAddr);
        m_fromCoordsBut.setEnabled(false);
    }



    private void setupDepDate()
    {
        Date date = (Date) m_arguments.getSerializable(DEP_DATE_KEY);
        if (date == null)
            return;

        m_depDate.setTime(date);

        SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT, Locale.getDefault());
        m_depDateET.setText(sdf.format(date.getTime()));

        SimpleDateFormat sdfTime = new SimpleDateFormat(TIME_FORMAT, Locale.getDefault());
        m_depTimeET.setText(sdfTime.format(date.getTime()));

        View.OnClickListener emptyOnClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {}
        };

        m_depDateET.setOnClickListener(emptyOnClickListener);
        m_depTimeET.setOnClickListener(emptyOnClickListener);
    }



    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_add_stretch_dialog, container,
                false);

        initViews(view);
        lockEditTexts(view);
        setButtonsListeners(view);
        setDepDateEtListener();
        setDepTimeEtListener();
        setArrDateEtListener(view);
        setArrTimeEtListener(view);
        setupCurrency(view);
        setupDerivedFromPreviousStretch();

        return view;
    }



    private void initViews(View view)
    {
        m_depAddrTV     = view.findViewById(R.id.fragment_add_stretch_dialog_tv_from_addr);
        m_fromCoordsBut = view.findViewById(R.id.fragment_add_stretch_dialog_but_choose_dep_loc);
        m_depDateET     = view.findViewById(R.id.fragment_add_stretch_dialog_et_dep_date);
        m_depTimeET     = view.findViewById(R.id.fragment_add_stretch_dialog_et_dep_time);
    }



    private void lockEditTexts(View view)
    {
        m_depDateET.setInputType(InputType.TYPE_NULL);
        m_depTimeET.setInputType(InputType.TYPE_NULL);
        EditText arrDate = view.findViewById(R.id.fragment_add_stretch_dialog_et_arr_date);
        arrDate.setInputType(InputType.TYPE_NULL);
        EditText arrTime = view.findViewById(R.id.fragment_add_stretch_dialog_et_arr_time);
        arrTime.setInputType(InputType.TYPE_NULL);
    }



    @Override
    public void onResume()
    {
        super.onResume();

        // change width and height of this dialog
        Dialog dialog = getDialog();
        if (dialog == null)
            return;
        Window window = dialog.getWindow();
        if (window == null)
            return;

        WindowManager.LayoutParams params = window.getAttributes();
        params.width = ViewGroup.LayoutParams.MATCH_PARENT;
        params.height = ViewGroup.LayoutParams.MATCH_PARENT;
        window.setAttributes(params);
    }



    private void setButtonsListeners(View view)
    {
        setChooseDepLocButtonListener(view);
        setChooseArrLocButtonListener(view);
        setAddButtonListener(view);
    }



    private void setChooseDepLocButtonListener(View view)
    {
        ImageButton but = view.findViewById(R.id.fragment_add_stretch_dialog_but_choose_dep_loc);
        but.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(getContext(), LiftPointsPickActivity.class);
                i.putExtra(LiftPointsPickActivity.INITIAL_POSITION_COORDINATES_KEY, m_initCoords);
                startActivityForResult(i, FROM_COORDS_REQ_CODE);
            }
        });
    }



    private void setChooseArrLocButtonListener(View view)
    {
        ImageButton but = view.findViewById(R.id.fragment_add_stretch_dialog_but_choose_arr_loc);
        but.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(getContext(), LiftPointsPickActivity.class);
                i.putExtra(LiftPointsPickActivity.INITIAL_POSITION_COORDINATES_KEY, m_initCoords);
                startActivityForResult(i, TO_COORDS_REQ_CODE);
            }
        });
    }



    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode != Activity.RESULT_OK)
            return;

        switch (requestCode)
        {
            case FROM_COORDS_REQ_CODE : dealWithFromCoordsResult(data); return;
            case TO_COORDS_REQ_CODE   : dealWithToCoordsResult(data);
        }
    }



    private void dealWithFromCoordsResult(Intent data)
    {
        if (data == null)
            return;

        m_depCoords = data.getParcelableExtra(LiftPointsPickActivity.RESULT_COORDINATES_KEY);
        Address addr = getReverseGeocode(m_depCoords);
        writeAddressToFromAddrTvAndToField(addr);
        m_initCoords = m_depCoords;
    }



    private void dealWithToCoordsResult(Intent data)
    {
        if (data == null)
            return;

        m_arrCoords = data.getParcelableExtra(LiftPointsPickActivity.RESULT_COORDINATES_KEY);
        Address addr = getReverseGeocode(m_arrCoords);
        writeAddressToToAddrTvAndSaveToField(addr);
    }


    /**
     *
     * @param coords coordinates to reverse geocode
     * @return reverse geocoded address of null if Geocoder wasn't present or couldn't resolve
     * address
     */
    private Address getReverseGeocode(LatLng coords)
    {
        if (Geocoder.isPresent())
        {
            Geocoder geocoder      = new Geocoder(getContext());
            List<Address> addrList;
            try
            {
                addrList = geocoder.getFromLocation(coords.latitude, coords.longitude, 1);
                if (addrList != null && !addrList.isEmpty())
                    return addrList.get(0);
            }
            catch (IOException e)
            {
                showCantTranslateToCityToast();
                Log.e(TAG, IOEXCEPTION_GEOCODING);
            }
        }
        else
            showCantTranslateToCityToast();

        return null;
    }



    private void writeAddressToFromAddrTvAndToField(Address addr)
    {
        if (addr == null)
            return;

        View view = getView();
        if (view == null)
            return;

        TextView tv = view.findViewById(R.id.fragment_add_stretch_dialog_tv_from_addr);
        String addrStr = addr.getAddressLine(0);
        tv.setText(addrStr);
        m_depAddr = addrStr;
    }



    private void writeAddressToToAddrTvAndSaveToField(Address addr)
    {
        if (addr == null)
            return;

        View view = getView();
        if (view == null)
            return;

        TextView tv = view.findViewById(R.id.fragment_add_stretch_dialog_tv_to_addr);
        String addrStr = addr.getAddressLine(0);
        tv.setText(addrStr);
        m_arrAddr = addrStr;
    }



    private void setDepDateEtListener()
    {
        m_depDateET.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                if (getContext() == null)
                    Log.e(TAG, NULL_CONTEXT_ERROR);

                DatePickerDialog.OnDateSetListener dateSetListener = getOnDateSetListener(m_depDate,
                        (EditText) view);

                Calendar currCal = Calendar.getInstance();
                new DatePickerDialog(getContext(), dateSetListener, currCal.get(Calendar.YEAR),
                        currCal.get(Calendar.MONTH), currCal.get(Calendar.DAY_OF_MONTH)).show();
            }
        });
    }



    private void setDepTimeEtListener()
    {
        m_depTimeET.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                if (getContext() == null)
                    Log.e(TAG, NULL_CONTEXT_ERROR);

                TimePickerDialog.OnTimeSetListener timeSetListener = getOnTimeSetListener(m_depDate,
                        (EditText) view);

                Calendar currCal = Calendar.getInstance();
                new TimePickerDialog(getContext(), timeSetListener, currCal.get(Calendar.HOUR),
                        currCal.get(Calendar.MINUTE), true).show();
            }
        });
    }



    private void setArrDateEtListener(View view)
    {
        EditText et = view.findViewById(R.id.fragment_add_stretch_dialog_et_arr_date);
        et.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                if (getContext() == null)
                    Log.e(TAG, NULL_CONTEXT_ERROR);

                DatePickerDialog.OnDateSetListener dateSetListener = getOnDateSetListener(m_arrDate,
                        (EditText) view);

                Calendar currCal = Calendar.getInstance();
                new DatePickerDialog(getContext(), dateSetListener, currCal.get(Calendar.YEAR),
                        currCal.get(Calendar.MONTH), currCal.get(Calendar.DAY_OF_MONTH)).show();
            }
        });
    }



    private void setArrTimeEtListener(View view)
    {
        EditText et = view.findViewById(R.id.fragment_add_stretch_dialog_et_arr_time);
        et.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                if (getContext() == null)
                    Log.e(TAG, NULL_CONTEXT_ERROR);

                TimePickerDialog.OnTimeSetListener timeSetListener = getOnTimeSetListener(m_arrDate,
                        (EditText) view);

                Calendar currCal = Calendar.getInstance();
                new TimePickerDialog(getContext(), timeSetListener, currCal.get(Calendar.HOUR),
                        currCal.get(Calendar.MINUTE), true).show();
            }
        });
    }



    private DatePickerDialog.OnDateSetListener getOnDateSetListener(final Calendar calendar,
                                                                    final EditText dateEt)
    {
        return new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker datePicker, int year, int month, int day)
            {
                calendar.set(Calendar.DAY_OF_MONTH, day);
                calendar.set(Calendar.MONTH, month);
                calendar.set(Calendar.YEAR, year);
                updateDateEt(calendar, dateEt);
            }
        };
    }



    private void updateDateEt(Calendar calendar, EditText et)
    {
        SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT, Locale.getDefault());

        et.setText(sdf.format(calendar.getTime()));
    }



    private TimePickerDialog.OnTimeSetListener getOnTimeSetListener(final Calendar calendar,
                                                                    final EditText timeEt)
    {
        return new TimePickerDialog.OnTimeSetListener() {
            @Override
            public void onTimeSet(TimePicker timePicker, int hour, int minute) {
                calendar.set(Calendar.HOUR, hour);
                calendar.set(Calendar.MINUTE, minute);

                updateTimeEt(calendar, timeEt);
            }
        };
    }



    private void updateTimeEt(Calendar calendar, EditText et)
    {
        SimpleDateFormat sdf = new SimpleDateFormat(TIME_FORMAT, Locale.getDefault());

        et.setText(sdf.format(calendar.getTime()));
    }



    private void setupCurrency(View view)
    {
        TextView curr = view.findViewById(R.id.fragment_add_stretch_dialog_tv_currency);
        curr.setText(m_currency.getCurrencyCode());
    }



    private void setAddButtonListener(final View view)
    {
        Button but = view.findViewById(R.id.fragment_add_stretch_dialog_but_add);
        but.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View but) {
                if (!validateInputs(view))
                {
                    Toasts.showInvalidDataToast(getContext());
                    return;
                }

                Context context = getContext();
                if (context == null)
                    return;

                if (!InternetUtils.hasInternetConnection(context))
                {
                    Toasts.showNetworkErrorToast(context);
                    return;
                }

                m_price = getPrice(view);
                Stretch res = new Stretch(m_depCoords, m_arrCoords, m_depAddr, m_arrAddr, m_depDate,
                        m_arrDate, m_price, m_liftId);

                m_listener.onFragmentInteraction(res);
                dismiss();
            }
        });
    }



    private Price getPrice(View view)
    {
        EditText amountEt = view.findViewById(R.id.fragment_add_stretch_dialog_et_price);
        String amountStr = amountEt.getText().toString();
        String[] splitted = amountStr.split("[.,]");
        int mainPart = splitted.length >= 1 ? Integer.parseInt(splitted[0]) :
                Integer.parseInt(amountStr);
        String fracString = splitted.length == 2 ? splitted[1] : "0";
        int fracPart = fracString.length() == 1 ? Integer.parseInt(fracString) * 10 :
                Integer.parseInt(fracString);

        return new Price(mainPart, fracPart, m_currency);
    }



    private boolean validateInputs(View view)
    {
        boolean isValid = true;
        Resources resources = getResources();

        if (!validateDeparture(resources, view))
            isValid = false;

        if (!validateArrival(resources, view))
            isValid = false;

        if (!validatePrice(resources, view))
            isValid = false;

        return isValid;
    }



    private boolean validateDeparture(Resources resources, View view)
    {
        boolean isValid = true;

        EditText depDate = view.findViewById(R.id.fragment_add_stretch_dialog_et_dep_date);
        if (depDate.getText().toString().isEmpty())
        {
            depDate.setError(resources.getString(R.string.empty_field_error));
            isValid = false;
        }

        EditText depTime = view.findViewById(R.id.fragment_add_stretch_dialog_et_dep_time);
        if (depTime.getText().toString().isEmpty())
        {
            depTime.setError(resources.getString(R.string.empty_field_error));
            isValid = false;
        }

        if (m_depDate.before(Calendar.getInstance()))
        {
            depDate.setError(resources.getString(R.string.date_must_be_in_future));
            depTime.setError(resources.getString(R.string.date_must_be_in_future));
            isValid = false;
        }

        if (isValid)
        {
            depDate.setError(null);
            depTime.setError(null);
        }

        if (!validateDepartureLocation(resources, view))
            isValid = false;

        return isValid;
    }



    private boolean validateDepartureLocation(Resources resources, View view)
    {
        ImageButton depLoc = view.findViewById(R.id.fragment_add_stretch_dialog_but_choose_dep_loc);
        if (m_depCoords == null)
        {
            depLoc.setImageDrawable(resources.getDrawable(R.drawable.location_red));
            return false;
        }
        else
        {
            depLoc.setImageDrawable(resources.getDrawable(R.drawable.new_location_icon));
            return true;
        }
    }



    private boolean validateArrival(Resources resources, View view)
    {
        boolean isValid = true;

        EditText arrDate = view.findViewById(R.id.fragment_add_stretch_dialog_et_arr_date);
        if (arrDate.getText().toString().isEmpty())
        {
            arrDate.setError(resources.getString(R.string.empty_field_error));
            isValid = false;
        }

        EditText arrTime = view.findViewById(R.id.fragment_add_stretch_dialog_et_arr_time);
        if (arrTime.getText().toString().isEmpty())
        {
            arrTime.setError(resources.getString(R.string.empty_field_error));
            isValid = false;
        }

        if (!m_arrDate.after(m_depDate))
        {
            arrDate.setError(resources.getString(R.string.arr_date_must_be_after_dep_date));
            arrTime.setError(resources.getString(R.string.arr_date_must_be_after_dep_date));
            isValid = false;
        }

        if (m_arrDate.before(Calendar.getInstance()))
        {
            arrDate.setError(resources.getString(R.string.date_must_be_in_future));
            arrTime.setError(resources.getString(R.string.date_must_be_in_future));
            isValid = false;
        }

        if (isValid)
        {
            arrDate.setError(null);
            arrTime.setError(null);
        }

        if (!validateArrivalLocation(resources, view))
            isValid = false;

        return isValid;
    }



    private boolean validateArrivalLocation(Resources resources, View view)
    {
        ImageButton arrLoc = view.findViewById(R.id.fragment_add_stretch_dialog_but_choose_arr_loc);
        if (m_arrCoords == null)
        {
            arrLoc.setImageDrawable(resources.getDrawable(R.drawable.location_red));
            return false;
        }
        else
        {
            arrLoc.setImageDrawable(resources.getDrawable(R.drawable.new_location_icon));
            return true;
        }
    }



    private boolean validatePrice(Resources resources, View view)
    {
        boolean isValid = true;

        EditText price = view.findViewById(R.id.fragment_add_stretch_dialog_et_price);
        String priceText = price.getText().toString();
        if (priceText.isEmpty())
        {
            price.setError(resources.getString(R.string.empty_field_error));
            isValid = false;
        }

        if (!isInIntRange(priceText))
        {
            price.setError(resources.getString(R.string.too_big_price_error));
            isValid = false;
        }

        if (isValid)
            price.setError(null);

        return isValid;
    }



    private boolean isInIntRange(String number)
    {
        try
        {
            double num = Double.parseDouble(number);
            return num < Integer.MAX_VALUE;
        }
        catch (Exception e)
        {
            return false;
        }
    }



    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            m_listener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        m_listener = null;
    }



    // Listener ///////////////////////////////////////////////////////////////////////////////////



    public interface OnFragmentInteractionListener {
        void onFragmentInteraction(Stretch stretch);
    }



    // Toasts & snack bars ////////////////////////////////////////////////////////////////////////



    private void showCantTranslateToCityToast()
    {
        Toast.makeText(getContext(), R.string.cant_translate_to_city, Toast.LENGTH_LONG).show();
    }
}
