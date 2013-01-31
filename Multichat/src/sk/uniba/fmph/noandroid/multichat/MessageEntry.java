package sk.uniba.fmph.noandroid.multichat;

import java.io.Serializable;
import java.util.Date;

public class MessageEntry implements Serializable {
	private static final long serialVersionUID = -965781265608444701L;
	private String message;
	private String userID;
	private Date timestamp;
	
	MessageEntry(String u, String m, Date t) {
		userID = u;
		message = m;
		timestamp = t;
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
	
	@Override
	public String toString() {
		return getMessage();
	}

}
