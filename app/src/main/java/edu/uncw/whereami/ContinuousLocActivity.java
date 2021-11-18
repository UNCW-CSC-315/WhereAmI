package edu.uncw.whereami;

import android.Manifest;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.Query;

import java.util.Date;
import java.util.Locale;

public class ContinuousLocActivity extends AppCompatActivity {

    private static final String TAG = "ContinuousLocActivity";
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


    // This field will hold an object that gets triggered by the periodic location updates.
    // The object will update the UI and record data to the Firestore.
    private LocationCallback mLocationCallback;

    private static final int REQUEST_CHECK_SETTINGS = 123;

    // This is a configuration object that tells the Google client how often and to what degree
    // of accuracy you want to receive location updates.
    private LocationRequest mLocationRequest = LocationRequest.create()
            .setInterval(10000)
            .setFastestInterval(5000)
            .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_continuous);

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
        // The OnDataChangedListener makes the RecyclerCiew scroll to the topmost (newest) item
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

        // Setup the LocationCallback object, which will be triggered when the FusedLocationProvider
        // periodically updates the location. In our case, we want to update the UI and write
        // a LocationRecord to the Firestore. This is similar to the OnDemandActivity logic.
        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult != null) {
                    for (Location location : locationResult.getLocations()) {
                        if (location != null) {
                            latText.setText(String.format(Locale.US, "%.7f", location.getLatitude()));
                            lonText.setText(String.format(Locale.US, "%.7f", location.getLongitude()));
                            accuracyText.setText(String.format(Locale.US, "%.2f", location.getAccuracy()));

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
                }
            }
        };


        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);


    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putCharSequence(LATITUDE_KEY, latText.getText());
        outState.putCharSequence(LONGITUDE_KEY, lonText.getText());
        outState.putCharSequence(ACCURACY_KEY, accuracyText.getText());
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onResume() {
        super.onResume();
        createLocationRequest();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mFusedLocationClient.removeLocationUpdates(mLocationCallback);
    }

    /**
     * This method tries to start the continuous location updates, which is a complex process.
     * 1) Check to see if we can get location using the SettingsClient. Cases where you may not be able to get location
     * include: Airplane mode is on, Location Services are turned off, or the device does not have GPS.
     * 2) Next, check to see if the user has granted permission to access the location. If not, request permission. If so, start the location updates.
     */
    protected void createLocationRequest() {
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(mLocationRequest);
        SettingsClient client = LocationServices.getSettingsClient(this);
        Task<LocationSettingsResponse> task = client.checkLocationSettings(builder.build());
        task.addOnSuccessListener(this, new OnSuccessListener<LocationSettingsResponse>() {
            @Override
            public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                // All location settings are satisfied. The client can initialize
                // location requests here.
                // ...

                boolean hasFineLocationPermission = ContextCompat.checkSelfPermission(ContinuousLocActivity.this,
                        Manifest.permission.ACCESS_FINE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED;

                boolean hasCoarseLocationPermission = ContextCompat.checkSelfPermission(ContinuousLocActivity.this,
                        Manifest.permission.ACCESS_COARSE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED;
                if (!hasCoarseLocationPermission && !hasFineLocationPermission) {
                    Toast.makeText(ContinuousLocActivity.this, "I need permission to access location in order to record locations.", Toast.LENGTH_SHORT).show();
                    locationPermissionRequest.launch(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION});
                } else {
                    mFusedLocationClient.requestLocationUpdates(mLocationRequest,
                            mLocationCallback,
                            null /* Looper */);
                }
            }
        }).addOnFailureListener(this, new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                if (e instanceof ResolvableApiException) {
                    // Location settings are not satisfied, but this can be fixed
                    // by showing the user a dialog.
                    try {
                        // Show the dialog by calling startResolutionForResult(),
                        // and check the result in onActivityResult().
                        ResolvableApiException resolvable = (ResolvableApiException) e;
                        resolvable.startResolutionForResult(ContinuousLocActivity.this,
                                REQUEST_CHECK_SETTINGS);
                    } catch (IntentSender.SendIntentException sendEx) {
                        // Ignore the error.
                    }
                }
            }
        });
    }

    /**
     * This method is similar in nature to onRequestPermissionsResult(), except that this method
     * deal with the FusedLocationProvider not being able to handle the location polling
     * parameters you gave it.
     * <p>
     * An example would be that the phone is in Airplane made. Android will prompt the user to
     * disable it, and they may or may not do so.
     *
     * @param requestCode
     * @param resultCode
     * @param data
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CHECK_SETTINGS) {
            // Regardless of whether the user fixed the issue or not, try again.
            createLocationRequest();

            // This could be really annoying for the user...
            // In reality, you should check the resultCode for success. If not successful, you
            // should degrade the functionality.
        }
    }
}
