package com.dronepath;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Point;
import android.os.Bundle;
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
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.Projection;
import com.google.android.gms.maps.model.LatLng;
import com.o3dr.services.android.lib.coordinate.LatLong;

import java.util.List;

import static com.dronepath.DroneHandler.USER_CLICKED;
import static com.dronepath.DroneHandler.DRONE_CONNECTED;
import static com.dronepath.DroneHandler.DRONE_DISCONNECTED;
import static com.dronepath.DroneHandler.DRONE_ARMED;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener,
                    FlightVarsDialogFragment.OnCompleteListener,
                    GPSLocationDialogFragment.OnCompleteListener,
                    View.OnClickListener {

    // Global variables - if another Activity needs to change them, pass them back to the Main Activity
    public double velocity, altitude;
    public double maxVelocity = 10.0;   //
    public double maxAltitude = 50.0;   // TODO- allow the user to change these in a menu
    public String savedLatitude = "";
    public String savedLongitude = "";
    public boolean savedCheckBox = false;

    // Floating Action Buttons
    private FloatingActionButton menu_fab,edit_fab,place_fab,delete_fab, connect_arm_fab;
    boolean isFabExpanded, isMapDrawable = false;
    private Animation open_fab,close_fab;

    public DroneMapFragment mapFragment;    // TODO- you could make an argument for this being private...
    private DroneHandler droneHandler;

    private Toast toast;

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

        // Create map fragment
        mapFragment = (DroneMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.setRetainInstance(true); // Makes sure map is saved when orientation is changed

        // Set the drag listener for the map
        // Only adds points when the drawing is enabled
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

        // Set up menu FABs
        menu_fab = (FloatingActionButton) findViewById(R.id.menu_fab);
        edit_fab = (FloatingActionButton) findViewById(R.id.edit_fab);
        edit_fab.setRippleColor(getResources().getColor(R.color.colorPrimary));
        place_fab = (FloatingActionButton) findViewById(R.id.place_fab);
        place_fab.setRippleColor(getResources().getColor(R.color.colorPrimary));
        delete_fab = (FloatingActionButton) findViewById(R.id.delete_fab);
        connect_arm_fab = (FloatingActionButton) findViewById(R.id.connect_arm_fab);
        delete_fab.setRippleColor(getResources().getColor(R.color.colorPrimary));
        menu_fab.setOnClickListener(this);
        edit_fab.setOnClickListener(this);
        place_fab.setOnClickListener(this);
        delete_fab.setOnClickListener(this);
        connect_arm_fab.setOnClickListener(this);
        open_fab = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.open_fab);
        close_fab = AnimationUtils.loadAnimation(getApplicationContext(),R.anim.close_fab);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        droneHandler = new DroneHandler(this);

        toast = Toast.makeText(getApplicationContext(), "", Toast.LENGTH_LONG);
    }

    @Override
    // Handles input when any FAB button is pressed
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
                    edit_fab.setBackgroundTintList(getResources().getColorStateList(R.color.colorPrimary));
                    mapFragment.getMap().getUiSettings().setScrollGesturesEnabled(false);
                }
                else {
                    edit_fab.setBackgroundTintList(getResources().getColorStateList(R.color.colorAccent));
                    mapFragment.getMap().getUiSettings().setScrollGesturesEnabled(true);
                }
                break;
            case R.id.place_fab:
                mapFragment.convertToSpline();
                isMapDrawable = false;
                mapFragment.getMap().getUiSettings().setScrollGesturesEnabled(true);
                edit_fab.setBackgroundTintList(getResources().getColorStateList(R.color.colorAccent));
                break;
            case R.id.delete_fab:
                if (mapFragment != null) {
                    mapFragment.clearPoints();
                    isMapDrawable = false;
                    mapFragment.getMap().getUiSettings().setScrollGesturesEnabled(true);
                    edit_fab.setBackgroundTintList(getResources().getColorStateList(R.color.colorAccent));
                }
                break;
            case R.id.connect_arm_fab:
                animateConnectArmFab(USER_CLICKED);     // Show loading icon & disable add. clicking

                // Determine action for the button based on the state of the drone
                if (droneHandler.getDroneState() == DRONE_DISCONNECTED) { droneHandler.connectToDrone(); }
                else if (droneHandler.getDroneState() == DRONE_CONNECTED) { droneHandler.startFlight(); }
                else if (droneHandler.getDroneState() == DRONE_ARMED) { droneHandler.returnHome(); }

                break;
        }
    }

    @Override
    // Handle when the user presses the Android back button
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
        // Inflate the menu; this adds items to the action bar if they are present.
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

            case R.id.nav_disconnect:
                if (droneHandler.isDroneConnected()) {
                    droneHandler.disconnectDrone();
                } else {
                    alertUser("No drone is connected");
                }
                break;
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    // Animate the menu buttons on map screen to open and close
    public void animateFabButtons() {
        if (isFabExpanded){
            isFabExpanded = false;
            isMapDrawable = false;
            mapFragment.getMap().getUiSettings().setScrollGesturesEnabled(true);
            edit_fab.setBackgroundTintList(getResources().getColorStateList(R.color.colorAccent));
            menu_fab.setImageResource(R.mipmap.ic_more_vert_white_24dp);
            edit_fab.startAnimation(close_fab);
            place_fab.startAnimation(close_fab);
            delete_fab.startAnimation(close_fab);
            edit_fab.setClickable(false);
            place_fab.setClickable(false);
            delete_fab.setClickable(false);
        }
        else {
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
    /**
        Animation logic (which is in the animateConnectArmFab method) should be sound, while the drone
        logic, as I mention below, needs to be replaced.

        The thought process here is that the USER_CLICKED case is triggered from the onClick function
        above, and simply shows the loading icon and disables the user from pressing the button again
        so they don't start throwing out arm commands before the drone has connected or anything.

        The other cases, which will hide the loading icon and change the button icon/color to indicate
        its new functionality, were intended to be triggered by the main drone event listener, hence
        the case names like DRONE_CONNECTED, DRONE_ARMED, etc.

        So basically the button would wait until whatever drone action it triggered was completed
        successfully, and then change its appearance.

        Currently called in onClick above- animateConnectArmFab(USER_CLICKED);
            and onDroneEvent- animateConnectArmFab(DRONE_CONNECTED) (DRONE_DISCONNECTED) etc.

     */
    public void animateConnectArmFab(int event) {
        // Accessor for swirling loading icon
        ProgressBar loadingIndicator = (ProgressBar) findViewById(R.id.loading_indicator);

        switch (event) {
            case USER_CLICKED:  // Show loading icon
                Log.d("myTag", "User clicked button");
                // Remove any icons
                connect_arm_fab.setImageResource(R.drawable.empty_drawable);

                // Show loading indicator
                loadingIndicator.setVisibility(View.VISIBLE);

                // Make the button unclickable
                connect_arm_fab.setClickable(false);
                break;

            case DRONE_CONNECTED:   // Show arm icon
                Log.d("myTag", "Drone icon updated to arm icon");
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
                Log.d("myTag", "Drone icon updated to connect icon");
                loadingIndicator.setVisibility(View.INVISIBLE);
                connect_arm_fab.setImageResource(R.drawable.quantum_ic_bigtop_updates_white_24);
                connect_arm_fab.setBackgroundTintList(ColorStateList.valueOf
                        (ContextCompat.getColor(this, R.color.colorAccent)));
                connect_arm_fab.setClickable(true);
                break;

            case DRONE_ARMED:   // Show cancel/return icon
                Log.d("myTag", "Drone icon updated to return/cancel icon");
                loadingIndicator.setVisibility(View.INVISIBLE);
                connect_arm_fab.setImageResource(R.drawable.cast_ic_notification_disconnect);
                connect_arm_fab.setBackgroundTintList(ColorStateList.valueOf
                        (ContextCompat.getColor(this, android.R.color.holo_red_dark)));
                connect_arm_fab.setClickable(true);
                break;
        }
    }

    /**
     * Displays a short notification-like message on the screen for the user to see
     *
     * @param message message to display
     */
    public void alertUser(String message) {
        toast.setText(message);
        toast.show();
    }

    /**
     * Safely handles everything needed at start
     */
    @Override
    public void onStart() {
        super.onStart();
        droneHandler.controlTowerConnect();
    }

    /**
     * Safely handles everything when app closes
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (droneHandler.isDroneConnected()) {
            droneHandler.disconnectDrone();
        }
        droneHandler.controlTowerDisconnect();
    }
}
