package com.gmail.brian.broll.taxidash.app;

import android.content.Context;
import android.location.Location;
import android.os.AsyncTask;
import android.util.Log;

import com.google.android.gms.location.LocationClient;

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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;

/**
 * Created by Brian Broll on 5/26/14.
 *
 * This class will have utility functions that are used
 * by multiple classes
 */
public class Utils {

    //Loading TaxiDash constants
    public static class setTaxiDashConstants extends AsyncTask<Location, Void, Void>{

        @Override
        protected Void doInBackground(Location... params) {
            JSONObject servers = null;
            double latitude = params[0].getLatitude(),
                   longitude = params[0].getLongitude();

            //Request the nearest TaxiDash server
            String endpoint = "/getNearbyTaxiDash?latitude=" + latitude + "&longitude=" + longitude;
            servers = makeRequestToRegistrationServer(endpoint);

            Log.i("Server info", "Server info is " + servers);

            //Store the server info
            JSONArray serverList = null;
            JSONObject server = null;
            TaxiDashServer taxiDashServer = null;
            String city,
                   state,
                   address;

            if (servers != null) {
                try {
                    serverList = servers.getJSONArray("cities");

                    for (int i = 0; i < serverList.length(); i++){
                        try {
                            server = serverList.getJSONObject(i);

                            city = server.getString("city");
                            state = server.getString("state");
                            address = "http://" + server.getString("address");

                            taxiDashServer = new TaxiDashServer(city, state, address);

                            if(i == 0){
                                CONSTANTS.CURRENT_SERVER = taxiDashServer;
                                Log.i("INIT", "Current city is " + taxiDashServer.getCity());
                            }

                            CONSTANTS.NEARBY_SERVERS.add(taxiDashServer);

                        } catch (JSONException e) {
                            //Error retrieving one of the cities
                            e.printStackTrace();
                        }
                    }

                } catch (JSONException e) {
                    //Could not retrieve nearbyCities
                    //TODO
                    e.printStackTrace();
                }
            } else {
                //No nearby servers found
                //TODO
            }

            //TODO
            return null;
        }
    }

    //Getting all TaxiDash servers
    public static class GetAllTaxiDashServers extends AsyncTask<Void, Void, Void>{

        @Override
        protected Void doInBackground(Void... params) {
            //Request all TaxiDash servers from routing server and store them in CONSTANTS
            String path = "/getAllTaxiDashServers";
            JSONObject response = makeRequestToRegistrationServer(path);
            if (response != null){
                //Get the TaxiDash info
                try {
                    JSONArray servers = response.getJSONArray("cities");
                    JSONObject server;
                    TaxiDashServer taxiDashServer;
                    for(int i = 0; i < servers.length(); i++){
                        try {
                            server = servers.getJSONObject(i);
                            taxiDashServer = new TaxiDashServer(server.getString("city"),
                                 server.getString("state"), server.getString("address"));
                            CONSTANTS.ALL_SERVERS.add(taxiDashServer);
                        } catch (JSONException e) {
                            //Failed on a specific server instance
                            e.printStackTrace();
                        }
                    }
                } catch (JSONException e) {
                    //Could not find the list of cities
                    e.printStackTrace();
                }
            }
            return null;
        }
    }

    private static JSONObject makeRequestToRegistrationServer(String path){
        JSONObject response = null;
        String resString;

        String endpoint = CONSTANTS.ROUTER_ADDRESS + path;
        HttpClient http = new DefaultHttpClient();
        http.getParams().setParameter(CoreProtocolPNames.PROTOCOL_VERSION, HttpVersion.HTTP_1_1);

        HttpGet req = new HttpGet(endpoint);
        HttpResponse res = null;
        try {
            res = http.execute(req);
            HttpEntity entity = res.getEntity();
            resString = EntityUtils.toString(entity);
            response = new JSONObject(resString);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
         return response;
    }

    public static ArrayList<Driver> loadFavoriteDrivers(Context context) {
        //Load the favorite drivers from file
        //File is saved as CITY_NAME-favorites.dat
        ArrayList<Driver> favoriteDrivers;
        String favFileName = CONSTANTS.CURRENT_SERVER.getCity() +
                CONSTANTS.CURRENT_SERVER.getState() + "-favorites.dat";

        File favoriteDriverFile = new File(context.getFilesDir(), favFileName);

        if (favoriteDriverFile.exists()) {
            try {
                FileInputStream inputStream = new FileInputStream(favoriteDriverFile.getPath());
                ObjectInputStream in = new ObjectInputStream(inputStream);
                favoriteDrivers = (ArrayList<Driver>) in.readObject();

            } catch (Exception e) {
                e.printStackTrace();
                favoriteDrivers = new ArrayList<Driver>();//Anything breaks, reinitialize!
                Log.i("LOADING FAV DRIVERS", "INITIALIZING FAV DRIVERS");
            }

        } else {//No favorite drivers yet!
            favoriteDrivers = new ArrayList<Driver>();
            Log.i("LOADING FAV DRIVERS", "INITIALIZING FAV DRIVERS");
        }

        return favoriteDrivers;
    }
}
