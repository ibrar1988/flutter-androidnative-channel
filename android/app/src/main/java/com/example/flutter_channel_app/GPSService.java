package com.example.flutter_channel_app;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.KeyguardManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

public class GPSService extends Service implements SensorEventListener {
    private final String TAG = "GPSService";
    private final String NOTIFICATION_CHANNEL_ID = "com.techv";
    private final String CHANNEL_ONE_NAME = "com.techv.LocationService2";
    private final int IMPORTANCE_HIGH = NotificationManager.IMPORTANCE_HIGH;

    public static final String ACTION_PROCESS_UPDATES = "com.techv.LocationService2.action" + ".PROCESS_UPDATES";

    private FusedLocationProviderClient mFusedLocationClient;
    private LocationRequest mLocationRequest;
    private LocationCallback mLocationCallback;
    Intent intent;

    PowerManager powerManager;
    KeyguardManager keyguardManager;
    KeyguardManager.KeyguardLock lock;
    boolean isScreenOn;

    // Sensor variables
    private SensorManager mSensorManager;
    private Sensor accelerometer;

    private float[] mGravity;
    private float mAccel;
    private float mAccelCurrent;
    private float mAccelLast;
    static int count = 0;

    private static final float ERROR = (float) 7.0;
    private static final float SHAKE_THRESHOLD = 15.00f; // m/S**2
    private static final int MIN_TIME_BETWEEN_SHAKES_MILLISECS = 1000;
    private long mLastShakeTime;
    private long mServiceRunningCheckTime;


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate");
        intent = new Intent(ACTION_PROCESS_UPDATES);
        powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        //lock = ((KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE)).newKeyguardLock(KEYGUARD_SERVICE);
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mAccel = 0.00f;
        mAccelCurrent = SensorManager.GRAVITY_EARTH;
        mAccelLast = SensorManager.GRAVITY_EARTH;
        mSensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
    }

    @SuppressLint("ResourceType")
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand");
        init();
        NotificationChannel notificationChannel;
        Notification.Builder notificationBuilder;
        Notification notificationObject;
        NotificationManager notificationManager;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            int priority = NotificationManager.IMPORTANCE_DEFAULT;
            notificationChannel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, CHANNEL_ONE_NAME, priority);
            notificationChannel.setDescription("Totom GPS location service is running in background");
            //notificationChannel.enableLights(true);
            //notificationChannel.setLightColor(Color.RED);
            //notificationChannel.setVibrationPattern(new long[]{0, 1000, 500, 1000});
            //notificationChannel.enableVibration(true);
            //notificationChannel.setShowBadge(true);
            notificationManager = (NotificationManager)this.getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.createNotificationChannel(notificationChannel);
            notificationBuilder = new Notification.Builder(this, NOTIFICATION_CHANNEL_ID);
            notificationBuilder.setBadgeIconType(R.mipmap.ic_launcher);
            notificationBuilder.setContentTitle(getString(R.string.app_name));

            notificationBuilder.setSmallIcon(R.mipmap.ic_launcher);
            Bitmap icon = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher);
            notificationBuilder.setLargeIcon(icon);
        } else {
            notificationBuilder = new Notification.Builder(this);
        }

        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.LOLLIPOP) {
            notificationBuilder.setSmallIcon(R.mipmap.ic_launcher);
            notificationBuilder.setColor(ContextCompat.getColor(getApplicationContext(), Color.RED));
        } else {
            notificationBuilder.setSmallIcon(R.mipmap.ic_launcher);
        }

        notificationBuilder.setContentTitle("Totom Location Service");
        notificationBuilder.setContentText("App is running in background");
        notificationBuilder.setAutoCancel(true);
        notificationBuilder.setDefaults(Notification.DEFAULT_SOUND);
        notificationBuilder.setPriority(Notification.PRIORITY_HIGH);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), PendingIntent.FLAG_UPDATE_CURRENT);
        notificationBuilder.setContentIntent(pendingIntent);
        notificationObject = notificationBuilder.build();
        startForeground(1, notificationObject);
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
        stopLocationUpdates();
        if(mSensorManager!=null) {
            mSensorManager.unregisterListener(this);
        }
    }

    private void createLocationRequest() {
        Log.d(TAG, "createLocationRequest");
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(Utils.UPDATE_INTERVAL);
        mLocationRequest.setFastestInterval(Utils.FASTEST_UPDATE_INTERVAL);

        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        //mLocationRequest.setSmallestDisplacement(Utils.SMALLEST_DISPLACEMENT);
        //mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        //mLocationRequest.setPriority(LocationRequest.PRIORITY_LOW_POWER);
        mLocationRequest.setMaxWaitTime(Utils.MAX_WAIT_TIME);
    }
    StringBuilder location = new StringBuilder();
    private void init() {
        createLocationRequest();
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    Log.d(TAG, "onLocationResult--> ::: NULL");
                    return;
                }
                location.setLength(0);
                broadcastLocation(locationResult);
                //super.onLocationResult(locationResult);
                location.append("Latitude : ").append(locationResult.getLastLocation().getLatitude()).append("<--> Longitude : ").append(locationResult.getLastLocation().getLongitude());
                Log.d(TAG, "onLocationResult--> ::: " + location);
                //callAPI("location");
                Toast.makeText(getApplicationContext(), location, Toast.LENGTH_LONG).show();
            }
        };

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
    }

    private void broadcastLocation(LocationResult locationResult){
        intent.putExtra("location", locationResult);
        sendBroadcast(intent);
    }

    /**
     * Remove Location Update
     */
    public void stopLocationUpdates() {
        if(mFusedLocationClient!=null)
            mFusedLocationClient.removeLocationUpdates(mLocationCallback).addOnCompleteListener(task -> Log.d(TAG,"Removed location update"));
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            long curTime = System.currentTimeMillis();
            if ((curTime - mLastShakeTime) > MIN_TIME_BETWEEN_SHAKES_MILLISECS) {
                mLastShakeTime = curTime;
                if((curTime - mServiceRunningCheckTime)>2*60*1000) {
                    Toast.makeText(getApplicationContext(), "Your App is Running in Background Service", Toast.LENGTH_SHORT).show();
                    mServiceRunningCheckTime = curTime;
                }
                mGravity = event.values.clone();
                // Shake detection
                float x = mGravity[0];
                float y = mGravity[1];
                float z = mGravity[2];
                mAccelLast = mAccelCurrent;
                //float xyz = x*x + y*y + z*z;
                ///mAccelCurrent = FloatMath.sqrt(xyz);
                mAccelCurrent = (float) (Math.sqrt(Math.pow(x, 2) + Math.pow(y, 2) + Math.pow(z, 2)) - SensorManager.GRAVITY_EARTH);
                float delta = mAccelCurrent - mAccelLast;
                mAccel = mAccel * 0.9f + delta;
                // Make this higher or lower according to how much
                // motion you want to detect
                if(mAccel > 0.5){
                    // do something
                    Log.d(TAG, "<------ Your phone is moving ----->");
                    Toast.makeText(getApplicationContext(), "Your phone is moving", Toast.LENGTH_SHORT).show();

                    try {
                        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
                            isScreenOn = powerManager.isInteractive();
                        } else {
                            isScreenOn = powerManager.isScreenOn();
                        }
                        Log.d(TAG, "<------ Your phone screen is"+" "+isScreenOn +"----->");
                        Toast.makeText(getApplicationContext(), "Your phone screen is"+" "+isScreenOn, Toast.LENGTH_SHORT).show();
                        if (!isScreenOn) {
                            //Power-On of the phone for notificaiton
                            try{
                                //@SuppressLint("InvalidWakeLockTag")
                                WakeLock screenLock = powerManager.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, "TotomApp::TotomLocationServiceWakeup");
                                //lock.disableKeyguard();
                                screenLock.acquire(2*60*1000L /*3 minutes*/);
                                //later
                                screenLock.release();

                            } catch (Exception ex){
                                ex.printStackTrace();
                            }
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    //if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//        startForegroundService(intent)
//        } else {
//        startService(intent)
//        }
}
