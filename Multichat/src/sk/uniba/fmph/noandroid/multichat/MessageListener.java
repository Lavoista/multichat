package sk.uniba.fmph.noandroid.multichat;

import java.io.IOException;
import java.io.InputStream;
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
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


import com.facebook.Session;
import com.facebook.SessionState;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.widget.ArrayAdapter;

public class MessageListener extends AsyncTask<String, String, Void> {

	private Context context;
	private String lastDate = "";
	private ArrayList<User> users;
	
	public MessageListener(Context context) {
		super();
		this.context = context;
		users = new ArrayList<User>();
	}
	
	@Override
	protected Void doInBackground(String... params) {
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
			// HttpClient is more then less deprecated. Need to change to URLConnection
			HttpClient httpClient = new DefaultHttpClient();

			HttpGet httpGet = new HttpGet();
			List<NameValuePair> httpParams = new ArrayList<NameValuePair>();
			httpParams.add(new BasicNameValuePair("key", "80e777dd58c8378222e0a7196d13314578614244"));
			DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mmZ");
			String date = (lastDate.equals("")) ? /*"-1"*/"2013-01-22T16:00:00+0100" : lastDate;
			lastDate = dateFormat.format(new Date());
			httpParams.add(new BasicNameValuePair("lastPullTimestamp", date));
			String paramString = URLEncodedUtils.format(httpParams, "utf-8");
			url += paramString;
			httpGet.setURI(new URI(url));
			HttpResponse httpResponse = httpClient.execute(httpGet);
			HttpEntity httpEntity = httpResponse.getEntity();
			
			if(httpEntity != null){
				String content = EntityUtils.toString(httpEntity);
				System.out.println(content);
				
				JSONObject messObject = null;
				try {
					messObject = new JSONObject(content);
					String status = messObject.getString("status");   
					if (status.equals("success")) {
						JSONArray messages = messObject.getJSONArray("messages");
						for (int i = 0; i < messages.length(); i++) {
							JSONObject message = messages.getJSONObject(i);
							long userID = message.getLong("facebookUserID"); 
							String text = message.getString("messageText");
							String time = message.getString("timestamp");
							double latitude = message.getDouble("lat");
							double longitude = message.getDouble("lon");
							User u = updateUser(userID, latitude, longitude);
							((MainActivity) context).runOnUiThread(new GuiChanger(context, time, text, u.getName()));		
						} 
					}
				} catch (ParseException e) {
					e.printStackTrace();
				} catch (JSONException e) {
					e.printStackTrace();
				}/* catch (Exception e) {
					System.out.println(e);
				}*/
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
	
	private User updateUser(long userID, double latitude, double longitude) {
		for (User u : users) {
			if (u.getID() == userID) {
				u.setLatitude(latitude);
				u.setLongitude(longitude);
				return u;
			}
		}
		
		String name = "unknown";
		
		HttpClient httpClient = new DefaultHttpClient();

		HttpGet httpGet = new HttpGet();
		try {
			httpGet.setURI(new URI("https://graph.facebook.com/"+String.valueOf(userID)+"/?fields=name"));
			HttpResponse httpResponse;
			httpResponse = httpClient.execute(httpGet);
			HttpEntity httpEntity = httpResponse.getEntity();
			if(httpEntity != null){
				String content = EntityUtils.toString(httpEntity);
				System.out.println(content);
				
				JSONObject userObject = null;
				userObject = new JSONObject(content);
				name = userObject.getString("name");
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
		
		User u = new User(userID, name, latitude, longitude);
		users.add(u);
		return u;
	}
	
	public ArrayList<User> getUsers() {
		return users;
	}

}
