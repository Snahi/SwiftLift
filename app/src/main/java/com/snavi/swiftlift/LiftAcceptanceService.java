package com.snavi.swiftlift;


import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.snavi.swiftlift.database_objects.Const;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import androidx.annotation.NonNull;

public class LiftAcceptanceService extends FirebaseMessagingService {

    // CONST ///////////////////////////////////////////////////////////////////////////////////////
    private static final int NUM_OF_TRIALS_IF_FAILED_TO_UPDATE_TOKEN = 30;


    public LiftAcceptanceService()
    {
    }


    @Override
    public void onMessageReceived(RemoteMessage remoteMessage)
    {
        super.onMessageReceived(remoteMessage);
    }



    /**
     * When new token is generated and user is signed in, the token will be set as current user
     * token. User can have only one token and token can be owned only by one user.
     */
    @Override
    public void onNewToken(String newToken)
    {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth == null) return;

        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return;

        swapToken(newToken, user, NUM_OF_TRIALS_IF_FAILED_TO_UPDATE_TOKEN);
    }



    private void swapToken(final String             newToken,
                           final FirebaseUser       user,
                           final int                numOfTrials)
    {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        swapTokenChain1_findPreviousToken(newToken, db, user, numOfTrials);
    }



    private void swapTokenChain1_findPreviousToken(final String             newToken,
                                                   final FirebaseFirestore  db,
                                                   final FirebaseUser       user,
                                                   final int                numOfTrials)
    {
        db.collection(Const.FCM_TOKENS_COLLECTION)
                .whereEqualTo(Const.FCM_TOKEN_OWNER, user.getUid())
                .get()
                .addOnSuccessListener(
                        new OnSuccessListener<QuerySnapshot>() {
                            @Override
                            public void onSuccess(QuerySnapshot queryDocumentSnapshots)
                            {
                                swapTokenChain2_updateOrRemoveToken(newToken, db, user, numOfTrials,
                                        queryDocumentSnapshots);
                            }
                        }
                )
                .addOnFailureListener(
                        new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                if (numOfTrials > 0)
                                {
                                    swapTokenChain1_findPreviousToken(newToken, db, user,
                                            numOfTrials - 1);
                                }
                            }
                        }
                );
    }



    private void swapTokenChain2_updateOrRemoveToken(final String               newToken,
                                                     final FirebaseFirestore    db,
                                                     final FirebaseUser         user,
                                                     final int                  numOfTrials,
                                                     QuerySnapshot              querySnap)
    {
        List<DocumentSnapshot> docs = querySnap.getDocuments();
        if (docs.isEmpty())
            swapTokenChain3_createTokenDoc(newToken, db, user, numOfTrials);
        else
            swapTokenChain2_1_removePrevToken(newToken, db, user, numOfTrials,
                    querySnap.getDocuments().get(0));
    }



    private void swapTokenChain2_1_removePrevToken(final String               newToken,
                                                   final FirebaseFirestore    db,
                                                   final FirebaseUser         user,
                                                   final int                  numOfTrials,
                                                   final DocumentSnapshot     docSnap)
    {
        docSnap.getReference()
                .delete()
                .addOnSuccessListener(
                        new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid)
                            {
                                swapTokenChain3_createTokenDoc(newToken, db, user, numOfTrials);
                            }
                        }
                )
                .addOnFailureListener(
                        new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e)
                            {
                                if (numOfTrials > 0)
                                {
                                    swapTokenChain2_1_removePrevToken(newToken, db, user,
                                            numOfTrials - 1, docSnap);
                                }
                            }
                        }
                );
    }



    private void swapTokenChain3_createTokenDoc(final String              newToken,
                                                final FirebaseFirestore   db,
                                                final FirebaseUser        user,
                                                final int                 numOfTrials)
    {
        Map<String, Object> tokenMap = new HashMap<>();
        tokenMap.put(Const.FCM_TOKEN, newToken);
        tokenMap.put(Const.FCM_TOKEN_OWNER, user.getUid());

        db.collection(Const.FCM_TOKENS_COLLECTION).document()
                .set(tokenMap)
                .addOnFailureListener(
                        new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e)
                            {
                                if (numOfTrials > 0)
                                {
                                    swapTokenChain3_createTokenDoc(newToken, db, user,
                                            numOfTrials - 1);
                                }
                            }
                        }
                );
    }



}
