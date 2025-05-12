package com.example.voicenot;

import android.os.Bundle;
import android.util.Log;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Calendar;
import java.util.HashMap;

public class Todaystasks2 extends AppCompatActivity {

    private TableLayout taskTable;
    private DatabaseReference databaseReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_todaystasks2);

        taskTable = findViewById(R.id.taskTable);
        databaseReference = FirebaseDatabase.getInstance().getReference("weekly_tasks");

        fetchTasksForToday();
    }

    private void fetchTasksForToday() {
        // Get current day of the week (e.g., "Monday", "Tuesday", etc.)
        String todayDay = getCurrentDayOfWeek();

        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                boolean hasTasksForToday = false;

                for (DataSnapshot taskSnapshot : snapshot.getChildren()) {
                    Object taskObject = taskSnapshot.getValue();

                    if (taskObject instanceof HashMap) {
                        HashMap<String, Object> task = (HashMap<String, Object>) taskObject;

                        // Fetch task details
                        String taskDay = task.get("day") != null ? task.get("day").toString() : "";
                        String description = task.get("description") != null ? task.get("description").toString() : "N/A";
                        String time = task.get("time") != null ? task.get("time").toString() : "N/A";
                        String type = task.get("type") != null ? task.get("type").toString() : "N/A";
                        String notes = task.get("notes") != null ? task.get("notes").toString() : "N/A";

                        // Compare with current day
                        if (todayDay.equalsIgnoreCase(taskDay)) {
                            if (!hasTasksForToday) {
                                addDateHeader(todayDay);
                                hasTasksForToday = true;
                            }
                            addTaskToTable(description, time, type, taskDay, notes);
                        }
                    }
                }

                if (!hasTasksForToday) {
                    showNoTasksMessage();
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e("FirebaseError", "Error fetching tasks: " + error.getMessage());
            }
        });
    }

    private String getCurrentDayOfWeek() {
        Calendar calendar = Calendar.getInstance();
        int day = calendar.get(Calendar.DAY_OF_WEEK);
        switch (day) {
            case Calendar.SUNDAY:
                return "Sunday";
            case Calendar.MONDAY:
                return "Monday";
            case Calendar.TUESDAY:
                return "Tuesday";
            case Calendar.WEDNESDAY:
                return "Wednesday";
            case Calendar.THURSDAY:
                return "Thursday";
            case Calendar.FRIDAY:
                return "Friday";
            case Calendar.SATURDAY:
                return "Saturday";
            default:
                return "Unknown";
        }
    }

    private void addDateHeader(String day) {
        TableRow headerRow = new TableRow(this);
        TextView header = new TextView(this);
        header.setText("Tasks for " + day);
        header.setPadding(8, 8, 8, 8);
        header.setTextSize(18);
        headerRow.addView(header);
        taskTable.addView(headerRow);
    }

    private void addTaskToTable(String description, String time, String type, String day, String notes) {
        TableRow row = new TableRow(this);
        row.addView(createTableCell(description));
        row.addView(createTableCell(time));
        row.addView(createTableCell(type));
        row.addView(createTableCell(day));
        row.addView(createTableCell(notes));
        taskTable.addView(row);
    }

    private TextView createTableCell(String text) {
        TextView cell = new TextView(this);
        cell.setText(text);
        cell.setPadding(8, 8, 8, 8);
        return cell;
    }

    private void showNoTasksMessage() {
        TableRow row = new TableRow(this);
        TextView message = new TextView(this);
        message.setText("No tasks for today.");
        message.setPadding(8, 8, 8, 8);
        message.setTextSize(16);
        row.addView(message);
        taskTable.addView(row);
    }
}
