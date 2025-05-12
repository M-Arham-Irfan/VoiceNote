package com.example.voicenot;

import android.os.Bundle;
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

public class Historyoftasks extends AppCompatActivity {

    private TableLayout taskTable;
    private DatabaseReference databaseReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_historyoftasks);

        taskTable = findViewById(R.id.taskTable);
        databaseReference = FirebaseDatabase.getInstance().getReference("tasks");

        fetchAllTasks();
    }

    private void fetchAllTasks() {
        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                TreeMap<String, List<HashMap<String, String>>> tasksByDate = new TreeMap<>();

                // Group tasks by date
                for (DataSnapshot taskSnapshot : snapshot.getChildren()) {
                    HashMap<String, String> task = (HashMap<String, String>) taskSnapshot.getValue();
                    if (task != null && task.containsKey("date")) {
                        String date = task.get("date");
                        tasksByDate.putIfAbsent(date, new ArrayList<>());
                        tasksByDate.get(date).add(task);
                    }
                }

                // Display tasks grouped by date
                if (tasksByDate.isEmpty()) {
                    showNoTasksMessage();
                } else {
                    for (String date : tasksByDate.keySet()) {
                        addDateHeader(date);
                        for (HashMap<String, String> task : tasksByDate.get(date)) {
                            addTaskToTable(
                                    task.get("description"),
                                    task.get("time"),
                                    task.get("type"),
                                    task.getOrDefault("notes", "N/A")
                            );
                        }
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                // Handle error
            }
        });
    }

    private void addDateHeader(String date) {
        TableRow headerRow = new TableRow(this);
        TextView header = new TextView(this);
        header.setText("Tasks for " + date);
        header.setPadding(8, 8, 8, 8);
        header.setTextSize(18);
        headerRow.addView(header);
        taskTable.addView(headerRow);
    }

    private void addTaskToTable(String description, String time, String type, String notes) {
        TableRow row = new TableRow(this);
        row.addView(createTableCell(description));
        row.addView(createTableCell(time));
        row.addView(createTableCell(type));
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
