package com.example.ibnsina;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DataUploadActivity extends AppCompatActivity {

    private Button btnSelectFile, btnSync;
    private ProgressBar progressBar;
    private TextView tvStatus;
    private Uri fileUri;
    private List<CSVRow> parsedData = new ArrayList<>();
    private DatabaseReference mDatabase;
    private ActivityResultLauncher<Intent> filePickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_data_upload);

        mDatabase = FirebaseDatabase.getInstance().getReference("PRODUCTS");

        btnSelectFile = findViewById(R.id.btnStartUpload);
        btnSync = findViewById(R.id.btnSync);
        progressBar = findViewById(R.id.syncProgressBar);
        tvStatus = findViewById(R.id.tvStatus);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        filePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        fileUri = result.getData().getData();
                        readCSVFile(fileUri);
                    }
                }
        );

        btnSelectFile.setOnClickListener(v -> {
            // ১. সেশন থেকে বর্তমান ইউজারের আইডি নেওয়া
            SharedPreferences prefs = getSharedPreferences("USER_SESSION", MODE_PRIVATE);
            String sessionUserId = prefs.getString("userId", "");

            // ২. নিরাপত্তা চেক: আইডি অবশ্যই IPI-004686 হতে হবে
            if (sessionUserId.equalsIgnoreCase("IPI-004686")) {
                // আইডি মিলেছে, সরাসরি ফাইল পিকার ওপেন হবে (পাসওয়ার্ড ছাড়াই)
                openFilePicker();
            } else {
                // আইডি না মিললে এক্সেস ডিনাইড দেখাবে
                new AlertDialog.Builder(this)
                        .setTitle("Access Denied")
                        .setMessage("দুঃখিত, আপনার আইডি (" + sessionUserId + ") ডাটা আপলোড করার জন্য অনুমোদিত নয়। শুধুমাত্র নির্দিষ্ট অ্যাডমিন এই কাজ করতে পারবেন।")
                        .setPositiveButton("OK", null)
                        .show();
            }
        });

        btnSync.setOnClickListener(v -> startSync());
    }

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        String[] mimetypes = {"text/comma-separated-values", "text/csv", "application/csv"};
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimetypes);
        filePickerLauncher.launch(intent);
    }

    private void readCSVFile(Uri uri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            List<String[]> allRows = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                String[] columns = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
                allRows.add(columns);
            }
            reader.close();
            if (allRows.isEmpty()) return;

            parsedData.clear();
            int headerRowIndex = -1;
            int idIndex = -1, stockIndex = -1, catIndex = -1, nameIndex = -1, packIndex = -1, cartonIndex = -1;

            for (int i = 0; i < Math.min(allRows.size(), 30); i++) {
                String[] row = allRows.get(i);
                for (int j = 0; j < row.length; j++) {
                    String cell = row[j].trim().toLowerCase().replace("\"", "");
                    if (cell.equals("item id") || cell.equals("item_id") || cell.contains("code")) idIndex = j;
                    if (cell.contains("stock") || cell.contains("qty")) stockIndex = j;
                    if (cell.equals("category")) catIndex = j;
                    if (cell.contains("product name") || cell.equals("name")) nameIndex = j;
                    if (cell.contains("pack size") || cell.equals("pack_size")) packIndex = j;
                    if (cell.contains("carton size") || cell.equals("carton_size")) cartonIndex = j;
                }
                if (idIndex != -1 && stockIndex != -1) { headerRowIndex = i; break; }
            }

            if (headerRowIndex == -1) {
                tvStatus.setText("Error: Required CSV Columns not found!");
                return;
            }

            for (int i = headerRowIndex + 1; i < allRows.size(); i++) {
                String[] row = allRows.get(i);
                if (row.length > Math.max(idIndex, stockIndex)) {
                    String id = row[idIndex].trim().replace("\"", "");
                    String stock = row[stockIndex].trim().replace("\"", "");
                    String cat = (catIndex != -1 && row.length > catIndex) ? row[catIndex].trim().replace("\"", "") : "PHARMA";
                    String name = (nameIndex != -1 && row.length > nameIndex) ? row[nameIndex].trim().replace("\"", "") : "";
                    String pack = (packIndex != -1 && row.length > packIndex) ? row[packIndex].trim().replace("\"", "") : "";
                    String carton = (cartonIndex != -1 && row.length > cartonIndex) ? row[cartonIndex].trim().replace("\"", "") : "0";
                    if (!id.isEmpty()) parsedData.add(new CSVRow(id, stock, cat, name, pack, carton));
                }
            }
            tvStatus.setText("File Loaded: " + parsedData.size() + " items found.");
            btnSync.setVisibility(View.VISIBLE);
        } catch (Exception e) {
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void startSync() {
        if (parsedData.isEmpty()) return;
        btnSync.setEnabled(false);
        progressBar.setVisibility(View.VISIBLE);
        tvStatus.setText("Syncing...");

        mDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Map<String, String> codePathMap = new HashMap<>();
                for (DataSnapshot catSnap : snapshot.getChildren()) {
                    for (DataSnapshot prodSnap : catSnap.getChildren()) {
                        String code = prodSnap.child("Code").getValue(String.class);
                        if (code != null) codePathMap.put(code.trim(), catSnap.getKey() + "/" + prodSnap.getKey());
                    }
                }

                Map<String, Object> updates = new HashMap<>();
                List<CSVRow> notFoundList = new ArrayList<>();
                int matched = 0;

                for (CSVRow row : parsedData) {
                    if (codePathMap.containsKey(row.id)) {
                        updates.put(codePathMap.get(row.id) + "/totalQty", row.stock);
                        matched++;
                    } else {
                        notFoundList.add(row);
                    }
                }

                final int finalUpdatedCount = matched;
                if (!updates.isEmpty()) {
                    mDatabase.updateChildren(updates).addOnCompleteListener(task -> {
                        progressBar.setVisibility(View.GONE);
                        btnSync.setEnabled(true);
                        showSyncSummaryDialog(finalUpdatedCount, notFoundList);
                    });
                } else {
                    progressBar.setVisibility(View.GONE);
                    btnSync.setEnabled(true);
                    showSyncSummaryDialog(0, notFoundList);
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {
                progressBar.setVisibility(View.GONE);
                btnSync.setEnabled(true);
            }
        });
    }

    private void showSyncSummaryDialog(int updated, List<CSVRow> notFoundList) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Sync Result Summary");

        LinearLayout mainLayout = new LinearLayout(this);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setPadding(30, 30, 30, 30);

        TextView statsTv = new TextView(this);
        statsTv.setText("✅ Updated: " + updated + "\n⚠️ Not Found: " + notFoundList.size() + "\n");
        statsTv.setTextColor(Color.BLACK);
        statsTv.setTextSize(16);
        statsTv.setTypeface(null, Typeface.BOLD);
        mainLayout.addView(statsTv);

        if (!notFoundList.isEmpty()) {
            TextView hintTv = new TextView(this);
            hintTv.setText("Click an item below to edit and save to Firebase:\n");
            hintTv.setTextColor(Color.parseColor("#2E3192"));
            mainLayout.addView(hintTv);

            for (CSVRow item : notFoundList) {
                LinearLayout itemBox = new LinearLayout(this);
                itemBox.setOrientation(LinearLayout.VERTICAL);
                itemBox.setPadding(20, 25, 20, 25);
                itemBox.setBackgroundResource(android.R.drawable.list_selector_background);
                itemBox.setClickable(true);
                itemBox.setFocusable(true);

                StringBuilder info = new StringBuilder();
                info.append("Category: ").append(item.category).append("\n");
                info.append("Item Id: ").append(item.id).append("\n");
                info.append("Name: ").append(item.name).append("\n");
                info.append("Pack Size: ").append(item.packSize).append("\n");
                info.append("Sales Stock: ").append(item.stock).append("\n");
                info.append("Carton Size: ").append(item.cartonSize);

                TextView infoTv = new TextView(this);
                infoTv.setText(info.toString());
                infoTv.setTextColor(Color.DKGRAY);
                infoTv.setTextSize(13);
                itemBox.addView(infoTv);

                itemBox.setOnClickListener(v -> openEditAndAddDialog(item));
                mainLayout.addView(itemBox);

                View divider = new View(this);
                divider.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1));
                divider.setBackgroundColor(Color.LTGRAY);
                divider.setAlpha(0.6f);
                mainLayout.addView(divider);
            }
        }

        ScrollView scrollView = new ScrollView(this);
        scrollView.addView(mainLayout);
        builder.setView(scrollView);
        builder.setPositiveButton("Close", null);
        builder.show();
    }

    private void openEditAndAddDialog(CSVRow item) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Edit & Save Product");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 30, 50, 30);

        EditText etId = createFormEditText("Item Id (Code)", item.id, false);
        EditText etName = createFormEditText("Product Name", item.name, true);
        EditText etPack = createFormEditText("Pack Size", item.packSize, true);
        EditText etStock = createFormEditText("Sales Stock (Total Qty)", item.stock, true);
        etStock.setInputType(InputType.TYPE_CLASS_NUMBER);
        EditText etCarton = createFormEditText("Carton Size", item.cartonSize, true);
        etCarton.setInputType(InputType.TYPE_CLASS_NUMBER);
        
        TextView labelCat = new TextView(this); labelCat.setText("Category");
        Spinner spCat = new Spinner(this);
        String[] cats = {"PHARMA", "HERBAL", "OPTHALMIC", "IBNSINA"};
        spCat.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, cats));
        for(int i=0; i<cats.length; i++) if(cats[i].equalsIgnoreCase(item.category)) spCat.setSelection(i);

        layout.addView(labelCat); layout.addView(spCat);
        layout.addView(etId); layout.addView(etName);
        layout.addView(etPack); layout.addView(etStock); layout.addView(etCarton);

        builder.setView(layout);
        builder.setPositiveButton("Add to Firebase", (dialog, which) -> {
            String cat = spCat.getSelectedItem().toString();
            String name = etName.getText().toString().trim();
            String code = etId.getText().toString().trim();
            String pack = etPack.getText().toString().trim();
            
            if (name.isEmpty() || code.isEmpty()) {
                Toast.makeText(this, "Name and ID are required!", Toast.LENGTH_SHORT).show();
                return;
            }

            String key = code + "_" + name.toUpperCase().replace(" ", "_") + (pack.isEmpty() ? "" : "_" + pack);
            
            Map<String, Object> data = new HashMap<>();
            data.put("Carton_Size", etCarton.getText().toString());
            data.put("Category", cat);
            data.put("Code", code);
            data.put("Pack_Size", pack);
            data.put("Product_Name", name);
            data.put("totalQty", etStock.getText().toString());
            data.put("status", "Unchecked");
            data.put("remark", "");
            data.put("shortQty", "");
            data.put("excessQty", "");

            mDatabase.child(cat).child(key).setValue(data).addOnSuccessListener(aVoid -> 
                Toast.makeText(this, "Product Saved Successfully!", Toast.LENGTH_SHORT).show());
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private EditText createFormEditText(String label, String value, boolean enabled) {
        EditText et = new EditText(this);
        et.setHint(label); et.setText(value); et.setEnabled(enabled);
        et.setPadding(10, 25, 10, 25); et.setTextSize(14);
        return et;
    }

    private static class CSVRow {
        String id, stock, category, name, packSize, cartonSize;
        CSVRow(String id, String stock, String category, String name, String packSize, String cartonSize) { 
            this.id = id; this.stock = stock; this.category = category; this.name = name; this.packSize = packSize; this.cartonSize = cartonSize;
        }
    }
}