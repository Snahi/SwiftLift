package com.snavi.swiftlift.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.snavi.swiftlift.R;
import com.snavi.swiftlift.database_objects.Const;
import com.snavi.swiftlift.lift.AddStretchDialogFragment;
import com.snavi.swiftlift.lift.Lift;
import com.snavi.swiftlift.lift.Stretch;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Currency;
import java.util.Locale;

/**
 * activity that shows all stretches in a lift, allows for creation and edit of lift or stretch
 */
public class LiftActivity extends AppCompatActivity implements
        AddStretchDialogFragment.OnFragmentInteractionListener {

    // TODO only one currency!

    // CONST //////////////////////////////////////////////////////////////////////////////////////
    public static final String TAG = LiftActivity.class.getName();
    public static final String NULL_INTENT_ERROR = "null intent error, can't get stretches";
    private static final String ADD_STRETCH_DIALOD_TAG = "add stretch";
    public static final String STRETCHES_ARRAY_KEY = "stretches";
    public static final String LIFT_DOCUMENT_KEY = "lift_id";
    public static final String STRETCHES_TYPE_EXCEPTION = "Stretches passed to this activity should be in ArrayList<Stretch>, not ";
    public static final String RESULT_LIFT_KEY = "success";


    // fields /////////////////////////////////////////////////////////////////////////////////////
    private String m_liftId;
    private CollectionReference m_stretchesCollection;
    private StretchesAdapter m_adapter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lift);

        initRecyclerView();
        initFirebase();
        setButtonsListeners();
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

        m_liftId = intent.getStringExtra(LIFT_DOCUMENT_KEY);
        if (m_liftId == null)
            throw new LiftIdNotSpecifiedException();

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        m_stretchesCollection = db.collection(Const.STRETCHES_COLLECTION);
    }



    private void setButtonsListeners()
    {
        setAddStretchButtonListener();
        setSaveButtonListener();
    }



    private void setAddStretchButtonListener()
    {
        Button but = findViewById(R.id.activity_lift_but_add_stretch);
        but.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AddStretchDialogFragment addStretchDial = new AddStretchDialogFragment();
                Bundle bun = new Bundle();
                bun.putString(AddStretchDialogFragment.LIFT_ID_KEY, m_liftId);
                addStretchDial.setArguments(bun);
                addStretchDial.show(getSupportFragmentManager(), ADD_STRETCH_DIALOD_TAG);
            }
        });
    }



    private void setSaveButtonListener()
    {
        Button but = findViewById(R.id.activity_lift_but_save);
        but.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                ArrayList<Stretch> stretches = m_adapter.m_stretches;

                Intent data = new Intent();

                if (stretches.isEmpty())
                {
                    Lift lift = new Lift(stretches, Currency.getInstance(Locale.getDefault()),
                            m_liftId);
                    data.putExtra(RESULT_LIFT_KEY, lift);
                    setResult(Activity.RESULT_CANCELED, data);
                }
                else
                {
                    Lift lift = new Lift(stretches, stretches.get(0).getPrice().getCurrency(),
                            m_liftId);
                    data.putExtra(RESULT_LIFT_KEY, lift);
                    setResult(Activity.RESULT_OK, data);
                }
                // TODO parcelable lift
                finish();
            }
        });
    }



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
                    m_adapter.m_stretches.add(stretch);
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
            return new LiftActivity.StretchesAdapter.StretchViewHolder(card);
        }



        @Override
        public void onBindViewHolder(@NonNull LiftActivity.StretchesAdapter.StretchViewHolder holder,
                                     int position)
        {
            holder.bind(m_stretches.get(position));
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
