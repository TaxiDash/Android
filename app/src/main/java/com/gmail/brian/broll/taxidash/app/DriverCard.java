package com.gmail.brian.broll.taxidash.app;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import com.gmail.brian.broll.taxidash.app.Driver;

import it.gmariotti.cardslib.library.internal.Card;

/**
 * Created by Brian Broll on 5/20/14.
 */
public class DriverCard extends Card {
    /*
     * This is the layout for a given driver card.
     *
     * TODO create the layout for the card in /layouts
     */
    private Driver driver;

    public DriverCard(Context context, Driver driver) {
        super(context);
        this.driver = driver;
        init();
    }

    private void init(){
        //Set the layout values given
        //     - driver's name
        //     - company's picture
        //     - driver's rating (in stars)
        //
        //     - add click listener
        //TODO
    }

    @Override
    public void setupInnerViewElements(ViewGroup parent, View view){
        //TODO
    }
}
