package com.example.timetablescheduler;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.pdf.PdfDocument;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.print.PageRange;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintDocumentInfo;
import android.view.View;

import java.io.FileOutputStream;
import java.io.IOException;

public class TimetablePrintDocumentAdapter extends PrintDocumentAdapter {
    private Context context;
    private View tableLayoutView;
    private String docName;

    public TimetablePrintDocumentAdapter(Context context, View tableLayoutView, String batch, String section, String year) {
        this.context = context;
        this.tableLayoutView = tableLayoutView;
        this.docName = "Timetable_" + (batch != null ? batch : "") +
                (section != null && !section.isEmpty() ? "_" + section : "") +
                (year != null && !year.isEmpty() ? "_" + year : "") + ".pdf";
    }

    @Override
    public void onLayout(PrintAttributes oldAttributes, PrintAttributes newAttributes,
                         CancellationSignal cancellationSignal,
                         LayoutResultCallback callback, android.os.Bundle extras) {
        PrintDocumentInfo info = new PrintDocumentInfo
                .Builder(docName)
                .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                .build();
        callback.onLayoutFinished(info, true);
    }

    @Override
    public void onWrite(final PageRange[] pages,
                        final ParcelFileDescriptor destination,
                        final CancellationSignal cancellationSignal,
                        final WriteResultCallback callback) {
        // Measure and layout the view to its full height
        int widthSpec = View.MeasureSpec.makeMeasureSpec(tableLayoutView.getWidth(), View.MeasureSpec.EXACTLY);
        int heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        tableLayoutView.measure(widthSpec, heightSpec);
        tableLayoutView.layout(0, 0, tableLayoutView.getMeasuredWidth(), tableLayoutView.getMeasuredHeight());

        // Create bitmap of the full view
        Bitmap bitmap = Bitmap.createBitmap(tableLayoutView.getMeasuredWidth(),
                tableLayoutView.getMeasuredHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        tableLayoutView.draw(canvas);

        // Create PDF document
        PdfDocument document = new PdfDocument();

        int pageWidth = tableLayoutView.getMeasuredWidth();
        int pageHeight = 1120; // A4 height in pixels at 160 dpi (adjust as needed)
        int totalHeight = tableLayoutView.getMeasuredHeight();
        int pageCount = (int) Math.ceil((double) totalHeight / pageHeight);

        for (int i = 0; i < pageCount; i++) {
            PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, i + 1).create();
            PdfDocument.Page page = document.startPage(pageInfo);

            Canvas pdfCanvas = page.getCanvas();
            int top = i * pageHeight;
            pdfCanvas.drawBitmap(bitmap, 0, -top, null);

            document.finishPage(page);
        }

        try (FileOutputStream out = new FileOutputStream(destination.getFileDescriptor())) {
            document.writeTo(out);
            callback.onWriteFinished(new PageRange[]{PageRange.ALL_PAGES});
        } catch (IOException e) {
            callback.onWriteFailed(e.toString());
        } finally {
            document.close();
            bitmap.recycle();
        }
    }
}
