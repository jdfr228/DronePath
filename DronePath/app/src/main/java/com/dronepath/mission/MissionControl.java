package com.dronepath.mission;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.dronepath.MainActivity;
import com.o3dr.android.client.Drone;
import com.o3dr.android.client.apis.MissionApi;
import com.o3dr.services.android.lib.coordinate.LatLong;
import com.o3dr.services.android.lib.coordinate.LatLongAlt;
import com.o3dr.services.android.lib.drone.mission.Mission;
import com.o3dr.services.android.lib.drone.mission.item.MissionItem;
import com.o3dr.services.android.lib.drone.mission.item.command.ReturnToLaunch;
import com.o3dr.services.android.lib.drone.mission.item.command.Takeoff;
import com.o3dr.services.android.lib.drone.mission.item.spatial.Waypoint;
import com.o3dr.services.android.lib.model.AbstractCommandListener;
import com.o3dr.services.android.lib.util.Utils;

import java.util.ArrayList;
import java.util.List;

public class MissionControl {
    private static final String ACTION_MISSION_PROXY_UPDATE = Utils.PACKAGE_NAME + ".ACTION_MISSION_PROXY_UPDATE";

    // Private member variables
    private final Drone drone;
    private final MissionApi missionApi;
    private final LocalBroadcastManager lbm;
    public final List<MissionItem> missionItems = new ArrayList<MissionItem>();
    private MainActivity activity;

    public MissionControl(Context context, Drone drone) {
        this.drone = drone;
        if (context instanceof Activity) {
            activity = (MainActivity) context;
        }
        this.missionApi = new MissionApi(drone);

        lbm = LocalBroadcastManager.getInstance(activity);
    }

    /**
     * Generate a mission based on waypoints added through addWaypoints()
     *
     * @return A Mission object to be send directly to the drone
     */
    private Mission generateMission() {
        Mission mission = new Mission();

        Takeoff takeOff = new Takeoff();
        takeOff.setTakeoffAltitude(activity.getAltitude()); // Gets from altitude slider
        mission.addMissionItem(takeOff);

        if (!missionItems.isEmpty())
            for (MissionItem missionItem : missionItems)
                mission.addMissionItem(missionItem);

        ReturnToLaunch rtl = new ReturnToLaunch();
        mission.addMissionItem(rtl);

        return mission;
    }

    /**
     * Calls generateMission() to send a planned mission with waypoints to the drone
     */
    public void sendMissionToAPM() {
        // Set drone flight speed
        missionApi.setMissionSpeed((float) activity.getVelocity(), new AbstractCommandListener() {
            @Override
            public void onSuccess() {
                Log.d("velocity", "Drone velocity set successfully");
            }

            @Override
            public void onError(int executionError) {
                activity.alertUser("Error setting drone velocity: " + executionError);
            }

            @Override
            public void onTimeout() {
                activity.alertUser("Timeout when setting drone velocity");
            }
        });

        // Generate and send mission to drone
        missionApi.setMission(generateMission(), true);
    }

    public void updateVelocity(float newVelocity, final boolean inFlight) {
        missionApi.setMissionSpeed(newVelocity, new AbstractCommandListener() {
            @Override
            public void onSuccess() {
                if (inFlight) {
                    activity.alertUser("Drone velocity updated");
                }
            }

            @Override
            public void onError(int executionError) {
                activity.alertUser("Error setting drone velocity: " + executionError);
            }

            @Override
            public void onTimeout() {
                activity.alertUser("Timeout when setting drone velocity");
            }
        });
    }

    /**
     * Adds a list of latittude-longitude points so they can be part of a mission later
     *
     * @param points LatLong objects containing mission waypoints
     */
    public void addWaypoints(List<LatLong> points) {
        double alt = activity.getAltitude();

        // Clear any previously saved waypoints
        missionItems.clear();

        for (LatLong point : points) {
            Waypoint waypoint = new Waypoint();
            waypoint.setCoordinate(new LatLongAlt(point.getLatitude(), point.getLongitude(), alt));
            missionItems.add(waypoint);
        }

        notifyMissionUpdate();
    }

    // Checks to see if the next waypoint is the final one on the mission or not
    public boolean isFinalWaypoint(int nextWaypoint) {
        return (nextWaypoint == missionItems.size() + 1);   // Simplified if statement
    }

    private void notifyMissionUpdate() {
        lbm.sendBroadcast(new Intent(ACTION_MISSION_PROXY_UPDATE));
    }
}
