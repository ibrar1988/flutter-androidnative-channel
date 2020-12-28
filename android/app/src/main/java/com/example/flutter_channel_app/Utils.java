package com.example.flutter_channel_app;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.location.Address;
import android.preference.PreferenceManager;
import android.util.Log;

import java.text.DateFormat;
import java.util.Date;
import java.util.List;

import static android.content.Context.NOTIFICATION_SERVICE;

public class Utils {
    private static final String TAG = "UtilsClass";
    final static String KEY_LOCATION_UPDATES_RESULT = "location-update-result";
    public static float accuracy;
    static String addressFragments = "";
    static List<Address> addresses = null;
    public static final long UPDATE_INTERVAL = 2 * 1000;
    public static final float SMALLEST_DISPLACEMENT = 1.0F;
    public static final long FASTEST_UPDATE_INTERVAL = UPDATE_INTERVAL / 2;
    public static final long MAX_WAIT_TIME = UPDATE_INTERVAL * 2;

    static void setLocationUpdatesResult(Context context, String value) {
        Log.d(TAG, "setLocationUpdatesResult called");
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putString(KEY_LOCATION_UPDATES_RESULT, value)
                .apply();
    }

    public static void showNotificationOngoing(Context context, String broadCastEventName, String title) {

        Log.d(TAG, "showNotificationOngoing called");

        NotificationManager notificationManager = null;
        //if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
        notificationManager = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
        //}

        PendingIntent contentIntent = PendingIntent.getActivity(context, 0, new Intent(context, MainActivity.class), PendingIntent.FLAG_UPDATE_CURRENT);

        Notification.Builder notificationBuilder = new Notification.Builder(context)
                .setContentTitle(title + DateFormat.getDateTimeInstance().format(new Date()) + ":" + accuracy)
                .setContentText(addressFragments.toString())
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentIntent(contentIntent)
                .setOngoing(true)
                .setStyle(new Notification.BigTextStyle().bigText(addressFragments.toString()))
                .setAutoCancel(false);
        notificationManager.notify(3, notificationBuilder.build());
    }

    public static void removeNotification(Context context){
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
        notificationManager.cancelAll();
    }
}
