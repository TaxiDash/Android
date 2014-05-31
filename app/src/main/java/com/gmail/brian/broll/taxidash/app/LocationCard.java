package com.gmail.brian.broll.taxidash.app;

import android.content.Context;
import android.location.Address;
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
public class LocationCard extends Card {
    private Address address;

    public LocationCard(Context context, Address address) {
        super(context, R.layout.location_card_layout);
        this.address = address;
        init();
    }

    public Address getAddress(){
        return this.address;
    }

    private void init(){
        // Add company picture
        //ImageView image = (ImageView)
        //TODO
    }

    @Override
    public void setupInnerViewElements(ViewGroup parent, View view){
        TextView featureName = (TextView) parent.findViewById(R.id.card_location_name);
        TextView addr = (TextView) parent.findViewById(R.id.card_address);

        if(featureName != null){
            featureName.setText(address.getFeatureName());
        }else{
            featureName.setText(address.toString());
        }

        if(addr != null){
            addr.setText(this.address.getAddressLine(0));
        }
    }
}
