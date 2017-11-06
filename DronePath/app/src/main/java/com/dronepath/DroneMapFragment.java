package com.dronepath;

import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

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
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.o3dr.services.android.lib.coordinate.LatLong;
import com.o3dr.services.android.lib.util.MathUtils;

import java.util.ArrayList;
import java.util.List;


/**
 * Created by Edwin on 10/21/2017.
 */

public class DroneMapFragment extends SupportMapFragment implements GoogleMap.OnMarkerDragListener,OnMapReadyCallback{

    private static final String TAG = "DroneMapFragment";
    private View mOriginalView;
    private DroneMapWrapper mDroneMapWrapper;

    private GoogleMap mMap;
    private GoogleApiClient mClient;
    private Location mLastLocation;
    private final int mDefaultZoom = 18;
    private ArrayList<LatLng> latLngPoints = new ArrayList<LatLng>();
    private Polyline polypath;
    private ArrayList<Marker> markerArray = new ArrayList<Marker>();
    private boolean spline_complete;

    private static final String[] LOCATION_PERMISSIONS = new String[]{
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION,
    };
    private static final int REQUEST_LOCATION_PERMISSIONS = 0;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mOriginalView = super.onCreateView(inflater, container, savedInstanceState);
        mDroneMapWrapper = new DroneMapWrapper(getActivity());
        mDroneMapWrapper.addView(mOriginalView);
        spline_complete = false;

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

        getMapAsync(this);
        return mDroneMapWrapper;
    }

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
        mMap.setOnMarkerDragListener(this);

        updateUI();
    }

    @Override
    public void onStart() {
        mClient.connect();
        super.onStart();
    }

    @Override
    // If rotating phone it stops and restarts. Need to save all data and reapply on start.
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

    @Override
    public View getView() {
        return mOriginalView;
    }

    public GoogleMap getMap(){
        return mMap;
    }

    public void addPoint (LatLng point){
        latLngPoints.add(point);
        if (polypath == null) {
            PolylineOptions flightPathOptions = new PolylineOptions();
            flightPathOptions.clickable(false);
            polypath = getMap().addPolyline(flightPathOptions);
        }
        List<LatLng> oldpolypath = polypath.getPoints();
        oldpolypath.add(point);
        polypath.setPoints(oldpolypath);

    }

    public List<LatLong> convertToLatLong(List<LatLng> points){
        List<LatLong> new_points = new ArrayList<LatLong>();
        for (LatLng point : points){
            new_points.add(new LatLong(point.latitude, point.longitude));
        }

        return new_points;
    }

    public List<LatLng> convertToLatLng(List<LatLong> points){
        List<LatLng> new_points = new ArrayList<LatLng>();
        for (LatLong point : points){
            new_points.add(new LatLng(point.getLatitude(), point.getLongitude()));
        }

        return new_points;
    }

    public void convertToSpline(){
        if (isSplineComplete() || polypath == null)
            return;
        List<LatLong> new_points = MathUtils.SplinePath.process(convertToLatLong(polypath.getPoints()));
        new_points = MathUtils.simplify(new_points, .0001);
        //List<LatLong> new_points = convertToLatLong(polypath.getPoints());
        //new_points = MathUtils.SplinePath.process(new_points);
        polypath.setPoints(convertToLatLng(new_points));
        for (LatLng point : polypath.getPoints()){
            Marker m = getMap().addMarker(new MarkerOptions().position(point));
            m.setDraggable(true);
            m.setTitle("Point: " + point.latitude + "," + point.longitude);
            markerArray.add(m);
        }
        spline_complete = true;
    }

    public boolean isSplineComplete(){
        return spline_complete;
    }

    public List<LatLong> getLatLongWaypoints(){
        if (polypath == null) {
            return null;
        } else {
            return convertToLatLong(polypath.getPoints());
        }
    }

    public void clearPoints(){
        markerArray.clear();
        polypath.remove();
        polypath = null;
        getMap().clear();
        spline_complete = false;
    }


    public void setOnDragListener(DroneMapWrapper.OnDragListener onDragListener) {
        mDroneMapWrapper.setOnDragListener(onDragListener);
    }

    @Override
    public void onMarkerDragStart(Marker marker) {
        getMap().getUiSettings().setScrollGesturesEnabled(false);
    }

    @Override
    public void onMarkerDrag(Marker marker) {
        ArrayList<LatLng> points = new ArrayList<>();
        for (Marker m: markerArray){
            m.setTitle("Point: " + m.getPosition().latitude + "," + m.getPosition().longitude);
            points.add(m.getPosition());
        }
        polypath.setPoints(points);
    }

    @Override
    public void onMarkerDragEnd(Marker marker) {
        getMap().getUiSettings().setScrollGesturesEnabled(true);
    }
}
