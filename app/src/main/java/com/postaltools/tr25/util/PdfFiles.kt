package com.postaltools.tr25.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.widget.Toast
import androidx.core.content.FileProvider
import com.postaltools.tr25.data.TaBill
import com.postaltools.tr25.logic.Dates
import com.postaltools.tr25.pdf.Tr25aPdf
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.time.LocalDate

/** Creating, printing, sharing and saving the bill PDF. PDFs live in the app's private storage. */
object PdfFiles {

    /** e.g. TR25_Bill_001_2026-10-17.pdf */
    fun fileName(b: TaBill): String {
        val no = b.subBillNo.trim().ifBlank { "ID${b.id}" }.replace(Regex("[^A-Za-z0-9-]"), "_")
        val date = (Dates.parseDate(b.claimDate) ?: LocalDate.now()).format(Dates.FILE)
        return "TR25_Bill_${no}_$date.pdf"
    }

    fun pdfDir(ctx: Context) = File(ctx.filesDir, "pdfs").apply { mkdirs() }

    /** Generates the PDF and returns the file. Runs on the caller's thread – call from a background dispatcher. */
    fun generate(ctx: Context, b: TaBill): File {
        val f = File(pdfDir(ctx), fileName(b))
        Tr25aPdf.write(b, f)
        return f
    }

    private fun uriFor(ctx: Context, f: File): Uri =
        FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", f)

    fun share(ctx: Context, f: File) {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uriFor(ctx, f))
            putExtra(Intent.EXTRA_SUBJECT, f.nameWithoutExtension)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        ctx.startActivity(Intent.createChooser(send, "Share TR-25 bill"))
    }

    /** Opens the PDF in another installed PDF viewer, if there is one. */
    fun openExternal(ctx: Context, f: File) {
        val view = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uriFor(ctx, f), "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        try {
            ctx.startActivity(view)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(ctx, "No PDF viewer app installed. Use the preview in this app.", Toast.LENGTH_LONG).show()
        }
    }

    /** Copies the PDF to a location the user picked with the system "Save as" screen. */
    fun copyTo(ctx: Context, f: File, dest: Uri) {
        ctx.contentResolver.openOutputStream(dest)?.use { out ->
            FileInputStream(f).use { it.copyTo(out) }
        } ?: error("Could not open the selected location")
    }

    fun print(ctx: Context, f: File) {
        val pm = ctx.getSystemService(Context.PRINT_SERVICE) as PrintManager
        val attrs = PrintAttributes.Builder()
            .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
            .setColorMode(PrintAttributes.COLOR_MODE_MONOCHROME)
            .build()
        pm.print(f.nameWithoutExtension, FilePrintAdapter(f), attrs)
    }

    private class FilePrintAdapter(private val file: File) : PrintDocumentAdapter() {
        override fun onLayout(
            oldAttributes: PrintAttributes?, newAttributes: PrintAttributes?,
            cancellationSignal: CancellationSignal?, callback: LayoutResultCallback, extras: Bundle?,
        ) {
            if (cancellationSignal?.isCanceled == true) { callback.onLayoutCancelled(); return }
            val info = PrintDocumentInfo.Builder(file.name)
                .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                .build()
            callback.onLayoutFinished(info, oldAttributes != newAttributes)
        }

        override fun onWrite(
            pages: Array<out PageRange>?, destination: ParcelFileDescriptor,
            cancellationSignal: CancellationSignal?, callback: WriteResultCallback,
        ) {
            try {
                FileInputStream(file).use { input ->
                    FileOutputStream(destination.fileDescriptor).use { input.copyTo(it) }
                }
                if (cancellationSignal?.isCanceled == true) callback.onWriteCancelled()
                else callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
            } catch (e: Exception) {
                callback.onWriteFailed(e.message)
            }
        }
    }
}
