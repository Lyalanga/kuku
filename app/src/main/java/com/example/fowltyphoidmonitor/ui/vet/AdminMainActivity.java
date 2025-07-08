package com.example.fowltyphoidmonitor.ui.vet;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.fowltyphoidmonitor.data.models.Vet;
import com.example.fowltyphoidmonitor.services.auth.AuthManager;
import com.example.fowltyphoidmonitor.ui.vet.AdminProfileEditActivity;
import com.example.fowltyphoidmonitor.ui.vet.AdminSettingsActivity;
import com.example.fowltyphoidmonitor.ui.auth.LoginActivity;
import com.example.fowltyphoidmonitor.ui.vet.ManageDiseaseInfoActivity;
import com.example.fowltyphoidmonitor.R;
import com.example.fowltyphoidmonitor.ui.vet.ReportAnalyticsActivity;
import com.example.fowltyphoidmonitor.ui.vet.AdminConsultationActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import de.hdodenhof.circleimageview.CircleImageView;

import java.text.DateFormat;
import java.util.Date;
import java.util.Map;

public class AdminMainActivity extends AppCompatActivity {

    private static final String TAG = "AdminMainActivity";
    private static final int DASHBOARD_UPDATE_INTERVAL = 30000; // 30 seconds
    private static final int REQUEST_CODE_EDIT_PROFILE = 2001;

    // UI Elements
    private TextView txtAdminName, txtSpecialization, txtLocation;
    private TextView txtTotalFarmers, txtActiveReports, txtPendingConsultations;
    private TextView txtLastUpdated, txtWelcomeMessage;
    private CircleImageView adminProfileImage;
    private ImageButton btnBack;
    private MaterialButton btnEditProfile, btnConsultations, btnSendAlerts;
    private MaterialButton btnManageUsers, btnManageInformation, btnReportAnalytics;
    private MaterialButton btnViewReports, btnSetReminders, btnDashboardManager;
    private MaterialButton btnSettings, btnLogout;
    private BottomNavigationView bottomNavigation;
    private SwipeRefreshLayout swipeRefreshLayout;

    // AuthManager instance
    private AuthManager authManager;

