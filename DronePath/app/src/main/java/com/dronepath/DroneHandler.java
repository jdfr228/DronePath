package com.dronepath;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Spinner;

import com.dronepath.mission.MissionControl;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.model.LatLng;
import com.o3dr.android.client.ControlTower;
import com.o3dr.android.client.Drone;
import com.o3dr.android.client.apis.ControlApi;
import com.o3dr.android.client.apis.VehicleApi;
import com.o3dr.android.client.interfaces.DroneListener;
import com.o3dr.android.client.interfaces.TowerListener;
import com.o3dr.services.android.lib.coordinate.LatLong;
import com.o3dr.services.android.lib.drone.attribute.AttributeEvent;
import com.o3dr.services.android.lib.drone.attribute.AttributeType;
import com.o3dr.services.android.lib.drone.connection.ConnectionParameter;
import com.o3dr.services.android.lib.drone.connection.ConnectionType;
import com.o3dr.services.android.lib.drone.property.Gps;
import com.o3dr.services.android.lib.drone.property.State;
import com.o3dr.services.android.lib.drone.property.VehicleMode;
import com.o3dr.services.android.lib.model.AbstractCommandListener;

import java.util.List;

/**
 * Created by Dylan on 11/21/2017.
 */

public class DroneHandler implements DroneListener, TowerListener {
    private Spinner modeSelector;

    // Drone related stuff
    private Drone drone;
    private ControlTower controlTower;
    private MissionControl missionControl;
    private final Handler handler = new Handler();

    private int nextWaypoint = 0;

    // Drone state constants
    static final int USER_CLICKED = 0;
    static final int DRONE_CONNECTED = 1;
    static final int DRONE_DISCONNECTED = 2;
    static final int DRONE_ARMED = 3;
    private int droneState = DRONE_DISCONNECTED;

    private boolean updateMapFlag = true;

    private MainActivity activity;

    DroneHandler(Context context) {
        if (context instanceof Activity) {
            activity = (MainActivity) context;
        }

        this.controlTower = new ControlTower(context);
        this.drone = new Drone(context);
        this.missionControl = new MissionControl(context, drone);
    }

    // Getter functions
    int getDroneState() {
        return droneState;
    }

    boolean isDroneConnected() {
        return drone.isConnected();
    }


    // Drone connection functions
    /**
     * Connects to drone through UDP protocol
     */
    void connectToDrone() {
        Log.d("myTag", "connectToDrone() called");
        activity.alertUser("Connecting to drone...");
        Bundle extraParams = new Bundle();
        extraParams.putInt(ConnectionType.EXTRA_UDP_SERVER_PORT, 14550); // Set default port to 14550

        ConnectionParameter connectionParams = new ConnectionParameter(ConnectionType.TYPE_UDP,
                extraParams,
                null);

        this.drone.connect(connectionParams);

        // TODO- implement a timeout for drone connection
    }

    void disconnectDrone() {
        activity.alertUser("Disconnecting from drone...");
        activity.animateConnectArmFab(USER_CLICKED);
        this.drone.disconnect();

        // Remove drone GPS waypoint
        activity.mapFragment.clearDronePoint();
    }


