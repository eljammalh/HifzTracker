package com.hifz.tracker.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hifz.tracker.UiState
import com.hifz.tracker.logic.Planner

@Composable
fun MemorizeScreen(state: UiState, onOpenSurah: (Int) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableIntStateOf(0) } // 0 all, 1 in progress, 2 not started, 3 memorized

    val list = state.ordered.filter { s ->
        val p = state.progress[s.id]
        val f = Planner.fraction(s, p)
        val matchesFilter = when (filter) {
            1 -> f > 0 && p?.memorized != true
            2 -> f == 0.0
            3 -> p?.memorized == true
            else -> true
        }
        val q = query.trim()
        val matchesQuery = q.isEmpty() || s.nameAr.contains(q) || s.nameEn.contains(q, ignoreCase = true) || s.id.toString() == q
        matchesFilter && matchesQuery
    }
    val currentId = state.todaySurah?.id

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
    ) {
        item {
            ScreenHeader(
                tr("Memorize", "الحفظ"),
                tr(
                    "Daily amount: ${Fmt.amount(state.settings.dailyAmount, false)}",
                    "الوِرد اليومي: ${Fmt.amount(state.settings.dailyAmount, true)}",
                ),
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                placeholder = { Text(tr("Search surah name or number", "ابحث باسم السورة أو رقمها")) },
                shape = RoundedCornerShape(14.dp),
            )
            Row(
                Modifier.padding(vertical = 8.dp).horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                val labels = listOf(
                    tr("All", "الكل"),
                    tr("In progress", "قيد الحفظ"),
                    tr("Not started", "لم تبدأ"),
                    tr("Done", "محفوظة"),
                )
                labels.forEachIndexed { i, l ->
                    FilterChip(selected = filter == i, onClick = { filter = i }, label = { Text(l) })
                }
            }
        }
        items(list, key = { it.id }) { s ->
            val p = state.progress[s.id]
            val f = Planner.fraction(s, p)
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { onOpenSurah(s.id) }
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SurahNumber(s.id, highlighted = s.id == currentId)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    SurahTitle(s)
                    Text(
                        tr(
                            "${Fmt.pages(s.length, false)} · ${s.ayahs} ayahs · Juz ${s.juzStart}",
                            "${Fmt.pages(s.length, true)} · ${s.ayahs} آية · الجزء ${s.juzStart}",
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (f > 0 && p?.memorized != true) {
                        Spacer(Modifier.height(6.dp))
                        ProgressBar(f)
                    }
                }
                Spacer(Modifier.width(10.dp))
                if (p?.memorized == true) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                } else if (f > 0) {
                    Text(Fmt.pct(f), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
        if (list.isEmpty()) {
            item {
                Text(
                    tr("No surahs match.", "لا توجد سور مطابقة."),
                    Modifier.padding(24.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
