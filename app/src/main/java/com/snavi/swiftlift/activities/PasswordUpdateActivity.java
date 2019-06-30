package com.snavi.swiftlift.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;

import android.app.Activity;
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

public class PasswordUpdateActivity extends AppCompatActivity
        implements CredentialFragment.OnFragmentInteractionListener {


    // CONST //////////////////////////////////////////////////////////////////////////////////////
    public static final String TAG = PasswordUpdateActivity.class.getName();
    public static final String CREDENTIAL_FRAGMENT_TAG = "credential_fragment";


    // fields /////////////////////////////////////////////////////////////////////////////////////
    // views
    private EditText m_etPassword;
    private EditText m_etConfirmPassword;
    private Button m_butSave;
    private ProgressBar m_progressBar;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_password_update);

        initViews();
        setSaveButtonListener();
    }



    private void initViews()
    {
        m_etPassword        = findViewById(R.id.activity_password_update_et_password);
        m_etConfirmPassword = findViewById(R.id.activity_password_update_et_confirm_password);
        m_butSave           = findViewById(R.id.activity_password_update_but_save);
        m_progressBar       = findViewById(R.id.activity_password_update_progress_bar);
    }



    private void setSaveButtonListener()
    {
        m_butSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                if (!validatePassword())
                    return;
                FragmentManager fm = getSupportFragmentManager();
                CredentialFragment credFragment = new CredentialFragment();
                credFragment.show(fm, CREDENTIAL_FRAGMENT_TAG);
            }
        });
    }



    private boolean validatePassword()
    {
        Resources res = getResources();
        boolean password = InputValidator.validatePassword(m_etPassword,
                RegisterActivity.MIN_PASSWORD_LEN, RegisterActivity.MAX_PASSWORD_LEN, res);
        boolean confirmPassword = InputValidator.validateConfirmPassword(m_etPassword,
                m_etConfirmPassword, res);

        return password && confirmPassword;
    }



    private void prepareUpdatePassword()
    {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth == null)
        {
            showUpdateFailureSnackbar();
            Log.e(TAG, "auth == null in updatePassword()");
            m_progressBar.setVisibility(View.GONE);
            m_butSave.setEnabled(true);
            return;
        }

        FirebaseUser user = auth.getCurrentUser();
        if (user == null)
        {
            showUpdateFailureSnackbar();
            Log.e(TAG, "user == null in updatePassword");
            m_progressBar.setVisibility(View.GONE);
            m_butSave.setEnabled(true);
            return;
        }

        updatePassword(user);
    }



    private void updatePassword(FirebaseUser user)
    {
        user.updatePassword(m_etPassword.getText().toString())
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task)
                {
                    if (task.isSuccessful())
                    {
                        setResult(Activity.RESULT_OK);
                        finish();
                        showSuccessfulUpdateToast();
                    }
                    else
                        showUpdateFailureSnackbar();

                    m_progressBar.setVisibility(View.GONE);
                    m_butSave.setEnabled(true);
                }
            });
    }



    // CredentialFragment //////////////////////////////////////////////////////////////////////////



    @Override
    public void onDismiss()
    {
        m_progressBar.setVisibility(View.VISIBLE);
        m_butSave.setEnabled(false);                    // so that user can't update password before previous update finishes
    }



    @Override
    public void onCompleteCredential(boolean success)
    {
        if (success)
            prepareUpdatePassword();
        else
        {
            showCredentialFailureSnackbar();
            m_progressBar.setVisibility(View.GONE);
            m_butSave.setEnabled(true);
        }
    }



    // Toasts and snackbars ///////////////////////////////////////////////////////////////////////



    private void showSuccessfulUpdateToast()
    {
        Toast.makeText(this, R.string.successful_password_update, Toast.LENGTH_LONG).show();
    }



    private void showCredentialFailureSnackbar()
    {
        Snackbar.make(
                findViewById(R.id.activity_password_update_cl),
                R.string.credential_failure,
                Snackbar.LENGTH_INDEFINITE)
                .setAction(R.string.snackbar_ok, new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {}
                }).show();
    }



    private void showUpdateFailureSnackbar()
    {
        Snackbar.make(
                findViewById(R.id.activity_password_update_cl),
                R.string.password_update_failure,
                Snackbar.LENGTH_INDEFINITE)
                .setAction(R.string.snackbar_ok, new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {}
                }).show();
    }
}
