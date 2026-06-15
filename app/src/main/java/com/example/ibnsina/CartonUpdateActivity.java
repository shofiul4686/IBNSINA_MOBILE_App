package com.example.ibnsina;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CartonUpdateActivity extends AppCompatActivity {

    private Spinner spinnerCategory;
    private RecyclerView recyclerView;
    private CartonUpdateAdapter adapter;
    private EditText etSearch;
    private List<InventoryModel> fullList = new ArrayList<>();
    private List<InventoryModel> filteredList = new ArrayList<>();
    private DatabaseReference mDatabase;
    private ValueEventListener mListener;
    private String selectedCategory = "PHARMA";
    private String searchQuery = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_carton_update);

        // UI Components Initialize
        etSearch = findViewById(R.id.etSearch);
        spinnerCategory = findViewById(R.id.spinnerCategory);
        recyclerView = findViewById(R.id.recyclerViewCarton);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // ব্যাক বাটন
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // Stock Update বাটন - এটি MainActivity ওপেন করবে এবং বর্তমান ক্যাটাগরি পাঠাবে
        findViewById(R.id.btnStockUpdate).setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivity.class);
            intent.putExtra("SELECTED_CATEGORY", selectedCategory);
            startActivity(intent);
        });

        // Intent থেকে ক্যাটাগরি গ্রহণ করা
        String intentCategory = getIntent().getStringExtra("SELECTED_CATEGORY");
        if (intentCategory != null && !intentCategory.isEmpty()) {
            selectedCategory = intentCategory;
        }

        mDatabase = FirebaseDatabase.getInstance().getReference("PRODUCTS");

        final String[] categories = {"PHARMA", "INM", "IBNSINA", "HERBAL", "OPTHALMIC"};
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, categories);
        spinnerCategory.setAdapter(spinnerAdapter);

        // স্পিনারে অটোমেটিক সঠিক ক্যাটাগরি সিলেক্ট করা
        for (int i = 0; i < categories.length; i++) {
            if (categories[i].equalsIgnoreCase(selectedCategory)) {
                spinnerCategory.setSelection(i);
                break;
            }
        }

        spinnerCategory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedCategory = categories[position];
                filterData();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // সার্চ বার লজিক
        if (etSearch != null) {
            etSearch.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    searchQuery = s.toString().toLowerCase().trim();
                    filterData();
                }
                @Override public void afterTextChanged(Editable s) {}
            });
        }

        fetchData();
    }

    private void fetchData() {
        mListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                fullList.clear();
                if (snapshot.exists()) {
                    for (DataSnapshot catSnap : snapshot.getChildren()) {
                        String catName = catSnap.getKey();
                        for (DataSnapshot prodSnap : catSnap.getChildren()) {
                            try {
                                InventoryModel item = prodSnap.getValue(InventoryModel.class);
                                if (item != null) {
                                    item.setFirebaseKey(prodSnap.getKey());
                                    if (item.getCategory() == null || item.getCategory().isEmpty()) {
                                        item.setCategory(catName);
                                    }
                                    fullList.add(item);
                                }
                            } catch (Exception e) {
                                Log.e("Firebase", "Error: " + e.getMessage());
                            }
                        }
                    }
                }
                filterData();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(CartonUpdateActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        };
        mDatabase.addValueEventListener(mListener);
    }

    private void filterData() {
        filteredList.clear();
        for (InventoryModel item : fullList) {
            // ক্যাটাগরি ফিল্টার
            boolean matchesCategory = item.getCategory() != null && item.getCategory().equalsIgnoreCase(selectedCategory);
            
            // সার্চ ফিল্টার
            boolean matchesSearch = searchQuery.isEmpty() || 
                    (item.getProduct_Name() != null && item.getProduct_Name().toLowerCase().contains(searchQuery)) ||
                    (item.getCode() != null && item.getCode().toLowerCase().contains(searchQuery));

            if (matchesCategory && matchesSearch) {
                filteredList.add(item);
            }
        }

        Collections.sort(filteredList, (o1, o2) -> {
            String n1 = o1.getProduct_Name() != null ? o1.getProduct_Name() : "";
            String n2 = o2.getProduct_Name() != null ? o2.getProduct_Name() : "";
            return n1.compareToIgnoreCase(n2);
        });

        if (adapter == null) {
            adapter = new CartonUpdateAdapter(new ArrayList<>(filteredList));
            recyclerView.setAdapter(adapter);
        } else {
            adapter.updateList(new ArrayList<>(filteredList));
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mDatabase != null && mListener != null) {
            mDatabase.removeEventListener(mListener);
        }
    }
}