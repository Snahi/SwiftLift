package com.snavi.swiftlift.activities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;


import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.snavi.swiftlift.R;
import com.snavi.swiftlift.activities.users_data.SettingsActivity;
import com.snavi.swiftlift.lift.AddStretchDialogFragment;
import com.snavi.swiftlift.signed_in_fragments.DriverFragment;
import com.snavi.swiftlift.signed_in_fragments.FindLiftFragment;
import com.snavi.swiftlift.signed_in_fragments.PassengerFragment;


public class SignedUserMainActivity extends AppCompatActivity {


    // fields /////////////////////////////////////////////////////////////////////////////////////
    private FirebaseAuth m_auth;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signed_user_main);

        m_auth = FirebaseAuth.getInstance();

        setButtonsListeners();

        ViewPager vp = findViewById(R.id.activity_signed_user_main_vp);
        vp.setAdapter(new TabsPagerAdapter(getSupportFragmentManager()));

        TabLayout tl = findViewById(R.id.activity_signed_user_main_tl);
        tl.setupWithViewPager(vp);
    }



    private void setButtonsListeners()
    {
        setSettingsButtonListener();
        setSignOutButtonListener();
    }



    private void setSettingsButtonListener()
    {
        ImageButton button = findViewById(R.id.activity_signed_user_main_but_settings);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(SignedUserMainActivity.this,
                        SettingsActivity.class);
                startActivity(intent);
            }
        });
    }



    private void setSignOutButtonListener()
    {
        Button button = findViewById(R.id.activity_signed_user_main_but_sign_out);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                if (m_auth == null) {
                    showCantSignOutToast();
                    return;
                }
                m_auth.signOut();
                Intent intent = new Intent(SignedUserMainActivity.this,
                        MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            }
        });
    }



    // Toasts & snackbars /////////////////////////////////////////////////////////////////////////



    private void showCantSignOutToast()
    {
        Toast.makeText(this, R.string.cant_sign_out, Toast.LENGTH_LONG).show();
    }


    // ViewPager adapter //////////////////////////////////////////////////////////////////////////



    class TabsPagerAdapter extends FragmentPagerAdapter {

        // CONST //////////////////////////////////////////////////////////////////////////////////////
        static final int FIND_LIFT_FRAGMENT_POS = 0;
        static final int DRIVER_FRAGMENT_POS    = 1;
        static final int PASSENGER_FRAGMENT_POS = 2;
        static final int NUM_OF_TABS = 3;


        TabsPagerAdapter(FragmentManager fm)
        {
            super(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
        }



        @NonNull
        @Override
        public Fragment getItem(int position)
        {
            switch (position)
            {
                case FIND_LIFT_FRAGMENT_POS : return new FindLiftFragment();
                case DRIVER_FRAGMENT_POS    : return new DriverFragment();
                case PASSENGER_FRAGMENT_POS : return new PassengerFragment();
            }

            return new FindLiftFragment();
        }



        @Nullable
        @Override
        public CharSequence getPageTitle(int position)
        {
            Resources res = getResources();
            switch (position)
            {
                case FIND_LIFT_FRAGMENT_POS : return res.getString(R.string.find_lift_tab_title);
                case DRIVER_FRAGMENT_POS    : return res.getString(R.string.driver_tab_title);
                case PASSENGER_FRAGMENT_POS : return res.getString(R.string.passenger_tab_title);
            }

            return "";
        }



        @Override
        public int getCount() {
            return NUM_OF_TABS;
        }
    }
}
