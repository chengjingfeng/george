package io.pijun.george.database;

import androidx.annotation.NonNull;

public class FriendLocation {

    public final long friendId;
    public final double latitude;
    public final double longitude;
    public final long time;
    public final Float accuracy;
    public final Float speed;
    // The person's bearing, in degrees
    public final Float bearing;
    public final MovementType movement;
    public Integer batteryLevel;

    FriendLocation(long id, double lat, double lng, long time, Float accuracy, Float speed, Float bearing, MovementType movement, Integer batteryLevel) {
        this.friendId = id;
        this.latitude = lat;
        this.longitude = lng;
        this.time = time;
        this.accuracy = accuracy;
        this.speed = speed;
        this.bearing = bearing;
        this.movement = movement;
        this.batteryLevel = batteryLevel;
    }

    @Override
    @NonNull
    public String toString() {
        return "FriendLocation{" +
                "friendId=" + friendId +
                ", latitude=" + latitude +
                ", longitude=" + longitude +
                ", time=" + time +
                ", accuracy=" + accuracy +
                ", speed=" + speed +
                ", bearing=" + bearing +
                ", movement='" + movement + '\'' +
                ", batteryLevel=" + batteryLevel +
                '}';
    }

}
