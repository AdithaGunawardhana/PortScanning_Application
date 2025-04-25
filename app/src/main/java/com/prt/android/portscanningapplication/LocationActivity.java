package com.prt.android.portscanningapplication;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LocationActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 2;
    private static final String TAG = "LocationActivity";
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private TextView locationText;
    private String gatewayIp;
    private GoogleMap googleMap;
    private Marker locationMarker;
    private double latitude;
    private double longitude;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_location);

        // Initialize UI elements
        locationText = findViewById(R.id.locationText);

        // Initialize FusedLocationProviderClient
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Get gateway IP for geolocation
        WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        gatewayIp = intToIp(wifiManager.getDhcpInfo().gateway);
        Log.d(TAG, "Gateway IP: " + gatewayIp);

        // Initialize the map
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            Log.d(TAG, "Map fragment found, initializing map...");
            mapFragment.getMapAsync(this);
        } else {
            Log.e(TAG, "Map fragment not found!");
            Toast.makeText(this, "Error: Map fragment not found", Toast.LENGTH_LONG).show();
            locationText.setText(locationText.getText() + "\n\nError: Map fragment not found");
        }

        // Check location permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Requesting location permission...");
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            Log.d(TAG, "Location permission already granted, starting location updates...");
            startLocationUpdates();
        }

        // Handle window insets for edge-to-edge display
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Location permission granted, starting location updates...");
                startLocationUpdates();
            } else {
                Log.w(TAG, "Location permission denied");
                locationText.setText("Location permission denied. Cannot retrieve device location.");
            }
        }
    }

    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "Location permission not granted");
            return;
        }

        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setInterval(10000); // Update every 10 seconds
        locationRequest.setFastestInterval(5000); // Fastest update interval
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    Log.w(TAG, "Location result is null");
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    Log.d(TAG, "Live location update: Lat=" + location.getLatitude() + ", Lon=" + location.getLongitude());
                    StringBuilder locationInfo = new StringBuilder();
                    locationInfo.append("Device Location:\n");
                    locationInfo.append("Latitude: ").append(location.getLatitude()).append("\n");
                    locationInfo.append("Longitude: ").append(location.getLongitude()).append("\n\n");

                    // Store latitude and longitude for passing to detail activity
                    latitude = location.getLatitude();
                    longitude = location.getLongitude();

                    // Update the map with the live location
                    updateMap(new LatLng(location.getLatitude(), location.getLongitude()));

                    // Fetch network location (only once, after first location update)
                    if (locationText.getText().toString().isEmpty()) {
                        fetchNetworkLocation(locationInfo);
                    } else {
                        updateLocationText(locationInfo.toString());
                    }
                }
            }
        };

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to start location updates: " + e.getMessage());
                    locationText.setText("Failed to start location updates: " + e.getMessage());
                    latitude = 0.0;
                    longitude = 0.0;
                    fetchNetworkLocation(new StringBuilder());
                });
    }

    private void fetchNetworkLocation(StringBuilder locationInfo) {
        locationInfo.append("Network Location (Based on Gateway IP):\n");

        // Variables to store network location data
        String ipAddress = gatewayIp;

        if (gatewayIp == null || gatewayIp.equals("0.0.0.0") || gatewayIp.equals("10.0.2.2")) {
            Log.w(TAG, "Gateway IP not suitable for geolocation: " + gatewayIp);
            locationInfo.append("Gateway IP not suitable for geolocation on emulator\n");
            // For emulator, use mock data or skip API call
            String finalCountry = "N/A (Emulator)";
            String finalRegion = "N/A (Emulator)";
            String finalCity = "N/A (Emulator)";
            navigateToDetailActivity(ipAddress, finalCity, finalRegion, finalCountry, latitude, longitude);
            updateLocationText(locationInfo.toString());
            return;
        }

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            String finalCountry = "Unknown";
            String finalRegion = "Unknown";
            String finalCity = "Unknown";

            try {
                // Use HTTPS to get geolocation data for the gateway IP
                String urlString = "https://ip-api.com/json/" + gatewayIp;
                Log.d(TAG, "Fetching network location from: " + urlString);
                URL url = new URL(urlString);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);

                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();
                conn.disconnect();

                // Parse the JSON response manually (simplified)
                String json = response.toString();
                finalCountry = extractJsonField(json, "country") != null ? extractJsonField(json, "country") : "Unknown";
                finalRegion = extractJsonField(json, "regionName") != null ? extractJsonField(json, "regionName") : "Unknown";
                finalCity = extractJsonField(json, "city") != null ? extractJsonField(json, "city") : "Unknown";

                locationInfo.append("Gateway IP: ").append(gatewayIp).append("\n");
                locationInfo.append("Country: ").append(finalCountry).append("\n");
                locationInfo.append("Region: ").append(finalRegion).append("\n");
                locationInfo.append("City: ").append(finalCity).append("\n");

            } catch (Exception e) {
                Log.e(TAG, "Failed to retrieve network location: " + e.getMessage());
                locationInfo.append("Failed to retrieve network location: ").append(e.getMessage()).append("\n");
            }

            // Update UI on the main thread and navigate to detail activity
            String finalCity1 = finalCity;
            String finalRegion1 = finalRegion;
            String finalCountry1 = finalCountry;
            handler.post(() -> {
                updateLocationText(locationInfo.toString());
                navigateToDetailActivity(ipAddress, finalCity1, finalRegion1, finalCountry1, latitude, longitude);
            });
        });

        executor.shutdown();
    }

    private void navigateToDetailActivity(String ipAddress, String city, String region, String country, double latitude, double longitude) {
        Intent intent = new Intent(LocationActivity.this, LocationDetailActivity.class);
        intent.putExtra("IP_ADDRESS", ipAddress != null ? ipAddress : "Unknown");
        intent.putExtra("CITY", city != null ? city : "Unknown");
        intent.putExtra("REGION", region != null ? region : "Unknown");
        intent.putExtra("COUNTRY", country != null ? country : "Unknown");
        intent.putExtra("LATITUDE", latitude);
        intent.putExtra("LONGITUDE", longitude);
        startActivity(intent);
    }

    private void updateLocationText(String text) {
        locationText.setText(text);
    }

    private String intToIp(int ip) {
        if (ip == 0) return null;
        return String.format("%d.%d.%d.%d",
                (ip & 0xff), (ip >> 8 & 0xff),
                (ip >> 16 & 0xff), (ip >> 24 & 0xff));
    }

    private String extractJsonField(String json, String field) {
        String fieldPattern = "\"" + field + "\":\"";
        int startIndex = json.indexOf(fieldPattern);
        if (startIndex == -1) return null;
        startIndex += fieldPattern.length();
        int endIndex = json.indexOf("\"", startIndex);
        if (endIndex == -1) return null;
        return json.substring(startIndex, endIndex);
    }

    @Override
    public void onMapReady(GoogleMap map) {
        Log.d(TAG, "Map is ready!");
        googleMap = map;

        // Enable zoom controls on the map
        googleMap.getUiSettings().setZoomControlsEnabled(true);

        // Enable "My Location" layer if permission is granted
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Enabling My Location layer on map");
            googleMap.setMyLocationEnabled(true);
        } else {
            Log.w(TAG, "Location permission not granted, cannot enable My Location layer");
        }
    }

    private void updateMap(LatLng latLng) {
        if (googleMap == null) {
            Log.w(TAG, "Cannot update map: googleMap is null");
            return;
        }

        Log.d(TAG, "Updating map with live location: " + latLng.latitude + ", " + latLng.longitude);

        // Remove previous marker if it exists
        if (locationMarker != null) {
            locationMarker.remove();
        }

        // Add a new marker at the updated location
        locationMarker = googleMap.addMarker(new MarkerOptions()
                .position(latLng)
                .title("Your Location"));

        // Move the camera to the updated location and zoom in
        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Stop location updates when the activity is paused to save battery
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Restart location updates when the activity is resumed
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            startLocationUpdates();
        }
    }
}