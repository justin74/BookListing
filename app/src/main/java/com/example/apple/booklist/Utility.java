package com.example.apple.booklist;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.text.TextUtils;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;


/**
 * Created by justin on 2017/7/13.
 */

public class Utility {
    private static final String LOG_TAG = Utility.class.getSimpleName();
    private static MyApplication application;

    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    /**
     * Returns new URL object from the given string URL.
     */
    public static URL createUrl(String stringUrl) {
        URL url = null;
        try {
            url = new URL(stringUrl);
        } catch (MalformedURLException exception) {
            Log.e(LOG_TAG, "Error with creating URL", exception);
            return null;
        }
        return url;
    }

    /**
     * Make an HTTP request to the given URL and return a String as the response.
     */
    public static String makeHttpRequest(URL url) throws IOException {
        String jsonResponse = "";
        if (url == null) {
            return jsonResponse;
        }

        HttpURLConnection urlConnection = null;
        InputStream inputStream = null;

        try {
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.setReadTimeout(10000 /* milliseconds */);
            urlConnection.setConnectTimeout(15000 /* milliseconds */);
            urlConnection.connect();
            if (urlConnection.getResponseCode() == 200) {
                inputStream = urlConnection.getInputStream();
                jsonResponse = readFromStream(inputStream);
            } else {
                Log.e(LOG_TAG, "Error response code " + urlConnection.getResponseCode());
            }

        } catch (IOException e) {
            Log.e(LOG_TAG, "IOException");
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
            if (inputStream != null) {
                // function must handle java.io.IOException here
                inputStream.close();
            }
        }
        return jsonResponse;
    }

    /**
     * Convert the {@link InputStream} into a String which contains the
     * whole JSON response from the server.
     */
    public static String readFromStream(InputStream inputStream) throws IOException {
        StringBuilder output = new StringBuilder();
        if (inputStream != null) {
            InputStreamReader inputStreamReader = new InputStreamReader(
                    inputStream, Charset.forName("UTF-8"));
            BufferedReader reader = new BufferedReader(inputStreamReader);
            String line = reader.readLine();
            while (line != null) {
                output.append(line);
                line = reader.readLine();
            }
        }
        return output.toString();
    }

    public static ArrayList<Book> extractBookFromJson(String bookJSON) {
        ArrayList<Book> booksArrayList = new ArrayList<>();
        if (TextUtils.isEmpty(bookJSON)) {
            return null;
        }

        try {
            JSONObject baseJsonResponse = new JSONObject(bookJSON);
            JSONArray itemsArray = baseJsonResponse.getJSONArray(Constant.JSONKey.items);

            //If there are results in the items array
            for (int i = 0; i < itemsArray.length(); i++) {
                JSONObject itemJSONObject = itemsArray.getJSONObject(i);
                JSONObject volumeInfo = itemJSONObject.getJSONObject(Constant.JSONKey.volumeInfo);

                //Get book title.
                String title = volumeInfo.getString(Constant.JSONKey.title);

                //Get book authors.
                String authors = "";
                if (volumeInfo.isNull(Constant.JSONKey.authors)) {
                    authors = application.getContext().getString(R.string.default_author);
                } else {
                    JSONArray authorsArray = volumeInfo.getJSONArray(Constant.JSONKey.authors);
                    for (int authorsCount = 0; authorsCount < authorsArray.length(); authorsCount++) {
                        if (authorsCount == authorsArray.length() - 1) {
                            authors = authors + authorsArray.getString(authorsCount);
                        } else {
                            authors = authors + authorsArray.getString(authorsCount) + ", ";
                        }
                    }
                }
                booksArrayList.add(new Book(title, authors));
            }

            return booksArrayList;
        } catch (JSONException e) {
            Log.e(LOG_TAG, "Problem parsing the book JSON results", e);
        }
        return null;
    }
}
