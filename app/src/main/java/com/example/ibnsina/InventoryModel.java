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

    // যেকোনো type (Long, Double, String) কে নিরাপদে String এ রূপান্তর করে
    private static String toSafeString(Object val, String defaultVal) {
        if (val == null) return defaultVal;
        String s = String.valueOf(val).trim();
        return s.isEmpty() ? defaultVal : s;
    }

    @PropertyName("Category")
    public String getCategory() { return category; }
    @PropertyName("Category")
    public void setCategory(Object category) { this.category = toSafeString(category, ""); }

    @PropertyName("Code")
    public String getCode() { return code; }
    @PropertyName("Code")
    public void setCode(Object code) { this.code = toSafeString(code, ""); }

    @PropertyName("Product_Name")
    public String getProduct_Name() { return productName; }
    @PropertyName("Product_Name")
    public void setProduct_Name(Object product_Name) { this.productName = toSafeString(product_Name, ""); }

    @PropertyName("Pack_Size")
    public String getPack_Size() { return packSize; }
    @PropertyName("Pack_Size")
    public void setPack_Size(Object pack_Size) { this.packSize = toSafeString(pack_Size, ""); }

    @PropertyName("totalQty")
    public String getTotalQty() { return totalQty; }
    @PropertyName("totalQty")
    public void setTotalQty(Object totalQty) { this.totalQty = toSafeString(totalQty, "0"); }

    @PropertyName("Carton_Size")
    public String getCarton_Size() { return cartonSize; }
    @PropertyName("Carton_Size")
    public void setCarton_Size(Object carton_Size) { this.cartonSize = toSafeString(carton_Size, "0"); }

    @PropertyName("status")
    public String getStatus() { return status; }
    @PropertyName("status")
    public void setStatus(Object status) {
        String s = toSafeString(status, "");
        this.status = s.isEmpty() ? "Unchecked" : s;
    }

    @PropertyName("shortQty")
    public String getShortQty() { return shortQty; }
    @PropertyName("shortQty")
    public void setShortQty(Object shortQty) { this.shortQty = toSafeString(shortQty, ""); }

    @PropertyName("excessQty")
    public String getExcessQty() { return excessQty; }
    @PropertyName("excessQty")
    public void setExcessQty(Object excessQty) { this.excessQty = toSafeString(excessQty, ""); }

    @PropertyName("remark")
    public String getRemark() { return remark; }
    @PropertyName("remark")
    public void setRemark(Object remark) { this.remark = toSafeString(remark, ""); }

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