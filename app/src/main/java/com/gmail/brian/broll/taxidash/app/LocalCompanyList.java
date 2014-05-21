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
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Brian Broll on 5/17/14.
 */
public class LocalCompanyList extends Activity{
    private Map<Integer, String> companyPhoneBook = new HashMap<Integer, String>();
    /*
     * This will get a list of local companies from the server
     * sorted by rating. These will be presented in a list for
     * the user to touch to call.
     */

    //TODO:
    //Consider adding companies one at a time...
    //Create android spinner

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

        //Add the click listener to send intent to calling the company
        View.OnClickListener callCompany = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //What should happen when driver panel is pressed
                String phoneNumber = companyPhoneBook.get(v.getId());

                Intent callIntent = new Intent(Intent.ACTION_CALL);
                callIntent.setData(Uri.parse("tel:" + phoneNumber));
                startActivity(callIntent);
                //TODO add company logo to call screen?
            }
        };

        //Attach calling functionality
        panel.setId(company.getId());
        panel.setOnClickListener(callCompany);

        return panel;
    }

    private class createCompanyList extends AsyncTask<Void, Void, Company[]> {

        @Override
        protected Company[] doInBackground(Void... params) {
            Company[] companies = null;
            //Get the contact info for the companies
            Log.i("COMPANY NETWORK STUFF", "ABOUT TO REQUEST COMPANY CONTACT INFO");
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
                companies = createCompaniesFromJSON(companiesJSON);

            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return companies;
        }

        protected void onPostExecute(Company[] companies){
            LinearLayout list = (LinearLayout) findViewById(R.id.company_list);
            LinearLayout companyPanel;

            for(int i = 0; i < companies.length; i++) {
                companyPanel = createCompanyPanel(companies[i]);
                list.addView(companyPanel);
            }
        }

        private Company[] createCompaniesFromJSON(JSONArray json){
            Company[] result = new Company[json.length()];
            JSONObject jsonObject;
            Company company;

                for (int i = 0; i < result.length; i++) {
                    try {
                        //Create each company
                        jsonObject = (JSONObject) json.get(i);
                        company = new Company(jsonObject.getInt("id"),
                                              jsonObject.getString("name"),
                                              jsonObject.getDouble("average_rating"),
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
                        result[i] = company;

                        //Add company number to the "phone book"
                        companyPhoneBook.put(company.getId(), company.getPhoneNumber());
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
