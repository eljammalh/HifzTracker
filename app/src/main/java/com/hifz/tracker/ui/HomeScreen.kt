package com.hifz.tracker.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hifz.tracker.HifzViewModel
import com.hifz.tracker.UiState
import com.hifz.tracker.data.Quran

@Composable
fun HomeScreen(state: UiState, vm: HifzViewModel, onOpenSurah: (Int) -> Unit, onOpenReview: () -> Unit) {
    val ar = LocalArabic.current
    val st = state.stats
    val daily = state.settings.dailyAmount

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            ScreenHeader(
                tr("Assalamu alaikum", "السلام عليكم"),
                Fmt.date(state.today, ar),
            )
        }

        // Today's memorization
        item {
            SectionCard(
                tr("TODAY'S MEMORIZATION", "حفظ اليوم"),
                container = MaterialTheme.colorScheme.primaryContainer,
            ) {
                val s = state.todaySurah
                val seg = state.todaySegment
                if (s == null || seg == null) {
                    Text(
                        tr("You have memorized the entire Quran. May Allah accept it from you.", "أتممت حفظ القرآن كاملًا، تقبّل الله منك."),
                        style = MaterialTheme.typography.titleMedium,
                    )
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SurahNumber(s.id, highlighted = true)
                        Spacer(Modifier.width(12.dp))
                        SurahTitle(s)
                    }
                    Text(
                        Fmt.surahRange(s, seg.from, seg.to, ar),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        tr(
                            "Segment ${seg.index + 1} · ${Fmt.pct(seg.share)} of the surah",
                            "المقطع ${seg.index + 1} · ${Fmt.pct(seg.share)} من السورة",
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    val frac = com.hifz.tracker.logic.Planner.fraction(s, state.progress[s.id])
                    ProgressBar(frac)
                    Text(
                        tr("Surah progress: ${Fmt.pct(frac)}", "التقدّم في السورة: ${Fmt.pct(frac)}"),
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { vm.markNextSegment(s.id) }) {
                            Text(tr("Mark as memorized", "تم الحفظ"))
                        }
                        OutlinedButton(onClick = { onOpenSurah(s.id) }) {
                            Text(tr("Open surah", "فتح السورة"))
                        }
                    }
                    val todayDone = st.todayPages
                    Text(
                        if (todayDone >= daily - 0.05)
                            tr("Daily goal reached (${Fmt.pages(todayDone, false)} today). Anything more is a bonus.",
                                "أتممت وِرد اليوم (${Fmt.pages(todayDone, true)}). ما زاد فهو خير.")
                        else
                            tr("Today: ${Fmt.num(maxOf(0.0, todayDone))} of ${Fmt.amount(daily, false)}",
                                "اليوم: ${Fmt.num(maxOf(0.0, todayDone))} من ${Fmt.amount(daily, true)}"),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
        }

        // Today's review
        item {
            SectionCard(tr("TODAY'S REVIEW", "مراجعة اليوم"), container = MaterialTheme.colorScheme.secondaryContainer) {
                if (state.memorized.isEmpty()) {
                    Text(
                        tr("Review starts once you have memorized your first surah.",
                            "تبدأ المراجعة بعد أن تُتمّ حفظ أول سورة."),
                    )
                } else {
                    val total = state.plan.sumOf { it.pages }
                    val done = state.plan.count { it.doneToday }
                    Text(
                        tr("${state.plan.size} item(s) · ${Fmt.pages(total, false)}", "${state.plan.size} مقاطع · ${Fmt.pages(total, true)}"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    val names = state.plan.joinToString("، ".takeIf { ar } ?: ", ") {
                        val s = Quran.byId(it.surahId)
                        if (ar) s.nameAr else s.nameEn
                    }
                    Text(names, style = MaterialTheme.typography.bodyMedium, maxLines = 2)
                    ProgressBar(
                        if (state.plan.isEmpty()) 0.0 else done.toDouble() / state.plan.size,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                    Text(tr("$done of ${state.plan.size} done", "أُنجز $done من ${state.plan.size}"), style = MaterialTheme.typography.bodySmall)
                    FilledTonalButton(onClick = onOpenReview) { Text(tr("Go to review", "إلى المراجعة")) }
                }
            }
        }

        // Overall progress
        item {
            SectionCard(tr("OVERALL PROGRESS", "التقدّم العام")) {
                val surahFrac = st.surahsMemorized.toDouble() / Quran.TOTAL_SURAHS
                val pageFrac = st.pagesMemorized / Quran.TOTAL_PAGES
                StatLine(tr("Surahs memorized", "السور المحفوظة"), "${st.surahsMemorized} / 114 · ${Fmt.pct(surahFrac)}")
                ProgressBar(surahFrac)
                StatLine(tr("Pages memorized", "الصفحات المحفوظة"), "${Fmt.num(st.pagesMemorized)} / 604 · ${Fmt.pct(pageFrac)}")
                ProgressBar(pageFrac)
            }
        }

        // Completion estimate
        item {
            SectionCard(tr("COMPLETION ESTIMATE", "موعد الختم المتوقّع")) {
                if (st.remainingPages < 0.01) {
                    Text(tr("Complete. Alhamdulillah!", "اكتمل الحفظ، الحمد لله!"))
                } else {
                    Text(
                        tr("At ${Fmt.amount(daily, false)} a day", "بمعدّل ${Fmt.amount(daily, true)} يوميًا"),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        Fmt.date(st.plannedEndDay, ar),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        tr("${Fmt.days(st.plannedDays, false)} (${Fmt.span(st.plannedDays, false)}) · ${Fmt.num(st.remainingPages)} pages left",
                            "${Fmt.days(st.plannedDays, true)} (${Fmt.span(st.plannedDays, true)}) · متبقٍّ ${Fmt.num(st.remainingPages)} صفحة"),
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Spacer(Modifier.fillMaxWidth())
                    val pace = st.pacePerDay
                    val paceEnd = st.paceEndDay
                    if (pace == null || paceEnd == null) {
                        Text(
                            tr("Mark a few days of progress to see an estimate based on your real pace.",
                                "سجّل تقدّمك بضعة أيام لترى تقديرًا مبنيًا على وتيرتك الفعلية."),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        val diff = (paceEnd - st.plannedEndDay).toInt()
                        StatLine(
                            tr("Your recent pace", "وتيرتك مؤخرًا"),
                            tr("${Fmt.num(pace)} pages/day", "${Fmt.num(pace)} صفحة/يوم"),
                        )
                        StatLine(tr("At this pace", "بهذه الوتيرة"), Fmt.date(paceEnd, ar))
                        Text(
                            when {
                                diff < -2 -> tr("You are ahead of plan by ${Fmt.days(-diff, false)}.", "أنت متقدّم على الخطة بـ ${Fmt.days(-diff, true)}.")
                                diff > 2 -> tr("You are behind plan by ${Fmt.days(diff, false)}.", "أنت متأخر عن الخطة بـ ${Fmt.days(diff, true)}.")
                                else -> tr("You are on track.", "أنت على المسار الصحيح.")
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = if (diff > 2) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }
    }
}
