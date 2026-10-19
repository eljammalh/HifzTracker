package com.hifz.tracker.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.hifz.tracker.HifzViewModel
import com.hifz.tracker.UiState

private val DAILY_OPTIONS = listOf(0.5, 1.0, 1.5, 2.0, 2.5, 3.0, 3.5, 4.0, 4.5, 5.0)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(state: UiState, vm: HifzViewModel) {
    val ar = LocalArabic.current
    val st = state.settings
    val context = LocalContext.current
    var pendingType by remember { mutableStateOf<String?>(null) }
    var timeFor by remember { mutableStateOf<String?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        val t = pendingType
        if (granted && t != null) vm.setReminder(t, true)
        pendingType = null
    }

    fun enableReminder(type: String) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            pendingType = type
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            vm.setReminder(type, true)
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        ScreenHeader(tr("Settings", "الإعدادات"))

        SectionCard(tr("DAILY MEMORIZATION", "الوِرد اليومي للحفظ")) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DAILY_OPTIONS.forEach { v ->
                    FilterChip(
                        selected = kotlin.math.abs(st.dailyAmount - v) < 0.01,
                        onClick = { vm.setDailyAmount(v) },
                        label = { Text(Fmt.amount(v, ar)) },
                    )
                }
            }
            Text(
                tr(
                    "Changing this re-divides only the part of each surah you haven't memorized yet.",
                    "تغيير هذا يعيد تقسيم الجزء غير المحفوظ فقط من كل سورة.",
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        SectionCard(tr("DAILY REVIEW GOAL", "الهدف اليومي للمراجعة")) {
            GoalPicker(st.reviewGoal) { vm.setReviewGoal(it) }
        }

        SectionCard(tr("MEMORIZATION ORDER", "ترتيب الحفظ")) {
            ChoiceRow(tr("From An-Nas backward (Juz 'Amma first)", "من الناس صعودًا (جزء عمّ أولًا)"), st.orderBackward) { vm.setOrderBackward(true) }
            ChoiceRow(tr("From Al-Fatihah forward", "من الفاتحة نزولًا"), !st.orderBackward) { vm.setOrderBackward(false) }
            Text(
                tr("You can always pick a different surah from its page with “Make this my current surah”.",
                    "يمكنك دائمًا اختيار سورة أخرى من صفحتها عبر «اجعلها سورتي الحالية»."),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        SectionCard(tr("LANGUAGE", "اللغة")) {
            ChoiceRow("English", !st.arabic) { vm.setArabic(false) }
            ChoiceRow("العربية", st.arabic) { vm.setArabic(true) }
        }

        SectionCard(tr("REMINDERS", "التذكيرات")) {
            ReminderRow(
                label = tr("Memorization reminder", "تذكير الحفظ"),
                enabled = st.memReminder,
                time = Fmt.time(st.memHour, st.memMinute),
                onToggle = { on -> if (on) enableReminder("mem") else vm.setReminder("mem", false) },
                onTime = { timeFor = "mem" },
            )
            ReminderRow(
                label = tr("Review reminder", "تذكير المراجعة"),
                enabled = st.revReminder,
                time = Fmt.time(st.revHour, st.revMinute),
                onToggle = { on -> if (on) enableReminder("rev") else vm.setReminder("rev", false) },
                onTime = { timeFor = "rev" },
            )
            Text(
                tr("Android may deliver reminders a little after the set time to save battery.",
                    "قد يُرسل أندرويد التذكير بعد الموعد بقليل لتوفير البطارية."),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        SectionCard(tr("ABOUT THE DATA", "عن البيانات")) {
            Text(
                tr(
                    "Pages follow the standard 604-page Madani Mushaf (Hafs). Where several short surahs share a page, the page is divided between them, so their lengths are approximate.",
                    "الصفحات وفق المصحف المدني القياسي ذي 604 صفحات (رواية حفص). حين تشترك عدة سور قصيرة في صفحة واحدة تُقسَم الصفحة بينها، فأطوالها تقريبية.",
                ),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }

    timeFor?.let { type ->
        val h = if (type == "mem") st.memHour else st.revHour
        val m = if (type == "mem") st.memMinute else st.revMinute
        TimeDialog(h, m, onDismiss = { timeFor = null }) { nh, nm ->
            vm.setReminderTime(type, nh, nm)
            timeFor = null
        }
    }
}

@Composable
private fun ChoiceRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Spacer(Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun ReminderRow(label: String, enabled: Boolean, time: String, onToggle: (Boolean) -> Unit, onTime: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            TextButton(onClick = onTime, enabled = enabled) {
                Text(time, fontWeight = FontWeight.SemiBold)
            }
        }
        Switch(checked = enabled, onCheckedChange = onToggle)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeDialog(hour: Int, minute: Int, onDismiss: () -> Unit, onConfirm: (Int, Int) -> Unit) {
    val pickerState = rememberTimePickerState(initialHour = hour, initialMinute = minute, is24Hour = false)
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = { onConfirm(pickerState.hour, pickerState.minute) }) { Text(tr("OK", "حسنًا")) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(tr("Cancel", "إلغاء")) } },
        text = { TimePicker(state = pickerState) },
    )
}
