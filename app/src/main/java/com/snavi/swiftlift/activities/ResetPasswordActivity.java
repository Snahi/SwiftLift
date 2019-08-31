package com.snavi.swiftlift.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.snavi.swiftlift.R;
import com.snavi.swiftlift.utils.Snackbars;
import com.snavi.swiftlift.utils.Toasts;

public class ResetPasswordActivity extends AppCompatActivity {

    // CONST ///////////////////////////////////////////////////////////////////////////////////////
    // keys
    public static final String KEY_USER_EMAIL = "user_email";

    // fields //////////////////////////////////////////////////////////////////////////////////////
    // views
    private EditText    m_etEmail;
    private Button      m_butResetPassword;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reset_password);

        initViews();
        initViewValues();
        setResetPasswordButtonListener();
    }



    private void initViews()
    {
        m_etEmail           = findViewById(R.id.activity_reset_password_et_email);
        m_butResetPassword  = findViewById(R.id.activity_reset_password_but_reset);
    }



    private void initViewValues()
    {
        Intent intent = getIntent();
        if (intent != null)
        {
            String email = intent.getStringExtra(KEY_USER_EMAIL);
            if (email != null)
            {
                m_etEmail.setText(email);
            }
        }
    }



    private void setResetPasswordButtonListener()
    {
        m_butResetPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                FirebaseAuth auth = FirebaseAuth.getInstance();
                auth.sendPasswordResetEmail(m_etEmail.getText().toString())
                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid)
                            {
                                Snackbars.showPasswordResetSuccessSnackbar(
                                        ResetPasswordActivity.this,
                                        findViewById(R.id.activity_reset_password_cl));
                                Toasts.showPasswordResetSuccessToast(
                                        ResetPasswordActivity.this);
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e)
                            {
                                Snackbars.showPasswordResetFailureSnackbar(
                                        ResetPasswordActivity.this,
                                        findViewById(R.id.activity_reset_password_cl)
                                );
                                Toasts.showPasswordResetFailureToast(
                                        ResetPasswordActivity.this
                                );
                            }
                        });
            }
        });
    }
}
