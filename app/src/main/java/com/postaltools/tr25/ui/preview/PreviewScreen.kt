package com.postaltools.tr25.ui.preview

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.postaltools.tr25.TrApp
import com.postaltools.tr25.data.TaBill
import com.postaltools.tr25.logic.Calculator
import com.postaltools.tr25.logic.Money
import com.postaltools.tr25.ui.theme.OkGreen
import com.postaltools.tr25.util.PdfFiles
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/** Renders each PDF page to a bitmap for on-screen preview. */
private fun renderPages(file: File, widthPx: Int = 1240): List<ImageBitmap> {
    ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { fd ->
        PdfRenderer(fd).use { r ->
            return (0 until r.pageCount).map { i ->
                r.openPage(i).use { p ->
                    val h = (widthPx.toFloat() * p.height / p.width).toInt()
                    val bmp = Bitmap.createBitmap(widthPx, h, Bitmap.Config.ARGB_8888)
                    bmp.eraseColor(android.graphics.Color.WHITE)
                    p.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)
                    bmp.asImageBitmap()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreviewScreen(
    billId: Long,
    action: String,
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    onSavedDone: () -> Unit,
) {
    val ctx = LocalContext.current
    val app = ctx.applicationContext as TrApp
    val scope = rememberCoroutineScope()
    val snack = remember { SnackbarHostState() }

    var bill by remember { mutableStateOf<TaBill?>(null) }
    var file by remember { mutableStateOf<File?>(null) }
    var pages by remember { mutableStateOf<List<ImageBitmap>>(emptyList()) }
    var failure by remember { mutableStateOf<String?>(null) }

    val saveAs = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        val f = file
        if (uri != null && f != null) {
            scope.launch {
                val ok = withContext(Dispatchers.IO) { runCatching { PdfFiles.copyTo(ctx, f, uri) } }
                snack.showSnackbar(if (ok.isSuccess) "PDF saved: ${f.name}" else "Could not save: ${ok.exceptionOrNull()?.message}")
            }
        }
    }

    LaunchedEffect(billId) {
        try {
            val b = app.repo.load(billId) ?: error("Bill not found")
            bill = b
            val f = withContext(Dispatchers.IO) { PdfFiles.generate(ctx, b) }
            app.repo.setPdfPath(billId, f.absolutePath)
            pages = withContext(Dispatchers.IO) { renderPages(f) }
            file = f
            when (action) {
                "print" -> PdfFiles.print(ctx, f)
                "share" -> PdfFiles.share(ctx, f)
                else -> snack.showSnackbar("TR-25 bill generated: ${f.name}")
            }
        } catch (e: Exception) {
            failure = e.message ?: "Could not generate the PDF"
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snack) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Bill Preview")
                        bill?.let {
                            Text(
                                "Sub-Bill No. ${it.subBillNo.ifBlank { "—" }} · Net ${Money.rupees(Calculator.compute(it).net)}",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                ),
            )
        },
        bottomBar = {
            val f = file
            if (f != null) {
                Surface(shadowElevation = 8.dp) {
                    Column(
                        Modifier.navigationBarsPadding().padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilledTonalButton({ PdfFiles.openExternal(ctx, f) }, Modifier.weight(1f)) { Text("PREVIEW PDF") }
                            FilledTonalButton({ saveAs.launch(f.name) }, Modifier.weight(1f)) { Text("DOWNLOAD PDF") }
                            FilledTonalButton({ PdfFiles.print(ctx, f) }, Modifier.weight(1f)) { Text("PRINT BILL") }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilledTonalButton({ PdfFiles.share(ctx, f) }, Modifier.weight(1f)) { Text("SHARE PDF") }
                            OutlinedButton({ onEdit(billId) }, Modifier.weight(1f)) { Text("EDIT DATA") }
                            Button({
                                Toast.makeText(ctx, "Bill saved on this phone", Toast.LENGTH_SHORT).show()
                                onSavedDone()
                            }, Modifier.weight(1f)) { Text("SAVE BILL") }
                        }
                    }
                }
            }
        },
    ) { pad ->
        Box(Modifier.padding(pad).fillMaxSize().background(Color(0xFF9E9E9E))) {
            when {
                failure != null -> Column(
                    Modifier.align(Alignment.Center).background(Color.White).padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text("Could not create the PDF", style = MaterialTheme.typography.titleMedium)
                    Text(failure ?: "")
                    Button(onClick = { onEdit(billId) }) { Text("EDIT DATA") }
                }
                pages.isEmpty() -> Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = Color.White)
                    Text("Preparing the bill…", color = Color.White, modifier = Modifier.padding(top = 10.dp))
                }
                else -> LazyColumn(
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item {
                        Text(
                            "✔ ${file?.name ?: ""} · ${pages.size} page${if (pages.size == 1) "" else "s"} · A4. " +
                                "Check every entry before printing and signing.",
                            color = Color.White,
                            modifier = Modifier.background(OkGreen).padding(10.dp).fillMaxWidth(),
                        )
                    }
                    itemsIndexed(pages) { i, img ->
                        Image(
                            bitmap = img,
                            contentDescription = "Page ${i + 1}",
                            contentScale = ContentScale.FillWidth,
                            modifier = Modifier.fillMaxWidth()
                                .aspectRatio(img.width.toFloat() / img.height)
                                .background(Color.White)
                                .border(1.dp, Color(0xFF616161)),
                        )
                    }
                }
            }
        }
    }
}
