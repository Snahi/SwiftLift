package com.snavi.swiftlift.activities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.snavi.swiftlift.R;
import com.snavi.swiftlift.database_objects.Const;
import com.snavi.swiftlift.lift.AddStretchDialogFragment;
import com.snavi.swiftlift.lift.Lift;
import com.snavi.swiftlift.lift.Stretch;
import com.snavi.swiftlift.utils.KeyboardUtils;
import com.snavi.swiftlift.utils.Price;
import com.snavi.swiftlift.utils.Toasts;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Currency;
import java.util.Date;
import java.util.Locale;

/**
 * activity that shows all stretches in a lift, allows for creation and edit of lift or stretch
 */
public class LiftActivity extends AppCompatActivity implements
        AddStretchDialogFragment.OnFragmentInteractionListener {

    // TODO force stretches to be continuous
    // TODO previous points on map during new point choosing
    // TODO delete and edit buttons
    // TODO sometimes lifts are not loaded

    // CONST //////////////////////////////////////////////////////////////////////////////////////
    public static final String TAG = LiftActivity.class.getName();
    public static final String NULL_INTENT_ERROR = "null intent error, can't get stretches";
    private static final String ADD_STRETCH_DIALOD_TAG = "add stretch";
    public static final String STRETCHES_ARRAY_KEY = "stretches";
    public static final String LIFT_KEY = "lift";
    public static final String STRETCHES_TYPE_EXCEPTION = "Stretches passed to this activity should be in ArrayList<Stretch>, not ";
    public static final String RESULT_LIFT_KEY = "success";
    private static final int LOCATION_PERMISION_REQ_CODE = 1;


    // fields /////////////////////////////////////////////////////////////////////////////////////
    private Lift m_lift;
    private CollectionReference m_stretchesCollection;
    private StretchesAdapter m_adapter;
    /**
     * when user is in this activity he is supposed to add new stretches. To add new stretch user
     * has to choose location. It is great if the map starts at position close to where user is
     * likely to choose starting point for stretch. When there are no previous stretches then
     * it is last user location. When there is at least one stretch it is location of last stretch
     * end.
     */
    private LatLng m_mostConvenientLocation;
    /**
     * because stretches must be continuous when user adds first stretch the following ones will
     * have set coordFrom as the m_lastStretchLoc (coordTo of previous stretch). Null if it is the
     * first stretch
     */
    @Nullable private LatLng m_lastStretchLoc;
    @Nullable private String m_lastStretchAddr;
    /**
     * when user adds first stretch the next ones will have default departure date set to
     * m_lastStretchDate (arrDate of previous stretch). Null if it is the first stretch
     */
    @Nullable private Date m_lastStretchDate;


    // views
    private AutoCompleteTextView m_currency;
    private TextView m_noLiftsText;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lift);

        initViews();
        initMostConvenientLocation();
        initRecyclerView();
        initFirebase();
        initActivityDetails();
        setButtonsListeners();
    }



    private void initViews()
    {
        m_noLiftsText = findViewById(R.id.activity_lift_tv_empty_stretches);
        m_currency    = findViewById(R.id.activity_lift_actv_currency);
    }



    private void initActivityDetails()
    {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, Price.CURRENCIES);

        m_currency.setAdapter(adapter);
        m_currency.setThreshold(1);
        m_currency.setText(m_lift.getCurrencyCode());
        setCurrencyListener();
    }



    private void setCurrencyListener()
    {
        m_currency.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                if (validateCurrency())
                {
                    m_lift.setCurrency(Currency.getInstance(m_currency.getText().toString()));
                    updateStretchesCurrency();
                    KeyboardUtils.hideSoftKeyboard(LiftActivity.this);
                }
                return true;
            }
        });
    }



    private void updateStretchesCurrency()
    {
        Price price;
        for (Stretch stretch : m_adapter.m_stretches)
        {
            price = stretch.getPrice();
            price.setCurrency(m_lift.getCurrency());
        }

        m_adapter.notifyDataSetChanged();
    }



    private void initMostConvenientLocation()
    {
        FusedLocationProviderClient fusedLocProvider =
                LocationServices.getFusedLocationProviderClient(this);

        if ( ContextCompat.checkSelfPermission( this,
                android.Manifest.permission.ACCESS_COARSE_LOCATION ) !=
                PackageManager.PERMISSION_GRANTED )
        {
            ActivityCompat.requestPermissions(this,
                    new String[] {Manifest.permission.ACCESS_COARSE_LOCATION},
                    LOCATION_PERMISION_REQ_CODE);
        }

        fusedLocProvider.getLastLocation().addOnSuccessListener(new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location)
            {
                if (location != null)
                {
                    m_mostConvenientLocation = new LatLng(location.getLatitude(),
                            location.getLongitude());
                }
            }
        });
    }



    private void initRecyclerView()
    {
        RecyclerView recyclerView = findViewById(R.id.activity_lift_rv);
        m_adapter  = new StretchesAdapter(getStretches());
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);

        recyclerView.setAdapter(m_adapter);
        recyclerView.setLayoutManager(layoutManager);
    }


    /**
     * LiftIdNotSpecifiedException is thrown in this method, event though it is called from onCreate.
     * In this case it's ok, because the only possible option for this method fails is when programmer
     * fails during creation of this activity, so the exception will help him find error shortly.
     */
    private void initFirebase()
    {
        Intent intent = getIntent();
        if (intent == null)
            throw new LiftIdNotSpecifiedException();

        m_lift = intent.getParcelableExtra(LIFT_KEY);
        if (m_lift == null)
            throw new LiftIdNotSpecifiedException();
        m_lift.setStretches(m_adapter.m_stretches);

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        m_stretchesCollection = db.collection(Const.STRETCHES_COLLECTION);
    }



    private void setButtonsListeners()
    {
        setAddStretchButtonListener();
        setDoneButtonListener();
    }



    private void setAddStretchButtonListener()
    {
        Button but = findViewById(R.id.activity_lift_but_add_stretch);
        but.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (validateCurrency())
                {
                    AddStretchDialogFragment addStretchDial = new AddStretchDialogFragment();
                    Bundle bun = new Bundle();

                    bun.putString(AddStretchDialogFragment.LIFT_ID_KEY, m_lift.getId());            // Lift id
                    bun.putParcelable(AddStretchDialogFragment.INIT_COORDINATES_KEY,                // Init coordinates
                            m_mostConvenientLocation);
                    bun.putSerializable(AddStretchDialogFragment.CURRENCY_KEY,                      // currency
                            Currency.getInstance(m_currency.getText().toString()));
                    bun.putParcelable(AddStretchDialogFragment.DEP_COORDS_KEY, m_lastStretchLoc);       // departure coordinates
                    bun.putString(AddStretchDialogFragment.DEP_ADDR_KEY, m_lastStretchAddr);            // departure address
                    bun.putSerializable(AddStretchDialogFragment.DEP_DATE_KEY, m_lastStretchDate);      // departure date

                    addStretchDial.setArguments(bun);
                    addStretchDial.show(getSupportFragmentManager(), ADD_STRETCH_DIALOD_TAG);
                }
            }
        });
    }



    private boolean validateCurrency()
    {
        String currStr = m_currency.getText().toString();
        if (currStr.isEmpty())
        {
            m_currency.setError(this.getString(R.string.empty_field_error));
            return false;
        }

        try
        {
            Currency.getInstance(currStr);
        }
        catch (IllegalArgumentException e)
        {
            m_currency.setError(this.getString(R.string.invalid_currency_error));
            return false;
        }

        return true;
    }



    private void setDoneButtonListener()
    {
        Button but = findViewById(R.id.activity_lift_but_done);
        but.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                if(prepareActivityToFinish())
                    finish();
            }
        });
    }



    private boolean prepareActivityToFinish()
    {
        ArrayList<Stretch> stretches = m_adapter.m_stretches;

        Intent data = new Intent();

        String userId = FirebaseAuth.getInstance().getUid();
        if (userId == null)
        {
            Toasts.showUserSignedOutToast(this);
            return false;
        }

        if (!validateCurrency())
            return false;

        m_lift.setCurrency(Currency.getInstance(m_currency.getText().toString()));
        m_lift.updateInDb();
        updateStretchesCurrency();

        if (stretches.isEmpty())
        {
            data.putExtra(RESULT_LIFT_KEY, m_lift);
            setResult(Activity.RESULT_CANCELED, data);
        }
        else
        {
            data.putExtra(RESULT_LIFT_KEY, m_lift);
            setResult(Activity.RESULT_OK, data);
        }

        return true;
    }



    @Override
    public void onBackPressed()
    {
        if (prepareActivityToFinish())
            super.onBackPressed();
    }



    @SuppressWarnings("unchecked")
    private ArrayList<Stretch> getStretches()
    {
        Intent intent = getIntent();
        if (intent == null)
        {
            Log.e(TAG, NULL_INTENT_ERROR);
            return new ArrayList<>();
        }

        Bundle bun = intent.getExtras();
        if (bun == null)
            return new ArrayList<>();

        Serializable preArr = bun.getSerializable(STRETCHES_ARRAY_KEY);
        if (preArr == null)
            return new ArrayList<>();

        ArrayList<Stretch> res = new ArrayList<>();
        if (res.getClass().isAssignableFrom(preArr.getClass()))
        {
            res = (ArrayList<Stretch>) preArr;
            return res;
        }
        else
        {
            Log.e(TAG, STRETCHES_TYPE_EXCEPTION + preArr.getClass().getName());
            return new ArrayList<>();
        }
    }



    // AddStretchDialogFragment ///////////////////////////////////////////////////////////////////



    @Override
    public void onFragmentInteraction(final Stretch stretch)
    {
        final ProgressBar progBar = findViewById(R.id.activity_lift_progress_bar);
        progBar.setVisibility(View.VISIBLE);

        m_stretchesCollection.document().set(stretch.getFirestoreObject()).addOnCompleteListener(
                new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful())
                {
                    m_lift.addStretch(stretch);
                    m_adapter.notifyDataSetChanged();
                    m_mostConvenientLocation = stretch.getCoordTo();
                    m_lastStretchAddr = stretch.getAddrTo();
                    m_lastStretchLoc  = stretch.getCoordTo();
                    m_lastStretchDate = stretch.getArrDate();
                }
                else
                    showStretchAddErrorSnackbar();

                progBar.setVisibility(View.GONE);
            }
        });
    }



    // Toasts & snack bars ////////////////////////////////////////////////////////////////////////



    private void showStretchAddErrorSnackbar()
    {
        Snackbar.make(findViewById(R.id.activity_lift_cl), R.string.stretch_add_error,
                Snackbar.LENGTH_LONG).show();
    }



    // RecyclerView ///////////////////////////////////////////////////////////////////////////////



    class StretchesAdapter extends RecyclerView.Adapter<LiftActivity.StretchesAdapter.StretchViewHolder> {


        // fields /////////////////////////////////////////////////////////////////////////////////
        private ArrayList<Stretch> m_stretches;



        private StretchesAdapter(ArrayList<Stretch> stretches)
        {
            m_stretches = stretches;
        }


        @NonNull
        @Override
        public LiftActivity.StretchesAdapter.StretchViewHolder onCreateViewHolder(
                @NonNull ViewGroup parent, int viewType)
        {
            CardView card = (CardView) getLayoutInflater().inflate(R.layout.card_view_route_stretch,
                    parent, false);
            return new LiftActivity.StretchesAdapter.StretchViewHolder(card);
        }



        @Override
        public void onBindViewHolder(@NonNull LiftActivity.StretchesAdapter.StretchViewHolder holder,
                                     int position)
        {
            holder.bind(m_stretches.get(position));

            if (!m_stretches.isEmpty())
                m_noLiftsText.setVisibility(View.GONE);
        }



        @Override
        public int getItemCount()
        {
            return m_stretches.size();
        }



        // Holder //////////////////////////////////////////////////////////////////////////////////



        class StretchViewHolder extends RecyclerView.ViewHolder {

            private TextView m_tvFrom;
            private TextView m_tvTo;
            private TextView m_tvDateFrom;
            private TextView m_tvDateTo;
            private TextView m_tvPrice;


            private StretchViewHolder(CardView card)
            {
                super(card);
                m_tvFrom     = card.findViewById(R.id.card_view_route_stretch_tv_from);
                m_tvTo       = card.findViewById(R.id.card_view_route_stretch_tv_to);
                m_tvDateFrom = card.findViewById(R.id.card_view_route_stretch_tv_date_time_from);
                m_tvDateTo   = card.findViewById(R.id.card_view_route_stretch_tv_date_time_to);
                m_tvPrice    = card.findViewById(R.id.card_view_route_stretch_tv_price);
            }



            private void bind(Stretch stretch)
            {
                m_tvFrom.setText(stretch.getAddrFrom());
                m_tvTo.setText(stretch.getAddrTo());
                m_tvDateFrom.setText(stretch.depDateDisplay(Locale.getDefault()));
                m_tvDateTo.setText(stretch.arrDateDisplay(Locale.getDefault()));
                m_tvPrice.setText(stretch.getPrice().toString());
            }
        }
    }



    public class LiftIdNotSpecifiedException extends RuntimeException {

        public static final String MESSAGE = "Lift id must be passed to activity via intent. This " +
                "exception may occured because of one of two reasons: \n" +
                "1. getIntent() returned null \n" +
                "2. intent didn't contain lift id under LIFT_DOCUMENT_KEY";


        public LiftIdNotSpecifiedException()
        {
            super(MESSAGE);
        }
    }
}
