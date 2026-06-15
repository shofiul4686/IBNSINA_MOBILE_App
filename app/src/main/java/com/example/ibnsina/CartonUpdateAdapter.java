package com.example.ibnsina;

import android.content.Context;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.List;

public class CartonUpdateAdapter extends RecyclerView.Adapter<CartonUpdateAdapter.ViewHolder> {
    private List<InventoryModel> list;
    private Context context;
    private DatabaseReference mDatabase;

    public CartonUpdateAdapter(List<InventoryModel> list) {
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
        View view = LayoutInflater.from(context).inflate(R.layout.item_carton_update, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        InventoryModel model = list.get(position);

        holder.tvSl.setText(String.valueOf(position + 1));
        holder.tvCode.setText(model.getCode());
        holder.tvName.setText(model.getProduct_Name());
        holder.tvPack.setText(model.getPack_Size());
        holder.tvCartonSize.setText(model.getCarton_Size());

        holder.tvCartonSize.setOnLongClickListener(v -> {
            showEditDialog(model, position);
            return true;
        });
    }

    private void showEditDialog(InventoryModel model, int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Update Carton Size");
        builder.setMessage(model.getProduct_Name());
        
        final EditText input = new EditText(context);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setText(model.getCarton_Size());
        input.setSelection(input.getText().length());
        builder.setView(input);

        builder.setPositiveButton("Update", (dialog, which) -> {
            String newSize = input.getText().toString().trim();
            if (!newSize.isEmpty()) {
                mDatabase.child(model.getCategory()).child(model.getFirebaseKey())
                        .child("Carton_Size").setValue(newSize)
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(context, "Updated successfully", Toast.LENGTH_SHORT).show();
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(context, "Update failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvSl, tvCode, tvName, tvPack, tvCartonSize;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSl = itemView.findViewById(R.id.tvSl);
            tvCode = itemView.findViewById(R.id.tvCode);
            tvName = itemView.findViewById(R.id.tvName);
            tvPack = itemView.findViewById(R.id.tvPack);
            tvCartonSize = itemView.findViewById(R.id.tvCartonSize);
        }
    }
}