package com.example.ibnsina;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.print.PrintAttributes;
import android.print.PrintManager;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private InventoryAdapter adapter;
    private List<InventoryModel> fullInventoryList = new ArrayList<>();
    private List<InventoryModel> filteredList = new ArrayList<>();
    private EditText etSearch;
    private Spinner spinnerFilter;
    private String selectedFilter = "All";
    private MaterialButton btnResetAll, btnRefreshManual, btnPrint, btnCalculator;
    private ProgressBar progressBar;
    private SwipeRefreshLayout swipeRefreshLayout;

    private int currentPage = 0;
    private final int PAGE_SIZE = 54; 
    private boolean isPagingEnabled = true; 
    private TextView tvPageInfo, tvCheckedCount;
    private MaterialButton btnNextPage, btnPrevPage;

    private boolean loadingState = false;
    private DatabaseReference mDatabase;

    // Calculator Variables
    private String currentInput = "";
    private double firstOperand = Double.NaN;
    private String pendingOperator = "";
    private TextView tvCalcResult, tvCalcExpression;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mDatabase = FirebaseDatabase.getInstance().getReference("PRODUCTS");

        recyclerView = findViewById(R.id.recyclerView);
        etSearch = findViewById(R.id.etSearch);
        spinnerFilter = findViewById(R.id.spinnerFilter);
        btnResetAll = findViewById(R.id.btnResetAll);
        btnRefreshManual = findViewById(R.id.btnRefreshManual);
        btnPrint = findViewById(R.id.btnPrint); 
        btnCalculator = findViewById(R.id.btnCalculator);
        progressBar = findViewById(R.id.progressBar);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);

        tvPageInfo = findViewById(R.id.tvPageInfo);
        tvCheckedCount = findViewById(R.id.tvCheckedCount);
        btnNextPage = findViewById(R.id.btnNextPage);
        btnPrevPage = findViewById(R.id.btnPrevPage);

        if (recyclerView != null) {
            recyclerView.setLayoutManager(new LinearLayoutManager(this));
            recyclerView.setHasFixedSize(true);
        }

        final String[] options = {"All", "Checked", "Unchecked", "In Stock", "Stock Out", "PHARMA", "OPTHALMIC", "HERBAL"};
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, options);
        if (spinnerFilter != null) {
            spinnerFilter.setAdapter(spinnerAdapter);
            String intentCategory = getIntent().getStringExtra("SELECTED_CATEGORY");
            if (intentCategory != null && !intentCategory.isEmpty()) {
                selectedFilter = intentCategory;
                for (int i = 0; i < options.length; i++) {
                    if (options[i].equalsIgnoreCase(selectedFilter)) {
                        spinnerFilter.setSelection(i);
                        break;
                    }
                }
            }
        }

        fetchDataFromFirebase(true);

        if (swipeRefreshLayout != null)
            swipeRefreshLayout.setOnRefreshListener(() -> { fetchDataFromFirebase(true); });

        if (btnRefreshManual != null)
            btnRefreshManual.setOnClickListener(v -> { fetchDataFromFirebase(true); });

        if (btnPrint != null) btnPrint.setOnClickListener(v -> createWebPrintJob());
        if (btnCalculator != null) btnCalculator.setOnClickListener(v -> showCalculatorDialog());

        if (btnNextPage != null) btnNextPage.setOnClickListener(v -> { currentPage++; updateRecyclerView(true); });
        if (btnPrevPage != null) btnPrevPage.setOnClickListener(v -> { if (currentPage > 0) { currentPage--; updateRecyclerView(true); } });

        if (tvPageInfo != null) tvPageInfo.setOnClickListener(v -> showGoToPageDialog());

        if (etSearch != null) {
            etSearch.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) { applyFilterAndSearch(true); }
                @Override public void afterTextChanged(Editable s) {}
            });
        }

        if (spinnerFilter != null) {
            spinnerFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    selectedFilter = options[position];
                    applyFilterAndSearch(true);
                }
                @Override public void onNothingSelected(AdapterView<?> parent) {}
            });
        }

        if (btnResetAll != null) {
            btnResetAll.setOnClickListener(v -> {
                AlertDialog dialog = new AlertDialog.Builder(this).setTitle("Reset All?").setMessage("আপনি কি সব ইনপুট এবং চেক মার্ক মুছে ফেলতে চান?")
                        .setPositiveButton("Yes", (dialogInterface, which) -> resetAllStatusOnFirebase()).setNegativeButton("No", null).show();
            });
        }

        View btnBack = findViewById(R.id.btnBottomBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());
    }

    private void fetchDataFromFirebase(boolean showProgress) {
        if (showProgress) setLoading(true);
        mDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                fullInventoryList.clear();
                if (snapshot.exists()) {
                    for (DataSnapshot catSnap : snapshot.getChildren()) {
                        String catName = catSnap.getKey();
                        for (DataSnapshot prodSnap : catSnap.getChildren()) {
                            try {
                                InventoryModel item = prodSnap.getValue(InventoryModel.class);
                                if (item != null) {
                                    item.setFirebaseKey(prodSnap.getKey());
                                    if (item.getCategory() == null || item.getCategory().isEmpty()) item.setCategory(catName);
                                    fullInventoryList.add(item);
                                }
                            } catch (Exception e) { Log.e("FirebaseData", "Error: " + e.getMessage()); }
                        }
                    }
                }
                Collections.sort(fullInventoryList, (o1, o2) -> (o1.getProduct_Name() != null ? o1.getProduct_Name() : "").compareToIgnoreCase(o2.getProduct_Name() != null ? o2.getProduct_Name() : ""));
                
                setLoading(false);
                if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
                updateCheckedCount();
                applyFilterAndSearch(false);
            }
            @Override public void onCancelled(@NonNull DatabaseError error) { setLoading(false); }
        });
    }

    private void applyFilterAndSearch(boolean shouldScrollToTop) {
        if (fullInventoryList == null) return;
        String query = etSearch != null ? etSearch.getText().toString().toLowerCase().trim() : "";
        filteredList.clear();
        for (InventoryModel item : fullInventoryList) {
            String pName = item.getProduct_Name() != null ? item.getProduct_Name().toLowerCase() : "";
            String pCode = item.getCode() != null ? item.getCode().toLowerCase() : "";
            boolean matchesSearch = pName.contains(query) || pCode.contains(query);
            boolean matchesFilter = false;
            String status = item.getStatus() != null ? item.getStatus() : "Unchecked";
            String category = item.getCategory() != null ? item.getCategory().trim().toUpperCase() : "";

            if (selectedFilter.equals("All")) matchesFilter = true;
            else if (selectedFilter.equals("Checked")) matchesFilter = status.equalsIgnoreCase("Checked");
            else if (selectedFilter.equals("Unchecked")) matchesFilter = !status.equalsIgnoreCase("Checked");
            else matchesFilter = category.equalsIgnoreCase(selectedFilter.trim().toUpperCase());
            
            if (matchesSearch && matchesFilter) filteredList.add(item);
        }

        for (int i = 0; i < filteredList.size(); i++) {
            filteredList.get(i).setSl(String.valueOf(i + 1));
        }

        currentPage = 0;
        updateRecyclerView(shouldScrollToTop);
    }

    private void updateRecyclerView(boolean shouldScrollToTop) {
        if (filteredList == null) return;
        List<InventoryModel> displayList;
        int totalItems = filteredList.size();
        int start = currentPage * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, totalItems);
        displayList = (start < totalItems) ? new ArrayList<>(filteredList.subList(start, end)) : new ArrayList<>();
        
        if (tvPageInfo != null) {
            int totalPages = (int) Math.ceil((double) totalItems / PAGE_SIZE);
            tvPageInfo.setText("Page " + (currentPage + 1) + " of " + (totalPages == 0 ? 1 : totalPages));
        }
        if (btnPrevPage != null) btnPrevPage.setEnabled(currentPage > 0);
        if (btnNextPage != null) btnNextPage.setEnabled(end < totalItems);

        if (recyclerView != null) {
            if (adapter == null) {
                adapter = new InventoryAdapter(displayList);
                recyclerView.setAdapter(adapter);
            } else adapter.updateList(displayList);
            if (shouldScrollToTop) recyclerView.scrollToPosition(0);
        }
    }

    public void updateCheckedCount() {
        int count = 0;
        for (InventoryModel item : fullInventoryList) if (item != null && "Checked".equalsIgnoreCase(item.getStatus())) count++;
        if (tvCheckedCount != null) tvCheckedCount.setText("Checked: " + count);
    }

    public void showBigSuccessDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_success, null);
        builder.setView(dialogView);
        final AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            WindowManager.LayoutParams wlp = dialog.getWindow().getAttributes();
            wlp.gravity = Gravity.TOP;
            wlp.y = 100;
            dialog.getWindow().setAttributes(wlp);
        }
        dialog.show();
        new Handler(Looper.getMainLooper()).postDelayed(dialog::dismiss, 600);
    }

    private void showGoToPageDialog() {
        int totalPages = (int) Math.ceil((double) filteredList.size() / PAGE_SIZE);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Go to Page (1 - " + (totalPages == 0 ? 1 : totalPages) + ")");
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        builder.setView(input);
        builder.setPositiveButton("Go", (d, w) -> {
            String val = input.getText().toString();
            if (!val.isEmpty()) {
                int pageNum = Integer.parseInt(val);
                if (pageNum >= 1 && pageNum <= totalPages) { currentPage = pageNum - 1; updateRecyclerView(true); }
            }
        }).setNegativeButton("Cancel", null);
        builder.show();
    }

    private void resetAllStatusOnFirebase() {
        setLoading(true);
        mDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    for (DataSnapshot catSnap : snapshot.getChildren()) {
                        for (DataSnapshot prodSnap : catSnap.getChildren()) {
                            Map<String, Object> updates = new HashMap<>();
                            updates.put("status", "Unchecked");
                            updates.put("shortQty", "");
                            updates.put("excessQty", "");
                            updates.put("remark", "");
                            prodSnap.getRef().updateChildren(updates);
                        }
                    }
                }
                setLoading(false);
                showBigSuccessDialog();
                fetchDataFromFirebase(false);
            }
            @Override public void onCancelled(@NonNull DatabaseError error) { setLoading(false); }
        });
    }

    private void createWebPrintJob() {
        WebView wv = new WebView(this);
        wv.setWebViewClient(new WebViewClient() { @Override public void onPageFinished(WebView view, String url) { PrintManager pm = (PrintManager) getSystemService(Context.PRINT_SERVICE); pm.print("Inventory Report", view.createPrintDocumentAdapter("Inventory Report"), new PrintAttributes.Builder().build()); } });
        StringBuilder h = new StringBuilder();
        h.append("<html><head><style>body { font-family: sans-serif; padding: 15px; } table { width: 100%; border-collapse: collapse; margin-top: 15px; } th, td { border: 1px solid #ddd; padding: 8px; text-align: left; font-size: 11px; } th { background-color: #2E3192; color: white; } .header { text-align: center; color: #2E3192; border-bottom: 2px solid #ED1C24; padding-bottom: 10px; }</style></head><body><div class='header'><h2>The IBN SINA Pharmaceutical Industry PLC</h2><h3>DINAJPUR DEPOT</h3><p>Date: ").append(new SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault()).format(new Date())).append("</p><p>Category: ").append(selectedFilter).append("</p></div>");
        h.append("<table><thead><tr><th>Sl</th><th>Code</th><th>Product Name</th><th>Stock</th><th>Short</th><th>Excess</th><th>Remark</th></tr></thead><tbody>");
        for (InventoryModel item : filteredList) h.append("<tr><td>").append(item.getSl()).append("</td><td>").append(item.getCode()).append("</td><td>").append(item.getProduct_Name()).append("</td><td>").append(item.getTotalQty()).append("</td><td>").append(item.getShortQty()).append("</td><td>").append(item.getExcessQty()).append("</td><td>").append(item.getRemark()).append("</td></tr>");
        h.append("</tbody></table></body></html>");
        wv.loadDataWithBaseURL(null, h.toString(), "text/HTML", "UTF-8", null);
    }

    private void showCalculatorDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View v = getLayoutInflater().inflate(R.layout.dialog_calculator, null);
        builder.setView(v);
        
        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        tvCalcResult = v.findViewById(R.id.tvCalcResult);
        tvCalcExpression = v.findViewById(R.id.tvCalcExpression);
        Button btnClose = v.findViewById(R.id.btnCalcClose);
        
        currentInput = "";
        firstOperand = Double.NaN;
        pendingOperator = "";
        
        btnClose.setOnClickListener(view -> dialog.dismiss());

        // সংশোধিত কোড: সরাসরি আইডি দিয়ে GridLayout খুঁজে নেওয়া
        GridLayout grid = v.findViewById(R.id.gridCalculator);

        if (grid != null) {
            for (int i = 0; i < grid.getChildCount(); i++) {
                View child = grid.getChildAt(i);
                if (child instanceof Button) {
                    Button b = (Button) child;
                    b.setOnClickListener(view -> onCalcButtonClick(b.getText().toString()));
                }
            }
        }
        dialog.show();
    }

    private void onCalcButtonClick(String text) {
        switch (text) {
            case "C":
                currentInput = "";
                firstOperand = Double.NaN;
                pendingOperator = "";
                tvCalcExpression.setText("");
                tvCalcResult.setText("0");
                break;
            case "DEL":
                if (currentInput.length() > 0) {
                    currentInput = currentInput.substring(0, currentInput.length() - 1);
                    tvCalcResult.setText(currentInput.isEmpty() ? "0" : currentInput);
                }
                break;
            case "=":
                compute();
                pendingOperator = "";
                break;
            case "+": case "-": case "*": case "/": case "%":
                compute();
                pendingOperator = text;
                tvCalcExpression.setText(formatResult(firstOperand) + " " + text);
                currentInput = "";
                break;
            default: // Numbers and dot
                currentInput += text;
                tvCalcResult.setText(currentInput);
                break;
        }
    }

    private void compute() {
        if (!currentInput.isEmpty()) {
            double secondOperand = Double.parseDouble(currentInput);
            if (Double.isNaN(firstOperand)) {
                firstOperand = secondOperand;
            } else {
                switch (pendingOperator) {
                    case "+": firstOperand += secondOperand; break;
                    case "-": firstOperand -= secondOperand; break;
                    case "*": firstOperand *= secondOperand; break;
                    case "/": firstOperand /= secondOperand; break;
                    case "%": firstOperand %= secondOperand; break;
                }
            }
            tvCalcResult.setText(formatResult(firstOperand));
            currentInput = "";
        }
    }

    private String formatResult(double d) {
        if (Double.isNaN(d)) return "0";
        if (d == (long) d) return String.format(Locale.US, "%d", (long) d);
        return new DecimalFormat("0.######").format(d);
    }

    public void setLoading(boolean l) { if (progressBar != null) progressBar.setVisibility(l ? View.VISIBLE : View.GONE); }
}
