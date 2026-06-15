package com.example.ibnsina;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class DashboardActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        TextView tvUserName = findViewById(R.id.tvUserName);
        TextView tvUserDetails = findViewById(R.id.tvUserDetails);

        SharedPreferences prefs = getSharedPreferences("USER_SESSION", MODE_PRIVATE);
        tvUserName.setText(prefs.getString("userName", "User"));
        tvUserDetails.setText("ID: " + prefs.getString("userId", "N/A") + " | " + prefs.getString("designation", "N/A"));

        // মেনু বাটনগুলো কানেক্ট করা
        findViewById(R.id.cardPharma).setOnClickListener(v -> startActivity(new Intent(this, PharmaMenuActivity.class)));
        findViewById(R.id.cardSinaVision).setOnClickListener(v -> startActivity(new Intent(this, SinaVisionMenuActivity.class)));
        findViewById(R.id.cardINM).setOnClickListener(v -> startActivity(new Intent(this, InmMenuActivity.class)));
        
        // নতুন DATA UPLOAD বাটন
        findViewById(R.id.cardDataUpload).setOnClickListener(v -> startActivity(new Intent(this, DataUploadActivity.class)));

        findViewById(R.id.cardSetting).setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));
        
        findViewById(R.id.cardAbout).setOnClickListener(v -> 
                Toast.makeText(this, "IBN SINA Inventory System v1.0", Toast.LENGTH_SHORT).show());

        findViewById(R.id.cardLogout).setOnClickListener(v -> {
            getSharedPreferences("USER_SESSION", MODE_PRIVATE).edit().clear().apply();
            startActivity(new Intent(this, LoginActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
            finish();
        });
    }
}