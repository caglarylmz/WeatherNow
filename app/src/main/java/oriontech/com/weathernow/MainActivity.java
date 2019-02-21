package oriontech.com.weathernow;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Looper;
import android.os.PersistableBundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;

import java.text.DateFormat;
import java.util.Date;
import java.util.List;

import oriontech.com.weathernow.utils.Common;
import oriontech.com.weathernow.views.stars.AnimatedStarView;
import oriontech.com.weathernow.views.weatherview.BaseDrawer;
import oriontech.com.weathernow.views.weatherview.DynamicWeatherView;


public class MainActivity extends AppCompatActivity{
    private static final String TAG = MainActivity.class.getSimpleName();
    //for app context
    private static Context appContext;
    public static Context getAppContext(){
        return appContext;
    }
    //need some data
    private SharedPreferences sharedPreferences;
    //for current location
    private FusedLocationProviderClient fusedLocationProviderClient;
    private LocationRequest locationRequest;
    private boolean requestingLocationUpdates;
    private SettingsClient settingsClient;
    private LocationSettingsRequest locationSettingsRequest;
    private LocationCallback locationCallback;
    private Location currentLocation;

    private ToggleButton tbLocationUpdates;
    private TextView tvLat, tvLon;

    AnimatedStarView  starsWhite;
    DynamicWeatherView dynamicWeatherView;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        appContext=this;
        sharedPreferences = this.getPreferences(Context.MODE_PRIVATE);
        setContentView(R.layout.activity_main);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN|View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        reqPermission();
        initLocation();
        init();

