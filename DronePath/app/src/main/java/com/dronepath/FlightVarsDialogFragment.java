package com.dronepath;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.Locale;

/**
 * Created by Dylan on 10/19/2017.
 */

public class FlightVarsDialogFragment extends DialogFragment {
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        final View view = inflater.inflate(R.layout.flight_vars_dialog, null);

        // Reference to the SeekBars defined in the flight_vars_dialog layout
        final SeekBar velocitySeekBar = (SeekBar) view.findViewById(R.id.velocitySeekBar);
        final SeekBar altitudeSeekBar = (SeekBar) view.findViewById(R.id.altitudeSeekBar);

        // Disable the Altitude SeekBar if the drone is currently in flight
        altitudeSeekBar.setEnabled(!this.getArguments().getBoolean("inFlight"));

        // References to the TextViews to be shown above the SeekBars
        final TextView velocityText = (TextView) view.findViewById(R.id.velocityTextView);
        final TextView altitudeText = (TextView) view.findViewById(R.id.altitudeTextView);

        // Reset the TextViews to be invisible in case the screen has been rotated
        velocityText.setVisibility(View.INVISIBLE);
        altitudeText.setVisibility(View.INVISIBLE);

        // Get passed arguments from MainActivity
        double velocity = this.getArguments().getDouble("currVelocity");
        double altitude = this.getArguments().getDouble("currAltitude");

        // Get stored settings
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        final double maxVelocity = Double.parseDouble(sharedPreferences.getString(
                "pref_key_max_velocity", ""));
        final double minAltitude = Double.parseDouble(sharedPreferences.getString(
                "pref_key_min_altitude", ""));
        final double maxAltitude = Double.parseDouble(sharedPreferences.getString(
                "pref_key_max_altitude", ""));

        // Set the SeekBars to the correct starting values
        velocitySeekBar.setProgress((int) Math.round((velocity - 1.0) / (maxVelocity - 1.0) * 100));
        altitudeSeekBar.setProgress((int) Math.round((altitude - minAltitude) / (maxAltitude - minAltitude)
                * 100));


        // Define the dialog
        builder.setView(view)
                // "Setter methods" are chained together to modify the dialog's characteristics
                .setMessage("Modify drone flight variables")   // Text in window

                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) { // User hit OK
                        double newVelocity = (velocitySeekBar.getProgress() / 100.0) * (maxVelocity - 1.0) + 1.0;
                        double newAltitude = (altitudeSeekBar.getProgress() / 100.0) * (maxAltitude - minAltitude)
                                + minAltitude;
                        mListener.onFlightVarsDialogComplete(newVelocity, newAltitude);   // Pass to Main Activity
                    }
                })

                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) { // User hit Cancel
                        // Close the dialog without modifying any variables
                        dialog.cancel();
                    }
                });


        // Display velocity and altitude values to the user
        velocitySeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onStartTrackingTouch(SeekBar seekBar) {
                velocityText.setVisibility(View.VISIBLE);

                // Update the TextView's position since a screen rotation will trigger onProgressChanged
                float xPosition = velocitySeekBar.getX() + velocitySeekBar.getThumb().getBounds().exactCenterX();
                velocityText.setX(xPosition);
                velocityText.setY(velocitySeekBar.getY() - 128);
            }
            @Override public void onStopTrackingTouch(SeekBar seekBar) {
                velocityText.setVisibility(View.INVISIBLE);
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // Get the current velocity value
                double velocityVal = (velocitySeekBar.getProgress() / 100.0) * (maxVelocity - 1.0)
                        + 1.0;

                // Modify the TextView value
                velocityText.setText(String.format(Locale.ENGLISH, "%.1f", velocityVal));

                // Find where the TextView needs to be displayed
                //TODO- that getX() shouldn't be needed but makes it closer to centered...
                float xPosition = velocitySeekBar.getX() + velocitySeekBar.getThumb().getBounds().exactCenterX();
                velocityText.setX(xPosition);
                velocityText.setY(velocitySeekBar.getY() - 128);    // Offset above the user's thumb
            }
        });

        altitudeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onStartTrackingTouch(SeekBar seekBar) {
                altitudeText.setVisibility(View.VISIBLE);

                float xPosition = altitudeSeekBar.getX() + altitudeSeekBar.getThumb().getBounds().exactCenterX();
                altitudeText.setX(xPosition);
                altitudeText.setY(altitudeSeekBar.getY() - 128);
            }
            @Override public void onStopTrackingTouch(SeekBar seekBar) {
                altitudeText.setVisibility(View.INVISIBLE);
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                double altitudeVal = (altitudeSeekBar.getProgress() / 100.0) * (maxAltitude - minAltitude)
                        + minAltitude;
                altitudeText.setText(String.format(Locale.ENGLISH, "%.1f", altitudeVal));

                float xPosition = altitudeSeekBar.getX() + altitudeSeekBar.getThumb().getBounds().exactCenterX();
                altitudeText.setX(xPosition);
                altitudeText.setY(altitudeSeekBar.getY() - 128);
            }
        });


        // Create the AlertDialog object and return it
        return builder.create();
    }


    // An Interface allows the Dialog to communicate with the Main Activity
    // The Main Activity is where the onComplete function is actually implemented
    public interface OnCompleteListener {
        void onFlightVarsDialogComplete(double velocity, double altitude);
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
