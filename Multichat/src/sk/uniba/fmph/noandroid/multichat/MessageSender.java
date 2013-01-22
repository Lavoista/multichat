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
import org.apache.http.client.methods.HttpPost;
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
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.widget.ArrayAdapter;

public class MessageSender extends AsyncTask<String, String, Void> {
	
	@Override
	protected Void doInBackground(String... params) {
		
		if (params.length != 4 && params.length != 2) {
			return null;
		}
		// Create a new HttpClient and Post Header
		HttpClient httpclient = new DefaultHttpClient();
		HttpPost httppost = new HttpPost("http://lu-pa.sk/vma2012/api");

		try {
			DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mmZ");

			List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
			nameValuePairs.add(new BasicNameValuePair("key", "80e777dd58c8378222e0a7196d13314578614244"));
			nameValuePairs.add(new BasicNameValuePair("lastPullTimestamp", dateFormat.format(new Date())));
			nameValuePairs.add(new BasicNameValuePair("facebookUserId", params[0]));
			nameValuePairs.add(new BasicNameValuePair("facebookToken", Session.getActiveSession().getAccessToken()));
			nameValuePairs.add(new BasicNameValuePair("messageText", params[1]));
			if (params.length == 4) {
				nameValuePairs.add(new BasicNameValuePair("latitude", params[2]));
				nameValuePairs.add(new BasicNameValuePair("longitude", params[3]));
			}

			httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

			// Execute HTTP Post Request
			httpclient.execute(httppost);

		} catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
		} catch (IOException e) {
			// TODO Auto-generated catch block
		} 
		
		return null;
	}

	protected void onPostExecute(Void v) {
	}

}
