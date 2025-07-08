package com.example.fowltyphoidmonitor.ui.auth;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.fowltyphoidmonitor.data.api.ApiClient;
import com.example.fowltyphoidmonitor.data.api.ApiService;
import com.example.fowltyphoidmonitor.data.api.SupabaseClient;
import com.example.fowltyphoidmonitor.data.models.Farmer;
import com.example.fowltyphoidmonitor.data.models.Vet;
import com.example.fowltyphoidmonitor.services.auth.AuthManager;
import com.example.fowltyphoidmonitor.R;
import com.example.fowltyphoidmonitor.data.requests.AuthResponse;
import com.example.fowltyphoidmonitor.ui.vet.AdminMainActivity;
import com.example.fowltyphoidmonitor.ui.common.ProfileActivity;
import com.example.fowltyphoidmonitor.ui.farmer.FarmerProfileEditActivity;
import com.example.fowltyphoidmonitor.ui.farmer.MainActivity;
import com.example.fowltyphoidmonitor.utils.SharedPreferencesManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

/**
 * RegisterActivity - Handles user registration for farmers and veterinarians
 * @author LWENA27
 * @updated 2025-06-17
 */
public class RegisterActivity extends AppCompatActivity {

    private static final String TAG = "RegisterActivity";
    private static final int REQUEST_CODE_WEB_AUTH = 1001;

    // User role constants - UNIFIED SYSTEM: admin, vet, doctor are all the same
    // User type constants - internal app format (normalized values only)
    private static final String USER_TYPE_FARMER = "farmer";
    private static final String USER_TYPE_VET = "vet";  // Primary unified role for all medical professionals
    private static final String USER_TYPE_ADMIN = "vet"; // Internal: admin maps to vet
    private static final String USER_TYPE_DOCTOR = "vet"; // Internal: doctor maps to vet

    // UI elements
    private ImageButton btnRegisterBack;

    // User information fields
    private TextInputLayout tilDisplayName;
    private TextInputEditText etDisplayName;
    private TextInputLayout tilRegisterEmail;
    private TextInputEditText etRegisterEmail;
    private TextInputLayout tilPhoneNumber;
    private TextInputEditText etPhoneNumber;

    // Authentication fields
    private TextInputLayout tilRegisterPassword;
    private TextInputEditText etRegisterPassword;
    private TextInputLayout tilRegisterConfirmPassword;
    private TextInputEditText etRegisterConfirmPassword;

    // Optional fields for admin users (removed vet-specific fields)
    private TextInputLayout tilSpecialization;
    private TextInputEditText etSpecialization;
    private TextInputLayout tilLocation;
    private TextInputEditText etLocation;

    // Action buttons
    private MaterialButton btnRegisterSubmit;
    private MaterialButton btnGoToLogin;
    private TextView tvErrorBanner;
    private TextView tvRegisterSubtitle;

    // Auth manager
    private AuthManager authManager;
    private SharedPreferencesManager prefManager;

    // Loading state
    private View loadingOverlay;

    // User type received from intent
    private String userType; // No default value, must be set explicitly

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Initialize auth manager
        authManager = AuthManager.getInstance(this);
        prefManager = new SharedPreferencesManager(this);

