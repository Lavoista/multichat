package sk.uniba.fmph.noandroid.multichat;

import android.content.Context;

public class GuiChanger implements Runnable {
	
	Context context;
	String time;
	String text;
	long userID;
	
	public GuiChanger(Context context, String time, String text, long userID) {
		this.context = context;
		this.time = time;
		this.text = text;
		this.userID = userID;
	}

	@Override
	public void run() {
    	((MainActivity) context).showMessage(time + " " + userID + " " + text);
	}

}
