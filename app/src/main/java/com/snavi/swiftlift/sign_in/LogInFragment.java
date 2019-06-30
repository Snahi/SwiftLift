package com.snavi.swiftlift.sign_in;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseUser;
import com.snavi.swiftlift.R;


public class LogInFragment extends DialogFragment {


    // fields //////////////////////////////////////////////////////////////////////////////////////
    private OnFragmentInteractionListener m_listener;
    private Activity m_activity;
    private FirebaseAuth m_auth;

    // views
    private ProgressBar m_progressBar;
    private View m_view;


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
        m_progressBar = m_view.findViewById(R.id.fragment_log_in_progress_bar);

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
                    dealWithLoginFailureDataNotMatching();
                else
                {
                    disableButtons();
                    m_progressBar.setVisibility(View.VISIBLE);
                    signIn(email, password);
                }
            }
        });
    }



    private void disableButtons()
    {
        Button logIn = m_view.findViewById(R.id.fragment_log_but_log_in);
        logIn.setEnabled(false);

        Button signIn = m_view.findViewById(R.id.fragment_log_but_sign_up);
        signIn.setEnabled(false);
    }



    private void enableButtons()
    {
        Button logIn = m_view.findViewById(R.id.fragment_log_but_log_in);
        logIn.setEnabled(true);

        Button signIn = m_view.findViewById(R.id.fragment_log_but_sign_up);
        signIn.setEnabled(true);
    }



    private void signIn(String email, String password)
    {
        if (m_auth == null)
        {
            m_progressBar.setVisibility(View.GONE);
            showLogInErrorToast();
            dismiss();
            return;
        }
        m_auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task)
                    {
                        if (task.isSuccessful())
                            dealWithSignInSuccess();
                        else
                        {
                            Exception e = task.getException();
                            if (e instanceof FirebaseNetworkException)
                                dealWithNetworkException();
                            else if (e instanceof FirebaseAuthException)
                                dealWithLoginFailureDataNotMatching();
                            else
                                dealWithUnknownException();

                            m_progressBar.setVisibility(View.GONE);
                            enableButtons();
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
        final FirebaseUser user = m_auth.getCurrentUser();
        if (user != null)
            user.reload().addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    m_progressBar.setVisibility(View.GONE);
                    if (user.isEmailVerified())
                    {
                        m_listener.successfulLogin();
                        dismiss();                                                                  // close DialogFragment
                    }
                    else
                    {
                        showUnverifiedAccountSnackbar();
                        enableButtons();
                    }
                }
            });
        else
            showUnknownUserError();
    }



    private void showUnverifiedAccountSnackbar()
    {
        Snackbar.make(m_view, R.string.unverified_email, Snackbar.LENGTH_INDEFINITE)
                .setAction(R.string.snackbar_ok, new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {}
                }).show();
    }



    private void showUnknownUserError()
    {
        Toast.makeText(m_activity, getResources().getString(R.string.login_unknown_error),
                Toast.LENGTH_LONG).show();
    }



    private void dealWithLoginFailureDataNotMatching()
    {
        Snackbar.make(m_view, R.string.bad_login_data, Snackbar.LENGTH_LONG).show();
    }



    private void dealWithNetworkException()
    {
        Snackbar.make(m_view, R.string.network_error, Snackbar.LENGTH_INDEFINITE)
                .setAction(R.string.snackbar_ok, new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {}
                }).show();
    }



    private void dealWithUnknownException()
    {
        Snackbar.make(m_view, R.string.login_unknown_error, Snackbar.LENGTH_INDEFINITE)
                .setAction(R.string.snackbar_ok, new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {}
                }).show();
    }



    private void setSignUpButtonListener()
    {
        Button button = m_view.findViewById(R.id.fragment_log_but_sign_up);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                m_listener.userWantsToRegister();
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



    // Toasts && snackbars /////////////////////////////////////////////////////////////////////////



    private void showLogInErrorToast()
    {
        Toast.makeText(m_activity, R.string.login_error_null_auth_or_user, Toast.LENGTH_LONG).show();
    }



    // listener interface //////////////////////////////////////////////////////////////////////////



    public interface OnFragmentInteractionListener
    {
        void successfulLogin();
        void userWantsToRegister();
    }
}
