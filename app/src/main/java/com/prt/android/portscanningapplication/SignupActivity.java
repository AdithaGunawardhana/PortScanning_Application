package com.prt.android.portscanningapplication;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
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
import com.google.firebase.database.ValueEventListener;

public class SignupActivity extends AppCompatActivity {

    EditText signupName, signupEmail, signupPassword;
    ImageView signupButton;
    TextView loginRedirectText;

    FirebaseDatabase database;
    DatabaseReference reference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_signup);

        signupName = findViewById(R.id.name);
        signupEmail = findViewById(R.id.email);
        signupPassword = findViewById(R.id.password);
        signupButton = findViewById(R.id.btnSignup);
        loginRedirectText = findViewById(R.id.loginRedirect);

        signupButton.setOnClickListener(view -> {
            if (!validateName() || !validateEmail() || !validatePassword()) {
                return;
            }

            database = FirebaseDatabase.getInstance();
            reference = database.getReference("users");

            String name = signupName.getText().toString().trim();
            String email = signupEmail.getText().toString().trim();
            String password = signupPassword.getText().toString().trim();

            // Check if email already exists
            reference.orderByChild("email").equalTo(email).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        signupEmail.setError("Email already registered");
                        signupEmail.requestFocus();
                    } else {
                        // Proceed with user registration
                        String userId = reference.push().getKey();
                        HelperClass helperClass = new HelperClass(name, email, password);

                        // Store user under a unique ID
                        reference.child(userId).setValue(helperClass).addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                // Save email to SharedPreferences
                                SharedPreferences sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE);
                                SharedPreferences.Editor editor = sharedPreferences.edit();
                                editor.putString("email", email);
                                editor.apply();

                                Toast.makeText(SignupActivity.this, "User registered successfully", Toast.LENGTH_SHORT).show();
                                Intent intent = new Intent(SignupActivity.this, MainActivity.class);
                                intent.putExtra("name", name);
                                intent.putExtra("email", email);
                                startActivity(intent);
                                finish(); // Close SignupActivity
                            } else {
                                Toast.makeText(SignupActivity.this, "Registration failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Toast.makeText(SignupActivity.this, "Database error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        });

        loginRedirectText.setOnClickListener(view -> {
            Intent intent = new Intent(SignupActivity.this, LoginActivity.class);
            startActivity(intent);
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private Boolean validateName() {
        String val = signupName.getText().toString().trim();
        if (val.isEmpty()) {
            signupName.setError("Name cannot be empty");
            return false;
        } else {
            signupName.setError(null);
            return true;
        }
    }

    private Boolean validateEmail() {
        String val = signupEmail.getText().toString().trim();
        String emailPattern = "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+";
        if (val.isEmpty()) {
            signupEmail.setError("Email cannot be empty");
            return false;
        } else if (!val.matches(emailPattern)) {
            signupEmail.setError("Invalid email address");
            return false;
        } else {
            signupEmail.setError(null);
            return true;
        }
    }

    private Boolean validatePassword() {
        String val = signupPassword.getText().toString().trim();
        if (val.isEmpty()) {
            signupPassword.setError("Password cannot be empty");
            return false;
        } else if (val.length() < 6) {
            signupPassword.setError("Password must be at least 6 characters");
            return false;
        } else {
            signupPassword.setError(null);
            return true;
        }
    }
}