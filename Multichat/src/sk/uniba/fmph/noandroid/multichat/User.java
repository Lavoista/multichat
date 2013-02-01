package sk.uniba.fmph.noandroid.multichat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;



public class User implements Serializable {

	private static final long serialVersionUID = -6051108677105497506L;
	private String id;
	private String name;
	private double latitude;
	private double longitude;
	private Bitmap avatar;
	
	protected class BitmapDataObject implements Serializable {
	    private static final long serialVersionUID = 111696345129311948L;
	    public byte[] imageByteArray;
	}

	/** Included for serialization - write this layer to the output stream. */
	private void writeObject(ObjectOutputStream out) throws IOException {
	    out.writeObject(id);
	    out.writeObject(name);
	    out.writeDouble(latitude);
	    out.writeDouble(longitude);
	    
	    if(avatar != null) {
	    	out.writeBoolean(true);
	    	
		    ByteArrayOutputStream stream = new ByteArrayOutputStream();
		    avatar.compress(Bitmap.CompressFormat.PNG, 100, stream);
		    BitmapDataObject bitmapDataObject = new BitmapDataObject();     
		    bitmapDataObject.imageByteArray = stream.toByteArray();
	
		    out.writeObject(bitmapDataObject);
	    }
	    else {
	    	out.writeBoolean(false);
	    }
	}

	/** Included for serialization - read this object from the supplied input stream. */
	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
	    id = (String) in.readObject();
	    name = (String) in.readObject();
	    latitude = (double) in.readDouble();
	    longitude = (double) in.readDouble();
	    boolean hasAvatar = in.readBoolean();
	    
	    if(hasAvatar) {	
		    BitmapDataObject bitmapDataObject = (BitmapDataObject) in.readObject();
		    avatar = BitmapFactory.decodeByteArray(bitmapDataObject.imageByteArray, 0, bitmapDataObject.imageByteArray.length);
	    }
	}
	
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
