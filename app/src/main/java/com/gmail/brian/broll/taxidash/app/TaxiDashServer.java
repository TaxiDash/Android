package com.gmail.brian.broll.taxidash.app;

/**
 * Created by Brian Broll on 6/24/14.
 *
 * This is a TaxiDash object that the device has
 * connected to.
 */
public class TaxiDashServer {
    private String city = null;
    private String state = null;
    private String address = null;

    public TaxiDashServer(String city, String state, String address){
        this.city = city;
        this.state = state;
        this.address = address;
    }

    public String getCity(){
        return this.city;
    }

    public String getState(){
        return this.state;
    }

    public String getAddress() {
        return this.address;
    }
}
