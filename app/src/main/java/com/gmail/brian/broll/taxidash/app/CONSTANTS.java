package com.gmail.brian.broll.taxidash.app;

/*
 * Created by Brian Broll on 5/17/14.
 *
 * This file contains static settings to be used throughout
 * the app.
 *
 * Many of these settings are dependent upon the city
 * that the app is being used in; specifically, the
 * TaxiDash server that the app has connected to.
 *
 * This being said, these constants are all static
 * based on the city but not all are static from the app's
 * perspective. For example, if you are flying from one
 * city to another some of these "static" settings will be
 * reset. Therefore, we also have some semi-static settings.
 */

import java.lang.reflect.Array;
import java.util.ArrayList;

public class CONSTANTS {
    //Globally Static settings
    public static final boolean DEMO_MODE = false;
    public static final String ROUTER_ADDRESS = "http://68.53.106.44:8888";
    public static String TEMP = null;//Semi-persistent temp directory

    //Location specific static settings (semi-static)
    public static TaxiDashServer CURRENT_SERVER = null;
    //Consider changing the next item to "RECENT_SERVERS"
    public static ArrayList<TaxiDashServer> NEARBY_SERVERS = new ArrayList<TaxiDashServer>();
    public static ArrayList<TaxiDashServer> ALL_SERVERS = new ArrayList<TaxiDashServer>();

    public static int MAX_RATING = 5;

}
