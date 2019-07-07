package com.snavi.swiftlift.activities;

import androidx.fragment.app.FragmentActivity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.snavi.swiftlift.R;

public class LiftPointsPickActivity extends FragmentActivity implements OnMapReadyCallback {


    // CONST ///////////////////////////////////////////////////////////////////////////////////////
    public static final String TAG = LiftPointsPickActivity.class.getName();
    public static final String NULL_FRAGMENT_MANAGER_ERROR      = "null fragment manager";
    public static final String RESULT_COORDINATES_KEY           = "coordinates";
    public static final String INITIAL_POSITION_COORDINATES_KEY = "initial_position";
    public static final float ZOOM = 9f;


    // fields //////////////////////////////////////////////////////////////////////////////////////
    private GoogleMap m_map;
    private LatLng m_userChoice;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lift_points_pick);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment == null)
        {
            finish();
            showNullFragmentManagerToast();
            Log.e(TAG, NULL_FRAGMENT_MANAGER_ERROR);
            return;
        }
        mapFragment.getMapAsync(this);
    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap)
    {
        m_map = googleMap;
        setMapOnClickListener();
        setOkButtonListener();
        setInitialPosition();
        // Add a marker in Sydney and move the camera
//        LatLng sydney = new LatLng(-34, 151);
//        m_map.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
//        m_map.moveCamera(CameraUpdateFactory.newLatLng(sydney));
    }



    private void setMapOnClickListener()
    {
        m_map.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                m_userChoice = latLng;
                m_map.clear();
                m_map.addMarker(new MarkerOptions().position(latLng));
            }
        });
    }



    private void setInitialPosition()
    {
        Intent intent = getIntent();
        LatLng coords = intent.getParcelableExtra(INITIAL_POSITION_COORDINATES_KEY);
        if (coords != null)
        {
            m_map.moveCamera(CameraUpdateFactory.newLatLngZoom(coords, ZOOM));
        }
    }



    private void setOkButtonListener()
    {
        Button but = findViewById(R.id.activity_lift_points_pick_but_ok);
        but.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (m_userChoice == null)
                    return;

                Intent intent = new Intent();
                intent.putExtra(RESULT_COORDINATES_KEY, m_userChoice);
                setResult(Activity.RESULT_OK, intent);
                finish();
            }
        });
    }



    // Toasts and snackbars ////////////////////////////////////////////////////////////////////////



    private void showNullFragmentManagerToast()
    {
        Toast.makeText(this, R.string.null_fragment_manager_error, Toast.LENGTH_LONG).show();
    }
}
