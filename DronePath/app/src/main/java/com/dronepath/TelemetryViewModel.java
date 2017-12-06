package com.dronepath;

import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.ViewModel;

class TelemetryViewModel extends ViewModel {
    private MutableLiveData<String> latitude, longitude, velocity, altitude, vehicleMode, battery;

    // Getters for LiveData
    MutableLiveData<String> getLatitude() {
        if (latitude == null) {
            latitude = new MutableLiveData<>();
        }
        return latitude;
    }
    MutableLiveData<String> getLongitude() {
        if (longitude == null) {
            longitude = new MutableLiveData<>();
        }
        return longitude;
    }
    MutableLiveData<String> getVelocity() {
        if (velocity == null) {
            velocity = new MutableLiveData<>();
        }
        return velocity;
    }
    MutableLiveData<String> getAltitude() {
        if (altitude == null) {
            altitude = new MutableLiveData<>();
        }
        return altitude;
    }
    MutableLiveData<String> getVehicleMode() {
        if (vehicleMode == null) {
            vehicleMode = new MutableLiveData<>();
        }
        return vehicleMode;
    }
    MutableLiveData<String> getBattery() {
        if (battery == null) {
            battery = new MutableLiveData<>();
        }
        return battery;
    }
}
