package com.prt.android.portscanningapplication;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
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
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

public class LoginActivity extends AppCompatActivity {

    EditText loginEmail, loginPassword;
    ImageView loginButton;
    Button buttonFacebook, buttonGoogle;
    TextView signupRedirectText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        loginEmail = findViewById(R.id.email);
        loginPassword = findViewById(R.id.password);
        loginButton = findViewById(R.id.btnLogin);
        buttonFacebook = findViewById(R.id.btnFacebook);
        buttonGoogle = findViewById(R.id.btnGoogle);
        signupRedirectText = findViewById(R.id.signupRedirect);

        loginButton.setOnClickListener(view -> {
            if (!validateEmail() || !validatePassword()) {
                return;
            }
            checkUser();
        });

        buttonFacebook.setOnClickListener(view -> {
            Toast.makeText(LoginActivity.this, "Facebook login clicked", Toast.LENGTH_SHORT).show();
            // Placeholder: Add Facebook login logic here (e.g., Firebase Auth with Facebook)
        });

        buttonGoogle.setOnClickListener(view -> {
            Toast.makeText(LoginActivity.this, "Google login clicked", Toast.LENGTH_SHORT).show();
            // Placeholder: Add Google login logic here (e.g., Firebase Auth with Google)
        });

        signupRedirectText.setOnClickListener(view -> {
            Intent intent = new Intent(LoginActivity.this, SignupActivity.class);
            startActivity(intent);
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    public Boolean validateEmail() {
        String val = loginEmail.getText().toString().trim();
        String emailPattern = "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+";
        if (val.isEmpty()) {
            loginEmail.setError("Email cannot be empty");
            return false;
        } else if (!val.matches(emailPattern)) {
            loginEmail.setError("Invalid email address");
            return false;
        } else {
            loginEmail.setError(null);
            return true;
        }
    }

    public Boolean validatePassword() {
        String val = loginPassword.getText().toString().trim();
        if (val.isEmpty()) {
            loginPassword.setError("Password cannot be empty");
            return false;
        } else {
            loginPassword.setError(null);
            return true;
        }
    }

    public void checkUser() {
        String userEmail = loginEmail.getText().toString().trim();
        String userPassword = loginPassword.getText().toString().trim();

        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("users");
        Query checkUserDatabase = reference.orderByChild("email").equalTo(userEmail);

        checkUserDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                        String passwordFromDB = userSnapshot.child("password").getValue(String.class);
                        if (passwordFromDB != null && passwordFromDB.equals(userPassword)) {
                            String nameFromDB = userSnapshot.child("name").getValue(String.class);
                            String emailFromDB = userSnapshot.child("email").getValue(String.class);

                            SharedPreferences sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE);
                            SharedPreferences.Editor editor = sharedPreferences.edit();
                            editor.putString("email", emailFromDB);
                            editor.apply();

                            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                            intent.putExtra("name", nameFromDB);
                            intent.putExtra("email", emailFromDB);
                            startActivity(intent);
                            finish();
                            return;
                        }
                    }
                    loginPassword.setError("Invalid password");
                    loginPassword.requestFocus();
                } else {
                    loginEmail.setError("User does not exist");
                    loginEmail.requestFocus();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(LoginActivity.this, "Database error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}