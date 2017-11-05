package com.dronepath;

import android.app.Application;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;

import com.dronepath.mission.MissionControl;
import com.o3dr.android.client.ControlTower;
import com.o3dr.android.client.Drone;
import com.o3dr.android.client.interfaces.DroneListener;
import com.o3dr.android.client.interfaces.LinkListener;
import com.o3dr.android.client.interfaces.TowerListener;
import com.o3dr.services.android.lib.gcs.link.LinkConnectionStatus;

/**
 * Created by wsong on 11/5/17.
 */

public class DronePath extends Application implements DroneListener, TowerListener, LinkListener {

    // final variables
    private final Handler handler = new Handler();

    // Private member variables
    private Drone drone;
    private ControlTower controlTower;
    private MissionControl missionControl;
    private LocalBroadcastManager lbm;

    @Override
    public void onCreate() {
        super.onCreate();

        final Context context = getApplicationContext();
        lbm = LocalBroadcastManager.getInstance(context);

        // Initialize DroneKit variables
        controlTower = new ControlTower(context);
        drone = new Drone(context);
        missionControl = new MissionControl(this, this.drone);
    }

    @Override
    public void onTowerConnected() {
        // Disconnect this listener first in case if it is already
        drone.unregisterDroneListener(this);

        // then register drone and control tower
        controlTower.registerDrone(drone, handler);
        drone.registerDroneListener(this);

        // TODO Notify API Listener not implemented, might need to?
        // notifyApiConnected();
    }

    @Override
    public void onTowerDisconnected() {
        // TODO Notify API Listener not implemented, might need to?
        // notifyApiDisconnected();
    }

    @Override
    public void onDroneEvent(String event, Bundle extras) {

    }

    @Override
    public void onDroneServiceInterrupted(String errorMsg) {

    }

    @Override
    public void onLinkStateUpdated(@NonNull LinkConnectionStatus connectionStatus) {

    }

    // TODO Not implemented
    private void notifyApiConnected() {
        return;
    }

    // TODO Not implemented
    private void notifyApiDisconnected() {
        return;
    }
}