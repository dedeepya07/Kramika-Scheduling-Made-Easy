package com.example.dailyscheduler;

import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import java.util.Calendar;

public class DailyPlannerActivity extends AppCompatActivity {

    private CalendarView plannerCalendarView;
    private EditText morningTask, afternoonTask, eveningTask, nightTask;
    private TimePicker morningTimePicker, afternoonTimePicker, eveningTimePicker, nightTimePicker;
    private Button savePlanBtn;
    private SharedPreferences sharedPreferences;
    private String selectedDate = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_daily_planner);

        // Permissions for notifications (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1001);
            }
        }

        // Bind UI components
        plannerCalendarView = findViewById(R.id.plannerCalendarView);
        morningTask = findViewById(R.id.morningTask);
        afternoonTask = findViewById(R.id.afternoonTask);
        eveningTask = findViewById(R.id.eveningTask);
        nightTask = findViewById(R.id.nightTask);

        morningTimePicker = findViewById(R.id.morningTimePicker);
        afternoonTimePicker = findViewById(R.id.afternoonTimePicker);
        eveningTimePicker = findViewById(R.id.eveningTimePicker);
        nightTimePicker = findViewById(R.id.nightTimePicker);

        savePlanBtn = findViewById(R.id.savePlanBtn);

        // Set 24-hour format for all pickers
        morningTimePicker.setIs24HourView(true);
        afternoonTimePicker.setIs24HourView(true);
        eveningTimePicker.setIs24HourView(true);
        nightTimePicker.setIs24HourView(true);

        // SharedPreferences for persistence
        sharedPreferences = getSharedPreferences("DailyPlans", MODE_PRIVATE);

        // Initial selected date
        selectedDate = getDateString(plannerCalendarView.getDate());
        loadPlan(selectedDate);

        // Date change handler
        plannerCalendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            selectedDate = dayOfMonth + "-" + (month + 1) + "-" + year;
            loadPlan(selectedDate);
        });

        // Save and schedule plan
        savePlanBtn.setOnClickListener(v -> {
            save("morning", morningTask.getText().toString());
            save("afternoon", afternoonTask.getText().toString());
            save("evening", eveningTask.getText().toString());
            save("night", nightTask.getText().toString());

            scheduleReminder("Morning Plan", morningTask.getText().toString(), morningTimePicker.getHour(), morningTimePicker.getMinute());
            scheduleReminder("Afternoon Plan", afternoonTask.getText().toString(), afternoonTimePicker.getHour(), afternoonTimePicker.getMinute());
            scheduleReminder("Evening Plan", eveningTask.getText().toString(), eveningTimePicker.getHour(), eveningTimePicker.getMinute());
            scheduleReminder("Night Plan", nightTask.getText().toString(), nightTimePicker.getHour(), nightTimePicker.getMinute());

            Toast.makeText(this, "Plan saved and reminders set!", Toast.LENGTH_SHORT).show();
        });
    }

    private void save(String section, String value) {
        sharedPreferences.edit().putString(selectedDate + "_" + section, value).apply();
    }

    private void loadPlan(String date) {
        morningTask.setText(sharedPreferences.getString(date + "_morning", ""));
        afternoonTask.setText(sharedPreferences.getString(date + "_afternoon", ""));
        eveningTask.setText(sharedPreferences.getString(date + "_evening", ""));
        nightTask.setText(sharedPreferences.getString(date + "_night", ""));
    }

    private String getDateString(long millis) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(millis);
        return cal.get(Calendar.DAY_OF_MONTH) + "-" + (cal.get(Calendar.MONTH) + 1) + "-" + cal.get(Calendar.YEAR);
    }

    private void scheduleReminder(String section, String message, int hour, int minute) {
        Intent intent = new Intent(this, NotificationReceiver.class);
        intent.putExtra("title", section + ": " + message);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this,
                section.hashCode(),
                intent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, Math.max(minute - 5, 0));
        calendar.set(Calendar.SECOND, 0);

        if (calendar.getTimeInMillis() < System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_MONTH, 1);
        }

        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                try {
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
                } catch (SecurityException e) {
                    Toast.makeText(this, "Exact alarm permission denied.", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "App lacks permission to schedule exact alarms.", Toast.LENGTH_LONG).show();
            }
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
        }
    }
}