package com.dronepath;

import android.content.Intent;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.app.DialogFragment;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Toast;

import com.dronepath.mission.MissionControl;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.Projection;
import com.google.android.gms.maps.model.LatLng;
import com.o3dr.android.client.ControlTower;
import com.o3dr.android.client.Drone;
import com.o3dr.android.client.apis.ControlApi;
import com.o3dr.android.client.apis.VehicleApi;
import com.o3dr.android.client.interfaces.DroneListener;
import com.o3dr.android.client.interfaces.LinkListener;
import com.o3dr.android.client.interfaces.TowerListener;
import com.o3dr.services.android.lib.coordinate.LatLong;
import com.o3dr.services.android.lib.drone.attribute.AttributeEvent;
import com.o3dr.services.android.lib.drone.attribute.AttributeType;
import com.o3dr.services.android.lib.drone.connection.ConnectionParameter;
import com.o3dr.services.android.lib.drone.connection.ConnectionType;
import com.o3dr.services.android.lib.drone.property.Gps;
import com.o3dr.services.android.lib.drone.property.Speed;
import com.o3dr.services.android.lib.drone.property.Type;
import com.o3dr.services.android.lib.drone.property.VehicleMode;
import com.o3dr.services.android.lib.gcs.link.LinkConnectionStatus;
import com.o3dr.services.android.lib.model.AbstractCommandListener;

