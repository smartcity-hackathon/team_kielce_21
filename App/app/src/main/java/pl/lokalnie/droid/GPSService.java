package pl.lokalnie.droid;


import android.app.AlertDialog;
import android.app.Service;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.LocalBroadcastManager;

import java.text.DateFormat;
import java.util.Date;


public class GPSService extends Service implements LocationListener {
    // private static final String TAG = "GPSService";

    public double longitude;
    public double latitude;

    public LocationManager lm;

    public static double accuracy;
    public static double highestAccuracy = 100;
    private final IBinder mBinder = new LocalBinder();
    public static boolean isStarting = false;
    private static GPSService instance = null;

    public static boolean isInstanceCreated() {
        return instance != null;
    }

    public static GPSService getInstanceCreated() {
        return instance;
    }

    public static Date lastRun = new Date(100, 1, 1, 1, 1);
    public static boolean isRunning = false;

    @Override
    public void onCreate() {
        //Toast.makeText(this, "GPSService Created", Toast.LENGTH_SHORT).show();
        instance = this;
        isStarting = false;
        lm = (LocationManager) getSystemService(LOCATION_SERVICE);
        getLocations();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        isStarting = false;
        return Service.START_STICKY;
    }

    public void getLocations() {
        isStarting = false;
        if (lm != null) {
            Location loc;
            if (lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return;
                }
                lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 15000, 5, this);
                loc = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                if (loc != null) {
                    latitude = loc.getLatitude();
                    longitude = loc.getLongitude();
                    MainActivity.gpsLatitude = latitude;
                    MainActivity.gpsLonglitude = longitude;
                    MainActivity.gpsLastUpdateTime = DateFormat.getDateTimeInstance().format(new Date());
                }
            }

            if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 15000, 5, this);
                loc = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if (loc != null) {
                    longitude = loc.getLongitude();
                    latitude = loc.getLatitude();

                    MainActivity.gpsLatitude = latitude;
                    MainActivity.gpsLonglitude = longitude;
                    MainActivity.gpsLastUpdateTime = DateFormat.getDateTimeInstance().format(new Date());

                }

            }

            MainActivity.gpsLatitude = latitude;
            MainActivity.gpsLonglitude = longitude;
            MainActivity.gpsLastUpdateTime = DateFormat.getDateTimeInstance().format(new Date());

            lm.requestSingleUpdate(LocationManager.GPS_PROVIDER, new LocationListener() {
                @Override
                public void onLocationChanged(Location location) {
                    longitude = location.getLongitude();
                    latitude = location.getLatitude();

                    MainActivity.gpsLatitude = latitude;
                    MainActivity.gpsLonglitude = longitude;
                    MainActivity.gpsLastUpdateTime = DateFormat.getDateTimeInstance().format(new Date());

                }

                @Override
                public void onStatusChanged(String s, int i, Bundle bundle) {

                }

                @Override
                public void onProviderEnabled(String s) {

                }

                @Override
                public void onProviderDisabled(String s) {

                }
            }, null);

            recordLocation();
        }
    }

    public void stopUsingGPS() {
        recordLocation();
//		if(lm != null){
//			lm.removeUpdates(this);
//		}
    }

    public void startUsingGPS() {
        getLocations();
//		if(lm != null){
//			lm.removeUpdates(this);
//		}
    }


    @Override
    public void onDestroy() {
        recordLocation();
        if (lm != null) {
            lm.removeUpdates(this);
        }
    }


    @Override
    public void onStart(Intent intent, int startid) {
        //Toast.makeText(this, "GPS Service Started", Toast.LENGTH_LONG).show();
        getLocations();
    }


    @Override
    public void onLocationChanged(Location location) {
        longitude = location.getLongitude();
        latitude = location.getLatitude();
        recordLocation();
    }

    public void recordLocation() {
        isStarting=false;
        //Toast.makeText(this, latitude + ", " + longitude, Toast.LENGTH_SHORT).show();
       // GeneralFunctions gf = new GeneralFunctions(getApplicationContext());

       // OSGB o = new OSGB(UTM.FROM_LAT_LONG);
       // PVector p = new PVector();
       // p.y = (float) latitude;
       // p.x = (float) longitude;
        MainActivity.gpsLatitude = latitude;
        MainActivity.gpsLonglitude = longitude;
        MainActivity.gpsLastUpdateTime = DateFormat.getDateTimeInstance().format(new Date());
       // PVector ret = o.transformCoords(p);

       // gf.addEvent(IncidentTimes.TABLE_NAME, IncidentTimes.FROM[9], Float.toString(ret.x));
       // gf.addEvent(IncidentTimes.TABLE_NAME, IncidentTimes.FROM[10], Float.toString(ret.y));
       // gf.addEvent(IncidentTimes.TABLE_NAME, IncidentTimes.FROM[11], Double.toString(accuracy));

    }

//	private void buildAlertMessageTurnOffGps() {
//		final AlertDialog.Builder builder = new AlertDialog.Builder(this);
//		builder.setMessage(
//				"Incident location has been captured. Do you want to turn off GPS?")
//				.setCancelable(false)
//				.setPositiveButton("Yes",
//						new DialogInterface.OnClickListener() {
//							public void onClick(final DialogInterface dialog,
//									final int id) {
//								Intent intent = new Intent(
//										Settings.ACTION_LOCATION_SOURCE_SETTINGS);
//								intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//								startActivity(intent);
//							}
//						})
//				.setNegativeButton("No", new DialogInterface.OnClickListener() {
//					public void onClick(final DialogInterface dialog,
//							final int id) {
//						dialog.cancel();
//					}
//				});
//		final AlertDialog alert = builder.create();
//		alert.show();
//	}

    public void onProviderDisabled(String provider) {

    }

    public void onProviderEnabled(String provider) {

    }

    public void onStatusChanged(String provider, int status, Bundle extras) {

    }


    @Override
    public IBinder onBind(Intent arg0) {
        return mBinder;
    }

    public class LocalBinder extends Binder {
        public GPSService getService() {
            // Return this instance of LocalService so clients can call public methods
            return GPSService.this;
        }
    }

    public String getRetValue() {
        return (longitude + ", " + longitude);
    }

}
