package com.snavi.swiftlift.activities.users_data;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;

import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.snavi.swiftlift.R;
import com.snavi.swiftlift.utils.CredentialFragment;
import com.snavi.swiftlift.utils.InputValidator;

public class EmailUpdateActivity extends AppCompatActivity implements
        CredentialFragment.OnFragmentInteractionListener {


    // CONST ///////////////////////////////////////////////////////////////////////////////////////
    public static final String TAG = EmailUpdateActivity.class.getName();
    public static final String NULL_AUTH_ERROR = "null auth";
    public static final String NULL_USER_ERROR = "null user";
    public static final String UNKNOWN_ERROR = "unknown error";
    public static final String CREDENTIAL_FRAGMENT_TAG = "credential fragment";


    // fields //////////////////////////////////////////////////////////////////////////////////////
    private FirebaseUser m_currUser;
    private String m_email;

    // views
    private EditText m_etEmail;
    private EditText m_etConfirmEmail;
    private Button m_butSave;
    private ProgressBar m_progressBar;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_email_update);

        if (!initFirebase())
            return;
        initViews();
        setSaveButtonListener();
    }



    private boolean initFirebase()
    {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth == null)
        {
            Log.e(TAG, NULL_AUTH_ERROR);
            showFirebaseErrorToast();
            return false;
        }

        m_currUser = auth.getCurrentUser();
        if (m_currUser == null)
        {
            Log.e(TAG, NULL_USER_ERROR);
            showFirebaseErrorToast();
            return false;
        }

        return true;
    }



    private void initViews()
    {
        m_etEmail        = findViewById(R.id.activity_email_update_et_new_email);
        m_etConfirmEmail = findViewById(R.id.activity_email_update_et_confirm_email);
        m_butSave        = findViewById(R.id.activity_email_update_but_save);
        m_progressBar    = findViewById(R.id.activity_email_update_progress_bar);
    }



    private void setSaveButtonListener()
    {
        m_butSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (validateData())
                {
                    m_email = m_etEmail.getText().toString();
                    m_butSave.setEnabled(false);
                    m_progressBar.setVisibility(View.VISIBLE);
                    updateEmail();
                }
            }
        });
    }



    private boolean validateData()
    {
        Resources res = getResources();
        boolean email = InputValidator.validateEmail(m_etEmail, res);
        boolean confirmEmail = InputValidator.validateConfirmEmail(m_etEmail, m_etConfirmEmail, res);

        return email && confirmEmail;
    }



    private void updateEmail()
    {
        CredentialFragment credFrag = new CredentialFragment();
        FragmentManager fm = getSupportFragmentManager();
        credFrag.show(fm, CREDENTIAL_FRAGMENT_TAG);
    }



    // Toasts & snackbars /////////////////////////////////////////////////////////////////////////



    private void showFirebaseErrorToast()
    {
        Toast.makeText(this, R.string.load_data_failure, Toast.LENGTH_LONG).show();
    }



    private void showSuccessfulUpdateToast()
    {
        Toast.makeText(this, R.string.successful_update, Toast.LENGTH_LONG).show();
    }



    private void showFailureUpdateSnackbar()
    {
        Snackbar.make(findViewById(R.id.activity_email_update_cl), R.string.successful_update,
                Snackbar.LENGTH_INDEFINITE).setAction(R.string.snackbar_ok, new View.OnClickListener() {
            @Override
            public void onClick(View view) {}
        });
    }



    private void showCredentialFailureSnackbar()
    {
        Snackbar.make(
                findViewById(R.id.activity_email_update_cl),
                R.string.credential_failure,
                Snackbar.LENGTH_INDEFINITE)
                .setAction(R.string.snackbar_ok, new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {}
                }).show();
    }



    // CredentialFragment //////////////////////////////////////////////////////////////////////////



    @Override
    public void onDismiss()
    {

    }



    @Override
    public void onCompleteCredential(boolean success)
    {
        if (success)
        {
            updateEmailInFirebase();

        }
        else
        {
            m_progressBar.setVisibility(View.GONE);
            showCredentialFailureSnackbar();
            m_butSave.setEnabled(true);
        }
    }



    private void updateEmailInFirebase()
    {
        if (m_currUser != null)
        {
            m_currUser.updateEmail(m_email).addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {

                    if (task.isSuccessful())
                        verificateEmail();
                    else
                    {
                        Log.e(TAG, UNKNOWN_ERROR);
                        showFailureUpdateSnackbar();
                        m_butSave.setEnabled(true);
                    }
                }
            });
        }
        else
        {
            Log.e(TAG, NULL_USER_ERROR);
            m_progressBar.setVisibility(View.GONE);
            showFailureUpdateSnackbar();
            m_butSave.setEnabled(true);
        }
    }



    private void verificateEmail()
    {
        m_currUser.sendEmailVerification().addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful())
                {
                    showSuccessfulUpdateToast();
                    finish();
                }
                else
                {
                    Log.e(TAG, UNKNOWN_ERROR);
                    showFailureUpdateSnackbar();
                }

                m_butSave.setEnabled(true);
                m_progressBar.setVisibility(View.GONE);
            }
        });
    }
}
