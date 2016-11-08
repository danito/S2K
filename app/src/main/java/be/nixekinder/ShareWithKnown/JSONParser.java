package be.nixekinder.ShareWithKnown;

/**
 * Created by danielnix on 15/10/2016.
 */

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.StringTokenizer;

import static android.R.attr.apiKey;
import static android.R.attr.name;
import static android.content.ContentValues.TAG;

public class JSONParser {

    String charset = "UTF-8";
    HttpURLConnection conn;
    DataOutputStream wr;
    StringBuilder result = new StringBuilder();
    URL urlObj;
    JSONObject jObj = null;
    StringBuilder sbParams;
    String paramsString;


    DataOutputStream dataOutputStream;
    String lineEnd = "\r\n";
    String twoHyphens = "--";
    String boundary = "*****";


    int bytesRead, bytesAvailable, bufferSize;
    byte[] buffer;
    int maxBufferSize = 1 * 1024 * 1024;

    boolean withSignature = true;
    int code;
    private boolean photo;
    private FileInputStream fileInputStream;
    private String selectedFilePath;

    public JSONObject makeHttpRequest(String url, String method,
                                      HashMap<String, String> params, String username, String signature) {

        sbParams = new StringBuilder();
        //sbParams.append("");
        int i = 0;
        if (params != null) {
            for (String key : params.keySet()) {
                try {
                    if (i != 0) {
                        sbParams.append("&");
                    }
                    if (key.equals("syndication")) {
                        Log.i(TAG, "makeHttpRequest: Syndication");
                        StringTokenizer services = new StringTokenizer(params.get(key), ";");
                        int t = 0;
                        while (services.hasMoreTokens()) {
                            if (t != 0) {
                                sbParams.append("&");
                            }
                            String service = services.nextToken();
                            if (method.equals("PHOTO")) {
                                sbParams.append("syndication[]").append("=").append(service);
                            } else {
                                sbParams.append("syndication[]").append("=").append(URLEncoder.encode(service, charset));
                            }
                            t++;

                        }
                        if (t == 0) {
                            i--;
                        }

                    } else if (key.equals("photo")) {
                        photo = true;
                        selectedFilePath = params.get(key);
                        i--;

                    } else {
                        if (!method.equals("PHOTO")) {
                            sbParams.append(key).append("=")
                                    .append(URLEncoder.encode(params.get(key), charset));
                        } else {
                            sbParams.append(key).append("=")
                                    .append(params.get(key));
                        }
                        Log.i(TAG, "makeHttpRequest: not a photo " + sbParams.toString());

                    }

                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                i++;
            }
        }

        if (method.equals("POST")) {
            // request method is POST
            try {
                urlObj = new URL(url);

                conn = (HttpURLConnection) urlObj.openConnection();

                conn.setDoOutput(true);

                conn.setRequestMethod("POST");
                conn.setRequestProperty("Accept", "application/json");

                conn.setRequestProperty("Accept-Charset", charset);
                conn.setRequestProperty("X-KNOWN-USERNAME", username);
                conn.setRequestProperty("X-KNOWN-SIGNATURE", signature);


                conn.setReadTimeout(10000);
                conn.setConnectTimeout(15000);

                conn.connect();

                paramsString = sbParams.toString();
                Log.i(TAG, "makeHttpRequest: paramstring " + paramsString);

                wr = new DataOutputStream(conn.getOutputStream());
                wr.writeBytes(paramsString);

                wr.flush();
                wr.close();

            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if (method.equals("GET")) {
            // request method is GET


            if (sbParams.length() != 0) {
                url += "?" + sbParams.toString();
            }

            try {
                urlObj = new URL(url);

                conn = (HttpURLConnection) urlObj.openConnection();

                conn.setDoOutput(false);

                conn.setRequestMethod("GET");

                conn.setRequestProperty("Accept", "application/json");

                conn.setRequestProperty("Accept-Charset", charset);
                conn.setRequestProperty("X-KNOWN-USERNAME", username);
                conn.setRequestProperty("X-KNOWN-SIGNATURE", signature);
                Log.i(TAG, "makeHttpRequest: signature " + signature);
                conn.setConnectTimeout(15000);

                conn.connect();

            } catch (IOException e) {
                e.printStackTrace();
            }

        } else if (method.equals("CHECK")) {
            // request method is GET

            if (sbParams.length() != 0) {
                url += "?" + sbParams.toString();
            }
            withSignature = false;

            try {
                urlObj = new URL(url);

                conn = (HttpURLConnection) urlObj.openConnection();

                conn.setDoOutput(false);

                conn.setRequestMethod("GET");

                conn.setRequestProperty("Accept", "application/json");

                conn.setRequestProperty("Accept-Charset", charset);

                conn.setConnectTimeout(15000);

                conn.connect();

            } catch (IOException e) {
                e.printStackTrace();
            }

        } else if (method.equals("PHOTO")) {
            File selectedFile = new File(selectedFilePath);
            String[] parts = selectedFilePath.split("/");
            final String fileName = parts[parts.length - 1];
            Log.i(TAG, "makeHttpRequest: Photo filename " + fileName);
            photo = true;

            try {
                fileInputStream = new FileInputStream(selectedFile);

                try {
                    MultipartUtility multipart = new MultipartUtility(url, charset, signature, username);

                    multipart.addHeaderField("Accept-Charset", charset);
                    multipart.addHeaderField("X-KNOWN-USERNAME", username);
                    multipart.addHeaderField("X-KNOWN-SIGNATURE", signature);

                    StringTokenizer p = new StringTokenizer(sbParams.toString(), "&");
                    while (p.hasMoreTokens()) {
                        String v = p.nextToken();
                        Log.i(TAG, "makeHttpRequest:v " + v.toString());
                        String[] t = v.toString().split("=");
                        if (t.length == 2) {
                            multipart.addFormField(t[0], t[1]);
                        } else {
                            multipart.addFormField(t[0], "");
                        }

                    }

                    multipart.addFilePart("photo", selectedFile);
                    List<String> response = multipart.finish();
                    code = multipart.getResponseStatus();

                    for (String line : response) {
                        Log.i(TAG, "makeHttpRequest: line  " + line);
                        result.append(line);

                    }
                } catch (IOException ex) {
                    System.err.println(ex);
                }

            } catch (FileNotFoundException e) {
                e.printStackTrace();

            } catch (IOException e) {
                e.printStackTrace();
            }


        }

        try {
            //Receive the response from the server
            if (!photo) {
                code = conn.getResponseCode();
                if (code == 200) {
                    InputStream in = new BufferedInputStream(conn.getInputStream());
                    BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                    String line;
                    while ((line = reader.readLine()) != null) {
                        result.append(line);
                    }
                }
            }


            Log.d("JSON Parser", "result: " + result.toString());

        } catch (IOException e) {
            e.printStackTrace();
        }

        result.append("{\"code\": " + code + "}");
        if (!photo) {
            conn.disconnect();
        }

        // try parse the string to a JSON object
        try {
            Log.i(TAG, "makeHttpRequest: result " + result);
            jObj = new JSONObject(result.toString());
            jObj.put("signedin", withSignature);
            jObj.put("code", code);
        } catch (JSONException e) {
            Log.e("JSON Parser", "Error parsing data " + e.toString());
        }
        Log.i(TAG, "makeHttpRequest: return to mama");

        // return JSON Object
        return jObj;
    }


}