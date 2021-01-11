package com.nta.lc_server.model;

public class RestaurantLocationModel {
    private double lat, lng;

    public RestaurantLocationModel() {
    }

    public RestaurantLocationModel(double lat, double lng) {
        this.lat = lat;
        this.lng = lng;
    }

    public double getLat() {
        return lat;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public double getLng() {
        return lng;
    }

    public void setLng(double lng) {
        this.lng = lng;
    }
}
