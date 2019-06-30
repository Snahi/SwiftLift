package com.snavi.swiftlift.utils;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.snavi.swiftlift.R;

public class CredentialFragment extends DialogFragment {


    // CONST //////////////////////////////////////////////////////////////////////////////////////
    private static final String TAG = CredentialFragment.class.getName();
    private static final String ERROR_DURING_USER_AUTH = "Error occurred during user reauthentication";


    // fields /////////////////////////////////////////////////////////////////////////////////////
    private OnFragmentInteractionListener m_listener;
    private boolean m_wasConfirmButtonClicked;          // indicates whether user closed dialog via confirm button or just clicked somewhere outside dialog or
                                                        // clicked back
    // views
    private EditText m_etEmail;
    private EditText m_etPassword;
    private Button m_butConfirm;
    private View m_view;


    public CredentialFragment()
    {
        // Required empty public constructor
    }


    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);

        if (context instanceof OnFragmentInteractionListener)
        {
            m_listener = (OnFragmentInteractionListener) context;
        }
        else
        {
            throw new RuntimeException(context.toString() + "must override OnFragmentInteractionListener");
        }
    }



    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        m_wasConfirmButtonClicked = false;
    }



    private void initViews()
    {
        m_etEmail    = m_view.findViewById(R.id.fragment_credential_et_email);
        m_etPassword = m_view.findViewById(R.id.fragment_credential_et_password);
        m_butConfirm = m_view.findViewById(R.id.fragment_credential_but_confirm);
    }



    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment

        m_view = inflater.inflate(R.layout.fragment_credential, container, false);

        initViews();
        setConfirmButtonListener();

        return m_view;
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
    }


    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);

        m_listener.onDismiss();
        if (!m_wasConfirmButtonClicked)
            m_listener.onCompleteCredential(false);
    }

    @Override
    public void onDetach() {
        super.onDetach();

        if (!m_wasConfirmButtonClicked)
            m_listener = null;
    }



    private void setConfirmButtonListener()
    {
        m_butConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                m_wasConfirmButtonClicked = true;
                dismiss();
                doCredentialSuccessful();
            }
        });
    }



    private void doCredentialSuccessful()
    {
        String email = m_etEmail.getText().toString();
        String password = m_etPassword.getText().toString();

        if (email.isEmpty() || password.isEmpty())
        {
            m_listener.onCompleteCredential(false);
            return;
        }

        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth == null)
        {
            m_listener.onCompleteCredential(false);
            return;
        }

        FirebaseUser user = auth.getCurrentUser();
        if (user == null)
        {
            m_listener.onCompleteCredential(false);
            return;
        }

        AuthCredential credential = EmailAuthProvider.getCredential(email, password);
        reauthenticate(user, credential);
    }



    private void reauthenticate(FirebaseUser user, AuthCredential credential)
    {
        try
        {
            user.reauthenticate(credential).addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task)
                {

                    if (task.isSuccessful())
                        m_listener.onCompleteCredential(true);
                    else
                        m_listener.onCompleteCredential(false);

                    m_listener = null;
                }
            });
        }
        catch (Exception e)
        {
            Log.e(TAG, ERROR_DURING_USER_AUTH + ". Exception: " + e.getClass() + " message: "
                    + e.getMessage());
            m_listener.onCompleteCredential(false);
        }
    }



    public interface OnFragmentInteractionListener {
        void onDismiss();
        void onCompleteCredential(boolean success);
    }


}
