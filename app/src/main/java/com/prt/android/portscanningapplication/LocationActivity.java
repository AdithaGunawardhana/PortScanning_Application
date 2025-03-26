package com.prt.android.portscanningapplication;

import android.Manifest;
import android.content.Context;
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
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;

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
    private TextView locationText;
    private String gatewayIp;
    private GoogleMap googleMap;
    private LatLng deviceLocation;

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
            Log.d(TAG, "Location permission already granted, fetching location...");
            getLocationAndNetworkInfo();
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
                Log.d(TAG, "Location permission granted, fetching location...");
                getLocationAndNetworkInfo();
            } else {
                Log.w(TAG, "Location permission denied");
                locationText.setText("Location permission denied. Cannot retrieve device location.");
            }
        }
    }

    private void getLocationAndNetworkInfo() {
        StringBuilder locationInfo = new StringBuilder();

        // Get device location
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Fetching device location...");
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                        @Override
                        public void onSuccess(Location location) {
                            if (location != null) {
                                Log.d(TAG, "Device location retrieved: Lat=" + location.getLatitude() + ", Lon=" + location.getLongitude());
                                locationInfo.append("Device Location:\n");
                                locationInfo.append("Latitude: ").append(location.getLatitude()).append("\n");
                                locationInfo.append("Longitude: ").append(location.getLongitude()).append("\n\n");

                                // Store device location for the map
                                deviceLocation = new LatLng(location.getLatitude(), location.getLongitude());

                                // Update the map if it's ready
                                if (googleMap != null) {
                                    Log.d(TAG, "Map is ready, updating with device location...");
                                    updateMap();
                                } else {
                                    Log.w(TAG, "Map not ready yet, will update when ready");
                                }
                            } else {
                                Log.w(TAG, "Device location is null");
                                locationInfo.append("Device Location: Not available (try enabling location services)\n\n");
                            }
                            // After getting device location, fetch network location
                            fetchNetworkLocation(locationInfo);
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to retrieve device location: " + e.getMessage());
                        locationInfo.append("Device Location: Failed to retrieve (").append(e.getMessage()).append(")\n\n");
                        fetchNetworkLocation(locationInfo);
                    });
        } else {
            Log.w(TAG, "Location permission not granted");
            locationInfo.append("Device Location: Permission not granted\n\n");
            fetchNetworkLocation(locationInfo);
        }
    }

    private void fetchNetworkLocation(StringBuilder locationInfo) {
        locationInfo.append("Network Location (Based on Gateway IP):\n");
        if (gatewayIp == null || gatewayIp.equals("0.0.0.0") || gatewayIp.equals("10.0.2.2")) {
            Log.w(TAG, "Gateway IP not suitable for geolocation: " + gatewayIp);
            locationInfo.append("Gateway IP not suitable for geolocation on emulator\n");
            updateLocationText(locationInfo.toString());
            return;
        }

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
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
                String country = extractJsonField(json, "country");
                String region = extractJsonField(json, "regionName");
                String city = extractJsonField(json, "city");

                locationInfo.append("Gateway IP: ").append(gatewayIp).append("\n");
                if (country != null) locationInfo.append("Country: ").append(country).append("\n");
                if (region != null) locationInfo.append("Region: ").append(region).append("\n");
                if (city != null) locationInfo.append("City: ").append(city).append("\n");

            } catch (Exception e) {
                Log.e(TAG, "Failed to retrieve network location: " + e.getMessage());
                locationInfo.append("Failed to retrieve network location: ").append(e.getMessage()).append("\n");
            }

            // Update UI on the main thread
            handler.post(() -> updateLocationText(locationInfo.toString()));
        });

        executor.shutdown();
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

        // Update the map if the device location is already available
        if (deviceLocation != null) {
            Log.d(TAG, "Device location available, updating map...");
            updateMap();
        } else {
            Log.w(TAG, "Device location not yet available, waiting...");
        }
    }

    private void updateMap() {
        if (googleMap == null || deviceLocation == null) {
            Log.w(TAG, "Cannot update map: googleMap=" + googleMap + ", deviceLocation=" + deviceLocation);
            return;
        }

        Log.d(TAG, "Adding marker at: " + deviceLocation.latitude + ", " + deviceLocation.longitude);
        // Add a marker at the device's location
        googleMap.addMarker(new MarkerOptions()
                .position(deviceLocation)
                .title("Your Location"));

        // Move the camera to the device's location and zoom in
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(deviceLocation, 15));
    }
}