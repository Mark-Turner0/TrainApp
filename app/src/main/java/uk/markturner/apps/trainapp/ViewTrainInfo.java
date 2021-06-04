package uk.markturner.apps.trainapp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.provider.AlarmClock;
import android.text.Html;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.Integer.parseInt;

public class ViewTrainInfo extends AppCompatActivity implements LoaderManager.LoaderCallbacks<String>{

    public TextView mPlatform;
    public TextView mTime;
    public TextView mLegal;
    public ListView mCalling;
    public String ID;
    public String mode;
    public ArrayList<String> callingArray;
    public ArrayList<String> timesArray;
    public ArrayList<String> onTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_train_info);
        mPlatform = findViewById(R.id.platform);
        mTime = findViewById(R.id.time);
        mCalling = findViewById(R.id.calling);
        Intent intent = getIntent();
        String platform = intent.getStringExtra("PLATFORM"); //Grabs all the information from the intents
        String time = intent.getStringExtra("TIME");
        String calling = intent.getStringExtra("CALLING");
        String origin = intent.getStringExtra("ORIGIN");
        String ontime = intent.getStringExtra("ONTIME");
        mode = intent.getStringExtra("MODE");
        ID = intent.getStringExtra("ID");

        if (mode.equals("1")) { //If this train is an Arrival
            ConstraintSet set = new ConstraintSet();
            ConstraintLayout layout = (ConstraintLayout) findViewById(R.id.infoLayout);
            set.clone(layout); //Copy the current layout into a new one
            set.clear(R.id.legal2, ConstraintSet.BOTTOM);
            set.connect(R.id.legal2, ConstraintSet.TOP, R.id.infoLayout, ConstraintSet.TOP);
            set.clear(R.id.calling, ConstraintSet.TOP);
            set.clear(R.id.calling, ConstraintSet.BOTTOM);
            set.connect(R.id.calling, ConstraintSet.TOP, R.id.legal2, ConstraintSet.BOTTOM); //Moves the calling stations right up to the top
            set.connect(R.id.calling, ConstraintSet.BOTTOM, R.id.time, ConstraintSet.TOP);
            set.clear(R.id.time, ConstraintSet.TOP);//Remove constraints for the header from the top,
            set.connect(R.id.time, ConstraintSet.BOTTOM, R.id.infoLayout, ConstraintSet.BOTTOM); //Move the header down to the bottom
            set.clear(R.id.platform, ConstraintSet.TOP);
            set.connect(R.id.platform, ConstraintSet.BOTTOM, R.id.infoLayout, ConstraintSet.BOTTOM);

            set.applyTo(layout); //Apply this to the new cloned layout which is now attatched to this intent
        }

        mPlatform.setText(mPlatform.getText().toString().replace("X",platform)); //Replace the "X" in the default text to the Platform number (or maybe -)
        mTime.setText(time); //Set the time header to the time (sta / std)
        if (!ontime.equals("True")){ //If the train is not on time
            if (!ontime.equals("False")) {
                    mTime.setText(ontime); //If a time is given, set the header time to that (distinction will be in the colour)
            } else if (ontime.equals("Delayed")){
                    mTime.setText("Delayed"); //If no time is given, set it to Delayed
            } else {
                mTime.setText("Cancelled"); //If the train is cancelled, set it to Cancelled
            }
            mTime.setTextColor(Color.parseColor("#ffcc0000")); //Change the colour of both header texts to red
            mPlatform.setTextColor(Color.parseColor("#ffcc0000"));
        }
        callingArray = new ArrayList<>();
        timesArray = new ArrayList<>();
        ArrayList<android.text.Spanned> displayArray = new ArrayList<>();
        timesArray.add(time);
        callingArray.add(origin);
        onTime = new ArrayList<>();
        onTime.add(""); //Make arrays line up with 1,2,3
        Matcher matcher = Pattern.compile("(\\D*)(\\d\\d:\\d\\d)(\\S\\S\\S?\\S?\\S?)").matcher(calling); //Uses regex to find time and station names in the whole calling string
        while (matcher.find()){ //For each matched statement
            if (matcher.group(1).contains(" time")) { //If the train is on time
                callingArray.add(matcher.group(1).substring(5, matcher.group(1).length() - 3)); //Use substrings to trim off the 3 letter station code and time
            } else if (matcher.group(1).startsWith("ed")){
                callingArray.add(matcher.group(1).substring(2,matcher.group(1).length()-3));
            } else if (matcher.group(1).startsWith("lled")) {
                callingArray.add(matcher.group(1).substring(4, matcher.group(1).length() - 3)); //Catches anomalies and shaves them off
            }
            else {
                callingArray.add(matcher.group(1).substring(0, matcher.group(1).length() - 3)); //If it's clean, just take 3 letters off the end
            }
            timesArray.add(matcher.group(2)); //Add the second set of () in the regex to the time array
            onTime.add(matcher.group(3)); //Add the third to the onTime statud array
        }
        for (int i=0;i<callingArray.size();i++){
            if (callingArray.get(i).startsWith("report")){
                callingArray.set(i,callingArray.get(i).substring(6)); //Cutting off an anomaly that happened once
            }
            if (ontime.equals("False")){ //False = Cancelled
                displayArray.add(Html.fromHtml(timesArray.get(i)+ " - "+ callingArray.get(i) + "<br><font color=\"#FF0000\"><i>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Cancelled</i></font>"));
            }
            else if (onTime.get(i).contains("On")||onTime.get(i).contains("No")){ //If it contains "On Time" or "No Delay" then display as normal
                displayArray.add(Html.fromHtml(timesArray.get(i)+ " - "+callingArray.get(i)));
            }
            else if (!onTime.get(i).equals("")) { //If no additional information, just display expected time in red
                displayArray.add(Html.fromHtml(timesArray.get(i) + " - " + callingArray.get(i) + "<br><font color=\"#FF0000\"><i>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Expected " + onTime.get(i) + "</i></font>"));
            }
        }
        if (mode.equals("0")) { //If departing train..
            try {
                setTitle(time + displayArray.get(displayArray.size() - 1).toString().substring(5, displayArray.get(displayArray.size() - 1).toString().indexOf("\n"))); //Set the title to the first element in the array and cut it off before newline
            } catch (StringIndexOutOfBoundsException e) {
                setTitle(time + displayArray.get(displayArray.size() - 1).toString().substring(5)); //If there is no newline, just display as normal
            }
        } else { //If arriving train ..
            try {
                setTitle(time + displayArray.get(0).toString().substring(5, displayArray.get(0).toString().indexOf("\n")).replace("-","from")); //Set the title to the last element in the array and cut it off before the newline
            } catch (StringIndexOutOfBoundsException e){
                setTitle(time + displayArray.get(0).toString().substring(5).replace("-","from")); //If there is no newline, just display as normal
            }
        }
        ArrayAdapter adapter = new ArrayAdapter<>(this,android.R.layout.simple_list_item_1, displayArray);
        mCalling.setAdapter(adapter);

        mCalling.setOnItemClickListener(new AdapterView.OnItemClickListener(){
            @Override
            public void onItemClick(AdapterView<?>adapter, View v, final int position, long id) { //Onclick listener for each destination
                final int preferenceInt = 5; //How many minutes before station arrival the alarm will be set
                new AlertDialog.Builder(ViewTrainInfo.this) //Display a dialogue box
                        .setTitle("Set alarm?")
                        .setMessage("An alarm will be set for "+preferenceInt+" minutes before the train is scheduled to arrive at "+callingArray.get(position+1))
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) { //If "yes" is pressed
                                Intent intent = new Intent(AlarmClock.ACTION_SET_ALARM); //Create intent for Android Alarm clock
                                int hours = parseInt(timesArray.get(position+1).substring(0,2)); //Get the first 2 digits of the time array at index selected
                                int minutes = parseInt(timesArray.get(position+1).substring(3,5))-preferenceInt; //Get last 2 digits, minus the 5 minutes
                                if (minutes<0){ //If minutes go into the negative with the minus...
                                    minutes = 60 + minutes; //Add the negative number to 60, so 60 + - x
                                    hours--; //minus 1 hour
                                    if (hours < 0){ //If the hours go negative
                                        hours = 24 + hours; //Again add the negative number to 24
                                    }
                                }
                                intent.putExtra(AlarmClock.EXTRA_HOUR,hours); //Add the extras for hours
                                intent.putExtra(AlarmClock.EXTRA_MINUTES,minutes); //and minutes to the intent
                                intent.putExtra(AlarmClock.EXTRA_MESSAGE,"You're "+preferenceInt+" minutes away from "+callingArray.get(position+1)+"!!!"); //
                                startActivity(intent); //Send the intent to any Android app that can set alarms
                            }
                        })
                        .setNegativeButton(android.R.string.no, null) //When no is pressed, the dialogue closes and nothing happens
                        .show();
            }
        });
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        try {
            InputStream is = getApplicationContext().openFileInput("trains.txt"); //Same opening as ViewSavedTrains
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.equals(ID)) { //If a line equals the current train ID
                    menu.findItem(R.id.add_train).setTitle("Remove"); //The train must already be saved, so the "Save" button becomes "Remove"
                }
            }
        } catch (Exception e){} //Required error catch (no need to alert the user)
        return super.onPrepareOptionsMenu(menu);
    }

    public boolean onCreateOptionsMenu(Menu menu){
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.train_info_items, menu); //Inflates the menu
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.add_train) { //If the save/remove button is pressed
            if (item.getTitle().equals("Remove")) { //If it's currently a remove button:
                try {
                    InputStream is = getApplicationContext().openFileInput("trains.txt"); //Same delete function as ViewSavedTrains()
                    BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                    String line;
                    StringBuilder sb = new StringBuilder();
                    int deleted = 0;
                    boolean done = false;
                    while ((line = reader.readLine()) != null) {
                        if (!line.equals(ID) && deleted == 0){
                                sb.append(line + "\n");
                            } else if (!done) {
                                deleted++;
                                if (deleted > 2) {
                                    done = true;
                                    deleted = 0;
                                }
                            } else {
                                sb.append(line + "\n");
                            }
                    }
                    is.close();
                    FileOutputStream fos = getApplicationContext().openFileOutput("trains.txt", Context.MODE_PRIVATE);
                    fos.write(sb.toString().getBytes());
                    fos.close();
                    Toast.makeText(ViewTrainInfo.this, "Train deleted", Toast.LENGTH_LONG).show();
                    item.setTitle("Add"); //Changes the button back to add
                } catch (Exception e){
                    Toast.makeText(ViewTrainInfo.this, "Error deleting", Toast.LENGTH_LONG).show(); //General error catch
                }
            } else { //If the button is an add button
                try {
                    FileOutputStream fos = getApplicationContext().openFileOutput("trains.txt", Context.MODE_APPEND);
                    fos.write((ID + "\n").getBytes()); //MODE_APPEND so file does not get overwritten. Writes ID on first new line
                    if (mode.equals("0")) { //Adds word information on the second line, depending on if Arrival or Departure
                        fos.write((timesArray.get(0) + " - " + callingArray.get(0) + " to " + callingArray.get(callingArray.size() - 1)).getBytes());
                    } else {
                        fos.write((timesArray.get(0) + " - " + callingArray.get(0) + " from " + callingArray.get(callingArray.size() - 1)).getBytes());
                    }
                    fos.write(("\n" + mode + "\n").getBytes()); //Adds mode to the third line
                    Toast.makeText(ViewTrainInfo.this, "Train Saved", Toast.LENGTH_LONG).show();
                    item.setTitle("Remove"); //Flips the button to Remove
                } catch (Exception e) {
                    Toast.makeText(ViewTrainInfo.this, "An error has occurred", Toast.LENGTH_LONG).show(); //General error catch
                }
            }
        }
        else if (item.getItemId() == R.id.menu_refresh){ //If the refresh button is pressed

            ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo = null; //Same opening as ViewSavedTrains
            if (connMgr != null) {
                networkInfo = connMgr.getActiveNetworkInfo();
            }
            if (networkInfo != null && networkInfo.isConnected() && ID.length() != 0) {
                Bundle queryBundle = new Bundle();
                queryBundle.putString("queryString", ID);
                getSupportLoaderManager().restartLoader(0, queryBundle, ViewTrainInfo.this);
            } else {
                Toast.makeText(ViewTrainInfo.this,"Please check your network connection.",Toast.LENGTH_LONG).show();
            }

        } else {
            finish();
        }
    return  true;}

    public void toIntent(ArrayList<String> list) {
        if (list.size() > 0) {
            Intent intent = new Intent(this, ViewTrainInfo.class);
            intent.putExtra("PLATFORM", list.get(0)); //Adds to intent as it iterates through array
            intent.putExtra("TIME", list.get(1)); //This could not have been done in new ViewSavedTrains() from here
            intent.putExtra("CALLING", list.get(2));
            intent.putExtra("ORIGIN", list.get(3));
            intent.putExtra("ONTIME", list.get(4));
            intent.putExtra("ID", list.get(5));
            intent.putExtra("MODE", list.get(6));
            finish(); //Close current activity
            startActivity(intent); //Restart with up to date information
        }
    }

    @NonNull
    @Override
    public Loader<String> onCreateLoader(int id, @Nullable Bundle args) {
        String queryString = "";
        if (args != null) {queryString = args.getString("queryString");}
        return new TrainLoader(this, queryString,2); //Same as ViewSavedTrains
    }

    @Override
    public void onLoadFinished(@NonNull Loader<String> loader, String data) {

        toIntent(new ViewSavedTrains().postLoad(data, mode, ID, 1)); //This is why toIntent() could not be incorporated
    }

    @Override
    public void onLoaderReset(@NonNull Loader<String> loader) {

    }
}