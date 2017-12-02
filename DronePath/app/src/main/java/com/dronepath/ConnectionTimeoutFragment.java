package com.dronepath;

import android.support.v4.app.Fragment;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;


public class ConnectionTimeoutFragment extends Fragment {
    public interface OnCompleteListener {
        void onConnectionTimeoutTaskComplete();
    }

    private static ConnectionTimeoutTask mTask;
    private static OnCompleteListener mListener;


    // Fragment Lifecycle methods (in the order they would be called)
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        // Make mListener reference the current MainActivity
        try {
            mListener = (OnCompleteListener)context;
        }

        // Make sure the Main Activity has implemented the Interface
        catch (final ClassCastException e) {
            throw new ClassCastException(context.toString() + " has not implemented " +
                    "ConnectionTimeoutFragment's Interface");
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Don't destroy the fragment on a screen rotation
        setRetainInstance(true);
    }

    // Should only be called when the app is killed since setRetainInstance is set to true
    @Override
    public void onDestroy() {
        super.onDestroy();

        // Cancel the task if the app is exited
        if ((mTask != null) && (mTask.getStatus() == AsyncTask.Status.RUNNING)) {
            Log.d("timeoutTask", "cancelling task from onDestroy");
            mTask.cancel(true);
            mTask = null;
        }
    }

    // Destroy reference to an old MainActivity- on an orientation change, onAttach will make
    //      sure mListener is referencing the new MainActivity
    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }


    // Helper methods
    // Called externally to start the timeout task
    /** @param timeoutLength time until the task finishes and should disconnect drone (in seconds)*/
    public void startTask(int timeoutLength) {
        Log.d("timeoutTask", "task starting");
        mTask = new ConnectionTimeoutTask();
        mTask.execute(timeoutLength);
    }

    // Called externally to stop the timeout task
    public void stopTask() {
        Log.d("timeoutTask", "cancelling task from stopTask");
        mTask.cancel(true);
    }


    // AsyncTask implementation- actually does the timing for the Fragment
    private static class ConnectionTimeoutTask extends AsyncTask<Integer, Integer, Void> {
        protected Void doInBackground(Integer... timeoutLength) {
            int milliseconds = timeoutLength[0] * 1000;

            try {
                Thread.sleep(milliseconds);
            } catch(InterruptedException e) {
                Log.d("timeoutTask", "Timeout thread interrupted");
            }

            return null;
        }

        protected void onPostExecute(Void result) {
            Log.d("timeoutTask", "Timeout thread complete");

            // Fire the MainActivity listener
            if (mListener != null) {
                mListener.onConnectionTimeoutTaskComplete();
            }
            // TODO- else in case the activity is currently unattached
            //          Would only trigger if rotating *just* as the timeout ends
        }

        protected void onCancelled(Void result) {
            Log.d("timeoutTask", "Timeout thread cancelled");
            // Do nothing- the timeout was cancelled and shouldn't affect the connection
        }
    }

}
