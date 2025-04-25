package com.prt.android.portscanningapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

public class ProfileActivity extends AppCompatActivity {

    TextView profileName, profileEmail, profilePassword;
    ImageView editButton;
    private String userName, userEmail, userPassword; // Store user data locally

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_profile);

        profileName = findViewById(R.id.profileName);
        profileEmail = findViewById(R.id.profileEmail);
        profilePassword = findViewById(R.id.profilePassword);
        editButton = findViewById(R.id.btnEdit);

        // Hide password field for security
        profilePassword.setVisibility(View.GONE);

        // Fetch user data from Firebase
        fetchUserData();

        editButton.setOnClickListener(view -> passUserData());

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void fetchUserData() {
        // Get email from Intent
        Intent intent = getIntent();
        String emailUser = intent.getStringExtra("email");

        // If no email from Intent, try using the stored userEmail
        if ((emailUser == null || emailUser.isEmpty()) && userEmail != null && !userEmail.isEmpty()) {
            emailUser = userEmail;
        }

        // If still no email, redirect to LoginActivity
        if (emailUser == null || emailUser.isEmpty()) {
            Toast.makeText(this, "User data not found. Please log in again.", Toast.LENGTH_SHORT).show();
            Intent loginIntent = new Intent(ProfileActivity.this, LoginActivity.class);
            startActivity(loginIntent);
            finish();
            return;
        }

        // Query Firebase to fetch user data by email
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("users");
        Query checkUserDatabase = reference.orderByChild("email").equalTo(emailUser);

        checkUserDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                        userName = userSnapshot.child("name").getValue(String.class);
                        userEmail = userSnapshot.child("email").getValue(String.class);
                        userPassword = userSnapshot.child("password").getValue(String.class); // Store for EditActivity

                        // Update UI with fetched data
                        profileName.setText(userName != null ? userName : "N/A");
                        profileEmail.setText(userEmail != null ? userEmail : "N/A");
                        break; // Exit loop since email should be unique
                    }
                } else {
                    Toast.makeText(ProfileActivity.this, "User not found in database.", Toast.LENGTH_SHORT).show();
                    Intent loginIntent = new Intent(ProfileActivity.this, LoginActivity.class);
                    startActivity(loginIntent);
                    finish();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ProfileActivity.this, "Database error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void passUserData() {
        if (userEmail == null || userEmail.isEmpty()) {
            Toast.makeText(this, "User data not available. Please try again.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Pass stored user data to EditActivity
        Intent intent = new Intent(ProfileActivity.this, EditActivity.class);
        intent.putExtra("name", userName);
        intent.putExtra("email", userEmail);
        intent.putExtra("password", userPassword);
        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh data when returning from EditActivity
        fetchUserData();
    }
}