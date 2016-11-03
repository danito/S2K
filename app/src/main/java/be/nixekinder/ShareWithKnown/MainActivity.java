package be.nixekinder.ShareWithKnown;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BlurMaskFilter;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutCompat;
import android.text.InputType;
import android.util.Log;
import android.util.Patterns;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.RadioGroup;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Transformation;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.StringTokenizer;

import be.nixekinder.preferencestest.R;
import be.nixekinder.ShareWithKnown.Blur;

import static android.R.attr.gravity;
import static android.R.attr.key;
import static android.content.ContentValues.TAG;
import static android.icu.lang.UCharacter.GraphemeClusterBreak.L;

public class MainActivity extends AppCompatActivity implements StatusUpdate.NoticeDialogListener {
    private static final String TAG = "IneedCoffee";
    private static final String PREFS_NAME = "sharedPref";
    SharedPreferences sharedPreferences;
    String prefUsername;
    String prefHostname;
    String prefApikey;
    String prefAction;
    int debug = 0;

    ImageView ivProfilePicture;
    SharedPreferences settings;
    SharedPreferences.Editor editor;

    String prefDisplayname;
    String prefPicture;
    ArrayList<HashMap<String, HashMap<String, String>>> serviceList;
    private String setUsername;
    private String setHostname;
    private String setApiKey;
    private String setSyndication;
    private HashMap<String, String> setSyndicationList = new HashMap<>();
    private boolean setReactions;
    private RadioGroup radioGroup;
    private Button btnDisplay;
    private String statusAction;
    private HashMap<String, String> postParameters;
    private ArrayList<HashMap<String, String>> servicelist;
    private ListView lv;
    private Uri image;

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

    private void setServicesViews() {
        String services = getSharedPref("setServices", "");
        StringTokenizer tServices = new StringTokenizer(services, ";");
        int t = 0;
        while (tServices.hasMoreTokens()) {
            String service = tServices.nextToken();
            // twitter::user
            t++;
        }
    }

    private void removeImage(ImageView iv) {
        iv.setImageResource(R.drawable.ic_add_a_photo_black_24dp);

    }

