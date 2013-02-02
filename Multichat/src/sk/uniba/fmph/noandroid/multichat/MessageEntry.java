package sk.uniba.fmph.noandroid.multichat;

import java.io.Serializable;
import java.util.Date;

public class MessageEntry implements Serializable {
	private static final long serialVersionUID = -965781265608444701L;
	private String message;
	private String userID;
	private Date timestamp;
	private double latitude;
	private double longitude;
	
	MessageEntry(String u, String m, Date t) {
		userID = u;
		message = m;
		timestamp = t;
	}
	
	MessageEntry(String u, String m, Date t, double lat, double lon) {
		userID = u;
		message = m;
		timestamp = t;
		latitude = lat;
		longitude = lon;
	}
	
	public final String getMessage() {
		return message;
	}
	
	public final Date getTimestamp() {
		return timestamp;
	}
	
	public String getUserID() {
		return userID;
	}
	
	public final void setMessage(String m) {
		message = m;
	}
	
	public final void setTimestamp(Date t) {
		timestamp = t;
	}
	
	public void setUserID(String u) {
		userID = u;
	}
	
	public double getLatitude() {
		return latitude;
	}
	
	public double getLongitude() {
		return longitude;
	}
	
	public void setLatitude(double lat) {
		latitude = lat;
	}
	
	public void setLongitude(double lon) {
		longitude = lon;
	}
	
	@Override
	public String toString() {
		return getMessage();
	}

}
