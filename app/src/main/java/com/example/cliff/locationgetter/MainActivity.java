package com.example.cliff.locationgetter;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    ListView listView;

    static List<Location> locations = new ArrayList<>();
    static SharedPreferences sp;

    static ArrayList<String> places = new ArrayList<>();
    static ArrayAdapter<String> arrayAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Set up the ListView
        listView = (ListView) findViewById(R.id.listView);
        arrayAdapter =
                new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, places);
        listView.setAdapter(arrayAdapter);

        // Retrieve data with the Gson dependency by converting List<Location> into a JSON representation
        sp = this.getSharedPreferences("com.example.cliff.locationgetter", Context.MODE_PRIVATE);
        retrieveLocations();

        // update the ListView depending on the SharedPreferences data
        if (locations == null) {
            locations = new ArrayList<>();
            locations.add(new Location("Add a location...", 91.0, 181.0));
            updatePlaces();
        }
        else {
            updatePlaces();
        }

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(getApplicationContext(), MapsActivity.class);
                intent.putExtra("index", position);
                startActivity(intent);
            }
        });

        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int i, long id) {

                if (i == 0) {
                    Toast.makeText(getApplicationContext(), "You don't want to do that!", Toast.LENGTH_LONG).show();
                    return true;
                }

                final int itemToDelete = i;

                new AlertDialog.Builder(MainActivity.this)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setTitle("Are you sure?")
                        .setMessage("Do you want to delete this location?")
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                locations.remove(itemToDelete);
                                places.remove(itemToDelete);
                                arrayAdapter.notifyDataSetChanged();

                                saveLocations();
                            }
                        })
                        .setNegativeButton("No", null)
                        .show();
                return true;
            }
        });

    }

    // Update ArrayList<String> places to match the loaded ArrayList<Locations> locations
    public static void updatePlaces() {
        places.clear();
        places.add("Add a location...");

        for (int i = 1; i < locations.size(); i++) {
            places.add(locations.get(i).getPlace());
        }
        arrayAdapter.notifyDataSetChanged();
    }

    // Called from MapsActivity.java when a new location is stored
    public static void addLocation (String address, double latitude, double longitude) {
        locations.add(new Location(address, latitude, longitude));
        places.add(address);
        arrayAdapter.notifyDataSetChanged();
    }

    // Retrieve ArrayList<Locations> locations from memory
    public static void retrieveLocations() {
        Gson gson = new Gson();
        String response = sp.getString("locations" , "");

        // Return the Type representing the direct superclass of the entity
        Type type = new TypeToken<List<Location>>(){}.getType();
        locations = gson.fromJson(response, type);
    }

    // Save ArrayList<Locations> to memory
    public static void saveLocations() {
        // Convert the ArrayList to a Json-formatted string using the Gson dependency, which handles generic Lists
        SharedPreferences.Editor prefsEditor = sp.edit();
        Gson gson = new Gson();
        String json = gson.toJson(MainActivity.locations);
        prefsEditor.putString("locations", json);
        prefsEditor.apply();
    }
}
