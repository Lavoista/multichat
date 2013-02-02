package sk.uniba.fmph.noandroid.multichat;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.util.Log;

import com.facebook.Session;
import com.facebook.SessionState;

public class MessageListener extends AsyncTask<Void, MessageEntry, Void> {

	private Activity context;
	private static final String SERVICE_URL = "http://lu-pa.sk/vma2012/api?";
	private static final String SERVICE_KEY = "80e777dd58c8378222e0a7196d13314578614244";
	private static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ssZ";
	private static final String LAST_TIMESTAMP = "lastTimestamp";

	public MessageListener(Activity context) {
		super();
		this.context = context;
	}

	@Override
	protected Void doInBackground(Void... params) {
		ArrayList<MessageEntry> messageArray = new ArrayList<MessageEntry>();
		HashSet<String> userSet = new HashSet<String>();

		// Set up HTTP post
		try {
			// HttpClient is more then less deprecated. Need to change to
			// URLConnection
			HttpClient httpClient = new DefaultHttpClient();

			HttpGet httpGet = new HttpGet();
			List<NameValuePair> httpParams = new ArrayList<NameValuePair>();
			httpParams.add(new BasicNameValuePair("key", SERVICE_KEY));
			
			SharedPreferences pref = context.getPreferences(Context.MODE_PRIVATE);			
			String date = pref.getString(LAST_TIMESTAMP, "-1");
			
			httpParams.add(new BasicNameValuePair("lastPullTimestamp", date));
			String paramString = URLEncodedUtils.format(httpParams, "utf-8");
			String url = SERVICE_URL + paramString;
			httpGet.setURI(new URI(url));
			HttpResponse httpResponse = httpClient.execute(httpGet);
			HttpEntity httpEntity = httpResponse.getEntity();

			if (httpEntity != null) {
				String content = EntityUtils.toString(httpEntity);

				JSONObject messObject = null;
				try {
					messObject = new JSONObject(content);
					String status = messObject.getString("status");
					if (status.equals("success")) {
						JSONArray messages = messObject.getJSONArray("messages");
						
						pref.edit().putString(LAST_TIMESTAMP, messObject.getString("pullTimestamp")).apply();
						
						for (int i = 0; i < messages.length(); i++) {
							JSONObject message = messages.getJSONObject(i);
							String userID = String.valueOf(message.getLong("facebookUserID"));
							String text = message.getString("messageText");
							DateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);
							Date datetime = null;
							try {
								datetime = dateFormat.parse(message.getString("timestamp"));
							} catch (java.text.ParseException e) {
								Log.d("MessageListener Exception", "ParseException: " + e.getMessage());
							}
							double latitude = message.getDouble("lat");
							double longitude = message.getDouble("lon");
							

							updateUser(userID, latitude, longitude);
							if (((MainActivity) context).getUser(userID).getAvatar() == null) {
								userSet.add(userID);
							}
							
							MessageEntry messageEntry = new MessageEntry(userID, text, datetime, latitude, longitude);
							messageArray.add(0, messageEntry);
						}
						
						for(MessageEntry message : messageArray) {
							publishProgress(message);
						}
						
						final String[] userIds = userSet.toArray(new String[userSet.size()]);
						
						((MainActivity) context).runOnUiThread(new Runnable() {
							@Override
							public void run() {
								((MainActivity) context).loadUserAvatars(userIds);
							}
						});	
					}
				} catch (ParseException e) {
					e.printStackTrace();
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}
		} catch (URISyntaxException e) {
			Log.d("MessageListener Exception", "URISyntaxException: " + e.getMessage());
		} catch (ClientProtocolException e) {
			Log.d("MessageListener Exception", "ClientProtocolException: " + e.getMessage());
		} catch (IOException e) {
			Log.d("MessageListener Exception", "IOException: " + e.getMessage());
		}
		
		return null;
	}

	protected synchronized void getMessages() {
		
	}
	
	protected void onProgressUpdate(MessageEntry... messages) {
		for(final MessageEntry message : messages) {			
			((MainActivity) context).showMessage(message);							
		}
    }

	private void updateUser(final String userID, final double latitude, final double longitude) {
		if (((MainActivity) context).getUser(userID) != null) {
			((MainActivity) context).runOnUiThread(new Runnable() {
				@Override
				public void run() {
					((MainActivity) context).updateUserLocation(userID, latitude, longitude);
				}
			});			
		}
		else {
			String name = "Anonym";
	
			HttpClient httpClient = new DefaultHttpClient();
	
			HttpGet httpGet = new HttpGet();
			try {
				httpGet.setURI(new URI("https://graph.facebook.com/" + String.valueOf(userID) + "/?fields=name"));
				HttpResponse httpResponse;
				httpResponse = httpClient.execute(httpGet);
				HttpEntity httpEntity = httpResponse.getEntity();
				if (httpEntity != null) {
					String content = EntityUtils.toString(httpEntity);
					
					JSONObject userObject = null;
					userObject = new JSONObject(content);
					
					if(userObject.has("name")) {
						name = userObject.getString("name");
					}
				}
			} catch (URISyntaxException e) {
				e.printStackTrace();
			} catch (ClientProtocolException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (JSONException e) {
				e.printStackTrace();
			} catch (Exception e) {
				System.err.print(e);
			}
	
			((MainActivity) context).addUser(new User(userID, name, latitude, longitude));							
		}
	}

}
