package com.example.timetablescheduler;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Table;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;

public class TimetableActivity extends AppCompatActivity {

    private TableLayout tableLayout;
    private ProgressBar progressBar;
    private Button btnGenerate, btnDownload;
    private List<ParseObject> timetableData;

    private static final int REQUEST_CODE_WRITE_EXTERNAL_STORAGE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_timetable);

        tableLayout = findViewById(R.id.timetable_table);
        progressBar = findViewById(R.id.progressBar);
        btnGenerate = findViewById(R.id.btn_generate);
        btnDownload = findViewById(R.id.btn_download);

        btnGenerate.setOnClickListener(v -> generateTimetable());
        btnDownload.setOnClickListener(v -> checkPermissionAndGeneratePDF());
    }

    private void generateTimetable() {
        progressBar.setVisibility(View.VISIBLE);

        // Fetch timetable data from Parse
        ParseQuery<ParseObject> query = ParseQuery.getQuery("Timetable");
        query.whereEqualTo("user", ParseUser.getCurrentUser());
        query.findInBackground((objects, e) -> {
            progressBar.setVisibility(View.GONE);
            if (e == null && objects != null && !objects.isEmpty()) {
                timetableData = objects;
                populateTimetable();
            } else {
                Toast.makeText(this, "Error fetching timetable data", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void populateTimetable() {
        tableLayout.removeAllViews();

        // Header row
        TableRow headerRow = new TableRow(this);
        String[] headers = {"Time", "Mon", "Tue", "Wed", "Thu", "Fri"};
        for (String header : headers) {
            TextView tv = new TextView(this);
            tv.setText(header);
            tv.setPadding(16, 16, 16, 16);
            tv.setTextAppearance(android.R.style.TextAppearance_Medium);
            headerRow.addView(tv);
        }
        tableLayout.addView(headerRow);

        // For each period, create a row
        for (ParseObject period : timetableData) {
            TableRow row = new TableRow(this);
            String[] cells = {
                    period.getString("time"),
                    safeConcat(period.getString("mon_subject"), period.getString("mon_teacher")),
                    safeConcat(period.getString("tue_subject"), period.getString("tue_teacher")),
                    safeConcat(period.getString("wed_subject"), period.getString("wed_teacher")),
                    safeConcat(period.getString("thu_subject"), period.getString("thu_teacher")),
                    safeConcat(period.getString("fri_subject"), period.getString("fri_teacher"))
            };
            for (String cell : cells) {
                TextView tv = new TextView(this);
                tv.setText(cell);
                tv.setPadding(16, 16, 16, 16);
                row.addView(tv);
            }
            tableLayout.addView(row);
        }
    }

    // Helper to avoid nulls
    private String safeConcat(String subject, String teacher) {
        if (subject == null) subject = "";
        if (teacher == null) teacher = "";
        return subject + (teacher.isEmpty() ? "" : "\n" + teacher);
    }

    private void checkPermissionAndGeneratePDF() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        REQUEST_CODE_WRITE_EXTERNAL_STORAGE);
            } else {
                generatePDF();
            }
        } else {
            generatePDF();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == REQUEST_CODE_WRITE_EXTERNAL_STORAGE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                generatePDF();
            } else {
                Toast.makeText(this, "Permission denied to write PDF", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void generatePDF() {
        new Thread(() -> {
            try {
                String pdfPath = Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_DOWNLOADS) + "/timetable.pdf";

                PdfDocument pdfDoc = new PdfDocument(new PdfWriter(pdfPath));
                Document document = new Document(pdfDoc);
                int columnCount = tableLayout.getChildCount() > 0 ?
                        ((TableRow) tableLayout.getChildAt(0)).getChildCount() : 6;
                Table pdfTable = new Table(columnCount);

                // Add table data
                for (int i = 0; i < tableLayout.getChildCount(); i++) {
                    TableRow row = (TableRow) tableLayout.getChildAt(i);
                    for (int j = 0; j < row.getChildCount(); j++) {
                        TextView tv = (TextView) row.getChildAt(j);
                        pdfTable.addCell(tv.getText().toString());
                    }
                }

                document.add(pdfTable);
                document.close();

                runOnUiThread(() -> Toast.makeText(this,
                        "PDF saved to Downloads/timetable.pdf", Toast.LENGTH_LONG).show());

            } catch (FileNotFoundException e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(this,
                        "Error generating PDF", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }
}
