package com.snavi.swiftlift.signed_in_fragments;

import android.app.Activity;
import android.content.Context;
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
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;
import com.snavi.swiftlift.R;
import com.snavi.swiftlift.activities.LiftActivity;
import com.snavi.swiftlift.database_objects.Const;
import com.snavi.swiftlift.lift.Lift;
import com.snavi.swiftlift.lift.Stretch;
import com.snavi.swiftlift.utils.InternetUtils;
import com.snavi.swiftlift.utils.Toasts;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Currency;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DriverFragment extends Fragment {


    // CONST //////////////////////////////////////////////////////////////////////////////////////
    // request codes
    private static final int CREATE_LIFT_REQ_CODE = 7831;
    private static final int EDIT_LIFT_REQ_CODE   = 7832;
    // errors
    private static final String NULL_INTENT_ERROR = "Null data intent in onActivityResult. LiftActivity must return intent";
    // other
    private static final int INIT_LIFTS_LOAD_TRIAL_LEFT = 7;
    private static final String TAG                     = DriverFragment.class.getName();


    // fields /////////////////////////////////////////////////////////////////////////////////////
    private LiftsAdapter        m_adapter;
    private FirebaseFirestore   m_db;
    private CollectionReference m_liftsCollection;
    private CollectionReference m_stretchesCollection;
    private ProgressBar m_progressBar;
    /**
     * when loading lifts failed it will be repeated if m_liftsLoadTrialsLeft is greater than 0.
     * It will decrease by one during each trial.
     */
    private int m_liftsLoadTrialsLeft;


    public DriverFragment() {
        // Required empty public constructor
    }


    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        m_db                  = FirebaseFirestore.getInstance();
        m_liftsCollection     = m_db.collection(Const.LIFTS_COLLECTION);
        m_stretchesCollection = m_db.collection(Const.STRETCHES_COLLECTION);
        m_liftsLoadTrialsLeft = INIT_LIFTS_LOAD_TRIAL_LEFT;
    }



    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        View view = inflater.inflate(R.layout.fragment_driver, container, false);

        ArrayList<Lift> lifts = new ArrayList<>();
        initRecyclerView(view, lifts);

        m_progressBar = view.findViewById(R.id.fragment_driver_progress_bar);

        setAddLiftButtonListener(view);
        loadLifts();

        return view;
    }



    private void initRecyclerView(View view, ArrayList<Lift> lifts)
    {
        RecyclerView rv = view.findViewById(R.id.fragment_driver_rv);
        m_adapter = new LiftsAdapter(lifts);
        RecyclerView.LayoutManager manager = new LinearLayoutManager(getContext());
        rv.setAdapter(m_adapter);
        rv.setLayoutManager(manager);
    }



    private void loadLifts()
    {
        String userId = getUserId();
        if (userId == null)
        {
            showAuthErrorSnackbar();
            m_progressBar.setVisibility(View.GONE);
            return;
        }

        m_liftsCollection.whereEqualTo(Const.LIFT_OWNER, userId).get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots)
                    {
                        List<DocumentSnapshot> docs = queryDocumentSnapshots.getDocuments();
                        Lift lift;
                        for (DocumentSnapshot doc : docs)
                        {
                            lift = Lift.loadFromDoc(doc);
                            if (lift != null)
                                addLift(lift);
                            else
                                Toasts.showLiftLoadErrorToast(getContext());
                        }
                        m_progressBar.setVisibility(View.GONE);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e)
                    {
                        Log.e(TAG, e.getMessage());
                        if (m_liftsLoadTrialsLeft > 0)
                        {
                            m_liftsLoadTrialsLeft--;
                            loadLifts();
                        }
                        else
                            Toasts.showLiftsLoadErrorToast(getContext());
                    }
                });

    }



    private void addLift(Lift lift)
    {
        m_adapter.m_lifts.add(lift);
        lift.loadStretchesFromDb(m_adapter, m_adapter.m_lifts.size() - 1);
        m_adapter.notifyItemChanged(m_adapter.m_lifts.size() - 1);
    }



    private void setAddLiftButtonListener(View view)
    {
        Button but = view.findViewById(R.id.fragment_driver_but_add_lift);
        but.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Context context = getContext();
                if (context == null)
                    return;

                if (!InternetUtils.hasInternetConnection(context))
                {
                    Toasts.showNetworkErrorToast(context);
                    return;
                }

                Lift lift = createNewLift();
                if (lift == null)                   // because userId was not available
                {
                    showAuthErrorSnackbar();
                    return;
                }
                Intent intent = new Intent(getContext(), LiftActivity.class);
                intent.putExtra(LiftActivity.LIFT_KEY, lift);
                startActivityForResult(intent, CREATE_LIFT_REQ_CODE);
            }
        });
    }



    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode)
        {
            case CREATE_LIFT_REQ_CODE : dealWithCreateLiftResult(resultCode, data);
            case EDIT_LIFT_REQ_CODE   : dealWithEditLiftResult(resultCode, data);
        }
    }



    private void dealWithCreateLiftResult(int resCode, Intent data)
    {
        if (data == null)
            throw new RuntimeException(NULL_INTENT_ERROR);

        Lift lift = data.getParcelableExtra(LiftActivity.RESULT_LIFT_KEY);
        if (resCode == Activity.RESULT_CANCELED)
            deleteLift(lift.getId());
        else
            addLiftToRecyclerView(lift);
    }



    private void dealWithEditLiftResult(int resCode, Intent data)
    {
        if (data == null)
            throw new RuntimeException(NULL_INTENT_ERROR);

        Lift lift = data.getParcelableExtra(LiftActivity.RESULT_LIFT_KEY);
        if (resCode == Activity.RESULT_CANCELED)
            deleteLift(lift.getId());
        else
            m_adapter.replace(lift);
    }



    private void deleteLift(String liftId)
    {
        m_liftsCollection.document(liftId).delete();

        ArrayList<Lift> lifts = m_adapter.m_lifts;
        for (int i = 0; i < lifts.size(); ++i)
        {
            if (lifts.get(i).getId().equals(liftId))
            {
                lifts.remove(i);
                m_adapter.notifyItemChanged(i);
                return;
            }
        }
    }



    private void addLiftToRecyclerView(Lift lift)
    {
        m_adapter.m_lifts.add(lift);
        m_adapter.notifyItemChanged(m_adapter.m_lifts.size() - 1);
    }



    /**
     *
     * @return lift id
     */
    private Lift createNewLift()
    {
        DocumentReference liftDoc = m_liftsCollection.document();

        String userId = getUserId();
        if (userId == null)
            return null;

        Lift lift = new Lift(new ArrayList<Stretch>(), Currency.getInstance(Locale.getDefault()),
                liftDoc.getId(), userId);
        liftDoc.set(lift.getFirestoreObject());

        return lift;
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



    public class LiftsAdapter extends RecyclerView.Adapter<LiftsAdapter.MyViewHolder> {


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
            holder.bind(lift.getFrom(), lift.getTo(), lift.getDepDateString(), lift.getArrDateString(),
                    lift.getPrice(), lift.getId());

            if (getActivity() == null)
                return;

            TextView tvNoLifts = getActivity().findViewById(R.id.fragment_driver_tv_no_lifts);
            if (!m_lifts.isEmpty())
                tvNoLifts.setVisibility(View.GONE);
        }



        @Override
        public int getItemCount() {
            return m_lifts.size();
        }



        private void replace(Lift lift)
        {
            for (int i = 0; i < m_lifts.size(); i++)
            {
                if (m_lifts.get(i).getId().equals(lift.getId()))
                {
                    m_lifts.set(i, lift);
                    notifyItemChanged(i);
                    return;
                }
            }
        }



        public void sort()
        {
            Collections.sort(m_lifts, new Lift.DepDateAscending());
            notifyDataSetChanged();
        }


        // holder /////////////////////////////////////////////////////////////////////////////////


        private class MyViewHolder extends RecyclerView.ViewHolder {

            private TextView m_from;
            private TextView m_to;
            private TextView m_depDate;
            private TextView m_arrDate;
            private TextView m_price;
            private ImageButton m_deleteButton;
            private ImageButton m_editButton;

            private MyViewHolder(CardView view)
            {
                super(view);
                m_from    = view.findViewById(R.id.card_view_route_stretch_tv_from);
                m_to      = view.findViewById(R.id.card_view_route_stretch_tv_to);
                m_depDate = view.findViewById(R.id.card_view_route_stretch_tv_date_time_from);
                m_arrDate = view.findViewById(R.id.card_view_route_stretch_tv_date_time_to);
                m_price   = view.findViewById(R.id.card_view_route_stretch_tv_price);
                m_deleteButton = view.findViewById(R.id.card_view_route_stretch_but_delete);
                m_editButton   = view.findViewById(R.id.card_view_route_stretch_but_edit);
            }



            void bind(String from, String to, String depDate, String arrDate, String price,
                      String liftId)
            {
                m_from.setText(from);
                m_to.setText(to);
                m_depDate.setText(depDate);
                m_arrDate.setText(arrDate);
                m_price.setText(price);

                setButtonsListener(liftId);
            }



            private void setButtonsListener(final String liftId)
            {
                setDeleteButtonListener(liftId);
                setEditButtonListener();
            }



            private void setDeleteButtonListener(final String liftId)
            {
                m_deleteButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view)
                    {
                        Context context = DriverFragment.this.getContext();
                        if (context == null)
                            return;

                        if (InternetUtils.hasInternetConnection(context))
                        {
                            m_liftsCollection.document(liftId).delete();
                            m_lifts.remove(getAdapterPosition());
                            LiftsAdapter.this.notifyDataSetChanged();
                            deleteStretches(liftId);
                        }
                        else
                            Toasts.showNetworkErrorToast(DriverFragment.this.getContext());
                    }
                });
            }



            private void deleteStretches(String liftId)
            {
                m_stretchesCollection.whereEqualTo(Const.STRETCH_LIFT_ID, liftId).get()
                        .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>()
                        {
                            @Override
                            public void onSuccess(QuerySnapshot queryDocumentSnapshots)
                            {
                                List<DocumentSnapshot> snapshots = queryDocumentSnapshots
                                        .getDocuments();

                                WriteBatch batch = m_db.batch();

                                for (DocumentSnapshot snapshot : snapshots)
                                {
                                    batch.delete(snapshot.getReference());
                                }

                                batch.commit();
                            }
                });
            }



            private void setEditButtonListener()
            {
                m_editButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view)
                    {
                        Intent intent = new Intent(DriverFragment.this.getContext(),
                                LiftActivity.class);
                        Lift lift = m_lifts.get(getAdapterPosition());
                        intent.putExtra(LiftActivity.STRETCHES_ARRAY_KEY, lift.getStretches());
                        intent.putExtra(LiftActivity.LIFT_KEY, lift);

                        startActivityForResult(intent, EDIT_LIFT_REQ_CODE);
                    }
                });
            }
        }
    }
}
