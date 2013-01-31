package sk.uniba.fmph.noandroid.multichat;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.facebook.Request;
import com.facebook.Response;
import com.facebook.Session;
import com.facebook.SessionState;
import com.facebook.model.GraphUser;

public class MainActivity extends Activity implements OnClickListener,
		LocationListener {

	public Session session;
	private MessageAdapter messageAdapter;
	private ArrayList<MessageEntry> messageArray = new ArrayList<MessageEntry>();
	private HashMap<String, User> users = new HashMap<String, User>();
	private User user;
	private MessageListener messageListener;
	private static final String DATA_FILE = "data.dat";
	private static final String SERVICE_KEY = /*"828b251a8cef43be6885f5f2ccc1006d4489db2b"*/"80e777dd58c8378222e0a7196d13314578614244";
	private static final String SERVICE_URL = "http://lu-pa.sk/vma2012/api/";
	private static final String LAST_TIMESTAMP = "lastTimestamp";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_main);
		
		Button sendButton = (Button) findViewById(R.id.sendButton);
		sendButton.setOnClickListener(this);

		ListView messageList = (ListView) findViewById(R.id.messageView);

		registerForContextMenu(messageList);

		messageAdapter = new MessageAdapter(this, R.layout.message_row,	new ArrayList<MessageEntry>());

		messageList.setAdapter(messageAdapter);

		// start Facebook Login
		Session.openActiveSession(this, true, new Session.StatusCallback() {

			// callback when session changes state
			@Override
			public void call(Session sess, SessionState state, Exception exception) {
				if (sess.isOpened()) {
					session = sess;

					// make request to the /me API
					Request.executeMeRequestAsync(sess,	new Request.GraphUserCallback() {
						// callback after Graph API response with user
						// object
						@Override
						public void onCompleted(GraphUser gUser, Response response) {
							if (gUser != null) {
								TextView welcome = (TextView) findViewById(R.id.welcome);
								welcome.setText(getResources().getString(R.string.dummy_welcome) + " " + gUser.getName() + "!");
								user = new User(gUser.getId(), gUser.getName());
							}
						}
					});
				}
			}
		});
		
		loadMessages();			
	}

	protected void loadMessages() {
		new Thread(new Runnable() {
			@Override
			public void run() {
				loadSavedMessages();
				startListening();	
			}
		}).start();
	}

	@Override
	public void onPause() {
		saveTasks();

		super.onPause();
	}

	@Override
	public void onResume() {
		loadMessages();
		
		super.onResume();
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		Session.getActiveSession().onActivityResult(this, requestCode, resultCode, data);
	}

	public void onClick(View view) {

		switch (view.getId()) {
			case R.id.sendButton: {
				EditText textField = (EditText) findViewById(R.id.messageField);
				if (textField.getText().toString().length() > 0) {
					sendMessage(textField.getText().toString());
					textField.setText("");
				}
				break;
			}
		}
	}

	private void showMap() {
		Intent mapIntent = new Intent(MainActivity.this, MapViewActivity.class);

		Bundle b = new Bundle();
		String[] coords = new String[users.size()];
		int i = 0;
		for (User u : users.values()) {
			coords[i] = u.getName() + "#" + u.getLatitude() + "#" + u.getLongitude();
			i++;
		}
		b.putStringArray("coords", coords);
		mapIntent.putExtras(b);

		MainActivity.this.startActivity(mapIntent);
	}

	public void sendMessage(final String message) {
		LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		Location loc = null;
		if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
			lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
			loc = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
		} else {
			if (lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
				lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, this);
				loc = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
			}
		}

		final Location location = loc;
		
		new Thread(new Runnable() {
			
			@Override
			public void run() {
				try {
					Session session = Session.getActiveSession();
			        String token = "-1";
			        if (session != null && session.isOpened()) {
			        	token = session.getAccessToken();
			        }

			        SharedPreferences pref = getPreferences(Context.MODE_PRIVATE);
			        String lastTimestamp = pref.getString(LAST_TIMESTAMP, "-1");
			        
					List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
					nameValuePairs.add(new BasicNameValuePair("key", SERVICE_KEY));
					nameValuePairs.add(new BasicNameValuePair("lastPullTimestamp", /*lastTimestamp*/"-1"));
					nameValuePairs.add(new BasicNameValuePair("facebookUserID", user.getID()));
					nameValuePairs.add(new BasicNameValuePair("facebookToken", token));
					nameValuePairs.add(new BasicNameValuePair("messageText", message));
					if(location != null) {
						nameValuePairs.add(new BasicNameValuePair("latitude", String.valueOf(location.getLatitude())));
						nameValuePairs.add(new BasicNameValuePair("longitude", String.valueOf(location.getLongitude())));
					}

					HttpClient client = new DefaultHttpClient();
	                HttpPost httppost = new HttpPost(SERVICE_URL);
	                httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs, "utf-8"));
					
	                HttpEntity httpPostEntity = httppost.getEntity();
					if (httpPostEntity != null) {
						String content = EntityUtils.toString(httpPostEntity);


							Log.d("Content", content);
							
					}
	                
	                HttpResponse httpResponse = client.execute(httppost);
	                

					

					HttpEntity httpEntity = httpResponse.getEntity();

					if (httpEntity != null) {
						String content = EntityUtils.toString(httpEntity);

						JSONObject messObject = null;
						try {
							messObject = new JSONObject(content);
							String status = messObject.getString("error");
							System.err.println("Status: " + status);
//							String messages = messObject.getString("messages");
//							System.err.println("Messages: " + messages);
//							String pullTimestamp = messObject.getString("pullTimestamp");
//							System.err.println("PullTimestamp: " + pullTimestamp);
						} catch (ParseException e) {
							e.printStackTrace();
						} catch (JSONException e) {
							e.printStackTrace();
						}
					}
				} catch (ClientProtocolException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}).start();
	}
	
	public void updateUserLocation(String userID, double latitude, double longitude) {
		User userToUpdate = users.get(userID);
		if(userToUpdate != null) {
			userToUpdate.setLatitude(latitude);
			userToUpdate.setLongitude(longitude);
		}
	}
	
	public void updateUserAvatar(String userID, Bitmap avatar) {
		User userToUpdate = users.get(userID);
		if(userToUpdate != null) {
			userToUpdate.setAvatar(avatar);
			messageAdapter.notifyDataSetChanged();
		}
	}

	public HashMap<String, User> getUsers() {
		return users;
	}

	private void startListening() {
		messageListener = new MessageListener(this);
		messageListener.execute();
	}

	public void showMessage(MessageEntry message) {
		messageAdapter.add(message);
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

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {
		case R.id.menu_show_locations:
			showMap();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	private void saveTasks() {
		ObjectOutputStream oos;
		try {
			oos = new ObjectOutputStream(openFileOutput(DATA_FILE, Context.MODE_PRIVATE));

			oos.writeObject(users);
			
			messageArray.clear();

			for (int i = 0; i < messageAdapter.getCount(); i++) {
				messageArray.add(messageAdapter.getItem(i));
			}
			oos.writeObject(messageArray);
			
			oos.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@SuppressWarnings("unchecked")
	private void loadSavedMessages() {
		ObjectInputStream ois;
		
		users.clear();		
		messageArray.clear();
		try {
			ois = new ObjectInputStream(openFileInput(DATA_FILE));
			try {
				users = (HashMap<String, User>) ois.readObject();
				messageArray = (ArrayList<MessageEntry>) ois.readObject();
			} catch (IOException e) {
			} catch (ClassNotFoundException e) {
			}

			ois.close();
		} catch (IOException e) {
		}

		runOnUiThread(returnRes);
	}

	private Runnable returnRes = new Runnable() {

		@Override
		public void run() {
			if (messageArray != null && messageArray.size() > 0) {
				messageAdapter.clear();
				messageAdapter.addAll(messageArray);
			}

			messageAdapter.notifyDataSetChanged();
		}
	};

	public User getUser(String userID) {
		if(users != null) {
			return users.get(userID);
		}
		
		return null;
	}

	public void addUser(User u) {
		u.loadAvatarPicture(this);
		users.put(u.getID(), u);
	}

}
