package com.snavi.swiftlift.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.snavi.swiftlift.database_objects.StorageConst;
import com.squareup.picasso.Picasso;

import java.io.ByteArrayOutputStream;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class FirebaseUtils {


    public static void setUserPhoto(@NonNull final FirebaseUser user, @NonNull Uri photoUri,
                                          @NonNull Context context)
    {
        setUserPhoto(user, photoUri, context, null, null);
    }



    public static void setUserPhoto(@NonNull final FirebaseUser user, @NonNull Uri photoUri,
                                    @NonNull final Context context, @Nullable final ImageView img,
                                    @Nullable final ProgressBar progressBar)
    {
        if (progressBar != null)
            progressBar.setVisibility(View.VISIBLE);

        final StorageReference  profilePhotosRef = getUserPhotoStorageReference(user.getUid());
        UploadTask              photoUploadTask  = saveToFirebaseStorage(photoUri, profilePhotosRef,
                context);

        if (photoUploadTask == null)
            return;

        photoUploadTask.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot)
            {
                profilePhotosRef.getDownloadUrl().addOnSuccessListener(
                        new OnSuccessListener<Uri>() {
                            @Override
                            public void onSuccess(Uri uri)
                            {
                                if (img != null)
                                    Picasso.get().load(uri).into(img);

                                UserProfileChangeRequest profileChangeRequest =
                                        new UserProfileChangeRequest
                                        .Builder()
                                        .setPhotoUri(uri)
                                        .build();

                                user.updateProfile(profileChangeRequest).addOnFailureListener(
                                        new OnFailureListener() {
                                            @Override
                                            public void onFailure(@NonNull Exception e)
                                            {
                                                Toasts.showPhotoUploadError(context);
                                            }
                                        }
                                ).addOnCompleteListener(
                                        new OnCompleteListener<Void>() {
                                            @Override
                                            public void onComplete(@NonNull Task<Void> task)
                                            {
                                                if (progressBar != null)
                                                    progressBar.setVisibility(View.GONE);
                                            }
                                });
                            }
                        }
                ).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toasts.showPhotoUploadError(context);
                    }
                });
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e)
            {
                Toasts.showPhotoUploadError(context);
            }
        });
    }



    private static UploadTask saveToFirebaseStorage(Uri photoUri, StorageReference profilePhotosRef,
                                                    Context context)
    {
        Bitmap profileImage;
        try
        {
            profileImage = MediaStore.Images.Media.getBitmap(context.getContentResolver(),
                    photoUri);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            Toasts.showPhotoUploadError(context);
            return null;
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        profileImage.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        byte[] data = baos.toByteArray();

        return profilePhotosRef.putBytes(data);
    }



    public static StorageReference getUserPhotoStorageReference(String userId)
    {
        FirebaseStorage  storage     = FirebaseStorage.getInstance();
        StorageReference storageRef  = storage.getReference();

        return storageRef.child(StorageConst.PROFILE_PHOTOS_REF + "/" + userId);
    }
}
