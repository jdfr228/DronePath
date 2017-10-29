package com.dronepath;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.app.DialogFragment;
import android.app.Dialog;
import android.preference.EditTextPreference;
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
        double longitude = 0;
        double latitude = 0;

        // Define the dialog
        builder.setView(inflater.inflate(R.layout.gps_dialog, null))
                // "Setter methods" are chained together to modify the dialog's characteristics
                .setMessage("Manually input a GPS address")   // Text in window
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) { // User hit OK
                        /* TODO 3 possible actions
                            1) Pass GPS address to a sanity checker, then to the Google Maps class
                            2) Pass straight to GMaps class which will perform its own sanity checks
                            3) Store GPS address as a global and call a GMaps routine to display
                                the updated info?
                         */
                        final EditText longitude = (EditText) view.findViewById(R.id.longitude);
                        final EditText latitude = (EditText) view.findViewById(R.id.latitude);
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
}