        // Get user type from intent (required)
        userType = getIntent().getStringExtra("userType");
        if (userType == null || !(userType.equals("farmer") || userType.equals("vet") || userType.equals("admin") || userType.equals("doctor"))) {
            Toast.makeText(this, "User type is required. Please select a valid user type.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // Map admin/doctor to vet for internal logic
        if (userType.equals("admin") || userType.equals("doctor")) {
            userType = USER_TYPE_VET;
        }

        // Initialize views
        initializeViews();
        setupListeners();
        updateUIForUserType();

        Log.d(TAG, "[LWENA27] " + getCurrentTime() + " - RegisterActivity initialized with user type: " + userType);
    }

    private void initializeViews() {
        // Initialize UI elements
        btnRegisterBack = findViewById(R.id.btnRegisterBack);
        tilDisplayName = findViewById(R.id.tilDisplayName);
        etDisplayName = findViewById(R.id.etDisplayName);
        tilRegisterEmail = findViewById(R.id.tilRegisterEmail);
        etRegisterEmail = findViewById(R.id.etRegisterEmail);
        tilPhoneNumber = findViewById(R.id.tilPhoneNumber);
        etPhoneNumber = findViewById(R.id.etPhoneNumber);
        tilRegisterPassword = findViewById(R.id.tilRegisterPassword);
        etRegisterPassword = findViewById(R.id.etRegisterPassword);
        tilRegisterConfirmPassword = findViewById(R.id.tilRegisterConfirmPassword);
        etRegisterConfirmPassword = findViewById(R.id.etRegisterConfirmPassword);
        btnRegisterSubmit = findViewById(R.id.btnRegisterSubmit);
        btnGoToLogin = findViewById(R.id.btnGoToLogin);
        tvErrorBanner = findViewById(R.id.tvErrorBanner);
        tvRegisterSubtitle = findViewById(R.id.tvRegisterSubtitle);
        loadingOverlay = findViewById(R.id.loadingOverlay);
    }

    private void setupListeners() {
        btnRegisterSubmit.setOnClickListener(v -> handleRegistration());
        btnRegisterBack.setOnClickListener(v -> onBackPressed());
        btnGoToLogin.setOnClickListener(v -> {
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            finish();
        });
    }

    private void updateUIForUserType() {
        // Update subtitle based on user type
        String subtitle = userType.equals(USER_TYPE_FARMER)
            ? getString(R.string.register_farmer_subtitle)
            : getString(R.string.register_admin_subtitle);
        tvRegisterSubtitle.setText(subtitle);
    }

    private void handleRegistration() {
        // Clear any previous errors
        tvErrorBanner.setVisibility(View.GONE);
        clearErrors();

        // Get input values
        String displayName = etDisplayName.getText().toString().trim();
        String email = etRegisterEmail.getText().toString().trim();
        String phoneNumber = etPhoneNumber.getText().toString().trim();
        String password = etRegisterPassword.getText().toString();
        String confirmPassword = etRegisterConfirmPassword.getText().toString();

        // Validate inputs
        if (!validateInputs(displayName, email, phoneNumber, password, confirmPassword)) {
            return;
        }

        // Show loading state
        setLoadingState(true);

        // Create metadata for the user
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("display_name", displayName);
        metadata.put("phone_number", phoneNumber);
        // API requires "user_type" key format
        metadata.put("user_type", userType);
        metadata.put("email_verified", false);
        metadata.put("phone_verified", false);

        // Register user
        authManager.signUp(email, password, userType, new AuthManager.AuthCallback() {
            @Override
            public void onSuccess(AuthResponse response) {
                Log.d(TAG, "[LWENA27] " + getCurrentTime() + " - Registration successful");
                authManager.setUserType(userType);
                Log.d(TAG, "[LWENA27] " + getCurrentTime() + " - User type set to: " + userType);

                // --- Insert user into correct profile table with user_id ---
                ApiService apiService = SupabaseClient.getInstance(RegisterActivity.this).getApiService();
                String userId = null;
                if (response.getUser() != null) {
                    userId = response.getUser().getId();
                }
                if (userId == null || userId.isEmpty()) {
                    setLoadingState(false);
                    showError("Failed to get user ID from Supabase. Registration aborted.");
                    return;
                }
                if (userType.equals(USER_TYPE_FARMER)) {
                    Farmer farmer = new Farmer();
                    farmer.setUserId(userId);
                    farmer.setEmail(email);
                    farmer.setName(displayName);
                    farmer.setPhoneNumber(phoneNumber);
                    // Add any other required fields
                    apiService.createFarmer(farmer).enqueue(new retrofit2.Callback<Farmer>() {
                        @Override
                        public void onResponse(retrofit2.Call<Farmer> call, retrofit2.Response<Farmer> response) {
                            setLoadingState(false);
                            if (response.isSuccessful()) {
                                navigateToProfileSetup();
                            } else {
                                showError("Failed to create farmer profile. Please try again.");
                            }
                        }
                        @Override
                        public void onFailure(retrofit2.Call<Farmer> call, Throwable t) {
                            setLoadingState(false);
                            showError("Network error: " + t.getMessage());
                        }
                    });
                } else if (userType.equals(USER_TYPE_VET)) {
                    Vet vet = new Vet();
                    vet.setUserId(userId);
                    vet.setEmail(email);
                    vet.setName(displayName);
                    vet.setPhoneNumber(phoneNumber);
                    // Add any other required fields
                    String authToken = "Bearer " + response.getAccessToken();
                    String apiKey = com.example.fowltyphoidmonitor.config.SupabaseConfig.SUPABASE_ANON_KEY;
                    apiService.createVet(authToken, apiKey, vet).enqueue(new retrofit2.Callback<Vet>() {
                        @Override
                        public void onResponse(retrofit2.Call<Vet> call, retrofit2.Response<Vet> response) {
                            setLoadingState(false);
                            if (response.isSuccessful()) {
                                navigateToProfileSetup();
                            } else {
                                showError("Failed to create vet profile. Please try again.");
                            }
                        }
                        @Override
                        public void onFailure(retrofit2.Call<Vet> call, Throwable t) {
                            setLoadingState(false);
                            showError("Network error: " + t.getMessage());
                        }
                    });
                } else {
                    setLoadingState(false);
                    showError("Invalid user type. Registration aborted.");
                }
            }

            @Override
            public void onError(String message) {
                Log.e(TAG, "[LWENA27] " + getCurrentTime() + " - Registration error: " + message);
                setLoadingState(false);
                if (message != null && (message.contains("user_already_exists") || message.contains("already registered") || message.contains("Email may already be in use"))) {
                    showError("This email is already registered. Please log in or use a different email address.");
                } else {
                    showError(getString(R.string.registration_error));
                }
            }
        });
    }

    private boolean validateInputs(String displayName, String email, String phoneNumber,
                                 String password, String confirmPassword) {
        boolean isValid = true;

        if (TextUtils.isEmpty(displayName)) {
            tilDisplayName.setError(getString(R.string.error_display_name_required));
            isValid = false;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilRegisterEmail.setError(getString(R.string.error_invalid_email));
            isValid = false;
        }

        if (TextUtils.isEmpty(phoneNumber)) {
            tilPhoneNumber.setError(getString(R.string.error_phone_required));
            isValid = false;
        }

        if (password.length() < 8) {
            tilRegisterPassword.setError(getString(R.string.error_password_length));
            isValid = false;
        }

        if (!password.equals(confirmPassword)) {
            tilRegisterConfirmPassword.setError(getString(R.string.error_passwords_dont_match));
            isValid = false;
        }

        return isValid;
    }

    private void clearErrors() {
        tilDisplayName.setError(null);
        tilRegisterEmail.setError(null);
        tilPhoneNumber.setError(null);
        tilRegisterPassword.setError(null);
        tilRegisterConfirmPassword.setError(null);
    }

    private void setLoadingState(boolean isLoading) {
        loadingOverlay.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        btnRegisterSubmit.setEnabled(!isLoading);
        btnGoToLogin.setEnabled(!isLoading);
    }

    private void showError(String message) {
        tvErrorBanner.setText(message);
        tvErrorBanner.setVisibility(View.VISIBLE);
    }

    private void navigateToProfileSetup() {
        Intent intent;
        if (userType.equals(USER_TYPE_FARMER)) {
            intent = new Intent(this, FarmerProfileEditActivity.class);
        } else {
            intent = new Intent(this, ProfileActivity.class);
        }
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private String getCurrentTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        sdf.setTimeZone(TimeZone.getDefault());
        return sdf.format(new Date());
    }

    private void navigateToMainScreen() {
        // Get the appropriate activity based on user type
        Intent intent;
        if (userType.equals(USER_TYPE_ADMIN) || authManager.isAdmin()) {
            try {
                intent = new Intent(RegisterActivity.this, AdminMainActivity.class);
            } catch (Exception e) {
                intent = new Intent(RegisterActivity.this, MainActivity.class);
            }
        } else {
            intent = new Intent(RegisterActivity.this, MainActivity.class);
            intent.putExtra("userType", USER_TYPE_FARMER);
        }

        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void redirectToLogin() {
        Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    public void onBackPressed() {
        redirectToLogin();
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clear references to prevent memory leaks
        authManager = null;
        prefManager = null;
    }
}
