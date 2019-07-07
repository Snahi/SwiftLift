package com.snavi.swiftlift.activities;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.snavi.swiftlift.activities.users_data.RegisterActivity;
import com.snavi.swiftlift.sign_in.LogInFragment;
import com.snavi.swiftlift.R;


public class MainActivity extends AppCompatActivity implements LogInFragment.OnFragmentInteractionListener {

    // CONST ///////////////////////////////////////////////////////////////////////////////////////
    public static final String LOGIN_POPUP_TAG  = "log in";


    // fields //////////////////////////////////////////////////////////////////////////////////////
    private ProgressBar m_progressBar;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (isUserSignedIn())
        {
            moveToSignedUserActivity();
            return;
        }

        setContentView(R.layout.activity_main);

        initViews();

        m_progressBar.setVisibility(View.GONE);

        setButtonsListeners();
    }



    private boolean isUserSignedIn()
    {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth == null) return false;

        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return false;

        return user.isEmailVerified();
    }



    private void moveToSignedUserActivity()
    {
        Intent intent = new Intent(this, SignedUserMainActivity.class);
        startActivity(intent);
        finish();
    }



    private void initViews()
    {
        m_progressBar = findViewById(R.id.activity_main_progress_bar);
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
        loginFragment.show(fragmentManager, LOGIN_POPUP_TAG);
    }



    // login fragment //////////////////////////////////////////////////////////////////////////////



    @Override
    public void successfulLogin()
    {
        moveToSignedUserActivity();
    }



    @Override
    public void userWantsToRegister()
    {
        Intent intent = new Intent(this, RegisterActivity.class);
        startActivity(intent);
    }
}
