package com.example.jonathan.googledrive;

import android.app.Activity;
import android.app.Application;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.drive.CreateFileActivityOptions;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveClient;
import com.google.android.gms.drive.DriveContents;
import com.google.android.gms.drive.DriveResourceClient;
import com.google.android.gms.drive.MetadataChangeSet;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Random;

import com.example.jonathan.googledrive.database.CameraDBUtils;

/** Source: https://github.com/googlearchive/drive-android-quickstart/blob/master/app/src/main/java/com/google/android/gms/drive/sample/quickstart/MainActivity.java
 * Android Drive Quickstart activity. This activity takes a photo and saves it in Google Drive. The
 * user is prompted with a pre-made dialog which allows them to choose the file location.
 */

public class CameraActivity extends Activity {

    private static final String TAG = "GoogleDrive";
    private static final int REQUEST_CODE_SIGN_IN = 0;
    private static final int REQUEST_CODE_CAPTURE_IMAGE = 1;
    private static final int REQUEST_CODE_CREATOR = 2;
    private static final int SDCARD_PERMISSION_RESULT = 1;
    private int counter = 0;
//    private GoogleSignInClient mGoogleSignInClient;
    private DriveClient mDriveClient;
    private DriveResourceClient mDriveResourceClient;
    private Bitmap mBitmapToSave;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(!hasPermissions()){
            ActivityCompat.requestPermissions(this, Global.PERMISSIONS, SDCARD_PERMISSION_RESULT);
        }else{
            signIn();
        }
    }

    /** Start the sign in activity with Google. */
    private void signIn() {
        Log.i(TAG, "Start sign in");
        CameraDBUtils.setupSQLite(this);
        Global.mGoogleSignInClient = buildGoogleSignInClient();
        startActivityForResult(Global.mGoogleSignInClient.getSignInIntent(), REQUEST_CODE_SIGN_IN);
    }
    // This function is public so it can be accessed from the MainActivity, allowing the user to switch between gmail accounts by signing out
    public static void signOut() {
        if(Global.mGoogleSignInClient != null) {
            Global.mGoogleSignInClient.signOut();
        }
    }

    /** Build a Google SignIn client. */
    private GoogleSignInClient buildGoogleSignInClient() {
        GoogleSignInOptions signInOptions =
                new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestScopes(Drive.SCOPE_FILE)
                        .build();
        return GoogleSignIn.getClient(this, signInOptions);
    }

    /** Create a new file and save it to Drive. */
    private void saveFileToDrive() {
        // Start by creating a new contents, and setting a callback.
        Log.i(TAG, "Creating new contents.");
        final Bitmap image = mBitmapToSave;

        mDriveResourceClient
                .createContents()
                .continueWithTask(
                        new Continuation<DriveContents, Task<Void>>() {
                            @Override
                            public Task<Void> then(@NonNull Task<DriveContents> task) throws Exception {
                                return createFileIntentSender(task.getResult(), image);
                            }
                        })
                .addOnFailureListener(
                        new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.w(TAG, "Failed to create new contents.", e);
                            }
                        });
    }

    /**
     * Creates an {@link IntentSender} to start a dialog activity with configured {@link
     * CreateFileActivityOptions} for user to create a new photo in Drive.
     */
    private Task<Void> createFileIntentSender(DriveContents driveContents, Bitmap image) {
        Log.i(TAG, "New contents created.");
        // Get an output stream for the contents.
        OutputStream outputStream = driveContents.getOutputStream();
        // Write the bitmap data from it.
        ByteArrayOutputStream bitmapStream = new ByteArrayOutputStream();
        image.compress(Bitmap.CompressFormat.PNG, 100, bitmapStream);
        try {
            outputStream.write(bitmapStream.toByteArray());
        } catch (IOException e) {
            Log.w(TAG, "Unable to write file contents.", e);
        }

        // Create the initial metadata - MIME type and title.
        // Note that the user will be able to change the title later.
        MetadataChangeSet metadataChangeSet =
                new MetadataChangeSet.Builder()
                        .setMimeType("image/jpeg")
                        .setTitle("Android Photo.png")
                        .build();
        // Set up options to configure and display the create file activity.
        CreateFileActivityOptions createFileActivityOptions =
                new CreateFileActivityOptions.Builder()
                        .setInitialMetadata(metadataChangeSet)
                        .setInitialDriveContents(driveContents)
                        .build();

        return mDriveClient
                .newCreateFileActivityIntentSender(createFileActivityOptions)
                .continueWith(
                        new Continuation<IntentSender, Void>() {
                            @Override
                            public Void then(@NonNull Task<IntentSender> task) throws Exception {
                                startIntentSenderForResult(task.getResult(), REQUEST_CODE_CREATOR, null, 0, 0, 0);
                                return null;
                            }
                        });
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults){
        if(requestCode == SDCARD_PERMISSION_RESULT){
            if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){ // user has granted permissions
                signIn();
            }
            else{ // user has not granted permissions
                ActivityCompat.requestPermissions(this, Global.PERMISSIONS, SDCARD_PERMISSION_RESULT);
            }
//            return;
        }
        else{
            ActivityCompat.requestPermissions(this, Global.PERMISSIONS, SDCARD_PERMISSION_RESULT);
        }
    }
    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_CODE_SIGN_IN:
                Log.i(TAG, "Sign in request code");
                // Called after user is signed in.
                if (resultCode == RESULT_OK) {
                    Log.i(TAG, "Signed in successfully.");
                    // Use the last signed in account here since it already have a Drive scope.
                    mDriveClient = Drive.getDriveClient(this, GoogleSignIn.getLastSignedInAccount(this));
                    // Build a drive resource client.
                    mDriveResourceClient =
                            Drive.getDriveResourceClient(this, GoogleSignIn.getLastSignedInAccount(this));
                    // Start camera.
                    startActivityForResult(
                            new Intent(MediaStore.ACTION_IMAGE_CAPTURE), REQUEST_CODE_CAPTURE_IMAGE);
                    Intent intent = new Intent();
                    setResult(RESULT_OK, intent); // send this back to the MainActivity, so that the google sign-in button will appear
                }
                break;
            case REQUEST_CODE_CAPTURE_IMAGE:
                Log.i(TAG, "capture image request code");
                // Called after a photo has been taken.
                if (resultCode == Activity.RESULT_OK) {
                    Log.i(TAG, "Image captured successfully.");
                    // Store the image data as a bitmap for writing later.
                    mBitmapToSave = (Bitmap) data.getExtras().get("data");
                    saveFileToDrive();
                    SaveImage(mBitmapToSave); // write the image to a file
                    Intent intent = new Intent();
                    setResult(RESULT_OK, intent);
                    finish(); // go back to MainActivity
                }
                break;
            case REQUEST_CODE_CREATOR:
                Log.i(TAG, "creator request code");
                // Called after a file is saved to Drive.
                if (resultCode == RESULT_OK) {
                    Log.i(TAG, "Image successfully saved.");
                    mBitmapToSave = null;
                    // Just start the camera again for another photo.
                    startActivityForResult(
                            new Intent(MediaStore.ACTION_IMAGE_CAPTURE), REQUEST_CODE_CAPTURE_IMAGE);
                    SaveImage(mBitmapToSave); // write the image to a file
                    Intent intent = new Intent();
                    setResult(RESULT_OK, intent);
                    finish(); // go back to MainActivity
                }
                break;
        }
    }
    // This function writes the bitmap data to a file
    private void SaveImage(Bitmap mBitmapToSave) {
        if(!isExternalStorageWritable()){
            Log.d(TAG, "Error: SD card is not readable!");
        }
        else {

            File myDir = new File("/storage/emulated/0/DCIM/Camera/");
            if (!myDir.exists()) {
                myDir.mkdirs();
            }
            Random generator = new Random();
            int n = 10000;
            n = generator.nextInt(n);
            String fname = "Image-" + n + ".jpg";
            File file = new File(myDir, fname);
            if (file.exists())
                Log.d(TAG, "Error, file already exists!");
            file.delete();
            try {
                FileOutputStream out = new FileOutputStream(file);
                mBitmapToSave.compress(Bitmap.CompressFormat.JPEG, 90, out);
                out.flush();
                out.close();
                CameraDBUtils.insertData(file.getAbsolutePath());

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    /* Checks if external storage is available for read and write */
    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }
    public boolean hasPermissions(){
        for(String permission: Global.PERMISSIONS){
            if(ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED){
                return false; // permission has not been granted
            }
        }
        return true; // all permissions have been granted
    }
}