    private void setImage() {
        Intent intent = new Intent();
        // Show only images, no videos or anything else
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
// Always show the chooser (if there are multiple options available)
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), 0);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 0 && resultCode == RESULT_OK && data != null && data.getData() != null) {
            final Uri uri = data.getData();
            try {
                Transformation blurTransformation = new Transformation() {
                    @Override
                    public Bitmap transform(Bitmap source) {
                        Bitmap blurred = Blur.fastblur(getBaseContext(), source, 10);
                        source.recycle();
                        return blurred;
                    }

                    @Override
                    public String key() {
                        return "blur()";
                    }
                };
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
                // Log.d(TAG, String.valueOf(bitmap));


                final ImageView ivShareImage = (ImageView) findViewById(R.id.new_photo);
                ImageCompressionAsyncTask imageCompression = new ImageCompressionAsyncTask() {
                    @Override
                    protected void onPostExecute(byte[] imageBytes) {
                        // image here is compressed & ready to be sent to the server
                    }
                };
                String ib = uri.toString();
                imageCompression.execute(ib);
                //  imageView.setImageBitmap(bitmap);
                //Picasso.with(getBaseContext()).load(uri).fit().into(ivShareImage);
                Picasso.with(getBaseContext())
                        .load(uri) // thumbnail url goes here
                        .placeholder(R.drawable.ic_add_a_photo_black_24dp)
                        .fit()
                        .transform(blurTransformation)
                        .into(ivShareImage, new Callback() {
                            @Override
                            public void onSuccess() {
                                Picasso.with(getBaseContext())
                                        .load(uri) // image url goes here
                                        .fit()
                                        .placeholder(ivShareImage.getDrawable())
                                        .into(ivShareImage);
                            }

                            @Override
                            public void onError() {
                            }
                        });
                image = uri;
                ImageView ivDelImage = (ImageView) findViewById(R.id.del_image);
                ivDelImage.setVisibility(View.VISIBLE);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void onClickTb(View v) {
        ToggleButton t = (ToggleButton) v;
        myServices o = (myServices) t.getTag();
        String syndication = o.service + "::" + o.username;
        if (t.isChecked()) {
            setSyndicationList.put(syndication, o.service);
        } else {
            setSyndicationList.remove(syndication);
        }
    }

    public void onClickImg(View v) {
        ImageView iv = (ImageView) v;
        setImage();

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
        String myAction = "/";
        String syndication = "";
        StringBuilder sbSyn = new StringBuilder();
        int k = 0;
        for (String key : setSyndicationList.keySet()) {
            if (k != 0) {
                sbSyn.append(";");
            }
            sbSyn.append(key);
            k++;
        }
        if (sbSyn.length() > 0) {
            syndication = sbSyn.toString();
        }
        postParameters = new HashMap<>();
        postParameters.put("syndication", syndication);
        if (!status.equals("")) {
            switch (statusAction) {
                case "status":
                    myAction = "/status/edit";
                    postParameters.put("body", mStatus);
                    if (isUrl(mReply)) {
                        postParameters.put("inreplyto[]", mReply);
                    }

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

        setUsername = sharedPreferences.getString(
                "setUsername", "");
        setHostname = sharedPreferences.getString(
                "setHostname", "");
        setApiKey = sharedPreferences.getString(
                "setApiKey", "");
        setReactions = sharedPreferences.getBoolean("useReactions", false);

        setSyndication = sharedPreferences.getString("setServices", "");


        prefPicture = sharedPreferences.getString("setProfilePicture", "");
        if (!prefPicture.equals("")) {
            Log.i(TAG, "showPrefs: prefPicture " + prefPicture);
            Picasso.with(getBaseContext()).load(prefPicture).transform(new RoundedCornersTransform()).into(ivProfilePicture);
        }
        if (!setHostname.equals("") && !setHostname.equals(null) && setHostname != null) {
            TextView tvHostname = (TextView) findViewById(R.id.profile_host);
            tvHostname.setText(setHostname);
        }
        if (!setUsername.equals("")) {
            TextView tvHostname = (TextView) findViewById(R.id.profile_name);
            tvHostname.setText(setUsername);
        }
        boolean status = sharedPreferences.getBoolean("connStatus", false);
        ViewGroup vgEditStatus = (ViewGroup) findViewById(R.id.edit_status);
        TextView tvNoSettings = (TextView) findViewById(R.id.text_no_settings);
        if (status) {
            Log.i(TAG, "showPrefs: conn ok");
            tvNoSettings.setVisibility(View.GONE);
            vgEditStatus.setVisibility(View.VISIBLE);
            if (debug == 0) { //first time
                showSyndication(setSyndication);
            }
            debug++;
            statusAction = "status";

        } else {
            Log.i(TAG, "showPrefs: connNOK");
            vgEditStatus.setVisibility(View.GONE);
            tvNoSettings.setVisibility(View.VISIBLE);
        }


    }

    private void showSyndication(String services) {
        Log.i(TAG, "showSyndication: " + services);
        if (!services.equals("")) {
            LinearLayout llSyndication = (LinearLayout) findViewById(R.id.syndication);
            StringTokenizer tServices = new StringTokenizer(services, ";");
            int l = tServices.countTokens();
            int r = l % 3;
            int t = l + (3 - r);
            Log.i(TAG, "showSyndication: t = " + t);
            int u = 0;
            for (int i = 0; i < t; i += 3) {
                u++;
                Log.i(TAG, "showSyndication: u = " + u);
                LinearLayout LL = new LinearLayout(this);
                LinearLayoutCompat.LayoutParams LLParams = new LinearLayoutCompat.LayoutParams(LinearLayoutCompat.LayoutParams.MATCH_PARENT, LinearLayoutCompat.LayoutParams.MATCH_PARENT);
                LL.setLayoutParams(LLParams);
                LL.setOrientation(LinearLayout.HORIZONTAL);
                LL.setGravity(Gravity.END);
                llSyndication.addView(LL);
                String ntA = "";
                if (tServices.hasMoreElements()) {
                    ntA = tServices.nextToken();
                    Log.i(TAG, "showSyndication ntA: " + ntA);
                    StringTokenizer stValueA = new StringTokenizer(ntA, "::");
                    String service = stValueA.nextToken();
                    String username = stValueA.nextToken();
                    String name = stValueA.nextToken();
                    myServices myServiceA = new myServices(service, username, name);
                    LayoutInflater inflater = LayoutInflater.from(this);
                    switch (service) {
                        case "facebook":
                            View inflatedLayoutFb = inflater.inflate(R.layout.sl_facebook, null, false);
                            LL.addView(inflatedLayoutFb);
                            TextView tvFb = (TextView) inflatedLayoutFb.findViewById(R.id.service_name);
                            tvFb.setText(name);
                            ToggleButton tbFb = (ToggleButton) findViewById(R.id.fb_toggle);
                            tbFb.setTag(myServiceA);

                            break;
                        case "twitter":
                            View inflatedLayoutTw = inflater.inflate(R.layout.sl_twitter, null, false);
                            LL.addView(inflatedLayoutTw);
                            TextView tvTw = (TextView) inflatedLayoutTw.findViewById(R.id.service_name);
                            tvTw.setText(name);
                            ToggleButton tbTw = (ToggleButton) findViewById(R.id.tw_toggle);
                            tbTw.setTag(myServiceA);

                            break;
                        default:
                            View inflatedLayout = inflater.inflate(R.layout.sl_unknown, null, false);
                            LL.addView(inflatedLayout);
                            TextView tv = (TextView) inflatedLayout.findViewById(R.id.service_name);
                            tv.setText(name);
                            ToggleButton tb = (ToggleButton) findViewById(R.id.sh_toggle);
                            tb.setTag(myServiceA);

                            break;
                    }


                }
                String ntB = "";
                if (tServices.hasMoreElements()) {
                    ntB = tServices.nextToken();
                    StringTokenizer stValueB = new StringTokenizer(ntB, "::");
                    String service = stValueB.nextToken();
                    String username = stValueB.nextToken();
                    String name = stValueB.nextToken();
                    myServices myServiceB = new myServices(service, username, name);
                    LayoutInflater inflater = LayoutInflater.from(this);
                    switch (service) {
                        case "facebook":
                            View inflatedLayoutFb = inflater.inflate(R.layout.sl_facebook, null, false);
                            LL.addView(inflatedLayoutFb);
                            TextView tvFb = (TextView) inflatedLayoutFb.findViewById(R.id.service_name);
                            tvFb.setText(name);
                            ToggleButton tbFb = (ToggleButton) findViewById(R.id.fb_toggle);
                            tbFb.setTag(myServiceB);

                            break;
                        case "twitter":
                            View inflatedLayoutTw = inflater.inflate(R.layout.sl_twitter, null, false);
                            LL.addView(inflatedLayoutTw);
                            TextView tvTw = (TextView) inflatedLayoutTw.findViewById(R.id.service_name);
                            tvTw.setText(name);
                            ToggleButton tbTw = (ToggleButton) findViewById(R.id.tw_toggle);
                            tbTw.setTag(myServiceB);

                            break;
                        default:
                            View inflatedLayout = inflater.inflate(R.layout.sl_unknown, null, false);
                            LL.addView(inflatedLayout);
                            TextView tv = (TextView) inflatedLayout.findViewById(R.id.service_name);
                            tv.setText(name);
                            ToggleButton tb = (ToggleButton) findViewById(R.id.sh_toggle);
                            tb.setTag(myServiceB);

                            break;
                    }

                }
                String ntC = "";
                if (tServices.hasMoreElements()) {
                    ntC = tServices.nextToken();
                    StringTokenizer stValueA = new StringTokenizer(ntC, "::");
                    String service = stValueA.nextToken();
                    String username = stValueA.nextToken();
                    String name = stValueA.nextToken();
                    myServices myServiceC = new myServices(service, username, name);
                    LayoutInflater inflater = LayoutInflater.from(this);
                    switch (service) {
                        case "facebook":
                            View inflatedLayoutFb = inflater.inflate(R.layout.sl_facebook, null, false);
                            LL.addView(inflatedLayoutFb);
                            TextView tvFb = (TextView) inflatedLayoutFb.findViewById(R.id.service_name);
                            tvFb.setText(name);
                            ToggleButton tbFb = (ToggleButton) findViewById(R.id.fb_toggle);
                            tbFb.setTag(myServiceC);

                            break;
                        case "twitter":
                            View inflatedLayoutTw = inflater.inflate(R.layout.sl_twitter, null, false);
                            LL.addView(inflatedLayoutTw);
                            TextView tvTw = (TextView) inflatedLayoutTw.findViewById(R.id.service_name);
                            tvTw.setText(name);
                            ToggleButton tbTw = (ToggleButton) findViewById(R.id.tw_toggle);
                            tbTw.setTag(myServiceC);

                            break;
                        default:
                            View inflatedLayout = inflater.inflate(R.layout.sl_unknown, null, false);
                            LL.addView(inflatedLayout);
                            TextView tv = (TextView) inflatedLayout.findViewById(R.id.service_name);
                            tv.setText(name);
                            ToggleButton tb = (ToggleButton) findViewById(R.id.sh_toggle);
                            tb.setTag(myServiceC);

                            break;
                    }

                }


            }

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


    // Handle incoming text or image

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

    public abstract class ImageCompressionAsyncTask extends AsyncTask<String, Void, byte[]> {

        @Override
        protected byte[] doInBackground(String... strings) {
            if (strings.length == 0 || strings[0] == null)
                return null;
            return ImageUtils.compressImage(strings[0]);
        }

        protected abstract void onPostExecute(byte[] imageBytes);
    }
    /*
    ImageCompressionAsyncTask imageCompression = new ImageCompressionAsyncTask() {
        @Override
        protected void onPostExecute(byte[] imageBytes) {
            // image here is compressed & ready to be sent to the server
        }
    };


    imageCompression.execute(imagePath);// imagePath as a string
     */

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


    public class myServices {
        String service;
        String name;
        String username;

        public myServices() {
            service = null;
            name = null;
            username = null;
        }

        public myServices(String s, String n, String u) {
            service = s;
            name = n;
            username = u;
        }
    }


}
