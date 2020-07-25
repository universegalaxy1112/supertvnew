package com.uni.julio.supertvplus.model;

public class User {

    private String name;
    private String email;
    private Subscription subscription;
    private String device;
    private String version;
    private String user_agent;
    private String password;
    private String deviceId;
    private int adultos = 0;
    private int device_num = 0;

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Subscription getSubscription() {
        return subscription;
    }

    public void setSubscription(Subscription subscription) {
        this.subscription = subscription;
    }

    public String getDevice() {
        return device;
    }

    public void setDevice(String device) {
        this.device = device;
    }

    public String getUser_agent() {
        return user_agent;
    }

    public void setUser_agent(String user_agent) {
        this.user_agent = user_agent;
    }


    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public void setAdultos(int adultos) {
        this.adultos = adultos;
    }

    public int getAdultos() {
        return this.adultos;
    }

    public void setDevice_num(int num) {
        this.device_num = num;
    }

    public int getDevice_num() {
        return this.device_num;
    }

}
