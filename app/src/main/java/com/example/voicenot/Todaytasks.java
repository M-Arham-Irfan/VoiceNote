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

import java.util.Calendar;
import java.util.HashMap;

public class Todaytasks extends AppCompatActivity {

    private TableLayout taskTable;
    private DatabaseReference databaseReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_todaytasks);

        taskTable = findViewById(R.id.taskTable);
        databaseReference = FirebaseDatabase.getInstance().getReference("tasks");

        fetchTasksForToday();
    }

    private void fetchTasksForToday() {
        Calendar calendar = Calendar.getInstance();
        String todayDate = String.format("%04d-%02d-%02d",
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH) + 1,
                calendar.get(Calendar.DAY_OF_MONTH));

        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                boolean hasTasksForToday = false;

                // Iterate through tasks and find today's tasks
                for (DataSnapshot taskSnapshot : snapshot.getChildren()) {
                    HashMap<String, String> task = (HashMap<String, String>) taskSnapshot.getValue();
                    if (task != null && todayDate.equals(task.get("date"))) {
                        if (!hasTasksForToday) {
                            addDateHeader(todayDate);
                            hasTasksForToday = true;
                        }
                        addTaskToTable(
                                task.get("description"),
                                task.get("time"),
                                task.get("type"),
                                task.getOrDefault("notes", "N/A")
                        );
                    }
                }

                if (!hasTasksForToday) {
                    showNoTasksMessage();
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
        message.setText("No tasks for today.");
        message.setPadding(8, 8, 8, 8);
        message.setTextSize(16);
        row.addView(message);
        taskTable.addView(row);
    }
}
