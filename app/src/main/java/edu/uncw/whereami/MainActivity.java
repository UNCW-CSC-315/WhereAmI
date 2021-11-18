package edu.uncw.whereami;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void clickOnDemand(View view) {
        startActivity(new Intent(this, OnDemandActivity.class));
    }

    public void clickContinuous(View view) {
        startActivity(new Intent(this, ContinuousLocActivity.class));
    }

    public void clickMaps(View View) {
        startActivity(new Intent(this, MapsActivity.class));
    }
}
