package com.example.fowltyphoidmonitor.ui.farmer;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.fowltyphoidmonitor.R;
import com.example.fowltyphoidmonitor.services.auth.AuthManager;
import com.example.fowltyphoidmonitor.ui.auth.LoginActivity;
import com.example.fowltyphoidmonitor.ui.vet.ConsultationInboxItem;
import com.example.fowltyphoidmonitor.ui.vet.VetConsultationInboxAdapter;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * FarmerConsultationInboxActivity - Inbox for farmers to chat with vets/admins
 */
public class FarmerConsultationInboxActivity extends AppCompatActivity {
    private static final String TAG = "FarmerConsultationInbox";
    private Toolbar toolbar;
    private ImageButton btnBack;
    private TextView txtTitle, txtUserInfo;
    private SwipeRefreshLayout swipeRefreshLayout;
    private RecyclerView recyclerViewConsultations;
    private LinearLayout emptyStateLayout;
    private TextView txtEmptyMessage;
    private MaterialButton btnRefreshEmpty;
    private FloatingActionButton fabComposeMessage;

    private List<ConsultationInboxItem> consultationList;
    private VetConsultationInboxAdapter adapter;
    private AuthManager authManager;
    private ExecutorService executorService;
    private Handler mainHandler;
    private String currentFarmerId;
    private String currentFarmerName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            authManager = AuthManager.getInstance(this);
            if (!authManager.isLoggedIn() || !authManager.isFarmer()) {
                Log.d(TAG, "Unauthorized access, redirecting to login");
                redirectToLogin();
                return;
            }
            setContentView(R.layout.activity_vet_consultation_inbox);
            initializeComponents();
            initializeViews();
            setupToolbar();
            setupRecyclerView();
            setupClickListeners();
            loadConsultationInbox();
        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate: " + e.getMessage(), e);
            Toast.makeText(this, "Hitilafu wakati wa kuanza programu", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void initializeComponents() {
        executorService = Executors.newCachedThreadPool();
        mainHandler = new Handler(Looper.getMainLooper());
        currentFarmerId = authManager.getUserId();
        currentFarmerName = authManager.getUserEmail(); // Or get actual name from profile
        consultationList = new ArrayList<>();
    }

    private void initializeViews() {
        toolbar = findViewById(R.id.toolbar);
        btnBack = findViewById(R.id.btnBack);
        txtTitle = findViewById(R.id.txtTitle);
        txtUserInfo = findViewById(R.id.txtUserInfo);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        recyclerViewConsultations = findViewById(R.id.recyclerViewConsultations);
        emptyStateLayout = findViewById(R.id.emptyStateLayout);
        txtEmptyMessage = findViewById(R.id.txtEmptyMessage);
        btnRefreshEmpty = findViewById(R.id.btnRefreshEmpty);
        fabComposeMessage = findViewById(R.id.fabComposeMessage);
    }

    private void setupToolbar() {
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            txtTitle.setText("Inbox ya Ushauri");
            txtUserInfo.setText("Karibu, " + currentFarmerName);
            btnBack.setOnClickListener(v -> finish());
        }
    }

    private void setupRecyclerView() {
        adapter = new VetConsultationInboxAdapter(consultationList, item -> openChat(item));
        recyclerViewConsultations.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewConsultations.setAdapter(adapter);
    }

    private void setupClickListeners() {
        swipeRefreshLayout.setOnRefreshListener(this::loadConsultationInbox);
        btnRefreshEmpty.setOnClickListener(v -> loadConsultationInbox());
        fabComposeMessage.setOnClickListener(v -> composeNewConsultation());
    }

    private void loadConsultationInbox() {
        swipeRefreshLayout.setRefreshing(true);
        executorService.execute(() -> {
            // TODO: Load only consultations for this farmer from backend/database
            // For now, mock data or reuse vet logic with farmer filter
            mainHandler.post(() -> {
                swipeRefreshLayout.setRefreshing(false);
                adapter.notifyDataSetChanged();
                emptyStateLayout.setVisibility(consultationList.isEmpty() ? View.VISIBLE : View.GONE);
            });
        });
    }

    private void openChat(ConsultationInboxItem item) {
        // TODO: Open chat activity for this consultation
        Toast.makeText(this, "Fungua chat: " + item.getQuestion(), Toast.LENGTH_SHORT).show();
    }

    private void composeNewConsultation() {
        // TODO: Optionally allow new consultation from inbox
    }

    private void redirectToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
