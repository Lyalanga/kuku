package com.example.fowltyphoidmonitor.ui.common;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Html;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.fowltyphoidmonitor.R;
import com.example.fowltyphoidmonitor.data.api.DiseaseInfoApi;
import com.example.fowltyphoidmonitor.data.api.SupabaseClient;
import com.example.fowltyphoidmonitor.data.models.DiseaseInfo;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DiseaseInfoActivity extends AppCompatActivity {

    private static final String TAG = "DiseaseInfoActivity";
    private static final String PREFS_NAME = "DiseaseInfoPrefs";
    private static final String[] CATEGORIES = {"causes", "symptoms", "treatment", "prevention"};
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

    private TextView btnCauses, btnSymptoms, btnTreatment, btnPrevention, txtInfo;
    private ImageView imgCategoryIcon;
    private LinearLayout layoutContent;

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
        layoutContent = findViewById(R.id.layoutContent);

        // Enable scrolling for text content
        txtInfo.setMovementMethod(new ScrollingMovementMethod());

        // Set click listeners
        btnCauses.setOnClickListener(v -> switchCategory("causes"));
        btnSymptoms.setOnClickListener(v -> switchCategory("symptoms"));
        btnTreatment.setOnClickListener(v -> switchCategory("treatment"));
        btnPrevention.setOnClickListener(v -> switchCategory("prevention"));

        // Initialize DiseaseInfoApi using createApi
        diseaseInfoApi = SupabaseClient.getInstance(this).createApi(DiseaseInfoApi.class);

        // Load initial category
        switchCategory(currentCategory);
        logInfo("Activity initialized at " + getCurrentTimestamp());
    }

    private void switchCategory(String category) {
        currentCategory = category;
        resetButtonBackgrounds();
        updateButtonState(category);

        // Fetch from backend with Supabase filter
        String filter = "eq.category." + category;
        Call<List<DiseaseInfo>> call = diseaseInfoApi.getDiseaseInfoByCategory(filter);
        call.enqueue(new Callback<List<DiseaseInfo>>() {
            @Override
            public void onResponse(Call<List<DiseaseInfo>> call, Response<List<DiseaseInfo>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    DiseaseInfo info = response.body().get(0);
                    String content = info.getContent() != null ? info.getContent() : "";
                    txtInfo.setText(content.isEmpty() ? "Hakuna taarifa kwa sasa." : Html.fromHtml(content));
                    SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
                    prefs.edit().putString(category, content).apply();
                    logInfo("Loaded category " + category + " from API at " + getCurrentTimestamp());
                } else {
                    logWarning("API response unsuccessful or empty for " + category + ": " + response.code());
                    loadFromPrefs(category);
                }
            }

            @Override
            public void onFailure(Call<List<DiseaseInfo>> call, Throwable t) {
                logError("API failure for " + category + ": " + t.getMessage());
                loadFromPrefs(category);
            }
        });

        // Update icon (using placeholders until drawables are added)
        updateCategoryIcon(category);
    }

    private void loadFromPrefs(String category) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String info = prefs.getString(category, "");
        txtInfo.setText(info.isEmpty() ? "Hakuna taarifa kwa sasa." : Html.fromHtml(info));
        logInfo("Loaded category " + category + " from prefs at " + getCurrentTimestamp());
    }

    private void resetButtonBackgrounds() {
        btnCauses.setBackgroundResource(R.drawable.button_normal);
        btnSymptoms.setBackgroundResource(R.drawable.button_normal);
        btnTreatment.setBackgroundResource(R.drawable.button_normal);
        btnPrevention.setBackgroundResource(R.drawable.button_normal);
    }

    private void updateButtonState(String category) {
        int buttonRes = R.drawable.button_selected;
        switch (category) {
            case "causes":
                btnCauses.setBackgroundResource(buttonRes);
                break;
            case "symptoms":
                btnSymptoms.setBackgroundResource(buttonRes);
                break;
            case "treatment":
                btnTreatment.setBackgroundResource(buttonRes);
                break;
            case "prevention":
                btnPrevention.setBackgroundResource(buttonRes);
                break;
        }
    }

    private void updateCategoryIcon(String category) {
        // Placeholder until drawables are created
        int iconRes = android.R.drawable.ic_menu_gallery; // Default system icon
        switch (category) {
            case "causes":
                iconRes = android.R.drawable.ic_menu_gallery; // Replace with ic_causes
                break;
            case "symptoms":
                iconRes = android.R.drawable.ic_menu_gallery; // Replace with ic_symptoms
                break;
            case "treatment":
                iconRes = android.R.drawable.ic_menu_gallery; // Replace with ic_treatment
                break;
            case "prevention":
                iconRes = android.R.drawable.ic_menu_gallery; // Replace with ic_prevention
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