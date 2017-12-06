package com.dronepath;

import android.app.Activity;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;

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
import com.o3dr.services.android.lib.drone.property.Altitude;
import com.o3dr.services.android.lib.drone.property.Gps;
import com.o3dr.services.android.lib.drone.property.Speed;
import com.o3dr.services.android.lib.drone.property.State;
import com.o3dr.services.android.lib.drone.property.VehicleMode;
import com.o3dr.services.android.lib.model.AbstractCommandListener;

import java.util.List;


public class DroneHandlerFragment extends Fragment implements DroneListener, TowerListener {
    // Drone related variables
    private Drone drone;
    private ControlTower controlTower;
    private MissionControl missionControl;
    private final Handler handler = new Handler();
    private int nextWaypoint = 0;
    private boolean updateMapFlag = true;

    // Drone state constants
    static final int USER_CLICKED = 0;
    static final int DRONE_CONNECTED = 1;
    static final int DRONE_DISCONNECTED = 2;
    static final int DRONE_ARMED = 3;
    private static int droneState = DRONE_DISCONNECTED;

    // Class references
    private static ConnectionTimeoutFragment connectTimeout;
    private MainActivity activity;

    // Drone data
    private SharedPreferences sharedPreferences;
    private static TelemetryViewModel mModel;

    // Fragment Lifecycle methods (in the order they would be called)
    // Called each time a new MainActivity is created (such as on screen rotation)
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        // Set up activity reference now that the Fragment is attached to the new MainActivity
        if (context instanceof Activity) {
            activity = (MainActivity) context;
        }

