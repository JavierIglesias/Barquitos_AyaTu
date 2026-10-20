package com.ayatu.barquitos.ui.about

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ayatu.barquitos.BuildConfig
import com.ayatu.barquitos.R
import com.ayatu.barquitos.log.BarquitosLogger
import com.ayatu.barquitos.ui.common.BarquitosScreen
import com.ayatu.barquitos.ui.common.onMultiFingerTap

/**
 * El enlace de pago de Stripe que Javier ya usa para el café en Rutas AyaTu y Sudoku AyaTu. Es una
 * propina voluntaria: no desbloquea nada ni está atada a ninguna función de la app.
 */
private const val DONATION_URL = "https://buy.stripe.com/fZu3cvemddIJ6jN9Yl3cc00"

/**
 * «Acerca de»: la versión, el móvil en el que va y el café para el desarrollador, igual que en las
 * demás apps de AyaTu. Los datos del móvil están aquí porque son lo primero que hace falta cuando
 * algo falla y hay que preguntar en qué aparato.
 *
 * Un toque con **tres dedos** en cualquier parte del contenido abre el **registro técnico**
 * ([onLog]), que ya no ocupa sitio en el puerto. Es el mismo gesto, y en el mismo sitio, que en
 * Rutas AyaTu y Audio AyaTu: no hay que explicárselo a quien solo quiere jugar, y quien lo
 * necesita lo tiene siempre a mano.
 */
@Composable
fun AboutScreen(onBack: () -> Unit, onLog: () -> Unit) {
    val context = LocalContext.current
    BarquitosScreen(title = stringResource(R.string.about_title), onBack = onBack) { modifier ->
        Column(
            modifier
                .fillMaxSize()
                .onMultiFingerTap(3) {
                    BarquitosLogger.get(context).logInfo("Registro abierto con 3 dedos")
                    onLog()
                }
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                stringResource(R.string.app_name),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                stringResource(R.string.app_tagline),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(4.dp))

            InfoRow(
                stringResource(R.string.about_version),
                "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
            )
            InfoRow(
                stringResource(R.string.about_device),
                "${Build.MANUFACTURER} ${Build.MODEL}",
            )
            InfoRow(
                stringResource(R.string.about_android),
                "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
            )

            Spacer(Modifier.height(4.dp))

            CoffeeCard {
                // Si el móvil no tuviera navegador, no se puede abrir: no es motivo para reventar.
                runCatching {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(DONATION_URL)))
                }.onFailure { if (it !is ActivityNotFoundException) throw it }
            }

            Text(
                stringResource(R.string.about_no_ads),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            )
        }
    }
}

/** Una línea de «etiqueta … valor», con el valor pegado a la derecha. */
@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

/** La invitación al café, con la taza centrada encima del texto. */
@Composable
private fun CoffeeCard(onClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    ) {
        Column(
            Modifier.fillMaxWidth().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("☕", fontSize = 40.sp)
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.about_coffee_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
            )
            Text(
                stringResource(R.string.about_coffee_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}
