package com.ayatu.barquitos.ui.docs

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ayatu.barquitos.BarquitosApplication
import com.ayatu.barquitos.R
import com.ayatu.barquitos.ui.common.BarquitosScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Visor del registro de eventos, con el mismo comportamiento que en Rutas Ayatu y Agueda: se
 * refresca solo mientras se mira el fichero activo, se puede navegar por los ficheros rotados, se
 * puede seleccionar y copiar el texto, y se puede compartir el fichero entero.
 */
class LogViewModel(application: Application) : AndroidViewModel(application) {

    private val logger = (application as BarquitosApplication).logger

    data class NavState(val canGoOlder: Boolean, val canGoNewer: Boolean)

    private val _files = MutableStateFlow<List<File>>(emptyList())
    private val _selectedFile = MutableStateFlow<File?>(null)
    val selectedFile: StateFlow<File?> = _selectedFile.asStateFlow()

    private val _lines = MutableStateFlow<List<String>>(emptyList())
    val lines: StateFlow<List<String>> = _lines.asStateFlow()

    val navState: StateFlow<NavState> = combine(_files, _selectedFile) { files, selected ->
        val index = files.indexOf(selected)
        NavState(canGoOlder = index in 0 until files.lastIndex, canGoNewer = index > 0)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, NavState(false, false))

    init {
        refreshFileList()
        viewModelScope.launch {
            while (true) {
                delay(AUTO_REFRESH_MS)
                if (_selectedFile.value == _files.value.firstOrNull()) reload()
            }
        }
    }

    fun refreshFileList() {
        viewModelScope.launch {
            val list = withContext(Dispatchers.IO) { logger.listLogFiles() }
            _files.value = list
            if (_selectedFile.value == null || _selectedFile.value !in list) {
                _selectedFile.value = list.firstOrNull()
            }
            reload()
        }
    }

    fun selectOlder() {
        val list = _files.value
        val index = list.indexOf(_selectedFile.value)
        if (index in 0 until list.lastIndex) select(list[index + 1])
    }

    fun selectNewer() {
        val list = _files.value
        val index = list.indexOf(_selectedFile.value)
        if (index > 0) select(list[index - 1])
    }

    private fun select(file: File) {
        _selectedFile.value = file
        reload()
    }

    private fun reload() {
        val file = _selectedFile.value ?: run { _lines.value = emptyList(); return }
        viewModelScope.launch {
            _lines.value = withContext(Dispatchers.IO) {
                if (file.exists()) runCatching { file.readLines() }.getOrDefault(emptyList()) else emptyList()
            }
        }
    }

    private companion object {
        const val AUTO_REFRESH_MS = 5_000L
    }
}

@Composable
fun LogScreen(onBack: () -> Unit, viewModel: LogViewModel = viewModel()) {
    val context = LocalContext.current
    val lines by viewModel.lines.collectAsState()
    val nav by viewModel.navState.collectAsState()
    val selected by viewModel.selectedFile.collectAsState()
    val scrollState = rememberScrollState()
    val copiedTemplate = stringResource(R.string.log_copied, lines.size)

    LaunchedEffect(lines.size) { scrollState.scrollTo(scrollState.maxValue) }

    BarquitosScreen(
        title = selected?.nameWithoutExtension ?: stringResource(R.string.log_title),
        onBack = onBack,
        actions = {
            IconButton(onClick = viewModel::selectOlder, enabled = nav.canGoOlder) {
                Icon(Icons.Default.ChevronLeft, contentDescription = stringResource(R.string.log_older))
            }
            IconButton(onClick = viewModel::selectNewer, enabled = nav.canGoNewer) {
                Icon(Icons.Default.ChevronRight, contentDescription = stringResource(R.string.log_newer))
            }
            IconButton(onClick = viewModel::refreshFileList) {
                Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.log_refresh))
            }
            IconButton(
                onClick = {
                    // Todo el fichero al portapapeles, para poder pegarlo en un chat en vez de
                    // tener que mandar una captura de pantalla.
                    val clipboard = context.getSystemService(ClipboardManager::class.java)
                    clipboard?.setPrimaryClip(
                        ClipData.newPlainText("Registro de Barquitos AyaTu", lines.joinToString("\n")),
                    )
                    Toast.makeText(context, copiedTemplate, Toast.LENGTH_SHORT).show()
                },
                enabled = lines.isNotEmpty(),
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = stringResource(R.string.log_copy))
            }
            IconButton(onClick = {
                val file = selected ?: return@IconButton
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(
                    Intent.createChooser(intent, context.getString(R.string.log_share_title)),
                )
            }) {
                Icon(Icons.Default.Share, contentDescription = stringResource(R.string.log_share))
            }
        },
    ) { modifier ->
        Box(modifier.fillMaxSize()) {
            if (lines.isEmpty()) {
                Text(
                    stringResource(R.string.log_empty),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(24.dp),
                )
            } else {
                // SelectionContainer: al mantener pulsado se selecciona el texto y aparece
                // «Copiar», que un Text normal no permite.
                SelectionContainer(Modifier.fillMaxSize()) {
                    Text(
                        text = coloredLog(lines),
                        fontFamily = FontFamily.Monospace,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState)
                            .padding(12.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun coloredLog(lines: List<String>) = buildAnnotatedString {
    val error = Color(0xFFEF5350)
    val aviso = Color(0xFFFFB300)
    val inicioFin = Color(0xFF66BB6A)
    val normal = MaterialTheme.colorScheme.onBackground
    lines.forEach { line ->
        val color = when {
            line.contains("ERROR") -> error
            line.contains("AVISO") -> aviso
            line.contains("INICIO") || line.contains("FIN") -> inicioFin
            else -> normal
        }
        withStyle(
            SpanStyle(
                color = color,
                fontWeight = if (color != normal) FontWeight.Bold else null,
            ),
        ) { append(line) }
        append("\n")
    }
}
