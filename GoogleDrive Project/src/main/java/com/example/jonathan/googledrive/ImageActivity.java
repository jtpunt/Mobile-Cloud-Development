package com.example.jonathan.googledrive;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import com.example.jonathan.googledrive.database.CameraContract;
import com.example.jonathan.googledrive.database.CameraDBUtils;
import com.example.jonathan.googledrive.CameraCursorAdapter;
import java.util.ArrayList;
import java.util.List;

public class ImageActivity extends Activity {
//    SimpleCursorAdapter sqlCursorAdapter;

    private static final String TAG = "GoogleDrive";
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_images);
        if(!hasPermissions()){ // do we not have all the required permissions?
            ActivityCompat.requestPermissions(this, Global.PERMISSIONS, Global.SDCARD_PERMISSION_RESULT);
        }else{ // we have all required permissions, query the database, and then show the data
            showData();
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults){
        if(requestCode == Global.SDCARD_PERMISSION_RESULT){
            if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){ // user has granted permissions
                showData();
            }
            else{ // user has not granted permissions
                ActivityCompat.requestPermissions(this, Global.PERMISSIONS, Global.SDCARD_PERMISSION_RESULT);
            }
        }
        else{
            ActivityCompat.requestPermissions(this, Global.PERMISSIONS, Global.SDCARD_PERMISSION_RESULT);
        }
    }
    // Returns true if the user has granted all required permissions, false otherwise
    public boolean hasPermissions(){
        for(String permission: Global.PERMISSIONS){
            if(ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED){
                return false; // permission has not been granted
            }
        }
        return true; // all permissions have been granted
    }
    private void showData(){
        if(CameraDBUtils.validDB()) { // do we have a valid database?
            CameraCursorAdapter cursorAdapter = new CameraCursorAdapter(this,  CameraDBUtils.getDatabaseData());
            ListView SQLListView = (ListView) findViewById(R.id.google_list_view);
            SQLListView.setAdapter(cursorAdapter);

        }else{ // set up the database and call this function again
            CameraDBUtils.setupSQLite(this);
            showData();
        }
    }
}
