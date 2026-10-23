package de.schimmelpilz.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.AddTask
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.DocumentScanner
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.UploadFile
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.schimmelpilz.app.AppState
import de.schimmelpilz.app.data.ApiException
import de.schimmelpilz.app.data.CaseFile
import de.schimmelpilz.app.data.Dates
import de.schimmelpilz.app.ui.LocalLang
import de.schimmelpilz.app.ui.Pill
import de.schimmelpilz.app.ui.SectionLabel
import de.schimmelpilz.app.ui.Spark
import de.schimmelpilz.app.ui.errorText
import de.schimmelpilz.app.ui.rememberFileOpener
import de.schimmelpilz.app.ui.legalAreaLabel
import de.schimmelpilz.app.ui.statusPill
import de.schimmelpilz.app.ui.theme.LocalBrand
import de.schimmelpilz.app.ui.tr
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CaseDetailScreen(state: AppState, id: String, onBack: () -> Unit, onAsk: (String) -> Unit) {
    val cases by state.cases.collectAsStateWithLifecycle()
    val item = cases.firstOrNull { it.id == id }
    val lang = LocalLang.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var tab by remember { mutableIntStateOf(0) }
    var addTask by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    val uploadFailed = tr("Hochladen fehlgeschlagen.", "Upload failed.")
    val tooLarge = tr("Datei ist größer als 10 MB.", "File is larger than 10 MB.")
    val uploaded = tr("Dokument zur Akte hinzugefügt – der Text wird jetzt erkannt.", "Document added to the case – its text is being read now.")
    val files = rememberFileOpener(state.api)
    val docs = rememberDocumentActions(
        onFile = { file ->
            busy = true; message = null
            scope.launch {
                try { state.api.uploadToCase(id, file.name, file.mime, file.bytes); state.refresh().join(); message = uploaded }
                catch (e: ApiException) { message = uploadFailed + " " + e.detail }
                finally { busy = false }
            }
        },
        onError = { message = if (it == "too_large") tooLarge else uploadFailed },
    )

    // The server reads new uploads (OCR for photos and scans) after answering; refresh until it is done.
    val reading = item?.documents?.any { it.reading } == true
    LaunchedEffect(reading, cases) { if (reading) { delay(3000); state.refresh() } }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, tr("Zurück zu Akten", "Back to cases")) }
            Text(item?.reference ?: "", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary)
        }
        if (busy || files.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        if (item == null) { Text(tr("Akte nicht gefunden.", "Case not found."), Modifier.padding(20.dp)); return@Column }
        Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Column(Modifier.padding(horizontal = 4.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(item.title, style = MaterialTheme.typography.headlineSmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    val (status, colors) = statusPill(item.details.status)
                    Pill(status, colors.first, colors.second)
                    val neutral = MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
                    Pill(legalAreaLabel(item.details.legalArea), neutral.first, neutral.second)
                    stageLabel(item.details.stage)?.let { Pill(it, neutral.first, neutral.second) }
                    item.details.disputeValue?.let { value ->
                        val money = NumberFormat.getCurrencyInstance(if (lang == "de") Locale.GERMANY else Locale.UK).apply { currency = java.util.Currency.getInstance(item.details.currency) }.format(value)
                        Pill(tr("Wert ", "Value ") + money, neutral.first, neutral.second)
                    }
                }
            }
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp))
                    .background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.primaryContainer, LocalBrand.current.warning)))
                    .clickable { onAsk(item.id) }.padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Spark(size = 24.dp); Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(tr("Frage zu dieser Akte", "Ask about this case"), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Text(tr("Kennt Sachverhalt, Dokumente und Fristen", "Knows facts, documents and deadlines"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
                }
                Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                QuickAction(Icons.Outlined.DocumentScanner, tr("Scannen", "Scan"), Modifier.weight(1f), docs.scan)
                QuickAction(Icons.Outlined.UploadFile, tr("Datei", "File"), Modifier.weight(1f), docs.pick)
                QuickAction(Icons.Outlined.AddTask, tr("Frist", "Deadline"), Modifier.weight(1f)) { addTask = true }
            }
            message?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary) }
            PrimaryScrollableTabRow(selectedTabIndex = tab, edgePadding = 0.dp, containerColor = MaterialTheme.colorScheme.surface) {
                listOf(tr("Übersicht", "Overview"), tr("Dokumente", "Documents") + " ${item.documents.size}", tr("Schriftsätze", "Drafts") + " ${item.drafts.size}", tr("Fristen", "Deadlines") + " ${item.tasks.count { !it.completed }}")
                    .forEachIndexed { index, label -> Tab(selected = tab == index, onClick = { tab = index }, text = { Text(label) }) }
            }
            when (tab) {
                0 -> Overview(item, lang)
                1 -> ListCard(item.documents.map { doc ->
                    ListRow(Icons.Outlined.Description, doc.name, Dates.label(doc.createdAt, lang) + " · " + when {
                        doc.historical != null -> tr("Historisch (berichtigt)", "Historical (corrected)")
                        doc.reading -> tr("Text wird erkannt …", "Reading text …")
                        doc.reviewedUnits > 0 -> tr("Text geprüft", "Text reviewed")
                        doc.extractionStatus == "ready" && doc.ocrPages > 0 -> tr("Text per OCR erkannt, Prüfung offen", "Text recognised by OCR, review pending")
                        doc.extractionStatus == "ready" -> tr("Text erkannt, Prüfung offen", "Text extracted, review pending")
                        doc.autoTextStatus == "no_text" -> tr("Kein Text erkannt", "No text recognised")
                        doc.autoTextStatus == "ocr_unavailable" -> tr("Texterkennung auf dem Server nicht eingerichtet", "OCR not set up on the server")
                        doc.autoTextStatus == "failed" -> tr("Texterkennung fehlgeschlagen", "Text recognition failed")
                        else -> tr("Original gespeichert", "Original stored")
                    }, onClick = { files.open("api/cases/${item.id}/documents/${doc.id}/download", doc.name, false) },
                        action = Icons.Outlined.Share to tr("Teilen", "Share"),
                        onAction = { files.open("api/cases/${item.id}/documents/${doc.id}/download", doc.name, true) })
                }, tr("Noch keine Dokumente.", "No documents yet."))
                2 -> ListCard(item.drafts.map { d ->
                    val name = "Schriftsatz-${item.reference}-v${d.version}"
                    ListRow(Icons.Outlined.EditNote, d.recipient.lineSequence().firstOrNull()?.ifBlank { null } ?: tr("Entwurf", "Draft"),
                        tr("Fassung ", "Version ") + d.version + " · " + Dates.label(d.updatedAt ?: d.createdAt, lang) + " · " + reviewLabel(d.reviewStatus),
                        onClick = { files.open("api/cases/${item.id}/drafts/${d.id}/pdf", "$name.pdf", false) },
                        action = Icons.Outlined.Description to tr("Als Word-Datei teilen", "Share as Word file"),
                        onAction = { files.open("api/cases/${item.id}/drafts/${d.id}/docx", "$name.docx", true) })
                }, tr("Noch keine Schriftsätze. Entwürfe erstellen und bearbeiten Sie in der Web-App.", "No drafts yet. Create and edit drafts in the web app."))
                else -> TaskList(state, item.tasks.sortedWith(compareBy({ it.completed }, { it.dueOn ?: "9999" })), cases, lang, onOpenCase = null)
            }
            Spacer(Modifier.size(12.dp))
        }
    }
    if (addTask && item != null) TaskDialog(state, cases, presetCase = item.id, onDismiss = { addTask = false })
}

