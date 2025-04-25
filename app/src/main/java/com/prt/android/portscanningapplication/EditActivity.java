package com.prt.android.portscanningapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
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

public class EditActivity extends AppCompatActivity {

    EditText nameEdit, emailEdit, passwordEdit;
    ImageView saveBtn;
    String nameUser, emailUser, passwordUser, userId;
    DatabaseReference reference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_edit);

        // Correct Firebase reference to 'users'
        reference = FirebaseDatabase.getInstance().getReference("users");

        nameEdit = findViewById(R.id.editName);
        emailEdit = findViewById(R.id.editEmail);
        passwordEdit = findViewById(R.id.editPassword);
        saveBtn = findViewById(R.id.btnSave);

        // Show existing user data
        showData();

        // Find userId based on email before allowing save
        findUserId();

        saveBtn.setOnClickListener(view -> {
            if (userId == null) {
                Toast.makeText(EditActivity.this, "User not found. Please try again.", Toast.LENGTH_SHORT).show();
                return;
            }

            boolean isChanged = false;
            if (isNameChanged()) isChanged = true;
            if (isEmailChanged()) isChanged = true;
            if (isPasswordChanged()) isChanged = true;

            if (isChanged) {
                Toast.makeText(EditActivity.this, "Changes saved", Toast.LENGTH_SHORT).show();
                // Navigate back to ProfileActivity with updated data
                Intent intent = new Intent(EditActivity.this, ProfileActivity.class);
                intent.putExtra("email", emailUser);
                intent.putExtra("name", nameUser);
                startActivity(intent);
                finish(); // Close EditActivity
            } else {
                Toast.makeText(EditActivity.this, "No changes made", Toast.LENGTH_SHORT).show();
            }
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void findUserId() {
        if (emailUser == null || emailUser.isEmpty()) {
            Toast.makeText(this, "Email not provided.", Toast.LENGTH_SHORT).show();
            return;
        }

        Query query = reference.orderByChild("email").equalTo(emailUser);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                        userId = userSnapshot.getKey(); // Get the unique userId
                        break; // Exit after finding the user
                    }
                } else {
                    Toast.makeText(EditActivity.this, "User not found in database.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(EditActivity.this, "Database error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    public boolean isNameChanged() {
        String newName = nameEdit.getText().toString().trim();
        if (!nameUser.equals(newName)) {
            reference.child(userId).child("name").setValue(newName);
            nameUser = newName;
            return true;
        }
        return false;
    }

    public boolean isEmailChanged() {
        String newEmail = emailEdit.getText().toString().trim();
        if (!emailUser.equals(newEmail)) {
            reference.child(userId).child("email").setValue(newEmail);
            emailUser = newEmail;
            return true;
        }
        return false;
    }

    public boolean isPasswordChanged() {
        String newPassword = passwordEdit.getText().toString().trim();
        if (!passwordUser.equals(newPassword)) {
            reference.child(userId).child("password").setValue(newPassword);
            passwordUser = newPassword;
            return true;
        }
        return false;
    }

    public void showData() {
        Intent intent = getIntent();

        nameUser = intent.getStringExtra("name");
        emailUser = intent.getStringExtra("email");
        passwordUser = intent.getStringExtra("password");

        nameEdit.setText(nameUser != null ? nameUser : "");
        emailEdit.setText(emailUser != null ? emailUser : "");
        passwordEdit.setText(passwordUser != null ? passwordUser : "");
    }
}