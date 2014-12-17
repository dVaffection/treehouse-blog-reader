package com.example.dv.blogreader;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class MainListActivity extends ListActivity {

    public static final int NUMBER_OF_POSTS = 20;
    public static final String TAG = MainListActivity.class.getSimpleName();
    private JSONObject blogData;
    private ProgressBar progressBar;
    private TextView emptyTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_list);

        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        emptyTextView = (TextView) getListView().getEmptyView();

        if (isNetworkAvailable()) {
            progressBar.setVisibility(View.VISIBLE);

            GetBlogPostsTasks getBlogPostsTasks = new GetBlogPostsTasks();
            getBlogPostsTasks.execute();
        } else {
            Toast.makeText(this, "Network unavailable", Toast.LENGTH_LONG).show();

            emptyTextView.setText(getString(R.string.no_items));
        }
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);

        try {
            JSONArray jsonPosts = blogData.getJSONArray("posts");
            JSONObject jsonPost = jsonPosts.getJSONObject(position);
            String postUrl = jsonPost.getString("url");

            Intent intent = new Intent(this, BlogWebViewActivity.class);
            intent.setData(Uri.parse(postUrl));
            startActivity(intent);
        } catch (JSONException e) {
            Log.e(TAG, "Can not get post url from JSON data", e);
        }

    }

    private boolean isNetworkAvailable() {
        ConnectivityManager manager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = manager.getActiveNetworkInfo();

        return networkInfo != null && networkInfo.isConnected();
    }

    private class GetBlogPostsTasks extends AsyncTask<Object, Void, JSONObject> {

        @Override
        protected JSONObject doInBackground(Object[] params) {
            int responseCode = -1;
            JSONObject jsonResponse = null;

            try {
                URL blogFeedUrl = new URL("http://blog.teamtreehouse.com/api/get_recent_summary/?count=" + NUMBER_OF_POSTS);
                HttpURLConnection connection = (HttpURLConnection) blogFeedUrl.openConnection();
                connection.connect();

                responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    InputStream inputStream = connection.getInputStream();
                    Reader reader = new InputStreamReader(inputStream);

                    char[] chars = new char[connection.getContentLength()];
                    reader.read(chars);

                    String responseData = new String(chars);
                    try {
                        jsonResponse = new JSONObject(responseData);
                    } catch (JSONException e) {
                        Log.e(TAG, "Error parsing response", e);
                    }
                } else {
                    Log.d(TAG, "Unsuccessful response code: " + responseCode);
                }
            } catch (MalformedURLException e) {
                Log.e(TAG, "Blog malformed URL: ", e);
            } catch (IOException e) {
                Log.e(TAG, "Can not establish blog connection: ", e);
            }

            return jsonResponse;
        }

        @Override
        protected void onPostExecute(JSONObject result) {
            blogData = result;
            handleBlogResponse();
        }
    }

    private void handleBlogResponse() {
        progressBar.setVisibility(View.INVISIBLE);

        if (blogData == null) {
            updateDisplayForError();
        } else {
            try {
                JSONArray jsonPosts = blogData.getJSONArray("posts");
                List<Map<String, String>> blogPosts = new ArrayList<>();

                for (int i = 0; i < jsonPosts.length(); i++) {
                    JSONObject jsonPost = jsonPosts.getJSONObject(i);

                    Map<String, String> blogPost = new HashMap<>();
                    blogPost.put("title", Html.fromHtml(jsonPost.getString("title")).toString());
                    blogPost.put("author", Html.fromHtml(jsonPost.getString("author")).toString());

                    blogPosts.add(blogPost);
                }

                String[] keys = {"title", "author"};
                int[] ids = {android.R.id.text1, android.R.id.text2};

                SimpleAdapter adapter = new SimpleAdapter(this, blogPosts, android.R.layout.simple_list_item_2, keys, ids);
                setListAdapter(adapter);
            } catch (JSONException e) {
                Log.e(TAG, "Casting JSON to string error: ", e);
            }
        }
    }

    private void updateDisplayForError() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder
                .setTitle(getString(R.string.dialog_error_title))
                .setMessage(getString(R.string.dialog_error_message))
                .setPositiveButton(android.R.string.ok, null);
        AlertDialog dialog = builder.create();
        dialog.show();

        emptyTextView.setText(getString(R.string.no_items));
    }

}
