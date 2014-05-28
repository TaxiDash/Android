package com.gmail.brian.broll.taxidash.app;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
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
public class LocalCompanyList extends Activity{
    private List<Company> companies = new ArrayList<Company>();
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

    //TODO:
    //Consider adding companies one at a time...

    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_company_list);

        new createCompanyList().execute();
    }

    private LinearLayout createCompanyPanel(Company company){

        LinearLayout panel = new LinearLayout(this);
        //panel.setOrientation();//Set to horizontal

        //Add the company to the list
        Log.i("COMPANY_LIST", "ADDING COMPANY " + company.getName() + " to the list ");
        TextView rating = new TextView(this);
        rating.setText(company.getRating() + "");
        rating.setTextSize(25);
        panel.addView(rating);

        TextView name = new TextView(this);
        name.setText(company.getName() + "");
        name.setTextSize(25);
        panel.addView(name);

        //Attach calling functionality
        panel.setId(company.getId());

        return panel;
    }

    private class createCompanyList extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            Company[] companies = null;
            //Get the contact info for the companies
            Log.i(COMPANY_TAG, "ABOUT TO REQUEST COMPANY CONTACT INFO");
            String endpoint = CONSTANTS.SERVER_ADDRESS + "/mobile/companies/contact.json";

            try {
                HttpClient http = new DefaultHttpClient();
                http.getParams().setParameter(CoreProtocolPNames.PROTOCOL_VERSION, HttpVersion.HTTP_1_1);

                HttpGet req = new HttpGet(endpoint);
                HttpResponse response = http.execute(req);
                HttpEntity entity = response.getEntity();
                String companyRaw = EntityUtils.toString(entity);
                JSONObject companyContactInfo = new JSONObject(companyRaw);

                JSONArray companiesJSON = companyContactInfo.getJSONArray("companies");
                createCompaniesFromJSON(companiesJSON);

            } catch (Exception e) {
                //Add support for something breaking on the server
                //This would mean we need to handle things gracefully
                e.printStackTrace();
            }
            return null;
        }

        protected void onPostExecute(Void result){
            CardListView list = (CardListView) findViewById(R.id.company_list);
            List<Card> displayedCards = new ArrayList<Card>();

            //Add cards to the list
            for(Company company : companies){
                CompanyCard card = new CompanyCard(getApplicationContext(), company);

                card.setOnClickListener(callCompany);
                displayedCards.add(card);
            }

            CardArrayAdapter adapter = new CardArrayAdapter(getApplicationContext(), displayedCards);
            list.setAdapter(adapter);
            Log.i(COMPANY_TAG, "Finished adding cards (" + displayedCards.size() + ")");
        }

        private void createCompaniesFromJSON(JSONArray json){
            JSONObject jsonObject;
            Company company;

                for (int i = 0; i < json.length(); i++) {
                    try {
                        //Create each company
                        jsonObject = (JSONObject) json.get(i);
                        company = new Company(jsonObject.getInt("id"),
                                              jsonObject.getString("name"),
                                              (float) jsonObject.getDouble("average_rating"),
                                              jsonObject.getString("phone_number"));

                        Log.i("COMPANY RETRIEVAL", jsonObject.getString("name") + "'s phone number is " + jsonObject.getString("phone_number"));
                        //Get the image
                        URL url = new URL(CONSTANTS.SERVER_ADDRESS + "/mobile/images/companies/" + company.getId() + ".json");
                        HttpURLConnection connection = (HttpURLConnection) url
                                .openConnection();
                        connection.setDoInput(true);
                        connection.connect();
                        InputStream input = connection.getInputStream();
                        Bitmap image = BitmapFactory.decodeStream(input);

                        File path = new File(getFilesDir(), "company_" + company.getId() +  ".png");
                        saveImageToCache(path.getAbsolutePath(), image);

                        company.setLogo(path.getAbsolutePath());
                        companies.add(company);

                        //Add company number to the "phone book"
                    } catch (JSONException e) {
                        //Handle a server address failure
                        //TODO
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
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
