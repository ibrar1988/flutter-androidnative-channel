package com.example.flutter_channel_app;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.Display;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.OnCanceledListener;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import io.flutter.embedding.android.FlutterActivity;
import io.flutter.embedding.engine.FlutterEngine;
import io.flutter.plugin.common.MethodChannel;

public class MainActivity extends FlutterActivity {
    private static final String CHANNEL = "com.example.flutter_channel_app/location_service";
    final String TAG = "MainActivity";
    final static int REQUEST_CODE_PERMISSIONS = 101;
    final static int REQUEST_CODE_LOCATION_SETTING = 199;
    private static final int REQUEST_CODE_ENABLE_GPS = 516;
    private Context mContext;
    //private String globalLocation = "Unknown Location";
    private StringBuilder globalLocation;

    LocationRequest mLocationRequest;
    LocationSettingsRequest.Builder mSettingsBuilder;
    LocationSettingsRequest mLocationSettingsRequest;
    SettingsClient mSettingsClient;
    Task<LocationSettingsResponse> mLocationSettingTaskResult;

    int count = 0;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.mContext = this;
        globalLocation = new StringBuilder("Unknown Location");
        Log.d(TAG, "onCreate Called");
    }

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter();
        filter.addAction(GPSService.ACTION_PROCESS_UPDATES);
        filter.addAction(Intent.ACTION_USER_PRESENT);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        registerReceiver(mBroadcastReceiver, filter);
        geocoder = new Geocoder(mContext.getApplicationContext());
        //geocoder = new Geocoder(mContext, Locale.getDefault());
        if(checkLocationPermission()){
            startServiceLocation();
        }
        Log.d(TAG, "onResume Called");
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(mBroadcastReceiver);
        Log.d(TAG, "onStop Called");
    }

    @Override
    public void configureFlutterEngine(@NonNull FlutterEngine flutterEngine) {
        super.configureFlutterEngine(flutterEngine);
        Log.d(TAG, "configureFlutterEngine Called");
        new MethodChannel(flutterEngine.getDartExecutor().getBinaryMessenger(), CHANNEL)
                .setMethodCallHandler(
                        (call, result) -> {
                            // Note: this method is invoked on the main thread.
                            switch (call.method) {
                                case "getLocationInfo":
                                    //int batteryLevel = getBatteryLevel();
                                    count++;
                                    result.success("Lat : 17.4677544, Long : 87.4567543 <--> Count : " + count);
                                    break;
                                case "countDownValues":
                                    count++;
                                    result.success("Count : " + count);
                                    break;
                                case "startLocation":
                                    requestGPSLoation();
                                    result.success("Location is started");
                                    break;
                                case "updateLocation":
                                    if (globalLocation == null || globalLocation.toString().equalsIgnoreCase("Unknown Location")) {
                                        result.error("UNAVAILABLE", "Location is not available.", null);
                                    } else {
                                        result.success(globalLocation.toString());
                                    }
                                    break;
                                default:
                                    //result.notImplemented();
                                    result.error("UNAVAILABLE", "Channel is not matched. Please pass correct channel id", null);
                                    break;
                            }
                        }
                );
    }

    private void requestGPSLoation() {
        mSettingsClient = LocationServices.getSettingsClient(this);
        mLocationRequest = LocationRequest.create().setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY).setInterval(5 * 1000).setFastestInterval(1000);
        mSettingsBuilder = new LocationSettingsRequest.Builder().addLocationRequest(mLocationRequest);
        mSettingsBuilder.setAlwaysShow(true);
        mLocationSettingsRequest = mSettingsBuilder.build();
        mLocationSettingTaskResult = mSettingsClient.checkLocationSettings(mLocationSettingsRequest);
        mLocationSettingTaskResult.addOnCompleteListener(new OnCompleteListener<LocationSettingsResponse>() {
            @Override
            public void onComplete(@NonNull Task<LocationSettingsResponse> task) {
                //Toast.makeText(getApplicationContext(), "onComplete called", Toast.LENGTH_LONG).show();
                /*try {
                    LocationSettingsResponse response = task.getResult(ApiException.class);
                } catch (ApiException ex) {
                    switch (ex.getStatusCode()) {
                        case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                            try {
                                ResolvableApiException resolvableApiException = (ResolvableApiException) ex;
                                resolvableApiException.startResolutionForResult(MainActivity.this, REQUEST_CODE_LOCATION_SETTING);
                            } catch (IntentSender.SendIntentException e) {

                            }
                            break;
                        case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:

                            break;
                    }
                }*/
            }
        }).addOnSuccessListener(new OnSuccessListener<LocationSettingsResponse>() {
            @Override
            public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                //Toast.makeText(getApplicationContext(), "onSuccess called", Toast.LENGTH_LONG).show();
                requestLocationPermission();
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                //Toast.makeText(getApplicationContext(), "onFailure called", Toast.LENGTH_LONG).show();
                int statusCode = ((ApiException) e).getStatusCode();
                switch (statusCode) {
                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                        //Toast.makeText(getApplicationContext(), "RESOLUTION_REQUIRED called", Toast.LENGTH_LONG).show();
                        try {
                            ResolvableApiException rae = (ResolvableApiException) e;
                            rae.startResolutionForResult(MainActivity.this, REQUEST_CODE_LOCATION_SETTING);
                        } catch (IntentSender.SendIntentException sie) {
                            Toast.makeText(getApplicationContext(), "SendIntentException called", Toast.LENGTH_LONG).show();
                            alertUserForGPSLocation();
                        }
                        break;
                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                        Toast.makeText(getApplicationContext(), "SETTINGS_CHANGE_UNAVAILABLE called", Toast.LENGTH_LONG).show();
                        alertUserForGPSLocation();
                }
            }
        }).addOnCanceledListener(new OnCanceledListener() {
            @Override
            public void onCanceled() {
                //Toast.makeText(getApplicationContext(), "onCanceled called", Toast.LENGTH_LONG).show();
            }
        });
    }

    private boolean checkLocationPermission(){
        boolean foreground_1 = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        boolean foreground_2 = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        return foreground_1 && foreground_2;
    }

    private void requestLocationPermission() {
        if (checkLocationPermission()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                boolean background = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED;
                if (!background) {
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION}, REQUEST_CODE_PERMISSIONS);
                }
            }
            startServiceLocation();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_BACKGROUND_LOCATION}, REQUEST_CODE_PERMISSIONS);
        }
    }

    private void openGpsEnableSetting() {
        Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        startActivityForResult(intent, REQUEST_CODE_ENABLE_GPS);
    }

    private void startServiceLocation() {
        Intent locationServiceIntent = new Intent(this, GPSService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ContextCompat.startForegroundService(mContext, locationServiceIntent);
        } else {
            startService(locationServiceIntent);
        }
    }

    private void alertUserForGPSLocation() {
        // Permission denied.
//        View view = getActivity().findViewById(android.R.id.content);
//        Snackbar.make(view,
//                "GPS is not enabled, but is needed for location access and core functionality",
//                Snackbar.LENGTH_INDEFINITE)
//                .setAction("Settings", new View.OnClickListener() {
//                    @Override
//                    public void onClick(View view) {
//                        // Build intent that displays the App settings screen.
//                        openGpsEnableSetting();
//                    }
//                }).show();
    }

    private AlertDialog mAlertDialog = null;

    private void alertForBackgroundLocationPermission() {
        if (mAlertDialog != null && mAlertDialog.isShowing()) {
            mAlertDialog.dismiss();
            mAlertDialog = null;
        }
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(MainActivity.this)
                .setTitle("Background Location Permission Denied")
                .setMessage("Please grant permission for background location read. Please select first option called" + " 'ALLOW ALL THE TIME' " + "from the permission dialog options")
                .setCancelable(false)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION}, REQUEST_CODE_PERMISSIONS);
                        }
                    }
                })
                .setNegativeButton("NO, THANKS", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
//                        final Snackbar mSnackbar = Snackbar.make(getWindow().getDecorView().getRootView(), "Background permission was denied, The app would not be able to read location in app background", Snackbar.LENGTH_INDEFINITE);
//                        mSnackbar.setAction("Dismiss", new View.OnClickListener() {
//                            @Override
//                            public void onClick(View v) {
//                                mSnackbar.dismiss();
//                            }
//                        }).show();
                    }
                });
        mAlertDialog = alertDialogBuilder.create();
        mAlertDialog.show();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_LOCATION_SETTING) {
            switch (resultCode) {
                case Activity.RESULT_OK:
                    //Success Perform Task Here
                    //Toast.makeText(getApplicationContext(), "GPS Location Enabled Now", Toast.LENGTH_LONG).show();
                    requestLocationPermission();
                    break;
                case Activity.RESULT_CANCELED:
                    Log.e("GPS", "User denied to access location");
                    //openGpsEnableSetting();
                    alertUserForGPSLocation();
                    break;
            }
        } else if (requestCode == REQUEST_CODE_ENABLE_GPS) {
            LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            boolean isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
            if (!isGpsEnabled) {
                //openGpsEnableSetting();
                alertUserForGPSLocation();
            } else {
                //Toast.makeText(getApplicationContext(), "GPS Location Enabled Already", Toast.LENGTH_LONG).show();
                requestLocationPermission();
                //navigateToUser();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            boolean foreground_fine = false, foreground_coarse = false, background = false;
            for (int i = 0; i < permissions.length; i++) {
                if (permissions[i].equalsIgnoreCase(Manifest.permission.ACCESS_FINE_LOCATION)) {
                    //foreground permission allowed
                    if (grantResults[i] >= 0) {
                        foreground_fine = true;
                        //Toast.makeText(getApplicationContext(), "Foreground fine location permission allowed", Toast.LENGTH_SHORT).show();
                        continue;
                    } else {
                        Toast.makeText(getApplicationContext(), "Fine location Permission denied", Toast.LENGTH_SHORT).show();
                        break;
                    }
                }

                if (permissions[i].equalsIgnoreCase(Manifest.permission.ACCESS_COARSE_LOCATION)) {
                    //foreground permission allowed
                    if (grantResults[i] >= 0) {
                        foreground_coarse = true;
                        //Toast.makeText(getApplicationContext(), "Foreground coarse location permission allowed", Toast.LENGTH_SHORT).show();
                        continue;
                    } else {
                        Toast.makeText(getApplicationContext(), "Coarse location Permission denied", Toast.LENGTH_SHORT).show();
                        break;
                    }
                }

                if (permissions[i].equalsIgnoreCase(Manifest.permission.ACCESS_BACKGROUND_LOCATION)) {
                    if (grantResults[i] >= 0) {
                        foreground_fine = true;
                        foreground_coarse = true;
                        background = true;
                        //Toast.makeText(getApplicationContext(), "Background location permission allowed", Toast.LENGTH_SHORT).show();
                    } else {
                        // TODO : Show alert (YES/NO button) to user for background location read.
                        Toast.makeText(getApplicationContext(), "Background location permission denied", Toast.LENGTH_SHORT).show();
                        alertForBackgroundLocationPermission();
                    }
                }
            }

            if (foreground_fine && foreground_coarse) {
                /*if (background) {
                    handleLocationUpdates();
                } else {
                    handleForegroundLocationUpdates();
                }*/
                startServiceLocation();
            }
        }
    }

    private void handleLocationUpdates() {
        //foreground and background
        Toast.makeText(getApplicationContext(), "Start Foreground and Background Location Updates", Toast.LENGTH_SHORT).show();
    }

    private void handleForegroundLocationUpdates() {
        //handleForeground Location Updates
        Toast.makeText(getApplicationContext(), "Start foreground location updates", Toast.LENGTH_SHORT).show();
    }

    static String addressFragments = "";
    static List<Address> addresses = null;
    Geocoder geocoder;

    private String getAddress(Location location, Context context) {
        // Address found using the Geocoder.
        addresses = null;
        Address address = null;
        addressFragments = "";
        try {
            addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
            address = addresses.get(0);
        } catch (IOException ioException) {
            Log.e(TAG, "error", ioException);
        } catch (IllegalArgumentException illegalArgumentException) {
            Log.e(TAG, "Latitude = " + location.getLatitude() +
                    ", Longitude = " + location.getLongitude(), illegalArgumentException);
        } catch (Exception ex) {
            Log.e(TAG, "Exception Handle", ex);
        }

        if (addresses == null || addresses.size() == 0) {
            Log.e(TAG, "ERORR");
            addressFragments = "NO ADDRESS FOUND";
        } else {
            for (int i = 0; i <= address.getMaxAddressLineIndex(); i++) {
                addressFragments = addressFragments + String.valueOf(address.getAddressLine(i));
            }
        }
        return addressFragments;
    }

    public void updateTextField(LocationResult locationResult) {
        if (locationResult != null) {
            Date today = Calendar.getInstance().getTime();
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH.mm.ss", Locale.getDefault());
            String nowDate = formatter.format(today);
            List<Location> locations = locationResult.getLocations();
            Location firstLocation = locations.get(0);
            //getAddress(firstLocation, mContext);
            try{
                //viewLocationUpdate.setText("You are at " + getAddress(firstLocation, mContext) + "(" + nowDate + ") with accuracy " + firstLocation.getAccuracy() + " Latitude:" + firstLocation.getLatitude() + " Longitude:" + firstLocation.getLongitude() + " Speed:" + firstLocation.getSpeed() + " Bearing:" + firstLocation.getBearing());
                globalLocation.setLength(0);
                globalLocation.append("You are at ").append(getAddress(firstLocation, mContext)).append("(").append(nowDate).append(") with accuracy ").append(firstLocation.getAccuracy()).append(" Latitude:").append(firstLocation.getLatitude()).append(" Longitude:").append(firstLocation.getLongitude()).append(" Speed:").append(firstLocation.getSpeed()).append(" Bearing:").append(firstLocation.getBearing());
            } catch (Exception ex){
                Log.e(TAG, "java.io.IOException: grpc failed", ex);
            }
        }
    }

    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            //Toast.makeText(getApplicationContext(),"Broadcast receive called",Toast.LENGTH_SHORT).show();
            try {
                if (intent != null) {
                    if (intent.getAction().equalsIgnoreCase(GPSService.ACTION_PROCESS_UPDATES)) {
                        LocationResult locationResult = (LocationResult) intent.getParcelableExtra("location");
                        //Log.d(TAG, "OnReceive : " + locationResult);
                        updateTextField(locationResult);
                    } else if (intent.getAction().equals(Intent.ACTION_USER_PRESENT)) {
                        Log.d(TAG, "Phone unlocked");
                        requestGPSLoation();
                    } else if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
                        Log.d(TAG, "ACTION_SCREEN_OFF");
                    } else if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
                        Log.d(TAG, "ACTION_SCREEN_ON");
                    }
                }
            } catch (Exception ex){
                Log.e(TAG, "java.io.IOException: grpc failed", ex);
            }
        }
    };

    private int getBatteryLevel() {
        int batteryLevel = -1;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            BatteryManager batteryManager = (BatteryManager) getSystemService(BATTERY_SERVICE);
            batteryLevel = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
        } else {
            Intent intent = new ContextWrapper(getApplicationContext()).
                    registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            batteryLevel = (intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) * 100) /
                    intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        }

        return batteryLevel;
    }
}
