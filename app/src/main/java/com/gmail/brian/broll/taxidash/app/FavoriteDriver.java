package com.gmail.brian.broll.taxidash.app;

import java.io.Serializable;

/**
 * Created by Brian Broll on 5/24/14.
 *
 */
public class FavoriteDriver extends Driver implements Serializable{
    private int frequency = 0;//Number of times called/rode with

    public FavoriteDriver(Driver driver){
        super(driver.getId(), driver.getBeaconId(), driver.getName(),
                driver.getCompany(), driver.getRating(),
                driver.getPhoneNumber(), driver.hasValidLicense());

        if(driver.getImage() != null) {//If image url is set and image is still in cache
            this.setImage(driver.getImageURL());
        }
    }

   public int getFrequency(){
        return this.frequency;
    }

    public void incrementFrequency(){
        ++this.frequency;
    }

    public String getImageURL(){
        return this.image;
    }
}
