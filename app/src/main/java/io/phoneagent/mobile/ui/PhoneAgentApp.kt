package io.phoneagent.mobile.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.LocalFocusManager
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Stroke
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.phoneagent.mobile.data.AgentTask
import io.phoneagent.mobile.data.ChatMessage
import io.phoneagent.mobile.data.LocalCompanion
import io.phoneagent.mobile.data.TaskStore

private enum class MainTab {
    CHAT, ACTIVITY, SAFETY
}

private data class AvatarPersona(
    val id: String,
    val name: String,
    val role: String,
    val tagline: String,
    val accent: Color,
    val deep: Color
)

private val personas = listOf(
    AvatarPersona(
        id = "nova",
        name = "NOVA",
        role = "منظّمة الأفكار",
        tagline = "نحوّل الفكرة إلى خطوات واضحة.",
        accent = Color(0xFF72E4C2),
        deep = Color(0xFF163C37)
    ),
    AvatarPersona(
        id = "orion",
        name = "ORION",
        role = "المهندس التقني",
        tagline = "تفكير تقني، اختبار، ثم تحقق.",
        accent = Color(0xFF91B9FF),
        deep = Color(0xFF1A3150)
    ),
    AvatarPersona(
        id = "sentinel",
        name = "SENTINEL",
        role = "حارس الأمان",
        tagline = "الصلاحية قبل التنفيذ.",
        accent = Color(0xFFFFC47D),
        deep = Color(0xFF4A3424)
    )
)

