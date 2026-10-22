package de.schimmelpilz.app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import de.schimmelpilz.app.R
import de.schimmelpilz.app.ui.theme.Jakarta

const val BRAND = "Rechtsassistentin Schimmelpilz"

@Composable
fun brandClaim() = tr("Entwickelt von einem ausgebildeten Rechtsanwaltsfachangestellten", "Developed by a trained German paralegal")

@Composable
fun BrandMark(size: Dp, modifier: Modifier = Modifier) =
    Image(painterResource(R.drawable.logo_mark), contentDescription = null, modifier = modifier.size(size))

/** CI lockup: badge, "Rechtsassistentin", "Schimmelpilz" and (unless compact) the claim; [scale] 1 matches the welcome header. */
@Composable
fun BrandLockup(modifier: Modifier = Modifier, compact: Boolean = false, scale: Float = 1f) {
    val claim = brandClaim()
    Row(
        modifier.clearAndSetSemantics { contentDescription = if (compact) BRAND else "$BRAND – $claim" },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy((10 * scale).dp),
    ) {
        BrandMark(((if (compact) 44 else 64) * scale).dp)
        Column {
            Text("Rechtsassistentin", style = TextStyle(fontFamily = Jakarta, fontWeight = FontWeight.Bold, fontSize = (17 * scale).sp, letterSpacing = (-0.01).em, lineHeight = (20 * scale).sp), color = MaterialTheme.colorScheme.onSurface)
            Text("Schimmelpilz", style = TextStyle(fontFamily = Jakarta, fontWeight = FontWeight.ExtraBold, fontSize = (24 * scale).sp, letterSpacing = (-0.02).em, lineHeight = (27 * scale).sp), color = MaterialTheme.colorScheme.primary)
            if (!compact) Text(claim, style = TextStyle(fontFamily = Jakarta, fontWeight = FontWeight.Medium, fontSize = (10.5 * scale).sp, lineHeight = (13 * scale).sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
