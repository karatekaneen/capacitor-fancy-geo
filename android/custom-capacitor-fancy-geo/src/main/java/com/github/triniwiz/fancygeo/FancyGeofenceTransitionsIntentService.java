package com.github.triniwiz.fancygeo;

import android.app.IntentService;
import android.app.Notification;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;
import com.google.gson.Gson;

import java.util.List;


/**
 * Created by Osei Fortune on 12/13/18
 */
public class FancyGeofenceTransitionsIntentService extends IntentService {

    FancyGeofenceTransitionsIntentService(String name) {
        super(name);
    }

    public FancyGeofenceTransitionsIntentService() {
        super("FancyGeofenceTransitions");
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);
        if (geofencingEvent != null && geofencingEvent.hasError()) {
           /* String errorMessage = GeofenceErrorMessages.getErrorString(this,
                    geofencingEvent.getErrorCode());*/
            //Log.e(FancyGeo.TAG, geofencingEvent.getErrorCode());
            Log.e(FancyGeo.TAG, "fencing event Error");
            return;
        }

        PackageManager pm = getPackageManager();
        Intent launchIntent = pm.getLaunchIntentForPackage(getApplicationContext().getPackageName());
        if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(launchIntent);
            this.handleCircleFenceOnTransition(geofencingEvent, "internalNotification");
        } else {
            this.handleCircleFenceOnTransition(geofencingEvent, "notification");
        }
    }

    private void handleCircleFenceOnTransition(GeofencingEvent geofencingEvent, String action) {
        Gson gson = FancyGeo.getGsonInstance();
        int geofenceTransition = geofencingEvent != null ? geofencingEvent.getGeofenceTransition() : -1;
        List<Geofence> triggeringGeofences = geofencingEvent != null ? geofencingEvent.getTriggeringGeofences() : null;
        SharedPreferences preferences = getApplicationContext().getSharedPreferences(FancyGeo.GEO_LOCATION_DATA, 0);
        switch (geofenceTransition) {
            case Geofence.GEOFENCE_TRANSITION_ENTER:
                if (triggeringGeofences != null) {
                    for (Geofence fence : triggeringGeofences) {
                        String id = fence.getRequestId();
                        String request = preferences.getString(id, "");
                        String type = FancyGeo.getType(request);
                        if (!request.isEmpty() && type.equals("circle")) {
                            FancyGeo.CircleFence geoFence = gson.fromJson(request, FancyGeo.CircleFence.class);
                            if (action == "notification") {
                                FancyGeoNotifications.sendNotification(geoFence.getNotification(), "enter");
                            } else {
                                FancyGeo.FenceNotification appNotification = geoFence.getNotification();
                                FancyGeo.executeOnMessageReceivedListener(FancyGeo.getGsonInstance().toJson(appNotification));
                            }

                        }
                    }
                }
                break;
            case Geofence.GEOFENCE_TRANSITION_DWELL:
                // TODO
                break;
            case Geofence.GEOFENCE_TRANSITION_EXIT:
                if (triggeringGeofences != null) {
                    for (Geofence fence : triggeringGeofences) {
                        String id = fence.getRequestId();
                        String request = preferences.getString(id, "");
                        String type = FancyGeo.getType(request);
                        if (!request.isEmpty() && type.equals("circle")) {
                            FancyGeo.CircleFence geoFence = gson.fromJson(request, FancyGeo.CircleFence.class);
                            if (action == "notification") {
                                FancyGeoNotifications.sendNotification(geoFence.getNotification(), "exit");
                            } else {
                                FancyGeo.FenceNotification appNotification = geoFence.getNotification();
                                FancyGeo.executeOnMessageReceivedListener(FancyGeo.getGsonInstance().toJson(appNotification));
                            }

                        }
                    }
                }
                break;
        }
    }
}
