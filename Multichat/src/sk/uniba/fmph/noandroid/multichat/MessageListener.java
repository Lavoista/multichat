package sk.uniba.fmph.noandroid.multichat;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
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

import com.facebook.Session;
import com.facebook.SessionState;

public class MessageListener extends AsyncTask<Void, MessageEntry, Void> {

	private Activity context;
	private static final String LAST_TIMESTAMP = "lastTimestamp";

	public MessageListener(Activity context) {
		super();
		this.context = context;
	}

	@Override
	protected Void doInBackground(Void... params) {
		while (true) {
			if (Session.getActiveSession().getState().equals(SessionState.OPENED)) {
				getMessages();
			}
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	protected void getMessages() {

		String url = "http://lu-pa.sk/vma2012/api?";

		// Set up HTTP post
		try {
			// HttpClient is more then less deprecated. Need to change to
			// URLConnection
			HttpClient httpClient = new DefaultHttpClient();

			HttpGet httpGet = new HttpGet();
			List<NameValuePair> httpParams = new ArrayList<NameValuePair>();
			httpParams.add(new BasicNameValuePair("key", "828b251a8cef43be6885f5f2ccc1006d4489db2b"));
			
			SharedPreferences pref = context.getPreferences(Context.MODE_PRIVATE);			
			String date = pref.getString(LAST_TIMESTAMP, "-1");
			DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
			String lastDate = dateFormat.format(new Date());
			pref.edit().putString(LAST_TIMESTAMP, lastDate).apply();
			
			httpParams.add(new BasicNameValuePair("lastPullTimestamp", date));
			String paramString = URLEncodedUtils.format(httpParams, "utf-8");
			url += paramString;
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
						for (int i = 0; i < messages.length(); i++) {
							JSONObject message = messages.getJSONObject(i);
							String userID = String.valueOf(message.getLong("facebookUserID"));
							String text = message.getString("messageText");
							Date datetime = null;
							try {
								datetime = dateFormat.parse(message.getString("timestamp"));
							} catch (java.text.ParseException e) {
								e.printStackTrace();
							}
							double latitude = message.getDouble("lat");
							double longitude = message.getDouble("lon");
							
							updateUser(userID, latitude, longitude);
							MessageEntry messageEntry = new MessageEntry(userID, text, datetime);
							publishProgress(messageEntry);
						}
					}
				} catch (ParseException e) {
					e.printStackTrace();
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}
		} catch (URISyntaxException e) {
			e.printStackTrace();
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	protected void onPostExecute(Void v) {
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
	
			final User u = new User(userID, name, latitude, longitude);
							
			((MainActivity) context).runOnUiThread(new Runnable() {
				@Override
				public void run() {
					((MainActivity) context).addUser(u);
				}
			});
								
		}
	}

}
