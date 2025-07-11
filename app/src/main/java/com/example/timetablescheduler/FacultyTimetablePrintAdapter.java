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

public class FacultyTimetablePrintAdapter extends PrintDocumentAdapter {
    private Context context;
    private TableLayout tableLayout;
    private String teacherName;
    private PrintedPdfDocument pdfDocument;

    public FacultyTimetablePrintAdapter(Context context, TableLayout tableLayout, String teacherName) {
        this.context = context;
        this.tableLayout = tableLayout;
        this.teacherName = teacherName;
    }

    @Override
    public void onLayout(PrintAttributes oldAttributes, PrintAttributes newAttributes,
                         android.os.CancellationSignal cancellationSignal,
                         LayoutResultCallback callback, Bundle extras) {
        pdfDocument = new PrintedPdfDocument(context, newAttributes);
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
        PdfDocument.Page page = pdfDocument.startPage(0);
        Canvas canvas = page.getCanvas();

        int x = 40, y = 60;
        Paint paint = new Paint();
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        paint.setTextSize(28);
        canvas.drawText("Faculty Timetable", x, y, paint);

        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        paint.setTextSize(22);
        y += 40;
        canvas.drawText("Name: " + (teacherName != null ? teacherName : ""), x, y, paint);

        y += 40;

        // Render the table as a bitmap (simple approach)
        tableLayout.setDrawingCacheEnabled(true);
        tableLayout.buildDrawingCache();
        Bitmap bitmap = Bitmap.createBitmap(tableLayout.getDrawingCache());
        tableLayout.setDrawingCacheEnabled(false);

        if (bitmap != null) {
            Rect src = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
            Rect dst = new Rect(x, y, x + bitmap.getWidth(), y + bitmap.getHeight());
            canvas.drawBitmap(bitmap, src, dst, null);
        }

        pdfDocument.finishPage(page);

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
