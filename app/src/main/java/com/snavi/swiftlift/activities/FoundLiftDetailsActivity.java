package com.snavi.swiftlift.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.snavi.swiftlift.R;
import com.snavi.swiftlift.database_objects.Const;
import com.snavi.swiftlift.database_objects.StorageConst;
import com.snavi.swiftlift.lift.FoundLift;
import com.snavi.swiftlift.utils.FirebaseUtils;
import com.snavi.swiftlift.utils.Toasts;
import com.squareup.picasso.Picasso;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FoundLiftDetailsActivity extends AppCompatActivity {

    // CONST ///////////////////////////////////////////////////////////////////////////////////////
    // keys
    public static final String LIFT_KEY = "lift";
    // errors
    private static final String FOUND_LIFT_NULL_ERROR = "Found lift wasn't passed properly via intent. It must be put to intent as Parcelable under FoundLiftDetailsActivity.LIFT_KEY key.";


    // fields //////////////////////////////////////////////////////////////////////////////////////
    private FoundLift           m_foundLift;
    private FirebaseFirestore   m_db;
    private String              m_userId;
    private DocumentSnapshot    m_ownerDoc;
    private FirebaseAuth        m_auth;

    // views
    private ImageButton m_imgbutCall;
    private ImageButton m_imgbutMessage;
    private ImageButton m_imgbutEmail;
    private TextView    m_tvDepAddr;
    private TextView    m_tvArrAddr;
    private TextView    m_tvDepDate;
    private TextView    m_tvArrDate;
    private TextView    m_tvPrice;
    private TextView    m_tvOwnerName;
    private TextView    m_tvDescription;
    private ImageView   m_imgOwnerPhoto;



    // init ////////////////////////////////////////////////////////////////////////////////////////


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_found_lift_details);

        initFirebase();
        initFoundLift();
        initViews();
        initViewsValues();
        setButtonsListeners();
    }



    private void initFoundLift()
    {
        Intent intent   = getIntent();
        m_foundLift     = intent.getParcelableExtra(LIFT_KEY);

        if (m_foundLift == null)
            throw new RuntimeException(FOUND_LIFT_NULL_ERROR);
    }



    private void initOwner()
    {
        m_db.collection(Const.USERS_COLLECTION)
                .document(m_foundLift.getOwnerId())
                .get()
                .addOnSuccessListener(
                        new OnSuccessListener<DocumentSnapshot>() {
                            @Override
                            public void onSuccess(DocumentSnapshot documentSnapshot)
                            {
                                m_ownerDoc = documentSnapshot;
                                setOwnerName();
                            }
                        }
                );
    }



    private void initViews()
    {
        m_imgbutCall    = findViewById(R.id.activity_found_lift_details_imgBut_call);
        m_imgbutMessage = findViewById(R.id.activity_found_lift_details_imgBut_message);
        m_imgbutEmail   = findViewById(R.id.activity_found_lift_details_imgBut_email);
        m_tvDepAddr     = findViewById(R.id.activity_found_lift_details_tv_from_address);
        m_tvArrAddr     = findViewById(R.id.activity_found_lift_details_tv_to_address);
        m_tvDepDate     = findViewById(R.id.activity_found_lift_details_tv_from_date);
        m_tvArrDate     = findViewById(R.id.activity_found_lift_details_tv_to_date);
        m_tvPrice       = findViewById(R.id.activity_found_lift_details_tv_price);
        m_tvOwnerName   = findViewById(R.id.activity_found_lift_details_tv_owner_name);
        m_tvDescription = findViewById(R.id.activity_found_lift_details_tv_description);
        m_imgOwnerPhoto = findViewById(R.id.activity_found_lift_details_img_owner);
    }



    private void initViewsValues()
    {
        m_tvDepAddr.setText(m_foundLift.getFrom());
        m_tvArrAddr.setText(m_foundLift.getTo());
        m_tvDepDate.setText(m_foundLift.getDepDateString());
        m_tvArrDate.setText(m_foundLift.getArrDateString());
        m_tvPrice.setText(m_foundLift.getPrice());
        m_tvDescription.setText(m_foundLift.getDescription());

        initOwner();
        setOwnerPhoto();
    }



    private void setOwnerName()
    {
        if (m_ownerDoc != null)
        {
            String ownerName =
                    (String) m_ownerDoc.get(Const.USER_NAME);

            String ownerSurname =
                    (String) m_ownerDoc.get(Const.USER_SURNAME);

            m_tvOwnerName.setText(concatOwnerName(ownerName, ownerSurname));
        }
        else
        {
            Toasts.showCantFindOwnerData(FoundLiftDetailsActivity.this);
        }
    }



    private String concatOwnerName(String ownerName, String ownerSurname)
    {
        if (ownerName == null)
        {
            Toasts.showCantFindOwnerNameToast(FoundLiftDetailsActivity.this);
            ownerName = "";
        }

        if (ownerSurname == null)
        {
            Toasts.showCantFindOwnerSurnameToast(FoundLiftDetailsActivity.this);
            ownerSurname = "";
        }

        return ownerName + " " + ownerSurname;
    }



    private void setOwnerPhoto()
    {
        StorageReference profilePhotoReference = FirebaseUtils.getUserPhotoStorageReference(
                m_foundLift.getOwnerId());

        profilePhotoReference.getDownloadUrl()
                .addOnSuccessListener(
                        new OnSuccessListener<Uri>() {
                            @Override
                            public void onSuccess(Uri uri)
                            {
                                Picasso.get().load(uri).into(m_imgOwnerPhoto);
                            }
                        }
                );
    }



    private void initFirebase()
    {
        m_db = FirebaseFirestore.getInstance();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null)
            m_userId = user.getUid();

        m_auth = FirebaseAuth.getInstance();
    }



    // buttons /////////////////////////////////////////////////////////////////////////////////////



    private void setButtonsListeners()
    {
        setCallButtonListener();
        setMessageButtonListener();
        setEmailButtonListener();
    }



    private void setCallButtonListener()
    {
        m_imgbutCall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                if (m_auth.getCurrentUser() != null)
                {
                    Intent intent       = new Intent(Intent.ACTION_DIAL);
                    String uriString    = "tel:" + m_ownerDoc.get(Const.USER_PHONE);
                    intent.setData(Uri.parse(uriString));
                    startActivity(intent);
                }
                else
                    Toasts.showYouMustBeSignedIdToast(FoundLiftDetailsActivity.this);
            }
        });
    }



    private void setMessageButtonListener()
    {
        m_imgbutMessage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                if (m_auth.getCurrentUser() != null)
                {
                    Intent intent = new Intent(Intent.ACTION_VIEW,
                            Uri.fromParts("sms", (String) m_ownerDoc.get(Const.USER_PHONE),
                                    null));
                    startActivity(intent);
                }
                else
                    Toasts.showYouMustBeSignedIdToast(FoundLiftDetailsActivity.this);
            }
        });
    }



    private void setEmailButtonListener()
    {
        m_imgbutEmail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                if (m_auth.getCurrentUser() != null)
                {
                    String ownerEmail   = (String) m_ownerDoc.get(Const.USER_EMAIL);
                    String uriString    = "mailto:" + ownerEmail;
                    Intent intent       = new Intent(Intent.ACTION_SENDTO);
                    intent.setData(Uri.parse(uriString));
                    intent.putExtra(Intent.EXTRA_SUBJECT,
                            getString(R.string.request_lift_email_subject));

                    startActivity(intent);
                }
                else
                    Toasts.showYouMustBeSignedIdToast(FoundLiftDetailsActivity.this);
            }
        });
    }



    // firebase fcm listeners //////////////////////////////////////////////////////////////////////



    private class OnSearchLiftOwnerTokenCompleteListener
            implements OnCompleteListener<QuerySnapshot> {

        @Override
        public void onComplete(@NonNull Task<QuerySnapshot> task)
        {
            if (task.isSuccessful())
            {
                QuerySnapshot result = task.getResult();
                if (result != null)
                {
                    List<DocumentSnapshot> docs = result.getDocuments();
                    if (docs.isEmpty())
                        Toasts.showReqSendErrorToast(FoundLiftDetailsActivity.this);
                    else
                    {
                        DocumentSnapshot tokenSnap = docs.get(0);
                        if (tokenSnap.contains(Const.FCM_TOKEN))
                        {
                            String liftOwnerToken = (String) tokenSnap.get(Const.FCM_TOKEN);
                            sendEnrollMessage(liftOwnerToken);
                        }
                        else
                            Toasts.showReqSendErrorToast(FoundLiftDetailsActivity.this);
                    }
                }
                else
                    Toasts.showReqSendErrorToast(FoundLiftDetailsActivity.this);
            }
            else
                Toasts.showReqSendErrorToast(FoundLiftDetailsActivity.this);
        }
    }



    private void sendEnrollMessage(String liftOwnerToken)
    {
        if (m_userId == null)
        {
            Toasts.showAuthErrorToast(this);
            return;
        }

        Map<String, Object> message = new HashMap<>();
        message.put(Const.REQ_LIFT_OWNER_TOKEN, liftOwnerToken);
        message.put(Const.REQ_LIFT, m_foundLift.getId());
        message.put(Const.REQ_SENDER, m_userId);
        message.put(Const.REQ_FROM_STRETCH, m_foundLift.getStartStretch().getId());
        message.put(Const.REQ_TO_STRETCH, m_foundLift.getEndStretch().getId());

        m_db.collection(Const.REQ_COLLECTION)
                .document()
                .set(message)
                .addOnFailureListener(
                        new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e)
                            {
                                Toasts.showReqSendErrorToast(FoundLiftDetailsActivity.this);
                            }
                        }
                );
    }

}
