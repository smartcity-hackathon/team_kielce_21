package pl.lokalnie.droid;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckedTextView;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class ListViewAdapter extends CursorAdapter {

    // Declare Variables
    Context mContext;
    LayoutInflater inflater;
    Cursor cursor;
    private List<String> worldpopulationlist = null;
    private ArrayList<String> arraylist;

    public ListViewAdapter(Context context, Cursor cursor) {
        super(context, cursor, 0);
        mContext = context;
        this.cursor = cursor;
        //Log.d("DatabaseUtils", DatabaseUtils.dumpCursorToString(this.cursor));
        inflater = LayoutInflater.from(mContext);
        this.arraylist = new ArrayList<String>();
    }


    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup viewGroup) {
        return LayoutInflater.from(context).inflate(R.layout.simple_list_item_multiple, viewGroup, false);
    }

    int LocationId;
    String LocationName;

    public int getLocationId() {
        return LocationId;
    }

    public String getLocationName() {
        return LocationName;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {

        CheckedTextView tvName = view.findViewById(R.id.ctv);
        String name = cursor.getString(cursor.getColumnIndexOrThrow("Name"));
        tvName.setText(name);
        LocationName = name;
        LocationId = cursor.getInt(cursor.getColumnIndexOrThrow("Id"));

    }

    // Filter Class
    public void filterCheckonly() {
        notifyDataSetChanged();
    }

    // Filter Class
    public void filter(String charText) {


        charText = charText.toLowerCase(Locale.getDefault());
        worldpopulationlist = new ArrayList<String>();

        if (charText.length() == 0) {
            worldpopulationlist.addAll(arraylist);
        }
        else
        {
            for (String wp : arraylist)
            {
                if (wp.toLowerCase(Locale.getDefault()).contains(charText))
                {
                    worldpopulationlist.add(wp);
                }
            }
        }
        notifyDataSetChanged();
    }
}