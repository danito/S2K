package be.nixekinder.preferencestest;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.AsyncTask;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static android.R.attr.action;

public class MainActivity extends AppCompatActivity implements StatusUpdate.NoticeDialogListener {
    private static final String TAG = "IneedCoffee";
    private static final String PREFS_NAME = "sharedPref";
    SharedPreferences sharedPreferences;
    String prefUsername;
    String prefHostname;
    String prefApikey;
    String prefAction;
    private String setUsername;
    private String setHostname;
    private String setApiKey;

    ImageView ivLogo;
    ImageView ivProfilePicture;

    SharedPreferences settings;
    SharedPreferences.Editor editor;

    String prefDisplayname;
    String prefPicture;
    ArrayList<HashMap<String, HashMap<String, String>>> serviceList;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ivLogo = (ImageView) findViewById(R.id.logo_k);
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
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        // check what happened with settings
        showPrefs();
    }


    public void onFinishUserDialog(String user) {
        Toast.makeText(this, "Hello, " + user, Toast.LENGTH_SHORT).show();
    }


    public void onClick(View view) {
        // close existing dialog fragments


        switch (view.getId()) {
            case R.id.logo_k:
                showConnectionDialog();
                break;
          /*  case R.id.showAlertDialogFragment:
                MyAlertDialogFragment alertDialogFragment = new MyAlertDialogFragment();
                alertDialogFragment.show(manager, "fragment_edit_name");
                break;
                */
        }
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
                "setUsername", null);
        setHostname = sharedPreferences.getString(
                "setHostname", null);
        setApiKey = sharedPreferences.getString(
                "setApiKey", null);

        prefPicture = sharedPreferences.getString("setProfilePicture", "");
        if (!prefPicture.equals("")) {
            Log.i(TAG, "showPrefs: prefPicture " + prefPicture);
            Picasso.with(getBaseContext()).load(prefPicture).transform(new RoundedCornersTransform()).into(ivProfilePicture);
        }
        if(!setHostname.equals("")){
            TextView tvHostname = (TextView) findViewById(R.id.profile_host);
            tvHostname.setText(setHostname);
            ColorStateList o = tvHostname.getTextColors();
            int c = o.getDefaultColor();
            int f = ContextCompat.getColor(getBaseContext(), android.R.color.primary_text_dark);
            Log.i(TAG, "showPrefs: color " + o);
            Integer e = 603979776;
            String p = Integer.toHexString(603979776);
            String hexColor = String.format("#%06X", (0xFFFFFF & c));
            Log.i(TAG, "showPrefs: color " + p);


        }
        if(!setUsername.equals("")){
            TextView tvHostname = (TextView) findViewById(R.id.profile_name);
            tvHostname.setText(setUsername);
        }
        boolean status = sharedPreferences.getBoolean("connStatus", false);
        if (status){
            ivLogo.setVisibility(View.VISIBLE);
        }


    }

    @Override
    public void onDialogPositiveClick(String username) {


    }

    @Override
    public void onDialogNegativeClick() {

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
                // sharedText = "<a href='"+ sharedText +"'>" + sharedText + "</a>";
            }
            // tvIntent.setText(sharedText);
        }
    }

    void handleSendImage(Intent intent) {
        Uri imageUri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
        if (imageUri != null) {
            // Update UI to reflect image being shared
        }
    }

    class getJson extends AsyncTask<String, String, JSONObject> {
        JSONParser jsonParser = new JSONParser();
        private ProgressDialog pDialog;

        @Override
        protected void onPreExecute() {
            pDialog = new ProgressDialog(MainActivity.this);
            pDialog.setMessage(getString(R.string.attempt_conn));
            pDialog.setIndeterminate(false);
            pDialog.setCancelable(true);
            pDialog.show();
        }


        @Override
        protected JSONObject doInBackground(String... args) {

            try {
                String signature = getSignature(prefApikey, prefAction);
                String url = prefHostname + "/" + prefAction;
                HashMap<String, String> params = new HashMap<>();
                params = null;
                int l = args.length;
                if ((l%2) != 0){
                    args[l+1]="";
                }

                for (int a = 0; a < l; a += 2) {
                    params.put(args[a], args[a + 1]);
                }
                String method = "GET";

                JSONObject json = jsonParser.makeHttpRequest(url, method, params, prefUsername, signature);
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
            if (json != null) {
                try {
                    if (prefAction.toLowerCase().contains("profile")) {
                        JSONObject user = json.getJSONObject("user");
                        JSONObject image = user.getJSONObject("image");
                        String id = user.getString("id");
                        String displayname = user.getString("displayName");
                        String imageurl = image.getString("url");
                        Log.i(TAG, "onPostExecute: " + displayname + ", " + id + ", " + imageurl);
                        prefDisplayname = displayname;
                        setSharedPref("prefDisplayname", displayname);
                        prefPicture = imageurl;
                        setSharedPref("prefPicture", imageurl);
                        ivLogo.setVisibility(View.VISIBLE);
                    } else if (prefAction.toLowerCase().contains("edit")) {


                        JSONObject oServices = json.getJSONObject("services");
                        serviceList.clear();

                        for (int i = 0; i < oServices.length(); i++) {
                            String key = oServices.names().getString(i);
                            JSONArray arrJ = oServices.getJSONArray(key);
                            JSONObject value = arrJ.getJSONObject(0);
                            HashMap<String,String>  syndication = new HashMap<>();
                            String username = value.getString("username");
                            String name = value.getString("name");
                            String service = key + " (" + name + ")";
                            syndication.put(key,username);

                            HashMap<String, HashMap<String, String>> sl = new HashMap<>();
                            sl.put("service_name", syndication);
                            serviceList.add(sl);
                            Log.e("JSO", "Key = " + oServices.names().getString(i) + " value = " + oServices.get(oServices.names().getString(i)));

                        }
                    }


                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }
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


}
