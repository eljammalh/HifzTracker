package com.hifz.tracker.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hifz.tracker.HifzViewModel
import com.hifz.tracker.UiState
import com.hifz.tracker.data.Quran
import com.hifz.tracker.data.ReviewGoal
import com.hifz.tracker.logic.Planner

@Composable
fun ReviewScreen(state: UiState, vm: HifzViewModel) {
    val ar = LocalArabic.current
    var choosing by remember { mutableStateOf(false) }
    val goal = state.settings.reviewGoal

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            ScreenHeader(tr("Review", "المراجعة"))
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(tr("Daily goal:", "الهدف اليومي:"), style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.width(8.dp))
                GoalPicker(goal) { vm.setReviewGoal(it) }
            }
        }

        if (state.memorized.isEmpty()) {
            item {
                SectionCard {
                    Text(
                        tr(
                            "Review draws only from surahs you have fully memorized. Finish a surah, or mark the ones you already know from the Memorize tab, and your daily review will appear here.",
                            "تكون المراجعة من السور التي أتممت حفظها فقط. أتمّ سورة، أو حدّد السور التي تحفظها مسبقًا من تبويب الحفظ، وستظهر مراجعتك اليومية هنا.",
                        ),
                    )
                }
            }
            return@LazyColumn
        }

        if (state.goalCapped) {
            item {
                SectionCard(container = MaterialTheme.colorScheme.tertiaryContainer) {
                    Text(
                        tr(
                            "Your goal (${Fmt.goal(goal, false)}) is more than everything memorized so far (${Fmt.pages(state.memorizedPagesTotal, false)}), so today's review covers all of it.",
                            "هدفك (${Fmt.goal(goal, true)}) أكبر من كل ما حفظته حتى الآن (${Fmt.pages(state.memorizedPagesTotal, true)})، لذا تشمل مراجعة اليوم كل محفوظك.",
                        ),
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }

        item {
            val total = state.plan.sumOf { it.pages }
            val done = state.plan.filter { it.doneToday }.sumOf { it.pages }
            SectionCard(tr("TODAY", "اليوم"), container = MaterialTheme.colorScheme.secondaryContainer) {
                Text(
                    tr("${Fmt.num(done)} of ${Fmt.pages(total, false)} reviewed", "تمت مراجعة ${Fmt.num(done)} من ${Fmt.pages(total, true)}"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                ProgressBar(if (total > 0) done / total else 0.0, color = MaterialTheme.colorScheme.secondary)
                Text(
                    tr("Suggested from what has gone longest without review. Tap an item when you've reviewed it.",
                        "مقترحة بحسب الأطول مدّةً دون مراجعة. اضغط على المقطع بعد مراجعته."),
                    style = MaterialTheme.typography.bodySmall,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { choosing = true }) { Text(tr("Choose manually", "اختيار يدوي")) }
                    TextButton(onClick = { vm.newSuggestion() }) { Text(tr("Use suggestion", "الاقتراح التلقائي")) }
                }
            }
        }

        items(state.plan, key = { "plan-" + it.key }) { u ->
            UnitRow(u, state.today, checked = u.doneToday, onToggle = { vm.toggleReviewed(u) })
        }

        item {
            Spacer(Modifier.height(10.dp))
            Text(tr("All memorized surahs", "جميع السور المحفوظة"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                tr("Highlighted: not reviewed in ${Planner.OVERDUE_DAYS}+ days.", "المُبرز: لم يُراجَع منذ ${Planner.OVERDUE_DAYS} يومًا أو أكثر."),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        items(state.memorized, key = { "all-" + it.id }) { s ->
            val last = (0 until Planner.chunkCount(s))
                .map { state.chunks[Planner.chunkKey(s.id, it)]?.lastReviewed ?: -1L }
                .minOrNull() ?: -1L
            val overdue = last < 0 || state.today - last >= Planner.OVERDUE_DAYS
            Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("${s.id}. " + if (ar) s.nameAr else s.nameEn, Modifier.weight(1f))
                Text(
                    Fmt.ago(last, state.today, ar),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (overdue) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (overdue) FontWeight.SemiBold else FontWeight.Normal,
                )
            }
        }

        item {
            Spacer(Modifier.height(10.dp))
            Text(tr("Recent review history", "سجلّ المراجعة"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            val byDay = state.logs.filter { it.type == "REV" }.groupBy { it.day }.toList().sortedByDescending { it.first }.take(14)
            if (byDay.isEmpty()) {
                Text(tr("No reviews logged yet.", "لا توجد مراجعات مسجّلة بعد."), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            byDay.forEach { (day, entries) ->
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Text(Fmt.date(day, ar), Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                    Text(Fmt.pages(entries.sumOf { it.pages }, ar), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    }

    if (choosing) {
        ManualPlanDialog(state, onDismiss = { choosing = false }, onSave = { vm.setPlan(it); choosing = false })
    }
}

@Composable
private fun unitTitle(u: Planner.ReviewUnit): String {
    val s = Quran.byId(u.surahId)
    return if (LocalArabic.current) s.nameAr else s.nameEn
}

@Composable
private fun UnitRow(u: Planner.ReviewUnit, today: Long, checked: Boolean, onToggle: () -> Unit) {
    val ar = LocalArabic.current
    val s = Quran.byId(u.surahId)
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth().clickable { onToggle() },
    ) {
        Row(Modifier.padding(horizontal = 8.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = checked, onCheckedChange = { onToggle() })
            Column(Modifier.weight(1f)) {
                Text(
                    unitTitle(u) + if (u.wholeSurah) "" else (if (ar) " (جزء منها)" else " (part)"),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    Fmt.surahRange(s, u.from, u.to, ar) + " · " + Fmt.pages(u.pages, ar),
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    if (checked) tr("Reviewed today ✓", "رُوجعت اليوم ✓") else Fmt.ago(u.lastReviewed, today, ar),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ManualPlanDialog(state: UiState, onDismiss: () -> Unit, onSave: (List<String>) -> Unit) {
    val ar = LocalArabic.current
    val selected = remember { mutableStateListOf<String>().apply { addAll(state.plan.map { it.key }) } }
    // Show the standard units plus any custom units already in today's plan.
    val options = (state.plan + state.allUnits).distinctBy { it.key }
    val pages = options.filter { it.key in selected }.sumOf { it.pages }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(tr("Choose what to review", "اختر ما تراجعه")) },
        text = {
            Column {
                Text(
                    tr("Selected: ${Fmt.pages(pages, false)}", "المحدد: ${Fmt.pages(pages, true)}"),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(8.dp))
                Box(Modifier.heightIn(max = 420.dp)) {
                    LazyColumn {
                        items(options, key = { it.key }) { u ->
                            val s = Quran.byId(u.surahId)
                            val isOn = u.key in selected
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clickable { if (isOn) selected.remove(u.key) else selected.add(u.key) }
                                    .padding(vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Checkbox(checked = isOn, onCheckedChange = { if (isOn) selected.remove(u.key) else selected.add(u.key) })
                                Column(Modifier.weight(1f)) {
                                    Text(if (ar) s.nameAr else s.nameEn, style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        Fmt.surahRange(s, u.from, u.to, ar) + " · " + Fmt.ago(u.lastReviewed, state.today, ar),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(selected.toList()) }, enabled = selected.isNotEmpty()) {
                Text(tr("Save", "حفظ"))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(tr("Cancel", "إلغاء")) } },
    )
}

@Composable
fun GoalPicker(goal: ReviewGoal, onPick: (ReviewGoal) -> Unit) {
    val ar = LocalArabic.current
    var open by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { open = true }) { Text(Fmt.goal(goal, ar)) }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }, modifier = Modifier.heightIn(max = 400.dp)) {
            ReviewGoal.ALL.forEach { g ->
                DropdownMenuItem(
                    text = { Text(Fmt.goal(g, ar), fontWeight = if (g == goal) FontWeight.SemiBold else FontWeight.Normal) },
                    onClick = { onPick(g); open = false },
                )
            }
        }
    }
}
