package com.example.voicenot;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;

public class MainActivity2 extends AppCompatActivity {

    private TimePicker timePicker;
    private EditText taskDescription, notesBox;
    private Spinner taskTypeSpinner, daySpinner;
    private Button submitButton;
    private TableLayout taskTable;
    private TextToSpeech tts;
    private ImageView todaysTasksIcon, historyIcon, suggestionsIcon, forwardIcon;
    private TextView todaysTasksText, historyText, suggestionsText, forwardText;

    private DatabaseReference weeklyTasksReference;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);

        FirebaseDatabase database = FirebaseDatabase.getInstance();
        weeklyTasksReference = database.getReference("weekly_tasks");

        timePicker = findViewById(R.id.timePicker);
        taskDescription = findViewById(R.id.taskDescription);
        notesBox = findViewById(R.id.notesBox);
        submitButton = findViewById(R.id.submitButton);
        taskTable = findViewById(R.id.taskTable);
        taskTypeSpinner = findViewById(R.id.taskTypeSpinner);
        daySpinner = findViewById(R.id.daySpinner);

        timePicker.setIs24HourView(true);
        todaysTasksIcon = findViewById(R.id.todaysTasksIcon);
        historyIcon = findViewById(R.id.historyIcon);
        suggestionsIcon = findViewById(R.id.suggestionsIcon);
        forwardIcon = findViewById(R.id.forwardIcon);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this,
                R.array.days_of_week,
                android.R.layout.simple_spinner_item
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        daySpinner.setAdapter(adapter);
        ArrayAdapter<CharSequence> taskTypeAdapter = ArrayAdapter.createFromResource(
                this,
                R.array.task_types, // Ensure this array exists in res/values/strings.xml
                android.R.layout.simple_spinner_item
        );
        taskTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        taskTypeSpinner.setAdapter(taskTypeAdapter);
        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int result = tts.setLanguage(Locale.ENGLISH);
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e("TTS", "Text-to-Speech Language not supported!");
                }
            }
        });
        // Handle "Today's Tasks" icon click
        todaysTasksIcon.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity2.this, Todaystasks2.class);
            startActivity(intent);
        });

        // Handle "History" icon click
        historyIcon.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity2.this, Historyoftasks2.class);
            startActivity(intent);
        });

        // Handle "Forward Icon" click
        forwardIcon.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity2.this, WeatherTraffic.class);
            startActivity(intent);
        });

        // Handle "Suggestions Icon" click
        suggestionsIcon.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity2.this, Suggestions.class);
            startActivity(intent);
        });


        submitButton.setOnClickListener(v -> {
            String description = taskDescription.getText().toString().trim();
            String notes = notesBox.getText().toString().trim();
            String selectedDay = daySpinner.getSelectedItem().toString();
            String taskType = taskTypeSpinner.getSelectedItem().toString();
            int hour = timePicker.getHour();
            int minute = timePicker.getMinute();

            if (description.isEmpty()) {
                Toast.makeText(MainActivity2.this, "Please enter task description", Toast.LENGTH_SHORT).show();
            } else {
                addTaskToTable(description, hour, minute, selectedDay, taskType);
                saveTaskToFirebase(description, String.format("%02d:%02d", hour, minute), selectedDay, taskType, notes);
                scheduleNotification(description, hour, minute, taskType);
                clearFormFields();
                Toast.makeText(MainActivity2.this, "Weekly Task added successfully", Toast.LENGTH_SHORT).show();
            }
        });

        fetchWeeklyTasks();
    }

    private void addTaskToTable(String description, int hour, int minute, String day, String taskType) {
        TableRow newRow = new TableRow(this);

        TextView descriptionText = new TextView(this);
        descriptionText.setText(description);
        descriptionText.setPadding(8, 8, 8, 8);
        newRow.addView(descriptionText);

        TextView timeText = new TextView(this);
        timeText.setText(String.format("%02d:%02d", hour, minute));
        timeText.setPadding(8, 8, 8, 8);
        newRow.addView(timeText);

        TextView dayText = new TextView(this);
        dayText.setText(day);
        dayText.setPadding(8, 8, 8, 8);
        newRow.addView(dayText);

        TextView typeText = new TextView(this);
        typeText.setText(taskType);
        typeText.setPadding(8, 8, 8, 8);
        newRow.addView(typeText);

        ImageButton deleteButton = new ImageButton(this);
        deleteButton.setImageResource(R.drawable.baseline_delete_24);
        deleteButton.setBackground(null);
        deleteButton.setOnClickListener(v -> deleteTask(newRow, description));
        newRow.addView(deleteButton);

        taskTable.addView(newRow);
    }
    private void editTask(TableRow row, String description, int hour, int minute, String taskType) {
        // Populate the form fields with the selected task's data
        taskDescription.setText(description);
        timePicker.setHour(hour);
        timePicker.setMinute(minute);
        taskTypeSpinner.setSelection(((ArrayAdapter) taskTypeSpinner.getAdapter()).getPosition(taskType));

        // Remove the task from the table
        taskTable.removeView(row);

        // Optionally, delete the task from Firebase
        deleteTaskFromFirebase(description);
    }
    private void deleteTask(TableRow row, String description) {
        taskTable.removeView(row);
        deleteTaskFromFirebase(description);
    }

    private void deleteTaskFromFirebase(String description) {
        weeklyTasksReference.orderByChild("description").equalTo(description)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot taskSnapshot : snapshot.getChildren()) {
                            taskSnapshot.getRef().removeValue();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        showToast("Failed to delete task: " + error.getMessage());
                    }
                });
    }

    private void saveTaskToFirebase(String description, String time, String day, String taskType, String notes) {
        String taskId = weeklyTasksReference.push().getKey();
        if (taskId != null) {
            HashMap<String, String> taskData = new HashMap<>();
            taskData.put("description", description);
            taskData.put("time", time);
            taskData.put("day", day);
            taskData.put("taskType", taskType);
            taskData.put("notes", notes);

            weeklyTasksReference.child(taskId).setValue(taskData);
        }
    }

    private void fetchWeeklyTasks() {
        weeklyTasksReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                taskTable.removeAllViews();
                for (DataSnapshot taskSnapshot : snapshot.getChildren()) {
                    String description = taskSnapshot.child("description").getValue(String.class);
                    String time = taskSnapshot.child("time").getValue(String.class);
                    String day = taskSnapshot.child("day").getValue(String.class);
                    String taskType = taskSnapshot.child("taskType").getValue(String.class);

                    if (description != null && time != null && day != null && taskType != null) {
                        String[] timeParts = time.split(":");
                        int hour = Integer.parseInt(timeParts[0]);
                        int minute = Integer.parseInt(timeParts[1]);
                        addTaskToTable(description, hour, minute, day, taskType);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                showToast("Failed to fetch weekly tasks");
            }
        });
    }

    @SuppressLint("ScheduleExactAlarm")
    public void scheduleNotification(String description, int hour, int minute, String taskType) {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            showToast("Alarm Manager is not available");
            return;
        }

        // Get the current day of the week (1 = Sunday, 2 = Monday, ..., 7 = Saturday)
        Calendar now = Calendar.getInstance();
        int currentDayOfWeek = now.get(Calendar.DAY_OF_WEEK);

        // Convert selectedDay (e.g., "Monday") to Calendar's day format
        String selectedDay = daySpinner.getSelectedItem().toString().trim();
        if (selectedDay == null || selectedDay.isEmpty()) {
            Toast.makeText(this, "Please select a valid day", Toast.LENGTH_SHORT).show();
            return;
        }

        int taskDayOfWeek = getDayOfWeek(selectedDay);
        if (taskDayOfWeek == -1) {
            Toast.makeText(this, "Invalid day selection", Toast.LENGTH_SHORT).show();
            return;
        }

        // Calculate the difference in days
        int daysUntilTask = (taskDayOfWeek - currentDayOfWeek + 7) % 7;

        // If the task is today but time has passed, schedule it for the next week
        if (daysUntilTask == 0 && (hour < now.get(Calendar.HOUR_OF_DAY) ||
                (hour == now.get(Calendar.HOUR_OF_DAY) && minute <= now.get(Calendar.MINUTE)))) {
            daysUntilTask = 7;
        }

        // Set the notification time
        Calendar taskTime = Calendar.getInstance();
        taskTime.set(Calendar.HOUR_OF_DAY, hour);
        taskTime.set(Calendar.MINUTE, minute);
        taskTime.set(Calendar.SECOND, 0);
        taskTime.add(Calendar.DAY_OF_YEAR, daysUntilTask);

        long timeInMillis = taskTime.getTimeInMillis();

        // Create an intent for the broadcast receiver
        Intent intent = new Intent(this, TaskNotificationReceiver.class);
        intent.putExtra("taskDescription", description);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this, (int) timeInMillis, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        alarmManager.setExact(AlarmManager.RTC_WAKEUP, timeInMillis, pendingIntent);

        // If the task is "Important & Urgent", schedule a second notification 2 minutes later
        if (taskType.equals("Important & Urgent")) {
            long reminderTimeInMillis = timeInMillis + (2 * 60 * 1000); // 2 minutes later
            PendingIntent secondPendingIntent = PendingIntent.getBroadcast(
                    this, (int) reminderTimeInMillis, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            alarmManager.setExact(AlarmManager.RTC_WAKEUP, reminderTimeInMillis, secondPendingIntent);
        }

        showToast("Voice notification scheduled for " + description + " on " + selectedDay);
    }

    /**
     * Helper method to convert day names (e.g., "Monday") to Calendar day values.
     */
    private int getDayOfWeek(String day) {
        switch (day.toLowerCase()) {
            case "sunday": return Calendar.SUNDAY;
            case "monday": return Calendar.MONDAY;
            case "tuesday": return Calendar.TUESDAY;
            case "wednesday": return Calendar.WEDNESDAY;
            case "thursday": return Calendar.THURSDAY;
            case "friday": return Calendar.FRIDAY;
            case "saturday": return Calendar.SATURDAY;
            default: return Calendar.SUNDAY; // Default fallback
        }
    }

    private void scheduleExactNotification(AlarmManager alarmManager, String description, long timeInMillis) {
        Intent intent = new Intent(this, TaskNotificationReceiver.class);
        intent.putExtra("taskDescription", description);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this,
                (int) timeInMillis, // Use time as a unique identifier
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) { // Android 12+
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, timeInMillis, pendingIntent);
            } else {
                requestExactAlarmPermission();
            }
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, timeInMillis, pendingIntent);
        }
    }

    private void requestExactAlarmPermission() {
    }

    private void clearFormFields() {
        taskDescription.setText("");
        notesBox.setText("");
        daySpinner.setSelection(0);
        taskTypeSpinner.setSelection(0);
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
