package sk.uniba.fmph.noandroid.multichat;

import android.graphics.Bitmap;

public class AvatarRecord {
	public Bitmap avatar;
	public String userID;
	
	AvatarRecord(String userID, Bitmap avatar) {
		this.userID = userID;
		this.avatar = avatar;
	}
}
