package sk.uniba.fmph.noandroid.multichat;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipData.Item;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
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
	private HashMap<String, User> users = new HashMap<String, User>();
	private User user;
	private MessageListener messageListener;
	private boolean filtering = false;
	private static final String DATA_FILE = "data.dat";
	private static final String SERVICE_KEY = "80e777dd58c8378222e0a7196d13314578614244";
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
			menu.setHeaderTitle(messageAdapter.getItem(info.position).toString());
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

		switch(menuItemIndex) {
			case 0:
				if(!filtering) {
					messageAdapter.getFilter().filter(messageAdapter.getItem(info.position).getUserID());
					filtering = true;
				}
				else {
					messageAdapter.getFilter().filter(null);
					filtering = false;
				}
				break;
			case 1:
				Intent mapIntent = new Intent(MainActivity.this, MapViewActivity.class);

				Bundle b = new Bundle();
				String[] coords = new String[1];
				User u = users.get(messageAdapter.getItem(info.position).getUserID());
				coords[0] = u.getName() + "#" + u.getLatitude() + "#" + u.getLongitude();

				b.putStringArray("coords", coords);
				mapIntent.putExtras(b);

				MainActivity.this.startActivity(mapIntent);
				break;
			case 2:
				ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE); 
				if(clipboard != null) {
					clipboard.setPrimaryClip(ClipData.newPlainText(null, messageAdapter.getItem(info.position).getMessage()));
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
					nameValuePairs.add(new BasicNameValuePair("facebookUserID", user.getID()));
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
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	private void saveData() {
		ObjectOutputStream oos;
		try {
			oos = new ObjectOutputStream(openFileOutput(DATA_FILE, Context.MODE_PRIVATE));

			oos.writeObject(users);
			
			messageAdapter.getFilter().filter(null);
			ArrayList<MessageEntry> messages = new ArrayList<MessageEntry>();
			for(int i = 0; i < messageAdapter.getCount(); i++) {
				messages.add(messageAdapter.getItem(i));
			}			
			oos.writeObject(messages);
			
			oos.close();
		} catch (IOException e) {
			e.printStackTrace();
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
			
			if(messages.size() > 0) {
				SharedPreferences pref = getPreferences(Context.MODE_PRIVATE);
				DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
				String lastTimestamp = dateFormat.format(messages.get(0).getTimestamp());
				pref.edit().putString(LAST_TIMESTAMP, lastTimestamp).apply();
			}
			
			final ArrayList<MessageEntry> messageArray = messages;
			
			runOnUiThread(new Runnable() {

				@Override
				public void run() {
					messageAdapter.clear();
					messageAdapter.addAll(messageArray);					
				}
				
			});			

			ois.close();
		} catch (IOException e) {
		}
	}

	public User getUser(String userID) {
		if(users != null) {
			return users.get(userID);
		}
		
		return null;
	}

	public void addUser(final User u) {
		final Activity ctx = this;
		
		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				AvatarDownloader task  = new AvatarDownloader(ctx);
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
				    task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, u.getID());
				}
				else {
				    task.execute(u.getID());	
				}
			}
			
		});
		
		users.put(u.getID(), u);
	}

}
