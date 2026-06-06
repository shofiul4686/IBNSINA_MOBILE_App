package com.example.ibnsina;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import org.json.JSONArray;
import org.json.JSONObject;

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText etUser, etName, etDesignation, etPass;
    private MaterialButton btnLogin;
    private ProgressBar loginProgressBar;
    private RequestQueue requestQueue;
    private Handler searchHandler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // ১. সেশন চেক
        SharedPreferences prefs = getSharedPreferences("USER_SESSION", MODE_PRIVATE);
        if (prefs != null && prefs.getBoolean("isLoggedIn", false)) {
            startActivity(new Intent(this, DashboardActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_login);
        requestQueue = Volley.newRequestQueue(this);

        etUser = findViewById(R.id.etUser);
        etName = findViewById(R.id.etName);
        etDesignation = findViewById(R.id.etDesignation);
        etPass = findViewById(R.id.etPass);
        btnLogin = findViewById(R.id.btnLogin);
        loginProgressBar = findViewById(R.id.loginProgressBar);

        // আইডি টাইপ করার লজিক
        etUser.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                if (searchRunnable != null) searchHandler.removeCallbacks(searchRunnable);
                
                final String id = s.toString().trim();
                // কমপক্ষে ৪টি অক্ষর হলে (যেমন admin বা IPI-) সার্চ করবে
                if (id.length() >= 4) {
                    if (loginProgressBar != null) loginProgressBar.setVisibility(View.VISIBLE);
                    searchRunnable = () -> fetchAutoFillOnly(id);
                    searchHandler.postDelayed(searchRunnable, 800); 
                } else {
                    cancelAutoFillRequests();
                    etName.setText("");
                    etDesignation.setText("");
                    if (loginProgressBar != null) loginProgressBar.setVisibility(View.GONE);
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
            performFullLogin(userId, password);
        });
    }

    private void cancelAutoFillRequests() {
        if (requestQueue != null) requestQueue.cancelAll("autofill");
    }

    private void fetchAutoFillOnly(String userId) {
        cancelAutoFillRequests();
        if (loginProgressBar != null) loginProgressBar.setVisibility(View.VISIBLE);
        
        // আপনার স্ক্রিপ্টের জন্য সঠিক প্যারামিটার
        String url = Config.SCRIPT_URL + "?action=getAutoFill&userId=" + Uri.encode(userId);
        Log.d("LOGIN_DEBUG", "URL: " + url);
        
        StringRequest request = new StringRequest(Request.Method.GET, url,
                response -> {
                    if (isFinishing()) return;
                    if (loginProgressBar != null) loginProgressBar.setVisibility(View.GONE);
                    try {
                        Log.d("LOGIN_DEBUG", "Response: " + response);
                        JSONObject json = parseResponse(response);
                        if (json != null) {
                            if (json.optBoolean("success", false)) {
                                etName.setText(json.optString("name", ""));
                                etDesignation.setText(json.optString("designation", ""));
                            } else {
                                etName.setText("");
                                etDesignation.setText("");
                                String msg = json.optString("msg", json.optString("message", "আইডি পাওয়া যায়নি"));
                                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
                            }
                        }
                    } catch (Exception e) { 
                        Log.e("LOGIN_DEBUG", "JSON Error", e);
                    }
                }, error -> {
                    if (isFinishing()) return;
                    if (loginProgressBar != null) loginProgressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "সার্ভার কানেকশন এরর!", Toast.LENGTH_SHORT).show();
                });
        
        request.setTag("autofill");
        request.setShouldCache(false);
        request.setRetryPolicy(new DefaultRetryPolicy(10000, 1, 1.0f));
        requestQueue.add(request);
    }

    private void performFullLogin(String userId, String password) {
        if (searchRunnable != null) searchHandler.removeCallbacks(searchRunnable);
        cancelAutoFillRequests();
        
        if (loginProgressBar != null) loginProgressBar.setVisibility(View.VISIBLE);
        btnLogin.setEnabled(false);
        btnLogin.setText("Checking...");

        String url = Config.SCRIPT_URL + "?action=login&userId=" + Uri.encode(userId) + "&password=" + Uri.encode(password);

        StringRequest loginRequest = new StringRequest(Request.Method.GET, url,
                response -> {
                    if (isFinishing()) return;
                    if (loginProgressBar != null) loginProgressBar.setVisibility(View.GONE);
                    btnLogin.setEnabled(true);
                    btnLogin.setText("Login");
                    try {
                        JSONObject json = parseResponse(response);
                        if (json != null) {
                            if (json.optBoolean("success", false)) {
                                SharedPreferences.Editor editor = getSharedPreferences("USER_SESSION", MODE_PRIVATE).edit();
                                editor.putBoolean("isLoggedIn", true);
                                editor.putString("userId", userId);
                                editor.putString("userName", json.optString("name", ""));
                                editor.putString("designation", json.optString("designation", ""));
                                editor.apply();

                                startActivity(new Intent(LoginActivity.this, DashboardActivity.class));
                                finish();
                            } else {
                                String msg = json.optString("msg", json.optString("message", "ভুল পাসওয়ার্ড!"));
                                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
                            }
                        }
                    } catch (Exception e) {
                        Toast.makeText(this, "সার্ভার এরর!", Toast.LENGTH_SHORT).show();
                    }
                }, error -> {
            if (isFinishing()) return;
            if (loginProgressBar != null) loginProgressBar.setVisibility(View.GONE);
            btnLogin.setEnabled(true);
            btnLogin.setText("Login");
            Toast.makeText(this, "ইন্টারনেট সমস্যা!", Toast.LENGTH_SHORT).show();
        });

        loginRequest.setShouldCache(false);
        loginRequest.setRetryPolicy(new DefaultRetryPolicy(20000, 1, 1.0f));
        requestQueue.add(loginRequest);
    }

    private JSONObject parseResponse(String response) {
        try {
            String res = response.trim();
            if (res.startsWith("[")) {
                JSONArray array = new JSONArray(res);
                return array.length() > 0 ? array.getJSONObject(0) : null;
            } else {
                return new JSONObject(res);
            }
        } catch (Exception e) { 
            return null; 
        }
    }
}
