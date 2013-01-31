package sk.uniba.fmph.noandroid.multichat;

import java.util.List;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.Menu;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.google.android.maps.OverlayItem;


public class MapViewActivity extends MapActivity {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_map);

		MapView mv = (MapView) findViewById(R.id.mvMain);
		mv.setSatellite(false);
		mv.setBuiltInZoomControls(true);
		MapController mc = mv.getController();
		
		Intent intent = getIntent();
		Bundle b = intent.getExtras();
		String[] coords = b.getStringArray("coords");
		List<Overlay> mapOverlays = mv.getOverlays();
		Drawable drawable = this.getResources().getDrawable(R.drawable.androidmarker);
		FriendItemizedOverlay itemizedoverlay = new FriendItemizedOverlay(drawable, this);

		double minLat = Double.MAX_VALUE;
		double minLon = Double.MAX_VALUE;
		double maxLat = Double.MIN_VALUE;
		double maxLon = Double.MIN_VALUE;
		for (String coord : coords) {
			String[] values = coord.split("\\#");
			if (values.length != 3) {
				continue;
			}
			double lat = Double.valueOf(values[1]);
			double lon = Double.valueOf(values[2]);
			if (lat > 90 || lat < -90 || lon > 180 || lon < -180) {
				continue; 
			}
			GeoPoint friendPoint = new GeoPoint(
					(int) (lat * 1E6),
					(int) (lon * 1E6));
			OverlayItem item = new OverlayItem(friendPoint, values[0], "");
			itemizedoverlay.addOverlay(item);	
			if (lat < minLat) {
				minLat = lat;
			}
			if (lon < minLon) {
				minLon = lon;
			}
			if (lat > maxLat) {
				maxLat = lat;
			}
			if (lon > maxLon) {
				maxLon = lon;
			}
		}
		mapOverlays.add(itemizedoverlay);
		
		GeoPoint center = new GeoPoint(
				(int) ((maxLat + minLat) / 2 * 1E6),
				(int) ((maxLon + minLon) / 2 * 1E6));
		mc.setCenter(center);
		mc.zoomToSpan((int) (Math.abs(maxLat - minLat) * 1.1 * 1E6), 
				(int) (Math.abs(maxLon - minLon) * 1.1 * 1E6));
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}
	
	@Override
	protected boolean isRouteDisplayed() {
		return false;
	}

}
