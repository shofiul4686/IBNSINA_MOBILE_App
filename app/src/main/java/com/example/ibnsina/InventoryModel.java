package com.example.ibnsina;

import com.google.firebase.database.Exclude;
import com.google.firebase.database.IgnoreExtraProperties;
import com.google.firebase.database.PropertyName;

@IgnoreExtraProperties
public class InventoryModel {
    private String sl = "";
    private String category = "";
    private String code = "";
    private String productName = "";
    private String packSize = "";
    private String totalQty = "0";
    private String cartonSize = "0";
    private String shortQty = "";
    private String excessQty = "";
    private String remark = "";
    private String status = "Unchecked";
    
    @Exclude
    private String firebaseKey = "";

    public InventoryModel() {
        // Required for Firebase
    }

    // Firebase Mapping
    
    @PropertyName("Category")
    public String getCategory() { return category; }
    @PropertyName("Category")
    public void setCategory(String category) { this.category = category != null ? category : ""; }

    @PropertyName("Code")
    public String getCode() { return code; }
    @PropertyName("Code")
    public void setCode(String code) { this.code = code != null ? code : ""; }

    @PropertyName("Product_Name")
    public String getProduct_Name() { return productName; }
    @PropertyName("Product_Name")
    public void setProduct_Name(String product_Name) { this.productName = product_Name != null ? product_Name : ""; }

    @PropertyName("Pack_Size")
    public String getPack_Size() { return packSize; }
    @PropertyName("Pack_Size")
    public void setPack_Size(String pack_Size) { this.packSize = pack_Size != null ? pack_Size : ""; }

    @PropertyName("totalQty")
    public String getTotalQty() { return totalQty; }
    @PropertyName("totalQty")
    public void setTotalQty(Object totalQty) { 
        this.totalQty = (totalQty == null) ? "0" : String.valueOf(totalQty); 
    }

    @PropertyName("Carton_Size")
    public String getCarton_Size() { return cartonSize; }
    @PropertyName("Carton_Size")
    public void setCarton_Size(Object carton_Size) { 
        this.cartonSize = (carton_Size == null) ? "0" : String.valueOf(carton_Size); 
    }

    @PropertyName("status")
    public String getStatus() { return status; }
    @PropertyName("status")
    public void setStatus(String status) { this.status = (status == null || status.isEmpty()) ? "Unchecked" : status; }

    @PropertyName("shortQty")
    public String getShortQty() { return shortQty; }
    @PropertyName("shortQty")
    public void setShortQty(String shortQty) { this.shortQty = shortQty != null ? shortQty : ""; }

    @PropertyName("excessQty")
    public String getExcessQty() { return excessQty; }
    @PropertyName("excessQty")
    public void setExcessQty(String excessQty) { this.excessQty = excessQty != null ? excessQty : ""; }

    @PropertyName("remark")
    public String getRemark() { return remark; }
    @PropertyName("remark")
    public void setRemark(String remark) { this.remark = remark != null ? remark : ""; }

    // Helper for Firebase Key
    @Exclude
    public String getFirebaseKey() { return firebaseKey; }
    @Exclude
    public void setFirebaseKey(String firebaseKey) { this.firebaseKey = firebaseKey; }

    @Exclude
    public String getProductName() { return productName; }
    @Exclude
    public String getSl() { return sl; }
    @Exclude
    public void setSl(String sl) { this.sl = sl; }
}