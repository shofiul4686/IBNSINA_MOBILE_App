package com.example.ibnsina;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InventoryAdapter extends RecyclerView.Adapter<InventoryAdapter.ViewHolder> {
    private List<InventoryModel> list;
    private Context context;
    private long lastClickTime = 0;
    private DatabaseReference mDatabase;

    public InventoryAdapter(List<InventoryModel> list) {
        this.list = list;
        this.mDatabase = FirebaseDatabase.getInstance().getReference("PRODUCTS");
    }

    public void updateList(List<InventoryModel> newList) {
        this.list = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        View view = LayoutInflater.from(context).inflate(R.layout.item_inventory, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        InventoryModel model = list.get(position);

        if (holder.tvSl != null) holder.tvSl.setText(model.getSl()); 
        if (holder.tvCategory != null) holder.tvCategory.setText(model.getCategory());
        if (holder.tvProductName != null) holder.tvProductName.setText(model.getProduct_Name());
        if (holder.tvCode != null) holder.tvCode.setText(model.getCode());
        if (holder.tvPackSize != null) holder.tvPackSize.setText("Pack size: " + model.getPack_Size());
        
        updateCalculatedFields(holder, model);

        holder.etShortQty.setText(model.getShortQty());
        holder.etExcessQty.setText(model.getExcessQty());
        holder.etRemark.setText(model.getRemark());

        if ("Checked".equalsIgnoreCase(model.getStatus())) {
            holder.itemContainer.setBackgroundColor(Color.parseColor("#C8E6C9"));
            holder.btnCheckUpdate.setChecked(true);
        } else {
            holder.itemContainer.setBackgroundColor(Color.WHITE);
            holder.btnCheckUpdate.setChecked(false);
        }

        holder.tvCartonSize.setOnClickListener(v -> {
            long clickTime = System.currentTimeMillis();
            if (clickTime - lastClickTime < 350) {
                showEditCartonSizeDialog(model, holder);
            }
            lastClickTime = clickTime;
        });

        holder.btnCheckUpdate.setOnClickListener(v -> {
            // Tick দেওয়ার সময় অন্য টিক দেওয়া যাতে বন্ধ না হয়, তাই isLoading চেকটি সরিয়ে দেওয়া হলো
            if (holder.btnCheckUpdate.isChecked()) {
                model.setStatus("Checked");
                holder.itemContainer.setBackgroundColor(Color.parseColor("#C8E6C9"));
                
                model.setShortQty(holder.etShortQty.getText().toString());
                model.setExcessQty(holder.etExcessQty.getText().toString());
                model.setRemark(holder.etRemark.getText().toString());

                updateFirebaseData(model);
                if (context instanceof MainActivity) ((MainActivity) context).updateCheckedCount();
            } else {
                holder.btnCheckUpdate.setChecked(true);
                Toast.makeText(context, "Long press to uncheck", Toast.LENGTH_SHORT).show();
            }
        });

        holder.itemContainer.setOnLongClickListener(v -> {
            if ("Checked".equalsIgnoreCase(model.getStatus())) {
                AlertDialog dialog = new AlertDialog.Builder(context)
                        .setTitle("Uncheck Item?").setMessage("Do you want to uncheck this item?")
                        .setPositiveButton("Yes", (di, w) -> {
                            model.setStatus("Unchecked");
                            holder.btnCheckUpdate.setChecked(false);
                            holder.itemContainer.setBackgroundColor(Color.WHITE);
                            
                            model.setShortQty("");
                            model.setExcessQty("");
                            model.setRemark("");
                            
                            updateFirebaseData(model);
                            if (context instanceof MainActivity) ((MainActivity) context).updateCheckedCount();
                        }).setNegativeButton("No", null).show();
                
                dialog.show();
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.WHITE);
                dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.WHITE);
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setBackgroundColor(Color.parseColor("#4CAF50"));
                dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setBackgroundColor(Color.parseColor("#F44336"));
                return true;
            }
            return false;
        });
    }

    private void updateCalculatedFields(ViewHolder holder, InventoryModel model) {
        try {
            if (model == null || holder == null) return;

            double totalStock = 0;
            String tQty = model.getTotalQty();
            if (tQty != null && !tQty.isEmpty() && !tQty.equalsIgnoreCase("null")) {
                totalStock = Double.parseDouble(tQty.trim());
            }
            
            double cartonSize = 0;
            String cSize = model.getCarton_Size();
            if (cSize != null && !cSize.isEmpty() && !cSize.equalsIgnoreCase("null")) {
                cartonSize = Double.parseDouble(cSize.trim());
            }

            int calcCarton = (cartonSize > 0) ? (int)(totalStock / cartonSize) : 0;
            int calcLoose = (cartonSize > 0) ? (int)(totalStock % cartonSize) : (int)totalStock;

            if (holder.tvTotalQty != null) holder.tvTotalQty.setText("Total Stock: " + (int)totalStock);
            if (holder.tvCartonSize != null) holder.tvCartonSize.setText("Crton Size: " + (int)cartonSize);
            if (holder.tvCarton != null) holder.tvCarton.setText("Carton Qty: " + calcCarton);
            if (holder.tvLoose != null) holder.tvLoose.setText("Loose Qty: " + calcLoose);
            
        } catch (Exception e) {
            Log.e("Adapter", "Calculation error: " + e.getMessage());
        }
    }

    private void showEditCartonSizeDialog(InventoryModel model, ViewHolder holder) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Update Carton Size for " + model.getProduct_Name());
        final EditText input = new EditText(context);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setText(model.getCarton_Size());
        builder.setView(input);
        builder.setPositiveButton("Update", (dialog, which) -> {
            String newSize = input.getText().toString().trim();
            if (!newSize.isEmpty()) {
                model.setCarton_Size(newSize);
                updateCalculatedFields(holder, model);
                updateFirebaseData(model);
            }
        }).setNegativeButton("Cancel", null);
        AlertDialog dialog = builder.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.WHITE);
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.WHITE);
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setBackgroundColor(Color.parseColor("#2196F3"));
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setBackgroundColor(Color.GRAY);
    }

    private void updateFirebaseData(InventoryModel model) {
        // একাধিক টিক দেওয়ার সুবিধার্থে এখানে setLoading ব্যবহার করা হলো না যাতে UI লক না হয়
        SharedPreferences prefs = context.getSharedPreferences("USER_SESSION", Context.MODE_PRIVATE);
        String userId = prefs.getString("userId", "Unknown");
        String userName = prefs.getString("userName", "Unknown");

        Map<String, Object> updates = new HashMap<>();
        updates.put("status", model.getStatus());
        updates.put("shortQty", model.getShortQty());
        updates.put("excessQty", model.getExcessQty());
        updates.put("remark", model.getRemark());
        updates.put("Carton_Size", model.getCarton_Size());
        updates.put("lastUpdatedBy", userName + " (" + userId + ")");
        updates.put("lastUpdatedTime", System.currentTimeMillis());

        if (model.getFirebaseKey() != null && !model.getFirebaseKey().isEmpty() && model.getCategory() != null) {
            mDatabase.child(model.getCategory()).child(model.getFirebaseKey()).updateChildren(updates)
                .addOnSuccessListener(aVoid -> {
                    if (context instanceof MainActivity) {
                        ((MainActivity) context).showBigSuccessDialog();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(context, "Update failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
        }
    }

    @Override public int getItemCount() { return list == null ? 0 : list.size(); }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvSl, tvCategory, tvPackSize, tvCode, tvProductName, tvTotalQty, tvLoose, tvCarton, tvCartonSize;
        EditText etShortQty, etExcessQty, etRemark;
        CheckBox btnCheckUpdate;
        LinearLayout itemContainer;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSl = itemView.findViewById(R.id.tvSl);
            tvCategory = itemView.findViewById(R.id.tvCategory);
            tvPackSize = itemView.findViewById(R.id.tvPackSize);
            tvCode = itemView.findViewById(R.id.tvCode);
            tvProductName = itemView.findViewById(R.id.tvProductName);
            tvTotalQty = itemView.findViewById(R.id.tvTotalQty);
            tvLoose = itemView.findViewById(R.id.tvLoose);
            tvCarton = itemView.findViewById(R.id.tvCarton);
            tvCartonSize = itemView.findViewById(R.id.tvCartonSize);
            etShortQty = itemView.findViewById(R.id.etShortQty);
            etExcessQty = itemView.findViewById(R.id.etExcessQty);
            etRemark = itemView.findViewById(R.id.etRemark);
            btnCheckUpdate = itemView.findViewById(R.id.btnCheckUpdate);
            itemContainer = itemView.findViewById(R.id.itemContainer);
        }
    }
}