@Composable
fun PhoneAgentApp(taskStore: TaskStore) {
    var selectedPersonaId by rememberSaveable { mutableStateOf("nova") }
    val selectedPersona = personas.firstOrNull { it.id == selectedPersonaId } ?: personas.first()
    var messages by remember(selectedPersonaId) {
        mutableStateOf(taskStore.loadConversation(selectedPersonaId))
    }
    var tasks by remember { mutableStateOf(taskStore.loadDrafts()) }
    var currentTabName by rememberSaveable { mutableStateOf(MainTab.CHAT.name) }
    val currentTab = MainTab.values().firstOrNull { it.name == currentTabName } ?: MainTab.CHAT
    var composerText by rememberSaveable { mutableStateOf("") }

    val sendMessage: () -> Unit = {
        val clean = composerText.trim()
        if (clean.isNotBlank()) {
            taskStore.addConversationMessage(selectedPersonaId, clean, isUser = true)
            val actionable = LocalCompanion.isActionRequest(clean)
            val savedTask = if (actionable) taskStore.addDraft(clean) else null
            val reply = LocalCompanion.reply(
                companionId = selectedPersonaId,
                userText = clean,
                taskWasSaved = savedTask != null
            )
            taskStore.addConversationMessage(selectedPersonaId, reply, isUser = false)
            messages = taskStore.loadConversation(selectedPersonaId)
            tasks = taskStore.loadDrafts()
            composerText = ""
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            containerColor = AgentPalette.Background,
            bottomBar = {
                Column {
                    if (currentTab == MainTab.CHAT) {
                        ChatComposer(
                            value = composerText,
                            onValueChange = { composerText = it.take(2000) },
                            onSend = sendMessage
                        )
                    }
                    NavigationBar(
                        containerColor = AgentPalette.Surface,
                        contentColor = AgentPalette.TextPrimary,
                        tonalElevation = 0.dp
                    ) {
                        NavigationBarItem(
                            selected = currentTab == MainTab.CHAT,
                            onClick = { currentTabName = MainTab.CHAT.name },
                            icon = { Icon(Icons.Outlined.ChatBubbleOutline, contentDescription = null) },
                            label = { Text("الحوار") },
                            colors = navColors()
                        )
                        NavigationBarItem(
                            selected = currentTab == MainTab.ACTIVITY,
                            onClick = { currentTabName = MainTab.ACTIVITY.name; tasks = taskStore.loadDrafts() },
                            icon = { Icon(Icons.Outlined.History, contentDescription = null) },
                            label = { Text("النشاط") },
                            colors = navColors()
                        )
                        NavigationBarItem(
                            selected = currentTab == MainTab.SAFETY,
                            onClick = { currentTabName = MainTab.SAFETY.name },
                            icon = { Icon(Icons.Outlined.Shield, contentDescription = null) },
                            label = { Text("الأمان") },
                            colors = navColors()
                        )
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(AgentPalette.Background)
            ) {
                when (currentTab) {
                    MainTab.CHAT -> ConversationScreen(
                        persona = selectedPersona,
                        messages = messages,
                        onChoosePersona = { selectedPersonaId = it },
                        onQuickPrompt = { composerText = it }
                    )
                    MainTab.ACTIVITY -> ActivityScreen(tasks = tasks)
                    MainTab.SAFETY -> SafetyScreen()
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
private fun ConversationScreen(
    persona: AvatarPersona,
    messages: List<ChatMessage>,
    onChoosePersona: (String) -> Unit,
    onQuickPrompt: (String) -> Unit
) {
    val scrollState = rememberScrollState()
    LaunchedEffect(messages.size, persona.id) {
        scrollState.animateScrollTo(scrollState.maxValue)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp)
            .padding(top = 12.dp, bottom = 18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        AppHeader()
        CompanionStage(persona)
        PersonaPicker(selectedId = persona.id, onChoose = onChoosePersona)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    "مساحة الحوار",
                    color = AgentPalette.TextPrimary,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "محادثة محفوظة محليًا مع " + persona.name,
                    color = AgentPalette.TextSecondary,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Spacer(Modifier.weight(1f))
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = AgentPalette.SurfaceElevated,
                border = BorderStroke(1.dp, AgentPalette.Border)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Box(
                        Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(AgentPalette.Amber)
                    )
                    Text("محلي", color = AgentPalette.TextSecondary, style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        if (messages.isEmpty()) {
            WelcomeMessage(persona)
        } else {
            messages.takeLast(40).forEach { message ->
                ChatBubble(message = message, persona = persona)
            }
        }

        Text(
            text = "ردود هذه النسخة تجريبية ومحلية؛ لم يُربط نموذج ذكاء اصطناعي بعد.",
            color = AgentPalette.TextSecondary,
            style = MaterialTheme.typography.labelSmall,
            lineHeight = 17.sp,
            modifier = Modifier.padding(start = 4.dp, top = 2.dp, bottom = 4.dp)
        )
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            QuickPrompt("ما الذي تستطيع فعله؟", onClick = { onQuickPrompt("ما الذي تستطيع فعله؟") })
            QuickPrompt("ساعدني في التخطيط", onClick = { onQuickPrompt("ساعدني في التخطيط") })
            QuickPrompt("افحص بناء المشروع", onClick = { onQuickPrompt("افحص بناء المشروع") })
        }
    }
}

@Composable
private fun AppHeader() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(AgentPalette.Mint),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Outlined.AutoAwesome,
                contentDescription = null,
                tint = Color(0xFF08251E),
                modifier = Modifier.size(23.dp)
            )
        }
        Spacer(Modifier.width(10.dp))
        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(
                "PHONE AGENT",
                color = AgentPalette.TextPrimary,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.4.sp
            )
            Text("رفيقك الرقمي", color = AgentPalette.TextSecondary, style = MaterialTheme.typography.bodySmall)
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
                Text("نموذج غير متصل", color = AgentPalette.TextSecondary, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun CompanionStage(persona: AvatarPersona) {
    val transition = rememberInfiniteTransition(label = "stage-" + persona.id)
    val floatAmount by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(2400), repeatMode = RepeatMode.Reverse),
        label = "stage-pulse"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(30.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        persona.deep.copy(alpha = 0.96f),
                        Color(0xFF12202B),
                        AgentPalette.Surface
                    )
                )
            )
            .border(1.dp, persona.accent.copy(alpha = 0.26f), RoundedCornerShape(30.dp))
            .padding(horizontal = 18.dp, vertical = 18.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Outlined.Bolt, contentDescription = null, tint = persona.accent, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(5.dp))
                Text(
                    "COMPANION SPACE",
                    color = persona.accent,
                    style = MaterialTheme.typography.labelSmall,
                    letterSpacing = 1.4.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.weight(1f))
                Text("01 / LOCAL", color = AgentPalette.TextSecondary, style = MaterialTheme.typography.labelSmall)
            }
            Spacer(Modifier.height(2.dp))
            Box(
                modifier = Modifier.size(174.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(170.dp * (0.98f + floatAmount * 0.025f))
                        .border(1.dp, persona.accent.copy(alpha = 0.18f), CircleShape)
                )
                Box(
                    modifier = Modifier
                        .size(144.dp)
                        .border(1.dp, persona.accent.copy(alpha = 0.24f), CircleShape)
                )
                AvatarPortrait(persona = persona, size = 132.dp)
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 17.dp, end = 13.dp)
                        .size(13.dp)
                        .clip(CircleShape)
                        .background(persona.accent)
                        .border(3.dp, persona.deep, CircleShape)
                )
            }
            Text(
                persona.name,
                color = AgentPalette.TextPrimary,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 2.3.sp
            )
            Text(
                persona.role,
                color = persona.accent,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                persona.tagline,
                color = AgentPalette.TextSecondary,
                style = MaterialTheme.typography.bodyMedium
            )
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF0A1118).copy(alpha = 0.55f),
                border = BorderStroke(1.dp, persona.accent.copy(alpha = 0.15f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    Icon(Icons.Outlined.Lock, contentDescription = null, tint = persona.accent, modifier = Modifier.size(15.dp))
                    Text(
                        "المحادثة محلية • لا تنفيذ تلقائي",
                        color = AgentPalette.TextSecondary,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}

@Composable
private fun AvatarPortrait(persona: AvatarPersona, size: Dp) {
    Canvas(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
    ) {
        val w = size.width
        val h = size.height
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    persona.accent.copy(alpha = 0.43f),
                    persona.deep.copy(alpha = 0.92f),
                    Color(0xFF0C151E)
                ),
                center = Offset(w * 0.32f, h * 0.24f),
                radius = w * 0.88f
            ),
            radius = w * 0.49f
        )
        drawCircle(
            color = persona.accent.copy(alpha = 0.58f),
            radius = w * 0.465f,
            style = Stroke(width = w * 0.012f)
        )

        // A hand-drawn abstract android face keeps the avatar assets lightweight
        // and fully local—no remote images or tracking resources are required.
        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFFE2F1F2), Color(0xFF7E9DA7), Color(0xFF324A58))
            ),
            topLeft = Offset(w * 0.245f, h * 0.19f),
            size = Size(w * 0.51f, h * 0.61f),
            cornerRadius = CornerRadius(w * 0.18f, w * 0.18f)
        )
        drawRoundRect(
            color = Color(0xFF11202B),
            topLeft = Offset(w * 0.23f, h * 0.40f),
            size = Size(w * 0.54f, h * 0.18f),
            cornerRadius = CornerRadius(w * 0.065f, w * 0.065f)
        )
        val eyeY = h * 0.49f
        drawCircle(persona.accent, radius = w * 0.046f, center = Offset(w * 0.37f, eyeY))
        drawCircle(persona.accent, radius = w * 0.046f, center = Offset(w * 0.63f, eyeY))
        drawCircle(Color.White.copy(alpha = 0.9f), radius = w * 0.012f, center = Offset(w * 0.383f, eyeY - w * 0.012f))
        drawCircle(Color.White.copy(alpha = 0.9f), radius = w * 0.012f, center = Offset(w * 0.643f, eyeY - w * 0.012f))
        drawArc(
            color = persona.accent,
            startAngle = 18f,
            sweepAngle = 144f,
            useCenter = false,
            topLeft = Offset(w * 0.405f, h * 0.51f),
            size = Size(w * 0.19f, h * 0.10f),
            style = Stroke(width = w * 0.014f)
        )
        if (persona.id == "orion") {
            drawLine(
                color = persona.accent,
                start = Offset(w * 0.5f, h * 0.18f),
                end = Offset(w * 0.5f, h * 0.09f),
                strokeWidth = w * 0.018f
            )
            drawCircle(persona.accent, radius = w * 0.035f, center = Offset(w * 0.5f, h * 0.085f))
        }
        if (persona.id == "sentinel") {
            drawCircle(
                color = persona.accent.copy(alpha = 0.88f),
                radius = w * 0.027f,
                center = Offset(w * 0.5f, h * 0.70f)
            )
        }
    }
}

