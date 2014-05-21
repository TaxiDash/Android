package com.gmail.brian.broll.taxidash.app;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Parcel;
import android.os.Parcelable;

import java.io.File;

/**
 * Created by Brian Broll on 5/16/14.
 */
public class Company implements Comparable, Parcelable{
    private int id = -1;
    private String logo = "";
    private String name = null;
    private double rating = -1;
    private String phoneNumber = null;

    public Company(int idNumber, String name, double rating, String phoneNumber){
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

    public Drawable getLogo(){
        File filePath = new File(this.logo);
        Drawable d = Drawable.createFromPath(filePath.toString());
        return d;
    }

    public String getPhoneNumber(){
        return this.phoneNumber;
    }

    public double getRating(){
        return this.rating;
    }

    @Override
    public int compareTo(Object otherDriver) {
        //Sort in ascending order of distance
        return (int) (((Company) otherDriver).getRating() - this.getRating());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeInt(this.id);
        out.writeString(this.name);
        out.writeDouble(this.rating);
        out.writeString(this.phoneNumber);
        out.writeString(this.logo);
    }

    public void readFromParcel(Parcel in) {
        this.id = in.readInt();
        this.name = in.readString();
        this.rating = in.readDouble();
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
