package com.prt.android.portscanningapplication;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class LocationDetailActivity extends AppCompatActivity {

    private TextView ipAddressText, cityText, regionText, countryText, latitudeText, longitudeText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location_detail);

        // Initialize UI elements
        ipAddressText = findViewById(R.id.ipAddressText);
        cityText = findViewById(R.id.cityText);
        regionText = findViewById(R.id.regionText);
        countryText = findViewById(R.id.countryText);
        latitudeText = findViewById(R.id.latitudeText);
        longitudeText = findViewById(R.id.longitudeText);

        // Get data from intent
        String ipAddress = getIntent().getStringExtra("IP_ADDRESS");
        String city = getIntent().getStringExtra("CITY");
        String region = getIntent().getStringExtra("REGION");
        String country = getIntent().getStringExtra("COUNTRY");
        double latitude = getIntent().getDoubleExtra("LATITUDE", 0.0);
        double longitude = getIntent().getDoubleExtra("LONGITUDE", 0.0);

        // Set data to UI
        ipAddressText.setText("IP Address: " + ipAddress);
        cityText.setText("City: " + city);
        regionText.setText("Region: " + region);
        countryText.setText("Country: " + country);
        latitudeText.setText("Latitude: " + latitude);
        longitudeText.setText("Longitude: " + longitude);
    }
}