package com.snavi.swiftlift.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.snavi.swiftlift.R;
import com.snavi.swiftlift.custom_views.WaitingSpinner;
import com.snavi.swiftlift.database_objects.Const;
import com.snavi.swiftlift.utils.InputValidator;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class RegisterActivity extends AppCompatActivity {


    // CONST //////////////////////////////////////////////////////////////////////////////////////
    public static final int MIN_NAME_LEN     = 2;
    public static final int MAX_NAME_LEN     = 30;
    public static final int MIN_SURNAME_LEN  = 2;
    public static final int MAX_SURNAME_LEN  = 30;
    public static final int MIN_PASSWORD_LEN = 6;
    public static final int MAX_PASSWORD_LEN = 30;
    public static final int MIN_PHONE_LEN    = 5;
    public static final int MAX_PHONE_LEN    = 20;
    public static final int ACTIVITY_RESULT_ADDITIONAL_DATA_NOT_LOADED = 12712;


    // fields /////////////////////////////////////////////////////////////////////////////////////
    private FirebaseAuth m_firebaseAuth;

    // user data
    private String m_name;
    private String m_surname;
    private String m_email;
    private String m_password;
    private String m_phone;
    private WaitingSpinner m_waitingSpinner;
    private FirebaseFirestore m_db;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        m_waitingSpinner = findViewById(R.id.activity_register_spinner);
        m_firebaseAuth   = FirebaseAuth.getInstance();
        m_db = FirebaseFirestore.getInstance();

        setSignUpButtonListener();
    }



    private void setSignUpButtonListener()
    {
        Button button = findViewById(R.id.activity_register_but_sign_up);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                boolean isCorrect = validateInputs();
                if (isCorrect)
                {
                    m_waitingSpinner.start();
                    loadUserData();
                    createNewUser();
                }
            }
        });
    }



    private void loadUserData()
    {
        m_name     = ((EditText) findViewById(R.id.activity_register_et_name)).getText().toString();
        m_surname  = ((EditText) findViewById(R.id.activity_register_et_surname)).getText().toString();
        m_email    = ((EditText) findViewById(R.id.activity_register_et_email)).getText().toString();
        m_password = ((EditText) findViewById(R.id.activity_register_et_password)).getText().toString();
        m_phone    = ((EditText) findViewById(R.id.activity_register_et_phone)).getText().toString();
    }



    // input validation ////////////////////////////////////////////////////////////////////////////



    private boolean validateInputs()
    {
//        boolean nameRes    = validateName();
//        boolean surnameRes = validateSurname();
//        boolean emailRes   = validateEmail();
//        boolean confEmail  = validateConfirmEmail();
//        boolean passRes    = validatePassword();
//        boolean confPass   = validateConfirmPassword();
//        boolean phoneRes   = validatePhone();
//
//        return nameRes && surnameRes && emailRes && confEmail && passRes && confPass && phoneRes;

        Resources res = getResources();
        boolean name = InputValidator.validateName(
                (EditText) findViewById(R.id.activity_register_et_name),
                RegisterActivity.MIN_NAME_LEN, RegisterActivity.MAX_NAME_LEN, res);
        boolean surname = InputValidator.validateSurname(
                (EditText) findViewById(R.id.activity_register_et_surname),
                RegisterActivity.MIN_SURNAME_LEN, RegisterActivity.MAX_SURNAME_LEN, res);
        @SuppressLint("CutPasteId") boolean email = InputValidator.validateEmail(
                (EditText) findViewById(R.id.activity_register_et_email), res);
        @SuppressLint("CutPasteId") boolean confirmEmail = InputValidator.validateConfirmEmail(
                (EditText) findViewById(R.id.activity_register_et_email),
                (EditText) findViewById(R.id.activity_register_et_confirm_email), res
        );
        @SuppressLint("CutPasteId") boolean password = InputValidator.validatePassword(
                (EditText) findViewById(R.id.activity_register_et_password),
                RegisterActivity.MIN_PASSWORD_LEN, RegisterActivity.MAX_PASSWORD_LEN, res);
        @SuppressLint("CutPasteId") boolean confirmPassword = InputValidator.validateConfirmPassword(
                (EditText) findViewById(R.id.activity_register_et_password),
                (EditText) findViewById(R.id.activity_register_et_confirm_password),
                res);
        boolean phone = InputValidator.validatePhone(
                (EditText) findViewById(R.id.activity_register_et_phone),
                RegisterActivity.MIN_PHONE_LEN, RegisterActivity.MAX_PHONE_LEN, res);

        return name && surname && email && confirmEmail && password && confirmPassword && phone;
    }



    private boolean validateName()
    {
        EditText name   = findViewById(R.id.activity_register_et_name);
        boolean isValid = InputValidator.validateLength(name.getText().toString(), MIN_NAME_LEN, MAX_NAME_LEN);
        if (!isValid)
            name.setError(getResources().getString(R.string.name_error));

        return isValid;
    }



    private boolean validateSurname()
    {
        EditText sur = findViewById(R.id.activity_register_et_surname);
        boolean isValid = InputValidator.validateLength(sur.getText().toString(), MIN_SURNAME_LEN, MAX_SURNAME_LEN);
        if (!isValid)
            sur.setError(getResources().getString(R.string.surname_error));

        return isValid;
    }



    private boolean validateEmail()
    {
        EditText email = findViewById(R.id.activity_register_et_email);
        boolean isValid = InputValidator.validateEmailRegex(email.getText().toString());
        if (!isValid)
            email.setError(getResources().getString(R.string.email_error));

        return isValid;
    }



    private boolean validateConfirmEmail()
    {
        EditText email = findViewById(R.id.activity_register_et_email);
        EditText confirmEmail = findViewById(R.id.activity_register_et_confirm_email);

        String emailT = email.getText().toString();
        String confirmT = confirmEmail.getText().toString();

        boolean isValid = emailT.equals(confirmT);
        if (!isValid)
            confirmEmail.setError(getResources().getString(R.string.confirm_email_error));

        return isValid;
    }



    private boolean validatePassword()
    {
        EditText pass = findViewById(R.id.activity_register_et_password);
        boolean isValid = InputValidator.validateLength(pass.getText().toString(), MIN_PASSWORD_LEN, MAX_PASSWORD_LEN);
        if (!isValid)
            pass.setError(getResources().getString(R.string.password_error));

        return isValid;
    }



    private boolean validateConfirmPassword()
    {
        EditText confPass = findViewById(R.id.activity_register_et_confirm_password);
        EditText pass = findViewById(R.id.activity_register_et_password);

        String confPassT = confPass.getText().toString();
        String passT = pass.getText().toString();

        boolean isValid = confPassT.equals(passT);
        if (!isValid)
            confPass.setError(getResources().getString(R.string.confirm_password_error));

        return isValid;
    }



    private boolean validatePhone()
    {
        EditText phone = findViewById(R.id.activity_register_et_phone);
        boolean isValid = InputValidator.validateLength(phone.getText().toString(), MIN_PHONE_LEN, MAX_PHONE_LEN);
        if (!isValid)
            phone.setError(getResources().getString(R.string.phone_error));

        return isValid;
    }



    // firebase registration //////////////////////////////////////////////////////////////////////



    private void createNewUser()
    {
        Task task = m_firebaseAuth.createUserWithEmailAndPassword(m_email, m_password);
        setOnFirebaseRegistrationCompleteListener(task);
    }


    /**
     * method, that sets listener for Firebase registration complete. Once Firebase registration is
     * completed additional user data is saved into database;
     * @param task createUserWithEmailAndPassword task
     */
    private void setOnFirebaseRegistrationCompleteListener(Task task)
    {
        task.addOnCompleteListener(new OnCompleteListener() {
            @Override
            public void onComplete(@NonNull Task task)
            {

                if (task.isSuccessful())
                {
                    dealWithSuccessfulFirebaseRegister();
                }
                else
                {
                    dealWithRegistrationFailure(task);
                    m_waitingSpinner.stop();
                }
            }
        });
    }


    /**
     * method that initializes saving additional user data.
     */
    private void dealWithSuccessfulFirebaseRegister()
    {
        String userId = m_firebaseAuth.getUid();

        m_db.collection(Const.USERS_COLLECTION)
                .document(Objects.requireNonNull(userId)).set(createUserForDatabase())
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid)
                    {
                        dealWithSuccessfulAdditionalUserDataWrite();
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e)
                    {
                        dealWithAdditionalUserDataWriteFailure();
                    }
        });

        FirebaseUser user = m_firebaseAuth.getCurrentUser();
        if (user != null)
            user.sendEmailVerification();
    }



    private void dealWithSuccessfulAdditionalUserDataWrite()
    {
        Toast.makeText(RegisterActivity.this,
                getResources().getText(R.string.registration_successful),
                Toast.LENGTH_LONG)
                .show();
        m_waitingSpinner.stop();
        setResult(Activity.RESULT_OK);
        finish();
    }



    private void dealWithAdditionalUserDataWriteFailure()
    {
        showSnackbarIncompleteRegister();
        m_waitingSpinner.stop();
        setResult(ACTIVITY_RESULT_ADDITIONAL_DATA_NOT_LOADED);
        finish();
    }



    private Map<String, Object> createUserForDatabase()
    {
        Map<String, Object> user = new HashMap<>();
        user.put(Const.USER_NAME, m_name);
        user.put(Const.USER_SURNAME, m_surname);
        user.put(Const.USER_PHONE, m_phone);
        user.put(Const.USER_PHONE_VERIFIED, false);

        return user;
    }



    private void dealWithRegistrationFailure(Task task)
    {
        task.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                if (e instanceof FirebaseAuthException)
                {
                    FirebaseAuthException exc = (FirebaseAuthException) e;
                    String errorCode = exc.getErrorCode();

                    switch (errorCode)
                    {
                        case "ERROR_EMAIL_ALREADY_IN_USE" : showSnackbarEmailAlreadyInUse(); return;
                    }

                    return;
                }
                else if (e instanceof FirebaseNetworkException)
                {
                    showSnackbarNetworkError();
                    return;
                }

                showSnackbarUnknownError();
            }
        });
    }



    private void showSnackbarEmailAlreadyInUse()
    {
        Snackbar.make(findViewById(R.id.activity_register_cl), R.string.registration_failure_email_in_use, Snackbar.LENGTH_INDEFINITE)
                .setAction(R.string.snackbar_ok, new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {}
                }).show();
    }



    private void showSnackbarUnknownError()
    {
        Snackbar.make(findViewById(R.id.activity_register_cl), R.string.registration_failure_unknown_error, Snackbar.LENGTH_INDEFINITE)
                .setAction(R.string.snackbar_ok, new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {}
                }).show();
    }



    private void showSnackbarIncompleteRegister()
    {
        Snackbar.make(findViewById(R.id.activity_register_cl), R.string.incomplete_register, Snackbar.LENGTH_LONG)
                .setAction(R.string.snackbar_ok, new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {}
                }).show();
    }



    private void showSnackbarNetworkError()
    {
        Snackbar.make(findViewById(R.id.activity_register_cl), R.string.network_error, Snackbar.LENGTH_INDEFINITE)
                .setAction(R.string.snackbar_ok, new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {}
                }).show();
    }
}
