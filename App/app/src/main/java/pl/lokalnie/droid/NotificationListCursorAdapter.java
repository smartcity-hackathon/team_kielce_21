package pl.lokalnie.droid;

import android.app.Application;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Typeface;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.util.List;


/**
 * Created by inmos on 26.02.2018.
 */

public class NotificationListCursorAdapter extends RecyclerView.Adapter<NotificationListCursorAdapter.ViewHolder> {


    /*
    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        TextView tvBody = (TextView) view.findViewById(R.id.titleTV);
        String body = cursor.getString(cursor.getColumnIndexOrThrow("TitleString"));
        tvBody.setText(body);

        ImageView image = (ImageView) view.findViewById(R.id.image);
        String imageUrl = cursor.getString(cursor.getColumnIndexOrThrow("Image"));
        if(imageUrl != null && !imageUrl.equals("")) {
            Picasso.get().load(imageUrl).into(image);
        }else{
            LinearLayout imageLL = (LinearLayout) view.findViewById(R.id.imageLL);
            imageLL.setVisibility(View.GONE);
        }

        TextView dateTV = (TextView) view.findViewById(R.id.dataTV);
        TextView locationTV = (TextView) view.findViewById(R.id.locationTV);

        int locationInt = cursor.getInt(cursor.getColumnIndexOrThrow("Location"));
        String[] locations = mContext.getResources().getStringArray(R.array.location_array);
        locationTV.setText(locations[locationInt]);

        String date = cursor.getString((cursor.getColumnIndexOrThrow(("Date"))));
        dateTV.setText(date);
     }

    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return LayoutInflater.from(context).inflate(R.layout.list_row, parent, false);
    }

    */

    Context mContext;
    public NotificationListCursorAdapter(List<PushListItem> pushItemList, Context mContext) {
        this.mContext = mContext;
        this.pushItemList = pushItemList;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.list_row, parent, false);

        return new ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        PushListItem pushItem = pushItemList.get(position);

        if(pushItem.getStatus()==0){
            holder.titleTV.setTypeface(null, Typeface.NORMAL);

        }
        holder.titleTV.setText(pushItem.getTitle());
        holder.dateTV.setText(pushItem.getDate());
        String[] locations = mContext.getResources().getStringArray(R.array.location_array);

        holder.locationTV.setText(pushItem.getLocationName());

        if(pushItem.getImageURl() != null && !pushItem.getImageURl().equals("")) {
            String imgaeThumg = pushItem.getImageURl().replace("uploads", "thumbs");
            Picasso.get().load(imgaeThumg).into(holder.image);
        }else{
            holder.imageLL.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return pushItemList.size();
    }

    private List<PushListItem> pushItemList;

    public class ViewHolder extends RecyclerView.ViewHolder {
        public TextView titleTV, dateTV, locationTV;

        public ImageView image;
        public LinearLayout imageLL;

        public RelativeLayout viewBackground, viewForeground;


        public ViewHolder(View view) {
            super(view);
            titleTV = (TextView) view.findViewById(R.id.titleTV);
            dateTV = (TextView) view.findViewById(R.id.dataTV);
            locationTV = (TextView) view.findViewById(R.id.locationTV);
            image = (ImageView) view.findViewById(R.id.image);
            imageLL = (LinearLayout) view.findViewById(R.id.imageLL);

            viewBackground = view.findViewById(R.id.view_background);
            viewForeground = view.findViewById(R.id.view_foreground);

        }
    }

    public void removeItem(int position) {
        pushItemList.remove(position);
        // notify the item removed by position
        // to perform recycler view delete animations
        // NOTE: don't call notifyDataSetChanged()
        notifyItemRemoved(position);
    }

    public void restoreItem(PushListItem item, int position) {
        pushItemList.add(position, item);
        // notify item added by position
        notifyItemInserted(position);
    }
}