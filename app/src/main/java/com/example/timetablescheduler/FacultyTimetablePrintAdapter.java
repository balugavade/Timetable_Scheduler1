package com.example.timetablescheduler;

import android.content.Context;
import android.graphics.*;
import android.graphics.pdf.PdfDocument;
import android.os.Bundle;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintDocumentInfo;
import android.print.pdf.PrintedPdfDocument;
import android.view.View;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;

public class FacultyTimetablePrintAdapter extends PrintDocumentAdapter {

    private Context context;
    private TableLayout tableLayout;
    private String teacherName;
    private PrintedPdfDocument pdfDocument;
    private int pageHeight, pageWidth;
    private int rowsPerPage;
    private List<List<String>> tableData = new ArrayList<>();
    private int headerRowHeight = 60;
    private int contentRowHeight = 44;
    private int topMargin = 90;
    private int leftMargin = 40;
    private int rightMargin = 40;
    private int cellPadding = 12;

    // For column width calculation
    private int[] colWidths;

    public FacultyTimetablePrintAdapter(Context context, TableLayout tableLayout, String teacherName) {
        this.context = context;
        this.tableLayout = tableLayout;
        this.teacherName = teacherName;
        extractTableData();
    }

    private void extractTableData() {
        tableData.clear();
        int nRows = tableLayout.getChildCount();
        for (int i = 0; i < nRows; i++) {
            View child = tableLayout.getChildAt(i);
            if (child instanceof TableRow) {
                TableRow row = (TableRow) child;
                List<String> rowData = new ArrayList<>();
                for (int j = 0; j < row.getChildCount(); j++) {
                    View cell = row.getChildAt(j);
                    if (cell instanceof TextView) {
                        rowData.add(((TextView) cell).getText().toString());
                    } else {
                        rowData.add("");
                    }
                }
                tableData.add(rowData);
            }
        }
    }

    @Override
    public void onLayout(PrintAttributes oldAttributes, PrintAttributes newAttributes,
                         android.os.CancellationSignal cancellationSignal,
                         LayoutResultCallback callback, Bundle extras) {
        pdfDocument = new PrintedPdfDocument(context, newAttributes);
        pageHeight = newAttributes.getMediaSize().getHeightMils() * 72 / 1000;
        pageWidth = newAttributes.getMediaSize().getWidthMils() * 72 / 1000;

        measureColumnWidths();

        // Estimate rows per page
        int availableHeight = pageHeight - topMargin - 40; // 40 for bottom margin
        rowsPerPage = Math.max(1, (availableHeight - headerRowHeight) / contentRowHeight);

        PrintDocumentInfo info = new PrintDocumentInfo
                .Builder("faculty_timetable.pdf")
                .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                .build();
        callback.onLayoutFinished(info, true);
    }

    private void measureColumnWidths() {
        if (tableData.isEmpty()) return;
        int colCount = tableData.get(0).size();
        colWidths = new int[colCount];

        // Use a paint object to measure text width
        Paint paint = new Paint();
        paint.setTextSize(16f); // standard for content
        paint.setTypeface(Typeface.DEFAULT);

        // First, measure headers (bold)
        Paint headerPaint = new Paint(paint);
        headerPaint.setTypeface(Typeface.DEFAULT_BOLD);

        for (int c = 0; c < colCount; c++) {
            String text = tableData.get(0).get(c);
            float width = headerPaint.measureText(text);
            colWidths[c] = (int) width + 2 * cellPadding;
        }
        // Now, scan all data rows
        for (int r = 1; r < tableData.size(); r++) {
            List<String> row = tableData.get(r);
            for (int c = 0; c < colCount; c++) {
                String text = row.get(c);
                float width = paint.measureText(text);
                if ((int) width + 2 * cellPadding > colWidths[c]) {
                    colWidths[c] = (int) width + 2 * cellPadding;
                }
            }
        }

        // Now, check if total width exceeds page width, and scale down if needed
        int tableWidth = 0;
        for (int w : colWidths) tableWidth += w;
        int maxTableWidth = pageWidth - leftMargin - rightMargin;
        if (tableWidth > maxTableWidth) {
            float scale = (float) maxTableWidth / (float) tableWidth;
            for (int i = 0; i < colCount; i++) {
                colWidths[i] = Math.round(colWidths[i] * scale);
            }
        }
    }

