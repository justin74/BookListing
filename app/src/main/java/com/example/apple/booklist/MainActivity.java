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
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    /**
     * Tag for the log messages
     */
    private static final String LOG_TAG = MainActivity.class.getSimpleName();
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
            mBookArrayList = savedInstanceState.
                    getParcelableArrayList(Constant.JSONKey.BOOKS_LIST_STATE);
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
        outState.putParcelableArrayList(Constant.JSONKey.BOOKS_LIST_STATE, mBookArrayList);
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
            URL url = null;
            try {
                url = Utility.createUrl(Constant.JSONKey.GOOGLE_BOOK_API +
                        URLEncoder.encode(strings[0], "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                Log.e(LOG_TAG, "UnsupportedEncodingException");
            }

            // Perform HTTP request to the URL and receive a JSON response back
            String jsonResponse = "";

            try {
                jsonResponse = Utility.makeHttpRequest(url);
            } catch (IOException e) {
                Log.e(LOG_TAG, "IOException");
            }

            ArrayList<Book> booksList = Utility.extractBookFromJson(jsonResponse);

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
}
