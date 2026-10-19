package io.phoneagent.mobile.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.FormatListBulleted
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.phoneagent.mobile.data.AgentTask
import io.phoneagent.mobile.data.TaskStore
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class Destination {
    HOME, TASKS, SAFETY
}

@Composable
fun PhoneAgentApp(taskStore: TaskStore) {
    var tasks by remember { mutableStateOf(taskStore.loadDrafts()) }
    var destinationName by rememberSaveable { mutableStateOf(Destination.HOME.name) }
    val destination = Destination.values().firstOrNull { it.name == destinationName } ?: Destination.HOME
    val snackbarState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            containerColor = AgentPalette.Background,
            snackbarHost = { SnackbarHost(hostState = snackbarState) },
            bottomBar = {
                NavigationBar(
                    containerColor = AgentPalette.Surface,
                    contentColor = AgentPalette.TextPrimary,
                    tonalElevation = 0.dp
                ) {
                    NavigationBarItem(
                        selected = destination == Destination.HOME,
                        onClick = { destinationName = Destination.HOME.name },
                        icon = { Icon(Icons.Outlined.Home, contentDescription = null) },
                        label = { Text("الرئيسية") },
                        colors = navColors()
                    )
                    NavigationBarItem(
                        selected = destination == Destination.TASKS,
                        onClick = { destinationName = Destination.TASKS.name },
                        icon = { Icon(Icons.Outlined.FormatListBulleted, contentDescription = null) },
                        label = { Text("المهام") },
                        colors = navColors()
                    )
                    NavigationBarItem(
                        selected = destination == Destination.SAFETY,
                        onClick = { destinationName = Destination.SAFETY.name },
                        icon = { Icon(Icons.Outlined.Shield, contentDescription = null) },
                        label = { Text("الأمان") },
                        colors = navColors()
                    )
                }
            }
        ) { contentPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding)
                    .background(AgentPalette.Background)
            ) {
                when (destination) {
                    Destination.HOME -> HomeScreen(
                        tasks = tasks,
                        onOpenTasks = { destinationName = Destination.TASKS.name },
                        onCreateDraft = { title ->
                            val added = taskStore.addDraft(title)
                            if (added != null) {
                                tasks = taskStore.loadDrafts()
                                destinationName = Destination.TASKS.name
                                scope.launch {
                                    snackbarState.showSnackbar("حُفظت المهمة محليًا كمسودة فقط")
                                }
                            }
                            added != null
                        }
                    )
                    Destination.TASKS -> TasksScreen(
                        tasks = tasks,
                        onRefresh = { tasks = taskStore.loadDrafts() },
                        onNewTask = { destinationName = Destination.HOME.name }
                    )
                    Destination.SAFETY -> SafetyScreen()
                }
            }
        }
    }
}

@Composable
private fun navColors() = NavigationBarItemDefaults.colors(
    selectedIconColor = AgentPalette.Mint,
    selectedTextColor = AgentPalette.Mint,
    indicatorColor = AgentPalette.Mint.copy(alpha = 0.12f),
    unselectedIconColor = AgentPalette.TextSecondary,
    unselectedTextColor = AgentPalette.TextSecondary
)

