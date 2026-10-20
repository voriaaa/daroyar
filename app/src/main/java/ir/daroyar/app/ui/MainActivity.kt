package ir.daroyar.app.ui

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Medication
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.daroyar.app.data.MedStore
import ir.daroyar.app.data.Kind
import ir.daroyar.app.data.Medication
import ir.daroyar.app.drugs.InteractionChecker
import ir.daroyar.app.reminder.Notifier
import ir.daroyar.app.reminder.ReminderScheduler
import kotlinx.coroutines.delay
import java.time.LocalDateTime

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DaroYarTheme { AppRoot() }
        }
    }
}

private const val NO_EDITOR = -1L
private const val NEW_MEDICATION = 0L

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppRoot() {
    val context = LocalContext.current
    val store = remember { MedStore.get(context) }
    val meds by store.meds.collectAsStateWithLifecycle()
    val taken by store.taken.collectAsStateWithLifecycle()

    var tab by rememberSaveable { mutableIntStateOf(0) }
    var editingId by rememberSaveable { mutableLongStateOf(NO_EDITOR) }
    var viewingId by rememberSaveable { mutableLongStateOf(NO_EDITOR) }
    // Which kind a brand-new entry starts as (the editor can still switch it).
    var newKind by rememberSaveable { mutableStateOf(Kind.MEDICINE) }
    val startNew = { k: Kind ->
        newKind = k
        editingId = NEW_MEDICATION
    }
    val findings = remember(meds) { InteractionChecker.check(meds.filter { it.active }) }

    // Ticks so "late" badges and the date stay current while the app is open.
    val now by produceState(LocalDateTime.now()) {
        while (true) {
            delay(15_000)
            value = LocalDateTime.now()
        }
    }

    var canNotify by remember { mutableStateOf(Notifier.canNotify(context)) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        canNotify = Notifier.canNotify(context)
    }
    var askedPermission by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !canNotify && !askedPermission) {
            askedPermission = true
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
    // The user may switch notifications on in system settings and come back.
    LifecycleResumeEffect(Unit) {
        canNotify = Notifier.canNotify(context)
        onPauseOrDispose { }
    }

    if (editingId != NO_EDITOR) {
        val initial = remember(editingId) { store.medication(editingId) ?: Medication(kind = newKind) }
        val close = { editingId = NO_EDITOR }
        BackHandler(onBack = close)
        EditMedScreen(
            initial = initial,
            otherMeds = meds,
            onSave = { med ->
                store.save(med)
                ReminderScheduler.sync(context)
                close()
            },
            onDelete = if (initial.id == NEW_MEDICATION) null else {
                {
                    initial.times.forEach {
                        Notifier.cancel(context, initial.id, it)
                        ReminderScheduler.cancelSnooze(context, initial.id, it)
                    }
                    store.delete(initial.id)
                    ReminderScheduler.sync(context)
                    viewingId = NO_EDITOR
                    close()
                }
            },
            onBack = close,
        )
    } else if (meds.any { it.id == viewingId }) {
        val med = meds.first { it.id == viewingId }
        BackHandler { viewingId = NO_EDITOR }
        MedInfoScreen(
            med = med,
            allMeds = meds,
            onBack = { viewingId = NO_EDITOR },
            onEdit = { editingId = med.id },
            onApplyTimes = { times ->
                store.save(med.copy(times = times))
                ReminderScheduler.sync(context)
            },
        )
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("دارویار", fontWeight = FontWeight.Bold) },
                    actions = {
                        IconButton(onClick = {
                            if (canNotify) {
                                Notifier.showTest(context)
                            } else {
                                Toast.makeText(context, "اول اعلان‌های برنامه را روشن کنید", Toast.LENGTH_SHORT).show()
                            }
                        }) {
                            Icon(Icons.Outlined.NotificationsActive, contentDescription = "ارسال اعلان آزمایشی")
                        }
                    },
                )
            },
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(
                        selected = tab == 0,
                        onClick = { tab = 0 },
                        icon = { Icon(Icons.Outlined.Today, contentDescription = null) },
                        label = { Text("امروز") },
                    )
                    NavigationBarItem(
                        selected = tab == 1,
                        onClick = { tab = 1 },
                        icon = { Icon(Icons.Outlined.Medication, contentDescription = null) },
                        label = { Text("داروها و مکمل‌ها") },
                    )
                }
            },
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    onClick = { startNew(Kind.MEDICINE) },
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text("افزودن") },
                )
            },
        ) { padding ->
            Column(
                Modifier
                    .padding(padding)
                    .fillMaxSize()
            ) {
                if (!canNotify) {
                    NotificationsOffBanner(onEnable = {
                        context.startActivity(
                            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                        )
                    })
                }
                when (tab) {
                    0 -> TodayScreen(
                        meds = meds,
                        taken = taken,
                        now = now,
                        alertCount = findings.count { it.needsAttention },
                        onToggle = { med, minute, value ->
                            store.setTaken(med.id, now.toLocalDate(), minute, value)
                            if (value) {
                                Notifier.cancel(context, med.id, minute)
                                ReminderScheduler.cancelSnooze(context, med.id, minute)
                            }
                        },
                        onAdd = { startNew(Kind.MEDICINE) },
                        onShowAlerts = { tab = 1 },
                    )
                    else -> MedsScreen(
                        meds = meds,
                        findings = findings,
                        onOpen = { viewingId = it.id },
                        onToggleActive = { med, active ->
                            store.save(med.copy(active = active))
                            ReminderScheduler.sync(context)
                        },
                        onAdd = startNew,
                    )
                }
            }
        }
    }
}

@Composable
private fun NotificationsOffBanner(onEnable: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
        ),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.NotificationsOff, contentDescription = null)
            Spacer(Modifier.width(12.dp))
            Text(
                "اعلان‌ها خاموش است؛ بدون آن یادآوری نمی‌رسد.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
            FilledTonalButton(onClick = onEnable) { Text("روشن کردن") }
        }
    }
}
