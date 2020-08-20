package com.smad.m3s.hjhsalo.androidexample;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
import android.Manifest;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/***************************************************************************************
 *    Title: Maps SDK for Android: Get Started
 *    Author: Google
 *    Date: 20.12.2012
 *    Availability: https://developers.google.com/maps/documentation/android-sdk/start
 ***************************************************************************************/

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    //Bundle extras
    String API_KEY = BuildConfig.API_KEY;
    private GoogleMap mMap;
    LocationManager locationManager;
    ArrayList<LatLng> mMarkerPoints;
    Location location;
    private double lng;
    private double lat;
    private double[] targetCoords;
    private String currentLocation = "default";
    DataDownload dataDownload = new DataDownload();

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        if (checkLocationPermission()) {
            locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER, 5000, 10, new LocationListener() {
                        @Override
                        public void onLocationChanged(Location location) {

                                lat = location.getLatitude();
                                lng = location.getLongitude();
                                Log.d("onLocationChanged", lat + " " + lng);
                        }

                        @Override
                        public void onStatusChanged(String s, int i, Bundle bundle) {

                        }

                        @Override
                        public void onProviderEnabled(String s) {

                        }

                        @Override
                        public void onProviderDisabled(String s) {

                        }
                    });
        } else {
            Log.d("Permissions", "Permissions were not granted, not updating the map");
            //"Permissions not granted";
            return;
        }


        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_maps);
                SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        mMarkerPoints = new ArrayList<>();

    }
    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */

    public Location getLocation() {

            locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            if (checkLocationPermission()) {
                location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

            } else {
                Log.d("Permissions", "Permissions were not granted, not updating the map");
                //"Permissions not granted";
                return null;
            }
    return location;
    }
    public void onMapReady(GoogleMap googleMap) {
        location = getLocation();
        lat = location.getLatitude();
        lng = location.getLongitude();

        mMap = googleMap;
        dataDownload.execute();
        LatLng curLoc = new LatLng(lat, lng);
        if (mMarkerPoints.size() > 1){
            mMarkerPoints.clear();
            mMap.clear();
        }
        mMarkerPoints.add(curLoc);
        mMap.addMarker(new MarkerOptions().position(curLoc).title("Current location"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(curLoc));
        //zoom camera based on the distance between the starting and ending location in list of points
        //
        Bundle extras = getIntent().getExtras();
        String targetlocation = "0.0,0.0";
        try {
            targetlocation = extras.getString("LOCATION_DATA");
            //targetlocation = extras.getString()
        } catch (Exception e) {
            Log.d("Exception", e.toString());
        }
        double[] locations = parseLocation(targetlocation);
        double distance = distance(locations[0],locations[1], lat, lng);
        float zoom = 12.0f;
        Log.d("Distance: ", String.valueOf(distance));
        if (distance <= 25) {
            Log.d("Distance: ", "Distance is less than 25! Zoom 12.0f");
            zoom = 12.0f;
        }
        else if (distance <= 50) {
            Log.d("Distance: ", "Distance is less than 50! Zoom 9.0f");
            zoom = 10.0f;
        }
        else if (distance <= 100) {
            Log.d("Distance: ", "Distance is less than 100! Zoom8.0f");
            zoom = 9.0f;
        }
        else if (distance <= 300) {
            Log.d("Distance: ", "Distance is less than 300! Zoom 6.0f");
            zoom = 8.5f;
        }
        else if (distance <= 1000) {
            Log.d("Distance: ", "Distance is less than 1000! Zoom 5.0f");
            zoom = 5.0f;
        }
        else if (distance <= 2000){
            Log.d("Distance: ", "Distance is over 1000! Zoom 4.0f");
            zoom = 4.0f;
        }
        else {
            Log.d("Distance: ", "Distance is over 2000! Zoom 1.0f");
            zoom = 1.0f;
        }

        mMap.addMarker(new MarkerOptions().position(new LatLng(locations[0],locations[1])).title("Target location")).setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(curLoc,zoom));
    }
    public boolean checkLocationPermission()
    {
        String permission = "android.permission.ACCESS_FINE_LOCATION";
        int res = this.checkCallingOrSelfPermission(permission);
        return (res == PackageManager.PERMISSION_GRANTED);
    }
    public double[] parseLocation(String location) {
        double[] coordList = new double[2];

        String[] parts = location.split(",");
        coordList[0] = Double.parseDouble(parts[0].substring(2));
        coordList[1] = Double.parseDouble(parts[1]);
        return coordList;
    }
    private double distance(double lat1, double lon1, double lat2, double lon2) {
        if ((lat1 == lat2) && (lon1 == lon2)) {
            return 0;
        }
        else {
            double theta = lon1 - lon2;
            double dist = Math.sin(Math.toRadians(lat1)) * Math.sin(Math.toRadians(lat2)) + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.cos(Math.toRadians(theta));
            dist = Math.acos(dist);
            dist = Math.toDegrees(dist);
            dist = dist * 60 * 1.1515;
            return (dist * 1.609344);
        }
    }

    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 1: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                } else {
                    // permission denied
                }
                return;
            }

        }
    }