    // Real-time dashboard update handler
    private Handler dashboardUpdateHandler;
    private Runnable dashboardUpdateRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "[LWENA27] AdminMainActivity onCreate started");

        // Initialize AuthManager
        authManager = AuthManager.getInstance(this);
        if (!authManager.isLoggedIn() || !authManager.isVet()) {
            Log.d(TAG, "[LWENA27] User not logged in or not a vet, redirecting to login");
            redirectToLogin();
            finish();
            return;
        }

        setContentView(R.layout.activity_admin_main);
        Log.d(TAG, "[LWENA27] Layout set successfully");

        // Initialize views
        initializeViews();

        // Set up click listeners
        setupClickListeners();

        // Set up bottom navigation
        setupBottomNavigation();

        // Set up pull-to-refresh
        setupSwipeRefresh();

        // Load user data and dashboard stats
        loadUserData();
        loadDashboardStats();

        // Initialize real-time dashboard updates
        initializeDashboardUpdates();
        startDashboardUpdates();

        Log.d(TAG, "[LWENA27] Activity created successfully for vet user");
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!authManager.isLoggedIn() || !authManager.isVet()) {
            Log.w(TAG, "User not logged in or not a vet, redirecting to login");
            redirectToLogin();
            return;
        }

        authManager.autoRefreshIfNeeded(new AuthManager.AuthCallback() {
            @Override
            public void onSuccess(com.example.fowltyphoidmonitor.data.requests.AuthResponse response) {
                Log.d(TAG, "Token refresh successful, reloading data");
                loadUserData();
                loadDashboardStats();
                startDashboardUpdates();
            }

            @Override
            public void onError(String error) {
                Log.w(TAG, "Token refresh failed: " + error + ", continuing with session");
                loadUserData();
                loadDashboardStats();
                if (!authManager.isLoggedIn()) {
                    Toast.makeText(AdminMainActivity.this, "Session expired. Please log in again.", Toast.LENGTH_SHORT).show();
                    redirectToLogin();
                }
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopDashboardUpdates();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopDashboardUpdates();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_EDIT_PROFILE && resultCode == RESULT_OK && data != null) {
            if (data.getBooleanExtra("EXTRA_PROFILE_UPDATED", false)) {
                Log.d(TAG, "Profile updated, reloading data");
                loadUserData();
                Toast.makeText(this, "Wasifu umesasishwa", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void initializeViews() {
        txtAdminName = findViewById(R.id.txtAdminName);
        txtSpecialization = findViewById(R.id.txtSpecialization);
        txtLocation = findViewById(R.id.txtAdminLocation);
        txtTotalFarmers = findViewById(R.id.txtTotalFarmers);
        txtActiveReports = findViewById(R.id.txtActiveReports);
        txtPendingConsultations = findViewById(R.id.txtPendingConsultations);
        txtLastUpdated = findViewById(R.id.txtLastUpdated);
        txtWelcomeMessage = findViewById(R.id.txtWelcomeMessage);
        adminProfileImage = findViewById(R.id.adminProfileImage);
        btnBack = findViewById(R.id.btnBack);
        btnEditProfile = findViewById(R.id.btnEditProfile);
        btnConsultations = findViewById(R.id.btnConsultations);
        btnSendAlerts = findViewById(R.id.btnSendAlerts);
        btnManageUsers = findViewById(R.id.btnManageUsers);
        btnManageInformation = findViewById(R.id.btnManageInformation);
        btnReportAnalytics = findViewById(R.id.btnReportAnalytics);
        btnViewReports = findViewById(R.id.btnViewReports);
        btnSetReminders = findViewById(R.id.btnSetReminders);
        btnDashboardManager = findViewById(R.id.btnDashboardManager);
        btnSettings = findViewById(R.id.btnSettings);
        btnLogout = findViewById(R.id.btnLogout);
        bottomNavigation = findViewById(R.id.bottomNavigation);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
    }

    private void setupSwipeRefresh() {
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setOnRefreshListener(() -> {
                loadUserData();
                loadDashboardStats();
                swipeRefreshLayout.setRefreshing(false);
                Toast.makeText(this, "Dashboard imesasishwa", Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void setupClickListeners() {
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> onBackPressed());
        }

        if (btnEditProfile != null) {
            btnEditProfile.setOnClickListener(v -> {
                try {
                    Intent intent = new Intent(this, AdminProfileEditActivity.class);
                    startActivityForResult(intent, REQUEST_CODE_EDIT_PROFILE);
                } catch (Exception e) {
                    Log.e(TAG, "Error navigating to profile edit: " + e.getMessage());
                }
            });
        }

        if (btnConsultations != null) {
            btnConsultations.setOnClickListener(v -> {
                // Restore original behavior for Mazungumzo button
                Intent intent = new Intent(this, VetConsultationInboxActivity.class);
                startActivity(intent);
            });
        }

        if (btnSendAlerts != null) {
            btnSendAlerts.setOnClickListener(v -> navigateToActivity(com.example.fowltyphoidmonitor.ui.common.NotificationsActivity.class));
        }

        if (btnManageUsers != null) {
            btnManageUsers.setOnClickListener(v -> navigateToActivity(ManageUsersActivity.class));
        }

        if (btnManageInformation != null) {
            btnManageInformation.setOnClickListener(v -> navigateToActivity(ManageDiseaseInfoActivity.class));
        }

        if (btnReportAnalytics != null) {
            btnReportAnalytics.setOnClickListener(v -> navigateToActivity(ReportAnalyticsActivity.class));
        }

        if (btnViewReports != null) {
            btnViewReports.setOnClickListener(v -> navigateToActivity(AdminConsultationActivity.class));
        }

        if (btnSetReminders != null) {
            btnSetReminders.setOnClickListener(v -> navigateToActivity(com.example.fowltyphoidmonitor.ui.common.SetRemindersActivity.class));
        }

        if (btnDashboardManager != null) {
            btnDashboardManager.setOnClickListener(v -> navigateToActivity(com.example.fowltyphoidmonitor.ui.common.DashboardManagerActivity.class));
        }

        if (btnSettings != null) {
            btnSettings.setOnClickListener(v -> navigateToActivity(AdminSettingsActivity.class));
        }

        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> userLogout());
        }

        // Professional Tools click listeners (available to both admins and vets)
        // Dashboard Manager
        if (btnDashboardManager != null) {
            btnDashboardManager.setOnClickListener(v -> {
                navigateToActivity(com.example.fowltyphoidmonitor.ui.common.DashboardManagerActivity.class, "DashboardManager");
            });
        }
        // User Management
        if (btnManageUsers != null) {
            btnManageUsers.setOnClickListener(v -> {
                navigateToActivity(com.example.fowltyphoidmonitor.ui.vet.ManageUsersActivity.class, "ManageUsers");
            });
        }
        // Alert Manager (Send Alerts)
        if (btnSendAlerts != null) {
            btnSendAlerts.setOnClickListener(v -> {
                navigateToActivity(com.example.fowltyphoidmonitor.ui.common.NotificationsActivity.class, "Notifications");
            });
        }
        // Report Analytics
        if (btnReportAnalytics != null) {
            btnReportAnalytics.setOnClickListener(v -> {
                navigateToActivity(ReportAnalyticsActivity.class, "ReportAnalytics");
            });
        }
        // Manage Disease Information
        if (btnManageInformation != null) {
            btnManageInformation.setOnClickListener(v -> {
                navigateToActivity(ManageDiseaseInfoActivity.class, "ManageDiseaseInfo");
            });
        }
        // Set Reminders
        if (btnSetReminders != null) {
            btnSetReminders.setOnClickListener(v -> {
                navigateToActivity(com.example.fowltyphoidmonitor.ui.common.SetRemindersActivity.class, "SetReminders");
            });
        }
        // View Reports
        if (btnViewReports != null) {
            btnViewReports.setOnClickListener(v -> {
                navigateToActivity(AdminConsultationActivity.class, "AdminConsultation");
            });
        }
        // Manage Consultations
        if (btnConsultations != null) {
            btnConsultations.setOnClickListener(v -> {
                navigateToActivity(com.example.fowltyphoidmonitor.ui.vet.VetConsultationInboxActivity.class, "VetConsultationInbox");
            });
        }
    }

    private void setupBottomNavigation() {
        if (bottomNavigation != null) {
            bottomNavigation.setOnItemSelectedListener(item -> {
                int itemId = item.getItemId();
                if (itemId == R.id.navigation_home) {
                    return true;
                } else if (itemId == R.id.navigation_consultations) {
                    navigateToActivity(AdminConsultationActivity.class);
                    return true;
                } else if (itemId == R.id.navigation_reports) {
                    navigateToActivity(AdminConsultationActivity.class);
                    return true;
                } else if (itemId == R.id.navigation_settings) {
                    navigateToActivity(AdminSettingsActivity.class);
                    return true;
                }
                return false;
            });
        }
    }

    private void initializeDashboardUpdates() {
        dashboardUpdateHandler = new Handler(Looper.getMainLooper());
        dashboardUpdateRunnable = new Runnable() {
            @Override
            public void run() {
                loadDashboardStats();
                updateLastUpdatedTime();
                dashboardUpdateHandler.postDelayed(this, DASHBOARD_UPDATE_INTERVAL);
            }
        };
    }

    private void startDashboardUpdates() {
        if (dashboardUpdateHandler != null && dashboardUpdateRunnable != null) {
            dashboardUpdateHandler.post(dashboardUpdateRunnable);
            updateLastUpdatedTime(); // Initialize with current time
        }
    }

    private void stopDashboardUpdates() {
        if (dashboardUpdateHandler != null && dashboardUpdateRunnable != null) {
            dashboardUpdateHandler.removeCallbacks(dashboardUpdateRunnable);
        }
    }

    private void updateLastUpdatedTime() {
        if (txtLastUpdated != null) {
            String currentTime = DateFormat.getTimeInstance(DateFormat.SHORT).format(new Date());
            txtLastUpdated.setText("Imesasishwa: " + currentTime);
        }
    }

    private void loadUserData() {
        setLoading(true);
        authManager.loadUserProfile(new AuthManager.ProfileCallback() {
            @Override
            public void onProfileLoaded(Map<String, Object> profile) {
                setLoading(false);
                if (profile != null && (profile.get("userType").equals("vet") || profile.get("userType").equals("admin"))) {
                    Vet vet = createVetFromProfile(profile);
                    displayVetData(vet);
                } else {
                    Log.e(TAG, "Unexpected user type or null profile");
                    Toast.makeText(AdminMainActivity.this, "Invalid user type", Toast.LENGTH_SHORT).show();
                    redirectToLogin();
                }
            }

            @Override
            public void onError(String error) {
                setLoading(false);
                Log.e(TAG, "Error loading profile: " + error);
                Toast.makeText(AdminMainActivity.this, "Error loading profile", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void displayVetData(Vet vet) {
        if (txtAdminName != null) txtAdminName.setText(vet.getFullName() != null ? vet.getFullName() : "Daktari");
        if (txtSpecialization != null) txtSpecialization.setText("Utaalamu: " + (vet.getSpecialization() != null ? vet.getSpecialization() : "Daktari wa Mifugo"));
        if (txtLocation != null) txtLocation.setText("Eneo: " + (vet.getLocation() != null ? vet.getLocation() : "Haijawekwa"));
    }

    private Vet createVetFromProfile(Map<String, Object> profile) {
        Vet vet = new Vet();
        vet.setUserId((String) profile.get("user_id"));
        vet.setEmail((String) profile.get("email"));
        vet.setFullName((String) profile.get("display_name"));
        vet.setPhoneNumber((String) profile.get("phone"));
        vet.setSpecialization((String) profile.get("specialization"));
        vet.setLocation((String) profile.get("location"));
        if (profile.get("years_experience") != null) {
            try {
                vet.setYearsExperience(Integer.parseInt(profile.get("years_experience").toString()));
            } catch (NumberFormatException e) {
                Log.w(TAG, "Invalid years experience format");
            }
        }
        return vet;
    }

    private void loadDashboardStats() {
        setLoading(true);
        authManager.loadDashboardStats(new AuthManager.StatsCallback() {
            @Override
            public void onStatsLoaded(Map<String, Object> stats) {
                setLoading(false);
                if (txtTotalFarmers != null) txtTotalFarmers.setText(String.valueOf(getStatValue(stats, "total_farmers", 0)));
                if (txtActiveReports != null) txtActiveReports.setText(String.valueOf(getStatValue(stats, "active_reports", 0)));
                if (txtPendingConsultations != null) txtPendingConsultations.setText(String.valueOf(getStatValue(stats, "pending_consultations", 0)));
                updateLastUpdatedTime();
            }

            @Override
            public void onError(String error) {
                setLoading(false);
                Log.e(TAG, "Error loading dashboard stats: " + error);
                if (txtTotalFarmers != null) txtTotalFarmers.setText("0");
                if (txtActiveReports != null) txtActiveReports.setText("0");
                if (txtPendingConsultations != null) txtPendingConsultations.setText("0");
                Toast.makeText(AdminMainActivity.this, "Error loading dashboard statistics", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private int getStatValue(Map<String, Object> stats, String key, int defaultValue) {
        Object value = stats.get(key);
        if (value instanceof Integer) return (Integer) value;
        if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    private void navigateToActivity(Class<?> targetActivity) {
        try {
            startActivity(new Intent(this, targetActivity));
        } catch (Exception e) {
            Log.e(TAG, "Error navigating: " + e.getMessage());
            Toast.makeText(this, "Kitu kimekosa", Toast.LENGTH_SHORT).show();
        }
    }

    private void navigateToActivity(Class<?> targetActivity, String extraKey) {
        try {
            Intent intent = new Intent(this, targetActivity);
            intent.putExtra("EXTRA_KEY", extraKey);
            startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "Error navigating with extra: " + e.getMessage());
            Toast.makeText(this, "Kitu kimekosa", Toast.LENGTH_SHORT).show();
        }
    }

    private void userLogout() {
        authManager.logout(new AuthManager.AuthCallback() {
            @Override
            public void onSuccess(com.example.fowltyphoidmonitor.data.requests.AuthResponse response) {
                stopDashboardUpdates();
                redirectToLogin();
            }

            @Override
            public void onError(String error) {
                stopDashboardUpdates();
                redirectToLogin();
            }
        });
    }

    private void redirectToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.putExtra("userType", "vet");
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void setLoading(boolean isLoading) {
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setRefreshing(isLoading);
        }
    }
}
