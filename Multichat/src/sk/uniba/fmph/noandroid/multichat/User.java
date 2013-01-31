package sk.uniba.fmph.noandroid.multichat;

import java.io.Serializable;

import android.content.Context;
import android.graphics.Bitmap;



public class User implements Serializable {

	private static final long serialVersionUID = -6051108677105497506L;
	private String id;
	private String name;
	private double latitude;
	private double longitude;
	private Bitmap avatar;
	
	public User(String id, String name) {
		this.id = id;
		this.name = name;
		latitude = 1000;
		longitude = 1000;
	}
	
	public User(String id, String name, double latitude, double longitude) {
		this.id = id;
		this.name = name;
		this.latitude = latitude;
		this.longitude = longitude;
	}
	
	public void loadAvatarPicture(Context ctx) {
		new AvatarDownloader(ctx).execute(id);
	}
	
	public String getID() {
		return id;
	}
	
	public String getName() {
		return name;
	}
	
	public double getLatitude() {
		return latitude;
	}
	
	public double getLongitude() {
		return longitude;
	}
	
	public Bitmap getAvatar() {
		return this.avatar;
	}
	
	public void setLatitude(double lat) {
		latitude = lat;
	}
	
	public void setLongitude(double lon) {
		longitude = lon;
	}
	
	public void setAvatar(Bitmap a) {
		this.avatar = a;
	}

}
