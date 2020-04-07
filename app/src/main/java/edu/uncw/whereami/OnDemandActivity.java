package edu.uncw.whereami;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.Query;

import java.util.Date;
import java.util.Locale;

public class OnDemandActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    // Dictionary keys so we can store the last seen lat, lon, and acc in onSaveInstanceState()
    private static final String LATITUDE_KEY = "latitude";
    private static final String LONGITUDE_KEY = "longitude";
    private static final String ACCURACY_KEY = "accuracy";
    // We will need this constant during the process of requesting permission.
    // It can be any integer value. It is just something to support passing of data between
    // this app and the Android built-in app that requests permissions
    private static final int MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 65535;
    // A class from Google Services that simplifies the logic of getting location information from
    // the sensors on the phone.
    private FusedLocationProviderClient mFusedLocationClient;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private static final String LOCATIONRECORD_COLLECTION = "location-recordings";

    TextView latText;
    TextView lonText;
    TextView accuracyText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ondemand);

        latText = findViewById(R.id.latitude);
        lonText = findViewById(R.id.longitude);
        accuracyText = findViewById(R.id.acc);

        // Initialize the object that we will use for retrieving location readings.
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Setup the RecyclerView to display new and changed LocationRecords in the Firestore
        final RecyclerView recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        Query query = db.collection(LOCATIONRECORD_COLLECTION)
                .orderBy("timestamp", Query.Direction.DESCENDING);
        FirestoreRecyclerOptions<LocationRecord> options = new FirestoreRecyclerOptions.Builder<LocationRecord>()
                .setQuery(query, LocationRecord.class)
                .setLifecycleOwner(this)
                .build();
        // The OnDataChangedListener makes the RecyclerView scroll to the topmost (newest) item
        // automatically when one is added.
        LocationRecordAdapter adapter = new LocationRecordAdapter(options, new LocationRecordAdapter.OnDataChangedListener() {
            @Override
            public void onDataChanged() {
                recyclerView.scrollToPosition(0);
            }
        });
        recyclerView.setAdapter(adapter);
        recyclerView.addItemDecoration(new DividerItemDecoration(this,
                DividerItemDecoration.VERTICAL));


        if (savedInstanceState != null) {
            latText.setText(savedInstanceState.getCharSequence(LATITUDE_KEY));
            lonText.setText(savedInstanceState.getCharSequence(LONGITUDE_KEY));
            accuracyText.setText(savedInstanceState.getCharSequence(ACCURACY_KEY));
        }

    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putCharSequence(LATITUDE_KEY, latText.getText());
        outState.putCharSequence(LONGITUDE_KEY, lonText.getText());
        outState.putCharSequence(ACCURACY_KEY, accuracyText.getText());
        super.onSaveInstanceState(outState);
    }

    /**
     * This method is called when the user clicks the Record button. It goes through the process of
     * making sure the device can get its location, checking user permission, and finally grabbing
     * a location reading.
     *
     * @param view the Record button that was pressed
     */
    public void recordClick(View view) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission has not been granted for ACCESS_FINE_LOCATION yet.
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {
                // Explain to the user why you need this permission.
                Toast.makeText(this, "I need permission to access location in order to record locations.", Toast.LENGTH_SHORT).show();
            } else {
                // Launch a built-in dialog box that asks the user for permission to access the location.
                // The dialog box is part of the Android OS and is separate from your app.
                // The constant MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION facilitates communication
                // between your app and the Android OS dialog box.
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
            }
        } else {
            // Get the last known location from the location object.
            // Whether it gets a GPS reading, a WiFi location, or a cell tower location is all
            // hidden in the logic of Google's FusedLocationClient. However, it will try to give
            // a precise location.
            mFusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                        @Override
                        public void onSuccess(Location location) {
                            if (location != null) {
                                // We got a Location reading. Update the UI.
                                latText.setText(String.format(Locale.US, "%.7f", location.getLatitude()));
                                lonText.setText(String.format(Locale.US, "%.7f", location.getLongitude()));
                                accuracyText.setText(String.format(Locale.US, "%.2f", location.getAccuracy()));

                                // Now write the Location data to the Firestore using our LocationRecord object
                                // The RecyclerView should update automatically
                                LocationRecord lr = new LocationRecord(
                                        new Date(location.getTime()),
                                        new GeoPoint(location.getLatitude(), location.getLongitude()),
                                        location.getAccuracy());

                                db.collection(LOCATIONRECORD_COLLECTION)
                                        .add(lr)
                                        .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                                            @Override
                                            public void onSuccess(DocumentReference documentReference) {
                                                Log.d(TAG, "Location Record added with ID: " + documentReference.getId());
                                            }
                                        })
                                        .addOnFailureListener(new OnFailureListener() {
                                            @Override
                                            public void onFailure(@NonNull Exception e) {
                                                Log.w(TAG, "Error adding LocationRecord", e);
                                            }
                                        });
                            }
                        }
                    });
        }
    }

    /**
     * This method is called automatically by the Android OS when ActivityCompat.requestPermissions() finishes the dialog.
     * In here, we check if the user granted the permission or not, then do something based on that result.
     *
     * @param requestCode  a unique integer id affiliated with the permission request. This is used to help distinguish if an activity
     *                     has multiple permission requests
     * @param permissions  an array of the Android permissions that were requested
     * @param grantResults Integer value indicating if the permission was granted or not
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION) {
            // If request is cancelled, the result arrays are empty.
            if (grantResults.length > 0  && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "I can record the location now!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "This app won't work until you grant permission to access the location!", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
