package com.example.ibnsina;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import org.json.JSONException;
import org.json.JSONObject;

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText etUser, etName, etDesignation, etPass;
    private MaterialButton btnLogin;
    private ProgressBar loginProgressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // ১. সেশন চেক (নিশ্চিত করা যে সেশন ট্রুলি অ্যাক্টিভ আছে কিনা)
        SharedPreferences prefs = getSharedPreferences("USER_SESSION", MODE_PRIVATE);
        if (prefs != null && prefs.getBoolean("isLoggedIn", false)) {
            Intent intent = new Intent(this, DashboardActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }

        setContentView(R.layout.activity_login);

        etUser = findViewById(R.id.etUser);
        etName = findViewById(R.id.etName);
        etDesignation = findViewById(R.id.etDesignation);
        etPass = findViewById(R.id.etPass);
        btnLogin = findViewById(R.id.btnLogin);
        loginProgressBar = findViewById(R.id.loginProgressBar);

        // অটোফিল লজিক (টাইপ করার সময়)
        etUser.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                String id = s.toString().trim().toUpperCase();
                if (id.length() >= 4) {
                    fetchAutoFillOnly(id); // শুধু নাম-পদবী নিয়ে আসবে
                } else {
                    etName.setText("");
                    etDesignation.setText("");
                }
            }
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
        });

        btnLogin.setOnClickListener(v -> {
            String userId = etUser.getText().toString().trim().toUpperCase();
            String password = etPass.getText().toString().trim();

            if (userId.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "আইডি এবং পাসওয়ার্ড দিন", Toast.LENGTH_SHORT).show();
                return;
            }

            // সরাসরি লগইন এবং অটোফিল একসাথে শুরু করবে
            performFullLogin(userId, password);
        });
    }

    // টাইপ করার সময় শুধু ডাটা ফেচ করার জন্য (লগইন করবে না)
    private void fetchAutoFillOnly(String userId) {
        String url = Config.SCRIPT_URL + "?action=getAutoFill&userId=" + userId;
        StringRequest request = new StringRequest(Request.Method.GET, url,
                response -> {
                    try {
                        JSONObject json = new JSONObject(response);
                        if (json.getBoolean("success")) {
                            etName.setText(json.getString("name"));
                            etDesignation.setText(json.getString("designation"));
                        }
                    } catch (JSONException e) { e.printStackTrace(); }
                }, error -> {});
        
        request.setShouldCache(false); // নেটওয়ার্ক ক্যাশ বন্ধ করা হলো
        Volley.newRequestQueue(this).add(request);
    }

    // লগইন বাটনে ক্লিক করলে এই মেথডটি কাজ করবে
    private void performFullLogin(String userId, String password) {
        if (loginProgressBar != null) loginProgressBar.setVisibility(View.VISIBLE);
        btnLogin.setText("Checking...");
        btnLogin.setEnabled(false);

        String url = Config.SCRIPT_URL + "?action=login&userId=" + userId + "&password=" + password;

        StringRequest loginRequest = new StringRequest(Request.Method.GET, url,
                response -> {
                    if (loginProgressBar != null) loginProgressBar.setVisibility(View.GONE);
                    btnLogin.setEnabled(true);
                    btnLogin.setText("Login");
                    try {
                        JSONObject json = new JSONObject(response);
                        if (json.getBoolean("success")) {
                            String userName = json.optString("name", etName.getText().toString().trim());
                            String designation = json.optString("designation", etDesignation.getText().toString().trim());

                            // সেশন নিখুঁতভাবে সেভ করা
                            SharedPreferences prefs = getSharedPreferences("USER_SESSION", MODE_PRIVATE);
                            SharedPreferences.Editor editor = prefs.edit();
                            editor.putBoolean("isLoggedIn", true);
                            editor.putString("userId", userId);
                            editor.putString("userName", userName);
                            editor.putString("designation", designation);
                            editor.apply();

                            // ফ্রেশ টাস্ক হিসেবে ড্যাশবোর্ডে যাওয়া (ব্যাকস্ট্যাক ক্লিয়ার করে)
                            Intent intent = new Intent(LoginActivity.this, DashboardActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                            finish();
                        } else {
                            Toast.makeText(this, "আইডি বা পাসওয়ার্ড ভুল!", Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) { 
                        Toast.makeText(this, "ডাটা প্রসেসিং এরর!", Toast.LENGTH_SHORT).show();
                    }
                }, error -> {
            if (loginProgressBar != null) loginProgressBar.setVisibility(View.GONE);
            btnLogin.setEnabled(true);
            btnLogin.setText("Login");
            Toast.makeText(this, "সার্ভার কানেকশন এরর!", Toast.LENGTH_SHORT).show();
        });

        loginRequest.setShouldCache(false); // অত্যন্ত গুরুত্বপূর্ণ: লগইনের সময় নেটওয়ার্ক ক্যাশ বন্ধ করা হলো
        Volley.newRequestQueue(this).add(loginRequest);
    }
}
