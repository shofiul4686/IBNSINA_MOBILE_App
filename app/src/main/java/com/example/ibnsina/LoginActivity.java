package com.example.ibnsina;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText etUser, etName, etDesignation, etPass;
    private MaterialButton btnLogin;
    private ProgressBar loginProgressBar;
    private DatabaseReference mDatabase;
    private Handler searchHandler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences prefs = getSharedPreferences("USER_SESSION", MODE_PRIVATE);
        if (prefs != null && prefs.getBoolean("isLoggedIn", false)) {
            startActivity(new Intent(this, DashboardActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_login);
        
        // আপনার ইমেজের নোড নাম: Emp_Password
        mDatabase = FirebaseDatabase.getInstance().getReference("Emp_Password");

        etUser = findViewById(R.id.etUser);
        etName = findViewById(R.id.etName);
        etDesignation = findViewById(R.id.etDesignation);
        etPass = findViewById(R.id.etPass);
        btnLogin = findViewById(R.id.btnLogin);
        loginProgressBar = findViewById(R.id.loginProgressBar);

        etUser.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                if (searchRunnable != null) searchHandler.removeCallbacks(searchRunnable);
                
                final String id = s.toString().trim();
                if (id.length() >= 4) {
                    searchRunnable = () -> fetchUserFromFirebase(id);
                    searchHandler.postDelayed(searchRunnable, 600); 
                } else {
                    etName.setText("");
                    etDesignation.setText("");
                }
            }
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
        });

        btnLogin.setOnClickListener(v -> {
            String userId = etUser.getText().toString().trim();
            String password = etPass.getText().toString().trim();

            if (userId.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "আইডি এবং পাসওয়ার্ড দিন", Toast.LENGTH_SHORT).show();
                return;
            }
            performFirebaseLogin(userId, password);
        });
    }

    private void fetchUserFromFirebase(String userId) {
        if (loginProgressBar != null) loginProgressBar.setVisibility(View.VISIBLE);
        
        // USER ID ফিল্ড দিয়ে সার্চ করা হচ্ছে
        Query query = mDatabase.orderByChild("USER ID").equalTo(userId);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (loginProgressBar != null) loginProgressBar.setVisibility(View.GONE);
                if (snapshot.exists()) {
                    for (DataSnapshot userSnap : snapshot.getChildren()) {
                        String name = userSnap.child("USER NAME").getValue(String.class);
                        String designation = userSnap.child("DESIGNATION").getValue(String.class);
                        etName.setText(name != null ? name : "");
                        etDesignation.setText(designation != null ? designation : "");
                        break; // প্রথম ম্যাচটি নিবে
                    }
                } else {
                    etName.setText("");
                    etDesignation.setText("");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (loginProgressBar != null) loginProgressBar.setVisibility(View.GONE);
            }
        });
    }

    private void performFirebaseLogin(String userId, String password) {
        if (loginProgressBar != null) loginProgressBar.setVisibility(View.VISIBLE);
        btnLogin.setEnabled(false);
        btnLogin.setText("Checking...");

        Query query = mDatabase.orderByChild("USER ID").equalTo(userId);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (loginProgressBar != null) loginProgressBar.setVisibility(View.GONE);
                btnLogin.setEnabled(true);
                btnLogin.setText("Login");

                if (snapshot.exists()) {
                    boolean loggedIn = false;
                    for (DataSnapshot userSnap : snapshot.getChildren()) {
                        Object passObj = userSnap.child("PASSWORD").getValue();
                        String dbPassword = (passObj != null) ? passObj.toString() : "";
                        
                        if (dbPassword.equals(password)) {
                            // সেশন সেভ
                            SharedPreferences.Editor editor = getSharedPreferences("USER_SESSION", MODE_PRIVATE).edit();
                            editor.putBoolean("isLoggedIn", true);
                            editor.putString("userId", userId);
                            editor.putString("userName", userSnap.child("USER NAME").getValue(String.class));
                            editor.putString("designation", userSnap.child("DESIGNATION").getValue(String.class));
                            editor.putString("firebaseKey", userSnap.getKey()); // পাসওয়ার্ড আপডেটের জন্য কি সেভ করে রাখা
                            editor.apply();

                            startActivity(new Intent(LoginActivity.this, DashboardActivity.class));
                            finish();
                            loggedIn = true;
                            break;
                        }
                    }
                    if (!loggedIn) {
                        Toast.makeText(LoginActivity.this, "ভুল পাসওয়ার্ড!", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(LoginActivity.this, "আইডি পাওয়া যায়নি!", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (loginProgressBar != null) loginProgressBar.setVisibility(View.GONE);
                btnLogin.setEnabled(true);
                btnLogin.setText("Login");
                Toast.makeText(LoginActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