@Composable
private fun stageLabel(stage: String): String? = when (stage) {
    "out_of_court" -> tr("Außergerichtlich", "Out of court")
    "court" -> tr("Gerichtlich", "In court")
    "appeal" -> tr("Rechtsmittel", "Appeal")
    "enforcement" -> tr("Vollstreckung", "Enforcement")
    else -> null
}

@Composable
private fun reviewLabel(status: String): String = when (status) {
    "approved" -> tr("Freigegeben (eigene Prüfung)", "Approved (own review)")
    "in_review" -> tr("In Prüfung", "In review")
    else -> tr("Entwurf", "Draft")
}

@Composable
private fun QuickAction(icon: ImageVector, label: String, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier.clip(RoundedCornerShape(18.dp)).background(MaterialTheme.colorScheme.surfaceVariant).clickable(onClick = onClick).padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surface), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
        }
        Text(label, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun Overview(item: CaseFile, lang: String) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item.nextDue?.let { (date, title) ->
            val brand = LocalBrand.current
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(brand.warning).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.size(46.dp).clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surface), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Text(Dates.toDate(date)?.dayOfMonth?.toString()?.padStart(2, '0') ?: "", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.tertiary)
                    Text(Dates.month(date, lang), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary)
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(title, style = MaterialTheme.typography.titleSmall, color = brand.onWarning)
                    Text(Dates.label(date, lang) + " · " + Dates.relative(date, lang), style = MaterialTheme.typography.bodyMedium, color = brand.onWarning)
                }
            }
        }
        if (item.description.isNotBlank()) {
            SectionLabel(tr("Sachverhalt", "Facts"))
            Text(item.description, style = MaterialTheme.typography.bodyLarge)
        }
        val parties = listOfNotNull(
            item.details.clientName.ifBlank { null }?.let { it to tr("Mandant/in", "Client") },
            item.details.opponentName.ifBlank { null }?.let { it to (tr("Gegenseite", "Other party") + item.details.opponentAddress.let { a -> if (a.isBlank()) "" else " · $a" }) },
            item.details.court.ifBlank { null }?.let { it to tr("Gericht", "Court") },
        )
        if (parties.isNotEmpty()) {
            SectionLabel(tr("Beteiligte", "Parties"))
            ListCard(parties.map { ListRow(null, it.first, it.second) }, "")
        }
        if (item.details.responsible.isNotBlank()) Text(tr("Bearbeitung: ", "Handled by: ") + item.details.responsible, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** One line of a case list; rows with [onClick] open something, [action] is an extra icon button (share). */
private class ListRow(val icon: ImageVector?, val title: String, val subtitle: String, val onClick: (() -> Unit)? = null,
                      val action: Pair<ImageVector, String>? = null, val onAction: () -> Unit = {})

@Composable
private fun ListCard(rows: List<ListRow>, empty: String) {
    if (rows.isEmpty()) { Text(empty, color = MaterialTheme.colorScheme.onSurfaceVariant); return }
    Surface(shape = RoundedCornerShape(20.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
        Column {
            rows.forEachIndexed { index, row ->
                Row(Modifier.fillMaxWidth().then(row.onClick?.let { Modifier.clickable(onClick = it) } ?: Modifier)
                    .padding(start = 14.dp, end = 4.dp, top = 8.dp, bottom = 8.dp).heightIn(min = 40.dp), verticalAlignment = Alignment.CenterVertically) {
                    row.icon?.let { Icon(it, null, tint = MaterialTheme.colorScheme.primary); Spacer(Modifier.width(12.dp)) }
                    Column(Modifier.weight(1f).padding(vertical = 4.dp)) {
                        Text(row.title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
                        if (row.subtitle.isNotBlank()) Text(row.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    row.action?.let { (icon, label) -> IconButton(onClick = row.onAction) { Icon(icon, label, tint = MaterialTheme.colorScheme.onSurfaceVariant) } }
                }
                if (index < rows.lastIndex) androidx.compose.material3.HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    }
}

@Composable
fun uploadError(detail: String) = errorText(detail)
