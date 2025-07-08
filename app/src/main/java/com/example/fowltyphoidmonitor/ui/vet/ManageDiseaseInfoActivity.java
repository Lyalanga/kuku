package com.example.fowltyphoidmonitor.ui.vet;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.fowltyphoidmonitor.R;
import com.example.fowltyphoidmonitor.data.api.DiseaseInfoApi; // Updated package
import com.example.fowltyphoidmonitor.data.api.SupabaseClient;
import com.example.fowltyphoidmonitor.data.models.DiseaseInfo;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ManageDiseaseInfoActivity extends AppCompatActivity {

    private static final String TAG = "ManageDiseaseInfoActivity";
    private static final String PREFS_NAME = "DiseaseInfoPrefs";
    private static final String[] CATEGORIES = {"causes", "symptoms", "treatment", "prevention"};
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

    private TextView btnCauses, btnSymptoms, btnTreatment, btnPrevention, txtInfo;
    private ImageView imgCategoryIcon;
    private EditText editInfo;
    private Button btnSaveInfo;

    private String currentCategory = CATEGORIES[0];
    private DiseaseInfoApi diseaseInfoApi;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_disease_info);

        // Initialize views
        btnCauses = findViewById(R.id.btnCauses);
        btnSymptoms = findViewById(R.id.btnSymptoms);
        btnTreatment = findViewById(R.id.btnTreatment);
        btnPrevention = findViewById(R.id.btnPrevention);
        txtInfo = findViewById(R.id.txtInfo);
        imgCategoryIcon = findViewById(R.id.imgCategoryIcon);
        editInfo = findViewById(R.id.editInfo);
        btnSaveInfo = findViewById(R.id.btnSaveInfo);

        // Show edit fields for vet
        editInfo.setVisibility(View.VISIBLE);
        btnSaveInfo.setVisibility(View.VISIBLE);

        // Set click listeners
        btnCauses.setOnClickListener(v -> switchCategory("causes"));
        btnSymptoms.setOnClickListener(v -> switchCategory("symptoms"));
        btnTreatment.setOnClickListener(v -> switchCategory("treatment"));
        btnPrevention.setOnClickListener(v -> switchCategory("prevention"));
        btnSaveInfo.setOnClickListener(v -> saveInfo());

        // Initialize DiseaseInfoApi using createApi
        diseaseInfoApi = SupabaseClient.getInstance(this).createApi(DiseaseInfoApi.class);

        // Load initial category
        switchCategory(currentCategory);
        logInfo("Activity initialized at " + getCurrentTimestamp());
    }

    private void switchCategory(String category) {
        currentCategory = category;
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String info = prefs.getString(category, "");
        txtInfo.setText(info);
        editInfo.setText(info);
        updateCategoryIcon(category);
        logInfo("Switched to category " + category + " at " + getCurrentTimestamp());
    }

    private void saveInfo() {
        String info = editInfo.getText().toString().trim();
        if (info.isEmpty()) {
            Toast.makeText(this, "Taarifa hazipaswi kuwa tupu!", Toast.LENGTH_SHORT).show();
            return;
        }

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        prefs.edit().putString(currentCategory, info).apply();
        txtInfo.setText(info);
        Toast.makeText(this, "Taarifa imehifadhiwa!", Toast.LENGTH_SHORT).show();

        // Push to backend
        DiseaseInfo diseaseInfo = new DiseaseInfo();
        diseaseInfo.setCategory(currentCategory);
        diseaseInfo.setContent(info);
        diseaseInfoApi.saveDiseaseInfo(diseaseInfo).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(ManageDiseaseInfoActivity.this, "Saved to backend!", Toast.LENGTH_SHORT).show();
                    logInfo("Saved category " + currentCategory + " to backend at " + getCurrentTimestamp());
                } else {
                    Toast.makeText(ManageDiseaseInfoActivity.this, "Backend save failed: " + response.code(), Toast.LENGTH_SHORT).show();
                    logWarning("Backend save failed for " + currentCategory + ": " + response.code());
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(ManageDiseaseInfoActivity.this, "Backend unavailable: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                logError("Backend failure for " + currentCategory + ": " + t.getMessage());
            }
        });
    }

    private void updateCategoryIcon(String category) {
        int iconRes = R.drawable.ic_default;
        switch (category) {
            case "causes":
                iconRes = R.drawable.ic_causes;
                break;
            case "symptoms":
                iconRes = R.drawable.ic_symptoms;
                break;
            case "treatment":
                iconRes = R.drawable.ic_treatment;
                break;
            case "prevention":
                iconRes = R.drawable.ic_prevention;
                break;
        }
        imgCategoryIcon.setImageResource(iconRes);
    }

    private void logInfo(String message) {
        Log.i(TAG, "[" + getCurrentTimestamp() + "] " + message);
    }

    private void logWarning(String message) {
        Log.w(TAG, "[" + getCurrentTimestamp() + "] " + message);
    }

    private void logError(String message) {
        Log.e(TAG, "[" + getCurrentTimestamp() + "] " + message);
    }

    private String getCurrentTimestamp() {
        return DATE_FORMAT.format(new Date());
    }
}