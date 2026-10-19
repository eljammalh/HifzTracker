package com.hifz.tracker.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hifz.tracker.HifzViewModel
import com.hifz.tracker.UiState
import com.hifz.tracker.data.Quran
import com.hifz.tracker.logic.Planner

@Composable
fun SurahDetailScreen(surahId: Int, state: UiState, vm: HifzViewModel, onBack: () -> Unit) {
    val ar = LocalArabic.current
    val s = Quran.byId(surahId)
    val p = state.progress[surahId]
    val daily = state.settings.dailyAmount
    val segments = Planner.segments(s, p, daily)
    val frac = Planner.fraction(s, p)
    val memorized = p?.memorized == true
    val isCurrent = state.todaySurah?.id == surahId
    val nextIndex = segments.firstOrNull { !it.done }?.index
    var confirm by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = tr("Back", "رجوع"))
                }
                Spacer(Modifier.width(4.dp))
                Text(tr("Surah ${s.id}", "سورة رقم ${s.id}"), style = MaterialTheme.typography.titleMedium)
            }
        }
        item {
            SectionCard(container = MaterialTheme.colorScheme.primaryContainer) {
                SurahTitle(s, large = true)
                StatLine(tr("Ayahs", "عدد الآيات"), "${s.ayahs}")
                StatLine(tr("Length", "الطول"), Fmt.pages(s.length, ar))
                StatLine(tr("Location", "الموضع"), Fmt.surahRange(s, 0.0, s.length, ar))
                StatLine(
                    tr("Juz", "الجزء"),
                    if (s.juzStart == s.juzEnd) "${s.juzStart}" else "${s.juzStart}–${s.juzEnd}",
                )
                ProgressBar(frac)
                Text(
                    if (memorized) tr("Memorized ✓", "محفوظة ✓")
                    else tr("${Fmt.pct(frac)} memorized", "تم حفظ ${Fmt.pct(frac)}"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                if (!memorized) {
                    if (isCurrent) {
                        AssistChip(onClick = {}, label = { Text(tr("This is your current surah", "هذه سورتك الحالية")) })
                    } else {
                        OutlinedButton(onClick = { vm.setCurrentSurah(surahId) }) {
                            Text(tr("Make this my current surah", "اجعلها سورتي الحالية"))
                        }
                    }
                }
            }
        }
        item {
            Text(
                tr(
                    "Segments (${Fmt.amount(daily, false)} each)",
                    "المقاطع (${Fmt.amount(daily, true)} لكل مقطع)",
                ),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                tr("“3½” means the middle of page 3.", "«3½» تعني منتصف الصفحة 3."),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        items(segments, key = { it.index }) { seg ->
            val isNext = seg.index == nextIndex
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = when {
                    isNext -> MaterialTheme.colorScheme.secondaryContainer
                    else -> MaterialTheme.colorScheme.surfaceContainer
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(
                                if (seg.done) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outlineVariant,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (seg.done) {
                            Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(18.dp))
                        } else {
                            Text("${seg.index + 1}", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            Fmt.surahRange(s, seg.from, seg.to, ar),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (isNext) FontWeight.SemiBold else FontWeight.Normal,
                        )
                        Text(
                            tr("${Fmt.pct(seg.share)} of the surah", "${Fmt.pct(seg.share)} من السورة"),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (isNext) {
                        Button(onClick = { vm.markNextSegment(surahId) }) { Text(tr("Done", "تم")) }
                    }
                }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (frac > 0) {
                    TextButton(onClick = { vm.undoLastSegment(surahId) }) {
                        Text(tr("Undo last segment", "التراجع عن آخر مقطع"))
                    }
                }
                if (!memorized) {
                    TextButton(onClick = { confirm = "import" }) {
                        Text(tr("I already know this surah — mark all as memorized", "أحفظ هذه السورة مسبقًا — اعتبرها محفوظة"))
                    }
                }
                if (frac > 0) {
                    TextButton(onClick = { confirm = "reset" }) {
                        Text(tr("Reset this surah", "إعادة تعيين السورة"), color = MaterialTheme.colorScheme.tertiary)
                    }
                }
            }
        }
    }

    when (confirm) {
        "import" -> ConfirmDialog(
            title = tr("Mark as memorized?", "اعتبارها محفوظة؟"),
            text = tr(
                "Use this for surahs you memorized before using the app. It counts toward your progress but not toward your daily pace.",
                "استخدم هذا للسور التي حفظتها قبل استخدام التطبيق. تُحتسب في تقدّمك لكن لا تُحتسب في وتيرتك اليومية.",
            ),
            confirm = tr("Mark memorized", "نعم، محفوظة"),
            onConfirm = { vm.markAlreadyMemorized(surahId) },
            onDismiss = { confirm = null },
        )
        "reset" -> ConfirmDialog(
            title = tr("Reset this surah?", "إعادة تعيين السورة؟"),
            text = tr("All progress for this surah will be cleared.", "سيُمسح كل التقدّم المسجّل لهذه السورة."),
            confirm = tr("Reset", "إعادة تعيين"),
            onConfirm = { vm.resetSurah(surahId) },
            onDismiss = { confirm = null },
        )
    }
}