    // Drone flight functions
    /**
     * Checks if drone is connected, then sends mission, arm drone, and take off!
     * Pre condition: droneState = DRONE_CONNECTED
     */
    void startFlight() {
        Log.d("myTag", "startFlight() called");
        // Checks of a drone is connected
        if(!drone.isConnected()) {
            activity.alertUser("Drone is not connected");
            droneState = DRONE_DISCONNECTED;
            activity.animateConnectArmFab(droneState);
            return;
        }

        // Sends waypoints if we have them, if not we request the user to make them
        if(activity.mapFragment.isSplineComplete()) {
            List<LatLong> waypoints = activity.mapFragment.getLatLongWaypoints();

            missionControl.addWaypoints(activity.mapFragment.getLatLongWaypoints());
            missionControl.sendMissionToAPM();
            Log.d("myTag", "waypoints sent to APM");
        }

        else {
            Log.d("myTag", "no waypoints drawn");
            activity.alertUser("No waypoints drawn");
            activity.animateConnectArmFab(droneState);
            return;
        }

        final State vehicleState = this.drone.getAttribute(AttributeType.STATE);

        // Change drone vehicle mode back to default
        VehicleApi.getApi(drone).setVehicleMode(VehicleMode.COPTER_STABILIZE, new AbstractCommandListener() {
            @Override
            public void onSuccess() {
                Log.d("myTag", "Drone vehicle mode changed to stabilize");

                // Arm drone if necessary
                if (!vehicleState.isArmed()) {
                    activity.alertUser("Arming drone...");
                    VehicleApi.getApi(drone).arm(true, false, new AbstractCommandListener() {
                        @Override
                        public void onSuccess() {
                            Log.d("myTag", "Drone armed");
                            activity.alertUser("Arming successful");
                            droneState = DRONE_ARMED;
                            //animateConnectArmFab(droneState); - wait to allow the user to click again

                            takeoff();
                        }

                        @Override
                        public void onError(int executionError) {
                            Log.d("myTag", "Drone arming error");
                            activity.alertUser("Arming not successful: " + executionError);
                            droneState = DRONE_CONNECTED;
                            activity.animateConnectArmFab(droneState);
                        }

                        @Override
                        public void onTimeout() {
                            Log.d("myTag", "Drone arming timeout");
                            activity.alertUser("Arming timed out");
                            droneState = DRONE_CONNECTED;
                            activity.animateConnectArmFab(droneState);
                        }
                    });
                }

                // Take off standalone if the drone is already armed
                else if (!vehicleState.isFlying()) {
                    takeoff();
                }
            }

            @Override
            public void onError(int executionError) {
                Log.d("myTag", "Drone vehicle mode error");
                activity.alertUser("Drone stabilize mode failed: " + executionError);
                droneState = DRONE_CONNECTED;
                activity.animateConnectArmFab(droneState);
            }

            @Override
            public void onTimeout() {
                Log.d("myTag", "Drone vehicle mode timeout");
                activity.alertUser("Drone stabilize mode timed out");
                droneState = DRONE_CONNECTED;
                activity.animateConnectArmFab(droneState);
            }
        });
    }

    void takeoff() {
        Log.d("myTag", "takeoff() called");
        activity.alertUser("Drone taking off...");
        ControlApi.getApi(drone).takeoff(1, new AbstractCommandListener() {
            @Override
            public void onSuccess() {
                VehicleApi.getApi(drone).setVehicleMode(VehicleMode.COPTER_AUTO, new AbstractCommandListener() {
                    @Override
                    public void onSuccess() {
                        Log.d("myTag", "Drone Flying");
                        activity.alertUser("Takeoff successful");
                        droneState = DRONE_ARMED;
                        activity.animateConnectArmFab(droneState);
                    }

                    @Override
                    public void onError(int executionError) {
                        Log.d("myTag", "Drone vehicle mode error");
                        activity.alertUser("Drone auto mode failed: " + executionError);
                        droneState = DRONE_CONNECTED;
                        activity.animateConnectArmFab(droneState);
                    }

                    @Override
                    public void onTimeout() {
                        Log.d("myTag", "Drone vehicle mode timeout");
                        activity.alertUser("Drone auto mode timed out");
                        droneState = DRONE_CONNECTED;
                        activity.animateConnectArmFab(droneState);
                    }
                });
            }

            @Override
            public void onError(int executionError) {
                Log.d("myTag", "Drone flight error");
                activity.alertUser("Drone failed to take off: " + executionError);
                droneState = DRONE_CONNECTED;
                activity.animateConnectArmFab(droneState);
            }

            @Override
            public void onTimeout() {
                Log.d("myTag", "Drone flight timeout");
                activity.alertUser("Drone take off timed out");
                droneState = DRONE_CONNECTED;
                activity.animateConnectArmFab(droneState);
            }
        });
    }

    void returnHome() {
        VehicleApi.getApi(drone).setVehicleMode(VehicleMode.COPTER_RTL, new AbstractCommandListener() {
            @Override
            public void onSuccess() {
                Log.d("myTag", "Drone returning home");
                activity.alertUser("Drone returning home...");
            }

            @Override
            // TODO- drone may not actually still be armed if this error is thrown?
            public void onError(int executionError) {
                Log.d("myTag", "Drone returning home error");
                activity.alertUser("Drone returning home failed: " + executionError);
                droneState = DRONE_ARMED;
                activity.animateConnectArmFab(droneState);
            }

            @Override
            public void onTimeout() {
                Log.d("myTag", "Drone returning home timeout");
                activity.alertUser("Drone returning home timed out");
                droneState = DRONE_ARMED;
                activity.animateConnectArmFab(droneState);
            }
        });
    }


