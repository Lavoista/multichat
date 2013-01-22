package sk.uniba.fmph.noandroid.multichat;

import android.content.Context;

public class GuiChanger implements Runnable {
	
	Context context;
	String time;
	String text;
	String userName;
	
	public GuiChanger(Context context, String time, String text, String userName) {
		this.context = context;
		this.time = time;
		this.text = text;
		this.userName = userName;
	}

	@Override
	public void run() {
    	((MainActivity) context).showMessage(time + " " + userName + " " + text);
	}

}
