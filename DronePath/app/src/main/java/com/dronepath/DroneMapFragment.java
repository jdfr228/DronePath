package com.dronepath;

import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;


/**
 * Created by Edwin on 10/21/2017.
 */

public class DroneMapFragment extends SupportMapFragment {

    private static final String TAG = "DroneMapFragment";

    private GoogleMap mMap;
    private GoogleApiClient mClient;
    private Location mLastLocation;
    private final int mDefaultZoom = 18;

    private static final String[] LOCATION_PERMISSIONS = new String[]{
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION,
    };
    private static final int REQUEST_LOCATION_PERMISSIONS = 0;


    @Override
    public void onCreate(Bundle var1) {
        super.onCreate(var1);
        mClient = new GoogleApiClient.Builder(getActivity()).addApi(LocationServices.API)
                .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                    @Override
                    public void onConnected(@Nullable Bundle bundle) {
                        getLocation();
                    }

                    @Override
                    public void onConnectionSuspended(int i) {

                    }
                })
                .build();

        getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap googleMap) {
                mMap = googleMap;
                mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
                if (hasLocationPermission()) {
                    mMap.setMyLocationEnabled(true);
                }
                else {
                    requestPermissions(LOCATION_PERMISSIONS, REQUEST_LOCATION_PERMISSIONS);
                    if (hasLocationPermission())
                        mMap.setMyLocationEnabled(true);
                }

                updateUI();
            }
        });
    }

    @Override
    public void onStart() {
        mClient.connect();
        super.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
        mClient.disconnect();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        switch (requestCode) {
            case REQUEST_LOCATION_PERMISSIONS:
                if (hasLocationPermission()) {
                    mMap.setMyLocationEnabled(true);
                }
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    private boolean hasLocationPermission() {
        int result = ContextCompat
                .checkSelfPermission(getActivity(), LOCATION_PERMISSIONS[0]);
        return result == PackageManager.PERMISSION_GRANTED;
    }

    private void updateUI() {
        if (mMap == null || mLastLocation == null)
            return;

        LatLng currLocation = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());

        mMap.clear();

        CameraUpdate update = CameraUpdateFactory.newLatLngZoom(currLocation, mDefaultZoom);
        mMap.animateCamera(update);
    }

    public void getLocation() {
        if(!mClient.isConnected())
            return;
        LocationRequest request = LocationRequest.create();
        request.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        request.setNumUpdates(1);
        request.setInterval(0);
       try {
           LocationServices.FusedLocationApi
                   .requestLocationUpdates(mClient, request, new LocationListener() {
                       @Override
                       public void onLocationChanged(Location location) {
                           Log.i(TAG, "Location: " + location);
                           mLastLocation = location;
                           updateUI();
                       }
                   });
       }
       catch (SecurityException e){Log.i(TAG, "catch getLocation");}
    }
}
