package com.snavi.swiftlift.login;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.snavi.swiftlift.R;
import com.snavi.swiftlift.activities.MainActivity;
import com.snavi.swiftlift.activities.RegisterActivity;
import com.snavi.swiftlift.activities.SignedUserMainActivity;
import com.snavi.swiftlift.custom_views.WaitingSpinner;


public class LogInFragment extends DialogFragment {


    // CONST ///////////////////////////////////////////////////////////////////////////////////////
    public static final int REGISTER_ACTIVITY_RES_CODE = 6969;


    // fields //////////////////////////////////////////////////////////////////////////////////////
    private OnFragmentInteractionListener m_listener;
    private View m_view;
    private Activity m_activity;
    private FirebaseAuth m_auth;
    private WaitingSpinner m_waitingSpinner;



    public LogInFragment()
    {
        // Required empty public constructor
    }



    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        m_auth = FirebaseAuth.getInstance();
    }



    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        return inflater.inflate(R.layout.fragment_log_in, container, false);
    }



    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState)
    {
        super.onActivityCreated(savedInstanceState);

        m_view = getView();
        m_activity = getActivity();
        m_waitingSpinner = m_view.findViewById(R.id.fragment_log_in_sv_spinner);

        setButtonsListeners();
    }



    private void setButtonsListeners()
    {
        setSignInButtonListener();
        setSignUpButtonListener();
    }



    private void setSignInButtonListener()
    {
        Button signIn = m_view.findViewById(R.id.fragment_log_but_log_in);
        signIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                String email = getEmail();
                String password = getPassword();
                if (email.isEmpty() && password.isEmpty())
                    dealWithLoginFailure();
                else
                {
                    m_waitingSpinner.start();
                    signIn(email, password);
                }
            }
        });
    }



    private void signIn(String email, String password)
    {
        m_auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task)
                    {
                        if (task.isSuccessful())
                            dealWithSignInSuccess();
                        else
                        {
                            dealWithLoginFailure();
                            m_waitingSpinner.stop();
                        }
                    }
                });
    }



    private String getEmail()
    {
        EditText emailEt = m_view.findViewById(R.id.fragment_log_in_et_email);
        return emailEt.getText().toString();
    }



    private String getPassword()
    {
        EditText passwordEt = m_view.findViewById(R.id.fragment_log_in_et_password);
        return passwordEt.getText().toString();
    }



    private void dealWithSignInSuccess()
    {
        m_listener.successfulLogin();

        final FirebaseUser user = m_auth.getCurrentUser();
        if (user != null)
            user.reload().addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    m_waitingSpinner.stop();
                    if (user.isEmailVerified())
                    {
                        dismiss();                                                                  // close DialogFragment
                        saveUserToAutoSignIn();
                        startSignedUserActivity();
                    }
                    else
                        showUnverifiedAccountSnackbar();
                }
            });
        else
            showUnknownUserError();
    }



    private void saveUserToAutoSignIn()
    {
        SharedPreferences prefs = m_activity.getSharedPreferences(MainActivity.PREFERENCES_KEY,
                Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(MainActivity.IS_LOGGED_IN_KEY, true);
        editor.putString(MainActivity.EMAIL_KEY, getEmail());
        editor.putString(MainActivity.PASSWORD_KEY, getPassword());
        editor.apply();
    }



    private void showUnverifiedAccountSnackbar()
    {
        Snackbar.make(m_view, R.string.unverified_email, Snackbar.LENGTH_INDEFINITE)
                .setAction(R.string.snackbar_ok, new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {}
                }).show();
    }



    private void startSignedUserActivity()
    {
        Intent intent = new Intent(m_activity, SignedUserMainActivity.class);
        startActivity(intent);
    }



    private void showUnknownUserError()
    {
        Toast.makeText(m_activity, getResources().getString(R.string.login_unknown_error),
                Toast.LENGTH_LONG).show();
    }



    private void dealWithLoginFailure()
    {
        Snackbar.make(m_view, R.string.bad_login_data, Snackbar.LENGTH_LONG).show();
    }



    private void setSignUpButtonListener()
    {
        Button button = m_view.findViewById(R.id.fragment_log_but_sign_up);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(m_activity, RegisterActivity.class);
                m_activity.startActivity(intent);
            }
        });
    }



    @Override
    public void onAttach(@NonNull Context context)
    {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            m_listener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }



    @Override
    public void onDetach()
    {
        super.onDetach();
        m_listener = null;
    }




    public interface OnFragmentInteractionListener
    {
        void successfulLogin();
    }
}