@Composable
private fun PersonaPicker(selectedId: String, onChoose: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("اختر رفيق الحوار", color = AgentPalette.TextPrimary, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            Text("3 شخصيات", color = AgentPalette.TextSecondary, style = MaterialTheme.typography.labelSmall)
        }
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            personas.forEach { persona ->
                val selected = persona.id == selectedId
                Surface(
                    modifier = Modifier
                        .width(118.dp)
                        .clickable { onChoose(persona.id) },
                    shape = RoundedCornerShape(19.dp),
                    color = if (selected) persona.deep.copy(alpha = 0.75f) else AgentPalette.Surface,
                    border = BorderStroke(
                        if (selected) 1.5.dp else 1.dp,
                        if (selected) persona.accent.copy(alpha = 0.75f) else AgentPalette.Border
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 11.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        AvatarPortrait(persona, 49.dp)
                        Text(
                            persona.name,
                            color = if (selected) persona.accent else AgentPalette.TextPrimary,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        )
                        Text(
                            persona.role,
                            color = AgentPalette.TextSecondary,
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WelcomeMessage(persona: AvatarPersona) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        AvatarPortrait(persona, 38.dp)
        Column(
            modifier = Modifier
                .widthIn(max = 310.dp)
                .clip(RoundedCornerShape(topStart = 5.dp, topEnd = 20.dp, bottomEnd = 20.dp, bottomStart = 20.dp))
                .background(AgentPalette.Surface)
                .border(1.dp, AgentPalette.Border, RoundedCornerShape(topStart = 5.dp, topEnd = 20.dp, bottomEnd = 20.dp, bottomStart = 20.dp))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text("أنا " + persona.name, color = persona.accent, fontWeight = FontWeight.Bold)
            Text(
                "مرحبًا. " + persona.tagline + " اختر سؤالًا مقترحًا أو اكتب ما تريد مناقشته. هذه تجربة محلية أولية؛ لن أدّعي أنني أملك قدرات لم تُربط بعد.",
                color = AgentPalette.TextPrimary,
                style = MaterialTheme.typography.bodyMedium,
                lineHeight = 21.sp
            )
        }
    }
}

@Composable
private fun ChatBubble(message: ChatMessage, persona: AvatarPersona) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isUser) Arrangement.Start else Arrangement.End,
        verticalAlignment = Alignment.Top
    ) {
        if (!message.isUser) {
            AvatarPortrait(persona, 34.dp)
            Spacer(Modifier.width(8.dp))
        }
        Column(
            modifier = Modifier
                .widthIn(max = 318.dp)
                .clip(
                    if (message.isUser) RoundedCornerShape(20.dp, 20.dp, 5.dp, 20.dp)
                    else RoundedCornerShape(5.dp, 20.dp, 20.dp, 20.dp)
                )
                .background(if (message.isUser) AgentPalette.MintDeep else AgentPalette.Surface)
                .border(
                    1.dp,
                    if (message.isUser) AgentPalette.Mint.copy(alpha = 0.20f) else AgentPalette.Border,
                    if (message.isUser) RoundedCornerShape(20.dp, 20.dp, 5.dp, 20.dp)
                    else RoundedCornerShape(5.dp, 20.dp, 20.dp, 20.dp)
                )
                .padding(horizontal = 14.dp, vertical = 11.dp)
        ) {
            if (!message.isUser) {
                Text(
                    persona.name,
                    color = persona.accent,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.7.sp
                )
                Spacer(Modifier.height(4.dp))
            }
            Text(
                message.text,
                color = AgentPalette.TextPrimary,
                style = MaterialTheme.typography.bodyMedium,
                lineHeight = 21.sp
            )
        }
    }
}

