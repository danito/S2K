package be.nixekinder.ShareWithKnown;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.util.Log;
import android.util.Patterns;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.squareup.picasso.Picasso;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import be.nixekinder.preferencestest.R;

public class MainActivity extends AppCompatActivity implements StatusUpdate.NoticeDialogListener {
    private static final String TAG = "IneedCoffee";
    private static final String PREFS_NAME = "sharedPref";
    SharedPreferences sharedPreferences;
    String prefUsername;
    String prefHostname;
    String prefApikey;
    String prefAction;

    ImageView ivProfilePicture;
    SharedPreferences settings;
    SharedPreferences.Editor editor;

    String prefDisplayname;
    String prefPicture;
    ArrayList<HashMap<String, HashMap<String, String>>> serviceList;
    private String setUsername;
    private String setHostname;
    private String setApiKey;
    private boolean setReactions;
    private RadioGroup radioGroup;
    private Button btnDisplay;
    private String statusAction;
    private HashMap<String, String> postParameters;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.i(TAG, "onCreate: oncreate");


        settings = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE); //1
        editor = settings.edit();


        //
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        ivProfilePicture = (ImageView) findViewById(R.id.profile_image);
        showPrefs();

        Map<String, ?> allEntries = sharedPreferences.getAll();
        for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
            Log.d("map values", entry.getKey() + ": " + entry.getValue().toString());
        }

        // FontAwesome
        Typeface iconFont = FontManager.getTypeface(getApplicationContext(), FontManager.FONTAWESOME);

        //  FontManager.markAsIconContainer(findViewById(R.id.circlecontainer), iconFont);

        // Get intent, action and MIME type
        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();

        showPrefs();


        if (Intent.ACTION_SEND.equals(action) && type != null) {
            if ("text/plain".equals(type)) {
                handleSendText(intent); // Handle text being sent
            } else if (type.startsWith("image/")) {
                handleSendImage(intent); // Handle single image being sent
            }

        } else {
            // Handle other intents, such as being started from the home screen
        }


    }



    public void onClick(View view) {

        switch (view.getId()) {
            case R.id.profile_image:
                showConnectionDialog();
                break;
            case R.id.add_image:
                break;
            case R.id.form_status:
                ifStatus();
                break;
            case R.id.form_image:
                ifImage();
                break;
            case R.id.btn_bookmark:
                ifBookmark();
                break;
            case R.id.bt_cancel:
                emptyStatus();
                break;
            case R.id.bt_publish:
                publishStatus();
                break;
          /*  case R.id.showAlertDialogFragment:
                MyAlertDialogFragment alertDialogFragment = new MyAlertDialogFragment();
                alertDialogFragment.show(manager, "fragment_edit_name");
                break;
                */
        }
    }

    private void publishStatus() {
        EditText status = (EditText) findViewById(R.id.edit_status_form);
        String mStatus = status.getText().toString().trim();
        EditText reply = (EditText) findViewById(R.id.in_reply_to);
        String mReply = reply.getText().toString().trim();
        EditText description = (EditText) findViewById(R.id.edit_status_description);
        String mDescription = description.getText().toString().trim();

        ToggleButton btTwitter = (ToggleButton) findViewById(R.id.tw_toggle);
        boolean tw = btTwitter.isChecked();
        ToggleButton btFacebook = (ToggleButton) findViewById(R.id.fb_toggle);
        boolean fb = btFacebook.isChecked();
        String syn = "";
        if (tw) {
            syn = "twitter::nxD4n";
        }

        if (fb) {
            syn = "facebook::1156781735;" + syn;
        }

        String myAction = "/";

        postParameters = new HashMap<>();
        postParameters.put("syndication", syn);
        if (!status.equals("")) {
            switch (statusAction) {
                case "status":
                    myAction = "/status/edit";
                    postParameters.put("body", mStatus);
                    if (isUrl(mReply)) {
                        postParameters.put("inreplyto[]", mReply);
                    }
                    // TODO: 26/10/2016 handle syndication

                    break;
                case "bookmark":
                    if (isUrl(mStatus)) {
                        myAction = "/like/edit";
                        postParameters.put("body", mStatus);
                        postParameters.put("description", mDescription);
                    }
                    break;
                case "star":
                    if (isUrl(mStatus)) {
                        postParameters.put("like-of", mStatus);
                        myAction = "/indielike/edit";
                    }
                    break;
                case "repost":
                    if (isUrl(mStatus)) {
                        postParameters.put("repost-of", mStatus);
                        myAction = "/repost/edit";
                    }
                    break;
                case "photo":
                    // TODO: 26/10/2016
                    myAction = "/photo/edit";
                    break;
            }

            if (!postParameters.isEmpty()) {
                Log.i(TAG, "publishStatus: " + postParameters);
                new getJson().execute(setHostname, "POST", setUsername, setApiKey, myAction);
            }

        } else {
            // TODO: 26/10/2016 make toast nothing to publish
        }
    }

    private boolean isUrl(String mReply) {
        return Patterns.WEB_URL.matcher(mReply.toLowerCase()).matches();

    }

    private void emptyStatus() {
        EditText status = (EditText) findViewById(R.id.edit_status_form);
        EditText reply = (EditText) findViewById(R.id.in_reply_to);
        EditText description = (EditText) findViewById(R.id.edit_status_description);

        status.setText("");
        reply.setText("");
        description.setText("");
    }

    private void showConnectionDialog() {

        StatusUpdate dialog = new StatusUpdate();
        dialog.show(getFragmentManager(), "Connection Dialog");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menuSettings:
                Intent intentSetPref = new Intent(getApplicationContext(),
                        PrefActivity.class);
                startActivityForResult(intentSetPref, 0);
            default:
                return super.onOptionsItemSelected(item);
        }
    }


    private void showPrefs() {
        Log.i(TAG, "showPrefs: ");

        setUsername = sharedPreferences.getString(
                "setUsername", "");
        setHostname = sharedPreferences.getString(
                "setHostname", "");
        setApiKey = sharedPreferences.getString(
                "setApiKey", "");
        setReactions = sharedPreferences.getBoolean("useReactions", false);


        prefPicture = sharedPreferences.getString("setProfilePicture", "");
        if (!prefPicture.equals("")) {
            Log.i(TAG, "showPrefs: prefPicture " + prefPicture);
            Picasso.with(getBaseContext()).load(prefPicture).transform(new RoundedCornersTransform()).into(ivProfilePicture);
        }
        if (!setHostname.equals("") && !setHostname.equals(null) && setHostname != null) {
            TextView tvHostname = (TextView) findViewById(R.id.profile_host);
            tvHostname.setText(setHostname);
        }
        if(!setUsername.equals("")){
            TextView tvHostname = (TextView) findViewById(R.id.profile_name);
            tvHostname.setText(setUsername);
        }
        boolean status = sharedPreferences.getBoolean("connStatus", false);
        ViewGroup vgEditStatus = (ViewGroup) findViewById(R.id.edit_status);
        TextView tvNoSettings = (TextView) findViewById(R.id.text_no_settings);
        if (status){
            Log.i(TAG, "showPrefs: conn ok");
            tvNoSettings.setVisibility(View.GONE);
            vgEditStatus.setVisibility(View.VISIBLE);
            statusAction = "status";

        } else {
            Log.i(TAG, "showPrefs: connNOK");
            vgEditStatus.setVisibility(View.GONE);
            tvNoSettings.setVisibility(View.VISIBLE);
        }


    }

    @Override
    public void onDialogPositiveClick(String username) {


    }

    @Override
    public void onDialogNegativeClick() {

    }

    @Override
    public void onDialogDismiss(String dismiss) {
        Log.i(TAG, "onDialogDismiss: " + dismiss);
    }


    // Handle incoming text or image

    /**
     * @param intent
     */
    void handleSendText(Intent intent) {
        String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
        if (sharedText != null) {
            // Update UI to reflect text being shared
            // TextView tvIntent = (TextView) findViewById(R.id.fromIntent);

            boolean isurl = Patterns.WEB_URL.matcher(sharedText.toLowerCase()).matches();
            if (isurl) {
                ifBookmark(sharedText);
                // sharedText = "<a href='"+ sharedText +"'>" + sharedText + "</a>";
            } else ifStatus(sharedText);
            // tvIntent.setText(sharedText);
        }
    }

    void handleSendImage(Intent intent) {
        Uri imageUri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
        if (imageUri != null) {
            // Update UI to reflect image being shared
        }
    }

    public String getSignature(String apikey, String action) {
        ApiSecurity api = new ApiSecurity();
        api.setSecurity(action, apikey);
        return api.getHash();
    }

    public void setSharedPref(String name, String value) {
        editor.putString(name, value);
        editor.commit();
    }

    public void setSharedPref(String name, boolean value) {
        editor.putBoolean(name, value);
        editor.commit();
    }

    public void savePrefsSet(String name, Set val) {
        editor.putStringSet(name, val);
        editor.commit();
    }

    public String getSharedPref(String name, String defaultvalue) {
        return settings.getString(name, defaultvalue);
    }

    public boolean getSharedPref(String name, boolean defaultvalue) {
        return settings.getBoolean(name, defaultvalue);
    }

    private void ifStatus() {
        statusAction = "status";
        //visible :  android:id="@+id/edit_status_form"
        setStatusOff();
        ImageView status = (ImageView) findViewById(R.id.form_status);
        status.setImageResource(R.drawable.ic_comment_accent_24px);


        TextView edit_status_text = (TextView) findViewById(R.id.edit_status_text);
        edit_status_text.setText(getString(R.string.edit_status_text));

        // set input field edit_status_form to multilines, text
        EditText edit_status_form = (EditText) findViewById(R.id.edit_status_form);
        edit_status_form.setHint(getString(R.string.edit_status_text));
        edit_status_form.setInputType(InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_CLASS_TEXT);
        edit_status_form.setHint(getString(R.string.status_text_hint));

        // in reply to
        EditText in_reply_to = (EditText) findViewById(R.id.in_reply_to);
        in_reply_to.setVisibility(View.VISIBLE);

        //hide image placeholder
        ImageView new_photo = (ImageView) findViewById(R.id.new_photo);
        new_photo.setVisibility(View.INVISIBLE);

        // Hide reactions
        LinearLayout ll_title = (LinearLayout) findViewById(R.id.ll_title);
        ll_title.setVisibility(View.INVISIBLE);
        LinearLayout ll_url_reactions = (LinearLayout) findViewById(R.id.ll_url_reactions);
        ll_url_reactions.setVisibility(View.GONE);
    }

    private void ifStatus(String intentStatus) {
        statusAction = "status";

        //visible :  android:id="@+id/edit_status_form"
        setStatusOff();
        ImageView status = (ImageView) findViewById(R.id.form_status);
        status.setImageResource(R.drawable.ic_comment_accent_24px);

        TextView edit_status_text = (TextView) findViewById(R.id.edit_status_text);
        edit_status_text.setText(getString(R.string.edit_status_text));

        // set input field edit_status_form to multilines, text
        EditText edit_status_form = (EditText) findViewById(R.id.edit_status_form);
        edit_status_form.setHint(getString(R.string.edit_status_text));
        edit_status_form.setInputType(InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_CLASS_TEXT);
        edit_status_form.setHint(getString(R.string.status_text_hint));
        edit_status_form.setText(intentStatus);

        // in reply to
        EditText in_reply_to = (EditText) findViewById(R.id.in_reply_to);
        in_reply_to.setVisibility(View.VISIBLE);

        //hide image placeholder
        ImageView new_photo = (ImageView) findViewById(R.id.new_photo);
        new_photo.setVisibility(View.INVISIBLE);

        // Hide reactions
        LinearLayout ll_title = (LinearLayout) findViewById(R.id.ll_title);
        ll_title.setVisibility(View.INVISIBLE);
        LinearLayout ll_url_reactions = (LinearLayout) findViewById(R.id.ll_url_reactions);
        ll_url_reactions.setVisibility(View.GONE);
    }

    private void ifStatus(Uri url) {
        statusAction = "status";

        //visible :  android:id="@+id/edit_status_form"
        setStatusOff();
        ImageView status = (ImageView) findViewById(R.id.form_status);
        status.setImageResource(R.drawable.ic_comment_accent_24px);

        TextView edit_status_text = (TextView) findViewById(R.id.edit_status_text);
        edit_status_text.setText(getString(R.string.edit_status_text));

        // set input field edit_status_form to multilines, text to ""
        EditText edit_status_form = (EditText) findViewById(R.id.edit_status_form);
        edit_status_form.setInputType(InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_CLASS_TEXT);
        edit_status_form.setText("");

        // in reply to
        EditText in_reply_to = (EditText) findViewById(R.id.in_reply_to);
        in_reply_to.setVisibility(View.VISIBLE);
        in_reply_to.setText(url.toString());

        //hide image placeholder
        ImageView new_photo = (ImageView) findViewById(R.id.new_photo);
        new_photo.setVisibility(View.INVISIBLE);

        // Hide reactions
        LinearLayout ll_title = (LinearLayout) findViewById(R.id.ll_title);
        ll_title.setVisibility(View.INVISIBLE);
        LinearLayout ll_url_reactions = (LinearLayout) findViewById(R.id.ll_url_reactions);
        ll_url_reactions.setVisibility(View.GONE);
    }

    private void ifImage() {
        statusAction = "image";

        setStatusOff();
        ImageView status = (ImageView) findViewById(R.id.form_image);
        status.setImageResource(R.drawable.ic_image_accent_24px);

        TextView edit_status_text = (TextView) findViewById(R.id.edit_status_text);
        edit_status_text.setText(getString(R.string.edit_image_text));

        ImageView new_photo = (ImageView) findViewById(R.id.new_photo);
        new_photo.setVisibility(View.VISIBLE);

        LinearLayout ll_title = (LinearLayout) findViewById(R.id.ll_title);
        ll_title.setVisibility(View.VISIBLE);
        // set input field edit_status_form to single line, inputtype uri
        EditText edit_status_form = (EditText) findViewById(R.id.edit_status_form);
        edit_status_form.setSingleLine();
        edit_status_form.setInputType(InputType.TYPE_CLASS_TEXT);
        edit_status_form.setHint(getString(R.string.status_text_hint_photo));

        LinearLayout ll_url_reactions = (LinearLayout) findViewById(R.id.ll_url_reactions);
        ll_url_reactions.setVisibility(View.GONE);

    }

    private void ifBookmark() {
        statusAction = "bookmark";

        // set icon to black
        setStatusOff();

        // set bookmark icon to accent
        ImageView status = (ImageView) findViewById(R.id.btn_bookmark);
        status.setImageResource(R.drawable.ic_bookmark_accent_24px);

        //set Top title edit_status_text to new bookmark
        TextView edit_status_text = (TextView) findViewById(R.id.edit_status_text);
        edit_status_text.setText(getString(R.string.edit_bkm_text));

        // set input field edit_status_form to single line, inputtype uri
        EditText edit_status_form = (EditText) findViewById(R.id.edit_status_form);
        edit_status_form.setSingleLine();
        edit_status_form.setInputType(InputType.TYPE_TEXT_VARIATION_URI);
        edit_status_form.setHint(getString(R.string.status_text_hint_bkm));

        // invisilbe photo place holder
        ImageView new_photo = (ImageView) findViewById(R.id.new_photo);
        new_photo.setVisibility(View.INVISIBLE);

        // set description to visible


        // set reactions to visible
        if (setReactions) {
            LinearLayout ll_url_reactions = (LinearLayout) findViewById(R.id.ll_url_reactions);
            ll_url_reactions.setVisibility(View.VISIBLE);
            addListenerOnButton();
        }


    }

    private void ifBookmark(String url) {
        statusAction = "bookmark";

        // set icon to black
        setStatusOff();

        // set bookmark icon to accent
        ImageView status = (ImageView) findViewById(R.id.btn_bookmark);
        status.setImageResource(R.drawable.ic_bookmark_accent_24px);

        //set Top title edit_status_text to new bookmark
        TextView edit_status_text = (TextView) findViewById(R.id.edit_status_text);
        edit_status_text.setText(getString(R.string.edit_bkm_text));

        // set input field edit_status_form to single line, inputtype uri
        EditText edit_status_form = (EditText) findViewById(R.id.edit_status_form);
        edit_status_form.setHint(getString(R.string.status_text_hint_bkm));
        edit_status_form.setInputType(InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_CLASS_TEXT);
        edit_status_form.setMinLines(2);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            edit_status_form.setTextAppearance(android.R.style.TextAppearance_Small);
        }
        edit_status_form.setText(url);

        // invisilbe photo place holder
        ImageView new_photo = (ImageView) findViewById(R.id.new_photo);
        new_photo.setVisibility(View.INVISIBLE);

        // set description to visible


        // set reactions to visible
        if (setReactions) {
            LinearLayout ll_url_reactions = (LinearLayout) findViewById(R.id.ll_url_reactions);
            ll_url_reactions.setVisibility(View.VISIBLE);
            addListenerOnButton();
        }

    }

    private void setStatusOff() {
        // in reply to
        EditText in_reply_to = (EditText) findViewById(R.id.in_reply_to);
        in_reply_to.setVisibility(View.GONE);
        ImageView audio = (ImageView) findViewById(R.id.btn_audio);
        audio.setImageResource(R.drawable.ic_mic_black_24dp);
        ImageView image = (ImageView) findViewById(R.id.form_image);
        image.setImageResource(R.drawable.ic_image_black_24px);
        ImageView status = (ImageView) findViewById(R.id.form_status);
        status.setImageResource(R.drawable.ic_comment_black_24px);
        ImageView bkm = (ImageView) findViewById(R.id.btn_bookmark);
        bkm.setImageResource(R.drawable.ic_bookmark_black_24px);

    }

    public void addListenerOnButton() {
        final LinearLayout ll_title = (LinearLayout) findViewById(R.id.ll_title);
        ll_title.setVisibility(View.VISIBLE);

        radioGroup = (RadioGroup) findViewById(R.id.rd_url);
        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                EditText edit_status_form = (EditText) findViewById(R.id.edit_status_form);
                String u = edit_status_form.getText().toString();
                String s = getResources().getResourceEntryName(checkedId);
                TextView edit_status_text = (TextView) findViewById(R.id.edit_status_text);
                Log.i(TAG, "onCheckedChanged: s " + s);
                switch (s) {
                    case "rd_url_bkm":
                        //set Top title edit_status_text to new bookmark
                        edit_status_text.setText(getString(R.string.edit_bkm_text));
                        ll_title.setVisibility(View.VISIBLE);
                        break;
                    case "rd_url_star":
                        //set Top title edit_status_text to new like
                        edit_status_text.setText(getString(R.string.edit_bkm_text_star));
                        Log.i(TAG, "onCheckedChanged: " + edit_status_text.getText());
                        ll_title.setVisibility(View.INVISIBLE);
                        statusAction = "star";
                        break;
                    case "rd_url_retweet":
                        //set Top title edit_status_text to new repost
                        edit_status_text.setText(getString(R.string.edit_bkm_text_repost));
                        statusAction = "repost";
                        ll_title.setVisibility(View.INVISIBLE);
                        break;
                    case "rd_reply_to":
                        Uri newUrl = Uri.parse(u);
                        ifStatus(newUrl);
                }

            }
        });

    }

    private void toast(String toast) {
        Toast.makeText(MainActivity.this, toast, Toast.LENGTH_SHORT).show();
    }

    class getJson extends AsyncTask<String, String, JSONObject> {
        JSONParser jsonParser = new JSONParser();
        boolean sig = false;
        String hostname;
        private ProgressDialog pDialog;

        @Override
        protected void onPreExecute() {
            pDialog = new ProgressDialog(MainActivity.this);
            pDialog.setMessage(getString(R.string.posting));
            pDialog.setIndeterminate(false);
            pDialog.setCancelable(true);
            pDialog.show();
        }


        @Override
        protected JSONObject doInBackground(String... args) {

            try {

                String method = args[1];

                String signature = "";
                String url = args[0];
                url = url.replaceAll("/$", "");
                hostname = url;
                Log.i(TAG, "doInBackground: hostname " + hostname);
                if (!args[4].equals(null)) {
                    signature = getSignature(args[3], args[4]);
                    Log.i(TAG, "doInBackground: signature " + signature);
                    Log.i(TAG, "doInBackground: arg4 " + args[4]);
                    Log.i(TAG, "doInBackground: arg3 " + args[3]);
                    Log.i(TAG, "doInBackground: arg2 " + args[2]);
                    url = url + args[4];
                }
                Log.i(TAG, "doInBackground: url = " + url);

                /**
                 * new getJson().execute(url,"GET",mUsername, apikey,"profile/" + mUsername);
                 */
                JSONObject json = jsonParser.makeHttpRequest(url, method, postParameters, args[2], signature);
                if (json != null) {
                    Log.d("JSON result", json.toString());

                    return json;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(JSONObject json) {
            super.onPostExecute(json);

            String location;

            if (pDialog != null && pDialog.isShowing()) {
                pDialog.dismiss();
            }
            if (json != null) {
                try {
                    int code = json.getInt("code");
                    if (code != 403) {
                        if (json.has("location")) {
                            location = json.getString("location").replace("_t=json", "");
                            Log.i(TAG, "onPostExecute: location ok " + location);
                            toast(getString(R.string.post_published));
                            TextView tvLocation = (TextView) findViewById(R.id.location);
                            tvLocation.setText(location);
                            tvLocation.setVisibility(View.VISIBLE);
                        } else
                            Log.i(TAG, "onPostExecute: no location " + json);
                    } else {
                        Log.i(TAG, "onPostExecute: something went wrong");
                    }


                } catch (JSONException e) {
                    e.printStackTrace();
                }

            } else {
                Log.i(TAG, "onPostExecute: no json, no");
                if (sig) {
                    toast(getString(R.string.wrong_api));
                } else toast(getString(R.string.wrong_url));
            }
        }
    }


}
