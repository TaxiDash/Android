package com.gmail.brian.broll.taxidash.app;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import java.io.File;


/**
 * This class contains all the driver info for a given taxi driver.
 * The info is populated from the feedback from the server.
 *
 * A driver can also be given a phone number when he is stored
 * as a favorite driver.
 *
 * Created by Brian Broll on 5/14/14.
 */
public class Driver implements Comparable, Parcelable{
    //private IMAGE logo
    private String company;
    private String image = ""; //filename (stored in cache)
    private int id = -1;
    private int beacon_id = -1;
    private String name = "NO NAME RECEIVED";
    private double rating = -1;
    private boolean validLicense = false;
    private double distance = 100000;

    private String phoneNumber = null;//Used if driver is favorited

    public Driver(int id, int b, String n, String companyName, double dis, double r, boolean valid){
        this.id = id;
        this.beacon_id = b;
        this.rating = r;
        this.name = n;
        this.validLicense = valid;
        this.company = companyName;
    }

    public Driver(Parcel source){
        this.readFromParcel(source);
    }

    public int getId(){
        return this.id;
    }

    public int getBeaconId(){
        return this.beacon_id;
    }

    public String getName(){
        return this.name;
    }

    public void setDistance(double dis){
        this.distance = dis;
    }

    public double getDistance(){
        return this.distance;
    }

    public double getRating(){
        return ((double) Math.round(100*this.rating))/100;
    }

    public boolean hasValidLicense(){
        return this.validLicense;
    }

    public String getCompanyName() {
        return this.company;
    }

    public void setImage(String im){
        this.image = im;
    }

    public void setPhoneNumber(String number){
        //We should probably make sure we only use numbers and remove
        //any (), - and " " from number
        //TODO
        this.phoneNumber = number;
    }

    public String getPhoneNumber(){
        return this.phoneNumber;
    }

    public Drawable getImage(){
        File filePath = new File(this.image);
        Drawable d = Drawable.createFromPath(filePath.toString());

        return d;
    }

    @Override
    public int compareTo(Object otherDriver) {
        //Sort in ascending order of distance
        return (int) (this.getDistance() - ((Driver) otherDriver).getDistance());
    }

    @Override
    public int describeContents() {
        //TODO
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeInt(this.id);
        out.writeInt(this.beacon_id);
        out.writeString(this.name);
        out.writeString(this.company);
        out.writeDouble(this.distance);
        out.writeDouble(this.rating);
        out.writeString(this.image);
        if(this.validLicense) {
            out.writeInt(1);
        }else {
            out.writeInt(0);
        }
    }

    public void readFromParcel(Parcel in){
        this.id = in.readInt();
        this.beacon_id = in.readInt();
        this.name = in.readString();
        this.company = in.readString();
        this.distance = in.readDouble();
        this.rating = in.readDouble();
        this.image = in.readString();
        this.validLicense = false;

        int valid = in.readInt();
        if(valid == 1){
            this.validLicense = true;
        }
    }

    public static final Creator CREATOR = new Creator() {
        @Override
        public Driver createFromParcel(Parcel source) {
            Driver driver = new Driver(source);
            return driver;
        }

        @Override
        public Driver[] newArray(int size) {
            return new Driver[size];
        }
    };
}

