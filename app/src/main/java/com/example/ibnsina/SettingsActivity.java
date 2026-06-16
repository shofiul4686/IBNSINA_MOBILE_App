package com.example.ibnsina;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class SettingsActivity extends AppCompatActivity {

    EditText etOldPass, etNewPass, etConfirmPass;
    Button btnUpdatePass;
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // Firebase reference matching your image
        mDatabase = FirebaseDatabase.getInstance().getReference("Emp_Password");

        etOldPass = findViewById(R.id.etOldPass);
        etNewPass = findViewById(R.id.etNewPass);
        etConfirmPass = findViewById(R.id.etConfirmPass);
        btnUpdatePass = findViewById(R.id.btnUpdatePass);

        btnUpdatePass.setOnClickListener(v -> {
            String oldPass = etOldPass.getText().toString().trim();
            String newPass = etNewPass.getText().toString().trim();
            String confirmPass = etConfirmPass.getText().toString().trim();

            SharedPreferences prefs = getSharedPreferences("USER_SESSION", MODE_PRIVATE);
            String firebaseKey = prefs.getString("firebaseKey", "");

            if (oldPass.isEmpty() || newPass.isEmpty() || confirmPass.isEmpty()) {
                Toast.makeText(this, "সবগুলো ঘর পূরণ করুন", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!newPass.equals(confirmPass)) {
                etConfirmPass.setError("পাসওয়ার্ড মিলছে না!");
                return;
            }

            if (firebaseKey.isEmpty()) {
                Toast.makeText(this, "সেশন এরর! আবার লগইন করুন।", Toast.LENGTH_SHORT).show();
                return;
            }

            updatePasswordInFirebase(firebaseKey, oldPass, newPass);
        });
    }

    private void updatePasswordInFirebase(String firebaseKey, String oldPass, String newPass) {
        btnUpdatePass.setEnabled(false);
        btnUpdatePass.setText("Updating...");

        mDatabase.child(firebaseKey).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    // Fetching PASSWORD field (matches your image)
                    Object passObj = snapshot.child("PASSWORD").getValue();
                    String dbPassword = (passObj != null) ? passObj.toString() : "";

                    if (dbPassword.equals(oldPass)) {
                        // Updating PASSWORD field
                        mDatabase.child(firebaseKey).child("PASSWORD").setValue(newPass)
                                .addOnSuccessListener(aVoid -> {
                                    btnUpdatePass.setEnabled(true);
                                    btnUpdatePass.setText("Update Password");
                                    Toast.makeText(SettingsActivity.this, "পাসওয়ার্ড সফলভাবে আপডেট হয়েছে!", Toast.LENGTH_SHORT).show();
                                    etOldPass.setText("");
                                    etNewPass.setText("");
                                    etConfirmPass.setText("");
                                })
                                .addOnFailureListener(e -> {
                                    btnUpdatePass.setEnabled(true);
                                    btnUpdatePass.setText("Update Password");
                                    Toast.makeText(SettingsActivity.this, "আপডেট করতে সমস্যা হয়েছে!", Toast.LENGTH_SHORT).show();
                                });
                    } else {
                        btnUpdatePass.setEnabled(true);
                        btnUpdatePass.setText("Update Password");
                        Toast.makeText(SettingsActivity.this, "পুরাতন পাসওয়ার্ড ভুল!", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    btnUpdatePass.setEnabled(true);
                    btnUpdatePass.setText("Update Password");
                    Toast.makeText(SettingsActivity.this, "ইউজার ডেটা পাওয়া যায়নি!", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                btnUpdatePass.setEnabled(true);
                btnUpdatePass.setText("Update Password");
                Toast.makeText(SettingsActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