@Composable
private fun QuickPrompt(label: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = AgentPalette.SurfaceElevated,
        border = BorderStroke(1.dp, AgentPalette.Border)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(label, color = AgentPalette.TextSecondary, style = MaterialTheme.typography.labelMedium)
            Icon(Icons.Outlined.ChevronLeft, contentDescription = null, tint = AgentPalette.TextSecondary, modifier = Modifier.size(15.dp))
        }
    }
}

@Composable
private fun ChatComposer(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit
) {
    val focusManager = LocalFocusManager.current
    Surface(
        color = AgentPalette.Surface,
        tonalElevation = 0.dp
    ) {
        Column {
            HorizontalDivider(color = AgentPalette.Border)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(22.dp),
                    color = AgentPalette.Background,
                    border = BorderStroke(1.dp, AgentPalette.Border)
                ) {
                    BasicTextField(
                        value = value,
                        onValueChange = onValueChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 15.dp, vertical = 13.dp),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(color = AgentPalette.TextPrimary),
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences,
                            imeAction = ImeAction.Send
                        ),
                        keyboardActions = KeyboardActions(
                            onSend = {
                                onSend()
                                focusManager.clearFocus()
                            }
                        ),
                        maxLines = 4,
                        decorationBox = { innerTextField ->
                            Box {
                                if (value.isBlank()) {
                                    Text(
                                        "اكتب رسالة إلى " + currentPersonaLabel(value, "رفيقك") + "…",
                                        color = AgentPalette.TextSecondary,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                                innerTextField()
                            }
                        }
                    )
                }
                Surface(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .clickable(enabled = value.isNotBlank(), onClick = {
                            onSend()
                            focusManager.clearFocus()
                        }),
                    shape = CircleShape,
                    color = if (value.isNotBlank()) AgentPalette.Mint else AgentPalette.SurfaceSoft
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Outlined.Send,
                            contentDescription = "إرسال الرسالة",
                            tint = if (value.isNotBlank()) Color(0xFF08251E) else AgentPalette.TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
            Text(
                "الرسائل محفوظة على الهاتف. لا يوجد إرسال لخدمة خارجية.",
                color = AgentPalette.TextSecondary,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 18.dp, end = 18.dp, bottom = 7.dp)
            )
        }
    }
}

