package com.gmail.brian.broll.taxidash.app;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.radiusnetworks.ibeacon.IBeacon;
import com.radiusnetworks.ibeacon.IBeaconConsumer;
import com.radiusnetworks.ibeacon.IBeaconManager;
import com.radiusnetworks.ibeacon.RangeNotifier;
import com.radiusnetworks.ibeacon.Region;

import it.gmariotti.cardslib.library.internal.Card;
import it.gmariotti.cardslib.library.internal.CardArrayAdapter;
import it.gmariotti.cardslib.library.view.CardListView;


public class NearbyCabList extends NavigationActivity implements IBeaconConsumer{
    /*
     * A couple big things will be going on here:
     *     + Populating the nearby cab list
     *         + Getting beacon ids of nearby beacons
     *         + Sending beacon ids to server for driver info
     *         + Displaying the driver info of each driver in a list
     *
     *     + Clicking a cab for more info
     *         + Send intent to another activity
     *
     *     + If no cabs...
     *         + Alert user that no cabs are nearby
     *         + Offer to call top cab companies
     *             + Get cab companies and phone numbers from the server
     *             + Pass intent to phone calling functionality
     */

    /*
     * TODO:
     *      + Listen for changes to bluetooth after app is running
     *          + Warn the user that bluetooth must be enabled to get nearby cabs
     *      + Alert the user that the app will not be able to get nearby cabs if BT not supported
     */

    Map<Integer, Driver> driverCache = new HashMap<Integer, Driver>();
    Map<Integer, Company> companyCache = new HashMap<Integer, Company>();


    Map<Integer, DriverCard> beaconId2driverCard = new HashMap<Integer, DriverCard>();
    ArrayList<Integer> nearbyDrivers = new ArrayList<Integer>();
    ArrayList<Integer> displayedDrivers = new ArrayList<Integer>();
    List<Card> displayedCards = new ArrayList<Card>();

    //iBeacon Stuff
    String BEACON_TAG = "iBEACON MSG:";
    private final int TOTAL_ATTEMPTS = 10;
    private int ATTEMPTS_LEFT = TOTAL_ATTEMPTS;
    private IBeaconManager iBM = IBeaconManager.getInstanceForApplication(this);

    BluetoothAdapter bAdaptor;
    private final int REQUEST_ENABLE_BT = 1;

    Card.OnCardClickListener viewDriver;
    ProgressDialog progress;

    TextView noneFoundMsg = null;

    public NearbyCabList() {
        final IBeaconConsumer self;
        self = this;
        viewDriver = new Card.OnCardClickListener() {
            @Override
            public void onClick(Card c, View v) {
                //What should happen when driver panel is pressed
                iBM.unBind(self);
                Driver driver;
                driver = ((DriverCard) c).getDriver();
                Log.i("NEARBY CABS", "Driver clicked has rating of " + driver.getRating());

                Intent viewDriverIntent = new Intent(v.getContext(), DriverProfile.class);
                viewDriverIntent.putExtra("Driver", (android.os.Parcelable) driver);
                startActivity(viewDriverIntent);
            }
        };
    }

    private void setFonts(){
        //Set all textview objects to use font-awesome
        //TODO
        Typeface fontAwesome = Typeface.createFromAsset(getAssets(), "fontawesome-webfont.ttf");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LayoutInflater inflater = (LayoutInflater) this
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View contentView = inflater.inflate(R.layout.activity_taxi__list, null, false);
        content.addView(contentView, 0);

        //Get temp dir
        if(CONSTANTS.TEMP == null) {
            CONSTANTS.TEMP = getFilesDir() + "/tmp";
            new File(CONSTANTS.TEMP).mkdir();
        }

        //Set noneFoundMsg
        noneFoundMsg = new TextView(this);
        noneFoundMsg.setText("Searching for nearby taxis...");
        noneFoundMsg.setTextSize(24);
        noneFoundMsg.setTextColor(getResources().getColor(R.color.lightText));
        noneFoundMsg.setGravity(Gravity.CENTER);

        FrameLayout container = (FrameLayout) findViewById(R.id.containerq);
        container.addView(noneFoundMsg);

        bAdaptor = BluetoothAdapter.getDefaultAdapter();
        if(bAdaptor == null){//bluetooth not supported on device
            //Alert the user that the app will not be able to get nearby
            //cab info
            offerToCallCompany("Your device does not have bluetooth and " +
                    "will not be able to detect nearby cabs. ");
        }else{
            startBlueTooth();
        }
    }

