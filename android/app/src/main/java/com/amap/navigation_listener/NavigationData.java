package com.amap.navigation_listener;

public class NavigationData {
    private String roadName;
    private String action;
    private int distance;
    private int remainingTime;
    private int speed;
    private boolean isActive;

    public NavigationData() {
        this.roadName = "未开始导航";
        this.action = "等待导航开始";
        this.distance = 0;
        this.remainingTime = 0;
        this.speed = 0;
        this.isActive = false;
    }

    public NavigationData(String roadName, String action, int distance, int remainingTime, int speed, boolean isActive) {
        this.roadName = roadName;
        this.action = action;
        this.distance = distance;
        this.remainingTime = remainingTime;
        this.speed = speed;
        this.isActive = isActive;
    }

    public String getRoadName() {
        return roadName;
    }

    public void setRoadName(String roadName) {
        this.roadName = roadName;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public int getDistance() {
        return distance;
    }

    public void setDistance(int distance) {
        this.distance = distance;
    }

    public int getRemainingTime() {
        return remainingTime;
    }

    public void setRemainingTime(int remainingTime) {
        this.remainingTime = remainingTime;
    }

    public int getSpeed() {
        return speed;
    }

    public void setSpeed(int speed) {
        this.speed = speed;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    @Override
    public String toString() {
        return "NavigationData{" +
                "roadName='" + roadName + '\'' +
                ", action='" + action + '\'' +
                ", distance=" + distance +
                ", remainingTime=" + remainingTime +
                ", speed=" + speed +
                ", isActive=" + isActive +
                '}';
    }
}