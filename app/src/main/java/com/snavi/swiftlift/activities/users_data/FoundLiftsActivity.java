package com.snavi.swiftlift.activities.users_data;

import androidx.annotation.NonNull;
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
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.snavi.swiftlift.R;
import com.snavi.swiftlift.database_objects.Const;
import com.snavi.swiftlift.lift.FoundLift;
import com.snavi.swiftlift.utils.Price;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
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
    private static final double EARTH_CIRCUMFERENCE_KM = 40075;


    // fields //////////////////////////////////////////////////////////////////////////////////////
    private ArrayList<FoundLift> m_foundLifts;
    private FoundLiftsAdapter m_adapter;
    // search parameters
    private LatLng  m_from;
    private LatLng  m_to;
    private Date    m_departure;
    private Date    m_maxDate;
    // ranges for from and to
    private Double m_fromWest;      // max coordinates to west from 'from'
    private Double m_fromNorth;     // max coordinates to north from 'from'
    private Double m_fromEast;      // max coordinates to east from 'from'
    private Double m_fromSouth;     // max coordinates to south from 'from'
    private Double m_toWest;        // max coordinates to west from 'to'
    private Double m_toNorth;       // max coordinates to north from 'to'
    private Double m_toEast;        // max coordinates to east from 'to'
    private Double m_toSouth;       // max coordinates to south from 'to'


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_found_lifts);

        m_foundLifts = new ArrayList<>();
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

        m_fromWest  = addLongitudeToCoordinates(m_from, -fromRange);
        m_fromNorth = addLatitudeToCoordinates(m_from, fromRange);
        m_fromEast  = addLongitudeToCoordinates(m_from, fromRange);
        m_fromSouth = addLatitudeToCoordinates(m_from, -fromRange);

        double toRange = (double) intent.getIntExtra(TO_RANGE_KEY, -1);
        if (toRange < 0)
            throw new RuntimeException(TO_RANGE_KEY);

        m_toWest  = addLongitudeToCoordinates(m_to, -toRange);
        m_toNorth = addLatitudeToCoordinates(m_to, toRange);
        m_toEast  = addLongitudeToCoordinates(m_to, toRange);
        m_toSouth = addLatitudeToCoordinates(m_to, -toRange);
    }



    private void search()
    {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        selectFromStretches(db);
        selectToStretches(db);
    }

    // TODO here I finished
    /**
     * method performs query that is choosing stretches which have properties appropriate for
     * departure. When query is done it calls synchronized method notify ready, which returns true
     * if selectToStretches finished first and false if this method finished first. If
     * this was first it does nothing. If this method was second it calls another method which
     * is responsible for connecting both.
     * @param db database
     */
    private void selectFromStretches(FirebaseFirestore db)
    {
        db.collection(Const.STRETCHES_COLLECTION)
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
                        loadStretchesFrom(queryDocumentSnapshots.getDocuments());

                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {

                    }
                });
    }



    private void selectToStretches(FirebaseFirestore db)
    {
        db.collection(Const.STRETCHES_COLLECTION)
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
                        loadStretchesTo(queryDocumentSnapshots.getDocuments());
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {

                    }
                });
    }



    private void loadStretchesFrom(List<DocumentSnapshot> docs)
    {

    }



    private void loadStretchesTo(List<DocumentSnapshot> docs)
    {

    }



    synchronized private boolean notifyReady()
    {
        return false;
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



    private Double addLatitudeToCoordinates(LatLng loc, Double distKm)
    {
        double degrees = (distKm / (EARTH_CIRCUMFERENCE_KM / 2)) * 180;
        double res = loc.latitude + degrees;
        if (res >= -180 && res <= 180)
            return res;
        else if (res > 180)
            return -360 + res;
        else // if (res < -180)
            return 360 + res;
    }



    private Double addLongitudeToCoordinates(LatLng loc, Double distKm)
    {
        double degrees = (distKm / (EARTH_CIRCUMFERENCE_KM / 2)) * 180;
        double res = loc.longitude + degrees;
        if (res >= -180 && res <= 180)
            return res;
        else if (res > 180)
            return -360 + res;
        else // if (res < -180)
            return 360 + res;
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



            private void bind(String from, String departureDate, String to, String arrivalDate,
                              String price)
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
