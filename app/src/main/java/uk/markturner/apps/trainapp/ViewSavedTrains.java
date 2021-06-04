package uk.markturner.apps.trainapp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.jdom2.input.SAXBuilder;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.ArrayList;

public class ViewSavedTrains extends AppCompatActivity implements LoaderManager.LoaderCallbacks<String> {

    private ArrayList<String> trainIDs;
    private ArrayList<String> stations;
    private ArrayList<String> mode;
    private int current;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_saved_trains);
        ListView mTrainsList = (ListView) findViewById(R.id.savedTrainsList);
        stations = new ArrayList<>(); //Initialise the ArrayLists
        trainIDs = new ArrayList<>();
        mode = new ArrayList<>();
        try {
            InputStream is = getApplicationContext().openFileInput("trains.txt"); //Open trains.txt from applciation storage
            BufferedReader reader = new BufferedReader(new InputStreamReader(is)); //Read it in as a BufferedReader
            String line; //Initialise line
            int number = 1; //Alternating number start
            while ((line = reader.readLine()) != null) { //Until EOF...
                if (number == 1) { //Every 1st line is the train ID
                    trainIDs.add(line); //Add to the trainID array
                } else if (number == 2) { //Every 2nd line is the stations information
                    stations.add(line); //Add this to the stations array
                } else {
                    mode.add(line); //Every third line is the mode (departure or arrival).
                } //Add this to the mode array
                number++; //Increment the number after each line
                if (number>3){ //If this number ever goes over 3
                    number = 1; //it must be reset to 1
                }
            }
            is.close(); //Close the InputStream
        } catch (Exception e) {
            Toast.makeText(ViewSavedTrains.this, "File could not be opened. Check permissions.",Toast.LENGTH_LONG).show();
        } //Required catch -  displays an error to the user

        ArrayAdapter adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, stations);
        mTrainsList.setAdapter(adapter); //Attached list of stations to the ListView
        mTrainsList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapter, View v, int position, long id) { //Each item has a click listener
                current = position; //Converts to current (global variable)
                new AlertDialog.Builder(ViewSavedTrains.this) //When a train is clicked on
                        .setTitle("View or Delete?") //A dialogue box appears
                        .setMessage("Would you like to View or Delete this train?")
                        .setPositiveButton("View", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) { //Positive (right hand side)
                                try {
                                    loadTrain(trainIDs.get(current)); //Pass the train's unique ID to the load train function
                                }
                                catch (Exception e){
                                    Toast.makeText(ViewSavedTrains.this, "An error has occurred", Toast.LENGTH_LONG).show();
                                } //General error catch
                            }
                        })
                        .setNegativeButton("Delete", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) { //Negative (left hand side)
                                delete(trainIDs.get(current));
                            }
                        })
                        .show();
            }
        });
        if(stations.isEmpty()){ //If there are no stations, there must be nothing in the .txt file
            new AlertDialog.Builder(ViewSavedTrains.this)
                    .setTitle("No Saved Trains") //The user is alerted to this via a dialogue box
                    .setMessage("Save trains here by viewing them, and pressing 'Save' at the top right.")
                    .setPositiveButton("Okay", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            finish();
                        }
                    }).show();
        }
    }

    public void loadTrain(String ID){
        ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = null;
        if (connMgr != null) {
            networkInfo = connMgr.getActiveNetworkInfo();
        }
        if (networkInfo != null && networkInfo.isConnected() && ID.length() != 0) { //Same connectivity as in MainActivity, with ID.length replacing the user input
            Bundle queryBundle = new Bundle();
            queryBundle.putString("queryString", ID);
            getSupportLoaderManager().restartLoader(0, queryBundle, ViewSavedTrains.this);
        } else {
            Toast.makeText(ViewSavedTrains.this,"Please check your network connection.",Toast.LENGTH_LONG).show();
        }
    }

    public void delete(String idString){
        try {
            InputStream is = getApplicationContext().openFileInput("trains.txt");
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            String line; //Same start as before
            StringBuilder sb = new StringBuilder();
            int deleted = 0;
            boolean done = false;
            while ((line = reader.readLine()) != null) {
                if (!line.equals(idString) && deleted == 0) { //If line DOES NOT equal the ID of the train the user wishes to delete
                    sb.append(line + "\n"); //It can be added onto the new String
                }
                else if (!done){ //If it does equal, done represents whether a train has already been deleted or not. If not,
                    deleted++; //Add 1 to the deleted variable (lines will always be deleted in 3s)
                    if (deleted > 2){ //If this is 3 (greater than 2)
                        done = true; //Prevents from going back
                        deleted = 0; //Reset the counter
                    }
                }
                else {
                    sb.append(line+"\n"); //Although the ID matches, a train has already been deleted, so this can be added to the string
                }
            }
            is.close();
            FileOutputStream fos = getApplicationContext().openFileOutput("trains.txt", Context.MODE_PRIVATE);
            fos.write(sb.toString().getBytes()); //Writes the new string to the file. MODE_PRIVATE means that this will completely overwrite the file
            fos.close();
            Intent intent = getIntent(); //Get the current intent
            finish(); //Close the intent
            startActivity(intent); //Restart it to apply changes

        } catch (Exception e){
            Toast.makeText(ViewSavedTrains.this, "Error deleting.", Toast.LENGTH_LONG).show(); //General error catch
        }
    }

    public ArrayList<String> postLoad(String data, String modeString, String IDString, int caller){
        SAXBuilder saxBuilder = new SAXBuilder(); //The following is almost identical to that in MainActivity
        try {
            Document document = saxBuilder.build(new StringReader(data));
            Namespace namespace = Namespace.getNamespace("http://thalesgroup.com/RTTI/2017-10-01/ldb/types");
            final Element trainServices = document.getRootElement().getChildren().get(0).getChildren().get(0).getChildren().get(0);
            final Element locationName = document.getRootElement().getChildren().get(0).getChildren().get(0).getChildren().get(0).getChildren().get(2);
            String calling;
            String onTime;
            if (modeString.equals("0")) {
                calling = trainServices.getChild("subsequentCallingPoints", namespace).getValue();
                onTime = trainServices.getChild("etd", namespace).getValue();
            } else {
                calling = trainServices.getChild("previousCallingPoints", namespace).getValue();
                onTime = trainServices.getChild("eta", namespace).getValue();
            }
            if (!onTime.contains("On time")) {
                onTime = "False";
            } else {
                onTime = "True";
            }
            String platform;
            try {
                platform = trainServices.getChild("platform", namespace).getValue();
            } catch (NullPointerException e) {
                platform = "-";
            }
            String time;
            if (modeString.equals("0")) {
                time = trainServices.getChild("std", namespace).getValue();
            } else {
                time = trainServices.getChild("sta", namespace).getValue();
            }
            String origin = locationName.getValue();
            ArrayList<String> toIntent = new ArrayList<>();
            toIntent.add(platform); //This allows for modularity. The intent will be constructed from these later
            toIntent.add(time);
            toIntent.add(calling);
            toIntent.add(origin);
            toIntent.add(onTime);
            toIntent.add(IDString);
            toIntent.add(modeString);
            return toIntent; //returns the almost-created intent in the form of an ArrayList
        }
        catch (Exception e){
            if (caller == 0) {
               onError(); //If ViewSavedTrains called this function, show Toast. Otherwise, an error would occur
            }
        }
        return new ArrayList<>(); //Returns an empty array if catch is activated, regardless of calling function
    }

    public void toIntent(ArrayList<String> list){ //This function iterates through the ArrayList, adding the functions to the ViewTrainInfo intent
        Intent intent = new Intent(this, ViewTrainInfo.class);
        intent.putExtra("PLATFORM",list.get(0));
        intent.putExtra("TIME",list.get(1));
        intent.putExtra("CALLING",list.get(2));
        intent.putExtra("ORIGIN",list.get(3));
        intent.putExtra("ONTIME",list.get(4));
        intent.putExtra("ID",list.get(5));
        intent.putExtra("MODE",list.get(6));
        finish(); //This means that when the back button is pressed, MainActivity will be loaded, and not the Saved Trains List
        startActivity(intent);
    }

    public void onError(){ //Errors occur because the train has already left / arrived at the station. They are automatically deleted
        String message;
        if (mode.get(current).equals("0")){
            message = "This train has already left the station. Deleting...";
        } else {
            message = "This train has already arrived at the station. Deleting...";
        }
        new AlertDialog.Builder(ViewSavedTrains.this)
                .setTitle("Cannot display train info.")
                .setMessage(message)
                .setPositiveButton("Okay", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        delete(trainIDs.get(current));
                    }
                }).show();
    }

    @Override
    public void onResume(){
        super.onResume();
    }

    @NonNull
    @Override
    public Loader<String> onCreateLoader(int id, @Nullable Bundle args) {
        String queryString = "";
        if (args != null) {queryString = args.getString("queryString");}
        return new TrainLoader(this, queryString,2); //Same loader as MainActivity, but type = 2, not mode 1 / 2 as this is specific ID lookup
    }

    @Override
    public void onLoadFinished(@NonNull Loader<String> loader, final String data) {
            try {
                toIntent(postLoad(data, mode.get(current), trainIDs.get(current), 0)); //Rolls all previous functions into one line
            } catch (IndexOutOfBoundsException e){
                onError(); //IndexOutOfBounds means that the train no longer exists
            }
    }

    @Override
    public void onLoaderReset(@NonNull Loader<String> loader){

    }
}
