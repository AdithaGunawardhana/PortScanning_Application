package com.prt.android.portscanningapplication;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.method.ScrollingMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.view.View;
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

        // Make scan results scrollable and links clickable
        scanResultsText.setMovementMethod(new ScrollingMovementMethod());
        scanResultsText.setMovementMethod(LinkMovementMethod.getInstance());

        // Get data from intent
        String networkType = getIntent().getStringExtra("NETWORK_TYPE");
        String scanResults = getIntent().getStringExtra("SCAN_RESULTS");

        // Display the network type
        networkTypeText.setText("Network Type: " + (networkType != null ? networkType : "Unknown"));

        // Display scan results with color-coded risk levels and clickable links
        if (scanResults != null && !scanResults.trim().isEmpty()) {
            SpannableString spannableResults = formatScanResults(scanResults);
            scanResultsText.setText(spannableResults);
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

    private SpannableString formatScanResults(String scanResults) {
        SpannableString spannableString = new SpannableString(scanResults);

        // Split the results into lines for processing
        String[] lines = scanResults.split("\n");
        int currentPosition = 0;

        for (String line : lines) {
            // Color-code risk levels
            if (line.contains("[Risk: High]")) {
                int start = scanResults.indexOf("[Risk: High]", currentPosition);
                int end = start + "[Risk: High]".length();
                spannableString.setSpan(new ForegroundColorSpan(Color.RED), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else if (line.contains("[Risk: Moderate]")) {
                int start = scanResults.indexOf("[Risk: Moderate]", currentPosition);
                int end = start + "[Risk: Moderate]".length();
                spannableString.setSpan(new ForegroundColorSpan(Color.YELLOW), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else if (line.contains("[Risk: Low]")) {
                int start = scanResults.indexOf("[Risk: Low]", currentPosition);
                int end = start + "[Risk: Low]".length();
                spannableString.setSpan(new ForegroundColorSpan(Color.GREEN), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }

            // Make "Learn More" links clickable
            if (line.contains("Learn more: https://")) {
                int urlStart = line.indexOf("https://");
                if (urlStart != -1) {
                    int lineStart = scanResults.indexOf(line, currentPosition);
                    int start = lineStart + urlStart;
                    int end = lineStart + line.length();

                    String url = line.substring(urlStart);
                    spannableString.setSpan(new ClickableSpan() {
                        @Override
                        public void onClick(View widget) {
                            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                            startActivity(browserIntent);
                        }

                        @Override
                        public void updateDrawState(TextPaint ds) {
                            super.updateDrawState(ds);
                            ds.setColor(Color.BLUE);
                            ds.setUnderlineText(true);
                        }
                    }, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            }

            currentPosition += line.length() + 1; // +1 for the newline character
        }

        return spannableString;
    }
}