        // restore the values from saved instance state
        restoreValuesFromBundle(savedInstanceState);

    }

    /**
     * Request Multiple Permission for Location.
     * */
    private void reqPermission() {
        Dexter.withActivity(this)
                .withPermissions(Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION)
                .withListener(new MultiplePermissionsListener() {
                    @SuppressLint("MissingPermission")  //
                    @Override
                    public void onPermissionsChecked(MultiplePermissionsReport report) {
                        if (report.areAllPermissionsGranted()) {

                        }
                        
                        if (report.isAnyPermissionPermanentlyDenied()){
                            /**
                             * Showing Alert Dialog with Settings option
                             * Navigates user to app settings
                             */
                            AlertDialog.Builder builder = new AlertDialog.Builder(getAppContext());
                            builder.setTitle("Need Permissions")
                                    .setMessage("This app needs permission to use this feature. You can grant them in app settings.")
                                    .setPositiveButton("GO TO SETTINGS", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                            Uri uri = Uri.fromParts("package", getPackageName(), null);
                                            intent.setData(uri);
                                            startActivityForResult(intent, 101);

                                            dialog.cancel();
                                        }
                                    })
                                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            dialog.cancel();
                                        }
                                    })
                                    .show();
                        }

                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(List<PermissionRequest> permissions, PermissionToken token) {
                        token.continuePermissionRequest();
                    }
                })
                .onSameThread()
                .check();
    }
    /**
     * Inıtialize Location updates
     * */
    private void initLocation(){
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        settingsClient = LocationServices.getSettingsClient(this);
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
                // location is received
                currentLocation = locationResult.getLastLocation();
                Common.locationLastUpdateTime = DateFormat.getTimeInstance().format(new Date());

                /**
                 * This area update Location UI
                 * */
                updateLocationUI();

            }
        };
        requestingLocationUpdates=false;
        locationRequest= new LocationRequest();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(10000);
        locationRequest.setFastestInterval(5000);
        locationRequest.setSmallestDisplacement(10.0f);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        builder.addLocationRequest(locationRequest);
        locationSettingsRequest = builder.build();

    }
    /**
     * If need currentLocation Starting location updates.
     * Check whether location settings are satisfied and then
     * location updates will be requested
     */
    private void init(){
       /* tbLocationUpdates = findViewById(R.id.tb_switchLocationUpdates);
        tvLat = findViewById(R.id.textLatitude);
        tvLon = findViewById(R.id.textLongitude);
        tbLocationUpdates.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked)
                    startLocationUpdates();
                else
                    stopLocationUpdates();
            }
        });*/

        starsWhite=findViewById(R.id.stars_white);
        dynamicWeatherView=findViewById(R.id.main_dynamicweatherview);
        dynamicWeatherView.setDrawerType(BaseDrawer.Type.SNOW_N);




    }


    private void startLocationUpdates() {
        if (!checkPermissions()) {
            reqPermission();
        }else {
            Toast.makeText(getApplicationContext(), "Location updates started!", Toast.LENGTH_SHORT).show();
            settingsClient.checkLocationSettings(locationSettingsRequest)
                    .addOnSuccessListener(this, new OnSuccessListener<LocationSettingsResponse>() {
                        @SuppressLint("MissingPermission")
                        @Override
                        public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                            Log.i(TAG, "All location settings is ok");
                            fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());
                            updateLocationUI();
                        }
                    })
                    .addOnFailureListener(this, new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            int statusCode = ((ApiException) e).getStatusCode();
                            switch (statusCode) {
                                case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                                    Log.i(TAG, "Location settings are not satisfied. Attempting to upgrade " +
                                            "location settings ");
                                    try {
                                        // Show the dialog by calling startResolutionForResult(), and check the
                                        // result in onActivityResult().
                                        ResolvableApiException rae = (ResolvableApiException) e;
                                        rae.startResolutionForResult(MainActivity.this, 100);
                                    } catch (IntentSender.SendIntentException sie) {
                                        Log.i(TAG, "PendingIntent unable to execute request.");
                                    }
                                    break;
                                case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                                    String errorMessage = "Location settings are inadequate, and cannot be " +
                                            "fixed here. Fix in Settings.";
                                    Log.e(TAG, errorMessage);

                                    Toast.makeText(MainActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                            }

                            updateLocationUI();
                        }
                    });
        }
    }
    /**
     * stop location updates
     * */
    public void stopLocationUpdates() {
        // Removing location updates
        fusedLocationProviderClient
                .removeLocationUpdates(locationCallback)
                .addOnCompleteListener(this, new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        Toast.makeText(getApplicationContext(), "Location updates stopped!", Toast.LENGTH_SHORT).show();
                    }
                });
    }
    /**
     * Restoring values from saved instance state
     */
    private void restoreValuesFromBundle(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey("is_requesting_updates")) {
                requestingLocationUpdates = savedInstanceState.getBoolean("is_requesting_updates");
            }

            if (savedInstanceState.containsKey("last_known_location")) {
                currentLocation = savedInstanceState.getParcelable("last_known_location");
            }

            if (savedInstanceState.containsKey("last_updated_on")) {
                Common.locationLastUpdateTime = savedInstanceState.getString("last_updated_on");
            }
        }
        /**
         * This area update Location UI
         * */
        updateLocationUI();
    }
    /**
     * Update the UI displaying the location data
     * and toggling the buttons
     */
    private void updateLocationUI() {
        if (currentLocation != null) {
            Common.locationLat=currentLocation.getLatitude();
            Common.locationLon=currentLocation.getLongitude();
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putFloat("latitude", (float) currentLocation.getLatitude());
            editor.putFloat("longitude", (float) currentLocation.getLongitude());
           // tvLat.setText("Lat: " + currentLocation.getLatitude());
           // tvLon.setText("Lon: " + currentLocation.getLongitude());
        }
       // tvLat.setAlpha(0);
       // tvLat.animate().alpha(1).setDuration(300);
       // tvLon.setAlpha(0);
       // tvLon.animate().alpha(1).setDuration(300);
    }

    @Override
    public void onSaveInstanceState(Bundle outState, PersistableBundle outPersistentState) {
        super.onSaveInstanceState(outState, outPersistentState);
        outState.putBoolean("is_requesting_updates",requestingLocationUpdates);
        outState.putParcelable("last_known_location",currentLocation);
        outState.putString("last_update_on",Common.locationLastUpdateTime);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode){
            // Check for the integer request code originally supplied to startResolutionForResult().
            case 100:
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        Log.e(TAG, "User agreed to make required location settings changes.");
                        // Nothing to do. startLocationupdates() gets called in onResume again.
                        break;
                    case Activity.RESULT_CANCELED:
                        Log.e(TAG, "User chose not to make required location settings changes.");
                        requestingLocationUpdates = false;
                        break;
                }
                break;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        dynamicWeatherView.onResume();
        // Resuming location updates depending on button state and
        // allowed permissions
        if (requestingLocationUpdates && checkPermissions()) {
            startLocationUpdates();
        }

        updateLocationUI();
    }

    //for onResume check Permission
    private boolean checkPermissions() {
        int permissionState = ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION);
        return permissionState == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    protected void onPause() {
        super.onPause();
        dynamicWeatherView.onPause();

        if (requestingLocationUpdates) {
            // pausing location updates
            stopLocationUpdates();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        starsWhite.onStart();


    }

    @Override
    protected void onStop() {
        super.onStop();
        starsWhite.onStop();
    }

}
