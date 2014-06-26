package com.gmail.brian.broll.taxidash.app;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import it.gmariotti.cardslib.library.internal.Card;
import it.gmariotti.cardslib.library.internal.CardArrayAdapter;
import it.gmariotti.cardslib.library.view.CardListView;

/**
 * Created by Brian Broll on 5/17/14.
 */
public class LocalCompanyList extends NavigationActivity{
    private List<Company> companies = new ArrayList<Company>();
    List<Company> updatedCompanies;
    private String COMPANY_TAG = "COMPANY LIST";
    private Card.OnCardClickListener callCompany = new Card.OnCardClickListener() {
        @Override
        public void onClick(Card c, View v) {
            //What should happen when driver panel is pressed
            Intent callIntent = new Intent(Intent.ACTION_CALL);
            callIntent.setData(Uri.parse("tel:" + ((CompanyCard) c).getPhoneNumber()));
            startActivity(callIntent);
        }
    };
    /*
     * This will get a list of local companies from the server
     * sorted by rating. These will be presented in a list for
     * the user to touch to call.
     */

    //Consider adding companies one at a time...

    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        LayoutInflater inflater = (LayoutInflater) this
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View contentView = inflater.inflate(R.layout.activity_company_list, null, false);
        content.addView(contentView, 0);

        //Creating loading notification
        TextView searchingMsg = new TextView(this);
        searchingMsg.setText("Getting company info...");
        searchingMsg.setTextSize(24);
        searchingMsg.setTextColor(getResources().getColor(R.color.lightText));
        searchingMsg.setGravity(Gravity.CENTER);

        LinearLayout container = (LinearLayout) findViewById(R.id.company_list_container);
        container.addView(searchingMsg);

        //Load companies
        companies = Utils.loadLocalCompanies(this.getApplicationContext());
        if(companies != null){
            Log.i("Loading companies", "Loaded " + companies.size() + " companies");
        }else {
            Log.i("Loading companies", "Companies are null...");
        }

        if(companies != null) {
            Log.i("Company Loading", "Loaded Companies from file");
            createCompanyCards();
        }else{
            companies = new ArrayList<Company>();
        }

        new createCompanyList().execute();
    }

    private void createCompanyCards(){
        CardListView list = (CardListView) findViewById(R.id.company_list);
        List<Card> displayedCards = new ArrayList<Card>();

        //Add cards to the list
        for(Company company : companies){
            CompanyCard card = new CompanyCard(getApplicationContext(), company);

            card.setBackgroundResource(new ColorDrawable(getResources().getColor(R.color.cardColor)));
            card.setOnClickListener(callCompany);
            displayedCards.add(card);
        }

        CardArrayAdapter adapter = new CardArrayAdapter(getApplicationContext(), displayedCards);
        list.setAdapter(adapter);
        Log.i(COMPANY_TAG, "Finished adding cards (" + displayedCards.size() + ")");
    }

    private class createCompanyList extends Utils.GetLocalCompanies {

        @Override
        protected JSONArray doInBackground(Void... params) {
            updatedCompanies = createCompaniesFromJSON(super.doInBackground(params));
            return null;
        }

        protected void onPostExecute(JSONArray result){
            if(companies == null) {
                createCompanyCards();
            }
            Utils.saveLocalCompanies(getApplicationContext(), updatedCompanies);
        }

        private List<Company> createCompaniesFromJSON(JSONArray json){
            List<Company> result = new ArrayList<Company>();
            JSONObject jsonObject;
            Company company;

                for (int i = 0; i < json.length(); i++) {
                    try {
                        //Create each company
                        jsonObject = json.getJSONObject(i);
                        Log.i("Creating company", "" + jsonObject);
                        company = new Company(jsonObject.getInt("id"),
                                              jsonObject.getString("name"),
                                              (float) jsonObject.getDouble("average_rating"),
                                              jsonObject.getString("phone_number"));

                        Log.i("Company INFO: ", company.getName() + " " + company.getPhoneNumber());

                        Log.i("COMPANY RETRIEVAL", jsonObject.getString("name") + "'s phone number is " + jsonObject.getString("phone_number"));
                        //Get the image
                        URL url = new URL(CONSTANTS.CURRENT_SERVER.getAddress() + "/mobile/images/companies/" + company.getId() + ".json");
                        HttpURLConnection connection = (HttpURLConnection) url
                                .openConnection();
                        connection.setDoInput(true);
                        connection.connect();
                        InputStream input = connection.getInputStream();
                        Bitmap image = BitmapFactory.decodeStream(input);

                        File path = new File(getFilesDir(), "company_" + company.getId() +  ".png");
                        saveImageToCache(path.getAbsolutePath(), image);

                        company.setLogo(path.getAbsolutePath());
                        result.add(company);

                    } catch (JSONException e) {
                        //Handle a server address failure
                        //TODO
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            return result;
        }

        private void saveImageToCache(String filename, Bitmap image) {
            Log.i("IMAGE SAVING", "SAVING IMAGE TO " + filename);
            FileOutputStream out = null;
            try {
                out = new FileOutputStream(filename);
                image.compress(Bitmap.CompressFormat.PNG, 90, out);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try{
                    out.close();
                } catch(Throwable ignore) {}
            }
        }
    }

}
