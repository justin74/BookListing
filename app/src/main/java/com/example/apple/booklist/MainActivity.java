package com.example.apple.booklist;

import android.app.SearchManager;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

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

public class MainActivity extends AppCompatActivity {

    /**
     * Tag for the log messages
     */
    private static final String LOG_TAG = MainActivity.class.getSimpleName();
    private static final String GOOGLE_BOOK_API =
            "https://www.googleapis.com/books/v1/volumes?maxResults=40&q=";
    private static final String BOOKS_LIST_STATE = "book list state";

    private TextView emptyListTextView;
    private ListView listView;
    private ProgressBar progressBar;
    private ArrayList<Book> mBookArrayList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();

        if (savedInstanceState != null) {
            mBookArrayList = savedInstanceState.getParcelableArrayList(BOOKS_LIST_STATE);
            if (mBookArrayList != null) {
                Log.d(LOG_TAG, "mBookArrayList = " + mBookArrayList.size());
                updateUI();
            }
        }
    }

    /**
     * Initialize views.
     */
    private void initView() {
        emptyListTextView = (TextView) findViewById(R.id.empty_list_text_view);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        listView = (ListView) findViewById(R.id.book_list_view);
        listView.setEmptyView(emptyListTextView);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putParcelableArrayList(BOOKS_LIST_STATE, mBookArrayList);
        super.onSaveInstanceState(outState);
    }

    /**
     * Update list view.
     */
    private void updateUI() {
        BookAdapter bookAdapter = new BookAdapter(this, mBookArrayList);
        listView.setAdapter(bookAdapter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.searchview, menu);

        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        SearchView searchView = (SearchView) menu.findItem(R.id.search).getActionView();
        if (searchView != null) {
            searchView.setSearchableInfo(searchManager
                    .getSearchableInfo(getComponentName()));
            searchView.setIconifiedByDefault(false);
        }

        SearchView.OnQueryTextListener queryTextListener = new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                Log.d(LOG_TAG, "submit: " + query);

                if (Utility.isNetworkAvailable(MainActivity.this)) {
                    query = query.trim();
                    new BookAsyncTask().execute(query);
                } else {
                    Toast.makeText(MainActivity.this,
                            "Network not available!", Toast.LENGTH_LONG).show();
                }
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        };

        searchView.setOnQueryTextListener(queryTextListener);
        return super.onCreateOptionsMenu(menu);
    }


    /**
     * {@link AsyncTask} to perform the network request on a background thread, and then
     * update the UI.
     */
    private class BookAsyncTask extends AsyncTask<String, Void, ArrayList<Book>> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            emptyListTextView.setVisibility(View.GONE);
            listView.setVisibility(View.GONE);
            progressBar.setVisibility(View.VISIBLE);
        }

        @Override
        protected ArrayList<Book> doInBackground(String... strings) {
            // Create URL object
            URL url = createUrl(GOOGLE_BOOK_API + strings[0]);

            // Perform HTTP request to the URL and receive a JSON response back
            String jsonResponse = "";

            try {
                jsonResponse = makeHttpRequest(url);
            } catch (IOException e) {
                Log.e(LOG_TAG, "IOException");
            }

            ArrayList<Book> booksList = extractBookFromJson(jsonResponse);

            return booksList;
        }

        @Override
        protected void onPostExecute(ArrayList<Book> books) {
            progressBar.setVisibility(View.GONE);
            emptyListTextView.setVisibility(View.VISIBLE);
            listView.setVisibility(View.VISIBLE);
            mBookArrayList = books;
            updateUI();
        }
    }

    /**
     * Returns new URL object from the given string URL.
     */
    private URL createUrl(String stringUrl) {
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
    private String makeHttpRequest(URL url) throws IOException {
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
    private String readFromStream(InputStream inputStream) throws IOException {
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

    private ArrayList<Book> extractBookFromJson(String bookJSON) {
        ArrayList<Book> booksArrayList = new ArrayList<>();
        if (TextUtils.isEmpty(bookJSON)) {
            return null;
        }

        try {
            JSONObject baseJsonResponse = new JSONObject(bookJSON);
            JSONArray itemsArray = baseJsonResponse.getJSONArray("items");

            //If there are results in the items array
            for (int i = 0; i < itemsArray.length(); i++) {
                JSONObject itemJSONObject = itemsArray.getJSONObject(i);
                JSONObject volumeInfo = itemJSONObject.getJSONObject("volumeInfo");

                //Get book title.
                String title = volumeInfo.getString("title");

                //Get book authors.
                String authors = "";
                if (volumeInfo.isNull("authors")) {
                    authors = getString(R.string.default_author);
                } else {
                    JSONArray authorsArray = volumeInfo.getJSONArray("authors");
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
