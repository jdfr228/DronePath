package com.dronepath;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.util.Log;
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

import com.google.android.gms.common.ConnectionResult;
import com.o3dr.android.client.ControlTower;
import com.o3dr.android.client.Drone;
import com.o3dr.android.client.interfaces.DroneListener;
import com.o3dr.android.client.interfaces.TowerListener;
import com.o3dr.services.android.lib.drone.property.Type;

import java.util.ResourceBundle;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener,
                    FlightVarsDialogFragment.OnCompleteListener,
                       TowerListener, DroneListener {

    // Global variables - if another Activity needs to change them, pass them back to the Main Activity
    public double velocity;
    public double altitude;
    public double maxVelocity = 100.0;  // Stored at 10x the expected value unless we stop using
                                        //      sliders to set velocity/altitude in the GUI
    public double maxAltitude = 100.0;
    public double gpsAddress;

    // TODO - since the variables are encapsulated in MainActivity, setters may not be needed?
    public void setVelocity(double newVelocity) {
        velocity = newVelocity;
    }
    public void setAltitude(double newAltitude) {
        altitude = newAltitude;
    }

    // Drone Kit Stuff
    private ControlTower controlTower;
    private Drone drone;
    private int droneType = Type.TYPE_UNKNOWN;
    private final Handler handler = new Handler();

    // FlightVarsDialogFragment.OnCompleteListener implementation (passes variables)
    public void onComplete(double velocity, double altitude) {
        setVelocity(velocity);
        setAltitude(altitude);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        DroneMapFragment mapFragment = (DroneMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        // Initialize the DroneKit service manager
        this.controlTower = new ControlTower(getApplicationContext());
        this.drone = new Drone();
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

        if (id == R.id.nav_gps) {
            // Create an instance of the GPS Input dialog
            DialogFragment gpsDialog = new GPSLocationDialogFragment();

            // Display the dialog to the user
            gpsDialog.show(getFragmentManager(), "GPSDialog");

        } else if (id == R.id.nav_flight_vars) {
            DialogFragment flightVarsDialog = new FlightVarsDialogFragment();
            flightVarsDialog.show(getFragmentManager(), "FlightVarsDialog");

        } else if (id == R.id.nav_start) {
            // TODO Handle starting the drone flight
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
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

    }

    @Override
    public void onDroneConnectionFailed(ConnectionResult result) {

    }

    @Override
    public void onDroneServiceInterrupted(String errorMsg) {

    }
}
