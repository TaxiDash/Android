package com.gmail.brian.broll.taxidash.app;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import java.io.File;
import java.io.Serializable;


/**
 * This class contains all the driver info for a given taxi driver.
 * The info is populated from the feedback from the server.
 *
 * Created by Brian Broll on 5/14/14.
 */
public class Driver implements Comparable, Parcelable, Serializable{
    //private IMAGE logo
    private int id = -1;
    private int beacon_id = -1;
    private String name = "NO NAME RECEIVED";
    private float rating = -1;
    private String company;
    private String phoneNumber;
    protected String image = null; //filename (stored in cache)

    private boolean validLicense = false;
    private double distance = 100000;

    public Driver(int id, int b, String n, String companyName, float r, String number, boolean valid){
        this.id = id;
        this.beacon_id = b;
        this.name = n;
        this.rating = r;
        this.phoneNumber = number;
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

    public float getRating(){
        return ((float) Math.round(100*this.rating))/100;
    }

    public boolean hasValidLicense(){
        return this.validLicense;
    }

    public String getCompanyName() {
        return this.company;
    }

    public String getPhoneNumber(){
        return this.phoneNumber;
    }

    public void setImage(String im){
        this.image = im;
        Log.i("DRIVER", "Setting image to " + this.image);
    }

    public Bitmap getImage(){
        if(this.image == null || !(new File(this.image)).exists()){
            return null;
        }

        Bitmap d = BitmapFactory.decodeFile(this.image);
        return d;
    }

    public String getImageURL(){
        Log.i("DRIVER", "Retrieving image (" + this.image + ")");
        return this.image;
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
        out.writeString(this.image);
        out.writeString(this.phoneNumber);
        out.writeDouble(this.distance);
        out.writeDouble(this.rating);
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
        this.image = in.readString();
        this.phoneNumber = in.readString();
        this.distance = in.readDouble();
        this.rating = in.readFloat();
        this.validLicense = false;

        int valid = in.readInt();
        if(valid == 1){
            this.validLicense = true;
        }
    }

    public static final Creator CREATOR = new Creator() {
        @Override
        public Driver createFromParcel(Parcel source) {
            return new Driver(source);
        }

        @Override
        public Driver[] newArray(int size) {
            return new Driver[size];
        }
    };
}

