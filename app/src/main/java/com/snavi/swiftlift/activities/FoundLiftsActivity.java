package com.snavi.swiftlift.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.snavi.swiftlift.R;
import com.snavi.swiftlift.database_objects.Const;
import com.snavi.swiftlift.lift.FoundLift;
import com.snavi.swiftlift.lift.Stretch;
import com.snavi.swiftlift.searching.CellCreator;
import com.snavi.swiftlift.utils.Toasts;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Currency;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;

// TODO sort lifts
public class FoundLiftsActivity extends AppCompatActivity {

    // CONST ///////////////////////////////////////////////////////////////////////////////////////
    // keys
    public static final String FROM_KEY         = "f";
    public static final String FROM_RANGE_KEY   = "fr";
    public static final String TO_KEY           = "t";
    public static final String TO_RANGE_KEY     = "tk";
    public static final String DEPARTURE_KEY    = "d";
    public static final String MAX_DATE_KEY     = "md";
    // errors
    private static final String NO_FROM_ERROR       = "Intent doesn't contain 'from' coordinates!";
    private static final String NO_TO_ERROR         = "Intent doesn't contain 'to' coordinates!";
    private static final String NO_FROM_RANGE_ERROR = "Intent doesn't contain 'from range'!";
    private static final String NO_DEPARTURE_ERROR  = "Intent doesn't contain 'departure'";
    // default
    private static final int DEF_SEARCH_PERIOD_IN_DAYS = 3;
    // other
    private static final String TAG = FoundLiftsActivity.class.getName();


