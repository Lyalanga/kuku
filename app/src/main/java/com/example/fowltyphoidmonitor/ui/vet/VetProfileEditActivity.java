package com.example.fowltyphoidmonitor.ui.vet;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.fowltyphoidmonitor.R;
import com.example.fowltyphoidmonitor.data.api.ApiClient;
import com.example.fowltyphoidmonitor.data.models.Vet;
import com.example.fowltyphoidmonitor.services.auth.AuthManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class VetProfileEditActivity extends AppCompatActivity {
    private static final String TAG = "VetProfileEditActivity";
    private static final String PREFS_NAME = "FowlTyphoidMonitorPrefs";
    private static final String KEY_PROFILE_COMPLETE = "isProfileComplete";

    private TextInputEditText etFullName, etLocation, etSpecialty, etExperience, etPhone, etEmail;
    private MaterialButton btnSave;
    private AuthManager authManager;
    private boolean isNewUser = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vet_profile_edit); // You need to create this layout

        authManager = AuthManager.getInstance(this);
        isNewUser = getIntent().getBooleanExtra("isNewUser", false);

        etFullName = findViewById(R.id.etFullName);
        etLocation = findViewById(R.id.etLocation);
        etSpecialty = findViewById(R.id.etSpecialty);
        etExperience = findViewById(R.id.etExperience);
        etPhone = findViewById(R.id.etPhone);
        etEmail = findViewById(R.id.etEmail);
        btnSave = findViewById(R.id.btnSave);

        btnSave.setOnClickListener(v -> saveProfileData());
        loadProfileData();
    }

    private void loadProfileData() {
        // Optionally load existing vet profile from API or SharedPreferences
        etFullName.setText(authManager.getDisplayName());
        etEmail.setText(authManager.getUserEmail());
        etPhone.setText(authManager.getUserPhone());
    }

    private void saveProfileData() {
        String userId = authManager.getUserId();
        String fullName = etFullName.getText().toString().trim();
        String location = etLocation.getText().toString().trim();
        String specialty = etSpecialty.getText().toString().trim();
        String experienceStr = etExperience.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        Integer experience = experienceStr.isEmpty() ? null : Integer.valueOf(experienceStr);

        Vet vet = new Vet();
        vet.setUserId(userId);
        vet.setFullName(fullName);
        vet.setLocation(location);
        vet.setSpecialty(specialty);
        vet.setExperienceYears(experience);
        vet.setPhoneNumber(phone);
        vet.setEmail(email);

        String token = authManager.getAccessToken();
        if (token == null) {
            Toast.makeText(this, "Missing authentication token.", Toast.LENGTH_SHORT).show();
            return;
        }
        String authHeader = "Bearer " + token;
        String apiKey = com.example.fowltyphoidmonitor.config.SupabaseConfig.getApiKeyHeader();

        ApiClient.getApiService().createVet(authHeader, apiKey, vet).enqueue(new Callback<Vet>() {
            @Override
            public void onResponse(Call<Vet> call, Response<Vet> response) {
                if (response.isSuccessful()) {
                    SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
                    prefs.edit().putBoolean(KEY_PROFILE_COMPLETE, true).apply();
                    authManager.markProfileComplete();
                    Toast.makeText(VetProfileEditActivity.this, "Profile saved!", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK, new Intent());
                    finish();
                } else {
                    Toast.makeText(VetProfileEditActivity.this, "Failed to save profile.", Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(Call<Vet> call, Throwable t) {
                Toast.makeText(VetProfileEditActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
