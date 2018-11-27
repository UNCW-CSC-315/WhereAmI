//        Copyright 2018 Lucas Layman Licensed under the
//        Educational Community License, Version 2.0 (the "License"); you may
//        not use this file except in compliance with the License. You may
//        obtain a copy of the License at
//
//        http://www.osedu.org/licenses/ECL-2.0
//
//        Unless required by applicable law or agreed to in writing,
//        software distributed under the License is distributed on an "AS IS"
//        BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
//        or implied. See the License for the specific language governing
//        permissions and limitations under the License.

package edu.uncw.whereami;

import android.support.constraint.ConstraintLayout;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;

import io.objectbox.Box;
import io.objectbox.query.QueryBuilder;

public class LocationRecordAdapter extends RecyclerView.Adapter<LocationRecordAdapter.MyViewHolder> {

    private static final DateFormat formatter = new SimpleDateFormat("MM-dd-yy HH:mm:ss");

    private Box<LocationRecording> locationBox;

    LocationRecordAdapter(Box<LocationRecording> locationBox) {
        this.locationBox = locationBox;
    }


    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    class MyViewHolder extends RecyclerView.ViewHolder {
        public LinearLayout layout;
        TextView id;
        TextView timestamp;
        TextView latitude;
        TextView longitude;
        TextView accuracy;

        MyViewHolder(LinearLayout v) {
            super(v);
            layout = v;
            id = v.findViewById(R.id.item_id);
            timestamp = v.findViewById(R.id.timestamp);
            latitude = v.findViewById(R.id.item_lat);
            longitude = v.findViewById(R.id.item_lon);
            accuracy = v.findViewById(R.id.item_acc);
        }
    }

    // Create new views (invoked by the layout manager)
    @Override
    public LocationRecordAdapter.MyViewHolder onCreateViewHolder(ViewGroup parent,
                                                                 int viewType) {
        // create a new view
        LinearLayout v = (LinearLayout) LayoutInflater.from(parent.getContext())
                .inflate(R.layout.location_item, parent, false);

        return new MyViewHolder(v);
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(MyViewHolder holder, final int position) {
        // - get element from your dataset at this position
        // - replace the contents of the view with that element


        List<LocationRecording> locations = locationBox.query().order(LocationRecording_.timestamp, QueryBuilder.DESCENDING).build().find();
        if(position < locations.size() ) {
            LocationRecording loc = locations.get(position);
            holder.id.setText(Long.toString(loc.getId()));
            holder.timestamp.setText(formatter.format(loc.getTimestamp()));
            holder.latitude.setText(String.format("%.7f", loc.getLatitude()));
            holder.longitude.setText(String.format("%.7f", loc.getLongitude()));
            holder.accuracy.setText(String.format("%.2f",loc.getAcc()));
        }
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return (int) locationBox.count();
    }
}