package com.dronepath.mission;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;

//import com.dronepath.DronePath; - This doesn't seem to exist? Someone correct me if I'm wrong -Dylan

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.o3dr.android.client.Drone;
import com.o3dr.android.client.apis.MissionApi;
import com.o3dr.services.android.lib.coordinate.LatLong;
import com.o3dr.services.android.lib.coordinate.LatLongAlt;
import com.o3dr.services.android.lib.drone.attribute.AttributeEvent;
import com.o3dr.services.android.lib.drone.attribute.AttributeType;
import com.o3dr.services.android.lib.drone.mission.Mission;
import com.o3dr.services.android.lib.drone.mission.item.MissionItem;
import com.o3dr.services.android.lib.drone.mission.item.command.ReturnToLaunch;
import com.o3dr.services.android.lib.drone.mission.item.command.Takeoff;
import com.o3dr.services.android.lib.drone.mission.item.spatial.Waypoint;
import com.o3dr.services.android.lib.util.Utils;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by wsong on 10/28/17.
 */

public class MissionControl {
    public static final String ACTION_MISSION_PROXY_UPDATE = Utils.PACKAGE_NAME + ".ACTION_MISSION_PROXY_UPDATE";

    private static final IntentFilter eventFilter = new IntentFilter();

    static {
        eventFilter.addAction(AttributeEvent.MISSION_UPDATED);
        eventFilter.addAction(AttributeEvent.MISSION_RECEIVED);
    }

    // Private member variables
    private final Drone drone;
    private final Context context;
    private final MissionApi missionApi;
    private final LocalBroadcastManager lbm;
    private final List<MissionItem> missionItems = new ArrayList<MissionItem>();

    private Mission currentMission;

    public MissionControl(Context context, Drone drone) {
        // TODO Not sure if this works...
        this.drone = drone;
        this.context = context;
        this.missionApi = new MissionApi(drone);

        lbm = LocalBroadcastManager.getInstance(this.context);
    }

    // Adds take off, waypoints, and RTL to a mission
    private Mission generateMission() {
        Mission mission = new Mission();

        Takeoff takeOff = new Takeoff();
        takeOff.setTakeoffAltitude(60D);
        mission.addMissionItem(takeOff);

        if (!missionItems.isEmpty())
            for (MissionItem missionItem : missionItems)
                mission.addMissionItem(missionItem);

        ReturnToLaunch rtl = new ReturnToLaunch();
        mission.addMissionItem(rtl);

        return mission;
    }

    public void sendMissionToAPM() {
        missionApi.setMission(generateMission(), true);
    }

    public void addWaypoints(List<LatLong> points) {
        float alt = 60F;

        for (LatLong point : points) {
            Waypoint waypoint = new Waypoint();
            waypoint.setCoordinate(new LatLongAlt(point.getLatitude(), point.getLongitude(), alt));
            missionItems.add(waypoint);
        }

        notifyMissionUpdate();
    }

    public void notifyMissionUpdate() {
        currentMission = generateMission();
        lbm.sendBroadcast(new Intent(ACTION_MISSION_PROXY_UPDATE));
    }
}