private class DataDownload extends AsyncTask<String, Void, String[]> {

        public Location getLocation() {

            locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            if (checkLocationPermission()) {
                location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

            } else {
                Log.d("Permissions", "Permissions were not granted, not updating the map");
                //"Permissions not granted";
                return null;
            }
            return location;
        }
        @Override
        protected void onPreExecute() {
            super.onPreExecute();

        }

        @RequiresApi(api = Build.VERSION_CODES.CUPCAKE)
        @Override
        protected String[] doInBackground(String... params) {
            if (!isCancelled()) {
                //extras = getIntent().getExtras();
                Bundle extras = getIntent().getExtras();
                String location = extras.getString("LOCATION_DATA");
                String response;
                location = location.substring(1, (location.length() - 1)); //0
                Log.d("Location", "Location is:" + location);
                location = location.substring(1);
                location = location.replace("\n", "");
                lat = getLocation().getLatitude();
                lng = getLocation().getLongitude();

                if (!currentLocation.equals(location)) {
                    currentLocation = location;
                    if (lat == 0.0 || lng == 0.0) {
                        Log.d("Location", "Origin location not known! Is the GPS on? Lat & Long were 0.0, 0.0");
                        return null;
                    } else
                    {
                    try {
                        response = getDirections("https://maps.googleapis.com/maps/api/directions/json?origin="+ lat + "," + lng + "&destination="+location+"&key="+API_KEY);
                        return new String[]{response};
                    } catch (Exception e) {
                        return new String[]{"error"};
                    }
                    }


                } else {
                    return null;
                }
            }
            return null;

        }

