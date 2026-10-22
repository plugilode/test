package de.schimmelpilz.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Balance
import androidx.compose.material.icons.outlined.Contacts
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.schimmelpilz.app.AppState
import de.schimmelpilz.app.BuildConfig
import de.schimmelpilz.app.ThemeMode
import de.schimmelpilz.app.data.Billing
import de.schimmelpilz.app.data.Dates
import de.schimmelpilz.app.ui.Avatar
import de.schimmelpilz.app.ui.LocalLang
import de.schimmelpilz.app.ui.SectionLabel
import de.schimmelpilz.app.ui.BRAND
import de.schimmelpilz.app.ui.BrandLockup
import de.schimmelpilz.app.ui.tr

@Composable
fun MoreScreen(state: AppState) {
    val user by state.user.collectAsStateWithLifecycle()
    val theme by state.themeMode.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lang = LocalLang.current
    var billing by remember { mutableStateOf<Billing?>(null) }
    LaunchedEffect(Unit) { billing = runCatching { state.api.billing() }.getOrNull() }

    Column(Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        ScreenTitle(tr("Mehr", "More"))
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(MaterialTheme.colorScheme.surfaceVariant).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Avatar(user?.initials ?: "", size = 48.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(user?.fullName ?: "", style = MaterialTheme.typography.titleMedium)
                Text(listOfNotNull(user?.organisation?.ifBlank { null }, user?.email).joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        SectionLabel(tr("In der Web-App", "In the web app"))
        Text(tr("Diese Bereiche öffnen sich im Browser, mit demselben Konto.", "These sections open in the browser with the same account."),
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Links(listOf(
            Triple(Icons.Outlined.Contacts, tr("Adressen", "Address book"), ""),
            Triple(Icons.Outlined.EditNote, tr("Schriftverkehr & Entwürfe", "Correspondence & drafts"), ""),
            Triple(Icons.AutoMirrored.Outlined.ReceiptLong, tr("Kostenprüfung", "Cost review"), ""),
            Triple(Icons.Outlined.Balance, tr("Verfahrenswege", "Procedural routes"), ""),
            Triple(Icons.Outlined.Groups, tr("Team & Rechte", "Team & access"), ""),
            Triple(Icons.Outlined.Settings, tr("Profil, Briefpapier & Sicherheit", "Profile, letter paper & security"), ""),
        )) { openWeb(context) }

        SectionLabel(tr("Abonnement", "Subscription"))
        Surface(shape = RoundedCornerShape(20.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                val b = billing
                Text(when (b?.effectiveStatus) {
                    "pilot" -> tr("Kostenloser Pilotzugang", "Free pilot access")
                    "trial" -> tr("Testphase bis ", "Trial until ") + Dates.label(b.trialEnd, lang)
                    "active" -> tr("Aktiv bis ", "Active until ") + Dates.label(b.periodEnd, lang)
                    "past_due" -> tr("Zahlung fehlgeschlagen – bitte Zahlungsmittel aktualisieren", "Payment failed – please update your payment method")
                    null -> "…"
                    else -> tr("Kein aktiver Zugang – Lesen bleibt möglich", "No active access – reading remains available")
                }, style = MaterialTheme.typography.titleSmall)
                Text(tr("Abos verwalten Sie derzeit in der Web-App.", "Subscriptions are currently managed in the web app."), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                TextButton(onClick = { openWeb(context) }, contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)) { Text(tr("Abo öffnen", "Open subscription")) }
            }
        }

        SectionLabel(tr("Darstellung", "Appearance"))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Language, null); Spacer(Modifier.width(12.dp))
            Text(tr("Sprache", "Language"), Modifier.weight(1f))
            LanguageSwitch(state, Modifier.width(140.dp))
        }
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            listOf(ThemeMode.System to tr("System", "System"), ThemeMode.Light to tr("Hell", "Light"), ThemeMode.Dark to tr("Dunkel", "Dark")).forEachIndexed { index, (mode, label) ->
                SegmentedButton(theme == mode, { state.setTheme(mode) }, SegmentedButtonDefaults.itemShape(index, 3)) { Text(label) }
            }
        }
        TextButton(onClick = { state.signOut() }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.AutoMirrored.Outlined.Logout, null, tint = MaterialTheme.colorScheme.error); Spacer(Modifier.width(8.dp))
            Text(tr("Abmelden", "Sign out"), color = MaterialTheme.colorScheme.error)
        }
        BrandLockup(Modifier.padding(top = 8.dp), scale = 0.85f, fullWidth = 240.dp)
        Text("$BRAND ${BuildConfig.VERSION_NAME} · " + tr("Allgemeine Rechtsinformation, keine anwaltliche Beratung im Einzelfall.", "General legal information, not individual legal advice."),
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 24.dp))
    }
}

@Composable
private fun Links(rows: List<Triple<ImageVector, String, String>>, onClick: () -> Unit) {
    Surface(shape = RoundedCornerShape(20.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
        Column {
            rows.forEachIndexed { index, (icon, label, _) ->
                Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(icon, null, tint = MaterialTheme.colorScheme.primary); Spacer(Modifier.width(14.dp))
                    Text(label, Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
                    Icon(Icons.AutoMirrored.Outlined.OpenInNew, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (index < rows.lastIndex) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    }
}
