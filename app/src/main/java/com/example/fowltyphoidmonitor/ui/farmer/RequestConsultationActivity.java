package com.example.fowltyphoidmonitor.ui.farmer;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.fowltyphoidmonitor.R;
import com.example.fowltyphoidmonitor.ui.auth.LoginActivity;
import com.example.fowltyphoidmonitor.ui.farmer.FarmerConsultationInboxActivity;
import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class RequestConsultationActivity extends AppCompatActivity {

    // Authentication constants - matching AdminMainActivity for consistency
    private static final String PREFS_NAME = "FowlTyphoidMonitorPrefs";
    private static final String KEY_USERNAME = "username";
    // User type constants - internal app format (camelCase key, normalized values)
    private static final String KEY_USER_TYPE = "userType";
    // Note: Only 'farmer' and 'vet' supported in unified system
    private static final String USER_TYPE_FARMER = "farmer";
    private static final String TAG = "RequestConsultationActivity";

    // UI Views
    private EditText etPoultryType;
    private TextView etPhoneNumber;
    private EditText etSymptoms, etAdditionalNotes;
    private MaterialButton btnSubmitRequest;
    private TextView txtTitle, txtUserInfo;
    private ImageButton btnBack;
    private Toolbar toolbar;

    // Current user data
    private String currentUserType;
    private String currentUsername;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Check authentication using AuthManager
        com.example.fowltyphoidmonitor.services.auth.AuthManager authManager = com.example.fowltyphoidmonitor.services.auth.AuthManager.getInstance(this);
        if (!authManager.isLoggedIn()) {
            Log.d(TAG, "User not logged in, redirecting to login screen");
            redirectToLogin();
            return;
        }

        setContentView(R.layout.activity_request_consultation);

        // Get current user data
        currentUserType = getCurrentUserType();
        currentUsername = getCurrentUsername();

        // Initialize views
        initializeViews();

        // Set up toolbar
        setupToolbar();

        // Load user information
        loadUserData();

        // Set up click listeners
        setupClickListeners();

        Log.d(TAG, "RequestConsultationActivity created for user: " + currentUsername + " (" + currentUserType + ")");
    }

    private void initializeViews() {
        // Toolbar and navigation
        toolbar = findViewById(R.id.toolbar);
        btnBack = findViewById(R.id.btnBack);
        txtTitle = findViewById(R.id.txtTitle);
        txtUserInfo = findViewById(R.id.txtUserInfo);

        // Form fields (only those present in the new layout)
        etPoultryType = findViewById(R.id.etPoultryType);
        etPhoneNumber = findViewById(R.id.txtPhoneNumber); // This is a TextView in the new layout
        etSymptoms = findViewById(R.id.etSymptoms);
        etAdditionalNotes = findViewById(R.id.etAdditionalNotes);

        // Buttons
        btnSubmitRequest = findViewById(R.id.btnSubmitRequest);
        // No cancel button in new layout
    }

    private void setupToolbar() {
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setDisplayShowHomeEnabled(true);
                getSupportActionBar().setTitle("Omba Ushauri");
            }
        }

        if (txtTitle != null) {
            txtTitle.setText("Omba Ushauri wa Daktari");
        }
    }

    private void loadUserData() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String userName = prefs.getString("farmerName", "");
        String phoneNumber = prefs.getString("farmerPhone", "");
        if (txtUserInfo != null && !userName.isEmpty()) {
            txtUserInfo.setText("Jina: " + userName);
        }
        if (etPhoneNumber != null && !phoneNumber.isEmpty()) {
            etPhoneNumber.setText(phoneNumber);
        }
    }

    private void setupClickListeners() {
        // Back button
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> onBackPressed());
        }

        // Submit button
        if (btnSubmitRequest != null) {
            btnSubmitRequest.setOnClickListener(v -> submitConsultationRequest());
        }
    }

    private void submitConsultationRequest() {
        if (!validateForm()) {
            return;
        }
        ConsultationRequest request = createConsultationRequest();
        saveConsultationRequest(request);
        updateFarmerStats();
        Toast.makeText(this, "Ombi la ushauri limepelekwa kikamilifu", Toast.LENGTH_LONG).show();
        // Navigate to FarmerConsultationInboxActivity after submission
        Intent intent = new Intent(this, FarmerConsultationInboxActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private boolean validateForm() {
        boolean isValid = true;
        if (etPoultryType.getText().toString().trim().isEmpty()) {
            etPoultryType.setError("Aina ya kuku inahitajika");
            isValid = false;
        }
        String phone = etPhoneNumber.getText().toString().trim();
        if (phone.isEmpty()) {
            etPhoneNumber.setError(null); // TextView does not support setError
            etPhoneNumber.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
            isValid = false;
        } else if (phone.length() < 10) {
            etPhoneNumber.setError(null);
            etPhoneNumber.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
            isValid = false;
        } else {
            etPhoneNumber.setTextColor(getResources().getColor(android.R.color.black));
        }
        if (etSymptoms.getText().toString().trim().isEmpty()) {
            etSymptoms.setError("Dalili zinahitajika");
            isValid = false;
        }
        return isValid;
    }

    private ConsultationRequest createConsultationRequest() {
        return new ConsultationRequest(
                etPoultryType.getText().toString().trim(),
                etPhoneNumber.getText().toString().trim(),
                "", // email removed
                "Kuku", // consultationType
                "Normal", // urgencyLevel
                "", // preferredDate
                "", // preferredTime
                etSymptoms.getText().toString().trim(),
                etAdditionalNotes.getText().toString().trim(),
                currentUsername,
                currentUserType,
                System.currentTimeMillis()
        );
    }

    private void saveConsultationRequest(ConsultationRequest request) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        // Generate unique consultation ID
        String consultationId = "CONS_" + System.currentTimeMillis();

        // Save consultation request details
        String prefix = "consultation_" + consultationId + "_";
        editor.putString(prefix + "patientName", request.patientName);
        editor.putString(prefix + "phoneNumber", request.phoneNumber);
        editor.putString(prefix + "email", request.email);
        editor.putString(prefix + "consultationType", request.consultationType);
        editor.putString(prefix + "urgencyLevel", request.urgencyLevel);
        editor.putString(prefix + "preferredDate", request.preferredDate);
        editor.putString(prefix + "preferredTime", request.preferredTime);
        editor.putString(prefix + "symptoms", request.symptoms);
        editor.putString(prefix + "additionalNotes", request.additionalNotes);
        editor.putString(prefix + "requestedBy", request.requestedBy);
        editor.putString(prefix + "userType", request.userType);
        editor.putLong(prefix + "timestamp", request.timestamp);
        editor.putString(prefix + "status", "PENDING");

        // Add to consultation list
        String consultationList = prefs.getString("consultationList", "");
        if (!consultationList.isEmpty()) {
            consultationList += ",";
        }
        consultationList += consultationId;
        editor.putString("consultationList", consultationList);

        editor.apply();

        Log.d(TAG, "Consultation request saved with ID: " + consultationId);
    }

    private void updateFarmerStats() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        // Update farmer's pending consultations count
        int currentPending = prefs.getInt("farmerPendingConsultations", 0);
        editor.putInt("farmerPendingConsultations", currentPending + 1);

        // Update total consultations requested
        int totalRequested = prefs.getInt("farmerTotalConsultationsRequested", 0);
        editor.putInt("farmerTotalConsultationsRequested", totalRequested + 1);

        editor.apply();

        Log.d(TAG, "Farmer statistics updated - Pending: " + (currentPending + 1));
    }

    // Authentication helper methods (matching AdminMainActivity pattern)

    private String getCurrentUserType() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        return prefs.getString(KEY_USER_TYPE, USER_TYPE_FARMER);
    }

    private String getCurrentUsername() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        return prefs.getString(KEY_USERNAME, "");
    }

    private void redirectToLogin() {
        // Only admin and farmer supported
        Intent intent = new Intent(RequestConsultationActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    public void onBackPressed() {
        setResult(RESULT_CANCELED);
        super.onBackPressed();
    }

    // Data class for consultation request
    public static class ConsultationRequest {
        public final String patientName;
        public final String phoneNumber;
        public final String email;
        public final String consultationType;
        public final String urgencyLevel;
        public final String preferredDate;
        public final String preferredTime;
        public final String symptoms;
        public final String additionalNotes;
        public final String requestedBy;
        public final String userType;
        public final long timestamp;

        public ConsultationRequest(String patientName, String phoneNumber, String email,
                                   String consultationType, String urgencyLevel, String preferredDate,
                                   String preferredTime, String symptoms, String additionalNotes,
                                   String requestedBy, String userType, long timestamp) {
            this.patientName = patientName;
            this.phoneNumber = phoneNumber;
            this.email = email;
            this.consultationType = consultationType;
            this.urgencyLevel = urgencyLevel;
            this.preferredDate = preferredDate;
            this.preferredTime = preferredTime;
            this.symptoms = symptoms;
            this.additionalNotes = additionalNotes;
            this.requestedBy = requestedBy;
            this.userType = userType;
            this.timestamp = timestamp;
        }
    }
}
