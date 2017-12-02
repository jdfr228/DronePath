package com.dronepath;

import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.Nullable;
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
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.o3dr.services.android.lib.coordinate.LatLong;
import com.o3dr.services.android.lib.util.MathUtils;

import java.util.ArrayList;
import java.util.List;


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
    private Marker droneMarker = null;

    private static final String[] LOCATION_PERMISSIONS = new String[]{
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION,
    };
    private static final int REQUEST_LOCATION_PERMISSIONS = 0;


    // Fragment Lifecycle methods (in the order they would be called)
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Don't destroy the fragment on a screen rotation
        setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Add wrapper view over map so that a drag listener can be implemented
        mOriginalView = super.onCreateView(inflater, container, savedInstanceState);
        mDroneMapWrapper = new DroneMapWrapper(getActivity());
        mDroneMapWrapper.addView(mOriginalView);
        spline_complete = false;

        // Create GoogleApiClient instance in order to get location data
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

        // Get map and set location as enabled
        getMapAsync(this);
        return mDroneMapWrapper;
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
    public void onDestroy() {
        super.onDestroy();

        // clean up some potential memory leaks in case the fragment is killed in an odd way
        mMap.setOnMarkerDragListener(null);
        mMap = null;
        mClient = null;
        mDroneMapWrapper = null;
        mOriginalView = null;
    }


    // Other listeners
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
        // Set location as enabled
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
    // Request permissions from user
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


    // Helper methods
    // Checks if location permissions are allowed
    private boolean hasLocationPermission() {
        int result = ContextCompat
                .checkSelfPermission(getActivity(), LOCATION_PERMISSIONS[0]);
        return result == PackageManager.PERMISSION_GRANTED;
    }

    // Center map on current position
    private void updateUI() {
        if (mMap == null || mLastLocation == null)
            return;

        LatLng currLocation = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());
        CameraUpdate update = CameraUpdateFactory.newLatLngZoom(currLocation, mDefaultZoom);
        mMap.animateCamera(update);
    }

    // Get last location update
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

    // Return map
    public GoogleMap getMap(){
        return mMap;
    }

    // Get default zoom level
    public int getDefaultZoom() { return mDefaultZoom; }

    // Add a point to the polyline path
    public void addPoint (LatLng point){
        latLngPoints.add(point);
        // Initialize the polyline if it hasn't been created
        if (polypath == null) {
            PolylineOptions flightPathOptions = new PolylineOptions();
            flightPathOptions.clickable(false);
            polypath = getMap().addPolyline(flightPathOptions);
        }
        List<LatLng> oldpolypath = polypath.getPoints();
        oldpolypath.add(point);
        polypath.setPoints(oldpolypath);

        // If a point is added manually after a path has been set,
        // it adds the new point to the path and recreates the waypoints
        if (isSplineComplete()){
            spline_complete = false;
            convertToSpline();
        }
    }

    // Convert from Google Map API Latitude/Longitude to DroneKit API Latitude/Longitude
    public List<LatLong> convertToLatLong(List<LatLng> points){
        List<LatLong> new_points = new ArrayList<LatLong>();
        for (LatLng point : points){
            new_points.add(new LatLong(point.latitude, point.longitude));
        }

        return new_points;
    }

    // Convert from DroneKit API Latitude/Longitude to Google Map API Latitude/Longitude
    public List<LatLng> convertToLatLng(List<LatLong> points){
        List<LatLng> new_points = new ArrayList<LatLng>();
        for (LatLong point : points){
            new_points.add(new LatLng(point.getLatitude(), point.getLongitude()));
        }

        return new_points;
    }

    // Convert the polyline to a smaller set of waypoints that the drone can use
    public void convertToSpline(){
        if (isSplineComplete() || polypath == null)
            return;
        //List<LatLong> new_points = MathUtils.SplinePath.process(convertToLatLong(polypath.getPoints()));
        List<LatLong> new_points = convertToLatLong(polypath.getPoints());
        new_points = MathUtils.simplify(new_points, .0001);
        polypath.setPoints(convertToLatLng(new_points));
        for (LatLng point : polypath.getPoints()){
            Marker m = getMap().addMarker(new MarkerOptions().position(point));
            m.setDraggable(true);
            m.setTitle(String.format("Point: %.4f , %.4f", point.latitude, point.longitude));
            markerArray.add(m);
        }
        spline_complete = true;
    }

    public boolean isSplineComplete(){
        return spline_complete;
    }

    // Return waypoints in the DroneKit API format
    public List<LatLong> getLatLongWaypoints(){
        if (polypath == null) {
            return null;
        } else {
            return convertToLatLong(polypath.getPoints());
        }
    }

    // Clears map of points and markers
    public void clearPoints(){
        markerArray.clear();
        if (polypath != null)
            polypath.remove();
        polypath = null;
        getMap().clear();
        if (droneMarker != null)
            onDroneConnected(new LatLong(droneMarker.getPosition().latitude, droneMarker.getPosition().longitude));
        spline_complete = false;
    }

    // Clear just the drone map marker
    public void clearDronePoint() {
        // TODO- *properly* remove just the drone map marker
        if (droneMarker != null) {
            droneMarker.setVisible(false);
        }
    }

    public void setOnDragListener(DroneMapWrapper.OnDragListener onDragListener) {
        mDroneMapWrapper.setOnDragListener(onDragListener);
    }


    // Drag listeners
    @Override
    // Stop map from moving so waypoint can be dragged
    public void onMarkerDragStart(Marker marker) {
        getMap().getUiSettings().setScrollGesturesEnabled(false);
    }

    @Override
    // Move marker on screen
    // Update the point text
    // Redo polyline
    public void onMarkerDrag(Marker marker) {
        ArrayList<LatLng> points = new ArrayList<>();
        for (Marker m: markerArray){
            m.setTitle(String.format("Point: %.4f , %.4f", m.getPosition().latitude, m.getPosition().longitude));
            points.add(m.getPosition());
        }
        polypath.setPoints(points);
    }

    @Override
    // Allow map to move again once marker has already been dragged
    public void onMarkerDragEnd(Marker marker) {
        getMap().getUiSettings().setScrollGesturesEnabled(true);
    }


    // Drone connection listeners
    // Create marker for drone position
    public void onDroneConnected(LatLong drone){
        MarkerOptions markerOptions = new MarkerOptions();

        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE));
        markerOptions.draggable(false);
        markerOptions.position(new LatLng(drone.getLatitude(), drone.getLongitude()));

        droneMarker = getMap().addMarker(markerOptions);
        droneMarker.setVisible(true);
    }

    // Update drone marker position
    public void onDroneGPSUpdated(LatLong drone){
        if (droneMarker != null)
            droneMarker.setPosition(new LatLng(drone.getLatitude(), drone.getLongitude()));
    }
}