private fun currentPersonaLabel(value: String, fallback: String): String = fallback

@Composable
private fun ActivityScreen(tasks: List<AgentTask>) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp)
            .padding(top = 15.dp, bottom = 22.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        AppHeader()
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("مركز النشاط", color = AgentPalette.TextPrimary, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("ما يرسله الحوار يظهر هنا. التنفيذ الحقيقي لم يُفعّل بعد.", color = AgentPalette.TextSecondary, style = MaterialTheme.typography.bodySmall)
        }
        Surface(
            shape = RoundedCornerShape(25.dp),
            color = AgentPalette.Surface,
            border = BorderStroke(1.dp, AgentPalette.Border)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(43.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(AgentPalette.Amber.copy(alpha = 0.13f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.Terminal, contentDescription = null, tint = AgentPalette.Amber)
                    }
                    Spacer(Modifier.width(11.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text("محرك تنفيذ الهاتف", color = AgentPalette.TextPrimary, fontWeight = FontWeight.Bold)
                        Text("غير موصول — لا توجد إجراءات تعمل بالخلفية", color = AgentPalette.Amber, style = MaterialTheme.typography.bodySmall)
                    }
                }
                HorizontalDivider(color = AgentPalette.Border)
                Text(
                    "تُسجّل الأوامر العملية التي ترسلها كمسودات محلية فقط. عند ربط محرك التنفيذ لاحقًا، سيُعرض هنا تقدّم العمل والنتيجة والأخطاء والإلغاء.",
                    color = AgentPalette.TextSecondary,
                    style = MaterialTheme.typography.bodyMedium,
                    lineHeight = 21.sp
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ActivityMetric(modifier = Modifier.weight(1f), value = tasks.size.toString(), label = "مسودات", icon = { Icon(Icons.Outlined.History, contentDescription = null, tint = AgentPalette.Sky) })
            ActivityMetric(modifier = Modifier.weight(1f), value = "0", label = "قيد التنفيذ", icon = { Icon(Icons.Outlined.Bolt, contentDescription = null, tint = AgentPalette.Amber) })
            ActivityMetric(modifier = Modifier.weight(1f), value = "0", label = "مكتمل", icon = { Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = AgentPalette.Mint) })
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("سجل الطلبات", color = AgentPalette.TextPrimary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            Text("محلي فقط", color = AgentPalette.TextSecondary, style = MaterialTheme.typography.labelSmall)
        }
        if (tasks.isEmpty()) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = AgentPalette.Surface,
                border = BorderStroke(1.dp, AgentPalette.Border)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Outlined.History, contentDescription = null, tint = AgentPalette.TextSecondary, modifier = Modifier.size(29.dp))
                    Text("لا توجد طلبات محفوظة", color = AgentPalette.TextPrimary, fontWeight = FontWeight.SemiBold)
                    Text("عندما ترسل طلبًا عمليًا في الحوار سيظهر هنا كمسودة.", color = AgentPalette.TextSecondary, style = MaterialTheme.typography.bodySmall)
                }
            }
        } else {
            tasks.forEach { task -> DraftActivityRow(task) }
        }
    }
}

