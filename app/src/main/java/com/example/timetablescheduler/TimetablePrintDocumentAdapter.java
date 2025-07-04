package com.example.timetablescheduler;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintDocumentInfo;
import android.view.View;
import android.widget.TableLayout;

public class TimetablePrintDocumentAdapter extends PrintDocumentAdapter {
    private Context context;
    private TableLayout tableLayout;
    private String batch, section, academicYear;

    public TimetablePrintDocumentAdapter(Context context, TableLayout tableLayout, String batch, String section, String academicYear) {
        this.context = context;
        this.tableLayout = tableLayout;
        this.batch = batch;
        this.section = section;
        this.academicYear = academicYear;
    }

    @Override
    public void onLayout(PrintAttributes oldAttributes, PrintAttributes newAttributes,
                         android.os.CancellationSignal cancellationSignal,
                         LayoutResultCallback callback, android.os.Bundle extras) {
        PrintDocumentInfo info = new PrintDocumentInfo
                .Builder("Timetable.pdf")
                .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                .build();
        callback.onLayoutFinished(info, true);
    }

    @Override
    public void onWrite(android.print.PageRange[] pages, android.os.ParcelFileDescriptor destination,
                        android.os.CancellationSignal cancellationSignal,
                        WriteResultCallback callback) {
        PdfDocument pdfDocument = new PdfDocument();
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(1200, 1800, 1).create();
        PdfDocument.Page page = pdfDocument.startPage(pageInfo);

        Canvas canvas = page.getCanvas();
        Paint paint = new Paint();
        paint.setTextSize(36);
        int y = 80;
        canvas.drawText("Batch: " + batch + "  Section: " + section + "  Academic Year: " + academicYear, 40, y, paint);
        y += 60;

        // Render the TableLayout as a bitmap (simple approach)
        tableLayout.setDrawingCacheEnabled(true);
        tableLayout.buildDrawingCache();
        android.graphics.Bitmap bitmap = android.graphics.Bitmap.createBitmap(tableLayout.getDrawingCache());
        canvas.drawBitmap(bitmap, 40, y, paint);
        tableLayout.setDrawingCacheEnabled(false);

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
