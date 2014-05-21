package com.gmail.brian.broll.taxidash.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.RemoteException;
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
import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.radiusnetworks.ibeacon.IBeacon;
import com.radiusnetworks.ibeacon.IBeaconConsumer;
import com.radiusnetworks.ibeacon.IBeaconManager;
import com.radiusnetworks.ibeacon.RangeNotifier;
import com.radiusnetworks.ibeacon.Region;


public class NearbyCabList extends Activity implements IBeaconConsumer{
    //String ROUTER_ADDRESS;
    Map<Integer, Driver> driverCache = new HashMap<Integer, Driver>();
    Map<String, Company> companyCache = new HashMap<String, Company>();
    ArrayList<Driver> nearbyDrivers = new ArrayList<Driver>();
    ArrayList<Driver> displayedDrivers = new ArrayList<Driver>();

    //iBeacon Stuff
    String BEACON_TAG = "iBEACON MSG:";
    private final int TOTAL_ATTEMPTS = 10;
    private int ATTEMPTS_LEFT = TOTAL_ATTEMPTS;
    private IBeaconManager iBM = IBeaconManager.getInstanceForApplication(this);

    BluetoothAdapter bAdaptor;
    private final int REQUEST_ENABLE_BT = 1;

    View.OnClickListener viewDriver;
    ProgressDialog progress;

    public NearbyCabList() {
        final IBeaconConsumer self;
        self = this;
        viewDriver = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //What should happen when driver panel is pressed
                iBM.unBind(self);
                Driver driver = driverCache.get(v.getId());

                Intent viewDriverIntent = new Intent(v.getContext(), DriverProfile.class);
                viewDriverIntent.putExtra("Driver", driver);
                startActivity(viewDriverIntent);
                //TODO Pass an intent to Driver Profile Activity
            }
        };
    }

    private void setFonts(){
        //Set all textview objects to use font-awesome
        //TODO
        Typeface fontAwesome = Typeface.createFromAsset(getAssets(), "fontawesome-webfont.ttf");
    }

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_taxi__list);

        //Make sure the user has bluetooth
        bAdaptor = BluetoothAdapter.getDefaultAdapter();
        if(bAdaptor == null){//bluetooth not supported on device
            //Alert the user that the app will not be able to get nearby
            //cab info
            offerToCallCompany("Your device does not have bluetooth and " +
                    "will not be able to detect nearby cabs. ");
        }

        if(!bAdaptor.isEnabled()){
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }else{
            iBM.bind(this);
            progress = new ProgressDialog(this);
            progress.setTitle("Searching...");
            progress.setMessage("Please wait while we look up info on any nearby cab drivers...");
            progress.show();
        }
    }

    protected void onDestroy(){
        super.onDestroy();
        iBM.unBind(this);
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
                    progress.setMessage("Please wait while we look up info on any nearby cab drivers...");
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
            progress.dismiss();
            ATTEMPTS_LEFT = TOTAL_ATTEMPTS;
        }else if(ATTEMPTS_LEFT == 0) {
            //Ask to keep searching
            ATTEMPTS_LEFT--;//Make sure we only ask with one box
            askToKeepSearching();
        }else{
            ATTEMPTS_LEFT--;
        }

        //Clear the current list
        LinearLayout taxiList = (LinearLayout) findViewById(R.id.taxi_list);
        Log.i("BEACON STUFF", "ABOUT TO CLEAR VIEWS!!!!!!");
        //taxiList.removeAllViews();
        Log.i("BEACON STUFF", "ADDING " + nearbyDrivers.size() + "!!!!!!");
        ArrayList<Driver> nearbyDriversClone = (ArrayList<Driver>) nearbyDrivers.clone();//Snapshot of nearby drivers

        for(Driver displayedDriver : displayedDrivers){
            if(nearbyDriversClone.indexOf(displayedDriver) == -1){
                displayedDrivers.remove(displayedDriver);
            }
        }

        Collections.sort(nearbyDriversClone);
        for(Driver nearbyDriver : nearbyDriversClone){
            //Create the driver's icon
            if(displayedDrivers.indexOf(nearbyDriver) == -1){
                createDriverPanel(this.getApplicationContext(), taxiList, nearbyDriver);
                displayedDrivers.add(nearbyDriver);
            }
            //taxiList.addCard(new PlayCard(nearbyDriver.getName(), nearbyDriver.getCompanyName(),
                    //"#1155cc", "#cc5511", true, false));
        }
        //nearbyDrivers.clear();
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

    private void createDriverPanel(Context cxt, LinearLayout container, Driver driver){
        LinearLayout panel = new LinearLayout(cxt);
        panel.setId(driver.getBeaconId());
        panel.setOnClickListener(viewDriver);

        //Dress this up a little more
        //TODO
        /*
        ImageView icon = new ImageView(cxt);
        icon.setImageDrawable(driver.getImage());
        panel.addView(icon);
        */

        TextView test = new TextView(cxt);
        test.setText(driver.getRating() + " " + driver.getName() + " ");

        panel.addView(test);
        container.addView(panel);
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
                        nearbyDrivers.add(driverCache.get(beaconId));
                    }else{
                        beaconIds.add(b.getMinor());
                        //Request driver info from the server
                        Log.i(BEACON_TAG, "Gonna request info for driver with beacon id: " + b.getMinor());
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
                String endpoint = CONSTANTS.SERVER_ADDRESS + "/mobile/" + beaconId + ".json";

                try {
                    HttpClient http = new DefaultHttpClient();
                    http.getParams().setParameter(CoreProtocolPNames.PROTOCOL_VERSION, HttpVersion.HTTP_1_1);

                    HttpGet req = new HttpGet(endpoint);
                    HttpResponse response = http.execute(req);
                    HttpEntity entity = response.getEntity();
                    driverInfo = EntityUtils.toString(entity);
                    result[i] = new JSONObject(driverInfo);

                    JSONObject company = result[i].getJSONObject("company");
                    String companyName = company.getString("name");
                    if(companyCache.get(companyName) == null){
                        companyCache.put(companyName, new Company(company.getInt("id"),
                                company.getString("name"), company.getDouble("average_rating"), company.getString("phone_number")));
                    }

                    driverCache.put(beaconId, new Driver(result[i].getInt("id"), beaconId,
                            result[i].getString("first_name")+ " " +result[i].getString("last_name"),
                            companyName, -1, result[i].getDouble("average_rating"),
                            result[i].getBoolean("valid")));

                    nearbyDrivers.add(driverCache.get(beaconId));
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                //Get the image for the driver
                /*
                try {
                    URL url = new URL(CONSTANTS.SERVER_ADDRESS + "/mobile/images/drivers/" + beaconId + ".json");
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