@Composable
private fun HomeScreen(
    tasks: List<AgentTask>,
    onOpenTasks: () -> Unit,
    onCreateDraft: (String) -> Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 16.dp, bottom = 26.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        BrandHeader()
        HeroPanel()
        OverviewMetrics(taskCount = tasks.size)
        TaskComposer(onCreateDraft = onCreateDraft)
        RecentTasks(tasks = tasks.take(3), onOpenTasks = onOpenTasks)
        Text(
            text = "إصدار تأسيسي 0.1  •  لا يوجد تنفيذ تلقائي",
            style = MaterialTheme.typography.labelSmall,
            color = AgentPalette.TextSecondary,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun BrandHeader() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(AgentPalette.Mint),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.AutoAwesome,
                contentDescription = null,
                tint = Color(0xFF08251E),
                modifier = Modifier.size(25.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = "PHONE AGENT",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.3.sp,
                color = AgentPalette.TextPrimary
            )
            Text(
                text = "وكيلك الرقمي، تحت سيطرتك",
                style = MaterialTheme.typography.bodySmall,
                color = AgentPalette.TextSecondary
            )
        }
        Spacer(Modifier.weight(1f))
        Surface(
            shape = CircleShape,
            color = AgentPalette.SurfaceElevated,
            border = BorderStroke(1.dp, AgentPalette.Border)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(AgentPalette.Amber)
                )
                Text(
                    text = "تجريبي",
                    color = AgentPalette.TextSecondary,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

@Composable
private fun HeroPanel() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF18372F),
                        Color(0xFF142833),
                        Color(0xFF17222E)
                    )
                )
            )
            .border(1.dp, Color(0xFF2C5148), RoundedCornerShape(28.dp))
            .padding(22.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Lock,
                    contentDescription = null,
                    tint = AgentPalette.Mint,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "LOCAL FIRST  /  01",
                    color = AgentPalette.Mint,
                    style = MaterialTheme.typography.labelMedium,
                    letterSpacing = 1.5.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = "خطوة بخطوة.\nوبإذنك فقط.",
                style = MaterialTheme.typography.headlineMedium,
                color = AgentPalette.TextPrimary,
                fontWeight = FontWeight.Bold,
                lineHeight = 36.sp
            )
            Text(
                text = "حوّل أفكارك إلى مهام منظمة. في هذا الإصدار، تُحفظ الطلبات كمسودات محلية ولا تُنفّذ على الهاتف.",
                color = Color(0xFFD0DFE1),
                style = MaterialTheme.typography.bodyMedium,
                lineHeight = 23.sp
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF0B181F).copy(alpha = 0.60f))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.CloudOff,
                    contentDescription = null,
                    tint = AgentPalette.Amber,
                    modifier = Modifier.size(18.dp)
                )
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "لا يوجد اتصال بخدمات خارجية",
                        color = AgentPalette.TextPrimary,
                        style = MaterialTheme.typography.labelLarge
                    )
                    Text(
                        text = "لا اتصال شبكة ولا صلاحيات تحكم مطلوبة",
                        color = AgentPalette.TextSecondary,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
private fun OverviewMetrics(taskCount: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        MetricCard(
            modifier = Modifier.weight(1f),
            value = taskCount.toString(),
            label = "مسودات محفوظة",
            icon = { Icon(Icons.Outlined.FormatListBulleted, contentDescription = null, tint = AgentPalette.Sky) }
        )
        MetricCard(
            modifier = Modifier.weight(1f),
            value = "0",
            label = "صلاحيات تحكم",
            icon = { Icon(Icons.Outlined.Security, contentDescription = null, tint = AgentPalette.Mint) }
        )
        MetricCard(
            modifier = Modifier.weight(1f),
            value = "متوقف",
            label = "التنفيذ",
            icon = { Icon(Icons.Outlined.Lock, contentDescription = null, tint = AgentPalette.Amber) }
        )
    }
}

