package com.dronepath;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.app.DialogFragment;
import android.app.Dialog;

/**
 * Created by Dylan on 10/17/2017.
 */

public class GPSLocationDialogFragment extends DialogFragment {

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // The Builder class provides a wrapper for easily modifying a Dialog (popup menu)
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        builder.setMessage("Testing")   // Text in window
                // "Setter methods" are chained together to modify the dialog's characteristics
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // User hit OK
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // User hit Cancel
                    }
                });

        // Create the AlertDialog object and return it
        return builder.create();
    }
}
