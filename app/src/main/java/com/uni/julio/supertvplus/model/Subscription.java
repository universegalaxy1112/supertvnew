package com.uni.julio.supertvplus.model;

import com.uni.julio.supertvplus.LiveTvApplication;
import com.uni.julio.supertvplus.adapter.LivetvAdapterNew;

public class Subscription {
    private int isActive;
    private int isCancelled;
    private int membership = 0;
    private int isOnHold;
    private int isExpired;
    private String purchaseToken;
    private String expiration_date;
    public String getExpiration_date() {
        return expiration_date;
    }

    public void setExpiration_date(String expiration_date) {
        this.expiration_date = expiration_date;
    }
    public void setIsActive(int isActive) {
        this.isActive = isActive;
    }
    public int getIsActive() {
        return isActive;
    }

    public void setIsCancelled(int isCancelled) {
        this.isCancelled = isCancelled;
    }

    public int getIsCancelled() {
        return isCancelled;
    }

    public void setIsOnHold(int isOnHold) {
        this.isOnHold = isOnHold;
    }

    public int getIsOnHold() {
        return isOnHold;
    }

    public void setIsExpired(int isExpired) {
        this.isExpired = isExpired;
    }

    public int getIsExpired() {
        return isExpired;
    }

    public void setMembership(int membership) {
        this.membership = membership;
    }

    public int getMembership() {
        return membership;
    }

    public void setPurchaseToken(String token) {
        this.purchaseToken = token;
    }

    public String getPurchaseToken() {
        return purchaseToken;
    }

    public boolean showAds() {
        return membership == 0 || isActive == 0 || isExpired == 1 || isCancelled == 1 || isOnHold == 1;
    }

    public boolean hasAccessPremium() {
        return !showAds();
    }

    public boolean hasAccessPlus() {
        return membership == 2 && !showAds();
    }
}
