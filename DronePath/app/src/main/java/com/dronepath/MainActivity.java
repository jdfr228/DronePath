package com.dronepath;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
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
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import com.dronepath.mission.MissionControl;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.Projection;
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
import com.o3dr.services.android.lib.drone.property.State;
import com.o3dr.services.android.lib.drone.property.VehicleMode;
import com.o3dr.services.android.lib.model.SimpleCommandListener;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener,
                    FlightVarsDialogFragment.OnCompleteListener,
                    GPSLocationDialogFragment.OnCompleteListener,
                    View.OnClickListener, DroneListener,
                    TowerListener {

    // Global variables - if another Activity needs to change them, pass them back to the Main Activity
    public double velocity, altitude;
    public double maxVelocity = 18.351;   //
    public double maxAltitude = 17.2;   // TODO- allow the user to change these in a menu
    public String savedLatitude = "";
    public String savedLongitude = "";
    public boolean savedCheckBox = false;

    // Drone state constants
    int droneState = 0;
    static final int USER_CLICKED = 0;
    static final int DRONE_CONNECTED = 1;
    static final int DRONE_DISCONNECTED = 2;
    static final int DRONE_ARMED = 3;

    // Drone Kit stuff
    private ControlTower controlTower;
    private Drone drone;
    private final Handler handler = new Handler();
    Spinner modeSelector;

    // Floating Action Buttons
    private FloatingActionButton menu_fab,edit_fab,place_fab,delete_fab, connect_arm_fab;
    boolean isFabExpanded, isMapDrawable = false;
    private Animation open_fab,close_fab;

    private DroneMapFragment mapFragment;


    // Dialog Listeners
    // FlightVarsDialogFragment.OnCompleteListener implementation (passes variables)
    public void onFlightVarsDialogComplete(double newVelocity, double newAltitude) {
        velocity = newVelocity;
        altitude = newAltitude;
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

        // Save entered Latitude/Longitude and CheckBox state for the next time the Dialog opens
        savedLatitude = Double.toString(latitude);
        savedLongitude = Double.toString(longitude);
        savedCheckBox = isWaypoint;
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mapFragment = (DroneMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
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
        connect_arm_fab = (FloatingActionButton) findViewById(R.id.connect_arm_fab);
        menu_fab.setOnClickListener(this);
        edit_fab.setOnClickListener(this);
        place_fab.setOnClickListener(this);
        delete_fab.setOnClickListener(this);
        connect_arm_fab.setOnClickListener(this);
        open_fab = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.open_fab);
        close_fab = AnimationUtils.loadAnimation(getApplicationContext(),R.anim.close_fab);
//        fab.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//                        .setAction("Action", null).show();
//            }
//        });

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        // Initialize the DroneKit service manager
        this.controlTower = new ControlTower(getApplicationContext());
        // TODO Not sure why there's a context argument here, isn't in the tutorial
        this.drone = new Drone(getApplicationContext());
    }

    @Override
    // TODO- Is there a reason this can't be rolled into onCreate()?
    public void onStart() {
        super.onStart();
        this.controlTower.connect(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        if(this.drone.isConnected()) {
            this.drone.disconnect();
            // TODO (William) Update connected status here
        }
        this.controlTower.unregisterDrone(this.drone);
        this.controlTower.disconnect();
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        switch (id){
            case R.id.menu_fab:
                animateFabButtons();
                break;
            case R.id.edit_fab:
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
            case R.id.connect_arm_fab:
                animateConnectArmFab(USER_CLICKED);
                manageDrone();
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

    @SuppressWarnings("StatementWithEmptyBody")
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
                GPSargs.putBoolean("savedCheckBox", savedCheckBox);
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

    // Logic for the appearance of the connect/arm/disarm button
    public void animateConnectArmFab(int event) {
        // Accessor for swirling loading icon
        ProgressBar loadingIndicator = (ProgressBar) findViewById(R.id.loading_indicator);

        switch (event) {
            case USER_CLICKED:  // Show loading icon
                // Remove any icons
                connect_arm_fab.setImageResource(R.drawable.empty_drawable);

                // Show loading indicator
                loadingIndicator.setVisibility(View.VISIBLE);

                // Make the button unclickable
                connect_arm_fab.setClickable(false);
                break;

            case DRONE_CONNECTED:   // Show arm icon
                // Hide loading indicator
                loadingIndicator.setVisibility(View.INVISIBLE);

                // Change icon
                connect_arm_fab.setImageResource(R.drawable.ic_menu_send);

                // Change button color
                connect_arm_fab.setBackgroundTintList(ColorStateList.valueOf
                        (ContextCompat.getColor(this, android.R.color.holo_green_dark)));

                // Make the button clickable again
                connect_arm_fab.setClickable(true);
                break;

            case DRONE_DISCONNECTED:    // Show connect icon
                loadingIndicator.setVisibility(View.INVISIBLE);
                connect_arm_fab.setImageResource(R.drawable.quantum_ic_bigtop_updates_white_24);
                connect_arm_fab.setBackgroundTintList(ColorStateList.valueOf
                        (ContextCompat.getColor(this, R.color.colorAccent)));
                connect_arm_fab.setClickable(true);
                break;

            case DRONE_ARMED:   // Show cancel/return icon
                loadingIndicator.setVisibility(View.INVISIBLE);
                connect_arm_fab.setImageResource(R.drawable.cast_ic_notification_disconnect);
                connect_arm_fab.setBackgroundTintList(ColorStateList.valueOf
                        (ContextCompat.getColor(this, android.R.color.holo_red_dark)));
                connect_arm_fab.setClickable(true);
                break;

            // case DRONE_RETURNED_HOME:
            //  break;
        }
    }


    // Drone Connectivity stuff TODO- see if this can be moved to clean up MainActivity?
    @Override
    public void onTowerConnected() {
        // TODO (William) Questionable arguments...
        this.controlTower.registerDrone(this.drone, this.handler);
        this.drone.registerDroneListener(this);
    }

    @Override
    public void onTowerDisconnected() {

    }

    @Override
    public void onDroneEvent(String event, Bundle extras) {
        switch (event) {
            case AttributeEvent.STATE_CONNECTED:
                alertUser("Drone Connected");
                //updateConnectedButton(this.drone.isConnected());
                animateConnectArmFab(DRONE_CONNECTED);
                break;

            case AttributeEvent.STATE_DISCONNECTED:
                alertUser("Drone Disconnected");
                //updateConnectedButton(this.drone.isConnected());
                animateConnectArmFab(DRONE_DISCONNECTED);
                break;

            case AttributeEvent.STATE_ARMING:
                alertUser("Arming Drone and taking off...");
                animateConnectArmFab(DRONE_ARMED);
                break;

            // TODO- see if there's an equivalent for STATE_ARMED ???
            // If arming takes a long time, allowing the user to cancel flight could cause
            // problems without this?

            default:
                break;
        }
    }

    protected void alertUser(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onDroneServiceInterrupted(String errorMsg) {

    }

    // Logic for the connect/arm/disarm button
    public void manageDrone() {
        State vehicleState = this.drone.getAttribute(AttributeType.STATE);

        if (!this.drone.isConnected()) {    // Attempt to connect
            alertUser("Connecting to drone...");

            Bundle extraParams = new Bundle();
            extraParams.putInt(ConnectionType.EXTRA_UDP_SERVER_PORT, 14550); // Set default port to 14550

            ConnectionParameter connectionParams = new ConnectionParameter(ConnectionType.TYPE_UDP,
                    extraParams,
                    null);
            this.drone.connect(connectionParams);

        } else {
            if (vehicleState.isFlying()) {  // Attempt to return home


            } else if (!vehicleState.isArmed()) {   // Attempt to arm and take off

                ArrayList<LatLong> waypoints = new ArrayList<LatLong>();
                waypoints.add(new LatLong(37.873000, -122.303202));
                waypoints.add(new LatLong(37.873000, -122.304113));
                waypoints.add(new LatLong(37.873000, -122.305293));

                MissionControl missionControl = new MissionControl(this.getApplicationContext(), drone);
                missionControl.addWaypoints(waypoints);
                missionControl.sendMissionToAPM();
                alertUser("Drone mission sent");

                final VehicleApi vehicleApi = new VehicleApi(drone);
                vehicleApi.arm(true);
                ControlApi.getApi(this.drone).takeoff(20, new SimpleCommandListener() {
                    @Override
                    public void onSuccess() {
                        vehicleApi.setVehicleMode(VehicleMode.COPTER_AUTO);
                    }
                });
            }
        }
    }

    public void onFlightModeSelected(View view) {
        VehicleMode vehicleMode = (VehicleMode) this.modeSelector.getSelectedItem();
        // this.drone.changeVehicleMode(vehicleMode);
    }

}
