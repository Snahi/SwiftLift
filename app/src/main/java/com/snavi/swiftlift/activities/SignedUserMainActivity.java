package com.snavi.swiftlift.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;
import com.google.firebase.auth.FirebaseAuth;
import com.snavi.swiftlift.R;


public class SignedUserMainActivity extends AppCompatActivity {



    // fields /////////////////////////////////////////////////////////////////////////////////////
    private FirebaseAuth m_auth;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signed_user_main);

        m_auth = FirebaseAuth.getInstance();

        setButtonsListeners();
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
}
