package com.example.fowltyphoidmonitor.ui.common;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.fowltyphoidmonitor.R;
import com.example.fowltyphoidmonitor.services.supabase.SupabaseReminderService;
import com.example.fowltyphoidmonitor.services.supabase.model.Reminder;

import java.util.ArrayList;
import java.util.List;

public class ReminderActivity extends AppCompatActivity {
    private ListView reminderListView;
    private ProgressBar progressBar;
    private ArrayAdapter<String> adapter;
    private ArrayList<String> reminderList;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reminder);

        reminderListView = findViewById(R.id.reminderListView);
        progressBar = findViewById(R.id.progressBar);
        reminderList = new ArrayList<>();
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, reminderList);
        reminderListView.setAdapter(adapter);

        fetchReminders();
    }

    private void fetchReminders() {
        progressBar.setVisibility(View.VISIBLE);
        SupabaseReminderService.fetchRemindersForFarmer(new SupabaseReminderService.ReminderCallback() {
            @Override
            public void onSuccess(List<Reminder> reminders) {
                progressBar.setVisibility(View.GONE);
                reminderList.clear();
                for (Reminder r : reminders) {
                    reminderList.add(r.getTitle() + "\n" + r.getDescription());
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onError(String error) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(ReminderActivity.this, "Failed to load reminders", Toast.LENGTH_SHORT).show();
                Log.e("ReminderActivity", error);
            }
        });
    }
}

