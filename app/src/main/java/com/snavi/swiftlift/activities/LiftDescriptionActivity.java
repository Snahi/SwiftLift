package com.snavi.swiftlift.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.snavi.swiftlift.R;

public class LiftDescriptionActivity extends AppCompatActivity {

    // CONST ///////////////////////////////////////////////////////////////////////////////////////
    public static int MAX_DESCRIPTION_LEN = 500;
    // keys
    public static String DESCRIPTION_KEY = "d";


    // fields //////////////////////////////////////////////////////////////////////////////////////
    private EditText    m_etDescription;
    private Button      m_butSave;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lift_description);

        initViews();
        initViewsValues();
        setButtonsListeners();
    }



    private void initViews()
    {
        m_etDescription = findViewById(R.id.activity_lift_description_et_description);
        m_butSave       = findViewById(R.id.activity_lift_description_but_save);
    }



    private void initViewsValues()
    {
        String currDesc = getCurrentDescription();

        if (!currDesc.isEmpty())
        {
            m_etDescription.setText(getCurrentDescription());
        }
    }



    private String getCurrentDescription()
    {
        Intent data = getIntent();
        if (data == null)
            return "";

        String desc = data.getStringExtra(DESCRIPTION_KEY);

        return desc == null ? "" : desc;
    }



    private void setButtonsListeners()
    {
        setSaveButtonListener();
    }



    private void setSaveButtonListener()
    {
        m_butSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                if (validateDescription())
                {
                    Intent result = new Intent();
                    result.putExtra(DESCRIPTION_KEY, m_etDescription.getText().toString());
                    setResult(Activity.RESULT_OK);
                    setResult(Activity.RESULT_OK, result);
                    finish();
                }
            }
        });
    }



    private boolean validateDescription()
    {
        String desc = m_etDescription.getText().toString();

        if (desc.length() > MAX_DESCRIPTION_LEN)
        {
            m_etDescription.setError(
                    getString(R.string.too_long_description) + " " + MAX_DESCRIPTION_LEN);

            return false;
        }

        return true;
    }
}
