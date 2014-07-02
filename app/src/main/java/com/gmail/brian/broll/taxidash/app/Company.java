package com.gmail.brian.broll.taxidash.app;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.Parcel;
import android.os.Parcelable;

import java.io.File;
import java.io.Serializable;

/**
 * Created by Brian Broll on 5/16/14.
 */
public class Company implements Serializable, Comparable, Parcelable{
    private int id = -1;
    private String logo = null;
    private String name = null;
    private float rating = -1;
    private String phoneNumber = null;

    public Company(int idNumber, String name, float rating, String phoneNumber){
        this.id = idNumber;
        this.name = name;
        this.rating = rating;
        this.phoneNumber = phoneNumber;
    }

    public Company(Parcel source){
        this.readFromParcel(source);
    }

    public int getId(){
        return this.id;
    }

    public String getName(){
        return this.name;
    }

    public void setLogo(String filePath){
        this.logo = filePath;
    }

    public Bitmap getLogo(){
        if(this.logo == null || !(new File(this.logo)).exists()) {
            return null;
        }
        return BitmapFactory.decodeFile(this.logo);
    }

    public String getLogoURL(){
        return this.logo;
    }

    public String getPhoneNumber(){
        return this.phoneNumber;
    }

    public float getRating(){
        return this.rating;
    }

    @Override
    public int compareTo(Object otherDriver) {
        //Sort in ascending order of distance
        double difference = ((Company) otherDriver).getRating() - this.getRating();
        int sortInt = 1;
        if(difference < 0){
            sortInt = -1;
        }
        return sortInt;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeInt(this.id);
        out.writeString(this.name);
        out.writeFloat(this.rating);
        out.writeString(this.phoneNumber);
        out.writeString(this.logo);
    }

    public void readFromParcel(Parcel in) {
        this.id = in.readInt();
        this.name = in.readString();
        this.rating = in.readFloat();
        this.phoneNumber = in.readString();
        this.logo = in.readString();
    }

    public static final Creator CREATOR = new Creator() {
        @Override
        public Company createFromParcel(Parcel source) {
            return new Company(source);
        }

        @Override
        public Company[] newArray(int size) {
            return new Company[size];
        }
    };
}