    @Override
    public void onWrite(android.print.PageRange[] pages,
                        android.os.ParcelFileDescriptor destination,
                        android.os.CancellationSignal cancellationSignal,
                        WriteResultCallback callback) {
        if (tableData.isEmpty() || colWidths == null) {
            callback.onWriteFinished(new android.print.PageRange[]{android.print.PageRange.ALL_PAGES});
            return;
        }

        int totalRows = tableData.size();
        int pageCount = (int) Math.ceil((double) (totalRows - 1) / rowsPerPage);

        for (int pageNum = 0; pageNum < pageCount; pageNum++) {
            PdfDocument.Page page = pdfDocument.startPage(pageNum);

            Canvas canvas = page.getCanvas();

            // Title + teacher
            Paint headerPaint = new Paint();
            headerPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            headerPaint.setTextSize(28);
            headerPaint.setColor(Color.BLACK);

            int y = topMargin - 25;
            canvas.drawText("Faculty Timetable", leftMargin, y, headerPaint);

            headerPaint.setTextSize(20);
            headerPaint.setTypeface(Typeface.DEFAULT);
            y += 36;
            canvas.drawText("Name: " + (teacherName != null ? teacherName : ""), leftMargin, y, headerPaint);

            y += 30; // space under title

            // Draw table header
            Paint cellPaint = new Paint();
            cellPaint.setStyle(Paint.Style.STROKE);
            cellPaint.setColor(Color.BLACK);
            cellPaint.setStrokeWidth(2);

            Paint textPaint = new Paint();
            textPaint.setColor(Color.BLACK);
            textPaint.setTextSize(16);
            textPaint.setTypeface(Typeface.DEFAULT_BOLD);

            int x = leftMargin;
            int colCount = colWidths.length;

            // Calculate starting cell positions
            List<String> headerRow = tableData.get(0);
            int cellTop = y + 12;
            int cellBottom = cellTop + headerRowHeight;

            for (int c = 0; c < colCount; c++) {
                int cellLeft = x;
                int cellRight = x + colWidths[c];
                // Cell rect
                canvas.drawRect(cellLeft, cellTop, cellRight, cellBottom, cellPaint);

                // Center header text in cell
                String text = headerRow.get(c);
                float textWidth = textPaint.measureText(text);
                float tx = cellLeft + (colWidths[c] - textWidth) / 2;
                float ty = cellTop + (headerRowHeight + textPaint.getTextSize()/2) / 2 + 3;
                canvas.drawText(text, tx, ty, textPaint);

                x = cellRight;
            }
            y = cellBottom;

            textPaint.setTypeface(Typeface.DEFAULT);

            // Rows for this page
            int start = 1 + pageNum * rowsPerPage;
            int end = Math.min(start + rowsPerPage, totalRows);

            for (int r = start; r < end; r++) {
                List<String> row = tableData.get(r);
                x = leftMargin;
                cellBottom = y + contentRowHeight;
                for (int c = 0; c < colCount; c++) {
                    int cellLeft = x;
                    int cellRight = x + colWidths[c];
                    canvas.drawRect(cellLeft, y, cellRight, cellBottom, cellPaint);

                    // Center cell text
                    String text = row.get(c);
                    float textWidth = textPaint.measureText(text);
                    float tx = cellLeft + (colWidths[c] - textWidth) / 2;
                    float ty = y + (contentRowHeight + textPaint.getTextSize()/2) / 2 + 2;
                    canvas.drawText(text, tx, ty, textPaint);

                    x = cellRight;
                }
                y = cellBottom;
            }

            pdfDocument.finishPage(page);
        }

        try {
            pdfDocument.writeTo(new java.io.FileOutputStream(destination.getFileDescriptor()));
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            pdfDocument.close();
        }

        callback.onWriteFinished(new android.print.PageRange[]{android.print.PageRange.ALL_PAGES});
    }
}
