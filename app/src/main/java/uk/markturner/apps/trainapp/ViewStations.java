package uk.markturner.apps.trainapp;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class ViewStations extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_stations); //Same layout as Saved Trains
        ListView mStationsList = (ListView) findViewById(R.id.stationsList);
        final ArrayList<String> stations = new ArrayList<>();
        try {
            InputStream is = getAssets().open("station_codes.csv"); //Read in the CSV file as InputStream
            BufferedReader reader = new BufferedReader(new InputStreamReader(is)); //BufferedReader, as with ViewSavedTrains
            String line;
            while ((line = reader.readLine()) != null) { //until EOF
                stations.add(line.substring(0, line.indexOf(",")) + " - " + line.substring(line.indexOf(",") + 1)); //Get each line, replacing the comma with '-'
            }
        } catch (Exception e) {
            Toast.makeText(ViewStations.this, "Could not read stations file",Toast.LENGTH_LONG).show(); //General error
        }
        stations.remove(0); //Remove first line of CSV file
        ArrayAdapter adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, stations);
        mStationsList.setAdapter(adapter);  //Add array to stationsList ListView
        mStationsList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapter, View v, int position, long id) { //Click listener on each item
                String station = stations.get(position).substring(stations.get(position).indexOf("-")+2); //Get current pos (+2 for alignment)
                Intent data = new Intent(); //create a new intent
                data.putExtra("TOSEARCH",station); //Add the station the user has selected to it
                setResult(RESULT_OK,data); //Set this intent as the activity result to be passed back up to MainActivity
                finish(); //Close this activity
            }
        });
    }
}

