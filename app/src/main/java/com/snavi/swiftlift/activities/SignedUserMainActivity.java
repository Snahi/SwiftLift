package com.snavi.swiftlift.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;

import com.snavi.swiftlift.R;
import com.snavi.swiftlift.utils.InputValidator;

public class SignedUserMainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signed_user_main);

        setButtonsListeners();
    }



    private void setButtonsListeners()
    {
        setSettingsButtonListener();
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
}
