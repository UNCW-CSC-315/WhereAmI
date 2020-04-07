package edu.uncw.whereami;

import android.Manifest;
import android.content.Intent;
import android.content.IntentSender;
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

public class MainActivity extends AppCompatActivity {

    private FusedLocationProviderClient mFusedLocationClient;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private LocationRecordAdapter mAdapter;

    private static final int MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 65535;
    private static final String LOCATIONRECORD_COLLECTION = "location-recordings";
    private static final String TAG = "MainActivity";

    TextView latText;
    TextView lonText;
    TextView accuracyText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        latText = findViewById(R.id.latitude);
        lonText = findViewById(R.id.longitude);
        accuracyText = findViewById(R.id.acc);

        // Do the many steps required to setup the RecyclerView to listen for new and
        // changed LocationRecords in the Firestore
        final RecyclerView recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        Query query = db.collection(LOCATIONRECORD_COLLECTION)
                .orderBy("timestamp", Query.Direction.DESCENDING);
        FirestoreRecyclerOptions<LocationRecord> options = new FirestoreRecyclerOptions.Builder<LocationRecord>()
                .setQuery(query, LocationRecord.class)
                .setLifecycleOwner(this)
                .build();
        mAdapter = new LocationRecordAdapter(options
                , new LocationRecordAdapter.OnDataChangedListener() {
            @Override
            public void onDataChanged() {
                recyclerView.scrollToPosition(0);
            }
        });
        recyclerView.setAdapter(mAdapter);
        recyclerView.addItemDecoration(new DividerItemDecoration(this,
                DividerItemDecoration.VERTICAL));


        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult != null) {
                    for (Location location : locationResult.getLocations()) {
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
        };

        if (savedInstanceState != null) {
            latText.setText(savedInstanceState.getCharSequence(LATITUDE_KEY));
            lonText.setText(savedInstanceState.getCharSequence(LONGITUDE_KEY));
            accuracyText.setText(savedInstanceState.getCharSequence(ACCURACY_KEY));
        }

    }

    private void recordFirestoreEntryFromLocationResult(Location location) {
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

    public void recordClick(View view) {

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "I need permission to access location in order to record locations.", Toast.LENGTH_SHORT).show();

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        } else {
            mFusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                        @Override
                        public void onSuccess(Location location) {
                            recordFirestoreEntryFromLocationResult(location);
                        }
                    });
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "I can record the location now!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "This app won't work until you grant permission!", Toast.LENGTH_SHORT).show();
                }
                return;
            }
        }
    }

    private static int REQUEST_CHECK_SETTINGS = 123;
    private LocationRequest mLocationRequest;
    private LocationCallback mLocationCallback;
    private static String LATITUDE_KEY = "latitude";
    private static String LONGITUDE_KEY = "longitude";
    private static String ACCURACY_KEY = "accuracy";

    /**
     * This method is called to kick off the chain of events that requests continuous location updates.
     */
    protected void createLocationRequest() {
        if (mLocationRequest == null) {
            mLocationRequest = new LocationRequest();
            mLocationRequest.setInterval(10000);
            mLocationRequest.setFastestInterval(5000);
            mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        }

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
                startLocationUpdates();
            }
        });


        task.addOnFailureListener(this, new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                if (e instanceof ResolvableApiException) {
                    // Location settings are not satisfied, but this can be fixed
                    // by showing the user a dialog.
                    try {
                        // Show the dialog by calling startResolutionForResult(),
                        // and check the result in onActivityResult().
                        ResolvableApiException resolvable = (ResolvableApiException) e;
                        resolvable.startResolutionForResult(MainActivity.this,
                                REQUEST_CHECK_SETTINGS);
                    } catch (IntentSender.SendIntentException sendEx) {
                        // Ignore the error.
                    }
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CHECK_SETTINGS) {
            // Regardless of whether the user fixed the issue or not, try again.
            // This could be really annoying for the user...
            // In reality, you should check the resultCode for success. If not successful, you
            // should degrade the functionality.
            createLocationRequest();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        createLocationRequest();
    }

    private void startLocationUpdates() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "I need permission to access location in order to record locations.", Toast.LENGTH_SHORT).show();

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        } else {
            mFusedLocationClient.requestLocationUpdates(mLocationRequest,
                    mLocationCallback,
                    null /* Looper */);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mFusedLocationClient.removeLocationUpdates(mLocationCallback);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putCharSequence(LATITUDE_KEY, latText.getText());
        outState.putCharSequence(LONGITUDE_KEY, lonText.getText());
        outState.putCharSequence(ACCURACY_KEY, accuracyText.getText());
        super.onSaveInstanceState(outState);
    }

}
