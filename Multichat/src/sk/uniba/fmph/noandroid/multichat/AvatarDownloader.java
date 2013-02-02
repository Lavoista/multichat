package sk.uniba.fmph.noandroid.multichat;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;

public class AvatarDownloader extends AsyncTask<String, AvatarRecord, Void> {

	private MainActivity context;
	
	AvatarDownloader(Context context) {
		this.context = (MainActivity) context;
	}
	
	@Override
	protected Void doInBackground(String... userIDs) {
		for(String userID : userIDs) {
			Bitmap avatar = null;
		
			try {
				URL img_value = new URL("http://graph.facebook.com/"+ userID +"/picture?type=large");
				avatar = BitmapFactory.decodeStream(img_value.openConnection().getInputStream());
			} catch (MalformedURLException e) {
			} catch (IOException e) {
			}
			
			publishProgress(new AvatarRecord(userID, avatar));
		}
		
		return null;
	}
	
	@Override
	protected void onProgressUpdate(AvatarRecord... avatarRecords) {
		for(AvatarRecord ar : avatarRecords) {
			context.updateUserAvatar(ar.userID, ar.avatar);
		}
	}

}
