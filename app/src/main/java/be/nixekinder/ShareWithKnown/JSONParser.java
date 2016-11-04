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
                        Log.i(TAG, "makeHttpRequest: i != 0 " + sbParams.toString());
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
                            sbParams.append("syndication[]").append("=").append(URLEncoder.encode(service, charset));
                            Log.i(TAG, "makeHttpRequest: services " + service);
                            t++;

                        }
                        i--;
                        Log.i(TAG, "makeHttpRequest: syndication done " + sbParams.toString());
                        Log.i(TAG, "makeHttpRequest: syndication I " + i);

                    } else if (key.equals("photo")) {
                        Log.i(TAG, "makeHttpRequest: Photo");
                        photo = true;
                        selectedFilePath = params.get(key);
                        i--;
                        Log.i(TAG, "makeHttpRequest: photo I " + i);

                    } else {
                        sbParams.append(key).append("=")
                                .append(URLEncoder.encode(params.get(key), charset));
                        Log.i(TAG, "makeHttpRequest: get params " + sbParams.toString());
                    }

                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                i++;
                Log.i(TAG, "makeHttpRequest: I++ " + i);
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
                urlObj = new URL(url);

                /**
                 *
                 */
                try {
                    MultipartUtility multipart = new MultipartUtility(url, charset);

                    multipart.addHeaderField("Accept-Charset", charset);
                    multipart.addHeaderField("X-KNOWN-USERNAME", username);
                    multipart.addHeaderField("X-KNOWN-SIGNATURE", signature);

                    StringTokenizer p = new StringTokenizer(sbParams.toString(), "&");
                    while (p.hasMoreTokens()) {
                        String v = p.nextToken();
                        Log.i(TAG, "makeHttpRequest:v " + v.toString());
                        String[] t = v.toString().split("=");
                        multipart.addFormField(t[0], t[1]);
                    }

                    multipart.addFilePart("photo", selectedFile);
                    List<String> response = multipart.finish();

                    for (String line : response) {
                        Log.i(TAG, "makeHttpRequest: line  " + line);
                        result.append(line);
                        code = 200;

                    }
                } catch (IOException ex) {
                    System.err.println(ex);
                }

            /*    conn = (HttpURLConnection) urlObj.openConnection();
                conn.setDoInput(true);//Allow Inputs
                conn.setDoOutput(true);//Allow Outputs
                conn.setUseCaches(false);//Don't use a cached Copy
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Connection", "Keep-Alive");
               // conn.setRequestProperty("ENCTYPE", "multipart/form-data");
                conn.setRequestProperty("Accept-Charset", charset);
                conn.setRequestProperty("X-KNOWN-USERNAME", username);
                conn.setRequestProperty("X-KNOWN-SIGNATURE", signature);
                conn.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);
                conn.setRequestProperty("photo", selectedFilePath);


                //creating new dataoutputstream
                dataOutputStream = new DataOutputStream(conn.getOutputStream());
                paramsString = sbParams.toString();
                Log.i(TAG, "makeHttpRequest: paramstring PHOTO  " + paramsString);

                dataOutputStream.writeBytes(lineEnd);
                //writing bytes to data outputstream
                dataOutputStream.writeBytes(twoHyphens + boundary + lineEnd);
                dataOutputStream.writeBytes("Content-Disposition: form-data;name=\"photo\";filename=\""
                        + selectedFilePath + "\"" + lineEnd);



                //returns no. of bytes present in fileInputStream
                bytesAvailable = fileInputStream.available();
                //selecting the buffer size as minimum of available bytes or 1 MB
                bufferSize = Math.min(bytesAvailable, maxBufferSize);
                //setting the buffer as byte array of size of bufferSize
                buffer = new byte[bufferSize];

                //reads bytes from FileInputStream(from 0th index of buffer to buffersize)
                bytesRead = fileInputStream.read(buffer, 0, bufferSize);
                //loop repeats till bytesRead = -1, i.e., no bytes are left to read
                while (bytesRead > 0) {
                    //write the bytes read from inputstream
                    dataOutputStream.write(buffer, 0, bufferSize);
                    bytesAvailable = fileInputStream.available();
                    bufferSize = Math.min(bytesAvailable, maxBufferSize);
                    bytesRead = fileInputStream.read(buffer, 0, bufferSize);
                }

                dataOutputStream.writeBytes(lineEnd);
                dataOutputStream.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);

                dataOutputStream.writeBytes(lineEnd);
                dataOutputStream.writeBytes(twoHyphens + boundary + lineEnd);
                dataOutputStream.writeBytes("Content-Disposition: form-data; name=\"" + "title" + "\"" + lineEnd);
                dataOutputStream.writeBytes("Content-Type: text/plain; charset=" + charset +lineEnd);
                dataOutputStream.writeBytes(lineEnd);
                dataOutputStream.writeBytes("A test Title");
                dataOutputStream.writeBytes(lineEnd);
                //dataOutputStream.writeBytes(paramsString);


                InputStream in = new BufferedInputStream(conn.getInputStream());
                BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                /*
                result = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    Log.i(TAG, "makeHttpRequest: line " + line);
                    result.append(line);
                }

                */
                //closing the input and output streams
              /*
                fileInputStream.close();
                dataOutputStream.flush();
                dataOutputStream.close();

*/
            } catch (FileNotFoundException e) {
                e.printStackTrace();

            } catch (MalformedURLException e) {
                e.printStackTrace();

            } catch (IOException e) {
                e.printStackTrace();
            }


        }

        try {
            //Receive the response from the server
            if (!photo) {
                code = conn.getResponseCode();
                Log.i(TAG, "makeHttpRequest: Response code from server " + code);
            }
            if (code == 200) {
                result = new StringBuilder();
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

        result.append("{\"code\": " + code + "}");
        conn.disconnect();


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