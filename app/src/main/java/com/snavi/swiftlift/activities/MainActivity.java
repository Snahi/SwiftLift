package com.snavi.swiftlift.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.snavi.swiftlift.custom_views.WaitingSpinner;
import com.snavi.swiftlift.login.LogInFragment;
import com.snavi.swiftlift.R;


public class MainActivity extends AppCompatActivity implements LogInFragment.OnFragmentInteractionListener {


    // CONST ///////////////////////////////////////////////////////////////////////////////////////
    public static final String IS_LOGGED_IN_KEY = "is_logged_in";
    public static final String EMAIL_KEY        = "email";
    public static final String PASSWORD_KEY     = "password";
    public static final String PREFERENCES_KEY  = "user_data";


    // fields //////////////////////////////////////////////////////////////////////////////////////
    private SharedPreferences m_sharedPreferences;
    private WaitingSpinner m_waitingSpinner;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        m_sharedPreferences = getSharedPreferences(PREFERENCES_KEY, MODE_PRIVATE);
        m_waitingSpinner    = findViewById(R.id.activity_main_spinner);

        setButtonsListeners();

        if (isUserSaved())
            signUserIn();
    }



    private boolean isUserSaved()
    {
        return m_sharedPreferences.getBoolean(IS_LOGGED_IN_KEY, false);
    }



    private void signUserIn()
    {
        m_waitingSpinner.start();
        if (FirebaseAuth.getInstance().getCurrentUser() != null)
        {
            m_waitingSpinner.stop();
            moveToSignedUserActivity();
            return;
        }

        String email = m_sharedPreferences.getString(EMAIL_KEY, "no email");
        String password = m_sharedPreferences.getString(PASSWORD_KEY, "no password");

        assert email != null;
        assert password != null;
        FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task)
            {
                m_waitingSpinner.stop();
                if (task.isSuccessful())
                {
                    moveToSignedUserActivity();
                }
            }
        });
    }



    private void moveToSignedUserActivity()
    {
        Intent intent = new Intent(this, SignedUserMainActivity.class);
        startActivity(intent);
        finish();
    }



    private void setButtonsListeners()
    {
        setLoginButtonListener();
    }



    private void setLoginButtonListener()
    {
        Button button = findViewById(R.id.activity_main_but_log_in);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showLoginPopup();
            }
        });
    }



    private void showLoginPopup()
    {
        FragmentManager fragmentManager = getSupportFragmentManager();
        LogInFragment loginFragment = new LogInFragment();
        loginFragment.show(fragmentManager, "Log in");
    }



    @Override
    public void onActivityResult(int reqCode, int resCode, Intent intent)
    {
        super.onActivityResult(reqCode, resCode, intent);

        switch (reqCode)
        {
            case LogInFragment.REGISTER_ACTIVITY_RES_CODE : processRegister(resCode); break;
        }
    }



    private void processRegister(int resCode)
    {
        if (resCode == Activity.RESULT_OK)
            changeToLoggedInMode();
    }



    private void changeToLoggedInMode()
    {

    }



    @Override
    public void successfulLogin() {

    }
}
