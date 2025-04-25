package com.prt.android.portscanningapplication;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class NewsletterActivity extends AppCompatActivity {

    private EditText emailEditText;
    private Button joinButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_newsletter);

        // Enable back button in action bar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Join Our Newsletter");
        }

        // Initialize UI elements
        emailEditText = findViewById(R.id.emailEditText);
        joinButton = findViewById(R.id.joinButton);

        // Pre-fill email if provided
        String email = getIntent().getStringExtra("email");
        if (!TextUtils.isEmpty(email)) {
            emailEditText.setText(email);
        }

        // Join button click listener
        joinButton.setOnClickListener(v -> {
            String emailInput = emailEditText.getText().toString().trim();
            if (isValidEmail(emailInput)) {
                // Simulate newsletter subscription
                showSubscriptionDialog(emailInput);
            } else {
                Toast.makeText(NewsletterActivity.this, "Please enter a valid email address", Toast.LENGTH_SHORT).show();
            }
        });

        // Handle window insets for edge-to-edge display
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private boolean isValidEmail(String email) {
        if (TextUtils.isEmpty(email)) {
            return false;
        }
        // Basic email regex pattern
        String emailPattern = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$";
        return email.matches(emailPattern);
    }

    private void showSubscriptionDialog(String email) {
        new AlertDialog.Builder(this)
                .setTitle("Subscription Successful")
                .setMessage("Thank you for joining our newsletter! You'll receive updates at " + email + ".")
                .setPositiveButton("OK", (dialog, which) -> finish()) // Close activity on confirmation
                .setCancelable(false)
                .show();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}