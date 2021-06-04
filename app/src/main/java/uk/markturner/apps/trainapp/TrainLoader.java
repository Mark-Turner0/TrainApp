package uk.markturner.apps.trainapp;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.loader.content.AsyncTaskLoader;

public class TrainLoader extends AsyncTaskLoader<String> {

    private final String mQueryString;
    private final int mType;

    public TrainLoader(@NonNull Context context, String queryString, int type){
        super(context);
        mType = type;
        mQueryString = queryString;
    }

    @Override
    protected void onStartLoading(){forceLoad();}

    @Override
    public String loadInBackground() {
        return NetworkUtils.getTrainInfo(mQueryString,mType);
    }

}