@Composable
private fun MetricCard(
    modifier: Modifier = Modifier,
    value: String,
    label: String,
    icon: @Composable () -> Unit
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = AgentPalette.Surface),
        border = BorderStroke(1.dp, AgentPalette.Border)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(AgentPalette.SurfaceSoft),
                contentAlignment = Alignment.Center
            ) { icon() }
            Text(
                text = value,
                color = AgentPalette.TextPrimary,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = label,
                color = AgentPalette.TextSecondary,
                style = MaterialTheme.typography.labelSmall,
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
private fun TaskComposer(onCreateDraft: (String) -> Boolean) {
    var draft by rememberSaveable { mutableStateOf("") }
    var validationMessage by rememberSaveable { mutableStateOf<String?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeading(
            title = "ما المهمة التي تريد تنظيمها؟",
            supporting = "ابدأ بالطلب؛ لن يحدث أي إجراء على الجهاز."
        )

        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = AgentPalette.Surface),
            border = BorderStroke(1.dp, AgentPalette.Border)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = draft,
                    onValueChange = {
                        draft = it.take(1000)
                        validationMessage = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text(
                            "مثال: رتّب خطوات فحص ملفات مشروعي…",
                            color = AgentPalette.TextSecondary
                        )
                    },
                    minLines = 3,
                    maxLines = 5,
                    shape = RoundedCornerShape(16.dp),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Default
                    ),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = AgentPalette.Background,
                        unfocusedContainerColor = AgentPalette.Background,
                        disabledContainerColor = AgentPalette.Background,
                        focusedIndicatorColor = AgentPalette.Mint,
                        unfocusedIndicatorColor = AgentPalette.Border,
                        focusedTextColor = AgentPalette.TextPrimary,
                        unfocusedTextColor = AgentPalette.TextPrimary,
                        cursorColor = AgentPalette.Mint
                    )
                )
                if (validationMessage != null) {
                    Text(
                        text = validationMessage.orEmpty(),
                        color = AgentPalette.Danger,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Button(
                    onClick = {
                        if (draft.isBlank()) {
                            validationMessage = "اكتب وصفًا للمهمة أولًا."
                        } else if (onCreateDraft(draft)) {
                            draft = ""
                            validationMessage = null
                        } else {
                            validationMessage = "تعذّر حفظ المسودة. حاول مرة أخرى."
                        }
                    },
                    enabled = draft.isNotBlank(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AgentPalette.Mint,
                        contentColor = Color(0xFF08251E),
                        disabledContainerColor = AgentPalette.SurfaceSoft,
                        disabledContentColor = AgentPalette.TextSecondary
                    )
                ) {
                    Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(19.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("حفظ كمسودة محلية", fontWeight = FontWeight.Bold)
                }
                Text(
                    text = "لن تُرسل المسودة إلى أي خادم في هذا الإصدار.",
                    color = AgentPalette.TextSecondary,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ExampleChip(label = "فحص ملفات", onClick = { draft = "أنشئ قائمة لفحص ملفات المشروع" })
            ExampleChip(label = "خطة عمل", onClick = { draft = "قسّم المهمة إلى خطوات قابلة للمراجعة" })
        }
    }
}

@Composable
private fun ExampleChip(label: String, onClick: () -> Unit) {
    AssistChip(
        onClick = onClick,
        label = { Text(label) },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = AgentPalette.SurfaceElevated,
            labelColor = AgentPalette.TextSecondary
        ),
        border = BorderStroke(1.dp, AgentPalette.Border)
    )
}

@Composable
private fun RecentTasks(tasks: List<AgentTask>, onOpenTasks: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SectionHeading(
                title = "أحدث المسودات",
                supporting = "محفوظة على هذا الجهاز"
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = "عرض الكل",
                color = AgentPalette.Mint,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onOpenTasks)
                    .padding(7.dp)
            )
        }
        if (tasks.isEmpty()) {
            EmptyDraftCard()
        } else {
            tasks.forEach { task -> TaskRow(task = task) }
        }
    }
}

@Composable
private fun SectionHeading(title: String, supporting: String? = null) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = AgentPalette.TextPrimary,
            fontWeight = FontWeight.Bold
        )
        if (supporting != null) {
            Text(
                text = supporting,
                style = MaterialTheme.typography.bodySmall,
                color = AgentPalette.TextSecondary
            )
        }
    }
}

