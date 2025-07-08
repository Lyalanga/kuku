package com.example.fowltyphoidmonitor.ui.farmer;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.fowltyphoidmonitor.R;
import com.example.fowltyphoidmonitor.data.api.ApiClient;
import com.example.fowltyphoidmonitor.data.models.Farmer;
import com.example.fowltyphoidmonitor.services.auth.AuthManager;
import com.example.fowltyphoidmonitor.config.SupabaseConfig;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import de.hdodenhof.circleimageview.CircleImageView;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FarmerProfileEditActivity extends AppCompatActivity {

    private static final String TAG = "ProfileEditActivity";
    private static final String PREFS_NAME = "FowlTyphoidMonitorPrefs";
    private static final String KEY_PROFILE_COMPLETE = "isProfileComplete";
    public static final String EXTRA_PROFILE_UPDATED = "profile_updated";

    // UI Elements - Enhanced with new fields
    private TextInputEditText etFarmName;
    private TextInputEditText etLocation;
    private TextInputEditText etFarmSize;
    private TextInputEditText etFarmAddress;
    private AutoCompleteTextView etFarmType;
    private TextInputEditText etExperience;
    private MaterialButton btnSave;
    private MaterialButton btnCancel;
    private ImageButton btnBackEdit;
    private ImageButton btnDismissError;
    private MaterialButton btnChangePhoto;
    private CircleImageView profileImageEdit;
    private TextView tvErrorMessage;
    private TextView tvLoadingMessage;
    private TextView tvHeaderTitle;
    private View loadingOverlay;
    private View errorCard;
    private View progressStep3;

    // Enhanced form validation and UX
    private String[] farmTypeOptions = {
        "Mayai (Egg Production)",
        "Nyama (Meat Production)",
        "Kienyeji (Free Range)",
        "Broiler (Commercial Meat)",
        "Layer (Commercial Eggs)",
        "Kienyeji na Kisasa (Mixed)"
    };

    private AuthManager authManager;
    private Uri selectedImageUri = null;
    private boolean isNewUser = false;
    private Farmer currentFarmer;

    // Store original data for change detection
    private Farmer originalFarmerData;

    private final ActivityResultLauncher<String> imagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    selectedImageUri = uri;
                    profileImageEdit.setImageURI(uri);
                    Log.d(TAG, "[LWENA27] " + getCurrentTime() + " - Profile image selected: " + uri.toString());
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_edit);

        authManager = AuthManager.getInstance(this);
        isNewUser = getIntent().getBooleanExtra("isNewUser", false);

        initViews();
        loadProfileData();
        setupClickListeners();

        Log.d(TAG, "[LWENA27] " + getCurrentTime() + " - FarmerProfileEditActivity initialized - isNewUser: " + isNewUser);
    }

    private void initViews() {
        try {
            etFarmName = findViewById(R.id.etFarmName);
            etLocation = findViewById(R.id.etLocation);
            etFarmSize = findViewById(R.id.etFarmSize);
            etFarmAddress = findViewById(R.id.etFarmAddress);
            etFarmType = findViewById(R.id.etFarmType);
            etExperience = findViewById(R.id.etExperience);
            btnSave = findViewById(R.id.btnSave);
            btnCancel = findViewById(R.id.btnCancel);
            btnBackEdit = findViewById(R.id.btnBackEdit);
            btnDismissError = findViewById(R.id.btnDismissError);
            btnChangePhoto = findViewById(R.id.btnChangePhoto);
            profileImageEdit = findViewById(R.id.profileImageEdit);
            tvErrorMessage = findViewById(R.id.tvErrorMessage);
            tvLoadingMessage = findViewById(R.id.tvLoadingMessage);
            tvHeaderTitle = findViewById(R.id.tvHeaderTitle);
            loadingOverlay = findViewById(R.id.loadingOverlay);
            errorCard = findViewById(R.id.errorCard);
            progressStep3 = findViewById(R.id.progressStep3);

            clearEditTextSpans(etLocation);
            clearEditTextSpans(etFarmSize);
            clearEditTextSpans(etFarmAddress);
            clearAutoCompleteSpans(etFarmType);
            clearEditTextSpans(etFarmName);
            clearEditTextSpans(etExperience);

            // Update header title based on user status and display name
            String displayName = authManager.getDisplayName();
            if (isNewUser) {
                tvHeaderTitle.setText("Jaza Wasifu Wako");
                btnSave.setText("Endelea");
            } else {
                if (displayName != null && !displayName.isEmpty()) {
                    tvHeaderTitle.setText("Wasifu wa " + displayName);
                } else {
                    tvHeaderTitle.setText("Hariri Wasifu Wako");
                }
                btnSave.setText("Hifadhi Mabadiliko");
            }

            etLocation.setHint("");
            etFarmSize.setHint("");
            etFarmAddress.setHint("");
            etFarmType.setHint("");
            etFarmSize.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
            tvErrorMessage.setVisibility(View.GONE);

            // Setup farm type dropdown
            ArrayAdapter<String> farmTypeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, farmTypeOptions);
            etFarmType.setAdapter(farmTypeAdapter);

            Log.d(TAG, "[LWENA27] " + getCurrentTime() + " - Views initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "[LWENA27] " + getCurrentTime() + " - Error initializing views: " + e.getMessage(), e);
            Toast.makeText(this, "Hitilafu katika kuanzisha UI", Toast.LENGTH_SHORT).show();
        }
    }

    private void clearEditTextSpans(TextInputEditText editText) {
        if (editText != null && editText.getText() instanceof SpannableStringBuilder) {
            SpannableStringBuilder spannable = (SpannableStringBuilder) editText.getText();
            Object[] spans = spannable.getSpans(0, spannable.length(), Object.class);
            for (Object span : spans) {
                spannable.removeSpan(span);
            }
        }
    }

    private void clearAutoCompleteSpans(AutoCompleteTextView autoCompleteTextView) {
        if (autoCompleteTextView != null && autoCompleteTextView.getText() instanceof SpannableStringBuilder) {
            SpannableStringBuilder spannable = (SpannableStringBuilder) autoCompleteTextView.getText();
            Object[] spans = spannable.getSpans(0, spannable.length(), Object.class);
            for (Object span : spans) {
                spannable.removeSpan(span);
            }
        }
    }

    private void loadProfileData() {
        Log.d(TAG, "[LWENA27] " + getCurrentTime() + " - Loading farmer profile data");

        currentFarmer = new Farmer();
        String userId = authManager.getUserId();
        String email = authManager.getUserEmail();
        String phone = authManager.getUserPhone();
        String displayName = authManager.getDisplayName();
        
        if (userId != null) {
            currentFarmer.setUserId(userId);
            Log.d(TAG, "Set userId: " + userId);
        }
        
        if (email != null) {
            currentFarmer.setEmail(email);
            Log.d(TAG, "Set email: " + email);
        }
        
        if (phone != null) {
            currentFarmer.setPhoneNumber(phone);
            Log.d(TAG, "Set phone: " + phone);
        }
        
        if (displayName != null) {
            currentFarmer.setFullName(displayName);
            Log.d(TAG, "Set full name: " + displayName);
        }

        // Load profile data based on user type
        if (!isNewUser) {
            // For existing users, try to load from database first
            Log.d(TAG, "Loading existing user profile from database...");
            loadFromDatabase();
        } else {
            // For new users, load any previously saved data from preferences
            Log.d(TAG, "Loading new user profile from preferences...");
            loadDefaultsFromPrefs();
        }
    }

    private void loadFromDatabase() {
        String userId = authManager.getUserId();
        String token = authManager.getAccessToken();

        if (userId == null || token == null) {
            Log.e(TAG, "[LWENA27] " + getCurrentTime() + " - Missing userId or token");
            loadDefaultsFromPrefs();
            showErrorMessage("Hakuna mtumiaji aliyeingia");
            return;
        }

        showLoading(true);
        String authHeader = "Bearer " + token;
        ApiClient.getApiService().getFarmerByUserId(authHeader, SupabaseConfig.getApiKeyHeader(), userId)
                .enqueue(new Callback<List<Farmer>>() {
                    @Override
                    public void onResponse(Call<List<Farmer>> call, Response<List<Farmer>> response) {
                        showLoading(false);
                        if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                            currentFarmer = response.body().get(0);
                            displayFarmerData(currentFarmer);
                            
                            // CRITICAL FIX: If we successfully loaded farmer data, mark profile as complete
                            if (currentFarmer.isProfileComplete()) {
                                authManager.markProfileComplete();
                                Log.d(TAG, "[LWENA27] " + getCurrentTime() + " - Profile marked as complete after loading from database");
                            }
                            
                            Log.d(TAG, "[LWENA27] " + getCurrentTime() + " - Loaded farmer data from API: " +
                                    currentFarmer.getFullName() + ", " + currentFarmer.getFarmLocation());
                        } else {
                            Log.w(TAG, "[LWENA27] " + getCurrentTime() + " - No farmer data found or request failed: HTTP " + response.code());
                            // Try fetching by email as fallback
                            loadFarmerByEmail(authHeader, currentFarmer.getEmail());
                        }
                    }

                    @Override
                    public void onFailure(Call<List<Farmer>> call, Throwable t) {
                        showLoading(false);
                        Log.e(TAG, "[LWENA27] " + getCurrentTime() + " - Error loading farmer data: " + t.getMessage());
                        Toast.makeText(FarmerProfileEditActivity.this, "Hitilafu: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                        loadDefaultsFromPrefs();
                        showErrorMessage("Hitilafu ya mtandao: " + t.getMessage());
                    }
                });
    }

    private void loadFarmerByEmail(String authHeader, String email) {
        if (email == null || email.isEmpty()) {
            Log.w(TAG, "[LWENA27] " + getCurrentTime() + " - No email available for fallback fetch");
            loadDefaultsFromPrefs();
            showErrorMessage("Tafadhali jaza wasifu wako");
            return;
        }
        showLoading(true);
        ApiClient.getApiService().getFarmerByEmail(authHeader, SupabaseConfig.getApiKeyHeader(), email)
                .enqueue(new Callback<List<Farmer>>() {
                    @Override
                    public void onResponse(Call<List<Farmer>> call, Response<List<Farmer>> response) {
                        showLoading(false);
                        if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                            currentFarmer = response.body().get(0);
                            displayFarmerData(currentFarmer);
                            
                            // CRITICAL FIX: If we successfully loaded farmer data, mark profile as complete
                            if (currentFarmer.isProfileComplete()) {
                                authManager.markProfileComplete();
                                Log.d(TAG, "[LWENA27] " + getCurrentTime() + " - Profile marked as complete after loading by email");
                            }
                            
                            Log.d(TAG, "[LWENA27] " + getCurrentTime() + " - Loaded farmer data by email: " +
                                    currentFarmer.getFullName() + ", " + currentFarmer.getFarmLocation());
                        } else {
                            Log.w(TAG, "[LWENA27] " + getCurrentTime() + " - No farmer data found by email: HTTP " + response.code());
                            loadDefaultsFromPrefs();
                            showErrorMessage("Tafadhali jaza wasifu wako");
                        }
                    }

                    @Override
                    public void onFailure(Call<List<Farmer>> call, Throwable t) {
                        showLoading(false);
                        Log.e(TAG, "[LWENA27] " + getCurrentTime() + " - Error loading farmer by email: " + t.getMessage());
                        Toast.makeText(FarmerProfileEditActivity.this, "Hitilafu: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                        loadDefaultsFromPrefs();
                        showErrorMessage("Hitilafu ya mtandao: " + t.getMessage());
                    }
                });
    }

    private void displayFarmerData(Farmer farmer) {
        Log.d(TAG, "[LWENA27] " + getCurrentTime() + " - Displaying farmer data in form fields");
        // Store a deep copy of the original data for change detection
        originalFarmerData = new Farmer();
        originalFarmerData.setUserId(farmer.getUserId());
        originalFarmerData.setFullName(farmer.getFullName());
        originalFarmerData.setFarmLocation(farmer.getFarmLocation());
        originalFarmerData.setBirdCount(farmer.getBirdCount());
        originalFarmerData.setFarmAddress(farmer.getFarmAddress());
        originalFarmerData.setBirdType(farmer.getBirdType());
        originalFarmerData.setEmail(farmer.getEmail());
        originalFarmerData.setPhoneNumber(farmer.getPhoneNumber());
        // ...add more fields if needed...

        // Display actual farmer data from database
        if (farmer.getFarmLocation() != null && !farmer.getFarmLocation().isEmpty()) {
            etLocation.setText(farmer.getFarmLocation());
            Log.d(TAG, "Loaded location: " + farmer.getFarmLocation());
        }
        
        if (farmer.getBirdCount() != null && farmer.getBirdCount() > 0) {
            etFarmSize.setText(String.valueOf(farmer.getBirdCount()));
            Log.d(TAG, "Loaded bird count: " + farmer.getBirdCount());
        }
        
        if (farmer.getFarmAddress() != null && !farmer.getFarmAddress().isEmpty()) {
            etFarmAddress.setText(farmer.getFarmAddress());
            Log.d(TAG, "Loaded farm address: " + farmer.getFarmAddress());
        }
        
        if (farmer.getBirdType() != null && !farmer.getBirdType().isEmpty()) {
            etFarmType.setText(farmer.getBirdType());
            Log.d(TAG, "Loaded bird type: " + farmer.getBirdType());
        }

        // Load additional data from SharedPreferences (for fields not in database model)
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String farmName = prefs.getString("farmName", "");
        String experience = prefs.getString("experience", "");

        if (!farmName.isEmpty()) {
            etFarmName.setText(farmName);
            Log.d(TAG, "Loaded farm name from prefs: " + farmName);
        }
        
        if (!experience.isEmpty()) {
            etExperience.setText(experience);
            Log.d(TAG, "Loaded experience from prefs: " + experience);
        }
        
        Log.d(TAG, "[LWENA27] " + getCurrentTime() + " - Successfully populated form with farmer data");
    }

    private void loadDefaultsFromPrefs() {
        Log.d(TAG, "[LWENA27] " + getCurrentTime() + " - Loading profile data from SharedPreferences");
        
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        
        // Load all available data from preferences
        String location = prefs.getString("location", "");
        int farmSize = prefs.getInt("farmSize", 0);
        String farmAddress = prefs.getString("farmAddress", "");
        String farmType = prefs.getString("farmType", "");
        String farmName = prefs.getString("farmName", "");
        String experience = prefs.getString("experience", "");
        
        // Also check AuthManager preferences for user display name
        String displayName = authManager.getDisplayName();
        String userEmail = authManager.getUserEmail();
        
        // Populate form fields with available data
        if (!location.isEmpty()) {
            etLocation.setText(location);
            Log.d(TAG, "Loaded location from prefs: " + location);
        }
        
        if (farmSize > 0) {
            etFarmSize.setText(String.valueOf(farmSize));
            Log.d(TAG, "Loaded farm size from prefs: " + farmSize);
        }
        
        if (!farmAddress.isEmpty()) {
            etFarmAddress.setText(farmAddress);
            Log.d(TAG, "Loaded farm address from prefs: " + farmAddress);
        }
        
        if (!farmType.isEmpty()) {
            etFarmType.setText(farmType);
            Log.d(TAG, "Loaded farm type from prefs: " + farmType);
        }
        
        if (!farmName.isEmpty()) {
            etFarmName.setText(farmName);
            Log.d(TAG, "Loaded farm name from prefs: " + farmName);
        } else if (displayName != null && !displayName.isEmpty()) {
            // Use display name as default farm name if no farm name is set
            String defaultFarmName = "Shamba la " + displayName;
            etFarmName.setText(defaultFarmName);
            Log.d(TAG, "Set default farm name: " + defaultFarmName);
        }
        
        if (!experience.isEmpty()) {
            etExperience.setText(experience);
            Log.d(TAG, "Loaded experience from prefs: " + experience);
        }

        Log.d(TAG, "[LWENA27] " + getCurrentTime() + " - Loaded data from prefs - Location: " +
                location + ", Farm Size: " + farmSize + ", Farm Address: " + farmAddress +
                ", Farm Type: " + farmType + ", Farm Name: " + farmName + ", Experience: " + experience +
                ", Display Name: " + displayName + ", Email: " + userEmail);
    }

    private void setupClickListeners() {
        btnSave.setOnClickListener(v -> saveProfileData());
        btnCancel.setOnClickListener(v -> {
            setResult(RESULT_CANCELED);
            finish();
        });
        btnBackEdit.setOnClickListener(v -> {
            if (isNewUser) {
                Toast.makeText(this, "Tafadhali kamilisha usajili kwanza", Toast.LENGTH_SHORT).show();
            } else {
                setResult(RESULT_CANCELED);
                finish();
            }
        });
        btnChangePhoto.setOnClickListener(v -> imagePickerLauncher.launch("image/*"));
        btnDismissError.setOnClickListener(v -> errorCard.setVisibility(View.GONE));
    }

    private void saveProfileData() {
        // PROTECTION: Check user type BEFORE saving
        String userType = authManager.getUserType();
        Log.d(TAG, "🔍 BEFORE PROFILE SAVE - User type: '" + userType + "'");

        if (userType == null || userType.trim().isEmpty()) {
            Log.e(TAG, "❌ USER TYPE NULL BEFORE SAVE! Fixing it now...");
            authManager.setUserType("farmer"); // Force it to farmer
        }

        String location = etLocation.getText().toString().trim();
        String farmSizeStr = etFarmSize.getText().toString().trim();
        String farmAddress = etFarmAddress.getText().toString().trim();
        String farmType = etFarmType.getText().toString().trim();
        String farmName = etFarmName.getText().toString().trim();
        String experience = etExperience.getText().toString().trim();

        if (location.isEmpty()) {
            etLocation.setError("Mahali pahitajika");
            etLocation.requestFocus();
            return;
        }
        if (farmSizeStr.isEmpty()) {
            etFarmSize.setError("Idadi ya kuku inahitajika");
            etFarmSize.requestFocus();
            return;
        }

        int farmSize;
        try {
            farmSize = Integer.parseInt(farmSizeStr);
            if (farmSize < 0) {
                etFarmSize.setError("Idadi ya kuku lazima iwe chanya");
                etFarmSize.requestFocus();
                return;
            }
        } catch (NumberFormatException e) {
            etFarmSize.setError("Tafadhali ingiza namba sahihi");
            etFarmSize.requestFocus();
            return;
        }

        showLoading(true);
        tvErrorMessage.setVisibility(View.GONE);

        // Get username - use display name, email, or prompt user
        String username = authManager.getDisplayName();
        if (username == null || username.trim().isEmpty()) {
            // Fallback to email username if display name is empty
            String email = authManager.getUserEmail();
            if (email != null && !email.isEmpty()) {
                username = email.split("@")[0]; // Use part before @ as username
            } else {
                username = "Mfugaji"; // Default fallback
            }
        }

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("username", username);
        editor.putString("farmerName", username); // Also save as farmerName for compatibility
        editor.putString("location", location);
        editor.putInt("farmSize", farmSize);
        editor.putString("farmAddress", farmAddress);
        editor.putString("farmType", farmType);
        editor.putString("farmName", farmName);
        editor.putString("experience", experience);
        editor.putBoolean(KEY_PROFILE_COMPLETE, true);
        boolean saved = editor.commit();

        if (!saved) {
            showLoading(false);
            Log.e(TAG, "[LWENA27] " + getCurrentTime() + " - Failed to save profile data to prefs");
            showErrorMessage("Hitilafu katika kuhifadhi data");
            Toast.makeText(this, "Hitilafu katika kuhifadhi data", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d(TAG, "[LWENA27] " + getCurrentTime() + " - Profile saved to prefs - Username: " +
                username + ", Location: " + location + ", Farm Size: " + farmSize +
                ", Farm Address: " + farmAddress + ", Farm Type: " + farmType +
                ", Farm Name: " + farmName + ", Experience: " + experience);

        SharedPreferences authPrefs = getSharedPreferences(AuthManager.PREFS_NAME, MODE_PRIVATE);
        authPrefs.edit().putBoolean(AuthManager.KEY_PROFILE_COMPLETE, true).apply();

        saveToDatabase(location, farmSize, farmAddress, farmType, farmName, experience, new DatabaseSaveCallback() {
            @Override
            public void onSuccess() {
                showLoading(false);

                // PROTECTION: Ensure user type is still valid after database save
                String userType = authManager.getUserType();
                Log.d(TAG, "🔍 AFTER DATABASE SAVE - User type: '" + userType + "'");

                if (userType == null || userType.trim().isEmpty()) {
                    Log.e(TAG, "❌ USER TYPE NULL AFTER SAVE! Fixing it now...");
                    authManager.setUserType("farmer"); // Force it to farmer
                }

                // Mark profile as complete after successful save
                authManager.markProfileComplete();

                // MORE PROTECTION: Log complete auth state
                Log.d(TAG, "🔍 Auth state after profile save:");
                Log.d(TAG, "  - Logged in: " + authManager.isLoggedIn());
                Log.d(TAG, "  - User type: '" + authManager.getUserType() + "'");
                Log.d(TAG, "  - User ID: " + authManager.getUserId());
                Log.d(TAG, "  - Session valid: " + authManager.isSessionValid());

                Toast.makeText(FarmerProfileEditActivity.this, "Wasifu umesasishwa kwa mafanikio", Toast.LENGTH_SHORT).show();
                Intent resultIntent = new Intent();
                resultIntent.putExtra(EXTRA_PROFILE_UPDATED, true);
                resultIntent.putExtra("updated_username", authManager.getDisplayName());
                resultIntent.putExtra("updated_location", location);
                resultIntent.putExtra("updated_farmSize", farmSize);
                setResult(RESULT_OK, resultIntent);
                if (isNewUser) {
                    Intent intent = new Intent(FarmerProfileEditActivity.this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                } else {
                    finish();
                }
            }

            @Override
            public void onError(String errorMessage) {
                showLoading(false);

                // PROTECTION: Ensure user type is still valid even on error
                String userType = authManager.getUserType();
                Log.d(TAG, "🔍 AFTER DATABASE ERROR - User type: '" + userType + "'");

                if (userType == null || userType.trim().isEmpty()) {
                    Log.e(TAG, "❌ USER TYPE NULL AFTER ERROR! Fixing it now...");
                    authManager.setUserType("farmer"); // Force it to farmer
                }

                // Still mark profile as complete even if database save failed (offline scenario)
                authManager.markProfileComplete();

                Log.e(TAG, "[LWENA27] " + getCurrentTime() + " - Error saving to database: " + errorMessage);
                showErrorMessage("Wasifu umehifadhiwa bila kuunganisha kwenye mtandao");
                Toast.makeText(FarmerProfileEditActivity.this, "Wasifu umehifadhiwa bila kuunganisha kwenye mtandao", Toast.LENGTH_SHORT).show();
                if (isNewUser) {
                    Intent intent = new Intent(FarmerProfileEditActivity.this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                } else {
                    finish();
                }
            }
        });
    }

    private void showErrorMessage(String message) {
        tvErrorMessage.setText(message);
        tvErrorMessage.setVisibility(View.VISIBLE);
        errorCard.setVisibility(View.VISIBLE);
    }

    private void showLoading(boolean isLoading) {
        btnSave.setEnabled(!isLoading);
        btnSave.setText(isLoading ? "Inahifadhi..." : (isNewUser ? "Endelea" : "Hifadhi Mabadiliko"));
        if (loadingOverlay != null) {
            loadingOverlay.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        }
    }

    interface DatabaseSaveCallback {
        void onSuccess();
        void onError(String errorMessage);
    }

    private void saveToDatabase(String location, int farmSize, String farmAddress, String farmType, String farmName, String experience, DatabaseSaveCallback callback) {
        try {
            String userType = authManager.getUserType();
            String userId = authManager.getUserId();
            String token = authManager.getAccessToken();
            if (userId == null || token == null) {
                Log.e(TAG, "[LWENA27] " + getCurrentTime() + " - Missing userId or token, can't save to DB");
                callback.onError("Missing authentication credentials");
                return;
            }
            String authHeader = "Bearer " + token;
            if (currentFarmer == null) {
                currentFarmer = new Farmer();
                currentFarmer.setUserId(userId);
            }
            currentFarmer.setFullName(authManager.getDisplayName());
            currentFarmer.setFarmLocation(location);
            currentFarmer.setBirdCount(farmSize);
            currentFarmer.setFarmAddress(farmAddress);
            currentFarmer.setBirdType(farmType);
            currentFarmer.setPassword(null);
            if (selectedImageUri != null) {
                Log.d(TAG, "[LWENA27] " + getCurrentTime() + " - Image URI set but not uploaded: " + selectedImageUri);
            }
            if (currentFarmer.getFarmerId() == null) {
                ApiClient.getApiService().createFarmer(authHeader, SupabaseConfig.getApiKeyHeader(), currentFarmer)
                        .enqueue(new Callback<List<Farmer>>() {
                            @Override
                            public void onResponse(Call<List<Farmer>> call, Response<List<Farmer>> response) {
                                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                                    currentFarmer = response.body().get(0);
                                    Log.d(TAG, "[LWENA27] " + getCurrentTime() + " - Farmer created in DB successfully");
                                    if (currentFarmer.getFarmerId() != null) {
                                        Log.d(TAG, "[LWENA27] " + getCurrentTime() + " - Received farmer ID: " + currentFarmer.getFarmerId());
                                        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
                                        prefs.edit().putString("farmerId", currentFarmer.getFarmerId()).apply();
                                    }
                                    callback.onSuccess();
                                } else {
                                    String errorBody = response.errorBody() != null ? response.errorBody().toString() : "Unknown error";
                                    if (response.code() == 409 && errorBody.contains("farmers_email_key")) {
                                        Log.d(TAG, "[LWENA27] " + getCurrentTime() + " - User already exists, trying to load existing farmer profile");
                                        loadExistingFarmerByEmail(authHeader, currentFarmer.getEmail(), callback);
                                    } else {
                                        Log.e(TAG, "[LWENA27] " + getCurrentTime() + " - Failed to create farmer: HTTP " + response.code() + ", " + errorBody);
                                        callback.onError("Server error: " + errorBody);
                                    }
                                }
                            }
                            @Override
                            public void onFailure(Call<List<Farmer>> call, Throwable t) {
                                Log.e(TAG, "[LWENA27] " + getCurrentTime() + " - Network error creating farmer: " + t.getMessage());
                                callback.onError("Network error: " + t.getMessage());
                            }
                        });
            } else {
                currentFarmer.setUserId(userId);
                currentFarmer.setFarmLocation(location);
                currentFarmer.setBirdCount(farmSize);
                currentFarmer.setFarmAddress(farmAddress);
                currentFarmer.setBirdType(farmType);
                currentFarmer.setPassword(null);
                ApiClient.getApiService().updateFarmer(authHeader, SupabaseConfig.getApiKeyHeader(), userId, currentFarmer)
                        .enqueue(new Callback<Farmer>() {
                            @Override
                            public void onResponse(Call<Farmer> call, Response<Farmer> response) {
                                if (response.isSuccessful()) {
                                    Log.d(TAG, "[LWENA27] " + getCurrentTime() + " - Farmer updated in DB successfully");
                                    SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
                                    prefs.edit().putBoolean(KEY_PROFILE_COMPLETE, true).apply();
                                    authManager.markProfileComplete();
                                    callback.onSuccess();
                                } else {
                                    String errorBody = "Unknown error";
                                    if (response.errorBody() != null) {
                                        try {
                                            errorBody = response.errorBody().string();
                                        } catch (Exception e) {
                                            errorBody = e.getMessage();
                                        }
                                    }
                                    Log.e(TAG, "[LWENA27] " + getCurrentTime() + " - Failed to update farmer: HTTP " + response.code() + ", " + errorBody);
                                    callback.onError("Server error: " + errorBody);
                                }
                            }

                            @Override
                            public void onFailure(Call<Farmer> call, Throwable t) {
                                Log.e(TAG, "[LWENA27] " + getCurrentTime() + " - Network error updating farmer: " + t.getMessage());
                                callback.onError("Network error: " + t.getMessage());
                            }
                        });
            }
        } catch (Exception e) {
            Log.e(TAG, "[LWENA27] " + getCurrentTime() + " - Exception in saveToDatabase: " + e.getMessage(), e);
            callback.onError("Exception: " + e.getMessage());
        }
    }

    private void loadExistingFarmerByEmail(String authHeader, String email, DatabaseSaveCallback callback) {
        ApiClient.getApiService().getFarmerByEmail(authHeader, SupabaseConfig.getApiKeyHeader(), email)
                .enqueue(new Callback<List<Farmer>>() {
                    @Override
                    public void onResponse(Call<List<Farmer>> call, Response<List<Farmer>> response) {
                        if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                            currentFarmer = response.body().get(0);
                            runOnUiThread(() -> displayFarmerData(currentFarmer));
                            Log.d(TAG, "[LWENA27] " + getCurrentTime() + " - Loaded existing farmer data by email: " +
                                    currentFarmer.getFullName() + ", " + currentFarmer.getFarmLocation());
                            callback.onSuccess();
                        } else {
                            Log.w(TAG, "[LWENA27] " + getCurrentTime() + " - No existing farmer data found by email or request failed: HTTP " + response.code());
                            callback.onError("Couldn't find existing profile");
                        }
                    }

                    @Override
                    public void onFailure(Call<List<Farmer>> call, Throwable t) {
                        Log.e(TAG, "[LWENA27] " + getCurrentTime() + " - Error loading existing farmer data by email: " + t.getMessage());
                        callback.onError("Network error: " + t.getMessage());
                    }
                });
    }

    private String getCurrentTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault());
        sdf.setTimeZone(TimeZone.getTimeZone("GMT+03:00"));
        return sdf.format(new Date());
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (etLocation != null) etLocation.clearFocus();
        if (etFarmSize != null) etFarmSize.clearFocus();
        if (etFarmAddress != null) etFarmAddress.clearFocus();
        if (etFarmType != null) etFarmType.clearFocus();
        if (etFarmName != null) etFarmName.clearFocus();
        if (etExperience != null) etExperience.clearFocus();
    }
}
