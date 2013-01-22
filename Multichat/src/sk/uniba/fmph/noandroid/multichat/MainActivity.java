package sk.uniba.fmph.noandroid.multichat;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.Signature;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.facebook.*;
import com.facebook.model.*;

public class MainActivity extends Activity implements OnClickListener, LocationListener {
	
	public Session session;
	ArrayAdapter<String> listAdapter;
	private String userID = "";
	private MessageSender messageSender;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.activity_main);
		/*
		if (Session.getActiveSession() != null) {
			Session.getActiveSession().close();
		}*/

		// start Facebook Login
		Session.openActiveSession(this, true, new Session.StatusCallback() {

			// callback when session changes state
			@Override
			public void call(Session sess, SessionState state,
					Exception exception) {

				System.out.println("tak do riti vykona toto ci nie? " + sess.getState());
				
				if (sess.isOpened()) {
					
					session = sess;
										
					System.out.println("token " + sess.getAccessToken());

					// make request to the /me API
					Request.executeMeRequestAsync(sess,
							new Request.GraphUserCallback() {

						// callback after Graph API response with user
						// object
						@Override
						public void onCompleted(GraphUser user,
								Response response) {
							System.out.println("why not user");
							if (user != null) {
								TextView welcome = (TextView) findViewById(R.id.welcome);
								welcome.setText("Hello "
										+ user.getName() + "!");
								userID = user.getId();
								startAsync();
							}
						}
					});
				}
			}
		});
		
		Button sendButton = (Button) findViewById(R.id.sendButton);
		sendButton.setOnClickListener(this);
		
		ListView messageList = (ListView) findViewById(R.id.messageView);

		listAdapter = new ArrayAdapter<String>(this, R.layout.textrow,
				new ArrayList<String>());

		messageList.setAdapter(listAdapter);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		Session.getActiveSession().onActivityResult(this, requestCode,
				resultCode, data);
	}

	public void onClick(View view) {
		
		switch (view.getId()) {
		case R.id.sendButton : {
			EditText textField = (EditText) findViewById(R.id.messageField);
			if (textField.getText().toString().length() > 0) {
				sendMessage(textField.getText().toString());
				textField.setText("");
			}
			break;
		/*	if (session != null) {
				session.close();
			}*/
		}
		}
	}
	
	public void sendMessage(String message) {
		
		if (messageSender == null) {
			return;
		}
		
		LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		Location loc = null;
		if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
			lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
			loc = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
		} else {
			if (lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
				lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0,
						0, this);
				loc = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
			}
		}
		
		String[] params = (loc != null) ? new String[4] : new String[2];
		params[0] = userID;
		params[1] = message;
		if (loc != null) {
			params[2] = String.valueOf(loc.getLatitude());
			params[3] = String.valueOf(loc.getLongitude());
		}

		messageSender.execute(params);
		
		/*
		// Create a new HttpClient and Post Header
		HttpClient httpclient = new DefaultHttpClient();
		HttpPost httppost = new HttpPost("http://lu-pa.sk/vma2012/api");

		try {
			// Add your data
		//	String accessToken = (session != null) ? session.getAccessToken() : "-1";
			
			
			List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
			nameValuePairs.add(new BasicNameValuePair("key", "80e777dd58c8378222e0a7196d13314578614244"));
			nameValuePairs.add(new BasicNameValuePair("lastPullTimestamp", dateFormat.format(new Date())));
			nameValuePairs.add(new BasicNameValuePair("facebookUserId", userID));
			nameValuePairs.add(new BasicNameValuePair("facebookToken", Session.getActiveSession().getAccessToken()));
			nameValuePairs.add(new BasicNameValuePair("messageText", message));
			if (loc != null) {
				nameValuePairs.add(new BasicNameValuePair("latitude", String.valueOf(loc.getLatitude())));
				nameValuePairs.add(new BasicNameValuePair("longitude", String.valueOf(loc.getLongitude())));
			}

			httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

			// Execute HTTP Post Request
			HttpResponse response = httpclient.execute(httppost);

		} catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
		} catch (IOException e) {
			// TODO Auto-generated catch block
		} */
	}
	
	private void startAsync() {
		new MessageListener(this).execute();
		messageSender = new MessageSender();
	}
	
	public void showMessage(String message) {
		listAdapter.add(message);
	}

	@Override
	public void onLocationChanged(Location arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onProviderDisabled(String provider) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onProviderEnabled(String provider) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		// TODO Auto-generated method stub
		
	}

}