    // fields //////////////////////////////////////////////////////////////////////////////////////
    /**
     * lifts found during search. Will be added asynchronously. RecyclerView's adapter also uses
     * this ArrayList. ITEMS SHOULD BE ALWAYS INSERTED VIA addFoundLift(FoundLift foundLift).
     */
    private List<FoundLift> m_foundLifts;
    private FoundLiftsAdapter m_adapter;
    private CollectionReference m_liftsCollection;
    private CollectionReference m_stretchesCollection;
    // search parameters
    private LatLng  m_from;
    private LatLng  m_to;
    private Date    m_maxDate;
    private Date    m_departure;
    private long[]  m_fromCellSurroundings;
    private long[]  m_toCellSurroundings;
    private int     m_fromRangeKm;
    private int     m_toRangeKm;
    // query synchronization
    /**
     * because stretches are searched only in one cell per query some synchronization was
     * necessary. Whenever some 'from query' for one cell finishes this field is incremented by 1.
     * When this field is equal to m_fromCellSurroundings.length then all queries for 'from
     * stretches' are ready.
     */
    private int m_fromQueryCounter;
    /**
     * because stretches are searched only in one cell per query some synchronization was
     * necessary. Whenever some 'to query' for one cell finishes this field is incremented by 1.
     * When this field is equal to m_toCellSurroundings.length then all queries for 'from
     * stretches' are ready.
     */
    private int m_toQueryCounter;
    /**
     * when 'from query' or 'to query' finishes, then this flag is set to true, so that when the
     * other query finishes it knows that it must start further processing (connecting from
     * stretches with to stretches)
     */
    private boolean m_didOtherQueryFinish;
    /**
     * when query that is looking for 'from stretches' finishes it puts the result here
     */
    private ArrayList<Stretch> m_fromStretches;
    /**
     * when query that is looking for 'to stretches' finishes it puts the result here
     */
    private ArrayList<Stretch> m_toStretches;
    /**
     * once stretches are found lifts needs to be loaded. It's initial size is the number of lifts
     * found. When all lifts are loaded the progress bar should disappear, it will do so, when this
     * variable is equal to 0
     */
    private int m_leftLiftsToLoad;
    private int m_numOfFoundLifts;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_found_lifts);

        m_foundLifts            = new LinkedList<>();
        m_fromStretches         = new ArrayList<>();
        m_toStretches           = new ArrayList<>();
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        m_liftsCollection       = db.collection(Const.LIFTS_COLLECTION);
        m_stretchesCollection   = db.collection(Const.STRETCHES_COLLECTION);
        m_fromQueryCounter      = 0;
        m_toQueryCounter        = 0;
        m_leftLiftsToLoad       = 0;
        m_numOfFoundLifts       = 0;
        initSearchParameters();
        initCells();
        initRecyclerView();
        search();
    }



    private void initSearchParameters()
    {
        Intent intent = getIntent();

        m_from = intent.getParcelableExtra(FROM_KEY);
        if (m_from == null)
            throw new RuntimeException(NO_FROM_ERROR);

        m_to = intent.getParcelableExtra(TO_KEY);
        if (m_to == null)
            throw new RuntimeException(NO_TO_ERROR);

        initRanges(intent);

        // date and time of departure
        m_departure = (Date) intent.getSerializableExtra(DEPARTURE_KEY);
        if (m_departure == null)
            throw new RuntimeException(NO_DEPARTURE_ERROR);

        m_maxDate = (Date) intent.getSerializableExtra(MAX_DATE_KEY);
        if (m_maxDate == null)
        {
            Calendar c = Calendar.getInstance();
            c.setTime(m_departure);
            c.add(Calendar.DATE, DEF_SEARCH_PERIOD_IN_DAYS);
            m_maxDate = c.getTime();
        }
    }



    private void initRanges(Intent intent)
    {
        m_fromRangeKm = intent.getIntExtra(FROM_RANGE_KEY, -1);
        if (m_fromRangeKm < 0)
            throw new RuntimeException(NO_FROM_RANGE_ERROR);

        m_toRangeKm = intent.getIntExtra(TO_RANGE_KEY, -1);
        if (m_toRangeKm < 0)
            throw new RuntimeException(TO_RANGE_KEY);
    }



    private void initCells()
    {
        long fromCell = CellCreator.assignCell(m_from.latitude, m_from.longitude);
        m_fromCellSurroundings  = CellCreator.getSearchedCells(
                fromCell,
                CellCreator.getRange(m_fromRangeKm)
        );

        long toCell = CellCreator.assignCell(m_to.latitude, m_to.longitude);
        m_toCellSurroundings    = CellCreator.getSearchedCells(
                toCell,
                CellCreator.getRange(m_toRangeKm));
    }



    private void initRecyclerView()
    {
        m_adapter = new FoundLiftsAdapter();
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
        RecyclerView rv = findViewById(R.id.activity_found_lifts_rv);

        rv.setAdapter(m_adapter);
        rv.setLayoutManager(layoutManager);
        rv.setHasFixedSize(false);
    }



    private void search()
    {
        selectFromStretches();
        selectToStretches();
    }


    /**
     * method performs query that is choosing stretches which have properties appropriate for
     * departure. When query is done it calls synchronized didOtherQueryFinish(), which returns true
     * if selectToStretches finished first and false if this method finished first. If
     * this was first it does nothing. If this method was second it calls another method which
     * is responsible for connecting both.
     */
    private void selectFromStretches()
    {
        for (long cell : m_fromCellSurroundings)
        {
            m_stretchesCollection
                    .whereEqualTo(Const.STRETCH_FROM_CELL, cell)
                    .whereGreaterThan(Const.STRETCH_DEP, m_departure)
                    .whereLessThan(Const.STRETCH_DEP, m_maxDate)
                    .get()
                    .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                        @Override
                        public void onSuccess(QuerySnapshot queryDocumentSnapshots)
                        {
                            List<DocumentSnapshot> docs = queryDocumentSnapshots.getDocuments();
                            loadFromStretches(docs);
                            if (isFromQueryReady())
                            {
                                if (didOtherQueryFinish())
                                {
                                    combineResults();
                                }
                            }
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e)
                        {
                            Log.e(TAG, e.getMessage());
                            Toasts.showSearchErrorToast(FoundLiftsActivity.this);
                        }
                    });
        }
    }



    synchronized private boolean isFromQueryReady()
    {
        m_fromQueryCounter++;

        return m_fromQueryCounter == m_fromCellSurroundings.length;
    }



    /**
     * method performs query that is choosing stretches which have properties appropriate for
     * arrival. When query is done it calls synchronized didOtherQueryFinish(), which returns true
     * if selectToStretches finished first and false if this method finished first. If
     * this was first it does nothing. If this method was second it calls another method which
     * is responsible for connecting both.
     */
    private void selectToStretches()
    {
        for (long cell : m_toCellSurroundings)
        {
            m_stretchesCollection.whereEqualTo(Const.STRETCH_TO_CELL, cell)
                    .whereGreaterThan(Const.STRETCH_DEP, m_departure)
                    .whereLessThan(Const.STRETCH_DEP, m_maxDate).get()
                    .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                        @Override
                        public void onSuccess(QuerySnapshot queryDocumentSnapshots)
                        {
                            List<DocumentSnapshot> docs = queryDocumentSnapshots.getDocuments();
                            loadToStretches(docs);
                            if (isToQueryReady())
                            {
                                if (didOtherQueryFinish())
                                {
                                    combineResults();
                                }
                            }
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e)
                        {
                            Log.e(TAG, e.getMessage());
                            Toasts.showSearchErrorToast(FoundLiftsActivity.this);
                        }
                    });
        }
    }



    synchronized private boolean isToQueryReady()
    {
        m_toQueryCounter++;

        return m_toQueryCounter == m_toCellSurroundings.length;
    }



    synchronized private void loadFromStretches(List<DocumentSnapshot> docs)
    {
        Currency defaultCurrency = Currency.getInstance(Locale.getDefault());                       // for now default currency, most of the stretches
        for (DocumentSnapshot doc : docs)                                                           // will be dismissed anyway
            m_fromStretches.add(Stretch.loadFromDoc(doc, defaultCurrency, doc.getId()));
    }



    synchronized private void loadToStretches(List<DocumentSnapshot> docs)
    {
        Currency defaultCurrency = Currency.getInstance(Locale.getDefault());                       // for now default currency, most of the stretches
        for (DocumentSnapshot doc : docs)                                                           // will be dismissed anyway
            m_toStretches.add(Stretch.loadFromDoc(doc, defaultCurrency, doc.getId()));
    }



    synchronized private boolean didOtherQueryFinish()
    {
        if (m_didOtherQueryFinish)  // the previous query has already finished
            return true;
        else                        // the previous query is not yet finished. Set flag to true, so that the other query knows that this one is ready
        {
            m_didOtherQueryFinish = true;
            return false;
        }
    }


    /**
     * when both queries are ready this method is called. It combines from stretches and to
     * stretches and creates foundLifts
     */
    private void combineResults()
    {
        HashSet<String> fromLiftIds = new HashSet<>(); // set that will contain ids of all lifts that contain at least one matching from stretch
        for (Stretch fromStretch : m_fromStretches)
            fromLiftIds.add(fromStretch.getLiftId());

        ArrayList<Stretch> toStretchesWithCovering = new ArrayList<>();  // to stretches that has appropriate from stretch
        for (Stretch toStretch : m_toStretches)
        {
            if (fromLiftIds.contains(toStretch.getLiftId()))
                toStretchesWithCovering.add(toStretch);
        }

        createFoundLifts(toStretchesWithCovering);
    }



    private void createFoundLifts(ArrayList<Stretch> toStretches)
    {
        ArrayList<Stretch> fromStretchesOk    = new ArrayList<>();
        ArrayList<Stretch> toStretchesOk      = new ArrayList<>();

        for (Stretch toStretch : toStretches)
        {
            for (Stretch fromStretch : m_fromStretches)
            {
                if (fromStretch.getLiftId().equals(toStretch.getLiftId())
                        && !fromStretch.getDepDate().after(toStretch.getDepDate()))                 // fromStretch must be before toStretch
                {
                    m_leftLiftsToLoad++;
                    fromStretchesOk.add(fromStretch);
                    toStretchesOk.add(toStretch);
                }
            }
        }

        if (m_leftLiftsToLoad == 0)
            finishSearching();

        for (int i = 0; i < fromStretchesOk.size(); i++)
        {
            createFoundLift(fromStretchesOk.get(i), toStretchesOk.get(i));
        }
    }



    private void finishSearching()
    {
        View progressBar = findViewById(R.id.activity_found_lifts_progress_bar);
        if (m_leftLiftsToLoad == 0)
        {
            progressBar.setVisibility(View.GONE);

            if (m_numOfFoundLifts > 0)
            {
                Toasts.showSearchCompletedToast(FoundLiftsActivity.this,
                        m_numOfFoundLifts);
            }
            else
            {
                findViewById(R.id.activity_found_lifts_tv_no_lifts_found)
                        .setVisibility(View.VISIBLE);
            }
        }
    }



    private void createFoundLift(final Stretch fromStretch, final Stretch toStretch)
    {
        m_liftsCollection.document(fromStretch.getLiftId()).get().addOnSuccessListener(
                new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot)
                    {
                        // TODO check what happens if adapter gets outOfBounds idx in notifyItemChanged
                        FoundLift foundLift = FoundLift.loadFromDoc(documentSnapshot, m_adapter,
                                m_foundLifts.size(), fromStretch, toStretch);
                        if (foundLift != null)
                        {
                            addFoundLift(foundLift);
                            m_numOfFoundLifts++;
                        }
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, e.getMessage());
                        Toasts.showUnknownErrorToast(FoundLiftsActivity.this);
                    }
                })
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task)
                    {
                        decrementLiftsLeft();
                        if (m_leftLiftsToLoad == 0)
                            finishSearching();
                    }
                });
    }



    synchronized void decrementLiftsLeft()
    {
        m_leftLiftsToLoad--;
    }



    private void addFoundLift(@NonNull FoundLift foundLift)
    {
        ListIterator<FoundLift> it = m_foundLifts.listIterator();
        FoundLift curr;
        while(it.hasNext())
        {
            curr = it.next();

            if (foundLift.getArrDate().before(curr.getArrDate()))
            {
                it.previous();
                it.add(foundLift);
                m_adapter.notifyDataSetChanged();
                return;
            }
        }

        // if foundLift has the latest arrival date
        m_foundLifts.add(foundLift);
        m_adapter.notifyDataSetChanged();
    }



    // RecyclerView ////////////////////////////////////////////////////////////////////////////////



    private class FoundLiftsAdapter extends RecyclerView.Adapter<FoundLiftsAdapter.ViewHolder> {


        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
        {
            LayoutInflater inflater = LayoutInflater.from(FoundLiftsActivity.this);
            CardView card = (CardView) inflater.inflate(R.layout.card_view_route, parent,
                    false);
            return new ViewHolder(card);
        }



        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position)
        {
            FoundLift fLift = m_foundLifts.get(position);

            holder.bind(
                    fLift.getDepPostCode() + " " + fLift.getDepCity(),
                    fLift.getDepStreet() + " " + fLift.getDepHouseNum(),
                    fLift.getDepDateString(),
                    fLift.getArrPostCode() + " " + fLift.getArrCity(),
                    fLift.getArrStreet() + " " + fLift.getArrHouseNum(),
                    fLift.getArrDateString(),
                    fLift.getPrice()
                    );
        }



        @Override
        public int getItemCount()
        {
            return m_foundLifts.size();
        }


        // ViewHolder //////////////////////////////////////////////////////////////////////////////



        private class ViewHolder extends RecyclerView.ViewHolder {

            @NonNull private CardView m_card;
            @NonNull private TextView m_tvDepCity;
            @NonNull private TextView m_tvDepStreet;
            @NonNull private TextView m_tvDepTime;
            @NonNull private TextView m_tvArrCity;
            @NonNull private TextView m_tvArrStreet;
            @NonNull private TextView m_tvArrTime;
            @NonNull private TextView m_tvPrice;

            private ViewHolder(@NonNull CardView card)
            {
                super(card);

                m_card          = card;
                m_tvDepCity     = card.findViewById(R.id.card_view_route_stretch_tv_departure_city);
                m_tvDepStreet   = card.findViewById(R.id.card_view_route_stretch_tv_departure_street);
                m_tvDepTime     = card.findViewById(R.id.card_view_route_stretch_tv_departure_time);
                m_tvArrCity     = card.findViewById(R.id.card_view_route_stretch_tv_arrival_city);
                m_tvArrStreet   = card.findViewById(R.id.card_view_route_stretch_tv_arrival_street);
                m_tvArrTime     = card.findViewById(R.id.card_view_route_stretch_tv_arrival_time);
                m_tvPrice       = card.findViewById(R.id.card_view_route_stretch_tv_price);
            }



            private void bind(@NonNull String departureCity, @NonNull String departureStreet,
                              @NonNull String departureTime, @NonNull String arrivalCity,
                              @NonNull String arrivalStreet, @NonNull String arrivalTime,
                              @NonNull String price)
            {
                m_tvDepCity.setText(departureCity);
                m_tvDepStreet.setText(departureStreet);
                m_tvDepTime.setText(departureTime);
                m_tvArrCity.setText(arrivalCity);
                m_tvArrStreet.setText(arrivalStreet);
                m_tvArrTime.setText(arrivalTime);
                m_tvPrice.setText(price);

                setOnClickListener();
            }



            private void setOnClickListener()
            {
                m_card.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view)
                    {
                        Intent intent = new Intent(FoundLiftsActivity.this,
                                FoundLiftDetailsActivity.class);

                        intent.putExtra(FoundLiftDetailsActivity.LIFT_KEY,
                                m_foundLifts.get(getAdapterPosition()));

                        startActivity(intent);
                    }
                });
            }
        }
    }
}
