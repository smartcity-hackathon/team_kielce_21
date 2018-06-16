package pl.lokalnie.droid;
/**
 * Created by AndroidBash on 20-Aug-16.
 */

import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.Movie;
import android.os.Bundle;
import android.os.Debug;
import android.os.Handler;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.github.arturogutierrez.Badges;
import com.github.arturogutierrez.BadgesNotSupportedException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.messaging.FirebaseMessaging;

import java.io.Console;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;



public class MainActivity extends AppCompatActivity implements RecyclerItemTouchHelper.RecyclerItemTouchHelperListener {

    static public SQLiteDatabase mydatabase;
    static RecyclerView listview;
    static Context context;
    static private Handler mHandler;
    static Cursor resultSet;
    static String LokalnieSP = "LokalnieSP";
    static List<PushListItem> mArrayList;
    static List<LocationsFSItem> LocationsFSItemyList;
    static String lokalnie;
    static CoordinatorLayout coordinatorLayout;
    static NotificationListCursorAdapter customAdapter;
    private static SharedPreferences SettingsSP;
    private static TextView titleTV;
    static String locationId;
    public static boolean isDebuggable;
    public static double gpsLatitude;
    public static double gpsLonglitude;
    public static String gpsLastUpdateTime;
    private int requestCode;
    private int grantResults[];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        isDebuggable = 0 != (getApplicationInfo().flags &= ApplicationInfo.FLAG_DEBUGGABLE);

        titleTV = (TextView) findViewById(R.id.titleTV);

        SettingsSP = getSharedPreferences("Lokalnie", MODE_PRIVATE);

        coordinatorLayout = (CoordinatorLayout) findViewById(R.id.coordinatorLayout);
        mHandler = new Handler();
        mydatabase = openOrCreateDatabase("lokalnieDb", MODE_PRIVATE, null);
        mydatabase.execSQL("CREATE TABLE IF NOT EXISTS Notifications(_id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, TitleString VARCHAR, Message VARCHAR, Image VARCHAR, Location INTEGER, Date VARCHAR, Readed INTEGER);");
        mydatabase.execSQL("CREATE TABLE IF NOT EXISTS Locations(_id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, Name VARCHAR, Id VARCHAR, Subscribe INTEGER);");

        if (getIntent().getExtras() != null) {

            for (String key : getIntent().getExtras().keySet()) {
                String value = getIntent().getExtras().getString(key);
            }
        }

        context = MainActivity.this;
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                .setTimestampsInSnapshotsEnabled(true)
                .build();
        firestore.setFirestoreSettings(settings);

        CollectionReference db = firestore.collection("Lokations");

