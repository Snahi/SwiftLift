package com.snavi.swiftlift.activities.users_data;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import com.snavi.swiftlift.R;

public class SettingsActivity extends AppCompatActivity {

    // fields //////////////////////////////////////////////////////////////////////////////////////
    // views
    private Button m_butChangePassword;
    private Button m_butChangePersonalData;
    private Button m_butChangeEmail;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        initViews();
        setButtonsListeners();
    }



    private void initViews()
    {
        m_butChangePassword = findViewById(R.id.activity_settings_but_change_password);
        m_butChangePersonalData = findViewById(R.id.activity_settings_but_personal_data);
        m_butChangeEmail = findViewById(R.id.activity_settings_but_change_email);
    }



    private void setButtonsListeners()
    {
        setChangePersonalDataButtonListener();
        setChangePasswordButtonListener();
        setChangeEmailButtonListener();
    }



    private void setChangePersonalDataButtonListener()
    {
        m_butChangePersonalData.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(SettingsActivity.this,
                        PersonalDataUpdateActivity.class);
                startActivity(intent);
            }
        });
    }



    private void setChangePasswordButtonListener()
    {
        m_butChangePassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(SettingsActivity.this,
                        PasswordUpdateActivity.class);
                startActivity(intent);
            }
        });

    }



    private void setChangeEmailButtonListener()
    {
        m_butChangeEmail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(SettingsActivity.this,
                        EmailUpdateActivity.class);
                startActivity(intent);
            }
        });
    }


}
