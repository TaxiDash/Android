package com.gmail.brian.broll.taxidash.app;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RatingBar;
import android.widget.TextView;

import it.gmariotti.cardslib.library.internal.Card;

/**
 * Created by Brian Broll on 5/26/14.
 *
 * This is the card used for displaying companies.
 */
public class CompanyCard extends Card {
    private Company company = null;

    public CompanyCard(Context context, Company company){
        super(context, R.layout.company_card_layout);
        this.company = company;
        init();
    }

    private void init(){
   }

    public String getPhoneNumber(){
        return this.company.getPhoneNumber();
    }

    @Override
    public void setupInnerViewElements(ViewGroup parent, View view){
        TextView companyName= (TextView) parent.findViewById(R.id.card_company_name);
        RatingBar rating = (RatingBar) parent.findViewById(R.id.card_rating);

        if(companyName != null){
            companyName.setText(this.company.getName());
        }

        if(rating != null) {
            rating.setNumStars(CONSTANTS.MAX_RATING);
            rating.setRating(this.company.getRating());
        }
    }
}
