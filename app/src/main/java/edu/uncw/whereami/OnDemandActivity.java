package edu.uncw.whereami;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.Query;

import java.util.Date;
import java.util.Locale;

public class OnDemandActivity extends AppCompatActivity {

    private static final String TAG = "OnDemandActivity";

    // Dictionary keys so we can store the last seen lat, lon, and acc in onSaveInstanceState()
    private static final String LATITUDE_KEY = "latitude";
    private static final String LONGITUDE_KEY = "longitude";
    private static final String ACCURACY_KEY = "accuracy";

    // A class from Google Services that simplifies the logic of getting location information from
    // the sensors on the phone.
    private FusedLocationProviderClient mFusedLocationClient;

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private static final String LOCATIONRECORD_COLLECTION = "location-recordings";

    TextView latText;
    TextView lonText;
    TextView accuracyText;


    ActivityResultLauncher<String[]> locationPermissionRequest =
            registerForActivityResult(new ActivityResultContracts
                            .RequestMultiplePermissions(), result -> {
                        Boolean fineLocationGranted = result.getOrDefault(
                                Manifest.permission.ACCESS_FINE_LOCATION, false);
                        Boolean coarseLocationGranted = result.getOrDefault(
                                Manifest.permission.ACCESS_COARSE_LOCATION, false);

                        if (fineLocationGranted != null && fineLocationGranted) {
                            // Precise location access granted.
                            Toast.makeText(this, "I can see precise location!", Toast.LENGTH_SHORT).show();
                        } else if (coarseLocationGranted != null && coarseLocationGranted) {
                            // Only approximate location access granted.
                            Toast.makeText(this, "I can see approximate location!", Toast.LENGTH_SHORT).show();
                        } else {
                            // No location access granted.
                            Toast.makeText(this, "I need permission to access location in order to record locations.", Toast.LENGTH_SHORT).show();
                        }
                    }
            );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ondemand);

        latText = findViewById(R.id.latitude);
        lonText = findViewById(R.id.longitude);
        accuracyText = findViewById(R.id.acc);
        if (savedInstanceState != null) {
            latText.setText(savedInstanceState.getCharSequence(LATITUDE_KEY));
            lonText.setText(savedInstanceState.getCharSequence(LONGITUDE_KEY));
            accuracyText.setText(savedInstanceState.getCharSequence(ACCURACY_KEY));
        }


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
        LocationRecordAdapter adapter = new LocationRecordAdapter(options, () -> recyclerView.scrollToPosition(0));
        recyclerView.setAdapter(adapter);
        recyclerView.addItemDecoration(new DividerItemDecoration(this,
                DividerItemDecoration.VERTICAL));


        // Initialize the object that we will use for retrieving location readings.
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        Button recordLocationBtn = findViewById(R.id.record_button);
        recordLocationBtn.setOnClickListener(this::recordClick);

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
    private void recordClick(View view) {

        boolean hasFineLocationPermission = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;

        boolean hasCoarseLocationPermission = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;

        if (!hasCoarseLocationPermission && !hasFineLocationPermission) {
            Toast.makeText(this, "I need permission to access location in order to record locations.", Toast.LENGTH_SHORT).show();
            locationPermissionRequest.launch(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION});
        } else {
            // Get the last known location from the location object.
            // Whether it gets a GPS reading, a WiFi location, or a cell tower location is all
            // hidden in the logic of Google's FusedLocationClient. However, it will try to give
            // a precise location.
            mFusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, location -> {
                        if (location != null) {
                            // We got a Location reading. Update the UI.
                            latText.setText(String.format(Locale.US, "%.7f", location.getLatitude()));
                            lonText.setText(String.format(Locale.US, "%.7f", location.getLongitude()));
                            accuracyText.setText(String.format(Locale.US, "%.2f", location.getAccuracy()));

                            addLocationRecord(location);
                        }
                    });
        }
    }

    private void addLocationRecord(Location location) {
        // Now write the Location data to the Firestore using our LocationRecord object
        // The RecyclerView should update automatically
        LocationRecord lr = new LocationRecord(
                new Date(location.getTime()),
                new GeoPoint(location.getLatitude(), location.getLongitude()),
                location.getAccuracy());

        db.collection(LOCATIONRECORD_COLLECTION)
                .add(lr)
                .addOnSuccessListener(documentReference -> Log.d(TAG, "Location Record added with ID: " + documentReference.getId()))
                .addOnFailureListener(e -> Log.w(TAG, "Error adding LocationRecord", e));
    }
}
