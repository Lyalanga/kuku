package com.example.fowltyphoidmonitor.services.supabase;

import android.os.AsyncTask;
import com.example.fowltyphoidmonitor.services.supabase.model.Reminder;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class SupabaseReminderService {
    public interface ReminderCallback {
        void onSuccess(List<Reminder> reminders);
        void onError(String error);
    }

    // Replace with your actual Supabase REST endpoint for reminders
    private static final String SUPABASE_REMINDER_URL = "https://YOUR_SUPABASE_PROJECT.supabase.co/rest/v1/reminders?select=*";
    private static final String SUPABASE_API_KEY = "YOUR_SUPABASE_API_KEY";

    public static void fetchRemindersForFarmer(ReminderCallback callback) {
        new AsyncTask<Void, Void, List<Reminder>>() {
            String error = null;
            @Override
            protected List<Reminder> doInBackground(Void... voids) {
                List<Reminder> reminders = new ArrayList<>();
                try {
                    URL url = new URL(SUPABASE_REMINDER_URL);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    conn.setRequestProperty("apikey", SUPABASE_API_KEY);
                    conn.setRequestProperty("Authorization", "Bearer " + SUPABASE_API_KEY);
                    conn.setRequestProperty("Content-Type", "application/json");
                    conn.setConnectTimeout(5000);
                    conn.setReadTimeout(5000);
                    int responseCode = conn.getResponseCode();
                    if (responseCode == 200) {
                        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                        StringBuilder response = new StringBuilder();
                        String inputLine;
                        while ((inputLine = in.readLine()) != null) {
                            response.append(inputLine);
                        }
                        in.close();
                        JSONArray arr = new JSONArray(response.toString());
                        for (int i = 0; i < arr.length(); i++) {
                            JSONObject obj = arr.getJSONObject(i);
                            Reminder reminder = new Reminder();
                            reminder.setTitle(obj.optString("title"));
                            reminder.setDescription(obj.optString("description"));
                            reminders.add(reminder);
                        }
                    } else {
                        error = "HTTP error: " + responseCode;
                    }
                } catch (Exception e) {
                    error = e.getMessage();
                }
                return reminders;
            }
            @Override
            protected void onPostExecute(List<Reminder> reminders) {
                if (error != null) {
                    callback.onError(error);
                } else {
                    callback.onSuccess(reminders);
                }
            }
        }.execute();
    }
}
