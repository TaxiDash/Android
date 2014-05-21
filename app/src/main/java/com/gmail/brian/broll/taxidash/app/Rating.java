package com.gmail.brian.broll.taxidash.app;

/**
 * Created by Brian Broll on 5/18/14.
 */
public class Rating {
    //Rating object for submitting on a driver
    private Driver driver = null;
    private int rating = 1;
    private String comments = "";
    private boolean sendingRide = true;

    public Rating(Driver d){
        this.driver = d;
    }

    public void setRating(int r){
        this.rating = r;
    }

    public int getRating(){
        return this.rating;
    }

    public void setComments(String c){
        this.comments = c;
    }

    public String getComments(){
        return this.comments;
    }

    public void removeRide(){
        this.sendingRide = false;
    }

    public boolean isSendingRide(){
        return this.sendingRide;
    }
}
