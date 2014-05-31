package com.gmail.brian.broll.taxidash.app;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by Brian Broll on 5/18/14.
 */
public class Ride implements Parcelable {
    /*
     * This class contains the ride info for the trip.
     * It may or may not be submitted to the server.
     */
    //Location info
    private double startLatitude = 0;
    private double startLongitude = 0;
    private double endLatitude = 0;
    private double endLongitude = 0;

    //Fare info
    private double estimateFare = -1;

    public Ride(double startLatitude, double startLongitude){
        this.startLatitude = startLatitude;
        this.startLongitude = startLongitude;
    }

    public Ride(Parcel source){
        this.startLatitude = source.readDouble();
        this.startLongitude = source.readDouble();
        this.endLatitude = source.readDouble();
        this.endLongitude = source.readDouble();
        this.estimateFare = source.readDouble();
    }

    public double getStartLatitude(){
        return this.startLatitude;
    }

    public double getStartLongitude(){
        return this.startLongitude;
    }

    public double getEndLatitude(){
        return this.endLatitude;
    }

    public double getEndLongitude(){
        return this.endLongitude;
    }

    public void setEndPoint(double endLatitude, double endLongitude){
        this.endLatitude = endLatitude;
        this.endLongitude = endLongitude;
    }

    public void setEstimateFare(double fare){
        this.estimateFare = fare;
    }

    public double getEstimateFare(){
        return this.estimateFare;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeDouble(this.startLatitude);
        dest.writeDouble(this.startLongitude);
        dest.writeDouble(this.endLatitude);
        dest.writeDouble(this.endLongitude);
        dest.writeDouble(this.estimateFare);
    }

    public static final Creator CREATOR = new Creator() {
        @Override
        public Ride createFromParcel(Parcel source) {
            Ride ride = new Ride(source);
            return ride;
        }

        @Override
        public Ride[] newArray(int size) {
            return new Ride[size];
        }
    };
}
