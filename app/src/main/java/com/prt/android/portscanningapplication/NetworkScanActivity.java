package com.prt.android.portscanningapplication;

import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class NetworkScanActivity extends AppCompatActivity {

    private TextView networkTypeText, scanResultsText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_network_scan);

        // Initialize UI elements
        networkTypeText = findViewById(R.id.networkTypeText);
        scanResultsText = findViewById(R.id.scanResultsText);

        // Make scan results scrollable
        scanResultsText.setMovementMethod(new ScrollingMovementMethod());

        // Get data from intent
        String networkType = getIntent().getStringExtra("NETWORK_TYPE");
        String scanResults = getIntent().getStringExtra("SCAN_RESULTS");

        // Display the data
        networkTypeText.setText("Network Type: " + (networkType != null ? networkType : "Unknown"));
        if (scanResults != null && !scanResults.trim().isEmpty()) {
            scanResultsText.setText(scanResults);
        } else {
            scanResultsText.setText("Scan Results:\nNo devices or open ports found. Ensure you're connected to a network and try again.");
        }

        // Handle window insets for edge-to-edge display
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
}