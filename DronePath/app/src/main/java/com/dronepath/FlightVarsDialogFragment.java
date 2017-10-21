package com.dronepath;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.SeekBar;

/**
 * Created by Dylan on 10/19/2017.
 */

public class FlightVarsDialogFragment extends DialogFragment {
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        final View view = inflater.inflate(R.layout.flight_vars_dialog, null);

        // These reference the SeekBars defined in the flight_vars_dialog layout
        final SeekBar velocitySeekBar = (SeekBar) view.findViewById(R.id.velocitySeekBar);
        final SeekBar altitudeSeekBar = (SeekBar) view.findViewById(R.id.altitudeSeekBar);

        // Define the dialog
        builder.setView(view)
                // "Setter methods" are chained together to modify the dialog's characteristics
                .setMessage("Modify drone flight variables")   // Text in window

                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) { // User hit OK
                        double velocity = progressToDouble(velocitySeekBar.getProgress());
                        double altitude = progressToDouble(altitudeSeekBar.getProgress());
                        mListener.onComplete(velocity, altitude);   // Pass to Main Activity
                    }
                })

                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) { // User hit Cancel
                        // Close the dialog without modifying any variables
                        dialog.cancel();
                    }
                });

        // TODO Display velocity and altitude values to the user
        /*velocitySeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int val = velocitySeekBar.getProgress();

            }
        });*/

        // Create the AlertDialog object and return it
        return builder.create();
    }


    // Helper function to convert the int progress to the proper double value (with a step of 0.1)
    // NOTE - In order to allow for a step of 0.1, maxVelocity and maxAltitude need to be stored
    //          as a value 10x the actual expected value
    private double progressToDouble(int progress) {
        return progress / 10.0;
    }


    // An Interface allows the Dialog to communicate with the Main Activity
    // The Main Activity is where the onComplete function is actually implemented
    public interface OnCompleteListener {
        void onComplete(double velocity, double altitude);
    }


    // Make sure the Main Activity has implemented the Interface
    private OnCompleteListener mListener;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        try {
            this.mListener = (OnCompleteListener)context;
        }
        catch (final ClassCastException e) {
            throw new ClassCastException(context.toString() + " has not implemented " +
                    "FlightVarsDialogFragment's Interface");
        }
    }
}
