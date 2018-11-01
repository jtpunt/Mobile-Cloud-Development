package com.example.jonathan.week6;

import android.Manifest;
import com.example.jonathan.week6.database.LocationContract;
import com.example.jonathan.week6.database.LocationDBHelper;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.app.Dialog;
import android.content.pm.PackageManager;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;


public class display_geolocation extends AppCompatActivity implements
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
    private static final String TAG = "Location-SQLActivity";
    private static final int LOCATION_PERMISSON_RESULT = 1;
    private GoogleApiClient googleApiClient;
    private LocationRequest locationRequest;
    private LocationListener locationListener;
    private Location lastLocation;
    SimpleCursorAdapter sqlCursorAdapter;
    LocationDBHelper locationDbHelper; // Subclass of SQLiteOpenHelper to access the database
    SQLiteDatabase locationDB;
    String latText;
    String lonText;
    Button sqlBtn;
    String[] PERMISSIONS = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_week6);
        setupSQLite();
        setupApiClient();
    }

    @Override
    protected void onStart() {
        googleApiClient.connect();
        super.onStart();
    }

    @Override
    protected void onStop() {
        googleApiClient.disconnect();
        super.onStop();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {

    }
    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Dialog errDialog = GoogleApiAvailability.getInstance().getErrorDialog(this, connectionResult.getErrorCode(), 0);
        errDialog.show();
        return;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults){
        if(requestCode == LOCATION_PERMISSON_RESULT){
            if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){ // user has granted permissions
                updateLocation();
            }
            else{ // user has not granted permissions
                updateLocation();
            }
            return;
        }
        else{
            updateLocation();
        }
    }
    private void setupApiClient(){
        if (googleApiClient == null) {
            googleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }
        // Create the Location request to start receiving updates
        locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(5000);
        locationRequest.setFastestInterval(5000);
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                if (location != null) {
                    latText = "Latitude: " + String.valueOf(location.getLatitude());
                    lonText = "Longitude: " + String.valueOf(location.getLongitude());
                } else {
                    latText = "Latitude: 44.5";
                    lonText = "Longitude: -123.2 ";
                }
            }
        };
    }
    public void getLocation(){
        if(hasPermissions()){ // did the user already grant all the required permissions?
            updateLocation();
        }else{  // the user did not grant all required permissions
            ActivityCompat.requestPermissions(this, PERMISSIONS, LOCATION_PERMISSON_RESULT);
        }
    }
    public boolean hasPermissions(){
        for(String permission: PERMISSIONS){
            if(ActivityCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED){
                return false;
            }
        }
        return true;
    }
    private void setupSQLite(){
        locationDbHelper = new LocationDBHelper(this); // instantiate the subclass of SQLiteOpenHelper to get access to the db
        locationDB = locationDbHelper.getWritableDatabase(); // gets the data repository in write mode

        sqlBtn = (Button) findViewById(R.id.add_entry_button);

        sqlBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(locationDB  != null){
                    getLocation();
                } else {
                    Log.d(TAG, "Unable to access database for writing.");
                }
            }
        });
        showData();
    }
    private void updateLocation() {
        try {
            lastLocation = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);

            if (lastLocation != null) {
                latText = "Latitude: " + String.valueOf(lastLocation.getLatitude());
                lonText = "Longitude: " + String.valueOf(lastLocation.getLongitude());
            } else {
                latText = "Latitude: 44.5";
                lonText = "Longitude: -123.2 ";
            }
            updateDatabase();
        }catch(SecurityException e){
            e.printStackTrace();
        }
    }
    private void updateDatabase(){
        ContentValues values = new ContentValues();
        values.put(LocationContract.LocationEntry.COLUMN_USER_INPUT, ((EditText)findViewById(R.id.sql_text_input)).getText().toString());
        values.put(LocationContract.LocationEntry.COLUMN_LATITUDE, latText);
        values.put(LocationContract.LocationEntry.COLUMN_LONGITUDE, lonText);
        locationDB.insert(LocationContract.LocationEntry.TABLE_NAME,null,values);
        showData();
    }

    private void showData(){
        if(locationDB != null) {
            try {
                if(sqlCursorAdapter != null && sqlCursorAdapter.getCursor() != null){
                    if(!sqlCursorAdapter.getCursor().isClosed()){
                        sqlCursorAdapter.getCursor().close();
                    }
                }
                ListView SQLListView = (ListView) findViewById(R.id.sql_list_view);
                sqlCursorAdapter = getCursorAdapter();
                SQLListView.setAdapter(sqlCursorAdapter); // attaches the adapter to our ListView
            } catch (Exception e) {
                Log.d(TAG, "Error loading data from database");
            }
        }
    }
    private Cursor getDatabaseData(){
        // Specify the array of columns to return
        String[] projection = {
                LocationContract.LocationEntry._ID,
                LocationContract.LocationEntry.COLUMN_USER_INPUT,
                LocationContract.LocationEntry.COLUMN_LATITUDE,
                LocationContract.LocationEntry.COLUMN_LONGITUDE
        };
        // Read from the database and return the Cursor object
        return locationDB.query(
                LocationContract.LocationEntry.TABLE_NAME,
                projection,
                null,
                null,
                null,
                null,
                null
        );
    }
    private SimpleCursorAdapter getCursorAdapter(){
        return new SimpleCursorAdapter(this,
                R.layout.sql_item,
                getDatabaseData(), // Read from the database
                new String[]{LocationContract.LocationEntry.COLUMN_USER_INPUT, LocationContract.LocationEntry.COLUMN_LATITUDE, LocationContract.LocationEntry.COLUMN_LONGITUDE},
                new int[]{R.id.sql_listview_string, R.id.sql_listview_lat, R.id.sql_listview_long},
                0);
    }
}
