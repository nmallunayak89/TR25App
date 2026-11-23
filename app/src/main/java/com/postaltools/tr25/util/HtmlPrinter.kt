package com.postaltools.tr25.util

import android.content.Context
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient

/** Lays out report HTML in an off-screen WebView and opens Android's print screen ("Save as PDF"). */
object HtmlPrinter {
    private var keep: WebView? = null   // must stay alive until printing starts

    fun print(ctx: Context, html: String, jobName: String, landscape: Boolean) {
        val wv = WebView(ctx)
        wv.settings.allowFileAccess = true
        wv.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String?) {
                val pm = ctx.getSystemService(Context.PRINT_SERVICE) as PrintManager
                val size = if (landscape) PrintAttributes.MediaSize.ISO_A4.asLandscape() else PrintAttributes.MediaSize.ISO_A4
                val attrs = PrintAttributes.Builder().setMediaSize(size)
                    .setMinMargins(PrintAttributes.Margins.NO_MARGINS).build()
                pm.print(jobName, view.createPrintDocumentAdapter(jobName), attrs)
            }
        }
        // Base URL lets the page load the Palatino-style font from the app's assets.
        wv.loadDataWithBaseURL("file:///android_asset/", html, "text/html", "utf-8", null)
        keep = wv
    }
}
