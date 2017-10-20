package com.dronepath;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;

/**
 * Created by Dylan on 10/19/2017.
 */

public class FlightVarsDialogFragment extends DialogFragment {
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();

        // Define the dialog TODO - Maybe add a slider for velocity and altitude?
        builder.setView(inflater.inflate(R.layout.flight_vars_dialog, null))
                // "Setter methods" are chained together to modify the dialog's characteristics
                .setMessage("Modify drone flight variables")   // Text in window
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) { // User hit OK
                        // TODO Read the actual velocity and altitude from the Dialog
                        double velocity = 1.0;
                        double altitude = 1.0;
                        mListener.onComplete(velocity, altitude);
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
