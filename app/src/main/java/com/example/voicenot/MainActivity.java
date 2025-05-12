package com.example.voicenot;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.speech.tts.TextToSpeech;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
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
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private TimePicker timePicker;
    private EditText taskDescription, notesBox;
    private Spinner taskTypeSpinner;
    private Button submitButton;
    private Button  WeeklyButton;
    private TableLayout taskTable;
    private TextToSpeech tts;
    private ImageView todaysTasksIcon, historyIcon, suggestionsIcon, forwardIcon;
    private TextView todaysTasksText, historyText, suggestionsText, forwardText;

    // Firebase Database references
    private DatabaseReference tasksReference;
    private DatabaseReference notesReference;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main); // Replace with your actual XML layout file

        // Initialize Firebase database references
        tasksReference = FirebaseDatabase.getInstance().getReference("tasks");
        notesReference = FirebaseDatabase.getInstance().getReference("notes");

        // Initialize UI components
        timePicker = findViewById(R.id.timePicker);
        taskDescription = findViewById(R.id.taskDescription);
        taskTypeSpinner = findViewById(R.id.taskTypeSpinner);
        notesBox = findViewById(R.id.notesBox);
        submitButton = findViewById(R.id.submitButton);
        WeeklyButton=findViewById(R.id.WeeklyButton);
        taskTable = findViewById(R.id.taskTable);
        todaysTasksIcon = findViewById(R.id.todaysTasksIcon);
        historyIcon = findViewById(R.id.historyIcon);
        suggestionsIcon = findViewById(R.id.suggestionsIcon);
        forwardIcon = findViewById(R.id.forwardIcon);

        todaysTasksText = findViewById(R.id.todaysTasksText);
        historyText = findViewById(R.id.historyText);
        suggestionsText = findViewById(R.id.suggestionsText);
        forwardText = findViewById(R.id.forwardText);

        // Set TimePicker to 24-hour format
        timePicker.setIs24HourView(true);

        // Set up Spinner with ArrayAdapter
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this,
                R.array.task_types, // Defined in strings.xml
                android.R.layout.simple_spinner_item
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        taskTypeSpinner.setAdapter(adapter);

        // Initialize Text-to-Speech
        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                tts.setLanguage(Locale.ENGLISH);
            }
        });

        // Handle "Today's Tasks" icon click
        todaysTasksIcon.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, Todaytasks.class);
            startActivity(intent);
        });

        // Handle "History" icon click
        historyIcon.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, Historyoftasks.class);
            startActivity(intent);
        });

        // Handle "Forward Icon" click
        forwardIcon.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, WeatherTraffic.class);
            startActivity(intent);
        });

        // Handle "Suggestions Icon" click
        suggestionsIcon.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, Suggestions.class);
            startActivity(intent);
        });

        // Handle "Submit Task" button click
        submitButton.setOnClickListener(v -> {
            String description = taskDescription.getText().toString().trim();
            String notes = notesBox.getText().toString().trim();
            String taskType = taskTypeSpinner.getSelectedItem().toString();
            int hour = timePicker.getHour();
            int minute = timePicker.getMinute();

            if (description.isEmpty()) {
                Toast.makeText(MainActivity.this, "Please enter task description", Toast.LENGTH_SHORT).show();
            } else {
                addTaskToTable(description, hour, minute, taskType);
                saveTaskToFirebase(description, String.format("%02d:%02d", hour, minute), taskType);
                if (!notes.isEmpty()) {
                    saveNotesToFirebase(notes);
                }
                scheduleNotification(description, hour, minute, taskType); // Pass taskType
                clearFormFields();
                Toast.makeText(MainActivity.this, "Task added successfully", Toast.LENGTH_SHORT).show();
            }
        });

        if (WeeklyButton != null) {
            WeeklyButton.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity.this, MainActivity2.class);
                startActivity(intent);
            });
        }

        // Automatically save notes when focus changes
        notesBox.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) { // User moved away from the notes box
                String notes = notesBox.getText().toString().trim();
                if (!notes.isEmpty()) {
                    saveNotesToFirebase(notes);
                }
            }
        });

        // Save notes as user types (optional)
        notesBox.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                String notes = s.toString().trim();
                if (!notes.isEmpty()) {
                    saveNotesToFirebase(notes);
                }
            }
        });
    }

    // Method to add task to the table
    private void addTaskToTable(String description, int hour, int minute, String taskType) {
        // Dynamically add a new row to the task table
        TableRow newRow = new TableRow(this);

        TextView descriptionText = new TextView(this);
        descriptionText.setText(description);
        descriptionText.setPadding(8, 8, 8, 8);
        newRow.addView(descriptionText);

        TextView timeText = new TextView(this);
        timeText.setText(String.format("%02d:%02d", hour, minute));
        timeText.setPadding(8, 8, 8, 8);
        newRow.addView(timeText);

        TextView typeText = new TextView(this);
        typeText.setText(taskType);
        typeText.setPadding(8, 8, 8, 8);
        newRow.addView(typeText);

        // Add Edit and Delete buttons
        ImageButton editButton = new ImageButton(this);
        editButton.setImageResource(R.drawable.baseline_edit_note_24); // Replace with your edit icon
        editButton.setBackground(null);
        editButton.setOnClickListener(v -> editTask(newRow, description, hour, minute, taskType));
        newRow.addView(editButton);

        ImageButton deleteButton = new ImageButton(this);
        deleteButton.setImageResource(R.drawable.baseline_delete_24); // Replace with your delete icon
        deleteButton.setBackground(null);
        deleteButton.setOnClickListener(v -> deleteTask(newRow, description));
        newRow.addView(deleteButton);

        taskTable.addView(newRow);
    }

    // Method to edit a task
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

    // Method to delete a task
    private void deleteTask(TableRow row, String description) {
        // Remove the task from the table
        taskTable.removeView(row);

        // Delete the task from Firebase
        deleteTaskFromFirebase(description);
    }

    // Method to delete a task from Firebase
    private void deleteTaskFromFirebase(String description) {
        tasksReference.orderByChild("description").equalTo(description).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot taskSnapshot : snapshot.getChildren()) {
                    taskSnapshot.getRef().removeValue()
                            .addOnSuccessListener(aVoid -> showToast("Task deleted successfully"))
                            .addOnFailureListener(e -> showToast("Failed to delete task: " + e.getMessage()));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                showToast("Failed to delete task: " + error.getMessage());
            }
        });
    }

    // Method to save task to Firebase
    private void saveTaskToFirebase(String description, String time, String type) {
        String taskId = tasksReference.push().getKey();
        if (taskId != null) {
            HashMap<String, String> taskData = new HashMap<>();
            taskData.put("description", description);
            taskData.put("time", time);
            taskData.put("type", type);
            taskData.put("date", new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date()));

            tasksReference.child(taskId).setValue(taskData)
                    .addOnSuccessListener(aVoid -> showToast("Task saved successfully"))
                    .addOnFailureListener(e -> showToast("Failed to save task: " + e.getMessage()));
        }
    }

    // Method to save notes to Firebase
    private void saveNotesToFirebase(String notes) {
        String noteId = notesReference.push().getKey(); // Unique ID for each note
        if (noteId != null) {
            HashMap<String, String> noteData = new HashMap<>();
            noteData.put("notes", notes);

            notesReference.child(noteId).setValue(noteData)
                    .addOnSuccessListener(aVoid -> showToast("Notes saved successfully"))
                    .addOnFailureListener(e -> showToast("Failed to save notes: " + e.getMessage()));
        }
    }

    // Method to schedule voice notification
    public void scheduleNotification(String description, int hour, int minute, String taskType) {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            showToast("Alarm Manager not available");
            return;
        }

        // Time for the first notification
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);
        long timeInMillis = calendar.getTimeInMillis();

        // 🔸 FIRST INTENT (Normal Reminder)
        Intent intent = new Intent(this, TaskNotificationReceiver.class);
        intent.putExtra("taskDescription", description);
        intent.putExtra("taskType", taskType);
        intent.putExtra("isSecondReminder", false); // Mark it as first reminder

        int firstRequestCode = (int) (timeInMillis % Integer.MAX_VALUE); // unique-ish ID
        PendingIntent firstPendingIntent = PendingIntent.getBroadcast(
                this, firstRequestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        alarmManager.setExact(AlarmManager.RTC_WAKEUP, timeInMillis, firstPendingIntent);

        // 🔸 SECOND INTENT (Only for Important & Urgent)
        if ("Important & Urgent".equals(taskType)) {
            long secondReminderTime = timeInMillis + (2 * 60 * 1000); // 2 minutes later

            Intent secondIntent = new Intent(this, TaskNotificationReceiver.class);
            secondIntent.putExtra("taskDescription", description);
            secondIntent.putExtra("taskType", taskType);
            secondIntent.putExtra("isSecondReminder", true); // Mark it as second reminder

            int secondRequestCode = (int) ((timeInMillis + 1) % Integer.MAX_VALUE); // Make it different!
            PendingIntent secondPendingIntent = PendingIntent.getBroadcast(
                    this, secondRequestCode, secondIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            alarmManager.setExact(AlarmManager.RTC_WAKEUP, secondReminderTime, secondPendingIntent);
        }

        showToast("Voice notification scheduled for " + description);
    }




    // Helper method to schedule an exact notification
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

    // Method to clear the form fields
    private void clearFormFields() {
        taskDescription.setText("");
        notesBox.setText("");
        taskTypeSpinner.setSelection(0);
        timePicker.setHour(Calendar.getInstance().get(Calendar.HOUR_OF_DAY));
        timePicker.setMinute(Calendar.getInstance().get(Calendar.MINUTE));
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}