package com.ayatu.barquitos.ui.docs

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.ayatu.barquitos.ui.common.BarquitosScreen
import com.ayatu.barquitos.ui.common.MarkdownView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Qué documento se enseña. */
enum class DocKind(val asset: String) {
    MANUAL("manual_usuario.md"),
    NOVEDADES("novedades.md"),
    HISTORIAL("historial.md"),
}

/**
 * Enseña dentro de la app los documentos vivos del proyecto. Se empaquetan como assets desde
 * `Docs/` con la tarea `syncDocs` de Gradle, así que lo que se lee aquí es exactamente el mismo
 * fichero que se mantiene en el repositorio.
 */
@Composable
fun DocsScreen(kind: DocKind, title: String, onBack: () -> Unit) {
    val context = LocalContext.current
    var text by remember(kind) { mutableStateOf("") }

    LaunchedEffect(kind) {
        text = withContext(Dispatchers.IO) {
            runCatching {
                context.assets.open(kind.asset).bufferedReader().use { it.readText() }
            }.getOrElse { "No se pudo abrir el documento (${kind.asset})." }
        }
    }

    BarquitosScreen(title = title, onBack = onBack) { modifier ->
        MarkdownView(
            text = text,
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
        )
    }
}