        @Override
        protected void onPostExecute(String... result) {

                    super.onPostExecute(result);
                    ParserTask parserTask = new ParserTask();
                    parserTask.execute(result);
        }
    public String getDirections(String requestURL) throws IOException {
        String data = "";
        InputStream iStream = null;
        HttpURLConnection urlConnection = null;
        try {
            URL url = new URL(requestURL);
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setReadTimeout(3000);
            urlConnection.setConnectTimeout(3000);
            urlConnection.setRequestMethod("GET"); //http GET
            urlConnection.setUseCaches(false);
            urlConnection.setAllowUserInteraction(false);
            urlConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            int responseCode = urlConnection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                // Reading data from url
                iStream = urlConnection.getInputStream();
                BufferedReader br = new BufferedReader(new InputStreamReader(iStream));
                StringBuffer sb = new StringBuffer();
                String line = "";
                while ((line = br.readLine()) != null) {
                    sb.append(line);
                }
            data = sb.toString();
            br.close();
            }else {
                data = "";
                Log.d("Response", "Responsecode was " + responseCode);
            }
        } catch (Exception e) {
            Log.d("Exception", e.toString());
        } finally {
            iStream.close();
            urlConnection.disconnect();
        }
        return data;
    }

}
    /***************************************************************************************
     *    Title: Drawing Route Between Two Points Using Google Map
     *    Author: Subba Raju
     *    Date: 2018
     *    Availability: https://medium.com/@sraju432/drawing-route-between-two-points-using-google-map-ab85f4906035
     ***************************************************************************************/
                @RequiresApi(api = Build.VERSION_CODES.CUPCAKE)
                private class ParserTask extends AsyncTask<String, Integer, List<List<HashMap<String, String>>>> {
                    @Override
                    protected List<List<HashMap<String, String>>> doInBackground(String... jsonData) {

                        JSONObject jObject;
                        List<List<HashMap<String, String>>> routes = null;

                        try {
                            jObject = new JSONObject(jsonData[0]);
                            Log.d("ParserTask", jsonData[0].toString());
                            DataParser parser = new DataParser();
                            Log.d("ParserTask", parser.toString());

                            // Starts parsing data
                            routes = parser.parse(jObject);
                            Log.d("ParserTask", "Executing routes");
                            Log.d("ParserTask", routes.toString());

                        } catch (Exception e) {
                            Log.d("ParserTask", e.toString());
                            e.printStackTrace();
                        }
                        return routes;
                    }

                    // Executes in UI thread, after the parsing process
                    @Override
                    protected void onPostExecute(List<List<HashMap<String, String>>> result) {
                        ArrayList<LatLng> points;
                        PolylineOptions lineOptions = null;

                        // Traversing through all the routes
                        for (int i = 0; i < result.size(); i++) {
                            points = new ArrayList<>();
                            lineOptions = new PolylineOptions();

                            // Fetching i-th route
                            List<HashMap<String, String>> path = result.get(i);

                            // Fetching all the points in i-th route
                            for (int j = 0; j < path.size(); j++) {
                                HashMap<String, String> point = path.get(j);

                                double lat = Double.parseDouble(point.get("lat"));
                                double lng = Double.parseDouble(point.get("lng"));
                                LatLng position = new LatLng(lat, lng);

                                points.add(position);
                            }

                            // Adding all the points in the route to LineOptions
                            lineOptions.addAll(points);
                            lineOptions.width(10);
                            lineOptions.color(Color.RED);

                            Log.d("onPostExecute", "onPostExecute lineoptions decoded");

                        }

                        // Drawing polyline in the Google Map for the i-th route
                        if (lineOptions != null) {
                            mMap.addPolyline(lineOptions);

                        } else {
                            Log.d("onPostExecute", "without Polylines drawn");
                        }
                    }

                }





    private class DataParser {
        /** Receives a JSONObject and returns a list of lists containing latitude and longitude */
        public List<List<HashMap<String,String>>> parse(JSONObject jObject){

            List<List<HashMap<String, String>>> routes = new ArrayList<>() ;
            JSONArray jRoutes;
            JSONArray jLegs;
            JSONArray jSteps;

            try {

                jRoutes = jObject.getJSONArray("routes");

                /** Traversing all routes */
                for(int i=0;i<jRoutes.length();i++){
                    jLegs = ( (JSONObject)jRoutes.get(i)).getJSONArray("legs");
                    List path = new ArrayList<>();

                    /** Traversing all legs */
                    for(int j=0;j<jLegs.length();j++){
                        jSteps = ( (JSONObject)jLegs.get(j)).getJSONArray("steps");

                        /** Traversing all steps */
                        for(int k=0;k<jSteps.length();k++){
                            String polyline = "";
                            polyline = (String)((JSONObject)((JSONObject)jSteps.get(k)).get("polyline")).get("points");
                            List<LatLng> list = decodePoly(polyline);

                            /** Traversing all points */
                            for(int l=0;l<list.size();l++){
                                HashMap<String, String> hm = new HashMap<>();
                                hm.put("lat", Double.toString((list.get(l)).latitude) );
                                hm.put("lng", Double.toString((list.get(l)).longitude) );
                                path.add(hm);
                            }
                        }
                        routes.add(path);
                    }
                }

            } catch (JSONException e) {
                e.printStackTrace();
            }catch (Exception e){
            }


            return routes;
        }


        /***************************************************************************************
        *    Title: Method to decode polyline points source code
        *    Author: Sambells, J
        *    Date: 2010
        *    Availability: https://jeffreysambells.com/2010/05/27/decoding-polylines-from-google-maps-direction-api-with-java
        *
        ***************************************************************************************/
        private List<LatLng> decodePoly(String encoded) {

            List<LatLng> poly = new ArrayList<>();
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

                LatLng p = new LatLng((((double) lat / 1E5)),
                        (((double) lng / 1E5)));
                poly.add(p);
            }

            return poly;
        }
    }

    }