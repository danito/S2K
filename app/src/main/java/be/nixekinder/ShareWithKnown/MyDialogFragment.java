package be.nixekinder.ShareWithKnown;

/**
 * Created by dan on 18.10.16.
 */


import android.app.DialogFragment;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;

import be.nixekinder.preferencestest.R;

public class MyDialogFragment extends DialogFragment implements TextView.OnEditorActionListener {

    private EditText mEditText;


    // Empty constructor required for DialogFragment
    public MyDialogFragment() {

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_status, container);
        mEditText = (EditText) view.findViewById(R.id.edit_status_form);

        // set this instance as callback for editor action
        mEditText.setOnEditorActionListener(this);
        mEditText.requestFocus();
        getDialog().getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        getDialog().setTitle("Please enter username");

        return view;
    }

    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        // Return input text to activity
        statusUpdateListener activity = (statusUpdateListener) getActivity();
        activity.onFinishUserDialog(mEditText.getText().toString());
        this.dismiss();
        return true;
    }

    public interface statusUpdateListener {
        void onFinishUserDialog(String user);
    }
}