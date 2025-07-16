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

    public FacultyTimetablePrintAdapter(Context context, TableLayout tableLayout, String teacherName) {
        this.context = context;
        this.tableLayout = tableLayout;
        this.teacherName = teacherName;
        extractTableData();
    }

    private void extractTableData() {
        // Extract text data from each row/cell of the table
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
                        rowData.add(""); // Blank if not a TextView
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
        pageHeight = newAttributes.getMediaSize().getHeightMils() * 72 / 1000; // mils to points
        pageWidth = newAttributes.getMediaSize().getWidthMils() * 72 / 1000;
        // Estimate rowsPerPage: header rows, page title, margins, etc.
        int availableHeight = pageHeight - topMargin - 40; // 40 for bottom margin
        rowsPerPage = Math.max(1, (availableHeight - headerRowHeight) / contentRowHeight);
        PrintDocumentInfo info = new PrintDocumentInfo
                .Builder("faculty_timetable.pdf")
                .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                .build();
        callback.onLayoutFinished(info, true);
    }

    @Override
    public void onWrite(android.print.PageRange[] pages,
                        android.os.ParcelFileDescriptor destination,
                        android.os.CancellationSignal cancellationSignal,
                        WriteResultCallback callback) {
        int totalRows = tableData.size();
        int pageCount = (int) Math.ceil((double) (totalRows - 1) / rowsPerPage); // -1 for header
        int rowIndex = 0;

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

            y += 30;

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
            int colCount = tableData.get(0).size();
            int[] colWidths = new int[colCount];

            // Estimate column widths (divide evenly)
            int tableWidth = pageWidth - 2 * leftMargin;
            for (int c = 0; c < colCount; c++) {
                colWidths[c] = tableWidth / colCount;
            }

            // Header row
            List<String> headerRow = tableData.get(0);
            int cellTop = y + 16;
            int cellLeft = x;
            int cellBottom = cellTop + headerRowHeight;
            int cellRight;

            for (int c = 0; c < colCount; c++) {
                cellRight = cellLeft + colWidths[c];
                // Cell rect
                canvas.drawRect(cellLeft, cellTop, cellRight, cellBottom, cellPaint);
                // Header text
                canvas.drawText(headerRow.get(c), cellLeft + 16, cellTop + headerRowHeight / 2 + 6, textPaint);
                cellLeft = cellRight;
            }
            y = cellBottom;

            textPaint.setTypeface(Typeface.DEFAULT);

            // Content rows for this page
            int start = 1 + pageNum * rowsPerPage;
            int end = Math.min(start + rowsPerPage, totalRows);

            for (int r = start; r < end; r++) {
                List<String> row = tableData.get(r);
                cellLeft = x;
                cellBottom = y + contentRowHeight;
                for (int c = 0; c < colCount; c++) {
                    cellRight = cellLeft + colWidths[c];
                    // Cell rect
                    canvas.drawRect(cellLeft, y, cellRight, cellBottom, cellPaint);
                    // Cell text
                    canvas.drawText(row.get(c), cellLeft + 12, y + contentRowHeight / 2 + 6, textPaint);
                    cellLeft = cellRight;
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
