package sk.uniba.fmph.noandroid.multichat;

public class User {

	private long id;
	private String name;
	private double latitude;
	private double longitude;
	
	public User(long id, String name) {
		this.id = id;
		this.name = name;
		latitude = 1000;
		longitude = 1000;
	}
	
	public User(long id, String name, double latitude, double longitude) {
		this.id = id;
		this.name = name;
		this.latitude = latitude;
		this.longitude = longitude;
	}
	
	public long getID() {
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
	
	public void setLatitude(double lat) {
		latitude = lat;
	}
	
	public void setLongitude(double lon) {
		longitude = lon;
	}
}
