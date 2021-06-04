package uk.markturner.apps.trainapp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.text.Html;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.*;
import java.util.ArrayList;

import org.jdom2.*;
import org.jdom2.input.SAXBuilder;

import static org.jdom2.Namespace.getNamespace;

public class MainActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<String> {

    private EditText mStationInput;
    private TextView mStationText;
    private ListView mTrainsList;
    private TextView mLegal;
    public String queryString;
    private String destList;
    private String etd;
    private int mode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mStationInput = (EditText) findViewById(R.id.stationInput);
        mStationText = (TextView) findViewById(R.id.titleText);
        mTrainsList = (ListView) findViewById(R.id.trainsList);
        mLegal = (TextView) findViewById(R.id.legal);
        try{
            mode = Integer.parseInt(getIntent().getStringExtra("MODE")); //MODE distinguishes between Arrival and Departures Board
        }
        catch (NumberFormatException e){mode = 0;} //If an error occurs for whatever reason, or mode has not been set (first run) the default MODE is 0
        if (mode == 1){
            setTitle(getResources().getString(R.string.arrivals_board)); //If it's 1, change the title to Arrivals Board
        }
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState){
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu){
        if (mode == 1){
            menu.findItem(R.id.arrivalsBoard).setTitle("Departures Board"); //Swap the menu items and title if mode = 1
        }
        return super.onPrepareOptionsMenu(menu); //Rest of the code
    }


    public void searchStation(View view) { //Activated when button is pressed
        mLegal.setVisibility(View.INVISIBLE);
        queryString = mStationInput.getText().toString(); //Convert what the user has typed in to a string
        mTrainsList.setAdapter(null); //Remove the list of previous search results from view
        InputMethodManager inputManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);

        if (inputManager != null) {
            inputManager.hideSoftInputFromWindow(view.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
        }
        ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = null;
        if (connMgr != null) { //If there is no information on the service
            networkInfo = connMgr.getActiveNetworkInfo(); //Get it
        }
        if (networkInfo != null && networkInfo.isConnected() && queryString.length() != 0) { //If there is both internet and input
            Bundle queryBundle = new Bundle();
            queryBundle.putString("queryString", queryString); //Pass queryString into the bundle
            getSupportLoaderManager().restartLoader(mode, queryBundle, this);
            mStationText.setText(R.string.loading); //Intermediary text
        } else {
            if (queryString.length() == 0) { //If the user didn't enter anything:
                mStationText.setText(R.string.no_search_term);
            } else { //If the connectivity failed in the if statement but entered text
                mStationText.setText(R.string.no_network);
            }
        }
    }

    @NonNull
    @Override
    public Loader<String> onCreateLoader(int id, @Nullable Bundle args) {
        String queryString = ""; //Initialise string
        if (args != null) {queryString = args.getString("queryString");}
        return new TrainLoader(this, queryString,mode); //Send the string and the activity's mode to the loader
}

    @Override
    public void onLoadFinished(@NonNull Loader<String> loader, final String data) {
        SAXBuilder saxBuilder = new SAXBuilder();
        try {
            mLegal.setVisibility(View.VISIBLE);
            Document document = saxBuilder.build(new StringReader(data)); //Convert returned data to a document ready for parsing
            final Element locationName = document.getRootElement().getChildren().get(0).getChildren().get(0).getChildren().get(0).getChildren().get(1);
            Element crs = document.getRootElement().getChildren().get(0).getChildren().get(0).getChildren().get(0).getChildren().get(2);
            final Element trainServices = document.getRootElement().getChildren().get(0).getChildren().get(0).getChildren().get(0).getChildren().get(5);
            mStationText.setText(crs.getValue() + " - " + locationName.getValue()); //Hard-coded array indexes to get the information to make up the title
            ArrayList<android.text.Spanned> services = new ArrayList<>(); //Initialise list of services
            final ArrayList<String> destLists = new ArrayList<>(); //Initialise list of destinations
            final ArrayList<String> ontime = new ArrayList<>(); //Initialise the on-time status of trains as an array
            final Namespace namespace2015 = getNamespace("http://thalesgroup.com/RTTI/2015-11-27/ldb/types"); //Namespace for easier referencing
            for (int i = 0; i < trainServices.getChildren().size(); i++) {
                final StringBuilder trainService = new StringBuilder();
                if (mode == 0) { //If Destination Board..
                    destList = trainServices.getChildren().get(i).getChild("destination", getNamespace("http://thalesgroup.com/RTTI/2016-02-16/ldb/types")).getValue();
                } else { //Get the destination information, otherwise, get the origin information (this will avoid errors)
                    destList = trainServices.getChildren().get(i).getChild("origin", getNamespace("http://thalesgroup.com/RTTI/2016-02-16/ldb/types")).getValue();
                } //Write the time to the list of trainServices
                trainService.append(" " + trainServices.getChildren().get(i).getChildren().get(0).getValue() + "   -   ");
                if (destList.contains("via")) { //If the destination contains "via"...
                    destList = destList.substring(0, destList.indexOf("via") - 3) + " " + destList.substring(destList.indexOf("via")); //Cut off 2 station codes
                    trainService.append(destList); //Add this to the list of services
                    destLists.add(destList.substring(0, destList.indexOf("via") - 1)); //Trim
                } else {
                    destList = destList.substring(0, destList.length() - 3); //Cut off just the one station code
                    trainService.append(destList); //Add this to the list of services
                    destLists.add(destList);
                }
                try {
                    String cancelReason = trainServices.getChildren().get(i).getChild("cancelReason", namespace2015).getValue(); //If there is a cancellation, sometimes a reason is listed. This will be priority
                    ontime.add("False"); //If it is cancelled, it is obviously not on time
                    services.add(Html.fromHtml(trainService.toString() + "<br><font color=\"#FF0000\"><i>" + cancelReason + "</i></font>")); //Write the reason to the screen in red on a new line
                } catch (NullPointerException e) { //If there is no cancellation reason...
                    if (mode == 0) { //If Destination Board...
                        etd = trainServices.getChildren().get(i).getChild("etd", namespace2015).getValue(); //Get Estimated Time for Departure
                    } else { //If Arrivals Board...
                        etd = trainServices.getChildren().get(i).getChild("eta",namespace2015).getValue(); //Get Estimated Time for Arrival
                    }
                    if (!etd.contains("On time")) { //If the time does not contain the words 'on time'
                        if (etd.contains("Cancelled")) { //If the train is cancelled
                            services.add(Html.fromHtml(trainService.toString() + "<br><font color=\"#FF0000\">&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<i>Cancelled - Reason Unknown</i></font>"));
                            ontime.add("False"); //Write cancelled in red (&nbsp; allows for whitespace in HTML)
                        } else if (etd.contains("Delayed")) { //If it contains delayed...
                            services.add(Html.fromHtml(trainService.toString() + "<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<font color=\"#FF0000\"><i>Delayed</i></font>"));
                            ontime.add(etd); //Write delayed in red and add the time to the etd
                        } else { //Otherwise
                            services.add(Html.fromHtml(trainService.toString() + "<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<i><font color=\"#FF0000\">Expected "+etd+"</i></font>"));
                            ontime.add(etd); //Write the time expected in red and add to the etd
                        }
                    } else {
                        ontime.add("True"); //Otherwise it must be on time
                        services.add(Html.fromHtml(trainService.toString())); //Add this train as normal to services
                    }
                }

                ArrayAdapter adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, services);
                mTrainsList.setAdapter(adapter);
                mTrainsList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> adapter, View v, int position, long id) { //Listener for when an item in the array is clicked...
                        try {
                            Intent intent = new Intent(MainActivity.this, ViewTrainInfo.class); //Init Intent object with the ViewTrainInfo class
                            try {
                                intent.putExtra("PLATFORM", trainServices.getChildren().get(position).getChild("platform", namespace2015).getValue());
                            } catch (NullPointerException e) { //Try and put the platform in the Extra
                                intent.putExtra("PLATFORM", "-"); //If the platform doesn't exist, it is unannounced, so replace with "-"
                            } catch (Exception e){
                                onError(); //Catches any other error
                            }
                            if (mode == 0) { //If Departures Board...
                                intent.putExtra("TIME", trainServices.getChildren().get(position).getChild("std", namespace2015).getValue());
                            } else { //Get the standard departure time
                                intent.putExtra("TIME", trainServices.getChildren().get(position).getChild("sta", namespace2015).getValue());
                            } //Otherwise get the standard arrival time
                            if (mode == 0) { //If Departures Board...
                                intent.putExtra("CALLING", trainServices.getChildren().get(position).getChild("subsequentCallingPoints", Namespace.getNamespace("http://thalesgroup.com/RTTI/2017-10-01/ldb/types")).getValue());
                            } else { //Put the subsequent calling points as Extra
                                intent.putExtra("CALLING", trainServices.getChildren().get(position).getChild("previousCallingPoints", Namespace.getNamespace("http://thalesgroup.com/RTTI/2017-10-01/ldb/types")).getValue());
                            } //If Arrivals board, put the previous calling points
                            intent.putExtra("ORIGIN", locationName.getValue()); //Origin was already saved for the title - use that
                            intent.putExtra("DESTINATION", destLists.get(position)); //Destinations were added along the way
                            intent.putExtra("ONTIME", ontime.get(position)); //Same with ontime status
                            intent.putExtra("ID", trainServices.getChildren().get(position).getChild("serviceID", namespace2015).getValue());
                            intent.putExtra("MODE",Integer.toString(mode)); //ID is grabbed from the Document and Mode is grabbed from the Intent
                            startActivity(intent); //Begin
                        } catch (Exception e){Toast.makeText(MainActivity.this,"An error has occured",Toast.LENGTH_LONG).show();} //Generic error catching
                    }
                });
            }
        } catch (IndexOutOfBoundsException e) { //If an array exists with length of less than 1
            mStationText.setText(Html.fromHtml(getResources().getString(R.string.no_trains))); //Then there must be no trains currently published
        } catch (Exception e) {
            onError(); //Catches general errors
        }
    }

    @Override
    public void onLoaderReset(@NonNull Loader<String> loader) {

    }

    public void onError(){ //If there is a general error
        ArrayAdapter adapter = new ArrayAdapter<>(this,android.R.layout.simple_list_item_1, new ArrayList<String>());
        mTrainsList.setAdapter(adapter);
        mStationText.setText(Html.fromHtml(getResources().getString(R.string.wrong_code))); //It's probably because the station does not exist
    }

    public boolean onCreateOptionsMenu(Menu menu){ //Just inflates the menu
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.activitymain_items, menu);
        return true;

    }

    public boolean onOptionsItemSelected(MenuItem item){
        Intent intent;
        if (item.getItemId() == R.id.view_stations) { //If the view stations menu item is selected
            intent = new Intent(MainActivity.this, ViewStations.class); //create this intent
        } else if (item.getItemId() == R.id.savedTrainsList) { //If the Saved Trains List is selected...
            intent = new Intent(MainActivity.this, ViewSavedTrains.class); //create this intent
        } else if (item.getItemId() == R.id.view_about){ //If About is selected...
            intent = new Intent(MainActivity.this, About.class); //create this intent
        } else { //Otherwise it must be the Arrivals / Departures Board
            intent = new Intent(MainActivity.this, MainActivity.class); //MainActivity again
            if (mode == 0) { //But if the mode is 0
                intent.putExtra("MODE", "1"); //Flip it to 1
            } else { //Or if it is 1
                intent.putExtra("MODE","0"); //Flip it to 0
            } //Add as extra to the intent
        }
        startActivityForResult(intent, 0); //start the intent, for result so if the user clicks a station it will callback

    return true;}

    @Override
   protected void onActivityResult(int requestCode, int resultCode, Intent data){
       try {
           if (data.hasExtra("TOSEARCH")) { //If the callback comes with data
               mStationInput.setText(data.getStringExtra("TOSEARCH")); //Put this data (Station name) into the textbox
                findViewById(R.id.searchButton).performClick(); //"Presses" the search button
           }
       } catch (Exception e) {} //If there is no data, just ignore it.
    }
}
