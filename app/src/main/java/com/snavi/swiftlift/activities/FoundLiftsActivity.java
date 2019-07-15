package com.snavi.swiftlift.activities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.snavi.swiftlift.R;
import com.snavi.swiftlift.database_objects.Const;
import com.snavi.swiftlift.lift.FoundLift;
import com.snavi.swiftlift.lift.Lift;
import com.snavi.swiftlift.lift.Stretch;
import com.snavi.swiftlift.utils.Snackbars;
import com.snavi.swiftlift.utils.Toasts;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Currency;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

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
    private static final String NO_TO_RANGE_ERROR   = "Intent doesn't contain 'to range'!";
    private static final String NO_PRICE_ERROR      = "Intent doesn't contain 'price'!";
    private static final String NO_DEPARTURE_ERROR  = "Intent doesn't contain 'departure'";
    // format
    private static final String DATE_TIME_FORMAT = "dd/MM/YYYY HH:mm";
    // default
    private static final int DEF_SEARCH_PERIOD_IN_DAYS = 1;
    // other
    private static final String TAG = FoundLiftsActivity.class.getName();
    private static final double EARTH_CIRCUMFERENCE_KM = 40075;


    // fields //////////////////////////////////////////////////////////////////////////////////////
    /**
     * lifts found during search. Will be added asynchronously. RecyclerView's adapter also uses
     * this ArrayList. ITEMS SHOULD BE ALWAYS INSERTED VIA addFoundLift(FoundLift foundLift).
     */
    private ArrayList<FoundLift> m_foundLifts;
    private FoundLiftsAdapter m_adapter;
    private FirebaseFirestore m_db;
    private CollectionReference m_liftsCollection;
    // search parameters
    private LatLng  m_from;
    private LatLng  m_to;
    private Date    m_departure;    // date and time of departure
    private Date    m_maxDate;
    // ranges for from and to
    private double m_fromWest;      // max coordinates to west from 'from'
    private double m_fromNorth;     // max coordinates to north from 'from'
    private double m_fromEast;      // max coordinates to east from 'from'
    private double m_fromSouth;     // max coordinates to south from 'from'
    private double m_toWest;        // max coordinates to west from 'to'
    private double m_toNorth;       // max coordinates to north from 'to'
    private double m_toEast;        // max coordinates to east from 'to'
    private double m_toSouth;       // max coordinates to south from 'to'
    // query synchronization
    /**
     * flag that is accessed by queries via synchronized method didOtherQueryFinish(). When one of
     * queries(from or to) finishes it sets this flag as true, so that the second knows.
     */
    private boolean m_isReady;
    /**
     * when query that is looking for 'from stretches' finishes it puts the result here
     */
    private ArrayList<Stretch> m_fromStretches;
    /**
     * when query that is looking for 'to stretches' finishes it puts the result here
     */
    private ArrayList<Stretch> m_toStretches;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_found_lifts);

        m_foundLifts    = new ArrayList<>();
        m_fromStretches = new ArrayList<>();
        m_toStretches   = new ArrayList<>();
        m_db              = FirebaseFirestore.getInstance();
        m_liftsCollection = m_db.collection(Const.LIFTS_COLLECTION);
        initSearchParameters();
        search();
        initRecyclerView();
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

        m_departure = (Date) intent.getSerializableExtra(DEPARTURE_KEY);
        if (m_departure == null)
            throw new RuntimeException(NO_DEPARTURE_ERROR);

        m_maxDate = (Date) intent.getSerializableExtra(MAX_DATE_KEY);
        if (m_maxDate == null)
        {
            Calendar c = Calendar.getInstance();
            c.add(Calendar.DATE, DEF_SEARCH_PERIOD_IN_DAYS);
            m_maxDate = c.getTime();
        }
    }



    private void initRanges(Intent intent)
    {
        double fromRange = (double) intent.getIntExtra(FROM_RANGE_KEY, -1);
        if (fromRange < 0)
            throw new RuntimeException(NO_FROM_RANGE_ERROR);

        Double fromWest  = addLongitudeToCoordinates(m_from, -fromRange);
        Double fromNorth = addLatitudeToCoordinates(m_from, fromRange);
        Double fromEast  = addLongitudeToCoordinates(m_from, fromRange);
        Double fromSouth = addLatitudeToCoordinates(m_from, -fromRange);

        if (fromWest == null || fromNorth == null || fromEast == null || fromSouth == null)
        {
            Snackbars.showUnsupportedLocSnackbar(this,
                    findViewById(R.id.activity_found_lifts_cv));
            return;
        }

        m_fromWest  = fromWest;
        m_fromNorth = fromNorth;
        m_fromEast  = fromEast;
        m_fromSouth = fromSouth;

        double toRange = (double) intent.getIntExtra(TO_RANGE_KEY, -1);
        if (toRange < 0)
            throw new RuntimeException(TO_RANGE_KEY);

        Double toWest  = addLongitudeToCoordinates(m_to, -fromRange);
        Double toNorth = addLatitudeToCoordinates(m_to, fromRange);
        Double toEast  = addLongitudeToCoordinates(m_to, fromRange);
        Double toSouth = addLatitudeToCoordinates(m_to, -fromRange);

        if (toWest == null || toNorth == null || toEast == null || toSouth == null)
        {
            Snackbars.showUnsupportedLocSnackbar(this,
                    findViewById(R.id.activity_found_lifts_cv));
            return;
        }

        m_toWest  = toWest;
        m_toNorth = toNorth;
        m_toEast  = toEast;
        m_toSouth = toSouth;
    }



    private void search()
    {
        selectFromStretches();
        selectToStretches();
    }


    // TODO firebase do not allow whereLess/more on more than one field
    /**
     * method performs query that is choosing stretches which have properties appropriate for
     * departure. When query is done it calls synchronized didOtherQueryFinish(), which returns true
     * if selectToStretches finished first and false if this method finished first. If
     * this was first it does nothing. If this method was second it calls another method which
     * is responsible for connecting both.
     */
    private void selectFromStretches()
    {
        m_db.collection(Const.STRETCHES_COLLECTION)
                .whereGreaterThanOrEqualTo(Const.STRETCH_DEP, m_departure)
                .whereLessThanOrEqualTo(Const.STRETCH_DEP, m_maxDate)
                .whereGreaterThanOrEqualTo(Const.STRETCH_FROM_LON, m_fromWest)
                .whereLessThanOrEqualTo(Const.STRETCH_FROM_LAT, m_fromNorth)
                .whereLessThanOrEqualTo(Const.STRETCH_FROM_LON, m_fromEast)
                .whereGreaterThanOrEqualTo(Const.STRETCH_FROM_LAT, m_fromSouth)
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots)
                    {
                        loadFromStretches(queryDocumentSnapshots.getDocuments());
                        if (didOtherQueryFinish())
                            combineResults();

                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, e.getMessage());
                        Toasts.showUnknownErrorToast(FoundLiftsActivity.this);
                    }
                });
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
        m_db.collection(Const.STRETCHES_COLLECTION)
                .whereGreaterThanOrEqualTo(Const.STRETCH_DEP, m_departure)
                .whereGreaterThanOrEqualTo(Const.STRETCH_TO_LON, m_toWest)
                .whereLessThanOrEqualTo(Const.STRETCH_TO_LAT, m_toNorth)
                .whereLessThanOrEqualTo(Const.STRETCH_TO_LON, m_toEast)
                .whereGreaterThanOrEqualTo(Const.STRETCH_TO_LON, m_toSouth)
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots)
                    {
                        loadToStretches(queryDocumentSnapshots.getDocuments());
                        if (didOtherQueryFinish())
                            combineResults();

                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, e.getMessage());
                        Toasts.showUnknownErrorToast(FoundLiftsActivity.this);
                    }
                });
    }



    private void loadFromStretches(List<DocumentSnapshot> docs)
    {
        Currency defaultCurrency = Currency.getInstance(Locale.getDefault());                       // for now default currency, most of the stretches
        for (DocumentSnapshot doc : docs)                                                           // will be dismissed anyway
            m_fromStretches.add(Stretch.loadFromDoc(doc, defaultCurrency, doc.getId()));
    }



    private void loadToStretches(List<DocumentSnapshot> docs)
    {
        Currency defaultCurrency = Currency.getInstance(Locale.getDefault());                       // for now default currency, most of the stretches
        for (DocumentSnapshot doc : docs)                                                           // will be dismissed anyway
            m_toStretches.add(Stretch.loadFromDoc(doc, defaultCurrency, doc.getId()));
    }



    synchronized private boolean didOtherQueryFinish()
    {
        if (m_isReady)          // the previous query has already finished
            return true;
        else                    // the previous query is not yet finished. Set flag to true, so that the other query knows that this one is ready
        {
            m_isReady = true;
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
        for (Stretch toStretch : toStretches)
        {
            for (Stretch fromStretch : m_fromStretches)
            {
                if (fromStretch.getLiftId().equals(toStretch.getLiftId()))
                    createFoundLift(fromStretch, toStretch);
            }
        }
    }



    private void createFoundLift(final Stretch fromStretch, final Stretch toStretch)
    {
        if (fromStretch.getArrDate().after(toStretch.getDepDate()))    // if fromStretch was after toStretch skip this pair
            return;

        m_liftsCollection.document(fromStretch.getLiftId()).get().addOnSuccessListener(
                new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot)
                    {
                        // TODO check what happens if adapter gets outOfBounds idx in notifyItemChanged
                        FoundLift foundLift = FoundLift.loadFromDoc(documentSnapshot, m_adapter,
                                m_foundLifts.size(), fromStretch, toStretch);
                        if (foundLift != null)
                            addFoundLift(foundLift);
                    }
                }
        ).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.e(TAG, e.getMessage());
                Toasts.showUnknownErrorToast(FoundLiftsActivity.this);
            }
        });
    }



    private void addFoundLift(@NonNull FoundLift foundLift)
    {
        m_foundLifts.add(foundLift);
        m_adapter.notifyItemChanged(m_foundLifts.size() - 1);
    }



    private void initRecyclerView()
    {
        m_adapter = new FoundLiftsAdapter();
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
        RecyclerView rv = findViewById(R.id.activity_found_lifts_rv);

        rv.setAdapter(m_adapter);
        rv.setLayoutManager(layoutManager);
        rv.setHasFixedSize(true);
    }



    // coordinates calculations ////////////////////////////////////////////////////////////////////


    /**
     *
     * @param loc LatLng to which distance will be added
     * @param distKm how much distance to add to loc
     * @return latitude increased or decreased by distKm OR NULL when after adding the coordinates
     * crossed North Pole or South Pole. Why null? Because I can't effectively search
     * in firebase database for these results.
     */
    @Nullable
    private Double addLatitudeToCoordinates(LatLng loc, Double distKm)
    {
        double degrees = distKm / EARTH_CIRCUMFERENCE_KM * 360;
        double res = loc.latitude + degrees;
        if (res >= -90 && res <= 90)
            return res;
        else
            return null;
    }



    /**
     *
     * @param loc LatLng to which distance will be added
     * @param distKm how much distance to add to loc
     * @return latitude increased or decreased by distKm OR NULL when after adding the coordinates
     * 180 meridian. Why null? Because I can't effectively search
     * in firebase database for these results.
     */
    @Nullable
    private Double addLongitudeToCoordinates(LatLng loc, Double distKm)
    {
        double degrees = distKm / EARTH_CIRCUMFERENCE_KM * 360;
        double res = loc.longitude + degrees;
        if (res >= -180 && res <= 180)
            return res;
        else
            return null;
    }



    // RecyclerView ////////////////////////////////////////////////////////////////////////////////



    private class FoundLiftsAdapter extends RecyclerView.Adapter<FoundLiftsAdapter.ViewHolder> {


        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
        {
            LayoutInflater inflater = LayoutInflater.from(FoundLiftsActivity.this);
            CardView card = (CardView) inflater.inflate(R.layout.card_view_found_lift, parent);
            return new ViewHolder(card);
        }



        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position)
        {
            FoundLift fLift = m_foundLifts.get(position);

            holder.bind(fLift.getFrom(), fLift.getDepDate(), fLift.getTo(), fLift.getArrDate(),
                    fLift.getPrice());
        }



        @Override
        public int getItemCount()
        {
            return m_foundLifts.size();
        }


        // ViewHolder //////////////////////////////////////////////////////////////////////////////



        private class ViewHolder extends RecyclerView.ViewHolder {

            @NonNull private TextView m_tvFrom;
            @NonNull private TextView m_tvDeparture;
            @NonNull private TextView m_tvTo;
            @NonNull private TextView m_tvArrival;
            @NonNull private TextView m_tvPrice;

            private ViewHolder(@NonNull CardView card)
            {
                super(card);

                m_tvFrom = card.findViewById(R.id.card_view_found_lift_tv_from);
                m_tvDeparture = card.findViewById(R.id.card_view_found_lift_tv_dep);
                m_tvTo = card.findViewById(R.id.card_view_found_lift_tv_to);
                m_tvArrival = card.findViewById(R.id.card_view_found_lift_tv_arr);
                m_tvPrice = card.findViewById(R.id.card_view_found_lift_tv_price);
            }



            private void bind(@NonNull String from, @NonNull String departureDate,
                              @NonNull String to, @NonNull String arrivalDate,
                              @NonNull String price)
            {
                m_tvFrom.setText(from);
                m_tvDeparture.setText(departureDate);
                m_tvTo.setText(to);
                m_tvArrival.setText(arrivalDate);
                m_tvPrice.setText(price);

            }
        }
    }
}
