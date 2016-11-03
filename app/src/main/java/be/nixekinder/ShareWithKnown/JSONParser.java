package be.nixekinder.ShareWithKnown;

/**
 * Created by danielnix on 15/10/2016.
 */

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.util.Log;

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
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.StringTokenizer;

import static android.content.ContentValues.TAG;

public class JSONParser {

    String charset = "UTF-8";
    HttpURLConnection conn;
    DataOutputStream wr;
    StringBuilder result;
    URL urlObj;
    JSONObject jObj = null;
    StringBuilder sbParams;
    String paramsString;

    String lineEnd = "\r\n";
    String twoHyphens = "--";
    String boundary = "------------------------afb19f4aeefb356c";
    int bytesRead, bytesAvailable, bufferSize;
    byte[] buffer;
    int maxBufferSize = 1 * 1024 * 1024;
    File sourceFile;

    boolean withSignature = true;
    int code;
    private boolean photo;
    private FileInputStream fileInputStream;

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
                        StringTokenizer services = new StringTokenizer(params.get(key), ";");
                        int t = 0;
                        while (services.hasMoreTokens()) {
                            if (t != 0) {
                                sbParams.append("&");
                            }
                            String service = services.nextToken();
                            sbParams.append("syndication[]").append("=").append(URLEncoder.encode(service, charset));
                            Log.i(TAG, "makeHttpRequest: services " + service);
                            t++;
                        }

                    } else if (key.equals("photo")) {
                        photo = true;
                        sourceFile = new File(params.get(key));
                        Log.i(TAG, "makeHttpRequest: getkeyphoto " + params.get(key));
                        try {
                            fileInputStream = new FileInputStream(sourceFile);
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        }

                        sbParams.append(key).append("=")
                                .append(URLEncoder.encode(params.get(key) + ";filename=alert.jpeg;type=image/jpeg", charset))
                        ;
                    } else {
                        sbParams.append(key).append("=")
                                .append(URLEncoder.encode(params.get(key), charset));
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
                if (photo) {
                    conn.setRequestProperty("ENCTYPE", "multipart/form-data");
                    conn.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);
                    conn.setRequestProperty("file", sourceFile.getName());
                }

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
                if (photo) {
                    Log.i(TAG, "makeHttpRequest: getname  " + sourceFile.getName());
                    wr.writeBytes(twoHyphens + boundary + lineEnd);
                    wr.writeBytes("Content-Disposition: form-data; name=\"file\";filename=\"" + sourceFile.getName() + "\"" + lineEnd);
                    wr.writeBytes(lineEnd);
                    // create a buffer of  maximum size
                    bytesAvailable = fileInputStream.available();
                    bufferSize = Math.min(bytesAvailable, maxBufferSize);
                    buffer = new byte[bufferSize];
                    // read file and write it into form...
                    bytesRead = fileInputStream.read(buffer, 0, bufferSize);
                    while (bytesRead > 0) {
                        wr.write(buffer, 0, bufferSize);
                        bytesAvailable = fileInputStream.available();
                        bufferSize = Math.min(bytesAvailable, maxBufferSize);
                        bytesRead = fileInputStream.read(buffer, 0, bufferSize);
                    }
                    wr.writeBytes(lineEnd);
                    wr.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);
                }

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

        }

        try {
            //Receive the response from the server
            code = conn.getResponseCode();
            Log.i(TAG, "makeHttpRequest: Response code from server " + code);
            result = new StringBuilder();
            if (code == 200) {
                InputStream in = new BufferedInputStream(conn.getInputStream());
                BufferedReader reader = new BufferedReader(new InputStreamReader(in));

                String line;

                while ((line = reader.readLine()) != null) {
                    result.append(line);
                }
            }

            Log.d("JSON Parser", "result: " + result.toString());

        } catch (IOException e) {
            e.printStackTrace();
        }

        result.append("{\"code\": "+code+"}");
        conn.disconnect();


        // try parse the string to a JSON object
        try {
            Log.i(TAG, "makeHttpRequest: result " + result);
            jObj = new JSONObject(result.toString());
            jObj.put("signedin", withSignature);
            jObj.put("code",code);
        } catch (JSONException e) {
            Log.e("JSON Parser", "Error parsing data " + e.toString());
        }
        Log.i(TAG, "makeHttpRequest: return to mama");

        // return JSON Object
        return jObj;
    }
}