        db.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.isSuccessful()) {
                    UpdateLocationsinDB(task);
                } else {
                    Log.w("FirebaseFirestore", "Error getting documents.", task.getException());
                }
            }
        });

        ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, requestCode);
        ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, requestCode);
        ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_COARSE_LOCATION}, requestCode);

        if (GPSService.isStarting && ((new Date().getTime() - GPSService.lastRun.getTime()) / 1000 > 15))
            GPSService.isStarting = false;

        GPSService.accuracy = GPSService.highestAccuracy;

        String provider = android.provider.Settings.Secure.getString(getContentResolver(), android.provider.Settings.Secure.LOCATION_PROVIDERS_ALLOWED);
        if (provider.contains("gps") || provider.contains("network")) { // if gps is enabled
            Log.i("Time", "" + (new Date().getTime() - GPSService.lastRun.getTime()) / 1000);
            if (!isServiceRunning(GPSService.class.getName()) && !GPSService.isStarting && ((new Date().getTime() - GPSService.lastRun.getTime()) / 1000 > 5)) {
                GPSService.isStarting = true;
                Intent service = new Intent(getApplicationContext(), GPSService.class);
                this.startService(service);
            } else if (GPSService.isInstanceCreated()) {
                GPSService.getInstanceCreated().getLocations();
            }
        }
    }


    public static void updateList() {
        locationId = restoreData();

        if (mydatabase != null && !locationId.equals("")) {
            resultSet = mydatabase.rawQuery("Select * from Notifications WHERE Notifications.Location ='" + locationId + "' ORDER BY _id DESC", null);

            Log.d("resultSet", DatabaseUtils.dumpCursorToString(resultSet));

            mArrayList = new ArrayList<>();
            for (resultSet.moveToFirst(); !resultSet.isAfterLast(); resultSet.moveToNext()) {
                // The Cursor is now set to the right position
                PushListItem pli = new PushListItem();
                pli.setTitle(resultSet.getString(1));
                pli.setImageURl(resultSet.getString(3));
                pli.setLocation(resultSet.getInt(4));
                pli.setDate(resultSet.getString(5));
                pli.setStatus(resultSet.getInt(6));
                mArrayList.add(pli);
            }

            customAdapter = new NotificationListCursorAdapter(mArrayList, context);
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    listview.setAdapter(customAdapter);
                }
            });
        }
    }

    public static interface ClickListenerInterface {
        public void onClick(View view, int position);

        public void onLongClick(View view, int position);
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateList();
        listview = (RecyclerView) findViewById(R.id.notificationsList);
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getApplicationContext());
        listview.setLayoutManager(mLayoutManager);
        listview.setItemAnimator(new DefaultItemAnimator());
        listview.addItemDecoration(new DividerItemDecoration(this, LinearLayoutManager.VERTICAL));

        listview.addOnItemTouchListener(new RecyclerTouchListener(this, listview, new ClickListenerInterface() {
            @Override
            public void onClick(View view, int position) {

                PushListItem pushItem = mArrayList.get(position);

                Intent myIntent = new Intent(MainActivity.this, NotificationDetails.class);
                resultSet.moveToPosition(position);
                int columnInt = resultSet.getColumnIndex("_id");
                String val = resultSet.getString(0);
                myIntent.putExtra("_id", val); //Optional parameters
                startActivity(myIntent);
            }

            @Override
            public void onLongClick(View view, int position) {

            }
        }));

        ItemTouchHelper.SimpleCallback itemTouchHelperCallback = new RecyclerItemTouchHelper(0, ItemTouchHelper.RIGHT, this);
        new ItemTouchHelper(itemTouchHelperCallback).attachToRecyclerView(listview);
        listview.setAdapter(customAdapter);

        final Cursor locationsSet = mydatabase.rawQuery("Select Name from Locations WHERE Id ='" + locationId + "'", null);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (locationsSet.moveToFirst())
                    titleTV.setText(locationsSet.getString(locationsSet.getColumnIndex("Name")));
            }
        });
        UpgradeBadges();


        Intent service = new Intent(getApplicationContext(), GPSService.class);
        this.startService(service);

        String provider = android.provider.Settings.Secure.getString(getContentResolver(), Settings.Secure.LOCATION_PROVIDERS_ALLOWED);
        if (provider.contains("gps") || provider.contains("network")) {
            // if gps is enabled
        } else {
            buildAlertMessageNoGps();
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();
        return super.onOptionsItemSelected(item);
    }

    public void gotoSettings() {
        Intent myIntent = new Intent(MainActivity.this, SettingsActivity.class);
        MainActivity.this.startActivity(myIntent);
    }

    public void gotoSettings(View v) {
        Intent myIntent = new Intent(MainActivity.this, SettingsActivity.class);
        MainActivity.this.startActivity(myIntent);
    }

    @Override
    public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction, int position) {
        if (viewHolder instanceof NotificationListCursorAdapter.ViewHolder) {
            // get the removed item name to display it in snack bar
            String name = mArrayList.get(viewHolder.getAdapterPosition()).getTitle();

            // backup of removed item for undo purpose
            final PushListItem deletedItem = mArrayList.get(viewHolder.getAdapterPosition());
            final int deletedIndex = viewHolder.getAdapterPosition();

            // remove the item from recycler view
            customAdapter.removeItem(viewHolder.getAdapterPosition());

            // showing snack bar with Undo option
            Snackbar snackbar = Snackbar.make(coordinatorLayout, name + " został usunięty !", Snackbar.LENGTH_LONG);
            snackbar.setAction("Cofnij", new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    // undo is selected, restore the deleted item
                    customAdapter.restoreItem(deletedItem, deletedIndex);
                }
            });
            snackbar.setActionTextColor(Color.YELLOW);
            snackbar.show();

            snackbar.addCallback(new Snackbar.Callback() {

                @Override
                public void onDismissed(Snackbar snackbar, int event) {
                    //see Snackbar.Callback docs for event details
                    int res = mydatabase.delete("Notifications", "_id=?", new String[]{String.valueOf(deletedItem._id)});

                    Log.d("Delete result", String.valueOf(res));
                }

                @Override
                public void onShown(Snackbar snackbar) {
                }
            });
        }
    }

    private void UpdateLocationsinDB(Task<QuerySnapshot> task) {
        mydatabase.delete("Locations", null, null);
//        for (QueryDocumentSnapshot document : task.getResult()) {
//            String name = document.getData().get("name").toString();
//            String id = document.getData().get("id").toString();
//            ContentValues contentValues = new ContentValues();
//            contentValues.put("name", name != null ? name : "");
//            contentValues.put("id", id != null ? id : "");
//            mydatabase.insertOrThrow("Locations", null, contentValues);
//        }


        ContentValues contentValues = new ContentValues();
        contentValues.put("name", "Hackathon");
        contentValues.put("id", "hackathon");
        mydatabase.insertOrThrow("Locations", null, contentValues);
        contentValues.put("name", "Sport");
        contentValues.put("id", "sport");
        mydatabase.insertOrThrow("Locations", null, contentValues);
        contentValues.put("name", "Kierowcy");
        contentValues.put("id", "kierowcy");
        mydatabase.insertOrThrow("Locations", null, contentValues);
        contentValues.put("name", "Biznes");
        contentValues.put("id", "biznes");
        mydatabase.insertOrThrow("Locations", null, contentValues);
        contentValues.put("name", "Wydarzenia");
        contentValues.put("id", "wydarzenia");
        mydatabase.insertOrThrow("Locations", null, contentValues);

        if (restoreData().equals(""))
            gotoSettings();

    }

    public static void saveLocationId(String locationId) {
        SharedPreferences.Editor preferencesEditor = SettingsSP.edit();
        preferencesEditor.putString("LocationId", locationId);
        preferencesEditor.commit();
    }

    public static String restoreData() {
        String locationId = SettingsSP.getString("LocationId", "");
        return locationId;
    }

    public static void UpgradeBadges() {

        if (!locationId.equals("")) {
            Cursor notReadNotifications = mydatabase.rawQuery("SELECT * FROM  Notifications where Location=?", new String[]{locationId});

            notReadNotifications.moveToFirst();
            int couter = 0;
            while (notReadNotifications.moveToNext()) {
                couter++;
            }

            try {
                Badges.setBadge(context, couter);
            } catch (BadgesNotSupportedException badgesNotSupportedException) {
                Log.d("Lokalnie", badgesNotSupportedException.getMessage());
            }
        }
    }

    public boolean isServiceRunning(String name) {
        final ActivityManager activityManager = (ActivityManager) this.getApplicationContext().getSystemService(Context.ACTIVITY_SERVICE);
        final List<ActivityManager.RunningServiceInfo> services = activityManager.getRunningServices(Integer.MAX_VALUE);
        for (ActivityManager.RunningServiceInfo runningServiceInfo : services) {
            if (runningServiceInfo.service.getClassName().equals(name)) {
                return true;
            }
        }
        return false;
    }

    AlertDialog alert = null;

    public void buildAlertMessageNoGps() {

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setTitle("GPS");
        alertDialogBuilder.setMessage("Czy chcesz udostępniac swoją pozycję ?");
        alertDialogBuilder.setCancelable(false);
        alertDialogBuilder.setIcon(android.R.drawable.ic_dialog_alert);
        alertDialogBuilder.setPositiveButton("TAK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                // continue with discard
                Intent intent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        startActivity(intent);            }
        });
        alertDialogBuilder.setNegativeButton("NIE", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                // do nothing
            }
        });
        alertDialogBuilder.show();

    }
}