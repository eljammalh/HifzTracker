package com.hifz.tracker.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hifz.tracker.UiState
import com.hifz.tracker.data.Quran
import com.hifz.tracker.logic.Planner

@Composable
fun MemorizedScreen(state: UiState, onOpenSurah: (Int) -> Unit) {
    val ar = LocalArabic.current
    val st = state.stats
    val surahFrac = st.surahsMemorized.toDouble() / Quran.TOTAL_SURAHS
    val pageFrac = st.pagesMemorized / Quran.TOTAL_PAGES

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
        item {
            ScreenHeader(tr("Memorized", "المحفوظ"))
            Spacer(Modifier.height(10.dp))
            SectionCard(container = MaterialTheme.colorScheme.primaryContainer) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        Fmt.pct(surahFrac),
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        tr("${st.surahsMemorized} of 114 surahs", "${st.surahsMemorized} من 114 سورة"),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 6.dp),
                    )
                }
                ProgressBar(surahFrac)
                StatLine(
                    tr("By pages", "بالصفحات"),
                    "${Fmt.num(st.pagesMemorized)} / 604 · ${Fmt.pct(pageFrac)}",
                )
                ProgressBar(pageFrac)
                StatLine(
                    tr("Equivalent juz", "ما يعادل من الأجزاء"),
                    Fmt.num(st.pagesMemorized / Quran.PAGES_PER_JUZ),
                )
                Text(
                    tr(
                        "Surahs differ greatly in length, so the page percentage is the truer measure of how much of the Quran you hold.",
                        "تتفاوت السور كثيرًا في طولها، لذا فنسبة الصفحات أدقّ في قياس ما تحفظه من القرآن.",
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(16.dp))
            Text(
                tr("Memorized surahs", "السور المحفوظة"),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
        if (state.memorized.isEmpty()) {
            item {
                Text(
                    tr(
                        "Nothing here yet. Finish your first surah, or mark surahs you already know from the Memorize tab.",
                        "لا شيء بعد. أتمّ أول سورة، أو حدّد السور التي تحفظها مسبقًا من تبويب الحفظ.",
                    ),
                    Modifier.padding(vertical = 16.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        items(state.memorized, key = { it.id }) { s ->
            val p = state.progress[s.id]
            val lastRev = (0 until Planner.chunkCount(s))
                .map { state.chunks[Planner.chunkKey(s.id, it)]?.lastReviewed ?: -1L }
                .minOrNull() ?: -1L
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { onOpenSurah(s.id) }
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SurahNumber(s.id)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    SurahTitle(s)
                    val when_ = when {
                        p?.imported == true -> tr("Memorized before the app", "محفوظة قبل التطبيق")
                        p?.memorizedDay != null -> tr("Completed ${Fmt.date(p.memorizedDay, false)}", "اكتملت ${Fmt.date(p.memorizedDay, true)}")
                        else -> ""
                    }
                    Text(
                        "$when_ · ${Fmt.pages(s.length, ar)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    Fmt.ago(lastRev, state.today, ar),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (lastRev < 0 || state.today - lastRev >= Planner.OVERDUE_DAYS) MaterialTheme.colorScheme.tertiary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}
