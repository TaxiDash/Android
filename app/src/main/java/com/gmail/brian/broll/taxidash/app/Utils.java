package com.gmail.brian.broll.taxidash.app;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;

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
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

/**
 * Created by Brian Broll on 5/26/14.
 *
 * This class will have utility functions that are used
 * by multiple classes
 */
public class Utils {

    /* * * * * * * Debugging * * * * * * */
    public static void debugLogging(Context context, String msg){
        if(CONSTANTS.DEBUG){
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
        }
    }

    /* * * * * * * END Debugging * * * * * * */

    /* * * * * * * Network Communications * * * * * * */

    //Registration Server Messages
    public static class initializeTaxiDashConstants extends AsyncTask<Location, Void, Void>{

        @Override
        protected Void doInBackground(Location... params) {
            JSONObject servers = null;
            double latitude = params[0].getLatitude(),
                   longitude = params[0].getLongitude();

            //Request the nearest TaxiDash server
            String endpoint = "/getNearbyTaxiDash?latitude=" + latitude + "&longitude=" + longitude;
            try {
                servers = makeRequestToRegistrationServer(endpoint);
            } catch (IOException e) {
                //Make this more clear
                //TODO
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }

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

            return null;
        }
    }

    public static class GetAllTaxiDashServers extends AsyncTask<Void, Void, Void>{

