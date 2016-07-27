package br.com.desafiobeerorcoffe.service;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.Iterator;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;

/**
 *
 */
public class HttpService {

    /**
     * Convert json string to json object
     * @param jsonString
     * @return
     */
    public JSONObject convertJSONString2Obj(String jsonString) {
        JSONObject jObj = null;
        try {
            Log.w("convertJSONString2Obj", "JsonString=" + jsonString);
            jObj = new JSONObject(jsonString);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return jObj;
    }

    /**
     * Realiza requisição POST
     * @param serviceUrl : String
     * @param params : String
     * @param apiKey : String
     * @return String
     * @throws IOException
     */
    public String sendPOST(String serviceUrl, String params, String apiKey) throws IOException {
        String jsonString = null;
        HttpsURLConnection conn = null;
        String line;

        URL url;
        try {
            url = new URL(serviceUrl);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("URL inválida: " + serviceUrl);
        }

        Log.w("sendPOST", "param=>" + params);
        byte[] bytes = params.getBytes();

        try {
            conn = (HttpsURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setUseCaches(false);
            conn.setFixedLengthStreamingMode(bytes.length);
            conn.setRequestMethod("POST");

            conn.setRequestProperty("Content-type", "application/json");

            if (apiKey != null && !apiKey.isEmpty()) {
                conn.setRequestProperty("x-api-key", apiKey);
            }

            conn.setRequestProperty("Accept-Encoding", "identity");

            conn.connect();

            // post the request
            OutputStream out = conn.getOutputStream();
            out.write(bytes);
            out.close();

            // handle the response
            int status = conn.getResponseCode();

            Log.w("sendPOST", "Response Status = " + status);
            if (status != HttpURLConnection.HTTP_OK) {
                throw new IOException("Post failed with error code " + status);
            }

            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder stringBuilder = new StringBuilder();

            while ((line = bufferedReader.readLine()) != null) {
                stringBuilder.append(line + '\n');
            }

            jsonString = stringBuilder.toString();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (ProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            conn.disconnect();
        }

        return jsonString;
    }
}
