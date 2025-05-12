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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;

public class Historyoftasks2 extends AppCompatActivity {

    private TableLayout taskTable;
    private DatabaseReference databaseReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_historyoftasks2);

        taskTable = findViewById(R.id.taskTable);
        databaseReference = FirebaseDatabase.getInstance().getReference("weekly_tasks");

        fetchAllTasks();
    }

    private void fetchAllTasks() {
        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                TreeMap<String, List<HashMap<String, String>>> tasksByDay = new TreeMap<>();

                // Group tasks by "day" (Monday, Tuesday, etc.)
                for (DataSnapshot taskSnapshot : snapshot.getChildren()) {
                    HashMap<String, String> task = (HashMap<String, String>) taskSnapshot.getValue();
                    if (task != null && task.containsKey("day")) {
                        String day = task.get("day");
                        tasksByDay.putIfAbsent(day, new ArrayList<>());
                        tasksByDay.get(day).add(task);
                    }
                }

                // Display tasks grouped by "day"
                if (tasksByDay.isEmpty()) {
                    showNoTasksMessage();
                } else {
                    for (String day : tasksByDay.keySet()) {
                        addDayHeader(day);
                        for (HashMap<String, String> task : tasksByDay.get(day)) {
                            addTaskToTable(
                                    task.get("description"),
                                    task.get("time"),
                                    task.get("type"),
                                    day,  // Display day correctly
                                    task.getOrDefault("notes", "N/A")
                            );
                        }
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e("FirebaseError", "Error fetching history: " + error.getMessage());
            }
        });
    }

    private void addDayHeader(String day) {
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
        row.addView(createTableCell(day));  // Display Day Column
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
        message.setText("No tasks found in history.");
        message.setPadding(8, 8, 8, 8);
        message.setTextSize(16);
        row.addView(message);
        taskTable.addView(row);
    }
}