@Composable
private fun ActivityMetric(
    modifier: Modifier,
    value: String,
    label: String,
    icon: @Composable () -> Unit
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(19.dp),
        color = AgentPalette.Surface,
        border = BorderStroke(1.dp, AgentPalette.Border)
    ) {
        Column(
            modifier = Modifier.padding(13.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                Modifier
                    .size(29.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(AgentPalette.SurfaceSoft),
                contentAlignment = Alignment.Center
            ) { icon() }
            Text(value, color = AgentPalette.TextPrimary, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(label, color = AgentPalette.TextSecondary, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun DraftActivityRow(task: AgentTask) {
    Surface(
        shape = RoundedCornerShape(19.dp),
        color = AgentPalette.Surface,
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
                Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(AgentPalette.SurfaceSoft),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.Tune, contentDescription = null, tint = AgentPalette.Sky)
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(task.title, color = AgentPalette.TextPrimary, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    java.text.SimpleDateFormat("dd/MM  HH:mm", java.util.Locale.getDefault())
                        .format(java.util.Date(task.createdAtEpochMillis)),
                    color = AgentPalette.TextSecondary,
                    style = MaterialTheme.typography.labelSmall
                )
            }
            Surface(shape = RoundedCornerShape(9.dp), color = AgentPalette.MintDeep) {
                Text("مسودة", modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp), color = AgentPalette.Mint, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun SafetyScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp)
            .padding(top = 15.dp, bottom = 22.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        AppHeader()
        Text("مركز الثقة", color = AgentPalette.TextPrimary, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("حالة هذا البناء الآن", color = AgentPalette.TextSecondary, style = MaterialTheme.typography.bodySmall)
        Surface(
            shape = RoundedCornerShape(25.dp),
            color = AgentPalette.Surface,
            border = BorderStroke(1.dp, AgentPalette.Border)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(15.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(11.dp)) {
                    Box(
                        Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(15.dp))
                            .background(AgentPalette.MintDeep),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.Shield, contentDescription = null, tint = AgentPalette.Mint, modifier = Modifier.size(25.dp))
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text("لا توجد صلاحيات تحكم مفعّلة", color = AgentPalette.TextPrimary, fontWeight = FontWeight.Bold)
                        Text("هذا البناء لا يستطيع التحكم بالتطبيقات الأخرى.", color = AgentPalette.TextSecondary, style = MaterialTheme.typography.bodySmall)
                    }
                }
                HorizontalDivider(color = AgentPalette.Border)
                TrustRow("البيانات", "محلية", "المحادثات والمسودات تحفظ في مساحة التطبيق الخاصة.")
                TrustRow("الشبكة", "غير مفعّلة", "لا يوجد إذن INTERNET أو اتصال بنموذج خارجي.")
                TrustRow("Accessibility", "غير مضافة", "لا يستطيع التطبيق قراءة واجهات التطبيقات الأخرى أو الضغط داخلها.")
                TrustRow("تنفيذ الأوامر", "متوقف", "الطلبات العملية تتحول إلى مسودات نصية فقط.")
            }
        }
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = AgentPalette.SurfaceElevated,
            border = BorderStroke(1.dp, AgentPalette.Border)
        ) {
            Column(
                Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("قاعدة التصميم", color = AgentPalette.Mint, fontWeight = FontWeight.Bold)
                Text(
                    "لن تُفعّل أي صلاحية بصمت. قبل إضافة التحكم، سنعرض القدرة المطلوبة، والبيانات التي قد تُقرأ أو تتغير، ثم نطلب منك تفعيلها يدويًا ونضيف سجلًا قابلًا للمراجعة.",
                    color = AgentPalette.TextSecondary,
                    style = MaterialTheme.typography.bodyMedium,
                    lineHeight = 22.sp
                )
            }
        }
    }
}

@Composable
private fun TrustRow(title: String, state: String, detail: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, color = AgentPalette.TextPrimary, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.weight(1f))
            Text(state, color = AgentPalette.Mint, style = MaterialTheme.typography.labelMedium)
        }
        Text(detail, color = AgentPalette.TextSecondary, style = MaterialTheme.typography.bodySmall, lineHeight = 19.sp)
    }
}