        @Override
        protected Void doInBackground(Void... params) {
            //Request all TaxiDash servers from routing server and store them in CONSTANTS
            String path = "/getAllTaxiDashServers";
            JSONObject response = null;
            try {
                response = makeRequestToRegistrationServer(path);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }
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

    //TaxiDash Server messages
    public abstract static class GetLocalCompanies extends AsyncTask<Void, Void, JSONArray>{

        @Override
        protected JSONArray doInBackground(Void... params) {
            String path = "/mobile/companies/contact.json";
            JSONObject companyContactInfo = null;
            try {
                companyContactInfo = makeRequestToTaxiDashServer(path);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }
            try {
                return companyContactInfo.getJSONArray("companies");

            } catch (JSONException e) {
                e.printStackTrace();
            }
            return null;
        }
    }

    //Load the driver images for a given driver
    public abstract static class GetDriverImages extends AsyncTask<Driver, Void, Void>{

        @Override
        protected Void doInBackground(Driver... params) {
            Driver driver;
            String endpoint;
            for(int i = 0; i < params.length; i++){
                driver = params[i];
                endpoint = CONSTANTS.CURRENT_SERVER.getAddress() + "/mobile/images/drivers/"
                        + driver.getBeaconId() + ".json";

                try {
                    Bitmap image = getImageFromServer(endpoint);
                } catch (IOException e) {
                    //Perhaps put something better here...
                    //TODO
                    Log.i("GETTING DRIVER IMAGE", "FAILED");
                    e.printStackTrace();
                }

            }
            return null;
        }
    }

   //Convenience methods
    private static JSONObject makeRequestToTaxiDashServer(String path) throws IOException, JSONException {
        String endpoint = CONSTANTS.CURRENT_SERVER.getAddress() + path;
        Log.i("JSON request", endpoint );
        return makeJSONRequestToServer(endpoint);
    }

    private static JSONObject makeRequestToRegistrationServer(String path) throws IOException, JSONException {
        String endpoint = CONSTANTS.ROUTER_ADDRESS + path;
        return makeJSONRequestToServer(endpoint);
    }

    public static JSONObject makeJSONRequestToServer(String endpoint) throws IOException, JSONException {
        HttpEntity entity = makeRequestToServer(endpoint);
        String resString = EntityUtils.toString(entity);
        return new JSONObject(resString);
    }

    private static HttpEntity makeRequestToServer(String endpoint) throws IOException {
        JSONObject response = null;
        String resString;

        HttpClient http = new DefaultHttpClient();
        http.getParams().setParameter(CoreProtocolPNames.PROTOCOL_VERSION, HttpVersion.HTTP_1_1);

        HttpGet req = new HttpGet(endpoint);
        HttpResponse res = null;
        res = http.execute(req);

        return res.getEntity();
    }

    public static Bitmap getImageFromServer(String endpoint) throws IOException {
        URL url = new URL(endpoint);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setDoInput(true);
        connection.connect();
        InputStream input = connection.getInputStream();
        return BitmapFactory.decodeStream(input);
    }

    /* * * * * * * END Network Communications * * * * * * */

    /* * * * * * * Saving/retrieving favorite drivers to/from file * * * * * * */

    //Convenience methods for loading
    public static ArrayList<Driver> loadFavoriteDrivers(Context context) {
        //Load the favorite drivers from file
        //File is saved as CITY_NAME-favorites.dat
        ArrayList<Driver> favoriteDrivers = null;

        File favoriteDriverFile = new File(context.getFilesDir(), getFavoriteDriverFileName());

        if (favoriteDriverFile.exists()) {
            favoriteDrivers = (ArrayList<Driver>) loadObjectFromFile(favoriteDriverFile);
        }

        if(favoriteDrivers == null) {//No favorite drivers yet!
            favoriteDrivers = new ArrayList<Driver>();
            Log.i("LOADING FAV DRIVERS", "INITIALIZING FAV DRIVERS");
        }

        return favoriteDrivers;
    }

    public static boolean haveStoredLocalCompanies(Context context){
        File companyFile = new File(context.getFilesDir(), getLocalCompaniesFileName());

        return companyFile.exists();
    }

    public static ArrayList<Company> loadLocalCompanies(Context context){
        File companyFile = new File(context.getFilesDir(), getLocalCompaniesFileName());

        return (ArrayList<Company>) loadObjectFromFile(companyFile);
    }

    public static Object loadObjectFromFile(File file){
        Object object = null;

        try {
            FileInputStream inputStream = new FileInputStream(file.getPath());
            ObjectInputStream in = new ObjectInputStream(inputStream);
            object = in.readObject();

        } catch (Exception e) {
            e.printStackTrace();
        }
        return object;
    }

    //Convenience methods for saving
    public static void saveFavoriteDrivers(Context context, Object favDrivers){
        saveData(context, getFavoriteDriverFileName(), favDrivers);
    }

    public static void saveLocalCompanies(Context context, List<Company> companies){
        Log.i("Saving Companies", "Saving " + companies.size() + " companies to file");
        saveData(context, getLocalCompaniesFileName(), companies);
    }

    public static void saveData(Context context, String filename, Object data) {
        //Write the favorite drivers array to file
        File favoriteDriverFile = new File(context.getFilesDir(), filename);
        try {

            Log.i("Saving Object", "About to save object to " + favoriteDriverFile.getPath());
            FileOutputStream fileOutputStream = new FileOutputStream(favoriteDriverFile.getPath());
            ObjectOutputStream out = new ObjectOutputStream(fileOutputStream);
            out.writeObject(data);
            out.flush();
            out.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String getFavoriteDriverFileName(){
        return CONSTANTS.CURRENT_SERVER.getCity() +
                CONSTANTS.CURRENT_SERVER.getState() + "-favorites.dat";
    }

    private static String getLocalCompaniesFileName(){
        return CONSTANTS.CURRENT_SERVER.getCity() +
                CONSTANTS.CURRENT_SERVER.getState() + "-companies.dat";
    }

    /* * * * * * * END Saving/retrieving favorite drivers to/from file * * * * * * */

    /* * * * * * * Google Maps Directions * * * * * * */

    /*
     * Thank you to Emil Adz for similar code on stack overflow:
     * http://stackoverflow.com/questions/15638884/google-maps-routing-api-v2-android
     */

    private static class GoogleDirections {

        public static Document getDocument(LatLng start, LatLng end) {
            String url = "http://maps.googleapis.com/maps/api/directions/xml?"
                    + "origin=" + start.latitude + "," + start.longitude
                    + "&destination=" + end.latitude + "," + end.longitude
                    + "&sensor=false&mode=driving";

            try {
                InputStream in = makeRequestToServer(url).getContent();
                DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
                Document doc = builder.parse(in);
                return doc;
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        public static String getDurationText (Document doc) {
            NodeList nl1 = doc.getElementsByTagName("duration");
            Node node1 = nl1.item(0);
            NodeList nl2 = node1.getChildNodes();
            Node node2 = nl2.item(getNodeIndex(nl2, "text"));
            Log.i("DurationText", node2.getTextContent());
            return node2.getTextContent();
        }

        public static int getDurationValue (Document doc) {
            //Get total duration
            //TODO
            NodeList nl1 = doc.getElementsByTagName("duration");
            Node node1 = nl1.item(0);
            NodeList nl2 = node1.getChildNodes();
            Node node2 = nl2.item(getNodeIndex(nl2, "value"));
            Log.i("DurationValue", node2.getTextContent());
            return Integer.parseInt(node2.getTextContent());
        }

        public static String getDistanceText (Document doc) {
            NodeList nl1 = doc.getElementsByTagName("distance");
            Node node1 = nl1.item(0);
            NodeList nl2 = node1.getChildNodes();
            Node node2 = nl2.item(getNodeIndex(nl2, "text"));
            Log.i("DistanceText", node2.getTextContent());
            return node2.getTextContent();
        }

        public static int getDistanceValue (Document doc) {
            //Get total distance
            //TODO
            /*
            NodeList steps = doc.getElementsByTagName("distance");
            Element step;
            NodeList
            double totalDistance = 0;
            for(int i = 0; i < steps.getLength(); i++){
                step = (Element) steps.item(i);
                step.getElementsByTagName("value");//in meters
            }

            return totalDistance;
            */
            NodeList nl1 = doc.getElementsByTagName("distance");
            int totalDistance = 0;
            Log.i("distance node length:", nl1.getLength() + "");
            for(int i = 0; i < nl1.getLength(); i++){
                Node node1 = nl1.item(i);
                NodeList nl2 = node1.getChildNodes();
                Node node2 = nl2.item(getNodeIndex(nl2, "value"));
                Log.i("DistanceValue", node2.getTextContent());
                totalDistance += Integer.parseInt(node2.getTextContent());
            }
            Log.i("TotalDistance: ", totalDistance + " meters (" + (totalDistance/1609) + ")");
            return totalDistance;
       }

        public static ArrayList<LatLng> getDirections (Document doc) {
            NodeList nl1, nl2, nl3;
            ArrayList<LatLng> listGeopoints = new ArrayList<LatLng>();
            nl1 = doc.getElementsByTagName("step");
            if (nl1.getLength() > 0) {
                for (int i = 0; i < nl1.getLength(); i++) {
                    Node node1 = nl1.item(i);
                    nl2 = node1.getChildNodes();

                    Node locationNode = nl2.item(getNodeIndex(nl2, "start_location"));
                    nl3 = locationNode.getChildNodes();
                    Node latNode = nl3.item(getNodeIndex(nl3, "lat"));
                    double lat = Double.parseDouble(latNode.getTextContent());
                    Node lngNode = nl3.item(getNodeIndex(nl3, "lng"));
                    double lng = Double.parseDouble(lngNode.getTextContent());
                    listGeopoints.add(new LatLng(lat, lng));

                    locationNode = nl2.item(getNodeIndex(nl2, "polyline"));
                    nl3 = locationNode.getChildNodes();
                    latNode = nl3.item(getNodeIndex(nl3, "points"));
                    ArrayList<LatLng> arr = decodePoly(latNode.getTextContent());
                    for(int j = 0 ; j < arr.size() ; j++) {
                        listGeopoints.add(new LatLng(arr.get(j).latitude, arr.get(j).longitude));
                    }

                    locationNode = nl2.item(getNodeIndex(nl2, "end_location"));
                    nl3 = locationNode.getChildNodes();
                    latNode = nl3.item(getNodeIndex(nl3, "lat"));
                    lat = Double.parseDouble(latNode.getTextContent());
                    lngNode = nl3.item(getNodeIndex(nl3, "lng"));
                    lng = Double.parseDouble(lngNode.getTextContent());
                    listGeopoints.add(new LatLng(lat, lng));
                }
            }

            return listGeopoints;
        }

        private static int getNodeIndex(NodeList nl, String nodename) {
            for(int i = 0 ; i < nl.getLength() ; i++) {
                if(nl.item(i).getNodeName().equals(nodename))
                    return i;
            }
            return -1;
        }

        private static ArrayList<LatLng> decodePoly(String encoded) {
            ArrayList<LatLng> poly = new ArrayList<LatLng>();
            int index = 0, len = encoded.length();
            int lat = 0, lng = 0;
            while (index < len) {
                int b, shift = 0, result = 0;
                do {
                    b = encoded.charAt(index++) - 63;
                    result |= (b & 0x1f) << shift;
                    shift += 5;
                } while (b >= 0x20);
                int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
                lat += dlat;
                shift = 0;
                result = 0;
                do {
                    b = encoded.charAt(index++) - 63;
                    result |= (b & 0x1f) << shift;
                    shift += 5;
                } while (b >= 0x20);
                int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
                lng += dlng;

                LatLng position = new LatLng((double) lat / 1E5, (double) lng / 1E5);
                poly.add(position);
            }
            return poly;
        }
    }

    public abstract static class directionsCalculator extends AsyncTask<LatLng, Void, Void>{
        protected int durationValue;
        protected String durationText;
        protected double distanceValue;
        protected String distanceText;
        protected List<LatLng> directions;
        protected ArrayList<Double> fares = null;
        //protected

        @Override
        protected Void doInBackground(LatLng... params) {
            //First I will get the directions, then I will get the
            //estimate fare from the TaxiDash server
            assert params.length == 2;
            LatLng start = params[0];
            LatLng end = params[1];
            Document document = GoogleDirections.getDocument(start, end);

            durationText = GoogleDirections.getDurationText(document);
            durationValue = GoogleDirections.getDurationValue(document);
            distanceText = GoogleDirections.getDistanceText(document);
            distanceValue = ((double)GoogleDirections.getDistanceValue(document)/1609.34)/2;//Convert to miles
            directions = GoogleDirections.getDirections(document);

            //Get estimate fare from TaxiDash server
            String endpoint =  "/mobile/estimate_fare.json?origin="
                    + start.latitude + "," + start.longitude + "&destination=" + end.latitude
                    + "," + end.longitude + "&distance=" + distanceValue + "&duration=" + durationValue;

            try {
                JSONObject response = makeRequestToTaxiDashServer(endpoint);
                JSONArray faresArray = response.getJSONArray("fares");
                fares = new ArrayList<Double>();

                for(int i = 0; i < faresArray.length(); i++){
                    fares.add(faresArray.optDouble(i));
                }

            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }

            return null;
        }
    }

    /* * * * * * * END Google Maps Directions * * * * * * */

    //Save to TEMP_DIR
    //driver images
    //TODO
    //Load driver info
}
