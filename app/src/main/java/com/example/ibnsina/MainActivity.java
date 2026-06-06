package com.example.ibnsina;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
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
    private final int PAGE_SIZE = 30;
    private boolean isPagingEnabled = false; 
    private TextView tvPageInfo, tvCheckedCount;
    private MaterialButton btnNextPage, btnPrevPage;

    private int preSearchPage = 0;
    private boolean preSearchPagingState = false;
    private boolean loadingState = false;

    private DatabaseReference mDatabase;

    private String currentInput = "";
    private String currentExpressionText = "";
    private double calcResult = 0;
    private char lastOperator = ' ';

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

        String[] options = {"All", "Checked", "Unchecked", "In Stock", "Stock Out", "PHARMA", "OPTHALMIC", "HERBAL"};
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, options);
        if (spinnerFilter != null) spinnerFilter.setAdapter(spinnerAdapter);

        fetchDataFromFirebase(true);

        if (swipeRefreshLayout != null)
            swipeRefreshLayout.setOnRefreshListener(() -> { isPagingEnabled = false; fetchDataFromFirebase(true); });

        if (btnRefreshManual != null)
            btnRefreshManual.setOnClickListener(v -> { isPagingEnabled = false; fetchDataFromFirebase(true); });

        if (btnPrint != null) btnPrint.setOnClickListener(v -> createWebPrintJob());
        if (btnCalculator != null) btnCalculator.setOnClickListener(v -> showCalculatorDialog());

        if (btnNextPage != null) btnNextPage.setOnClickListener(v -> { isPagingEnabled = true; currentPage++; updateRecyclerView(true); });
        if (btnPrevPage != null) btnPrevPage.setOnClickListener(v -> { if (currentPage > 0) { isPagingEnabled = true; currentPage--; updateRecyclerView(true); } });

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
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.WHITE);
                dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.WHITE);
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setBackgroundColor(Color.parseColor("#4CAF50"));
                dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setBackgroundColor(Color.parseColor("#F44336"));
            });
        }

        View btnBack = findViewById(R.id.btnBottomBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());
    }

    private void fetchDataFromFirebase(boolean showProgress) {
        if (showProgress) setLoading(true);
        
        mDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                fullInventoryList.clear();
                if (snapshot.exists()) {
                    for (DataSnapshot categorySnapshot : snapshot.getChildren()) {
                        String categoryName = categorySnapshot.getKey();
                        for (DataSnapshot productSnapshot : categorySnapshot.getChildren()) {
                            try {
                                InventoryModel item = productSnapshot.getValue(InventoryModel.class);
                                if (item != null) {
                                    item.setFirebaseKey(productSnapshot.getKey());
                                    if (item.getCategory() == null || item.getCategory().isEmpty()) {
                                        item.setCategory(categoryName);
                                    }
                                    fullInventoryList.add(item);
                                }
                            } catch (Exception e) {
                                Log.e("FirebaseData", "Error: " + e.getMessage());
                            }
                        }
                    }
                }

                Collections.sort(fullInventoryList, (o1, o2) -> {
                    int p1 = getCategoryPriority(o1.getCategory());
                    int p2 = getCategoryPriority(o2.getCategory());
                    if (p1 != p2) return Integer.compare(p1, p2);
                    String name1 = o1.getProduct_Name() != null ? o1.getProduct_Name() : "";
                    String name2 = o2.getProduct_Name() != null ? o2.getProduct_Name() : "";
                    return name1.compareToIgnoreCase(name2);
                });

                setLoading(false);
                if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
                updateCheckedCount();
                applyFilterAndSearch(false);
            }

            @Override public void onCancelled(@NonNull DatabaseError error) { setLoading(false); }
        });
    }

    private int getCategoryPriority(String category) {
        if (category == null) return 4;
        String cat = category.trim().toUpperCase();
        if (cat.equals("PHARMA")) return 1;
        if (cat.equals("OPTHALMIC")) return 2;
        if (cat.equals("HERBAL")) return 3;
        return 4;
    }

    private void applyFilterAndSearch(boolean shouldScrollToTop) {
        if (fullInventoryList == null) return;
        String query = etSearch != null ? etSearch.getText().toString().toLowerCase().trim() : "";
        
        filteredList.clear();
        for (InventoryModel item : fullInventoryList) {
            if (item == null) continue;
            String pName = item.getProduct_Name() != null ? item.getProduct_Name().toLowerCase() : "";
            String pCode = item.getCode() != null ? item.getCode().toLowerCase() : "";
            boolean matchesSearch = pName.contains(query) || pCode.contains(query);
            boolean matchesFilter = false;
            
            double stockCount = 0; 
            try { 
                String tQty = item.getTotalQty();
                if (tQty != null && !tQty.isEmpty() && !tQty.equalsIgnoreCase("null")) {
                    stockCount = Double.parseDouble(tQty.trim());
                }
            } catch (Exception e) {}
            
            String status = item.getStatus() != null ? item.getStatus() : "Unchecked";
            String category = item.getCategory() != null ? item.getCategory().trim().toUpperCase() : "";

            if (selectedFilter.equals("All")) matchesFilter = true;
            else if (selectedFilter.equals("Checked")) matchesFilter = status.equalsIgnoreCase("Checked");
            else if (selectedFilter.equals("Unchecked")) matchesFilter = !status.equalsIgnoreCase("Checked");
            else if (selectedFilter.equals("In Stock")) matchesFilter = stockCount > 0;
            else if (selectedFilter.equals("Stock Out")) matchesFilter = stockCount <= 0;
            else matchesFilter = category.equalsIgnoreCase(selectedFilter.trim().toUpperCase());
            
            if (matchesSearch && matchesFilter) filteredList.add(item);
        }

        String currentCatName = "";
        int catSl = 0;
        for (InventoryModel item : filteredList) {
            String itemCat = (item.getCategory() != null) ? item.getCategory().trim().toUpperCase() : "OTHER";
            if (!itemCat.equalsIgnoreCase(currentCatName)) {
                currentCatName = itemCat;
                catSl = 1;
            } else {
                catSl++;
            }
            item.setSl(String.valueOf(catSl));
        }

        if (!query.isEmpty()) currentPage = 0;
        updateRecyclerView(shouldScrollToTop);
    }

    private void updateRecyclerView(boolean shouldScrollToTop) {
        if (filteredList == null) return;
        List<InventoryModel> displayList;
        int totalItems = filteredList.size();

        if (!isPagingEnabled) {
            displayList = new ArrayList<>(filteredList);
            if (tvPageInfo != null) tvPageInfo.setText("All Items (" + totalItems + ")");
            if (btnPrevPage != null) btnPrevPage.setEnabled(false);
            if (btnNextPage != null) btnNextPage.setEnabled(totalItems > PAGE_SIZE);
        } else {
            int start = currentPage * PAGE_SIZE;
            int end = Math.min(start + PAGE_SIZE, totalItems);
            displayList = (start < totalItems) ? new ArrayList<>(filteredList.subList(start, end)) : new ArrayList<>();
            if (tvPageInfo != null) {
                int totalPages = (int) Math.ceil((double) totalItems / PAGE_SIZE);
                tvPageInfo.setText("Page " + (currentPage + 1) + " of " + totalPages);
            }
            if (btnPrevPage != null) btnPrevPage.setEnabled(currentPage > 0);
            if (btnNextPage != null) btnNextPage.setEnabled(end < totalItems);
        }

        if (recyclerView != null) {
            if (adapter == null) {
                adapter = new InventoryAdapter(displayList);
                recyclerView.setAdapter(adapter);
            } else {
                adapter.updateList(displayList);
            }
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
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.requestFeature(Window.FEATURE_NO_TITLE);
            window.setFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
            window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            WindowManager.LayoutParams wlp = window.getAttributes();
            wlp.gravity = Gravity.TOP;
            wlp.y = 80; 
            window.setAttributes(wlp);
        }
        dialog.show();
        new Handler().postDelayed(dialog::dismiss, 1000);
    }

    private void showGoToPageDialog() {
        int totalPages = (int) Math.ceil((double) filteredList.size() / PAGE_SIZE);
        if (totalPages <= 1 && isPagingEnabled) return;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Go to Page (1 - " + totalPages + ")");
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        builder.setView(input);
        builder.setPositiveButton("Go", (d, w) -> {
            String val = input.getText().toString();
            if (!val.isEmpty()) {
                int pageNum = Integer.parseInt(val);
                if (pageNum >= 1 && pageNum <= totalPages) { isPagingEnabled = true; currentPage = pageNum - 1; updateRecyclerView(true); }
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
                            // স্ট্যাটাস এবং সব ইনপুট ফিল্ড রিসেট করা হচ্ছে
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
            }
            @Override public void onCancelled(@NonNull DatabaseError error) { setLoading(false); }
        });
    }

    private void createWebPrintJob() {
        WebView wv = new WebView(this);
        wv.setWebViewClient(new WebViewClient() { @Override public void onPageFinished(WebView view, String url) { PrintManager pm = (PrintManager) getSystemService(Context.PRINT_SERVICE); pm.print("Inventory Report", view.createPrintDocumentAdapter("Inventory Report"), new PrintAttributes.Builder().build()); } });
        StringBuilder h = new StringBuilder("<html><head><style>body { font-family: serif; } table { width: 100%; border-collapse: collapse; font-size: 10px; } th, td { border: 1px solid #ccc; padding: 5px; text-align: left; } th { background: #eee; }</style></head><body>");
        h.append("<h2 style='text-align:center'>The IBN SINA Pharmaceutical Industry PLC</h2><p>Date: ").append(new SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault()).format(new Date())).append("</p><table><thead><tr><th>Code</th><th>Product Name</th><th>Stock</th><th>Short</th><th>Excess</th><th>Remark</th></tr></thead><tbody>");
        for (InventoryModel item : filteredList) h.append("<tr><td>").append(item.getCode()).append("</td><td>").append(item.getProduct_Name()).append("</td><td>").append(item.getTotalQty()).append("</td><td>").append(item.getShortQty()).append("</td><td>").append(item.getExcessQty()).append("</td><td>").append(item.getRemark()).append("</td></tr>");
        h.append("</tbody></table></body></html>");
        wv.loadDataWithBaseURL(null, h.toString(), "text/HTML", "UTF-8", null);
    }

    private void showCalculatorDialog() {
        AlertDialog.Builder b = new AlertDialog.Builder(this);
        View v = LayoutInflater.from(this).inflate(R.layout.dialog_calculator, null);
        b.setView(v);
        final AlertDialog d = b.create();
        final TextView tvDisp = v.findViewById(R.id.tvCalcDisplay);
        final TextView tvExpr = v.findViewById(R.id.tvCalcExpression);
        tvDisp.setText(currentInput.isEmpty() ? "0" : currentInput);
        tvExpr.setText(currentExpressionText);
        View.OnClickListener numL = view -> { Button btn = (Button) view; if (currentInput.equals("0")) currentInput = ""; currentInput += btn.getText().toString(); tvDisp.setText(currentInput); };
        int[] ids = {R.id.btn0, R.id.btn1, R.id.btn2, R.id.btn3, R.id.btn4, R.id.btn5, R.id.btn6, R.id.btn7, R.id.btn8, R.id.btn9, R.id.btnDot};
        for(int id : ids) v.findViewById(id).setOnClickListener(numL);
        v.findViewById(R.id.btnAdd).setOnClickListener(view -> handleOp(tvDisp, tvExpr, '+'));
        v.findViewById(R.id.btnSub).setOnClickListener(view -> handleOp(tvDisp, tvExpr, '-'));
        v.findViewById(R.id.btnMul).setOnClickListener(view -> handleOp(tvDisp, tvExpr, '*'));
        v.findViewById(R.id.btnDiv).setOnClickListener(view -> handleOp(tvDisp, tvExpr, '/'));
        v.findViewById(R.id.btnBack).setOnClickListener(view -> { if (!currentInput.isEmpty()) { currentInput = currentInput.substring(0, currentInput.length() - 1); tvDisp.setText(currentInput.isEmpty() ? "0" : currentInput); } });
        v.findViewById(R.id.btnEqual).setOnClickListener(view -> { if (!currentInput.isEmpty() && lastOperator != ' ') { calcFinal(); currentExpressionText += currentInput + " ="; tvExpr.setText(currentExpressionText); currentInput = fmtRes(calcResult); tvDisp.setText(currentInput); lastOperator = ' '; } });
        v.findViewById(R.id.btnClear).setOnClickListener(view -> { currentInput = ""; calcResult = 0; lastOperator = ' '; currentExpressionText = ""; tvDisp.setText("0"); tvExpr.setText(""); });
        v.findViewById(R.id.btnCloseCalc).setOnClickListener(view -> d.dismiss());
        d.show();
    }

    private void handleOp(TextView d, TextView e, char op) { if (!currentInput.isEmpty()) { if (lastOperator == ' ') calcResult = Double.parseDouble(currentInput); else calcFinal(); lastOperator = op; currentExpressionText = fmtRes(calcResult) + " " + op + " "; e.setText(currentExpressionText); currentInput = ""; d.setText("0"); } }
    private void calcFinal() { double cur = Double.parseDouble(currentInput); switch (lastOperator) { case '+': calcResult += cur; break; case '-': calcResult -= cur; break; case '*': calcResult *= cur; break; case '/': if (cur != 0) calcResult /= cur; break; } }
    private String fmtRes(double d) { return (d == (long) d) ? String.format("%d", (long) d) : String.format("%s", d); }
    public void setLoading(boolean l) { this.loadingState = l; if (progressBar != null) progressBar.setVisibility(l ? View.VISIBLE : View.GONE); }
    public boolean isLoading() { return loadingState; }
}
