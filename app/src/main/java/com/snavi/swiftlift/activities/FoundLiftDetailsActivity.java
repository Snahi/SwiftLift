package com.snavi.swiftlift.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

import com.snavi.swiftlift.R;
import com.snavi.swiftlift.lift.FoundLift;
import com.snavi.swiftlift.lift.Lift;

public class FoundLiftDetailsActivity extends AppCompatActivity {

    // CONST ///////////////////////////////////////////////////////////////////////////////////////
    // keys
    public static final String LIFT_KEY = "lift";
    // errors
    private static final String FOUND_LIFT_NULL_ERROR = "Found lift wasn't passed properly via intent. It must be put to intent as Parcelable under FoundLiftDetailsActivity.LIFT_KEY key.";


    // fields //////////////////////////////////////////////////////////////////////////////////////
    private FoundLift m_foundLift;



    // init ////////////////////////////////////////////////////////////////////////////////////////


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_found_lift_details);

        initFoundLift();
    }



    private void initFoundLift()
    {
        Intent intent   = getIntent();
        m_foundLift     = intent.getParcelableExtra(LIFT_KEY);

        if (m_foundLift == null)
            throw new RuntimeException(FOUND_LIFT_NULL_ERROR);
    }
}