@Composable
private fun EmptyDraftCard() {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = AgentPalette.Surface),
        border = BorderStroke(1.dp, AgentPalette.Border)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(AgentPalette.SurfaceSoft),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.Add, contentDescription = null, tint = AgentPalette.Mint)
            }
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    "قائمتك تبدأ من هنا",
                    color = AgentPalette.TextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "اكتب أول مهمة واحفظها كمسودة.",
                    color = AgentPalette.TextSecondary,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun TaskRow(task: AgentTask) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = AgentPalette.Surface),
        border = BorderStroke(1.dp, AgentPalette.Border)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(11.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(AgentPalette.MintDeep),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.Schedule, contentDescription = null, tint = AgentPalette.Mint)
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = task.title,
                    color = AgentPalette.TextPrimary,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${formatTimestamp(task.createdAtEpochMillis)}  •  مسودة محلية",
                    color = AgentPalette.TextSecondary,
                    style = MaterialTheme.typography.labelSmall
                )
            }
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = AgentPalette.SurfaceSoft
            ) {
                Text(
                    text = "غير منفّذة",
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 5.dp),
                    color = AgentPalette.Amber,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

@Composable
private fun TasksScreen(
    tasks: List<AgentTask>,
    onRefresh: () -> Unit,
    onNewTask: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 18.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        BrandHeader()
        SectionHeading(
            title = "قائمة المهام",
            supporting = "كل عنصر هنا مسودة نصية؛ لا يوجد منفّذ أوامر في الإصدار الحالي."
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatusPill(icon = { Icon(Icons.Outlined.CheckCircle, contentDescription = null) }, text = "محفوظة محليًا")
            Spacer(Modifier.weight(1f))
            Text(
                text = "تحديث",
                color = AgentPalette.Mint,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onRefresh)
                    .padding(8.dp),
                style = MaterialTheme.typography.labelLarge
            )
        }
        if (tasks.isEmpty()) {
            EmptyDraftCard()
        } else {
            tasks.forEach { TaskRow(task = it) }
        }
        Button(
            onClick = onNewTask,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = AgentPalette.Mint,
                contentColor = Color(0xFF08251E)
            )
        ) {
            Icon(Icons.Outlined.Add, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("إنشاء مسودة جديدة", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun StatusPill(icon: @Composable () -> Unit, text: String) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = AgentPalette.MintDeep,
        border = BorderStroke(1.dp, Color(0xFF2B554A))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(Modifier.size(15.dp)) { icon() }
            Text(text, color = AgentPalette.Mint, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun SafetyScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 18.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        BrandHeader()
        SectionHeading(
            title = "الخصوصية والصلاحيات",
            supporting = "حالة فعلية لهذا البناء، وليست وعودًا مستقبلية."
        )
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = AgentPalette.Surface),
            border = BorderStroke(1.dp, AgentPalette.Border)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(AgentPalette.MintDeep),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.Shield, contentDescription = null, tint = AgentPalette.Mint)
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text("لا توجد صلاحيات خطرة مطلوبة", color = AgentPalette.TextPrimary, fontWeight = FontWeight.Bold)
                        Text("لا يوجد تحكم بالتطبيقات الأخرى", color = AgentPalette.TextSecondary, style = MaterialTheme.typography.bodySmall)
                    }
                }
                HorizontalDivider(color = AgentPalette.Border)
                SafetyRow(
                    title = "صلاحيات أندرويد",
                    state = "لا توجد صلاحيات وقت تشغيل",
                    detail = "الـ Manifest الحالي لا يطلب صلاحيات وصول حساسة."
                )
                SafetyRow(
                    title = "خدمة إمكانية الوصول",
                    state = "لم تُضف بعد",
                    detail = "لا يستطيع التطبيق قراءة واجهات التطبيقات الأخرى أو الضغط داخلها."
                )
                SafetyRow(
                    title = "اتصال الشبكة",
                    state = "غير مفعّل",
                    detail = "لا يوجد إذن INTERNET في بيان التطبيق، ولا يوجد اتصال بنموذج ذكاء اصطناعي."
                )
                SafetyRow(
                    title = "تنفيذ المهام",
                    state = "متوقف بالكامل",
                    detail = "المهام نصوص محفوظة فقط، وليست أوامر قابلة للتنفيذ."
                )
            }
        }
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = AgentPalette.SurfaceElevated),
            border = BorderStroke(1.dp, AgentPalette.Border)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Outlined.PhoneAndroid, contentDescription = null, tint = AgentPalette.Sky, modifier = Modifier.size(22.dp))
                Text(
                    "كيف ستُضاف الصلاحيات لاحقًا؟",
                    color = AgentPalette.TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "كل قدرة ستُضاف كوحدة منفصلة، مع شرح ما تستطيع فعله، وطلب تفعيلها منك داخل إعدادات أندرويد، وسجل يوضح ما نُفّذ. لن تُفعّل أي قدرة بصمت.",
                    color = AgentPalette.TextSecondary,
                    style = MaterialTheme.typography.bodyMedium,
                    lineHeight = 22.sp
                )
            }
        }
    }
}

@Composable
private fun SafetyRow(
    title: String,
    state: String,
    detail: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                color = AgentPalette.TextPrimary,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = state,
                color = AgentPalette.Mint,
                style = MaterialTheme.typography.labelSmall
            )
        }
        Text(
            text = detail,
            color = AgentPalette.TextSecondary,
            style = MaterialTheme.typography.bodySmall,
            lineHeight = 19.sp
        )
    }
}

private fun formatTimestamp(timestamp: Long): String {
    if (timestamp <= 0L) return "وقت غير معروف"
    return SimpleDateFormat("dd/MM  HH:mm", Locale.getDefault()).format(Date(timestamp))
}
