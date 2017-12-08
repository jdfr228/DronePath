package com.dronepath;

import android.arch.lifecycle.Observer;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.widget.TextView;

public class TelemetryActivity extends FragmentActivity {

    // TextView references
    TextView latitudeTextView, longitudeTextView, velocityTextView, altitudeTextView,
            vehicleModeTextView, batteryTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_telemetry);

        // References to TextViews in activity_telemetry.xml
        latitudeTextView = (TextView) findViewById(R.id.telemetry_latitude);
        longitudeTextView = (TextView) findViewById(R.id.telemetry_longitude);
        velocityTextView = (TextView) findViewById(R.id.telemetry_velocity);
        altitudeTextView = (TextView) findViewById(R.id.telemetry_altitude);
        vehicleModeTextView = (TextView) findViewById(R.id.telemetry_vehicle_mode);
        batteryTextView = (TextView) findViewById(R.id.telemetry_battery);

        // Set up observers for when the MutableLiveData is updated by the DroneHandler
        final Observer<String> latitudeObserver = new Observer<String>() {
            @Override
            public void onChanged(@Nullable String newVal) {
                latitudeTextView.setText(newVal);
            }
        };
        final Observer<String> longitudeObserver = new Observer<String>() {
            @Override
            public void onChanged(@Nullable String newVal) {
                longitudeTextView.setText(newVal);
            }
        };
        final Observer<String> velocityObserver = new Observer<String>() {
            @Override
            public void onChanged(@Nullable String newVal) {
                velocityTextView.setText(newVal);
            }
        };
        final Observer<String> altitudeObserver = new Observer<String>() {
            @Override
            public void onChanged(@Nullable String newVal) {
                altitudeTextView.setText(newVal);
            }
        };
        final Observer<String> vehicleModeObserver = new Observer<String>() {
            @Override
            public void onChanged(@Nullable String newVal) {
                vehicleModeTextView.setText(newVal);
            }
        };
        final Observer<String> batteryObserver = new Observer<String>() {
            @Override
            public void onChanged(@Nullable String newVal) {
                batteryTextView.setText(newVal);
            }
        };

        // Get the TelemetryViewModel associated with the DroneHandlerFragment
        TelemetryViewModel mModel = DroneHandlerFragment.getModel();

        // Register the observers
        mModel.getLatitude().observe(this, latitudeObserver);
        mModel.getLongitude().observe(this, longitudeObserver);
        mModel.getVelocity().observe(this, velocityObserver);
        mModel.getAltitude().observe(this, altitudeObserver);
        mModel.getVehicleMode().observe(this, vehicleModeObserver);
        mModel.getBattery().observe(this, batteryObserver);
    }
}