    // Event listeners
    /**
     * Listener that responds to events and respond accordingly
     *
     * @param event
     * @param extras
     */
    @Override
    public void onDroneEvent(String event, Bundle extras) {
        switch (event) {
            case AttributeEvent.STATE_CONNECTED:
                Log.d("myTag", "Drone connected");
                activity.alertUser("Drone Connected");
                droneState = DRONE_CONNECTED;
                activity.animateConnectArmFab(droneState);

                LatLong dummy = new LatLong(0,0);
                activity.mapFragment.onDroneConnected(dummy);
                break;

            // Triggers when the drone reaches a waypoint
            // TODO- see if there's a better way to check for the final waypoint
            // TODO- doesn't work if there is a single waypoint on the mission
            case AttributeEvent.MISSION_ITEM_REACHED:
                nextWaypoint += 1;

                // Block user input if this was the final waypoint on the mission
                if (missionControl.isFinalWaypoint(nextWaypoint)) {
                    activity.alertUser("Drone returning home...");
                    activity.animateConnectArmFab(USER_CLICKED);
                }

                break;

            case AttributeEvent.STATE_DISCONNECTED:
                Log.d("myTag", "Drone disconnected");
                activity.alertUser("Drone Disconnected");
                droneState = DRONE_DISCONNECTED;
                activity.animateConnectArmFab(droneState);
                updateMapFlag = true;   // Update the map on the next drone connect
                break;

            // TODO Make this more concise
            // Note- this listener is also triggered when the drone has landed and is disarming
            case AttributeEvent.STATE_ARMING:
                Log.d("myTag", "Drone arming (listener)");
                State vehicleState = this.drone.getAttribute(AttributeType.STATE);

                if (!vehicleState.isArmed()) {                      // Drone has landed
                    Log.d("myTag", "Drone landed");
                    activity.alertUser("Drone successfully landed");
                    nextWaypoint = 0;
                    droneState = DRONE_CONNECTED;
                    activity.animateConnectArmFab(droneState);
                }

                //activity.alertUser("Arming Drone and taking off...");
//                Gps locationHome = drone.getAttribute(AttributeType.GPS);
//                LatLongAlt droneHome = new LatLongAlt(locationHome.getPosition(), 0);
//                Altitude altitude = drone.getAttribute(AttributeType.ALTITUDE);
//                droneHome.setAltitude(altitude.getAltitude());
//                VehicleApi.getApi(drone).setVehicleHome(droneHome, null);
                break;

            case AttributeEvent.STATE_VEHICLE_MODE:
                vehicleState = this.drone.getAttribute(AttributeType.STATE);
                Log.d("myTag", "Current vehicle mode is now: "
                        + vehicleState.getVehicleMode());
                break;

            // When drone has valid GPS location. Used for displaying the drone's location
            case AttributeEvent.GPS_POSITION:
                Gps location = drone.getAttribute(AttributeType.GPS);
                activity.mapFragment.onDroneGPSUpdated(location.getPosition());

                // Move the Map to the drone's location on initial connect
                // Placed in GPS_POSITION to ensure a Location is available
                if (updateMapFlag) {
                    Gps droneGps = this.drone.getAttribute(AttributeType.GPS);
                    LatLong vehicleLocation = droneGps.getPosition();

                    // Convert drone LatLong into Map LatLng
                    LatLng updateLocation = new LatLng(vehicleLocation.getLatitude(),
                            vehicleLocation.getLongitude());

                    // Update Map
                    CameraUpdate update = CameraUpdateFactory.newLatLngZoom(updateLocation,
                            activity.mapFragment.getDefaultZoom());
                    activity.mapFragment.getMap().animateCamera(update);
                    updateMapFlag = false;
                }
                break;

            default:
                break;
        }
    }

    @Override
    // TODO- find if this was interpreted correctly (triggers once, drone no longer connected)
    public void onDroneServiceInterrupted(String errorMsg) {
        Log.d("myTag", "onDroneServiceInterrupted triggered");
        activity.alertUser(errorMsg);
        droneState = DRONE_DISCONNECTED;
        activity.animateConnectArmFab(droneState);
    }

    /**
     * Safely handles when the Tower drone service connects
     */
    @Override
    public void onTowerConnected() {
        this.controlTower.registerDrone(this.drone, this.handler);
        this.drone.registerDroneListener(this);
    }

    /**
     * Safely handles when the Tower drone service disconnects
     */
    @Override
    public void onTowerDisconnected() {}

    public void onFlightModeSelected(View view) {
        VehicleMode vehicleMode = (VehicleMode) this.modeSelector.getSelectedItem();
        // this.drone.changeVehicleMode(vehicleMode);
    }


    // Helper functions
    void controlTowerConnect() {
        controlTower.connect(this);
    }

    void controlTowerDisconnect() {
        controlTower.unregisterDrone(drone);
        controlTower.disconnect();
    }
}