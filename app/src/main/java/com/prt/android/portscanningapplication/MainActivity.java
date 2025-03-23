package com.prt.android.portscanningapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {

    private EditText searchIp;
    private ImageView profileImage, settings, reports, locations, info, discover, newsletter, searchIcon;
    private TextView upgrade;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // Initialize UI elements
        searchIp = findViewById(R.id.searchText);
        profileImage = findViewById(R.id.imageProfile);
        settings = findViewById(R.id.btnSettings);
        reports = findViewById(R.id.btnReports);
        locations = findViewById(R.id.btnLocation);
        info = findViewById(R.id.btnInfo);
        discover = findViewById(R.id.btnDiscover);
        newsletter = findViewById(R.id.btnNewsletter);
        upgrade = findViewById(R.id.upgradeAccount);
        searchIcon = findViewById(R.id.searchIcon);

        // Search icon click listener
        searchIcon.setOnClickListener(view -> performSearch());

        // Other navigation click listeners
        profileImage.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, ProfileActivity.class)));
        settings.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, SettingsActivity.class)));
        reports.setOnClickListener(v ->
                Toast.makeText(this, "Reports coming soon!", Toast.LENGTH_SHORT).show());
        locations.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, LocationActivity.class)));
        info.setOnClickListener(v ->
                Toast.makeText(this, "Info coming soon!", Toast.LENGTH_SHORT).show());
        discover.setOnClickListener(v ->
                Toast.makeText(this, "Discover coming soon!", Toast.LENGTH_SHORT).show());
        newsletter.setOnClickListener(v ->
                Toast.makeText(this, "Newsletter coming soon!", Toast.LENGTH_SHORT).show());
        upgrade.setOnClickListener(v ->
                Toast.makeText(this, "Upgrade coming soon!", Toast.LENGTH_SHORT).show());

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void performSearch() {
        String searchAddress = searchIp.getText().toString().trim();
        if (searchAddress.isEmpty()) {
            Toast.makeText(this, "Please enter an IP or port number!", Toast.LENGTH_SHORT).show();
        } else {
            Intent intent = new Intent(MainActivity.this, DetailActivity.class);
            intent.putExtra("SEARCH_ADDRESS", searchAddress);
            startActivity(intent);
        }
    }
}