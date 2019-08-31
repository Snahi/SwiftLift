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
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;
import com.snavi.swiftlift.R;
import com.snavi.swiftlift.activities.users_data.SettingsActivity;
import com.snavi.swiftlift.database_objects.Const;
import com.snavi.swiftlift.signed_in_fragments.DriverFragment;
import com.snavi.swiftlift.signed_in_fragments.FindLiftFragment;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class SignedUserMainActivity extends AppCompatActivity {

    // CONST ///////////////////////////////////////////////////////////////////////////////////////
    private static final int NUM_OF_USER_UNREGISTER_FROM_TOKEN_TRIALS_IF_FAIL = 30;


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
                unregisterFromFCMToken(NUM_OF_USER_UNREGISTER_FROM_TOKEN_TRIALS_IF_FAIL);
                m_auth.signOut();
                Intent intent = new Intent(SignedUserMainActivity.this,
                        MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            }
        });
    }



    private void unregisterFromFCMToken(int numOfTrials)
    {
        FirebaseUser user = m_auth.getCurrentUser();
        if (user != null)
        {
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.collection(Const.FCM_TOKENS_COLLECTION)
                    .whereEqualTo(Const.FCM_TOKEN_OWNER, user.getUid())
                    .get()
                    .addOnCompleteListener(new OnUserTokenSearchCompleteListener(numOfTrials, db));
        }
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
        static final int NUM_OF_TABS = 2;


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
                // case PASSENGER_FRAGMENT_POS : return new PassengerFragment(); currently this feature is not available
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



    // Firebase tokens listeners ///////////////////////////////////////////////////////////////////



    private class OnUserTokenSearchCompleteListener implements OnCompleteListener<QuerySnapshot> {

        private int                 m_numOfTrials;
        private FirebaseFirestore   m_db;


        private OnUserTokenSearchCompleteListener(int numOfTrials, FirebaseFirestore db)
        {
            m_numOfTrials   = numOfTrials;
            m_db            = db;
        }



        @Override
        public void onComplete(@NonNull Task<QuerySnapshot> task)
        {
            if (task.isSuccessful())
            {
                QuerySnapshot result = task.getResult();
                if (result != null)
                {
                    List<DocumentSnapshot> docs = result.getDocuments();
                    if (!docs.isEmpty())
                        eraseUserFromTokenDoc(docs.get(0).getId());

                }
            }
            else
            {
                if (m_numOfTrials > 0)
                    unregisterFromFCMToken(m_numOfTrials - 1);
            }
        }



        private void eraseUserFromTokenDoc(String tokenDocId)
        {
            Map<String, Object> updateMap = new HashMap<>();
            updateMap.put(Const.FCM_TOKEN_OWNER, "");

            m_db.collection(Const.FCM_TOKENS_COLLECTION)
                    .document(tokenDocId)
                    .set(updateMap, SetOptions.merge())
                    .addOnFailureListener(
                            new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e)
                                {
                                    if (m_numOfTrials > 0)
                                        unregisterFromFCMToken(m_numOfTrials - 1);
                                }
                            }
                    );
        }
    }
}
