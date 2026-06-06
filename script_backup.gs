/**
 * IBN SINA Inventory Management - MASTER SCRIPT (FIXED)
 */

var MAIN_SS_ID = '1UKCJz6vT_N9L-uhhGDVtVVJOVE5CVurzZXJZo167vu8';

function doGet(e) {
  var action = e.parameter.action;
  
  try {
    var ss = SpreadsheetApp.openById(MAIN_SS_ID);
    var appSheet = ss.getSheetByName('MOBIL APPS');

    // 1. Fetch Inventory Data
    if (action == "getJson") {
      var lastRow = appSheet.getLastRow();
      if (lastRow < 2) return ContentService.createTextOutput(JSON.stringify([])).setMimeType(ContentService.MimeType.JSON);
      
      var data = appSheet.getRange(2, 1, lastRow - 1, 13).getValues(); 
      var list = [];
      for (var i = 0; i < data.length; i++) {
        if (data[i][1] != "") {
          var rawStatus = String(data[i][11]).trim().toLowerCase();
          var finalStatus = (rawStatus === "checked") ? "Checked" : "Unchecked";
          list.push({
            "sl": data[i][12], "category": data[i][0], "code": data[i][1],        
            "productName": data[i][2], "packSize": data[i][3], "totalQty": data[i][4],
            "cartonSize": data[i][7], "shortQty": data[i][8], "excessQty": data[i][9],
            "remark": data[i][10], "status": finalStatus
          });
        }
      }
      return ContentService.createTextOutput(JSON.stringify(list)).setMimeType(ContentService.MimeType.JSON);
    }

    // 2. Login & AutoFill (সঠিক কলাম ইনডেক্স সেট করা হয়েছে)
    if (action == "login" || action == "getAutoFill") {
      var searchId = String(e.parameter.userId).trim();
      var pass = String(e.parameter.password).trim();
      var credSheet = ss.getSheetByName('Credentials');
      var credData = credSheet.getDataRange().getValues();
      
      for (var i = 1; i < credData.length; i++) {
        // কলাম A (0) হলো USER ID
        if (String(credData[i][0]).trim().toLowerCase() == searchId.toLowerCase()) {
          
          // লগইন করার সময় পাসওয়ার্ড চেক (কলাম B (1) হলো PASSWORD)
          if (action == "login" && String(credData[i][1]).trim() != pass) {
            return ContentService.createTextOutput(JSON.stringify({"success": false, "message": "Wrong Password"})).setMimeType(ContentService.MimeType.JSON);
          }
          
          // সফল হলে নাম (কলাম C-2) ও পদবী (কলাম D-3) পাঠানো হবে
          return ContentService.createTextOutput(JSON.stringify({
            "success": true, 
            "name": String(credData[i][2]), 
            "designation": String(credData[i][3])
          })).setMimeType(ContentService.MimeType.JSON);
        }
      }
      return ContentService.createTextOutput(JSON.stringify({"success": false, "message": "User Not Found"})).setMimeType(ContentService.MimeType.JSON);
    }

    // 3. Update Stock
    if (action == "updateStock") {
      var code = String(e.parameter.code).trim().toLowerCase();
      var data = appSheet.getDataRange().getValues();
      for (var i = 1; i < data.length; i++) {
        if (String(data[i][1]).trim().toLowerCase() === code) {
          appSheet.getRange(i + 1, 9).setValue(e.parameter.shortQty);  
          appSheet.getRange(i + 1, 10).setValue(e.parameter.excessQty); 
          appSheet.getRange(i + 1, 11).setValue(e.parameter.remark);    
          appSheet.getRange(i + 1, 12).setValue(e.parameter.status == "Checked" ? "Checked" : ""); 
          appSheet.getRange(i + 1, 14).setValue(e.parameter.userId);   
          appSheet.getRange(i + 1, 15).setValue(e.parameter.userName); 
          return ContentService.createTextOutput("Success").setMimeType(ContentService.MimeType.TEXT);
        }
      }
      return ContentService.createTextOutput("Not Found").setMimeType(ContentService.MimeType.TEXT);
    }

  } catch (err) {
    return ContentService.createTextOutput("Error: " + err.message).setMimeType(ContentService.MimeType.TEXT);
  }
}
