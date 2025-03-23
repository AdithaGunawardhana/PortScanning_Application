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

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class EditActivity extends AppCompatActivity {

    EditText nameEdit, emailEdit, passwordEdit;
    ImageView saveBtn;
    String nameUser, emailUser, passwordUser;
    DatabaseReference reference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_edit);

        reference = FirebaseDatabase.getInstance().getReference("user");

        nameEdit = findViewById(R.id.editName);
        emailEdit = findViewById(R.id.editEmail);
        passwordEdit = findViewById(R.id.editPassword);
        saveBtn = findViewById(R.id.btnSave);

        showData();

        saveBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                if (isNameChanged() || isEmailChanged() || isPasswordChanged()) {
                    Toast.makeText(EditActivity.this, "Saved", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(EditActivity.this, "No changes", Toast.LENGTH_SHORT).show();
                }
            }
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    public boolean isNameChanged() {
        if (!nameUser.equals(nameEdit.getText().toString())) {

            reference.child(emailUser).child("name").setValue(nameEdit.getText().toString());
            nameUser = nameEdit.getText().toString();
            return true;
        } else {
            return false;
        }
    }

    public boolean isEmailChanged() {
        if (!emailUser.equals(emailEdit.getText().toString())) {

            reference.child(emailUser).child("email").setValue(emailEdit.getText().toString());
            emailUser = emailEdit.getText().toString();
            return true;
        } else {
            return false;
        }
    }

    public boolean isPasswordChanged() {
        if (!passwordUser.equals(passwordEdit.getText().toString())) {

            reference.child(passwordUser).child("password").setValue(passwordEdit.getText().toString());
            passwordUser = passwordEdit.getText().toString();
            return true;
        } else {
            return false;
        }
    }

    public void showData(){
        Intent intent = getIntent();

        nameUser = intent.getStringExtra("name");
        emailUser = intent.getStringExtra("email");
        passwordUser = intent.getStringExtra("password");

        nameEdit.setText(nameUser);
        emailEdit.setText(emailUser);
        passwordEdit.setText(passwordUser);
    }
}