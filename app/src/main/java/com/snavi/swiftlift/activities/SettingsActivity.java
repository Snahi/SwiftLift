package com.snavi.swiftlift.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.snavi.swiftlift.R;
import com.snavi.swiftlift.utils.InputValidator;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        setButtonsListeners();
    }



    private void setButtonsListeners()
    {
        setSaveButtonListener();
        setCancelButtonListener();
    }



    private void setSaveButtonListener()
    {
        Button button = findViewById(R.id.activity_settings_but_save);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean isInputValid = validateInput();
                if (isInputValid)
                {

                }
            }
        });
    }



    private void setCancelButtonListener()
    {
        Button button = findViewById(R.id.activity_settings_but_cancel);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
    }



    private boolean validateInput()
    {
        Resources res = getResources();
        boolean name = InputValidator.validateName(
                (EditText) findViewById(R.id.activity_settings_et_name),
                RegisterActivity.MIN_NAME_LEN, RegisterActivity.MAX_NAME_LEN, res);
        boolean surname = InputValidator.validateSurname(
                (EditText) findViewById(R.id.activity_settings_et_surname),
                RegisterActivity.MIN_SURNAME_LEN, RegisterActivity.MAX_SURNAME_LEN, res);
        boolean email = InputValidator.validateEmail(
                (EditText) findViewById(R.id.activity_settings_et_email), res);
        @SuppressLint("CutPasteId") boolean password = InputValidator.validatePassword(
                (EditText) findViewById(R.id.activity_settings_et_password),
                RegisterActivity.MIN_PASSWORD_LEN, RegisterActivity.MAX_PASSWORD_LEN, res);
        @SuppressLint("CutPasteId") boolean confirmPassword = InputValidator.validateConfirmPassword(
                (EditText) findViewById(R.id.activity_settings_et_password),
                (EditText) findViewById(R.id.activity_settings_et_confirm_new_password),
                res);
        boolean phone = InputValidator.validatePhone(
                (EditText) findViewById(R.id.activity_settings_et_phone),
                RegisterActivity.MIN_PHONE_LEN, RegisterActivity.MAX_PHONE_LEN, res);

        return name && surname && email && password && confirmPassword && phone;
    }



    private void changeName()
    {

    }



    private void changeSurname()
    {

    }



    private void changeEmail()
    {

    }



    private void changePassword()
    {

    }



    private void changePhone()
    {

    }
}
