package com.snavi.swiftlift.activities;

import androidx.annotation.NonNull;
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
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.snavi.swiftlift.R;
import com.snavi.swiftlift.database_objects.Const;
import com.snavi.swiftlift.lift.AddStretchDialogFragment;
import com.snavi.swiftlift.lift.Lift;
import com.snavi.swiftlift.lift.Stretch;
import com.snavi.swiftlift.utils.InternetUtils;
import com.snavi.swiftlift.utils.KeyboardUtils;
import com.snavi.swiftlift.utils.Price;
import com.snavi.swiftlift.utils.Toasts;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Currency;
import java.util.Locale;

/**
 * activity that shows all stretches in a lift, allows for creation and edit of lift or stretch
 */
public class LiftActivity extends AppCompatActivity implements
        AddStretchDialogFragment.OnFragmentInteractionListener {

    // CONST //////////////////////////////////////////////////////////////////////////////////////
    // keys
    public static final String STRETCHES_ARRAY_KEY = "stretches";
    public static final String RESULT_LIFT_KEY     = "success";
    public static final String LIFT_KEY            = "lift";
    // request codes
    private static final int REQ_LOCATION_PERMISION = 1;
    private static final int REQ_DESCRIPTION        = 1321;
    // errors
    public static final String NULL_INTENT_ERROR = "null intent error, can't get stretches";
    // other
    public static final String TAG                      = LiftActivity.class.getName();
    private static final String ADD_STRETCH_DIALOD_TAG  = "add stretch";
    public static final String STRETCHES_TYPE_EXCEPTION = "Stretches passed to this activity should be in ArrayList<Stretch>, not ";



    // fields /////////////////////////////////////////////////////////////////////////////////////
    private Lift                m_lift;
    private StretchesAdapter    m_adapter;
    private CollectionReference m_stretchesCollection;
    /**
     * when user is in this activity he is supposed to add new stretches. To add new stretch user
     * has to choose location. It is great if the map starts at position close to where user is
     * likely to choose starting point for stretch. When there are no previous stretches then
     * it is last user location. When there is at least one stretch it is location of last stretch
     * end.
     */
    private LatLng m_userLastLocation;

    // views
    private AutoCompleteTextView m_currency;
    private TextView m_noLiftsText;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lift);

        initViews();
        initUserLastLocation();
        initRecyclerView();
        initFirebase();
        initActivityDetails();
        setButtonsListeners();
    }



    private void initViews()
    {
        m_tvNoLiftsText     = findViewById(R.id.activity_lift_tv_empty_stretches);
        m_actvCurrency      = findViewById(R.id.activity_lift_actv_currency);
        m_butDescription    = findViewById(R.id.activity_lift_but_description);
    }



    private void initActivityDetails()
    {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, Price.CURRENCIES);

        m_actvCurrency.setAdapter(adapter);
        m_actvCurrency.setThreshold(1);
        m_actvCurrency.setText(m_lift.getCurrencyCode());
        setCurrencyListener();
    }



    private void setCurrencyListener()
    {
        m_actvCurrency.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                if (validateCurrency())
                {
                    m_lift.setCurrency(Currency.getInstance(m_actvCurrency.getText().toString()));
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



    private void initUserLastLocation()
    {
        FusedLocationProviderClient fusedLocProvider =
                LocationServices.getFusedLocationProviderClient(this);

        if (ContextCompat.checkSelfPermission( this,
                android.Manifest.permission.ACCESS_COARSE_LOCATION ) !=
                PackageManager.PERMISSION_GRANTED )
        {
            ActivityCompat.requestPermissions(this,
                    new String[] {Manifest.permission.ACCESS_COARSE_LOCATION},
                    REQ_LOCATION_PERMISION);
        }

        fusedLocProvider.getLastLocation().addOnSuccessListener(new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location)
            {
                if (location != null)
                {
                    m_userLastLocation = new LatLng(location.getLatitude(),
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
        setDescriptionButtonListener();
    }



    private void setDescriptionButtonListener()
    {
        m_butDescription.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                Intent intent = new Intent(
                        LiftActivity.this, LiftDescriptionActivity.class);
                intent.putExtra(LiftDescriptionActivity.DESCRIPTION_KEY, m_lift.getDescription());
                startActivityForResult(intent, REQ_DESCRIPTION);
            }
        });
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
                            m_lift.getStretches().isEmpty() ? m_userLastLocation :
                                    m_lift.getLastStretchArrCoords());
                    bun.putSerializable(AddStretchDialogFragment.CURRENCY_KEY,                      // currency
                            Currency.getInstance(m_actvCurrency.getText().toString()));
                    bun.putParcelable(AddStretchDialogFragment.DEP_COORDS_KEY,                      // departure coordinates
                            m_lift.getLastStretchArrCoords());
                    bun.putParcelable(AddStretchDialogFragment.DEP_ADDR_KEY,                        // departure address
                            m_lift.getLastStretchArrAddr());
                    bun.putSerializable(AddStretchDialogFragment.DEP_DATE_KEY,                      // departure date
                            m_lift.getLastStretchArrDate());

                    addStretchDial.setArguments(bun);
                    addStretchDial.show(getSupportFragmentManager(), ADD_STRETCH_DIALOD_TAG);
                }
            }
        });
    }



    private boolean validateCurrency()
    {
        String currStr = m_actvCurrency.getText().toString();
        if (currStr.isEmpty())
        {
            m_actvCurrency.setError(this.getString(R.string.empty_field_error));
            return false;
        }

        try
        {
            Currency.getInstance(currStr);
        }
        catch (IllegalArgumentException e)
        {
            m_actvCurrency.setError(this.getString(R.string.invalid_currency_error));
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



    // activity result /////////////////////////////////////////////////////////////////////////////



    @Override
    public void onActivityResult(int reqCode, int resCode, Intent data)
    {
        super.onActivityResult(reqCode, resCode, data);

        if (reqCode == REQ_DESCRIPTION && resCode == Activity.RESULT_OK)
        {
            m_lift.setDescription(data.getStringExtra(LiftDescriptionActivity.DESCRIPTION_KEY));
        }
    }



    // AddStretchDialogFragment ////////////////////////////////////////////////////////////////////



    @Override
    public void onFragmentInteraction(final Stretch stretch)
    {
        final ProgressBar progBar = findViewById(R.id.activity_lift_progress_bar);
        progBar.setVisibility(View.VISIBLE);

        final DocumentReference stretchDoc = m_stretchesCollection.document();
        stretchDoc.set(stretch.getFirestoreObject()).addOnCompleteListener(
                new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful())
                {
                    stretch.setId(stretchDoc.getId());
                    m_lift.addStretch(stretch);
                    m_adapter.notifyDataSetChanged();
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
            card.findViewById(R.id.card_view_route_stretch_but_edit).setVisibility(View.GONE);      // in this context stretches can't be edited
            return new LiftActivity.StretchesAdapter.StretchViewHolder(card);
        }



        @Override
        public void onBindViewHolder(@NonNull LiftActivity.StretchesAdapter.StretchViewHolder holder,
                                     int position)
        {
            holder.bind(m_stretches.get(position));

            if (!m_stretches.isEmpty())
                m_tvNoLiftsText.setVisibility(View.GONE);
        }



        @Override
        public int getItemCount()
        {
            return m_stretches.size();
        }



        // Holder //////////////////////////////////////////////////////////////////////////////////



        class StretchViewHolder extends RecyclerView.ViewHolder {

            private TextView    m_tvDepCity;
            private TextView    m_tvDepStreet;
            private TextView    m_tvDepTime;
            private TextView    m_tvArrCity;
            private TextView    m_tvArrStreet;
            private TextView    m_tvArrTime;
            private TextView    m_tvPrice;
            private ImageButton m_butDelete;


            private StretchViewHolder(CardView card)
            {
                super(card);

                m_tvDepCity     = card.findViewById(R.id.card_view_route_stretch_tv_departure_city);
                m_tvDepStreet   = card.findViewById(R.id.card_view_route_stretch_tv_departure_street);
                m_tvDepTime     = card.findViewById(R.id.card_view_route_stretch_tv_departure_time);
                m_tvArrCity     = card.findViewById(R.id.card_view_route_stretch_tv_arrival_city);
                m_tvArrStreet   = card.findViewById(R.id.card_view_route_stretch_tv_arrival_street);
                m_tvArrTime     = card.findViewById(R.id.card_view_route_stretch_tv_arrival_time);
                m_tvPrice       = card.findViewById(R.id.card_view_route_stretch_tv_price);
                m_butDelete     = card.findViewById(R.id.card_view_route_stretch_but_delete);
            }



            private void bind(Stretch stretch)
            {
                bindDeparture(stretch);
                bindArrival(stretch);

                m_tvPrice.setText(stretch.getPrice().toString());

                if (getAdapterPosition() != m_stretches.size() - 1)                                 // user can delete only the last stretch
                    m_butDelete.setVisibility(View.INVISIBLE);
                else
                {
                    m_butDelete.setVisibility(View.VISIBLE);
                    setupDeleteButtonListener();
                }
            }



            private void bindDeparture(Stretch stretch)
            {
                String depCityAndPostCode = stretch.getPostCodeFrom() + " " + stretch.getCityFrom();
                m_tvDepCity.setText(depCityAndPostCode);

                String depStreetAndNumber = stretch.getStreetFrom() + " " +
                        stretch.getStreetNumFrom();
                m_tvDepStreet.setText(depStreetAndNumber);

                m_tvDepTime.setText(stretch.depDateDisplay(Locale.getDefault()));
            }



            private void bindArrival(Stretch stretch)
            {
                String arrCityAndPostCode = stretch.getPostCodeTo() + " " + stretch.getCityTo();
                m_tvArrCity.setText(arrCityAndPostCode);

                String arrStreetAndNumber = stretch.getStreetTo() + " " + stretch.getStreetNumTo();
                m_tvArrStreet.setText(arrStreetAndNumber);

                m_tvArrTime.setText(stretch.arrDateDisplay(Locale.getDefault()));
            }



            /**
             * deletes the last stretch
             */
            private void setupDeleteButtonListener()
            {
                m_butDelete.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (!InternetUtils.hasInternetConnection(LiftActivity.this))
                        {
                            Toasts.showNetworkErrorToast(LiftActivity.this);
                            return;
                        }

                        final int posToRemove = m_stretches.size() - 1;
                        FirebaseFirestore db = FirebaseFirestore.getInstance();
                        db.collection(Const.STRETCHES_COLLECTION)
                                .document(m_stretches.get(posToRemove).getId())
                                .delete()
                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        m_stretches.remove(posToRemove);
                                        m_adapter.notifyItemChanged(posToRemove);
                                        if (posToRemove > 0)
                                            m_adapter.notifyItemChanged(posToRemove - 1);
                                        else
                                            m_adapter.notifyItemChanged(0);
                                    }
                                })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        Toasts.showStretchDeleteErrorToast(LiftActivity.this);
                                    }
                                });
                    }
                });
            }
        }
    }



    public class LiftIdNotSpecifiedException extends RuntimeException {

        static final String MESSAGE = "Lift id must be passed to activity via intent. This " +
                "exception may occured because of one of two reasons: \n" +
                "1. getIntent() returned null \n" +
                "2. intent didn't contain lift id under LIFT_DOCUMENT_KEY";


        LiftIdNotSpecifiedException()
        {
            super(MESSAGE);
        }
    }
}
