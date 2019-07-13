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
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
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
import com.snavi.swiftlift.utils.Price;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class AddStretchDialogFragment extends DialogFragment {

    // TODO pass proper location
    // TODO make the date and time pickers more user frinedly (no double click)
    // TODO select location button change when user clicked
    // TODO error toast if data is invalid

    // CONST ///////////////////////////////////////////////////////////////////////////////////////
    private static final int FROM_COORDS_REQ_CODE = 7771;
    private static final int TO_COORDS_REQ_CODE   = 7772;
    public static final String INIT_COORDINATES_KEY = "initial_coordinates";
    public static final String CURRECY_KEY = "currency";
    private static final String TAG = AddStretchDialogFragment.class.getName();
    private static final String IOEXCEPTION_GEOCODING = "io exception occured during geocoding";
    private static final String NULL_CONTEXT_ERROR = "null context error";
    public static final String LIFT_ID_KEY = "l_id";
    private static final String LIFT_BUNDLE_EXCEPTION = "You must pass bundle with lift id!";
    private static final String NO_CURRENCY_PASSED_EXCEPTION = "Currency wasn't passed in arguments. It's obligatory";


    // fields //////////////////////////////////////////////////////////////////////////////////////
    private OnFragmentInteractionListener m_listener;
    private LatLng m_initCoords;
    private String m_liftId;
    /**
     * currency in which all prices in current lift are
     */
    private Currency m_currency;

    // result
    private Calendar m_depDate;
    private Calendar m_arrDate;
    private LatLng m_depCoords;
    private LatLng m_arrCoords;
    private String m_depAddr;
    private String m_arrAddr;
    private Price m_price;



    public AddStretchDialogFragment() {
        // Required empty public constructor
    }



    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        m_depDate = Calendar.getInstance();
        m_arrDate = Calendar.getInstance();
        Bundle bun = getNonNullArguments();
        setupLiftId(bun);
        setupCurrency(bun);
    }



    /**
     * can throw runtime exception if programmer didn't passed bundle with id
     */
    private Bundle getNonNullArguments()
    {
        Bundle bun = getArguments();
        if (bun == null)
            throw new RuntimeException(LIFT_BUNDLE_EXCEPTION);

        return bun;
    }



    private void setupLiftId(Bundle bun)
    {
        m_liftId = bun.getString(LIFT_ID_KEY);

        if (m_liftId == null)
            throw new RuntimeException(LIFT_BUNDLE_EXCEPTION);
    }



    private void setupCurrency(Bundle bun)
    {
        m_currency = (Currency) bun.getSerializable(CURRECY_KEY);
        if (m_currency == null)
            throw new RuntimeException(NO_CURRENCY_PASSED_EXCEPTION);
    }



    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_add_stretch_dialog, container,
                false);

        Bundle args = getArguments();
        if (args != null)
            m_initCoords = getArguments().getParcelable(INIT_COORDINATES_KEY);

        lockEditTexts(view);
        setButtonsListeners(view);
        setDepDateEtListener(view);
        setDepTimeEtListener(view);
        setArrDateEtListener(view);
        setArrTimeEtListener(view);
        setupCurrency(view);

        return view;
    }



    private void lockEditTexts(View view)
    {
        EditText depDate = view.findViewById(R.id.fragment_add_stretch_dialog_et_dep_date);
        depDate.setInputType(InputType.TYPE_NULL);
        EditText depTime = view.findViewById(R.id.fragment_add_stretch_dialog_et_dep_time);
        depTime.setInputType(InputType.TYPE_NULL);
        EditText arrDate = view.findViewById(R.id.fragment_add_stretch_dialog_et_arr_date);
        arrDate.setInputType(InputType.TYPE_NULL);
        EditText arrTime = view.findViewById(R.id.fragment_add_stretch_dialog_et_arr_time);
        arrTime.setInputType(InputType.TYPE_NULL);
    }



    @Override
    public void onResume() {
        super.onResume();
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
        m_initCoords = m_arrCoords;
    }



    private Address getReverseGeocode(LatLng coords)
    {
        if (Geocoder.isPresent())
        {
            Geocoder geocoder      = new Geocoder(getContext());
            List<Address> addrList;
            try {
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



    private void setDepDateEtListener(View view)
    {
        EditText et = view.findViewById(R.id.fragment_add_stretch_dialog_et_dep_date);
        et.setOnClickListener(new View.OnClickListener() {
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



    private void setDepTimeEtListener(View view)
    {
        EditText et = view.findViewById(R.id.fragment_add_stretch_dialog_et_dep_time);
        et.setOnClickListener(new View.OnClickListener() {
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
        String format = "dd/MM/YYYY";
        SimpleDateFormat sdf = new SimpleDateFormat(format, Locale.getDefault());

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
        String format = "HH:mm";
        SimpleDateFormat sdf = new SimpleDateFormat(format, Locale.getDefault());

        et.setText(sdf.format(calendar.getTime()));
    }



    private void setupCurrency(View view)
    {
        TextView curr = view.findViewById(R.id.fragment_add_stretch_dialog_tv_currency);
        curr.setText(m_currency.getCurrencyCode());
//        if (getContext() == null)
//        {
//            Log.e(TAG, NULL_CONTEXT_ERROR);
//            return;
//        }
//
//        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(),
//                android.R.layout.simple_list_item_1, Price.CURRENCIES);
//
//        AutoCompleteTextView currency = view.findViewById(
//                R.id.fragment_add_stretch_dialog_acet_currency);
//        currency.setAdapter(adapter);
//        currency.setThreshold(1);
    }



    private void setAddButtonListener(final View view)
    {
        Button but = view.findViewById(R.id.fragment_add_stretch_dialog_but_add);
        but.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View but) {
                if (!validateInputs(view))
                    return;

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

//        AutoCompleteTextView currencyEt = view.findViewById(
//                R.id.fragment_add_stretch_dialog_acet_currency);
//        Currency currency = Currency.getInstance(currencyEt.getText().toString());
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

//        if (!validateCurrency(resources, view))
//            isValid = false;

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



//    private boolean validateCurrency(Resources resources, View view)
//    {
//        EditText currency = view.findViewById(R.id.fragment_add_stretch_dialog_acet_currency);
//        String currStr = currency.getText().toString();
//        if (currStr.isEmpty())
//        {
//            currency.setError(resources.getString(R.string.empty_field_error));
//            return false;
//        }
//
//        try
//        {
//            Currency.getInstance(currStr);
//        }
//        catch (IllegalArgumentException e)
//        {
//            currency.setError(resources.getString(R.string.invalid_currency_error));
//            return false;
//        }
//
//        return true;
//    }



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
