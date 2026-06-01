package studio.acks.reader

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import studio.acks.reader.ui.AcksApp

class MainActivity : ComponentActivity() {

    private val vm by viewModels<ReaderViewModel>()

    /** SAF file picker — no storage permission needed, system grants temporary access. */
    private val pickFile = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            // Persist permission so we can reopen from Recents later
            contentResolver.takePersistableUriPermission(
                uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            vm.importFile(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val state by vm.ui.collectAsState()
            AcksApp(
                state = state,
                vm = vm,
                onPickFile = {
                    pickFile.launch(arrayOf(
                        "text/markdown",
                        "text/html",
                        "text/plain",
                        "application/octet-stream",
                        "*/*"   // fallback to catch .md/.html by extension
                    ))
                }
            )
        }
        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val uri: Uri? = when (intent?.action) {
            Intent.ACTION_VIEW -> intent.data
            Intent.ACTION_SEND -> @Suppress("DEPRECATION")
                intent.getParcelableExtra(Intent.EXTRA_STREAM)
            else -> null
        }
        if (uri != null) vm.importFile(uri)
    }
}
