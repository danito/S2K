package be.nixekinder.ShareWithKnown;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.MultiSelectListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceGroup;
import android.preference.PreferenceManager;
import android.preference.RingtonePreference;
import android.util.Log;
import android.webkit.URLUtil;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.Set;

import static android.content.ContentValues.TAG;

@SuppressLint("NewApi")
/**
 * Created by dan on 18.10.16.
 */

public class PrefFragment extends PreferenceFragment implements
        SharedPreferences.OnSharedPreferenceChangeListener {

    CheckBoxPreference etpConnStatus;
    SharedPreferences defSharedPrefs;
    CheckBoxPreference etpUseReactions;
    EditTextPreference etpSetProfilePicture;
    String profilePicture;
    private boolean urlIsOK = false;
    private boolean apiIsOk = true;
    private boolean userIsOk = false;
    private String username;
    private boolean hasReactions = false;

    @SuppressLint("NewApi")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.settings);
        defSharedPrefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        profilePicture = defSharedPrefs.getString("setProfilePicture","");

        etpUseReactions = (CheckBoxPreference) findPreference("useReactions");
        if (etpUseReactions.isChecked()){
            hasReactions = true;
        }



        getPreferenceScreen().getSharedPreferences()
                .registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        for (int i = 0; i < getPreferenceScreen().getPreferenceCount(); ++i) {
            Preference preference = getPreferenceScreen().getPreference(i);
            if (preference instanceof PreferenceGroup) {
                PreferenceGroup preferenceGroup = (PreferenceGroup) preference;
                for (int j = 0; j < preferenceGroup.getPreferenceCount(); ++j) {
                    updatePreference(preferenceGroup.getPreference(j));
                }
            } else {
                updatePreference(preference);
            }
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
                                          String key) {

        EditTextPreference etpUrl, etpUsername, etpApikey;
        etpApikey = (EditTextPreference) findPreference("setApiKey");
        etpUrl = (EditTextPreference) findPreference("setHostname");
        etpUsername = (EditTextPreference) findPreference("setUsername");
        etpUseReactions = (CheckBoxPreference) findPreference("useReactions");

        String mUsername = etpUsername.getText();
        if (mUsername == null) {
            mUsername = "";
            userIsOk = false;
        }
        String url = etpUrl.getText();

        Log.i(TAG, "onSharedPreferenceChanged: urlgettext " + url);
        if (url == null || !URLUtil.isValidUrl(url)) {
            //etpUrl.setText(getText(R.string.wrong_url) + " " + url);
            url = "";
            urlIsOK = false;
        }
        Log.i(TAG, "onSharedPreferenceChanged: urlgettext " + url);

        String apikey = etpApikey.getText();
        if (apikey == null) {
            apikey = "";
            apiIsOk = false;
        }
        username = mUsername;

        Log.i(TAG, "onSharedPreferenceChanged: something changed " + key);
        updatePreference(findPreference(key));
        etpConnStatus = (CheckBoxPreference) findPreference("connStatus");
        if (apiIsOk && urlIsOK && userIsOk) {
            etpConnStatus.setSummary(getString(R.string.conn_status_ok));
        }

        if (key.equals("setHostname")) {
            EditTextPreference editTextPreference = (EditTextPreference) findPreference(key);
            url = editTextPreference.getText();
            try {
                if (checkConnection(url)) {
                    new getJson().execute(url, "CHECK", "", "", "");
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if (key.equals("setApiKey")) {
            if (!url.equals("") && !mUsername.equals("")) {
                new getJson().execute(url, "GET", mUsername, apikey, "/status/edit");
            }

        } else if (key.equals("setUsername")) {
            if (!url.equals("")) {
                Log.i(TAG, "onSharedPreferenceChanged: url = " + url);
                new getJson().execute(url, "CHECK", mUsername, apikey, "/" );
            }
        } else if (key.equals("useReactions")) {
            if (etpUseReactions.isChecked()) {
                new getJson().execute(url, "GET", mUsername, apikey, "/");
            } else {
                hasReactions = false;
            }
        }
    }

    private boolean checkConnection(String hostname) throws IOException {
        ConnectivityManager cm = (ConnectivityManager) getContext().getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null &&
                activeNetwork.isConnectedOrConnecting();
        if (!isConnected) {
            toast(getString(R.string.no_network));
            etpConnStatus.setSummary(getString(R.string.no_network));

        }
        return isConnected;

    }

    public String getSignature(String apikey, String action) {
        Log.i(TAG, "getSignature: apikey " + apikey);
        Log.i(TAG, "getSignature: actio " + action);
        ApiSecurity api = new ApiSecurity();
        api.setSecurity(action, apikey);
        return api.getHash();
    }

    private void toast(String toast) {
        Toast.makeText(getActivity(), toast, Toast.LENGTH_SHORT).show();
    }

    private void updatePreference(Preference preference) {
        if (preference instanceof EditTextPreference) {
            EditTextPreference editTextPreference = (EditTextPreference) preference;
            editTextPreference.setSummary(editTextPreference.getText());
        } else if (preference instanceof CheckBoxPreference) {
            CheckBoxPreference checkBoxPreference = (CheckBoxPreference) preference;

        } else if (preference instanceof ListPreference) {
            ListPreference listPreference = (ListPreference) preference;
            listPreference.setSummary(listPreference.getEntry());
        } else if (preference instanceof RingtonePreference) {
            RingtonePreference ringtonePreference = (RingtonePreference) preference;
            String ring = getPreferenceScreen().getSharedPreferences().getString("prefRingtone", null);
            ringtonePreference.setSummary(ring);
        } else if (preference instanceof MultiSelectListPreference) {
            MultiSelectListPreference multiSelectListPreference = (MultiSelectListPreference) preference;
            Set<String> prefMultiList = getPreferenceScreen().getSharedPreferences().getStringSet("prefMultiList", null);
            String[] selectedprefMultiList = prefMultiList.toArray(new String[]{});
            String selected = "";
            for (int i = 0; i < selectedprefMultiList.length; i++) {
                selected += "-" + selectedprefMultiList[i] + "-";
            }
            multiSelectListPreference.setSummary(selected);
        }


    }

    private void setPreference(String key, String value) {
        SharedPreferences.Editor editor = defSharedPrefs.edit();
        editor.putString(key, value);
        editor.commit();
    }

    private void setPreference(String key, boolean value) {
        SharedPreferences.Editor editor = defSharedPrefs.edit();
        editor.putBoolean(key, value);
        editor.commit();
    }

    private void setPreference(String key, int value) {
        SharedPreferences.Editor editor = defSharedPrefs.edit();
        editor.putInt(key, value);
        editor.commit();
    }

    /**
     * new getJson().execute(url,"GET",mUsername, apikey,"profile/" + mUsername);
     */
    class getJson extends AsyncTask<String, String, JSONObject> {
        JSONParser jsonParser = new JSONParser();
        boolean sig = false;
        String hostname;
        private ProgressDialog pDialog;

        @Override
        protected void onPreExecute() {
            pDialog = new ProgressDialog(getActivity());
            pDialog.setMessage(getString(R.string.attempt_conn));
            pDialog.setIndeterminate(false);
            pDialog.setCancelable(true);
            pDialog.show();
        }


        @Override
        protected JSONObject doInBackground(String... args) {

            try {

                String method = args[1];
                HashMap<String, String> params = new HashMap<>();
                params = null;
                String signature = "";
                String url = args[0];
                url = url.replaceAll("/$","");
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
                JSONObject json = jsonParser.makeHttpRequest(url, method, params, args[2], signature);
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
            String kId = "";
            String displayname = "";
            String imageurl = "";
            String title = "";


            if (pDialog != null && pDialog.isShowing()) {
                pDialog.dismiss();
            }
            if (json != null) {
                try {
                    int code = json.getInt("code");
                    Boolean withsignature = json.getBoolean("signedin");
                    if (code != 403) {



                        /**
                         * if authenticated on / check for object->actor
                         */
                        if (json.has("object")) {
                            JSONObject object = json.getJSONObject("object");
                            JSONObject user = object.getJSONObject("actor");
                            JSONObject image = user.getJSONObject("image");

                            kId = user.getString("id");
                            displayname = user.getString("displayName");
                            imageurl = image.getString("url");
                            title = json.getString("title");
                            profilePicture = imageurl;
                            Log.i(TAG, "onPostExecute: imageurl" + profilePicture);
                            urlIsOK = true;

                            etpConnStatus.setSummary(getString(R.string.conn_status_ok));
                            if(!json.has("user")){ //
                                Log.i(TAG, "onPostExecute: hasuser : true, apiok");
                                apiIsOk = true;
                            }

                            urlIsOK = true;
                            Log.i(TAG, "onPostExecute: url ok");
                        }
                        if (json.has("contentTypes")) {
                            Log.i(TAG, "onPostExecute: has ContentTypes");
                            Object o = json.get("contentTypes");
                            urlIsOK = true;
                            if (o instanceof Boolean){
                                Log.i(TAG, "onPostExecute: not authenticated, contenType: false");

                            } else {
                                JSONObject contentTypes = json.getJSONObject("contentTypes");
                                apiIsOk = true;
                                for (int i = 0; i < contentTypes.length(); i++) {
                                    String key = contentTypes.names().getString(i);
                                    JSONObject t = contentTypes.getJSONObject(key);
                                    String catTitle = t.getString("title");
                                    if (catTitle.equals("Repost")) {
                                        hasReactions = true;
                                        Log.i(TAG, "onPostExecute: hasReactions");
                                    }

                                }
                                if(!hasReactions){ // couldn't find category type Reactions/Repost
                                    etpUseReactions.setDefaultValue(false);
                                    etpUseReactions.setSummary(getString(R.string.set_reactions_summary_nok));
                                    etpUseReactions.setEnabled(false);
                                }
                            }
                        }
                        if(json.has("services") ) {
                            Log.i(TAG, "onPostExecute: hasServices opiOk");
                            apiIsOk = true;
                            StringBuilder sbServices = new StringBuilder();
                            JSONObject oServices = json.getJSONObject("services");
                            for (int i = 0; i < oServices.length(); i++) {
                                if (i != 0) {
                                    sbServices.append(";");
                                    Log.i(TAG, "onPostExecute: should append ;");
                                }
                                String key = oServices.names().getString(i);
                                JSONArray arrJ = oServices.getJSONArray(key);
                                JSONObject value = arrJ.getJSONObject(0);
                                String username = value.getString("username");
                                String name = value.getString("name");
                                sbServices.append(key + "::" + username + "::" + name);

                            }
                            String services = sbServices.toString();
                            setPreference("setServices", services);
                            Log.i(TAG, "onPostExecute: services : " + services);

                        }

                        Log.i(TAG, "onPostExecute: " + displayname + ", " + kId + ", " + imageurl);
                        Uri uri = Uri.parse(kId);
                        String profileUsername = uri.getLastPathSegment();
                        Log.i(TAG, "onPostExecute: puser = " + profileUsername + " ==  " + username);
                        if (profileUsername.toLowerCase().equals(username.toLowerCase())) {
                            userIsOk = true;
                            if (!profilePicture.equals("")) {
                                setPreference("setProfilePicture",profilePicture);
                            }
                            Log.i(TAG, "onPostExecute: user ok");

                        } else {
                            userIsOk = false;
                            etpConnStatus.setSummary(getString(R.string.wrong_username));
                            etpConnStatus.setDefaultValue(false);
                            toast(getString(R.string.wrong_username));
                        }
                    } else {
                        toast(getString(R.string.wrong_api));
                        apiIsOk = false;
                        etpConnStatus.setSummary(getString(R.string.conn_status_error_api));

                    }

                    if (userIsOk && urlIsOK && apiIsOk) {
                        etpConnStatus.setSummary(getString(R.string.conn_status_ok) + " " + hostname);
                        etpConnStatus.setDefaultValue(true);
                        etpConnStatus.setEnabled(true);
                        etpConnStatus.setChecked(true);
                        if (!profilePicture.equals("")) {
                            setPreference("setProfilePicture",imageurl);
                        }
                        Log.i(TAG, "onPostExecute: all ok");
                    } else {
                        etpConnStatus.setDefaultValue(false);
                        etpConnStatus.setEnabled(false);
                        etpConnStatus.setChecked(false);
                        Log.i(TAG, "onPostExecute: all nok user" + userIsOk + " url " + urlIsOK + " api "+ apiIsOk);
                    }
                    if (userIsOk && urlIsOK && !apiIsOk) {
                        etpConnStatus.setSummary(getString(R.string.conn_status_error_api));
                    }
                    if (hasReactions) {
                        etpUseReactions.setSummary(getString(R.string.set_reactions_summary));
                        etpUseReactions.setEnabled(true);
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