package sk.uniba.fmph.noandroid.multichat;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.Request;
import com.facebook.Response;
import com.facebook.Session;
import com.facebook.SessionState;
import com.facebook.model.GraphUser;

public class MainActivity extends Activity implements OnClickListener,
		LocationListener {

	public Session session;
	private MessageAdapter messageAdapter;
	private HashMap<String, User> users = new HashMap<String, User>();
	private User user;
	private Timer messageDownloadTimer;
	private static final String DATA_FILE = "data.dat";
	private static final String SERVICE_KEY = "80e777dd58c8378222e0a7196d13314578614244";
	private static final String SERVICE_URL = "http://lu-pa.sk/vma2012/api/";
	private static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ssZ";
	private static final String LAST_TIMESTAMP = "lastTimestamp";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_main);
		
		final TextView textTop = (TextView) findViewById(R.id.textTop);
		
		final Button sendButton = (Button) findViewById(R.id.sendButton);
		sendButton.setOnClickListener(this);

		final ListView messageList = (ListView) findViewById(R.id.messageView);
		registerForContextMenu(messageList);
		
		messageAdapter = new MessageAdapter(this, R.layout.message_row,	new ArrayList<MessageEntry>());
		messageList.setAdapter(messageAdapter);

		if(!isNetworkAvailable()) {
			sendButton.setVisibility(View.GONE);
			findViewById(R.id.messageField).setVisibility(View.GONE);
		}
		else {		
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
									
									textTop.setText(gUser.getName());
									user = new User(gUser.getId(), gUser.getName());
								}
							}
						});
					}
				}
			});
		}
		
		loadMessages();			
	}

	protected void loadMessages() {
		new Thread(new Runnable() {
			@Override
			public void run() {
				loadData();
				startListening();	
			}
		}).start();
	}

	@Override
	public void onPause() {
		saveData();

		super.onPause();
	}

	@Override
	public void onResume() {
		loadMessages();
		
		super.onResume();
	}
	
	@Override
	public void onCreateContextMenu(ContextMenu menu, View view, ContextMenuInfo menuInfo) {
		if (view.getId() == R.id.messageView) {
			AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
			
			User u = users.get(((MessageEntry) messageAdapter.getItem(info.position)).getUserID());
			
			menu.setHeaderTitle(u.getName());
			String[] menuItems = getResources().getStringArray(R.array.message_options);
			for (int i = 0; i < menuItems.length; i++) {
				menu.add(Menu.NONE, i, i, menuItems[i]);
			}
		}
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
		int menuItemIndex = item.getItemId();

		MessageEntry m = messageAdapter.getItem(info.position);
		User u = users.get(m.getUserID());
		
		switch(menuItemIndex) {
			case 0:
				// filter
				messageAdapter.getFilter().filter(messageAdapter.getItem(info.position).getUserID());
				break;
			case 1:
				// show on map
				Intent mapIntent = new Intent(MainActivity.this, MapViewActivity.class);

				Bundle b = new Bundle();
				String[] coords = new String[1];
				coords[0] = u.getName() + "#" + m.getLatitude() + "#" + m.getLongitude();

				b.putStringArray("coords", coords);
				mapIntent.putExtras(b);

				MainActivity.this.startActivity(mapIntent);
				break;
			case 2:
				// open profile
				if(!u.getID().equals("0")) {
					Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.facebook.com/profile.php?id=" + u.getID()));
					startActivity(browserIntent);
				}
				else {
					Toast toast = Toast.makeText(this, R.string.toast_no_profile, Toast.LENGTH_SHORT);
					toast.show();
				}
				break;
			case 3:
				// copy to clipboard
				ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE); 
				if(clipboard != null) {
					clipboard.setPrimaryClip(ClipData.newPlainText(null, messageAdapter.getItem(info.position).getMessage()));
					
					Toast toast = Toast.makeText(this, R.string.toast_text_in_clipboard, Toast.LENGTH_LONG);
					toast.show();
				}
				break;
		}
		
		return true;
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
					nameValuePairs.add(new BasicNameValuePair("lastPullTimestamp", lastTimestamp));
					nameValuePairs.add(new BasicNameValuePair("facebookUserID", "-1"));
					nameValuePairs.add(new BasicNameValuePair("facebookToken", token));
					nameValuePairs.add(new BasicNameValuePair("messageText", message));
					if(location != null) {
						nameValuePairs.add(new BasicNameValuePair("latitude", String.valueOf(location.getLatitude())));
						nameValuePairs.add(new BasicNameValuePair("longitude", String.valueOf(location.getLongitude())));
					}

					HttpClient httpclient = new DefaultHttpClient();
	                HttpPost httppost = new HttpPost(SERVICE_URL);
	                httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs, "utf-8"));
					
	                httpclient.execute(httppost);
				} catch (ClientProtocolException e) {
					Log.d("MainActivity sendMessage Exception", "ClientProtocolException: " + e.getMessage());
				} catch (IOException e) {
					Log.d("MainActivity sendMessage Exception", "IOException: " + e.getMessage());
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

	private boolean isNetworkAvailable() {
	    ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
	    NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
	    return activeNetworkInfo != null;
	}
	
	private void startListening() {
		final Activity ctx = this;
		
		if(isNetworkAvailable() && Session.getActiveSession().getState().equals(SessionState.OPENED)) {		
			if(messageDownloadTimer != null) {
				messageDownloadTimer.cancel();
			}		
			
			messageDownloadTimer = new Timer();
			messageDownloadTimer.schedule(new TimerTask() {

				@Override
				public void run() {
					new MessageListener(ctx).execute();						
				}
				
			}, 0, 1000);			
		}
	}

	public synchronized void showMessage(MessageEntry message) {
		messageAdapter.insert(message, 0);
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
		case R.id.menu_no_filter:
			messageAdapter.getFilter().filter("");
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	private void saveData() {
		if(messageAdapter.getCount() > 0) {
			ObjectOutputStream oos;
			try {
				oos = new ObjectOutputStream(openFileOutput(DATA_FILE, Context.MODE_PRIVATE));
	
				oos.writeObject(users);
				
				ArrayList<MessageEntry> messages = new ArrayList<MessageEntry>();
				for(int i = 0; i < messageAdapter.getCount(); i++) {
					messages.add(messageAdapter.getItem(i));
				}			
				oos.writeObject(messages);
				
				oos.close();
			} catch (IOException e) {
				Log.d("MainActivity saveData Exception", "IOException: " + e.getMessage());
			}
		}
	}

	@SuppressWarnings("unchecked")
	private void loadData() {
		ObjectInputStream ois;
		
		ArrayList<MessageEntry> messages = new ArrayList<MessageEntry>();
		users.clear();	
		
		try {
			ois = new ObjectInputStream(openFileInput(DATA_FILE));
			try {
				users = (HashMap<String, User>) ois.readObject();
				messages = (ArrayList<MessageEntry>) ois.readObject();
			} catch (IOException e) {
			} catch (ClassNotFoundException e) {
			}
			
			SharedPreferences pref = getPreferences(Context.MODE_PRIVATE);
			DateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);
			String lastTimestamp = "-1";			
			if(messages.size() > 0) {				
				lastTimestamp = dateFormat.format(messages.get(0).getTimestamp());
			}			
			pref.edit().putString(LAST_TIMESTAMP, lastTimestamp).apply();
			
			final ArrayList<MessageEntry> messageArray = messages;
			
			runOnUiThread(new Runnable() {

				@Override
				public void run() {
					messageAdapter.clear();
					messageAdapter.addAll(messageArray);	
					messageAdapter.notifyDataSetChanged();
				}
				
			});			

			ois.close();
		} catch (IOException e) {
			Log.d("MainActivity loadData Exception", "IOException: " + e.getMessage());
		}
	}

	public User getUser(String userID) {
		if(users != null) {
			return users.get(userID);
		}
		
		return null;
	}

	public void loadUserAvatars(final String[] userIds) {
		final Activity ctx = this;
		
		runOnUiThread(new Runnable() {

			@Override
			public void run() {				
				new AvatarDownloader(ctx).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, userIds);
			}
			
		});
	}
	
	public void addUser(final User u) {		
		users.put(u.getID(), u);
	}

}
