package com.gmail.brian.broll.taxidash.app;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RatingBar;
import android.widget.TextView;

import it.gmariotti.cardslib.library.internal.Card;

/*
 * Created by Brian Broll on 5/20/14.
 *
 * This is the layout for a given driver card.
 */
public class DriverCard extends Card {
    private Driver driver;

    public DriverCard(Context context) {
        super(context, R.layout.driver_card_layout);
        this.driver = new Driver(1, 1, "Dummy Driver", "Dumb Company", 2.4f, "", true);
        init();
    }

    public DriverCard(Context context, Driver driver) {
        super(context, R.layout.driver_card_layout);
        this.driver = driver;
        init();
    }

    public Driver getDriver(){
        return this.driver;
    }

    private void init(){
        // Add company picture
        //ImageView image = (ImageView)
        //TODO
    }

    public void setClickListener(OnCardClickListener listener){
        setOnClickListener(listener);
    }

    @Override
    public void setupInnerViewElements(ViewGroup parent, View view){
        TextView driverName = (TextView) parent.findViewById(R.id.card_driver_name);
        TextView companyName = (TextView) parent.findViewById(R.id.card_company_name);
        RatingBar rating = (RatingBar) parent.findViewById(R.id.card_rating);

        if(driverName != null){
            driverName.setText(this.driver.getName());
        }

        if(companyName != null){
            companyName.setText(this.driver.getCompanyName());
        }

        if(rating != null) {
            rating.setNumStars(CONSTANTS.MAX_RATING);
            rating.setRating(this.driver.getRating());
        }
    }
}
