package com.prt.android.portscanningapplication;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class LocationActivity extends AppCompatActivity {

    private EditText searchLocation;
    private ImageView searchLocationBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location);

        // Initialize UI elements
        searchLocation = findViewById(R.id.searchLocation);
        searchLocationBtn = findViewById(R.id.searchLocationBtn);

        // Search button click listener
        searchLocationBtn.setOnClickListener(v -> performLocationSearch());
    }

    private void performLocationSearch() {
        String ipAddress = searchLocation.getText().toString().trim();
        if (ipAddress.isEmpty()) {
            Toast.makeText(this, "Please enter an IP address!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Simulate fetching geolocation data (in a real app, you'd use an API like ip-api.com)
        GeoLocation geoLocation = fetchGeoLocation(ipAddress);
        if (geoLocation == null) {
            Toast.makeText(this, "Could not fetch location for IP: " + ipAddress, Toast.LENGTH_SHORT).show();
            return;
        }

        // Pass the geolocation data to LocationDetailActivity
        Intent intent = new Intent(LocationActivity.this, LocationDetailActivity.class);
        intent.putExtra("IP_ADDRESS", ipAddress);
        intent.putExtra("CITY", geoLocation.getCity());
        intent.putExtra("REGION", geoLocation.getRegion());
        intent.putExtra("COUNTRY", geoLocation.getCountry());
        intent.putExtra("LATITUDE", geoLocation.getLatitude());
        intent.putExtra("LONGITUDE", geoLocation.getLongitude());
        startActivity(intent);
    }

    // Simulated geolocation fetching (in a real app, use an API)
    private GeoLocation fetchGeoLocation(String ipAddress) {
        // This is a mock implementation. In a real app, you'd make an HTTP request to a geolocation API.
        // For example, using ip-api.com: http://ip-api.com/json/{ip}
        if (ipAddress.equals("8.8.8.8")) { // Example IP (Google DNS)
            return new GeoLocation("Mountain View", "California", "United States", 37.422, -122.084);
        } else if (ipAddress.equals("1.1.1.1")) { // Example IP (Cloudflare DNS)
            return new GeoLocation("Sydney", "New South Wales", "Australia", -33.8688, 151.2093);
        } else {
            return null; // Simulate failure for unknown IPs
        }
    }

    // Simple class to hold geolocation data
    private static class GeoLocation {
        private final String city;
        private final String region;
        private final String country;
        private final double latitude;
        private final double longitude;

        public GeoLocation(String city, String region, String country, double latitude, double longitude) {
            this.city = city;
            this.region = region;
            this.country = country;
            this.latitude = latitude;
            this.longitude = longitude;
        }

        public String getCity() {
            return city;
        }

        public String getRegion() {
            return region;
        }

        public String getCountry() {
            return country;
        }

        public double getLatitude() {
            return latitude;
        }

        public double getLongitude() {
            return longitude;
        }
    }
}