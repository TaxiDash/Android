package com.gmail.brian.broll.taxidash.app;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.lang.reflect.Array;
import java.util.ArrayList;

/**
 * Created by Brian Broll on 5/26/14.
 *
 * This class will have utility functions that are used
 * by multiple classes
 */
public class Utils {

    public static ArrayList<Driver> loadFavoriteDrivers(Context context) {
        //Load the favorite drivers from file
        //File is saved as CITY_NAME-favorites.dat
        ArrayList<Driver> favoriteDrivers;
        String favFileName = CONSTANTS.CITY_NAME + "-favorites.dat";

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