        // Attach a ConnectionTimeoutFragment for timing out the initial drone connection
        FragmentManager fragmentManager = activity.getSupportFragmentManager();
        connectTimeout = (ConnectionTimeoutFragment) fragmentManager.findFragmentByTag("timeoutFragment");
        if (connectTimeout == null) {
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            connectTimeout = new ConnectionTimeoutFragment();
            fragmentTransaction.add(connectTimeout, "timeoutFragment");
            fragmentTransaction.commit();
        }
    }

    // Called once since setRetainInstance is set to true
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Don't destroy the fragment on a screen rotation
        setRetainInstance(true);

        // Initial variable setup
        drone = new Drone(activity);
        controlTower = new ControlTower(activity);
        controlTowerConnect();
        missionControl = new MissionControl(activity, drone);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(activity);

        mModel = ViewModelProviders.of(this).get(TelemetryViewModel.class);
    }

    //@Override
    //public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        //view = inflater.inflate(R.layout.activity_telemetry, container, false);
        //return view;
    //}

    @Override
    public void onStart() {
        super.onStart();
        //latitudeTextView = (TextView) view.findViewById(R.id.telemetry_latitude);
        //latitudeTextView.setText("Testing");
    }

    // Should only be called when the app is killed since setRetainInstance is set to true
    @Override
    public void onDestroy() {
        super.onDestroy();
        returnHome(true);   // Attempt to send an rtl command on app exit
        disconnectDrone();
        controlTowerDisconnect();
        controlTower = null;
        connectTimeout = null;
        drone = null;
        missionControl = null;
    }

    @Override
    public void onDetach() {
        super.onDetach();

        // Avoid a memory leak on screen rotation by manually removing references to old MainActivity
        activity = null;
    }


    // Getter methods
    public int getDroneState() {
        return droneState;
    }

    public static TelemetryViewModel getModel() {
        return mModel;
    }

    public boolean isDroneConnected() {
        if (drone != null) {
            return drone.isConnected();
        } else return false;
    }


    // Drone connection helper methods
    /**
     * Connects to drone through UDP protocol
     */
    void connectToDrone() {
        activity.alertUser("Connecting to drone...");

        // Set up connection parameters with port read from SharedPreferences (Settings)
        Bundle extraParams = new Bundle();
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(
                activity.getApplicationContext());
        int port = Integer.parseInt(sharedPreferences.getString("pref_key_drone_port", ""));
        extraParams.putInt(ConnectionType.EXTRA_UDP_SERVER_PORT, port); // default port is 14550

        ConnectionParameter connectionParams = new ConnectionParameter(ConnectionType.TYPE_UDP,
                extraParams,
                null);

        this.drone.connect(connectionParams);

        // Get length in seconds of the connection timeout
        final int timeoutLength = Integer.parseInt(sharedPreferences.getString(
                "pref_key_timeout_length", ""));

        // start an asynchronous timeout thread for connection timeout
        Log.d("timeout", "sending timeout length of: " + timeoutLength);
        connectTimeout.startTask(timeoutLength);
    }

    void disconnectDrone() {
        if (drone != null && drone.isConnected()) {
            activity.alertUser("Disconnecting from drone...");
            activity.animateConnectArmFab(USER_CLICKED);
            this.drone.disconnect();
            droneState = DRONE_DISCONNECTED;

            // Remove drone GPS waypoint
            activity.mapFragment.clearDronePoint();
        }
    }

    void controlTowerConnect() {
        controlTower.connect(this);
    }

    void controlTowerDisconnect() {
        if (drone != null) {
            controlTower.unregisterDrone(drone);
        }
        controlTower.disconnect();
    }


    // Drone flight methods
    /**
     * Checks if drone is connected, then sends mission, arm drone, and take off!
     * Pre condition: droneState = DRONE_CONNECTED
     */
    void startFlight() {

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
            Log.d("drone", "Waypoints sent to APM");
        }
        else {
            activity.alertUser("No waypoints drawn");
            activity.animateConnectArmFab(droneState);
            return;
        }

        // Change drone vehicle mode back to default before attempting to arm
        VehicleApi.getApi(drone).setVehicleMode(VehicleMode.COPTER_STABILIZE, new AbstractCommandListener() {
            @Override
            public void onSuccess() {
                Log.d("drone", "Drone vehicle mode successfully changed to stabilize");
                arm();
            }

            @Override
            public void onError(int executionError) {
                activity.alertUser("Drone stabilize mode change failed: " + executionError);
                droneState = DRONE_CONNECTED;
                activity.animateConnectArmFab(droneState);
            }

            @Override
            public void onTimeout() {
                activity.alertUser("Drone stabilize mode change timed out");
                droneState = DRONE_CONNECTED;
                activity.animateConnectArmFab(droneState);
            }
        });
    }

    private void arm() {
        final State vehicleState = this.drone.getAttribute(AttributeType.STATE);

        // Arm drone if necessary
        if (!vehicleState.isArmed()) {
            activity.alertUser("Arming drone...");
            VehicleApi.getApi(drone).arm(true, false, new AbstractCommandListener() {
                @Override
                public void onSuccess() {
                    activity.alertUser("Arming successful");
                    droneState = DRONE_ARMED;
                    //animateConnectArmFab(droneState); - wait to allow the user to click again

                    takeoff();
                }

                @Override
                public void onError(int executionError) {
                    activity.alertUser("Arming not successful: " + executionError);
                    droneState = DRONE_CONNECTED;
                    activity.animateConnectArmFab(droneState);
                }

                @Override
                public void onTimeout() {
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

    private void takeoff() {

        activity.alertUser("Drone taking off...");
        // TODO- check this altitude: 1 setting- does it need to read the altitude variable?
        ControlApi.getApi(drone).takeoff(1, new AbstractCommandListener() {
            @Override
            public void onSuccess() {
                VehicleApi.getApi(drone).setVehicleMode(VehicleMode.COPTER_AUTO, new AbstractCommandListener() {
                    @Override
                    public void onSuccess() {
                        activity.alertUser("Takeoff successful");
                        droneState = DRONE_ARMED;
                        activity.animateConnectArmFab(droneState);
                    }

                    @Override
                    public void onError(int executionError) {
                        activity.alertUser("Drone auto mode change failed: " + executionError);
                        droneState = DRONE_CONNECTED;
                        activity.animateConnectArmFab(droneState);
                    }

                    @Override
                    public void onTimeout() {
                        activity.alertUser("Drone auto mode change timed out");
                        droneState = DRONE_CONNECTED;
                        activity.animateConnectArmFab(droneState);
                    }
                });
            }

            @Override
            public void onError(int executionError) {
                activity.alertUser("Drone failed to take off: " + executionError);
                droneState = DRONE_CONNECTED;
                activity.animateConnectArmFab(droneState);
            }

            @Override
            public void onTimeout() {
                activity.alertUser("Drone take off timed out");
                droneState = DRONE_CONNECTED;
                activity.animateConnectArmFab(droneState);
            }
        });
    }

    void returnHome(final boolean appExit) {
        VehicleApi.getApi(drone).setVehicleMode(VehicleMode.COPTER_RTL, new AbstractCommandListener() {
            @Override
            public void onSuccess() {
                if (!appExit) {
                    activity.alertUser("Drone returning home...");
                }
            }

            @Override
            // TODO- drone may not actually still be armed if this error is thrown?
            public void onError(int executionError) {
                if (!appExit) {
                    activity.alertUser("Drone returning home failed: " + executionError);
                    droneState = DRONE_ARMED;
                    activity.animateConnectArmFab(droneState);
                }
            }

            @Override
            public void onTimeout() {
                if (!appExit) {
                    activity.alertUser("Drone returning home timed out");
                    droneState = DRONE_ARMED;
                    activity.animateConnectArmFab(droneState);
                }
            }
        });
    }

    // Push new velocity to drone
    void updateVelocity(float newVelocity, boolean inFlight) {
        missionControl.updateVelocity(newVelocity, inFlight);
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
                // Cancel the timeout Async task
                connectTimeout.stopTask();

                activity.alertUser("Drone Connected");
                droneState = DRONE_CONNECTED;
                activity.animateConnectArmFab(droneState);

                LatLong dummy = new LatLong(0,0);
                activity.mapFragment.onDroneConnected(dummy);
                break;

            // Triggers when the drone reaches a waypoint
            // TODO- see if there's a better way to check for the final waypoint
            // TODO- *may* not work if there is a single waypoint on the mission
            //         ^^ Unable to reproduce at this time, may have been due to another bug?
            case AttributeEvent.MISSION_ITEM_REACHED:
                Log.d("waypoints", "total number of waypoints = " + missionControl.missionItems.size());
                Log.d("waypoints", "nextWaypoint = " + nextWaypoint);
                nextWaypoint += 1;

                // Block user input if this was the final waypoint on the mission
                if (missionControl.isFinalWaypoint(nextWaypoint)) {
                    activity.alertUser("Drone returning home...");
                    activity.animateConnectArmFab(USER_CLICKED);
                }

                break;

            case AttributeEvent.STATE_DISCONNECTED:
                activity.alertUser("Drone Disconnected");
                droneState = DRONE_DISCONNECTED;
                activity.animateConnectArmFab(droneState);
                updateMapFlag = true;   // Flag to update the map on the next drone connect
                break;

            // Note- this listener is also triggered when the drone has landed and is disarming
            case AttributeEvent.STATE_ARMING:
                Log.d("drone", "Drone arming (listener)");
                State vehicleState = this.drone.getAttribute(AttributeType.STATE);

                if (!vehicleState.isArmed()) {                      // Drone has landed
                    activity.alertUser("Drone successfully landed");
                    nextWaypoint = 0;
                    droneState = DRONE_CONNECTED;
                    activity.animateConnectArmFab(droneState);
                }
                break;

            case AttributeEvent.STATE_VEHICLE_MODE:
                vehicleState = this.drone.getAttribute(AttributeType.STATE);
                Log.d("drone", "Current vehicle mode is now: "
                        + vehicleState.getVehicleMode());
                break;

            // When drone has valid GPS location. Used for displaying the drone's location
            case AttributeEvent.GPS_POSITION:
                Gps location = drone.getAttribute(AttributeType.GPS);

                // Send GPS location of drone to the telemetry data screen
                //sharedPreferences.edit().putString("pref_key_telemetry_latitude", Double.toString(location.getPosition().getLatitude())).apply();
                //sharedPreferences.edit().putString("pref_key_telemetry_longitude", Double.toString(location.getPosition().getLongitude())).apply();
                mModel.getLatitude().setValue(Double.toString(location.getPosition().getLatitude()));
                mModel.getLongitude().setValue(Double.toString(location.getPosition().getLongitude()));

                // Send drone location to map
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

            case AttributeEvent.ALTITUDE_UPDATED:
                // Send altitude of drone to the telemetry data screen
                Altitude altitude = this.drone.getAttribute(AttributeType.ALTITUDE);
                //sharedPreferences.edit().putString("pref_key_telemetry_altitude", Double.toString(altitude.getAltitude())).apply();
                mModel.getAltitude().setValue(Double.toString(altitude.getAltitude()));
                break;

            case AttributeEvent.SPEED_UPDATED:
                // Send velocity of drone to the telemetry data screen
                Speed speed = this.drone.getAttribute(AttributeType.SPEED);
                //sharedPreferences.edit().putString("pref_key_telemetry_velocity", Double.toString(speed.getGroundSpeed())).apply();
                mModel.getVelocity().setValue(Double.toString(speed.getGroundSpeed()));

            default:
                break;
        }
    }

    @Override
    // TODO- was this interpreted correctly? (triggers once, drone no longer connected)
    public void onDroneServiceInterrupted(String errorMsg) {
        Log.d("drone", "onDroneServiceInterrupted triggered");
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
    public void onTowerDisconnected() {
        this.controlTower.unregisterDrone(this.drone);
        this.drone.unregisterDroneListener(this);
    }
}
