package com.example.ibnsina;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class PharmaMenuActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pharma_menu);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // Stock Report Update (MainActivity-তে Pharma ক্যাটাগরি পাঠাবে)
        findViewById(R.id.cardStockReport).setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivity.class);
            intent.putExtra("SELECTED_CATEGORY", "PHARMA");
            startActivity(intent);
        });

        // Carton QTY Update (CartonUpdateActivity-তে PHARMA পাঠাবে)
        findViewById(R.id.cardOrderList).setOnClickListener(v -> {
            Intent intent = new Intent(this, CartonUpdateActivity.class);
            intent.putExtra("SELECTED_CATEGORY", "PHARMA");
            startActivity(intent);
        });

        findViewById(R.id.cardSalesUpdate).setOnClickListener(v -> openWebLink("https://script.google.com/macros/s/AKfycbwIULJidE-kHC3Hzi-tczf3NS8hQC5ZX6ht0n43KybX_4zJfcBlsB3LIJdsu7m3GDGmvQ/exec"));
        findViewById(R.id.cardShortItems).setOnClickListener(v -> openWebLink("https://script.google.com/macros/s/AKfycbz5rg_3VG0pF4u98p_najf_lubb5XHA85GzcNqrX-3aRfT-BySUlULu0l9zeEMXtdQQVA/exec"));
    }

    private void openWebLink(String url) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (Exception e) {
            Toast.makeText(this, "Could not open link", Toast.LENGTH_SHORT).show();
        }
    }
}