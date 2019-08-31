package com.snavi.swiftlift.activities;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;
import com.snavi.swiftlift.activities.users_data.RegisterActivity;
import com.snavi.swiftlift.database_objects.Const;
import com.snavi.swiftlift.sign_in.LogInFragment;
import com.snavi.swiftlift.R;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class MainActivity extends FragmentActivity implements LogInFragment.OnFragmentInteractionListener {

    // CONST ///////////////////////////////////////////////////////////////////////////////////////
    public  static final String LOGIN_POPUP_TAG                       = "log in";
    private static final int    NUM_OF_TRIALS_WHEN_INSTANCE_ID_FAILS  = 30;


    // fields //////////////////////////////////////////////////////////////////////////////////////
    private ProgressBar         m_progressBar;
    private FirebaseFirestore   m_db;
    private String              m_userId;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        m_db = FirebaseFirestore.getInstance();

        if (isUserSignedIn())
        {
            moveToSignedUserActivity();
            updateFCMToken(NUM_OF_TRIALS_WHEN_INSTANCE_ID_FAILS);
            return;
        }

        setContentView(R.layout.activity_main);

        initViews();

        m_progressBar.setVisibility(View.GONE);

        setButtonsListeners();
    }



    private boolean isUserSignedIn()
    {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth == null) return false;

        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return false;
        m_userId    = user.getUid();

        return user.isEmailVerified();
    }



    private void moveToSignedUserActivity()
    {
        Intent intent = new Intent(this, SignedUserMainActivity.class);
        startActivity(intent);
        finish();
    }


    /**
     *                              find token instanceId
     *                                       |
     *                                       |
     *                           does token exist in database?
     *                                        |
     *                    ____________________|_____________________
     *                   |         no                   yes         |
     *                   |                                          |
     *  does user exist in tokens collection?           change owner to current user
     *                   |                                          |
     *        ___________|___________                               |
     *       |     no         yes    |                              |
     *       |                       |                              |
     *  create new token      change user token                     |
     *       |                       |                              |
     *       |_______________________|______________________________|
     *                                        |
     *                                        |
     *                                       END
     *
     * @param numOfTrials how many times to try if fails
     */
    private void updateFCMToken(int numOfTrials)
    {
        FirebaseInstanceId.getInstance()
                .getInstanceId()
                .addOnCompleteListener(new OnInstanceIdCompleteListener(numOfTrials));
    }



    private void initViews()
    {
        m_progressBar = findViewById(R.id.activity_main_progress_bar);
    }



    private void setButtonsListeners()
    {
        setLoginButtonListener();
    }



    private void setLoginButtonListener()
    {
        Button button = findViewById(R.id.activity_main_but_log_in);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showLoginPopup();
            }
        });
    }



    private void showLoginPopup()
    {
        FragmentManager fragmentManager = getSupportFragmentManager();
        LogInFragment loginFragment     = new LogInFragment();
        loginFragment.show(fragmentManager, LOGIN_POPUP_TAG);
    }



    // login fragment //////////////////////////////////////////////////////////////////////////////



    @Override
    public void successfulLogin()
    {
        moveToSignedUserActivity();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null)
        {
            m_userId = user.getUid();
            updateFCMToken(NUM_OF_TRIALS_WHEN_INSTANCE_ID_FAILS);
        }
    }



    @Override
    public void userWantsToRegister()
    {
        Intent intent = new Intent(this, RegisterActivity.class);
        startActivity(intent);
    }



    // firebase listeners //////////////////////////////////////////////////////////////////////////


    /**
     * Is called when token instanceId completes.
     *
     *                              find token instanceId
     *                                       |
     *                                       |
     *                           does token exist in database?
     *
     */
    private class OnInstanceIdCompleteListener implements OnCompleteListener<InstanceIdResult> {

        // fields //////////////////////////////////////////////////////////////////////////////////
        /**
         * when the task fails or write to database fails, then if this field is greater than 0,
         * then can try again and decrease this field by 1.
         */
        private int     m_numOfTrials;



        private OnInstanceIdCompleteListener(int numOfTrials)
        {
            m_numOfTrials   = numOfTrials;
        }



        @Override
        public void onComplete(@NonNull Task<InstanceIdResult> task)
        {
            if (task.isSuccessful())
            {
                InstanceIdResult result = task.getResult();
                if (result != null)
                {
                    String instanceId = result.getToken();
                    findTokenInTokensCollection(instanceId);
                }
            }
            else
            {
                if (m_numOfTrials > 0)
                    updateFCMToken(m_numOfTrials - 1);
            }
        }



        private void findTokenInTokensCollection(String instanceId)
        {
            m_db.collection(Const.FCM_TOKENS_COLLECTION)
                    .whereEqualTo(Const.FCM_TOKEN, instanceId)
                    .get()
                    .addOnCompleteListener(
                            new OnTokenInTokensCollectionCompleteListener(m_numOfTrials,
                                    instanceId));
        }
    }


    /**
     * Is called when search for token in Const.FCM_TOKENS_COLLECTION completes.
     *
     *                              find token instanceId
     *                                       |
     *                                       |
     *                           does token exist in database?
     *                                        |
     *                    ____________________|_____________________
     *                   |         no                   yes         |
     *                   |                                          |
     *  does user exist in tokens collection?           change owner to current user
     *                                                              |
     *                                                              |
     *                                                              |
     *                                                              |
     *                                                              |
     *                                                              |
     *                                         _____________________|
     *                                        |
     *                                        |
     *                                       END
     *
     */
    private class OnTokenInTokensCollectionCompleteListener
            implements OnCompleteListener<QuerySnapshot> {

        private int     m_numOfTrials;
        private String  m_token;


        private OnTokenInTokensCollectionCompleteListener(int numOfTrials, String token)
        {
            m_numOfTrials   = numOfTrials;
            m_token         = token;
        }



        @Override
        public void onComplete(@NonNull Task<QuerySnapshot> task)
        {
            if (task.isSuccessful())
            {
                QuerySnapshot tokenDocSnap = task.getResult();
                if (tokenDocSnap != null)
                {
                    List<DocumentSnapshot> results = tokenDocSnap.getDocuments();

                    if (results.isEmpty())          // no token is in db, search for user and assign token to him (if user exists) or create new token doc
                        findUserAndAssignToken();
                    else
                        updateTokenOwner(results);
                }
                else
                {
                    if (m_numOfTrials > 0)
                        updateFCMToken(m_numOfTrials - 1);
                }
            }
            else
            {
                if (m_numOfTrials > 0)
                    updateFCMToken(m_numOfTrials - 1);
            }
        }



        private void findUserAndAssignToken()
        {
            m_db.collection(Const.FCM_TOKENS_COLLECTION)
                    .whereEqualTo(Const.FCM_TOKEN_OWNER, m_userId)
                    .get()
                    .addOnCompleteListener(new OnFindUserInTokensColCompleteListener(m_numOfTrials,
                            m_token));
        }



        private void updateTokenOwner(List<DocumentSnapshot> results)
        {
            Map<String, Object> tokenUpdateMap = new HashMap<>();
            tokenUpdateMap.put(Const.FCM_TOKEN_OWNER, m_userId);

            String tokenDocId = results.get(0).getId();
            m_db.collection(Const.FCM_TOKENS_COLLECTION)
                    .document(tokenDocId)
                    .set(tokenUpdateMap, SetOptions.merge())
                    .addOnFailureListener(
                            new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e)
                                {
                                    if (m_numOfTrials > 0)
                                        updateFCMToken(m_numOfTrials - 1);
                                }
                            }
                    );
        }
    }


    /** Is called when search for user in tokens collection completes. If user does not exist new
     * token document is created. If user exists, then his token is changed.
     *
     *                              find token instanceId
     *                                       |
     *                                       |
     *                           does token exist in database?
     *                                        |
     *                    ____________________|
     *                   |         no
     *                   |
     *  does user exist in tokens collection?
     *                   |
     *        ___________|___________
     *       |     no         yes    |
     *       |                       |
     *  create new token      change user token
     *       |                       |
     *       |_______________________|________
     *                                        |
     *                                        |
     *                                       END
     */
    private class OnFindUserInTokensColCompleteListener
            implements OnCompleteListener<QuerySnapshot> {


        private int    m_numOfTrials;
        private String m_token;


        private OnFindUserInTokensColCompleteListener(int numOfTrials, String token)
        {
            m_token         = token;
            m_numOfTrials   = numOfTrials;
        }



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
                        createNewTokenDoc();
                    else
                        changeUserToken(docs.get(0).getId());
                }
            }
            else
            {
                if (m_numOfTrials > 0)
                    updateFCMToken(m_numOfTrials - 1);
            }
        }



        private void createNewTokenDoc()
        {
            Map<String, Object> tokenMap = new HashMap<>();
            tokenMap.put(Const.FCM_TOKEN, m_token);
            tokenMap.put(Const.FCM_TOKEN_OWNER, m_userId);

            m_db.collection(Const.FCM_TOKENS_COLLECTION)
                    .document()
                    .set(tokenMap)
                    .addOnFailureListener(
                            new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e)
                                {
                                    if (m_numOfTrials > 0)
                                        updateFCMToken(m_numOfTrials - 1);
                                }
                            }
                    );
        }



        private void changeUserToken(String tokenDocId)
        {
            Map<String, Object> tokenUpdateMap = new HashMap<>();
            tokenUpdateMap.put(Const.FCM_TOKEN, m_token);

            m_db.collection(Const.FCM_TOKENS_COLLECTION)
                    .document(tokenDocId)
                    .set(tokenUpdateMap, SetOptions.merge())
                    .addOnFailureListener(
                            new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e)
                                {
                                    if (m_numOfTrials > 0)
                                        updateFCMToken(m_numOfTrials - 1);
                                }
                            }
                    );

        }
    }

}
