package com.example.cliff.locationgetter;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleMap.OnMapLongClickListener {

    private GoogleMap mMap;

    // Prevent camera from updating while in use
    private boolean hasShownInitialPoint = false;

    // User-location related objects
    LocationManager locationManager;
    LocationListener locationListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    // Operations to take place once the map is loaded
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setOnMapLongClickListener(this);

        Intent intent = getIntent();
        final int index = intent.getIntExtra("index", -1);

        // This class provides access to the system location services
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

        // Used for receiving notifications from the LocationManager when the location has changed
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {

                // Markers will not update after displayed for the first time
                if (!hasShownInitialPoint) {
                    if (index == 0) { // Place a marker on the user's location
                        LatLng userLocation = new LatLng(location.getLatitude(), location.getLongitude());
                        String addressName = buildAddressName(userLocation);

                        mMap.addMarker(new MarkerOptions().position(userLocation).title("You are here: " + addressName));
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 15));// Zoom range 2.0 - 21.0
                    } else { // Place a marker at the chosen address
                        LatLng coordinates = new LatLng(MainActivity.locations.get(index).getLatitude(), MainActivity.locations.get(index).getLongitude());
                        mMap.addMarker(new MarkerOptions().position(coordinates).title(MainActivity.places.get(index)));
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(coordinates, 15));
                    }
                    hasShownInitialPoint = true;
                }
            }
            @Override
            public void onStatusChanged(String s, int i, Bundle bundle) {}
            @Override
            public void onProviderEnabled(String s) {}
            @Override
            public void onProviderDisabled(String s) {}
        };

        // Permissions check for getting users location
        if (Build.VERSION.SDK_INT < 23) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 1, locationListener);
        } else {
            // If don't have permission...
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            }
            else {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 1, locationListener);
            }
        }
    }

    // Called when ActivityCompat.requestPermissions()
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 1 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                {
                    // Register the LocationListener with the LocationManager
                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                            5000, // minimum time interval between location updates, in milliseconds
                            1,    // minimum distance between location updates, in meters
                            locationListener);

                }
            }
        }
    }

    // Display a location on the map, as well as save its address to memory
    @Override
    public void onMapLongClick(LatLng point) {

        String addressName = buildAddressName(point);

        // Add the location, update the listView, and save to memory
        MainActivity.addLocation(addressName, point.latitude, point.longitude);
        MainActivity.saveLocations();

        mMap.addMarker(new MarkerOptions()
                .position(point)
                .title(addressName)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA)));

        Toast.makeText(this, "Location saved!", Toast.LENGTH_SHORT).show();
    }

    // Use Geocoder and a List<Address> to get details about LatLng point
    public String buildAddressName(LatLng point) {
        String result = "";

        // A Locale if the format for the address
        Geocoder gc = new Geocoder(getApplicationContext(), Locale.getDefault());
        try {
            List<Address> addresses;
            addresses = gc.getFromLocation(point.latitude, point.longitude, 1); // Return 1 result
            if(addresses != null && addresses.size() > 0){
                Address address = addresses.get(0);

                // Build addressName
                if (address.getSubThoroughfare() != null && address.getThoroughfare() != null) {
                    result += address.getSubThoroughfare() + " " + address.getThoroughfare() + " ";
                }
                if (!result.equals("")) {
                    if (address.getLocality() != null) {
                        result += address.getLocality() + " ";
                    }
                    if (address.getPostalCode() != null) {
                        result += address.getPostalCode();
                    }
                }
                else { // Show date and time instead
                    result = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Calendar.getInstance().getTime());
                }
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

    // Add options menu to remove all the markers or display all the markers from the saved locations
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {

            case R.id.show:
                for (int i = 1; i < MainActivity.locations.size(); i++) {
                    LatLng coordinates = new LatLng(MainActivity.locations.get(i).getLatitude(), MainActivity.locations.get(i).getLongitude());
                    mMap.addMarker(new MarkerOptions().position(coordinates).title(MainActivity.places.get(i)));
                }
                break;

            case R.id.remove:
                mMap.clear();
                break;

            default:
                break;
        }

        return super.onOptionsItemSelected(item);
    }

}