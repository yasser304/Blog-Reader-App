package com.ysr.blogreader;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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

public class MainListActivity extends ListActivity {
	
	public static final int NUMBER_OF_POSTS = 20;
	public static final String TAG = MainListActivity.class.getSimpleName();
	protected JSONObject mBlogData;
	protected ProgressBar mProgressBar;
	private static final String KEY_TITLE = "title";
	private static final String KEY_AUTH = "author";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_list);
        
        mProgressBar = (ProgressBar) findViewById(R.id.progressBar1);
   
        if(isNetworkAvaliable()){
        	mProgressBar.setVisibility(View.VISIBLE);
        	GetBlogPostsTask getBlogPostsTask = new GetBlogPostsTask();
            getBlogPostsTask.execute();      	
        }else{
        	Toast.makeText(this, "Network Unavailable!", Toast.LENGTH_LONG).show();
        }
        
        //Toast.makeText(this, getString(R.string.no_items), Toast.LENGTH_LONG).show();
    }


	private boolean isNetworkAvaliable() {
		ConnectivityManager manager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo= manager.getActiveNetworkInfo();
		
		boolean isAvailable = false;
		if(networkInfo != null && networkInfo.isConnected()){
			isAvailable = true;
		}
		
		return isAvailable;
		
	}
	
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		try {
			JSONArray jsonPosts = mBlogData.getJSONArray("posts");
			JSONObject jsonURL = jsonPosts.getJSONObject(position);
			String URL = jsonURL.getString("url");
			Intent intent = new Intent(this, BlogWebViewActivity.class);
			intent.setData(Uri.parse(URL));
			startActivity(intent);
			
		} catch (JSONException e) {
			Log.e(TAG,"JSONExpception "+e, e);
		}
	}


	public void handleBlogResponse() {
    	mProgressBar.setVisibility(View.INVISIBLE);
		//Log.d(TAG, "In updateTitle()");
		if(mBlogData == null){
			updateDisplayforError();
		}else{
			try {
				JSONArray jsonData = mBlogData.getJSONArray("posts");
				ArrayList<HashMap<String,String>> blogPosts = new ArrayList<HashMap<String,String>>();
        		for (int i=0; i<jsonData.length(); i++){
        			JSONObject jsonPosts= jsonData.getJSONObject(i);
        			String title = jsonPosts.getString(KEY_TITLE);
        			title = Html.fromHtml(title).toString();
        			String author = jsonPosts.getString(KEY_AUTH);
        			author = Html.fromHtml(author).toString();
        			
        			HashMap<String,String> blogPost = new HashMap<String, String>();
        			blogPost.put(KEY_TITLE, title);
        			blogPost.put(KEY_AUTH, author);
        			
        			blogPosts.add(blogPost);
        		}
        		
        		String[] keys = {KEY_TITLE, KEY_AUTH};
        		int[] ids = {android.R.id.text1,android.R.id.text2 };
        		SimpleAdapter adapter = new SimpleAdapter(this, blogPosts, android.R.layout.simple_list_item_2, keys, ids);
        		setListAdapter(adapter);
        		
			} catch (JSONException e) {
				Log.e(TAG, "JSON Exception caught"+e, e);
			}
		}
		
	}


	private void updateDisplayforError() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(getString(R.string.error_title));
		builder.setMessage(getString(R.string.error_message));
		builder.setPositiveButton(android.R.string.ok, null);
		
		AlertDialog dialog = builder.create();
		dialog.show();
		
		TextView emptyTextView = (TextView) getListView().getEmptyView();
		emptyTextView.setText(getString(R.string.empty_list));
	}
	
	 private class GetBlogPostsTask extends AsyncTask<Object, Void, JSONObject> {

			@Override
			protected JSONObject doInBackground(Object... arg0) {
				int responseCode = -1;
				JSONObject jsonObj = null;
				
		        try {
		        	//URL blogFeedUrl = new URL("https://ajax.googleapis.com/ajax/services/search/web?v=1.0&q=Android");
		        	URL blogFeedUrl = new URL("http://blog.teamtreehouse.com/api/get_recent_summary/?count=" + NUMBER_OF_POSTS);
		        	HttpURLConnection connection = (HttpURLConnection) blogFeedUrl.openConnection();
		        	connection.connect();
		        	
		        	responseCode = connection.getResponseCode();
		        	if(responseCode == HttpURLConnection.HTTP_OK){
		        		Log.i(TAG, "Code: " + responseCode);
		        		InputStream inputStream = connection.getInputStream();
		        		Reader reader = new InputStreamReader(inputStream);
		        		int contentLength = connection.getContentLength();
		        		char[] charArray = new char[contentLength];
		        		reader.read(charArray);
		        		String contentData = new String(charArray);
		        		
		        		jsonObj = new JSONObject(contentData);  		
		        	}else{
		        		Log.i(TAG, "Unsuccessful error code: " + responseCode);
		        	}
		        	
		        }
		        catch (MalformedURLException e) {
		        	Log.e(TAG, "URLException caught: "+e, e);
		        }
		        catch (IOException e) {
		        	Log.e(TAG, "IOException caught: "+e, e);
		        }
		        catch (Exception e) {
		        	Log.e(TAG, "Exception caught: "+e, e);
		        }
		        
		        return jsonObj;
			}
	    	
			
			@Override
			protected void onPostExecute(JSONObject result){
				mBlogData = result;
				handleBlogResponse();
			}
	    }
}
