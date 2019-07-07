package com.snavi.swiftlift.signed_in_fragments;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.snavi.swiftlift.R;
import com.snavi.swiftlift.activities.LiftActivity;
import com.snavi.swiftlift.database_objects.Const;
import com.snavi.swiftlift.lift.Lift;
import com.snavi.swiftlift.lift.Stretch;

import java.util.ArrayList;
import java.util.Currency;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;


public class DriverFragment extends Fragment {


    // CONST //////////////////////////////////////////////////////////////////////////////////////
    private static final int CREATE_LIFT_RES_CODE = 7831;


    // fields /////////////////////////////////////////////////////////////////////////////////////
    private LiftsAdapter m_adapter;
    private CollectionReference m_liftsCollection;
    private CollectionReference m_stretchesCollection;


    public DriverFragment() {
        // Required empty public constructor
    }


    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        m_liftsCollection = db.collection(Const.LIFTS_COLLECTION);
        m_stretchesCollection = db.collection(Const.STRETCHES_COLLECTION);
    }



    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        View view = inflater.inflate(R.layout.fragment_driver, container, false);

        ArrayList<Lift> lifts = new ArrayList<>();
        initRecyclerView(view, lifts);

        setAddLiftButtonListener(view);

        return view;
    }



    @Override
    public void onResume()
    {
        super.onResume();
        m_adapter.m_lifts.clear();
        loadLifts(m_adapter.m_lifts);
    }



    private void initRecyclerView(View view, ArrayList<Lift> lifts)
    {
        RecyclerView rv = view.findViewById(R.id.fragment_driver_rv);
        m_adapter = new LiftsAdapter(lifts);
        RecyclerView.LayoutManager manager = new LinearLayoutManager(getContext());
        rv.setAdapter(m_adapter);
        rv.setLayoutManager(manager);
    }



    private void loadLifts(@NonNull final ArrayList<Lift> lifts)
    {
        String userId = getUserId();
        if (userId == null)
        {
            showAuthErrorSnackbar();
            return;
        }
        Log.d("MY", "before lifts query");
        m_liftsCollection.whereEqualTo(Const.LIFT_OWNER, userId).get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots)
                    {
                        Log.d("MY", "in lifts query on success");
                        List<DocumentSnapshot> docs = queryDocumentSnapshots.getDocuments();
                        Log.d("MY", "docs size: " + docs.size());
                        ArrayList<String> ids = new ArrayList<>(docs.size());
                        for (DocumentSnapshot doc : docs)
                            ids.add(doc.getId());
                        createLifts(lifts, ids);
                    }
        });

    }



    private void createLifts(final ArrayList<Lift> lifts, ArrayList<String> ids)
    {
        Log.d("MY", "in create lifts");
        for (String id : ids)
        {
            Log.d("MY", "id: " + id);
            m_stretchesCollection.whereEqualTo(Const.STRETCH_LIFT_ID, id).get()
                    .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                        @Override
                        public void onSuccess(QuerySnapshot queryDocumentSnapshots)
                        {
                            List<DocumentSnapshot> docs = queryDocumentSnapshots.getDocuments();
                            Log.d("MY", "stretches size: " + docs.size());

                            ArrayList<Stretch> stretches = getStretches(docs);
                            Currency currency = stretches.isEmpty() ?
                                    Currency.getInstance(Locale.getDefault())
                                    :
                                    stretches.get(0).getPrice().getCurrency();
                            lifts.add(new Lift(stretches, currency));
                            m_adapter.notifyItemChanged(lifts.size() - 1);
                        }
            });
        }
    }



    private ArrayList<Stretch> getStretches(List<DocumentSnapshot> docs)
    {
        ArrayList<Stretch> stretches = new ArrayList<>();
        for (DocumentSnapshot doc : docs)
            stretches.add(new Stretch(doc));

        return stretches;
    }



    private void setAddLiftButtonListener(View view)
    {
        Button but = view.findViewById(R.id.fragment_driver_but_add_lift);
        but.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String liftId = createNewLift();
                if (liftId == null)
                {
                    showAuthErrorSnackbar();
                    return;
                }
                Intent intent = new Intent(getContext(), LiftActivity.class);
                intent.putExtra(LiftActivity.LIFT_DOCUMENT_KEY, liftId);
                startActivityForResult(intent, CREATE_LIFT_RES_CODE);
            }
        });
    }



    /**
     *
     * @return lift id
     */
    private String createNewLift()
    {
        DocumentReference liftDoc = m_liftsCollection.document();

        String userId = getUserId();
        if (userId == null)
            return null;

        Map<String, Object> lift = new HashMap<>();
        lift.put(Const.LIFT_OWNER, userId);
        liftDoc.set(lift);
        return liftDoc.getId();
    }



    @Nullable
    private String getUserId()
    {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth == null)
            return null;

        FirebaseUser user = auth.getCurrentUser();
        if (user == null)
            return null;

        return user.getUid();
    }



    // Toasts & snack bars ////////////////////////////////////////////////////////////////////////



    private void showAuthErrorSnackbar()
    {
        if (getView() == null)
            return;

        Snackbar.make(getView(), R.string.auth_error, Snackbar.LENGTH_INDEFINITE)
                .setAction(R.string.snackbar_ok, new View.OnClickListener() {
            @Override
            public void onClick(View view) {}
        });
    }



    // RecyclerView ///////////////////////////////////////////////////////////////////////////////



    private class LiftsAdapter extends RecyclerView.Adapter<LiftsAdapter.MyViewHolder> {


        @NonNull private ArrayList<Lift> m_lifts;



        private LiftsAdapter(@NonNull ArrayList<Lift> lifts)
        {
            m_lifts = lifts;
        }



        @NonNull
        @Override
        public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
        {
            CardView card = (CardView) getLayoutInflater().inflate(R.layout.card_view_route_stretch,
                    parent, false);
            return new MyViewHolder(card);
        }



        @Override
        public void onBindViewHolder(@NonNull MyViewHolder holder, int position)
        {
            Lift lift = m_lifts.get(position);
            holder.bind(lift.getFrom(), lift.getTo(), lift.getDepDate(), lift.getArrDate(),
                    lift.getPrice());
        }



        @Override
        public int getItemCount() {
            return m_lifts.size();
        }


        // holder /////////////////////////////////////////////////////////////////////////////////


        private class MyViewHolder extends RecyclerView.ViewHolder {

            private TextView m_from;
            private TextView m_to;
            private TextView m_depDate;
            private TextView m_arrDate;
            private TextView m_price;

            private MyViewHolder(CardView view)
            {
                super(view);
                m_from    = view.findViewById(R.id.card_view_route_stretch_tv_from);
                m_to      = view.findViewById(R.id.card_view_route_stretch_tv_to);
                m_depDate = view.findViewById(R.id.card_view_route_stretch_tv_date_time_from);
                m_arrDate = view.findViewById(R.id.card_view_route_stretch_tv_date_time_to);
                m_price   = view.findViewById(R.id.card_view_route_stretch_tv_price);
            }



            void bind(String from, String to, String depDate, String arrDate, String price)
            {
                m_from.setText(from);
                m_to.setText(to);
                m_depDate.setText(depDate);
                m_arrDate.setText(arrDate);
                m_price.setText(price);
            }
        }
    }
}
