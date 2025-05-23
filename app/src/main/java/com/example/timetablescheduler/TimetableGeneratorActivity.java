package com.example.timetablescheduler;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.os.Bundle;
import android.os.Environment;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TimetableGeneratorActivity extends AppCompatActivity {

    private Button btnGenerateTimetable, btnGeneratePDF, btnFinish;
    private ProgressBar progressBar;
    private TextView tvStatus;
    private TableLayout timetableTable;
    private Chromosome bestTimetable;

    private List<String> teachers = new ArrayList<>();
    private List<String> teacherSubjects = new ArrayList<>();
    private List<String> days = new ArrayList<>();
    private List<String> timeSlots = new ArrayList<>();
    private List<Map<String, Object>> batchesData = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_timetable_generator);

        initViews();
        loadDataFromDatabase();
        setupListeners();
    }

    private void initViews() {
        btnGenerateTimetable = findViewById(R.id.btnGenerateTimetable);
        btnGeneratePDF = findViewById(R.id.btnGeneratePDF);
        btnFinish = findViewById(R.id.btnFinish);
        progressBar = findViewById(R.id.progressBar);
        tvStatus = findViewById(R.id.tvStatus);
        timetableTable = findViewById(R.id.timetableTable);
    }

    private void loadDataFromDatabase() {
        // Load timetable data
        ParseQuery<ParseObject> timetableQuery = ParseQuery.getQuery("Timetable");
        timetableQuery.whereEqualTo("user", ParseUser.getCurrentUser());
        timetableQuery.findInBackground((timetableObjects, e) -> {
            if (e == null && !timetableObjects.isEmpty()) {
                ParseObject timetable = timetableObjects.get(0);
                List<String> selectedDays = timetable.getList("selectedDays");
                List<String> startTimes = timetable.getList("startTimes");
                List<String> endTimes = timetable.getList("endTimes");

                if (selectedDays != null) days.addAll(selectedDays);
                if (startTimes != null && endTimes != null) {
                    for (int i = 0; i < startTimes.size() && i < endTimes.size(); i++) {
                        timeSlots.add(startTimes.get(i) + " - " + endTimes.get(i));
                    }
                }
            }
        });

        // Load teachers data
        ParseQuery<ParseObject> teachersQuery = ParseQuery.getQuery("Teachers");
        teachersQuery.whereEqualTo("user", ParseUser.getCurrentUser());
        teachersQuery.findInBackground((teacherObjects, e) -> {
            if (e == null && !teacherObjects.isEmpty()) {
                ParseObject teacherData = teacherObjects.get(0);
                List<String> teacherNames = teacherData.getList("names");
                List<String> subjects = teacherData.getList("subjects");

                if (teacherNames != null) teachers.addAll(teacherNames);
                if (subjects != null) teacherSubjects.addAll(subjects);
            }
        });

        // Load batch data
        ParseQuery<ParseObject> batchQuery = ParseQuery.getQuery("BatchInfo");
        batchQuery.whereEqualTo("user", ParseUser.getCurrentUser());
        batchQuery.findInBackground((batchObjects, e) -> {
            if (e == null && !batchObjects.isEmpty()) {
                ParseObject batchInfo = batchObjects.get(0);
                List<Map<String, Object>> batches = batchInfo.getList("batchesData");
                if (batches != null) {
                    batchesData.addAll(batches);
                }
            }
        });
    }

    private void setupListeners() {
        btnGenerateTimetable.setOnClickListener(v -> generateTimetable());
        btnGeneratePDF.setOnClickListener(v -> generatePDF());
        btnFinish.setOnClickListener(v -> finish());
    }

    private void generateTimetable() {
        if (teachers.isEmpty() || batchesData.isEmpty() || days.isEmpty() || timeSlots.isEmpty()) {
            Toast.makeText(this, "Please ensure all data is loaded", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(ProgressBar.VISIBLE);
        tvStatus.setVisibility(TextView.VISIBLE);
        btnGenerateTimetable.setEnabled(false);

        EnhancedGeneticAlgorithm ga = new EnhancedGeneticAlgorithm(
                teachers, teacherSubjects, days, timeSlots, batchesData);

        new Thread(() -> {
            ga.generateTimetable(new EnhancedGeneticAlgorithm.ProgressCallback() {
                @Override
                public void onProgress(int generation, double bestFitness) {
                    runOnUiThread(() -> {
                        tvStatus.setText("Generation: " + generation + ", Best Fitness: " +
                                String.format("%.2f", bestFitness));
                    });
                }

                @Override
                public void onComplete(Chromosome bestChromosome) {
                    runOnUiThread(() -> {
                        bestTimetable = bestChromosome;
                        displayTimetable(bestChromosome);
                        progressBar.setVisibility(ProgressBar.GONE);
                        btnGenerateTimetable.setEnabled(true);
                        btnGeneratePDF.setEnabled(true);
                        tvStatus.setText("Timetable Generated Successfully!");
                    });
                }
            });
        }).start();
    }

    private void displayTimetable(Chromosome chromosome) {
        timetableTable.removeAllViews();
        timetableTable.setVisibility(TableLayout.VISIBLE);

        // Group genes by batch
        Map<String, List<Gene>> batchTimetables = new HashMap<>();
        for (Gene gene : chromosome.getGenes()) {
            batchTimetables.computeIfAbsent(gene.getBatchName(), k -> new ArrayList<>()).add(gene);
        }

        for (String batchName : batchTimetables.keySet()) {
            // Add batch header
            TableRow batchHeader = new TableRow(this);
            TextView batchTitle = new TextView(this);
            batchTitle.setText(batchName);
            batchTitle.setPadding(8, 8, 8, 8);
            batchTitle.setBackgroundColor(Color.BLUE);
            batchTitle.setTextColor(Color.WHITE);
            batchTitle.setTextSize(16);
            batchHeader.addView(batchTitle);
            timetableTable.addView(batchHeader);

            // Add timetable for this batch
            createBatchTimetable(batchTimetables.get(batchName));
        }
    }

    private void createBatchTimetable(List<Gene> batchGenes) {
        // Create header row
        TableRow headerRow = new TableRow(this);

        TextView timeHeader = new TextView(this);
        timeHeader.setText("Time/Day");
        timeHeader.setPadding(8, 8, 8, 8);
        timeHeader.setBackgroundColor(Color.LTGRAY);
        headerRow.addView(timeHeader);

        for (String day : days) {
            TextView dayHeader = new TextView(this);
            dayHeader.setText(day);
            dayHeader.setPadding(8, 8, 8, 8);
            dayHeader.setBackgroundColor(Color.LTGRAY);
            headerRow.addView(dayHeader);
        }
        timetableTable.addView(headerRow);

        // Create time slot rows
        for (int period = 0; period < timeSlots.size(); period++) {
            TableRow row = new TableRow(this);

            TextView timeSlot = new TextView(this);
            timeSlot.setText(timeSlots.get(period));
            timeSlot.setPadding(8, 8, 8, 8);
            timeSlot.setBackgroundColor(Color.LTGRAY);
            row.addView(timeSlot);

            for (int day = 0; day < days.size(); day++) {
                TextView cell = new TextView(this);
                cell.setPadding(8, 8, 8, 8);
                cell.setBackgroundColor(Color.WHITE);

                // Find subject for this time slot
                for (Gene gene : batchGenes) {
                    if (gene.getDay() == day && gene.getPeriod() == period) {
                        cell.setText(gene.getSubject() + "\n" + gene.getTeacherName());
                        break;
                    }
                }

                row.addView(cell);
            }
            timetableTable.addView(row);
        }
    }

    private void generatePDF() {
        if (bestTimetable == null) {
            Toast.makeText(this, "Please generate timetable first", Toast.LENGTH_SHORT).show();
            return;
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
            return;
        }

        PdfDocument pdfDocument = new PdfDocument();
        Paint paint = new Paint();

        // Group genes by batch for PDF
        Map<String, List<Gene>> batchTimetables = new HashMap<>();
        for (Gene gene : bestTimetable.getGenes()) {
            batchTimetables.computeIfAbsent(gene.getBatchName(), k -> new ArrayList<>()).add(gene);
        }

        int pageNumber = 1;
        for (String batchName : batchTimetables.keySet()) {
            PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, pageNumber++).create();
            PdfDocument.Page page = pdfDocument.startPage(pageInfo);
            Canvas canvas = page.getCanvas();

            // Title
            paint.setTextSize(20);
            paint.setColor(Color.BLACK);
            canvas.drawText("Timetable - " + batchName, 50, 50, paint);

            // Draw timetable
            paint.setTextSize(10);
            int startY = 100;
            int rowHeight = 30;
            int colWidth = 70;

            // Header
            canvas.drawText("Time", 50, startY, paint);
            for (int i = 0; i < days.size(); i++) {
                canvas.drawText(days.get(i), 150 + i * colWidth, startY, paint);
            }

            // Rows
            List<Gene> batchGenes = batchTimetables.get(batchName);
            for (int period = 0; period < timeSlots.size(); period++) {
                int y = startY + (period + 1) * rowHeight;
                canvas.drawText(timeSlots.get(period), 50, y, paint);

                for (int day = 0; day < days.size(); day++) {
                    String cellText = "";
                    for (Gene gene : batchGenes) {
                        if (gene.getDay() == day && gene.getPeriod() == period) {
                            cellText = gene.getSubject();
                            break;
                        }
                    }
                    canvas.drawText(cellText, 150 + day * colWidth, y, paint);
                }
            }

            pdfDocument.finishPage(page);
        }

        // Save PDF
        File file = new File(Environment.getExternalStorageDirectory(), "timetable_complete.pdf");
        try {
            pdfDocument.writeTo(new FileOutputStream(file));
            Toast.makeText(this, "PDF saved to: " + file.getAbsolutePath(), Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            Toast.makeText(this, "Error saving PDF: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }

        pdfDocument.close();
    }
}