    private void startBlueTooth(){
        //Make sure the user has bluetooth
        if(!bAdaptor.isEnabled()){
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }else{
            iBM.bind(this);
            //Write this to the background...
            //TODO
            progress = new ProgressDialog(this);
            progress.setTitle("Searching...");
            progress.setMessage("Please wait while we find nearby cabs...");
            progress.show();
        }
    }

    protected void onDestroy(){
        super.onDestroy();
        iBM.unBind(this);
    }

    @Override
    public void onPause(){
        //Don't scan while suspended
        iBM.unBind(this);
        super.onPause();
    }

    @Override
    public void onResume(){
        super.onResume();
        //if((bAdaptor = BluetoothAdapter.getDefaultAdapter()) != null) {
            //startBlueTooth();
        //}
    }

    private void offerToCallCompany(String reason){
        //Ask the user if he/she wants to call a local cab company
        AlertDialog.Builder alert = new AlertDialog.Builder(this);

        //alert.setTitle("Compan");
        alert.setMessage(reason + "\n\nWould you like to call a local cab company?");

        alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                // Do something with value!
                //Go to LocalCompanyList Activity
                //TODO
                Intent callCompanyIntent = new Intent(getBaseContext(), LocalCompanyList.class);
                startActivity(callCompanyIntent);
            }
        });

        alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
            }
        });

        alert.show();

    }

    //After Bluetooth is turned on/off...
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        switch(requestCode){//This switch case may be unnecessary but should be easily extended
            case REQUEST_ENABLE_BT://Bluetooth
                if(resultCode == RESULT_OK) {
                    //Start discovering nearby cabs
                    iBM.bind(this);
                    progress = new ProgressDialog(this);
                    progress.setTitle("Searching...");
                    progress.setMessage("Please wait while we find nearby cabs...");
                    progress.show();
                }else{
                    //Add warning message
                    offerToCallCompany("Without bluetooth you " +
                            "will not be able to detect nearby cabs. ");
                }
                break;
        }
    }

    protected void displayDriverInfo(){
        //Stop the waiting bar
        if(nearbyDrivers.size() > 0){
            if(progress != null) {
                progress.dismiss();
            }
            ATTEMPTS_LEFT = TOTAL_ATTEMPTS;
        }else if(ATTEMPTS_LEFT == 0) {
            //Ask to keep searching
            ATTEMPTS_LEFT--;//Make sure we only ask with one box
            askToKeepSearching();
        }else{
            ATTEMPTS_LEFT--;
        }

        //Clear the current list
        ArrayList<Integer> nearbyDriversClone = (ArrayList<Integer>) nearbyDrivers.clone();//Snapshot of nearby drivers

        CardListView list = (CardListView) findViewById(R.id.taxi_list);

        for(int i = displayedDrivers.size()-1; i >= 0; i--){
            Integer displayedDriverId = displayedDrivers.get(i);
            if(nearbyDriversClone.indexOf(displayedDriverId) != -1){//If not nearby, remove from screen
                displayedDrivers.remove(displayedDriverId);
                displayedCards.remove(beaconId2driverCard.get(displayedDriverId));
            }
        }

        Collections.sort(nearbyDriversClone);
        Context context = this.getApplicationContext();
        for(Integer nearbyDriverId : nearbyDriversClone){
            //Create the driver's card
            FrameLayout container = (FrameLayout) findViewById(R.id.containerq);
            container.removeView(noneFoundMsg);

            if(displayedDrivers.indexOf(nearbyDriverId) == -1) { //Add the driver
                DriverCard driverCard;
                if (CONSTANTS.DEMO_MODE) {
                    driverCard = new DriverCard(context);
                } else {
                    driverCard = new DriverCard(context, driverCache.get(nearbyDriverId));
                }

                driverCard.setClickListener(viewDriver);
                driverCard.setBackgroundResource(new ColorDrawable(getResources().getColor(R.color.cardColor)));
                //Adding the driver
                displayedDrivers.add(nearbyDriverId);
                displayedCards.add(driverCard);
                beaconId2driverCard.put(nearbyDriverId, driverCard);
            }
        }


        //TODO Find out how to remove cards from view
        CardArrayAdapter adapter;
        adapter = new CardArrayAdapter(context, displayedCards);
        list.setAdapter(adapter);
    }

    private void askToKeepSearching(){
                //Ask the user if he/she wants to call a local cab company
        AlertDialog.Builder alert = new AlertDialog.Builder(this);

        alert.setTitle("No cabs found yet");
        alert.setMessage("Would you like to search a little longer?");

        alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                //if yes...
                ATTEMPTS_LEFT = TOTAL_ATTEMPTS;
            }
        });

        alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                //if no...
                progress.dismiss();
            }
        });

        alert.show();

    }

    @Override
    public void onIBeaconServiceConnect() {
        iBM.setRangeNotifier(new RangeNotifier() {
            @Override public void didRangeBeaconsInRegion(Collection<IBeacon> iBeacons, Region region) {
                //For each beacon in range, get the driver info
                //Sort the driver info by distance
                Log.i(BEACON_TAG, "FOUND " + iBeacons.size() + " BEACON(S)");
                Iterator<IBeacon> itr = iBeacons.iterator();
                Integer beaconId;
                ArrayList<Integer> beaconIds = new ArrayList<Integer>();

                while(itr.hasNext()){
                    IBeacon b = itr.next();
                    beaconId = b.getMinor();//Make sure we are using the minor and not the major...
                    if(driverCache.containsKey(beaconId)){
                        driverCache.get(beaconId).setDistance(b.getAccuracy());
                        nearbyDrivers.add(beaconId);
                    }else{
                        beaconIds.add(b.getMinor());
                        //Request driver info from the server
                        Log.i(BEACON_TAG, "Gonna request info for driver with beacon id: " + beaconId);
                    }
                }
                Integer[] bIds = new Integer[beaconIds.size()];
                for(int i = 0; i < bIds.length; i++){
                    bIds[i] = beaconIds.get(i);
                }

                //DEMO PURPOSES ONLY
                if(CONSTANTS.DEMO_MODE){
                    Log.i(BEACON_TAG, "DEMO MODE ACTIVE");
                    bIds = new Integer[3];
                    bIds[0] = 37;
                    bIds[1] = 1661;
                    bIds[2] = 1662;
                }

                Log.i(BEACON_TAG, "ABOUT TO REQUEST DRIVER INFO");

                new getNearbyCabInfo().execute(bIds);
            }
        });

        try {
            iBM.startRangingBeaconsInRegion(new Region("myRangingUniqueId", null, null, null));
        } catch (RemoteException e) {
        }
    }

    private class getNearbyCabInfo extends AsyncTask<Integer[], Void, JSONObject[]>{

        @Override
        protected JSONObject[] doInBackground(Integer[]... params) {
            //Given the beaconIds, get the driver info
            Integer[] beaconIds = params[0];//Only grab the first one FIXME
            Log.i("NETWORK STUFF", "FOUND " + beaconIds.length + " BEACONS IN NETWORK SECTION");
            String driverInfo;
            Integer beaconId;
            JSONObject[] result = new JSONObject[beaconIds.length];
            for(int i = 0; i < beaconIds.length; i++) {
                beaconId = beaconIds[i];
                String endpoint = CONSTANTS.CURRENT_SERVER.getAddress() + "/mobile/" + beaconId + ".json";
                Log.i("REQUESTING CAB", "AT " + endpoint);

                try {
                    HttpClient http = new DefaultHttpClient();
                    http.getParams().setParameter(CoreProtocolPNames.PROTOCOL_VERSION, HttpVersion.HTTP_1_1);

                    HttpGet req = new HttpGet(endpoint);
                    HttpResponse response = http.execute(req);
                    HttpEntity entity = response.getEntity();
                    driverInfo = EntityUtils.toString(entity);
                    result[i] = new JSONObject(driverInfo);

                    Log.i("JSON RECEIVED: ", result[i].toString());

                    JSONObject company = result[i].getJSONObject("company");
                    Integer companyId = company.getInt("id");
                    if(companyCache.get(companyId) == null){
                        Company newCompany = new Company(company.getInt("id"),
                                company.getString("name"), (float) company.getDouble("average_rating"),
                                company.getString("phone_number"));

                        companyCache.put(companyId, newCompany);

                        //Get the company's logo
                        newCompany.setLogo(getCompanyLogo(companyId));
                    }

                    driverCache.put(beaconId, new Driver(result[i].getInt("id"), beaconId,
                            result[i].getString("first_name")+ " " +result[i].getString("last_name"),
                            companyCache.get(companyId), (float) result[i].getDouble("average_rating"),
                            result[i].getString("phone_number"),
                            result[i].getBoolean("valid")));

                    nearbyDrivers.add(beaconId);
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (JSONException e) {
                    Log.e("NO DRIVER ERROR", "No driver with beacon id " + beaconId);
                    e.printStackTrace();
                }
                //Get the image for the driver
                /*
                try {
                    HttpURLConnection connection = (HttpURLConnection) url
                            .openConnection();
                    connection.setDoInput(true);
                    connection.connect();
                    InputStream input = connection.getInputStream();
                    Log.i(BEACON_TAG, "SET IMAGE FOR " + driverCache.get(beaconId).getName());
                    Bitmap img = BitmapFactory.decodeStream(input);

                    File path = new File(getFilesDir(), beaconId + ".png");
                    saveImageToCache(path.getAbsolutePath(), img);
                    driverCache.get(beaconId).setImage(path.getAbsolutePath());

                } catch (IOException e) {
                    e.printStackTrace();
                    Log.e("getBmpFromUrl error: ", e.getMessage().toString());
                    return null;
                }
                */
            }
            return result;
        }

        protected void onPostExecute(JSONObject[] result){
            displayDriverInfo();
        }

        private void saveImageToCache(String filename, Bitmap image) {
            Log.i("IMAGE SAVING", "GONNA SAVE IMAGE TO " + filename);
            FileOutputStream out = null;
            try {
                out = new FileOutputStream(filename);
                image.compress(Bitmap.CompressFormat.PNG, 90, out);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try{
                    out.close();
                    Log.i("IMAGE SAVING", "SAVED IMAGE TO " + filename);
                } catch(Throwable ignore) {}
            }
        }

        private String getCompanyLogo(Integer companyId){
            try {
                URL url = new URL(CONSTANTS.CURRENT_SERVER.getAddress() + "/mobile/images/companies/" + companyId + ".json");
                HttpURLConnection connection = (HttpURLConnection) url
                        .openConnection();
                connection.setDoInput(true);
                connection.connect();
                InputStream input = connection.getInputStream();
                Bitmap img = BitmapFactory.decodeStream(input);

                File path = new File(CONSTANTS.TEMP, "COMPANY_" + companyId + ".png");
                Log.i("SAVING COMPANY IMAGE", "FILE PATH IS " + CONSTANTS.TEMP);
                saveImageToCache(path.getAbsolutePath(), img);
                //Log.i("Company IMAGE", "SAVING IMAGE TO " + path.getAbsolutePath());
                return path.getAbsolutePath();

            } catch (IOException e) {
                e.printStackTrace();
                Log.e("getBmpFromUrl error: ", e.getMessage().toString());
                return null;
            }
        }
    }

}