import java.util.List;
import java.util.ResourceBundle;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener,
                    FlightVarsDialogFragment.OnCompleteListener,
                    GPSLocationDialogFragment.OnCompleteListener,
                    View.OnClickListener, TowerListener, DroneListener {

    // Global variables - if another Activity needs to change them, pass them back to the Main Activity
    public double velocity, altitude;
    public double maxVelocity = 10.0;
    public double maxAltitude = 10.0;
    public String savedLatitude = "";
    public String savedLongitude = "";

    // Floating Action Buttons
    private FloatingActionButton menu_fab,edit_fab,place_fab,delete_fab;
    boolean isFabExpanded, isMapDrawable = false;
    private Animation open_fab,close_fab;

    private DroneMapFragment mapFragment;

    // Drone related stuff
    private Drone drone;
    private ControlTower controlTower;
    private MissionControl missionControl;
    private final Handler handler = new Handler();

    // TODO - since the variables are encapsulated in MainActivity, setters may not be needed?
    public void setVelocity(double newVelocity) {
        velocity = newVelocity;
    }
    public void setAltitude(double newAltitude) {
        altitude = newAltitude;
    }

    // FlightVarsDialogFragment.OnCompleteListener implementation (passes variables)
    public void onFlightVarsDialogComplete(double velocity, double altitude) {
        setVelocity(velocity);
        setAltitude(altitude);
    }

    // GPSLocationDialogFragment.OnCompleteListener implementation (add GPS waypoint)
    public void onGPSLocationDialogComplete(double latitude, double longitude, boolean isWaypoint) {
        mapFragment = (DroneMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        List<LatLong> points = mapFragment.getLatLongWaypoints();

        if (isWaypoint) {
            // Add a new waypoint on the map
            LatLng latLng = new LatLng(latitude, longitude);
            mapFragment.addPoint(latLng);

        } else {
            // Simply move the map if the address won't be added as a waypoint
            LatLng newLocation = new LatLng(latitude, longitude);
            CameraUpdate update = CameraUpdateFactory.newLatLngZoom(newLocation, mapFragment.getDefaultZoom());
            mapFragment.getMap().animateCamera(update);
        }

        // Save entered Latitude and Longitude for the next time the Dialog opens
        savedLatitude = Double.toString(latitude);
        savedLongitude = Double.toString(longitude);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mapFragment = (DroneMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.setRetainInstance(true);
        mapFragment.setOnDragListener(new DroneMapWrapper.OnDragListener() {
            @Override
            public void onDrag(MotionEvent motionEvent) {
                if (!isMapDrawable || !isFabExpanded || mapFragment.isSplineComplete())
                    return;

                Log.i("ON_DRAG", "X:" + String.valueOf(motionEvent.getX()));
                Log.i("ON_DRAG", "Y:" + String.valueOf(motionEvent.getY()));

                float x = motionEvent.getX();
                float y = motionEvent.getY();

                int x_co = Integer.parseInt(String.valueOf(Math.round(x)));
                int y_co = Integer.parseInt(String.valueOf(Math.round(y)));

                Projection projection = mapFragment.getMap().getProjection();
                Point x_y_points = new Point(x_co, y_co);
                LatLng latLng = projection.fromScreenLocation(x_y_points);
                double latitude = latLng.latitude;
                double longitude = latLng.longitude;

                Log.i("ON_DRAG", "lat:" + latitude);
                Log.i("ON_DRAG", "long:" + longitude);

                // Handle motion event:
                mapFragment.addPoint(latLng);
            }
        });

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        menu_fab = (FloatingActionButton) findViewById(R.id.menu_fab);
        edit_fab = (FloatingActionButton) findViewById(R.id.edit_fab);
        place_fab = (FloatingActionButton) findViewById(R.id.place_fab);
        delete_fab = (FloatingActionButton) findViewById(R.id.delete_fab);
        menu_fab.setOnClickListener(this);
        edit_fab.setOnClickListener(this);
        place_fab.setOnClickListener(this);
        delete_fab.setOnClickListener(this);
        open_fab = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.open_fab);
        close_fab = AnimationUtils.loadAnimation(getApplicationContext(),R.anim.close_fab);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        this.controlTower = new ControlTower(getApplicationContext());
        this.drone = new Drone(getApplicationContext());
        this.missionControl = new MissionControl(getApplicationContext(), drone);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        switch (id){
            case R.id.menu_fab:
                animateFabButtons();
                break;
            case R.id.edit_fab:
                if (mapFragment.isSplineComplete())
                    break;
                isMapDrawable = !isMapDrawable;
                if (isMapDrawable) {
                    mapFragment.getMap().getUiSettings().setScrollGesturesEnabled(false);
                }
                else {
                    mapFragment.getMap().getUiSettings().setScrollGesturesEnabled(true);
                }
                break;
            case R.id.place_fab:
                mapFragment.convertToSpline();
                isMapDrawable = false;
                mapFragment.getMap().getUiSettings().setScrollGesturesEnabled(true);
                break;
            case R.id.delete_fab:
                if (mapFragment != null)
                    mapFragment.clearPoints();
                break;
        }
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            // Create an Intent, which binds two Activities at runtime
            Intent intent = new Intent(this, SettingsActivity.class);

            // Switch to the settings screen Activity
            startActivity(intent);
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        switch(id) {
            case R.id.nav_gps:
                // Create an instance of the GPS Input Dialog
                GPSLocationDialogFragment gpsDialog = new GPSLocationDialogFragment();

                // Pass arguments
                Bundle GPSargs = new Bundle();
                GPSargs.putString("savedLatitude", savedLatitude);
                GPSargs.putString("savedLongitude", savedLongitude);
                gpsDialog.setArguments(GPSargs);

                // Display the dialog to the user
                gpsDialog.show(getFragmentManager(), "GPSDialog");
                break;

            case R.id.nav_flight_vars:
                FlightVarsDialogFragment flightVarsDialog = new FlightVarsDialogFragment();

                // Pass arguments to the new Dialog
                Bundle FlightVarArgs = new Bundle();
                FlightVarArgs.putDouble("currVelocity", velocity);
                FlightVarArgs.putDouble("maxVelocity", maxVelocity);
                FlightVarArgs.putDouble("currAltitude", altitude);
                FlightVarArgs.putDouble("maxAltitude", maxAltitude);
                flightVarsDialog.setArguments(FlightVarArgs);

                flightVarsDialog.show(getFragmentManager(), "FlightVarsDialog");
                break;

            case R.id.nav_connect:
                // TODO Handle drone connection
                connectToDrone();
                break;

            case R.id.nav_start:
                // TODO Handle starting the drone flight
                // TODO- this and the connect button will likely be combined into 1 button on the main screen
                startFlight();
                break;
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    public void animateFabButtons(){
        if (isFabExpanded){
            isFabExpanded = false;
            isMapDrawable = false;
            mapFragment.getMap().getUiSettings().setScrollGesturesEnabled(true);
            menu_fab.setImageResource(R.mipmap.ic_more_vert_white_24dp);
            edit_fab.startAnimation(close_fab);
            place_fab.startAnimation(close_fab);
            delete_fab.startAnimation(close_fab);
            edit_fab.setClickable(false);
            place_fab.setClickable(false);
            delete_fab.setClickable(false);
        }
        else{
            isFabExpanded = true;
            menu_fab.setImageResource(R.mipmap.ic_close_white_24dp);
            edit_fab.startAnimation(open_fab);
            place_fab.startAnimation(open_fab);
            delete_fab.startAnimation(open_fab);
            edit_fab.setClickable(true);
            place_fab.setClickable(true);
            delete_fab.setClickable(true);
        }
    }

    /**
     * Connects to drone through UDP protocol
     */
    private void connectToDrone() {
        Bundle extraParams = new Bundle();
        extraParams.putInt(ConnectionType.EXTRA_UDP_SERVER_PORT, 14550); // Set default port to 14550

        ConnectionParameter connectionParams = new ConnectionParameter(ConnectionType.TYPE_UDP,
                extraParams,
                null);
        this.drone.connect(connectionParams);
    }

    /**
     * Checks if drone is connected, then sends mission, arm drone, and take off!
     */
    private void startFlight() {
        // Checks of a drone is connected
        if(!drone.isConnected()) {
            alertUser("Drone is not connected");
            return;
        }

        // Sends waypoints if we have them, if not we request the user to make them
        if(mapFragment.isSplineComplete()) {
            List<LatLong> waypoints = mapFragment.getLatLongWaypoints();

            missionControl.addWaypoints(mapFragment.getLatLongWaypoints());
            missionControl.sendMissionToAPM();
        }

        else {
            alertUser("No waypoints drawn");
            return;
        }

        // Arming drone
        VehicleApi.getApi(drone).arm(true, false, new AbstractCommandListener() {
            @Override
            public void onSuccess() {
                alertUser("Arming successful");
            }

            @Override
            public void onError(int executionError) {
                alertUser("Arming not successful: " + executionError);
            }

            @Override
            public void onTimeout() {
                alertUser("Arming timed out");
            }
        });

        // Drone take off
        ControlApi.getApi(drone).takeoff(10, new AbstractCommandListener() {
            @Override
            public void onSuccess() {
                VehicleApi.getApi(drone).setVehicleMode(VehicleMode.COPTER_AUTO);
            }

            @Override
            public void onError(int executionError) {
                alertUser("Drone failed to take off: " + executionError);
            }

            @Override
            public void onTimeout() {
                alertUser("Drone take off timed out");
            }
        });
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

    }

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
                alertUser("Drone Connected");
                LatLong dummy = new LatLong(0,0);
                mapFragment.onDroneConnected(dummy);
                break;

            case AttributeEvent.STATE_DISCONNECTED:
                alertUser("Drone Disconnected");
                break;

            // When drone has valid GPS location. Used for displaying the drone's location
            case AttributeEvent.GPS_POSITION:
                Gps location = drone.getAttribute(AttributeType.GPS);
                mapFragment.onDroneGPSUpdated(location.getPosition());
                break;

            default:
                break;
        }
    }

    @Override
    public void onDroneServiceInterrupted(String errorMsg) {
        alertUser(errorMsg);
    }

    /**
     * Displays a short notification-like message on the screen for the user to see
     *
     * @param message message to display
     */
    protected void alertUser(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
    }

    /**
     * Safely handles everything needed at start
     */
    @Override
    public void onStart() {
        super.onStart();
        this.controlTower.connect(this);
    }

    /**
     * Safely handles everything when app closes
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        if(this.drone.isConnected()) {
            this.drone.disconnect();
        }
        this.controlTower.unregisterDrone(this.drone);
        this.controlTower.disconnect();
    }
}
