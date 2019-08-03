package com.snavi.swiftlift.activities.users_data;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.snavi.swiftlift.R;
import com.snavi.swiftlift.database_objects.Const;
import com.snavi.swiftlift.utils.FirebaseUtils;
import com.snavi.swiftlift.utils.InputValidator;
import com.snavi.swiftlift.utils.Toasts;
import com.squareup.picasso.Picasso;

import java.util.HashMap;
import java.util.Map;

public class PersonalDataUpdateActivity extends AppCompatActivity {


    // CONST //////////////////////////////////////////////////////////////////////////////////////
    // request codes
    public static final int REQ_PROFILE_PHOTO = 17731;
    // errors
    public static final String NULL_FIREBASE_AUTH_ERROR = "null FirebaseAuth error";
    public static final String NULL_USER_ERROR = "null user error";
    public static final String LOAD_DATA_ERROR = "load data error";
    public static final String LOAD_DATA_ERROR_NULL_DOC_SNAP = "load data error - null DocumentSnapshot";
    // other
    public static final String  TAG = PersonalDataUpdateActivity.class.getName();



    // fields /////////////////////////////////////////////////////////////////////////////////////
    private FirebaseFirestore   m_db;
    private FirebaseUser        m_currUser;

    // views
    private EditText    m_etName;
    private EditText    m_etSurname;
    private EditText    m_etPhone;
    private ImageView   m_imgUserPhoto;
    private Button      m_butSave;
    private ProgressBar m_progressBar;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_personal_data_update);

        boolean firebaseInitRes = initFirebase();
        if (!firebaseInitRes)
        {
            setResult(Activity.RESULT_CANCELED);
            finish();
            return;
        }

        initViews();
        loadData();
        initButtons();
        setProfilePhotoOnClickListener();
    }



    private boolean initFirebase()
    {
        m_db = FirebaseFirestore.getInstance();

        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth == null)
        {
            Log.e(TAG, NULL_FIREBASE_AUTH_ERROR);
            showLoadDataFailureToast();
            return false;
        }

        m_currUser = auth.getCurrentUser();
        if (m_currUser == null)
        {
            Log.e(TAG, NULL_USER_ERROR);
            showLoadDataFailureToast();
            return false;
        }

        return true;
    }



    private void initViews()
    {
        m_etName            = findViewById(R.id.activity_personal_data_update_et_name);
        m_etSurname         = findViewById(R.id.activity_personal_data_update_et_surname);
        m_etPhone           = findViewById(R.id.activity_personal_data_update_et_phone);
        m_butSave           = findViewById(R.id.activity_personal_data_update_but_save);
        m_progressBar       = findViewById(R.id.activity_personal_data_progress_bar);
        m_imgUserPhoto      = findViewById(R.id.activity_personal_data_img_profile_photo);
    }



    private void loadData()
    {
        loadUserImage(m_currUser);

        DocumentReference docRef = m_db.collection(Const.USERS_COLLECTION)
                .document(m_currUser.getUid());
        docRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful())
                {
                    DocumentSnapshot docSnap = task.getResult();
                    if (docSnap == null)
                    {
                        Log.e(TAG, LOAD_DATA_ERROR_NULL_DOC_SNAP);
                        showLoadDataFailureToast();
                        setResult(Activity.RESULT_CANCELED);
                        finish();
                        return;
                    }
                    loadDataIntoEditTexts(task.getResult());
                    m_progressBar.setVisibility(View.GONE);
                    m_butSave.setEnabled(true);
                }
                else
                {
                    Log.e(TAG, LOAD_DATA_ERROR);
                    showLoadDataFailureToast();
                    setResult(Activity.RESULT_CANCELED);
                    finish();
                }
            }
        });
    }



    private void loadDataIntoEditTexts(DocumentSnapshot docSnap)
    {
        m_etName.setText(docSnap.getString(Const.USER_NAME));
        m_etSurname.setText(docSnap.getString(Const.USER_SURNAME));
        m_etPhone.setText(docSnap.getString(Const.USER_PHONE));
    }



    private void loadUserImage(FirebaseUser user)
    {
        Uri photoUrl = user.getPhotoUrl();
        if (photoUrl == null)
            return;

        Picasso.get().load(user.getPhotoUrl()).into(m_imgUserPhoto);
    }



    private void initButtons()
    {
        setSaveButListener();
    }



    private void setProfilePhotoOnClickListener()
    {
        m_imgUserPhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setType("image/*");
                startActivityForResult(intent, REQ_PROFILE_PHOTO);
            }
        });
    }



    private void setSaveButListener()
    {
        m_butSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                if (validateInput())
                {
                    m_butSave.setEnabled(false);
                    m_progressBar.setVisibility(View.VISIBLE);
                    updateData();
                }
            }
        });
    }



    private boolean validateInput()
    {
        Resources res = getResources();
        boolean name = InputValidator.validateName(
                m_etName, RegisterActivity.MIN_NAME_LEN, RegisterActivity.MAX_NAME_LEN, res);
        boolean surname = InputValidator.validateSurname(
                m_etSurname, RegisterActivity.MIN_SURNAME_LEN, RegisterActivity.MAX_SURNAME_LEN, res);
        boolean phone = InputValidator.validatePhone(
                m_etPhone, RegisterActivity.MIN_PHONE_LEN, RegisterActivity.MAX_PHONE_LEN, res);

        return name && surname && phone;
    }



    private void updateData()
    {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth == null)
        {
            dealWithNullAuth();
            m_butSave.setEnabled(true);
            m_progressBar.setVisibility(View.GONE);
            return;
        }

        FirebaseUser user = auth.getCurrentUser();
        if (user == null)
        {
            dealWithNullUser();
            m_butSave.setEnabled(true);
            m_progressBar.setVisibility(View.GONE);
            return;
        }

        DocumentReference doc = m_db.collection(Const.USERS_COLLECTION).document(user.getUid());

        update(doc);
    }



    private void dealWithNullAuth()
    {
        Log.e(TAG, NULL_FIREBASE_AUTH_ERROR);
        showUpdateFailureSnackbar();
    }



    private void dealWithNullUser()
    {
        Log.e(TAG, NULL_USER_ERROR);
        showUpdateFailureSnackbar();
    }



    private Map<String, Object> createUpdateMap(String name, String surname, String phone)
    {
        Map<String, Object> res = new HashMap<>();
        res.put(Const.USER_NAME, name);
        res.put(Const.USER_SURNAME, surname);
        res.put(Const.USER_PHONE, phone);

        return res;
    }



    private void update(DocumentReference doc)
    {
        String name    = m_etName.getText().toString();
        String surname = m_etSurname.getText().toString();
        String phone   = m_etPhone.getText().toString();

        Map<String, Object> update = createUpdateMap(name, surname, phone);

        doc.update(update).addOnCompleteListener(new OnCompleteListener<Void>() {
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
                {
                    showUpdateFailureSnackbar();
                }

                m_butSave.setEnabled(true);
                m_progressBar.setVisibility(View.GONE);
            }
        });
    }



    // activity results ////////////////////////////////////////////////////////////////////////////



    @Override
    public void onActivityResult(int reqCode, int resCode, Intent data)
    {
        super.onActivityResult(reqCode, resCode, data);

        switch (reqCode)
        {
            case REQ_PROFILE_PHOTO : setUserPhoto(resCode, data); break;
        }
    }



    private void setUserPhoto(int resCode, Intent data)
    {
        if (resCode == Activity.RESULT_CANCELED)
            return;

        if (data == null)
        {
            Toasts.showPhotoUploadError(this);
            return;
        }

        Uri photoUri = data.getData();

        if (photoUri == null)
        {
            Toasts.showPhotoUploadError(this);
            return;
        }

        FirebaseUtils.setUserPhoto(m_currUser, photoUri, this, m_imgUserPhoto,
                m_progressBar);
    }



    // Toasts and snackbars ////////////////////////////////////////////////////////////////////////



    private void showSuccessfulUpdateToast()
    {
        Toast.makeText(this, R.string.successful_update, Toast.LENGTH_LONG).show();
    }



    private void showUpdateFailureSnackbar()
    {
        Snackbar.make(
                findViewById(R.id.activity_password_update_cl),
                R.string.update_fail,
                Snackbar.LENGTH_INDEFINITE)
                .setAction(R.string.snackbar_ok, new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {}
                }).show();
    }



    private void showLoadDataFailureToast()
    {
        Toast.makeText(this, R.string.load_data_failure, Toast.LENGTH_LONG).show();
    }

}
