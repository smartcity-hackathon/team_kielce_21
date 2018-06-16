package pl.lokalnie.droid;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.SQLException;
import android.graphics.drawable.ColorDrawable;
import android.location.Address;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Handler;
import android.os.ResultReceiver;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckedTextView;
import android.widget.CursorAdapter;
import android.widget.EditText;
import android.widget.FilterQueryProvider;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import static pl.lokalnie.droid.MainActivity.customAdapter;

public class SettingsActivity extends AppCompatActivity {

    ListView locationsLV;
    Button continueBT;
    static Context context;
    ListViewAdapter adapter;
    EditText inputSearch;
    private ArrayList<String> arraylist;
    private String SelectedLocation = "";
    Cursor locationsResultCursor;
    TextView textTV;
    LinearLayout mainLL;
    ImageButton getLocation;
    int fetchType = Constants.USE_ADDRESS_LOCATION;
    EditText locationET;
    AddressResultReceiver mResultReceiver;
    SharedPreferences SettingsSP;

    EditText nameET;
    EditText surnameET;
    EditText addressET;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        context = SettingsActivity.this;

        SettingsSP = getSharedPreferences("Lokalnie", MODE_PRIVATE);

        mResultReceiver = new AddressResultReceiver(null);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        textTV = (TextView) findViewById(R.id.textTV);
        mainLL = (LinearLayout) findViewById(R.id.mainLL);
        mainLL.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                textTV.clearFocus();
            }
        });

        locationET = (EditText) findViewById(R.id.locationET);

        continueBT = (Button) findViewById(R.id.continueButton);
        continueBT.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                subscribeToPushService();
                onBackPressed();
            }
        });

        locationsResultCursor = MainActivity.mydatabase.rawQuery("Select * from Locations ORDER BY Name ", null);
        adapter = new ListViewAdapter(this, locationsResultCursor);

        locationsLV = (ListView) findViewById(R.id.locationsLV);
        locationsLV.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        //locationsLV.setSelector(R.drawable.settingslistviewselector);
        locationsLV.setAdapter(adapter);

        locationsLV.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                mainLL.requestFocus();
                Cursor cursor = (Cursor) locationsLV.getAdapter().getItem(position);
                SelectedLocation = cursor.getString(cursor.getColumnIndexOrThrow("Id"));
                hideKeyboard();
            }
        });

        adapter.setFilterQueryProvider(new FilterQueryProvider() {
            public Cursor runQuery(CharSequence constraint) {
                return fetchCountriesByName(constraint.toString());
            }
        });

        inputSearch = (EditText) findViewById(R.id.searchET);
        inputSearch.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean b) {
                if (inputSearch.isFocused()) {
                    textTV.setVisibility(View.GONE);
                } else {
                    hideKeyboard();
                    textTV.setVisibility(View.VISIBLE);
                }
            }
        });
        inputSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence cs, int arg1, int arg2, int arg3) {
                adapter.getFilter().filter(cs.toString());
            }

            @Override
            public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
            }

            @Override
            public void afterTextChanged(Editable arg0) {
            }
        });

        String selectedLocation = MainActivity.restoreData();

        for (int i = 0; i < adapter.getCount(); i++) {
            Cursor cursor = (Cursor) adapter.getItem(i);
            if (selectedLocation.equals(cursor.getString(cursor.getColumnIndexOrThrow("Id")))) {
                locationsLV.setItemChecked(i, true);
            }
        }

        nameET = (EditText) findViewById(R.id.nameET);
        surnameET = (EditText) findViewById(R.id.surnameET);
        addressET = (EditText) findViewById(R.id.locationET);
        nameET.setText(SettingsSP.getString("nameET", ""));
        surnameET.setText(SettingsSP.getString("surnameET", ""));
        addressET.setText(SettingsSP.getString("addressET", ""));

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    //Name VARCHAR, Id VARCHAR, Subscribe INTEGER

    public Cursor fetchCountriesByName(String inputText) throws SQLException {
        Cursor mCursor = null;
        if (inputText == null || inputText.length() == 0) {
            mCursor = MainActivity.mydatabase.query("Locations", new String[]{"_id", "Name",
                            "Id", "Subscribe"},
                    null, null, null, null, null);

        } else {
            mCursor = MainActivity.mydatabase.query(true, "Locations", new String[]{"_id", "Name", "Id", "Subscribe"},
                    "Name" + " like '" + inputText + "%'", null,
                    null, null, null, null);
        }

        //Log.d("mCursor", DatabaseUtils.dumpCursorToString(mCursor));
        return mCursor;

    }

    public String removeLastCharFromString(String str) {
        if (str != null && str.length() > 0 && str.charAt(str.length() - 1) == ',') {
            str = str.substring(0, str.length() - 1);
        }
        return str;
    }

    private void subscribeToPushService() {
        if (!SelectedLocation.equals("")) {
            locationsResultCursor = MainActivity.mydatabase.rawQuery("Select * from Locations", null);
            locationsResultCursor.moveToFirst();
            while (locationsResultCursor.moveToNext()) {
                String Id = locationsResultCursor.getString(locationsResultCursor.getColumnIndex("Id"));
                FirebaseMessaging.getInstance().unsubscribeFromTopic(Id);
            }

            FirebaseMessaging.getInstance().subscribeToTopic(SelectedLocation);
            String token = FirebaseInstanceId.getInstance().getToken();
            MainActivity.saveLocationId(SelectedLocation);
            //Log.d("Token", token);
        }


        SharedPreferences.Editor preferencesEditor = SettingsSP.edit();
        preferencesEditor.putString("nameET", nameET.getText().toString());
        preferencesEditor.putString("surnameET", surnameET.getText().toString());
        preferencesEditor.putString("addressET", addressET.getText().toString());
        preferencesEditor.commit();
    }

    private void hideKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    public void onButtonClicked(View view) {

        double lat = MainActivity.gpsLatitude;
        double lng = MainActivity.gpsLonglitude;

        Intent intent = new Intent(this, FetchAddressIntentService.class);
        intent.putExtra(Constants.RECEIVER, mResultReceiver);
        intent.putExtra(Constants.FETCH_TYPE_EXTRA, fetchType);

        intent.putExtra(Constants.LOCATION_LATITUDE_DATA_EXTRA,
                Double.parseDouble(String.valueOf(lat)));
        intent.putExtra(Constants.LOCATION_LONGITUDE_DATA_EXTRA,
                Double.parseDouble(String.valueOf(lng)));

        startService(intent);

    }


    class AddressResultReceiver extends ResultReceiver {
        public AddressResultReceiver(Handler handler) {
            super(handler);
        }

        @Override
        protected void onReceiveResult(int resultCode, final Bundle resultData) {
            if (resultCode == Constants.SUCCESS_RESULT) {
                final Address address = resultData.getParcelable(Constants.RESULT_ADDRESS);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        locationET.setVisibility(View.VISIBLE);
                        locationET.setText(address.getAddressLine(0));
                    }
                });
            }
            else {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        locationET.setVisibility(View.VISIBLE);
                        locationET.setText(resultData.getString(Constants.RESULT_DATA_KEY));
                    }
                });
            }
        }
    }
}
