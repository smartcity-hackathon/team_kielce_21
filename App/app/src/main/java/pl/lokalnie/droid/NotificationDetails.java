package pl.lokalnie.droid;

import android.content.ContentValues;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Environment;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ShareActionProvider;
import android.widget.TextView;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;


public class NotificationDetails extends AppCompatActivity {

    private String fullScreenInd;
    private ShareActionProvider mShareActionProvider;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification_details);

        Intent intent = getIntent();
        String value = "";

        Bundle extras = getIntent().getExtras();
        if(extras !=null)
        {
            value = extras.getString("_id");
            fullScreenInd = getIntent().getStringExtra("fullScreenIndicator");
        }

        Cursor resultSet = MainActivity.mydatabase.rawQuery("Select * from Notifications WHERE _id='" +value +"'",null);
        resultSet.moveToFirst();

        Log.d("mCursor", DatabaseUtils.dumpCursorToString(resultSet));

        String locationId = resultSet.getString(resultSet.getColumnIndex("Location"));

        Cursor resultLocations = MainActivity.mydatabase.rawQuery("Select * from Locations WHERE Id='" +locationId +"'",null);
        resultLocations.moveToFirst();

        Log.d("mCursor", DatabaseUtils.dumpCursorToString(resultLocations));

        int cc = resultSet.getCount();
        final TextView titleView = (TextView) findViewById(R.id.titleTV);
        titleView.setText(resultSet.getString(resultSet.getColumnIndex("TitleString")));

        final TextView messageView = (TextView) findViewById(R.id.messageTV);
        messageView.setText(resultSet.getString(resultSet.getColumnIndex("Message")));

        final ImageView imageView = (ImageView) findViewById(R.id.image);
        final String url = resultSet.getString(resultSet.getColumnIndex("Image"));
        if(url != null && !url.equals("")) {

            Picasso.get().load(resultSet.getString(resultSet.getColumnIndex("Image"))).into(picassoImageTarget(getApplicationContext(), "Lokalnie", "shareImage.jpg"));

            Picasso.get().load(resultSet.getString(resultSet.getColumnIndex("Image"))).into(imageView);

            final String finalValue = value;
            imageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(NotificationDetails.this,
                            FullscreenActivity.class);

                    intent.putExtra("imageURL", url);
                    NotificationDetails.this.startActivity(intent);
                }
            });
        }else{
            imageView.setVisibility(View.GONE);
        }

        FloatingActionButton myFab = (FloatingActionButton) findViewById(R.id.fab);
        myFab.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                onBackPressed();
            }
        });

        FloatingActionButton myFabShare = (FloatingActionButton) findViewById(R.id.fabShare);
        myFabShare.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
                if(url != null && !url.equals("")) {
                    sharingIntent.setType("image/jpg");
                    sharingIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    ContextWrapper cw = new ContextWrapper(getApplicationContext());
                    final File mediaStorage = new File(Environment.getExternalStoragePublicDirectory(Environment.getExternalStorageState()), "Lokalnie");
                    //File directory = cw.getDir("imageToShare", Context.MODE_PRIVATE);
                    File myImageFile = new File(mediaStorage, "shareImage.jpg");
                    sharingIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(myImageFile));
                }else {
                    sharingIntent.setType("text/plain");
                }
                sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, titleView.getText());
                sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, messageView.getText());
                sharingIntent.putExtra(Intent.EXTRA_TITLE, messageView.getText());

                startActivity(Intent.createChooser(sharingIntent, "Shearing Option"));
            }
        });

        TextView dateTV = (TextView) findViewById(R.id.dataTV);
        TextView locationTV = (TextView) findViewById(R.id.locationTV);

        int locationInt = resultSet.getInt(resultSet.getColumnIndexOrThrow("Location"));
        String[] locations = getResources().getStringArray(R.array.location_array);

        String location = resultLocations.getString(resultLocations.getColumnIndex("Name"));
        locationTV.setText(location);

        String date = resultSet.getString((resultSet.getColumnIndexOrThrow(("Date"))));
        dateTV.setText(date);

        ContentValues cv = new ContentValues();
        cv.put("Readed",0); //These Fields should be your String values of actual column names

        MainActivity.mydatabase.update("Notifications", cv, "_id=" + value, null);
        MainActivity.UpgradeBadges();

    }

    private Target picassoImageTarget(Context context, final String imageDir, final String imageName) {
        Log.d("picassoImageTarget", " picassoImageTarget");
        ContextWrapper cw = new ContextWrapper(context);
        final File mediaStorage = new File(Environment.getExternalStoragePublicDirectory(Environment.getExternalStorageState()), imageDir);

        if (!mediaStorage.exists()) {
            if (!mediaStorage.mkdirs()) {
                Log.d("App", "failed to create directory");
            }
        }
        return new Target() {
            @Override
            public void onBitmapLoaded(final Bitmap bitmap, Picasso.LoadedFrom from) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        final File myImageFile = new File(mediaStorage, imageName); // Create image file
                        FileOutputStream fos = null;
                        try {
                            fos = new FileOutputStream(myImageFile);
                            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
                        } catch (IOException e) {
                            e.printStackTrace();
                        } finally {
                            try {
                                fos.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                        Log.i("image", "image saved to >>>" + myImageFile.getAbsolutePath());

                    }
                }).start();
            }

            @Override
            public void onBitmapFailed(Exception e, Drawable errorDrawable) {

            }

            @Override
            public void onPrepareLoad(Drawable placeHolderDrawable) {
                if (placeHolderDrawable != null) {}
            }
        };
    }
}
