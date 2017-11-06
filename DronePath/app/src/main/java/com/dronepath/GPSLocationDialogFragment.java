package com.dronepath;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.app.DialogFragment;
import android.app.Dialog;
import android.preference.EditTextPreference;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

/**
 * Created by Dylan on 10/17/2017.
 *
 * Creates and manages the dialog for manual GPS Location input
 */

public class GPSLocationDialogFragment extends DialogFragment {
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // The Builder class provides a wrapper for easily modifying a Dialog (popup menu)
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        // The layout inflater converts an XML layout into view objects
        LayoutInflater inflater = getActivity().getLayoutInflater();
        final View view = inflater.inflate(R.layout.gps_dialog, null);

        final EditText latitudeText = (EditText) view.findViewById(R.id.latitude);
        final EditText longitudeText = (EditText) view.findViewById(R.id.longitude);

        // Load saved inputs
        latitudeText.setText(this.getArguments().getString("savedLatitude"));
        longitudeText.setText(this.getArguments().getString("savedLongitude"));

        // Define the dialog
        builder.setView(view)
                // "Setter methods" are chained together to modify the dialog's characteristics
                .setMessage("Manually input a GPS address")   // Text in window

                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) { // User hit OK
                        // Get user-input text
                        String latEntry = latitudeText.getText().toString();
                        String longEntry = longitudeText.getText().toString();

                        // Check for blank input in either EditText
                        if (!"".equals(latEntry) && !"".equals(longEntry)) {
                            double latitude = Double.parseDouble(latitudeText.getText().toString());
                            double longitude = Double.parseDouble(longitudeText.getText().toString());

                            // Pass to MainActivity to handle Map integration
                            mListener.onGPSLocationDialogComplete(latitude, longitude);
                        }
                    }
                })

                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) { // User hit Cancel
                        // Close the dialog without modifying any variables
                        dialog.cancel();
                    }
                });

        // Create the AlertDialog object and return it
        return builder.create();
    }

    // An Interface allows the Dialog to communicate with the Main Activity
    // The Main Activity is where the onComplete function is actually implemented
    public interface OnCompleteListener {
        void onGPSLocationDialogComplete(double latitude, double longitude);
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
