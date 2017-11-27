package com.dronepath.mission;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

import com.dronepath.MainActivity;
import com.o3dr.android.client.Drone;
import com.o3dr.android.client.apis.MissionApi;
import com.o3dr.services.android.lib.coordinate.LatLong;
import com.o3dr.services.android.lib.coordinate.LatLongAlt;
import com.o3dr.services.android.lib.drone.attribute.AttributeType;
import com.o3dr.services.android.lib.drone.mission.Mission;
import com.o3dr.services.android.lib.drone.mission.item.MissionItem;
import com.o3dr.services.android.lib.drone.mission.item.command.ReturnToLaunch;
import com.o3dr.services.android.lib.drone.mission.item.command.Takeoff;
import com.o3dr.services.android.lib.drone.mission.item.spatial.Waypoint;
import com.o3dr.services.android.lib.drone.property.Speed;
import com.o3dr.services.android.lib.util.Utils;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by wsong on 10/28/17.
 */

public class MissionControl {
    public static final String ACTION_MISSION_PROXY_UPDATE = Utils.PACKAGE_NAME + ".ACTION_MISSION_PROXY_UPDATE";

    // Private member variables
    private final Drone drone;
    private final Context context;
    private final MissionApi missionApi;
    private final LocalBroadcastManager lbm;
    private final List<MissionItem> missionItems = new ArrayList<MissionItem>();
    private MainActivity activity;

    public MissionControl(Context context, Drone drone) {
        this.drone = drone;
        this.context = context;
        if (context instanceof Activity) {
            activity = (MainActivity) context;
        }
        this.missionApi = new MissionApi(drone);

        lbm = LocalBroadcastManager.getInstance(this.context);
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

        // Set drone flight speed
        // TODO- this seems to currently be ignored
        Speed droneSpeed = drone.getAttribute(AttributeType.SPEED);
        droneSpeed.setAirSpeed(activity.getVelocity());

        return mission;
    }

    /**
     * Calls generateMission() to send a planned mission with waypoints to the drone
     */
    public void sendMissionToAPM() {
        missionApi.setMission(generateMission(), true);
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

    public void notifyMissionUpdate() {
        lbm.sendBroadcast(new Intent(ACTION_MISSION_PROXY_UPDATE));
    }
}
