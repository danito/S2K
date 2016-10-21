package be.nixekinder.preferencestest;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ToggleButton;

/**
 * Created by dan on 18.10.16.
 */

public class StatusUpdate extends DialogFragment {
    NoticeDialogListener mListener;

    @Override
    public void  onAttach(Context activity) {
        super.onAttach(activity);
        // Verify that the host activity implements the callback interface
        try {
            // Instantiate the NoticeDialogListener so we can send events to the host
            mListener = (NoticeDialogListener) activity;
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw exception
            throw new ClassCastException(activity.toString()
                    + " must implement NoticeDialogListener");
        }
    }

    public Dialog onCreateDialog(Bundle savedInstanceState){

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        final LayoutInflater inflater = getActivity().getLayoutInflater();
        SharedPreferences sharedPreferences;
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        SharedPreferences.Editor editor = sharedPreferences.edit();


        View dialogView = inflater.inflate(R.layout.fragment_status, null);
        final ImageView imA = (ImageView) dialogView.findViewById(R.id.add_image);
        final TextView txB = (TextView) dialogView.findViewById(R.id.edit_status_text);
        final ToggleButton tbTw = (ToggleButton) dialogView.findViewById(R.id.tw_toggle);
        final ToggleButton tbFb = (ToggleButton) dialogView.findViewById(R.id.fb_toggle);
        ColorStateList lC = txB.getTextColors();
        imA.setImageTintList(lC);
        tbTw.setButtonTintList(lC);
        tbFb.setButtonTintList(lC);
        imA.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onClickB();
            }
        });

        builder.setView(dialogView).setPositiveButton(getString(R.string.publish),new DialogInterface.OnClickListener(){
            public void onClick(DialogInterface dialog, int id){
                // what happens after positive click
                String user = "ddd";
                mListener.onDialogPositiveClick(user);
            }

        }).setNegativeButton(getString(R.string.cancel),null);
        return builder.create();

    }

    public void onClickB() {

        this.dismiss();
    }

    public interface NoticeDialogListener {
        public void onDialogPositiveClick(String username);

        public void onDialogDismiss(String dismiss);

        public void onDialogNegativeClick();
    }




}
