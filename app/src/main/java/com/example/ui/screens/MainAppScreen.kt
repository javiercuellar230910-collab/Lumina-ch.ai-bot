package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.database.CharacterProfile
import com.example.data.database.ChatMessage
import com.example.data.database.GlobalChatMessage
import com.example.ui.viewmodel.LuminaViewModel
import kotlinx.coroutines.launch

enum class TabScreen {
    EXPLORE,
    CHAT,
    SOCIAL,
    CREATE
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(
    viewModel: LuminaViewModel,
    modifier: Modifier = Modifier
) {
    val isLoggedIn by viewModel.isLoggedIn.collectAsState()
    val userName by viewModel.userName.collectAsState()
    val apiKey by viewModel.apiKey.collectAsState()

    var currentTab by remember { mutableStateOf(TabScreen.EXPLORE) }
    val activeCharacterId by viewModel.activeCharacterId.collectAsState()
    val characters by viewModel.characters.collectAsState()

    val activeChar = remember(activeCharacterId, characters) {
        characters.find { it.id == activeCharacterId }
    }

    if (!isLoggedIn) {
        OnboardingScreen(
            onLogin = { name, key -> viewModel.login(name, key) },
            initialApiKey = apiKey,
            modifier = modifier
        )
    } else {
        Scaffold(
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.testTag("bottom_nav_bar")
                ) {
                    NavigationBarItem(
                        selected = currentTab == TabScreen.EXPLORE,
                        onClick = { currentTab = TabScreen.EXPLORE },
                        icon = { Icon(Icons.Default.Explore, contentDescription = "Explorar") },
                        label = { Text("Explorar") },
                        modifier = Modifier.testTag("nav_explore")
                    )
                    NavigationBarItem(
                        selected = currentTab == TabScreen.CHAT,
                        onClick = { currentTab = TabScreen.CHAT },
                        icon = {
                            BadgedBox(badge = {
                                if (activeChar != null) {
                                    Badge(containerColor = MaterialTheme.colorScheme.primary)
                                }
                            }) {
                                Icon(Icons.Default.Message, contentDescription = "Chats")
                            }
                        },
                        label = { Text("Chat") },
                        modifier = Modifier.testTag("nav_chat")
                    )
                    NavigationBarItem(
                        selected = currentTab == TabScreen.SOCIAL,
                        onClick = { currentTab = TabScreen.SOCIAL },
                        icon = { Icon(Icons.Default.Public, contentDescription = "Global") },
                        label = { Text("Global") },
                        modifier = Modifier.testTag("nav_social")
                    )
                    NavigationBarItem(
                        selected = currentTab == TabScreen.CREATE,
                        onClick = { currentTab = TabScreen.CREATE },
                        icon = { Icon(Icons.Default.AddCircle, contentDescription = "Crear") },
                        label = { Text("Crear") },
                        modifier = Modifier.testTag("nav_create")
                    )
                }
            },
            modifier = modifier
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (currentTab) {
                    TabScreen.EXPLORE -> {
                        ExploreScreen(
                            characters = characters,
                            onSelectCharacter = { charId ->
                                viewModel.selectCharacter(charId)
                                currentTab = TabScreen.CHAT
                            },
                            onDeleteCharacter = { viewModel.deleteCharacter(it) }
                        )
                    }
                    TabScreen.CHAT -> {
                        ChatScreen(
                            viewModel = viewModel,
                            activeChar = activeChar,
                            onGoToExplore = { currentTab = TabScreen.EXPLORE }
                        )
                    }
                    TabScreen.SOCIAL -> {
                        SocialScreen(viewModel = viewModel)
                    }
                    TabScreen.CREATE -> {
                        CreateScreen(
                            onCreate = { name, desc, avatarSeed ->
                                val avatarUrl = "https://api.dicebear.com/7.x/avataaars/svg?seed=$avatarSeed"
                                viewModel.createCharacter(name, desc, avatarUrl)
                                currentTab = TabScreen.CHAT
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun OnboardingScreen(
    onLogin: (String, String) -> Unit,
    initialApiKey: String,
    modifier: Modifier = Modifier
) {
    var name by remember { mutableStateOf("") }
    var key by remember { mutableStateOf(initialApiKey) }
    val keyboardController = LocalSoftwareKeyboardController.current

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        MaterialTheme.colorScheme.surface
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth()
                .widthIn(max = 450.dp)
                .testTag("onboarding_card"),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Header Graphic
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Lumina Roleplay",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = "Crea perfiles y chatea con IA única",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Helpful API key card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Column {
                            Text(
                                text = "¿No tienes una API Key?",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Consíguela gratis registrándote en Google AI Studio para un rol ilimitado y súper veloz.",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre de Usuario") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("username_input"),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = key,
                    onValueChange = { key = it },
                    label = { Text("Gemini API Key") },
                    leadingIcon = { Icon(Icons.Default.Key, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("api_key_input"),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() })
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            onLogin(name, key)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("login_button"),
                    shape = RoundedCornerShape(16.dp),
                    enabled = name.isNotBlank()
                ) {
                    Text(
                        text = "Entrar al Servidor",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
fun ExploreScreen(
    characters: List<CharacterProfile>,
    onSelectCharacter: (String) -> Unit,
    onDeleteCharacter: (CharacterProfile) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Descubrir",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Text(
            text = "Selecciona un personaje para iniciar tu historia",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxSize()
                .testTag("explore_list")
        ) {
            items(characters, key = { it.id }) { char ->
                CharacterCard(
                    char = char,
                    onClick = { onSelectCharacter(char.id) },
                    onDelete = { onDeleteCharacter(char) }
                )
            }
        }
    }
}

@Composable
fun CharacterCard(
    char: CharacterProfile,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("character_card_${char.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(char.avatar)
                    .crossfade(true)
                    .build(),
                contentDescription = char.name,
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = char.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Creado por: ${char.creator}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = char.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Options menu (if custom character)
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Opciones")
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Iniciar Chat") },
                        onClick = {
                            showMenu = false
                            onClick()
                        },
                        leadingIcon = { Icon(Icons.Default.Chat, contentDescription = null) }
                    )
                    if (!char.isDefault) {
                        DropdownMenuItem(
                            text = { Text("Eliminar Personaje", color = MaterialTheme.colorScheme.error) },
                            onClick = {
                                showMenu = false
                                onDelete()
                            },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ChatScreen(
    viewModel: LuminaViewModel,
    activeChar: CharacterProfile?,
    onGoToExplore: () -> Unit
) {
    if (activeChar == null) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.Chat,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(40.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Ningún chat abierto",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Ve a la pestaña descubrir y selecciona un personaje para iniciar.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 24.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onGoToExplore) {
                Text("Ir a Explorar")
            }
        }
    } else {
        val messages by viewModel.messages.collectAsState()
        val isTyping by viewModel.isAITyping.collectAsState()
        val chatBgSeed by viewModel.chatBackground.collectAsState()
        val coroutineScope = rememberCoroutineScope()
        val listState = rememberLazyListState()

        var inputText by remember { mutableStateOf("") }
        var showSettings by remember { mutableStateOf(false) }

        // Background Styling
        val bgBrush = when (chatBgSeed) {
            "Slate" -> Brush.verticalGradient(listOf(Color(0xFF1E293B), Color(0xFF0F172A)))
            "Cyber" -> Brush.verticalGradient(listOf(Color(0xFF0D0221), Color(0xFF0F172A)))
            "Lavender" -> Brush.verticalGradient(listOf(Color(0xFFFAF5FF), Color(0xFFF3E8FF)))
            "Rose" -> Brush.verticalGradient(listOf(Color(0xFFFFF1F2), Color(0xFFFFE4E6)))
            else -> Brush.verticalGradient(listOf(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.surface))
        }

        val onBgColor = if (chatBgSeed == "Slate" || chatBgSeed == "Cyber") Color.White else MaterialTheme.colorScheme.onSurface

        // Automatically scroll to bottom on new messages
        LaunchedEffect(messages.size, isTyping) {
            if (messages.isNotEmpty()) {
                listState.animateScrollToItem(messages.size - 1)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(bgBrush)
        ) {
            // Chat Header
            Surface(
                tonalElevation = 8.dp,
                color = if (chatBgSeed == "Slate" || chatBgSeed == "Cyber") Color(0xFF1E293B) else MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { viewModel.selectCharacter(null) }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = onBgColor)
                    }

                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(activeChar.avatar)
                            .crossfade(true)
                            .build(),
                        contentDescription = activeChar.name,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentScale = ContentScale.Crop
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = activeChar.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = onBgColor
                        )
                        Text(
                            text = "Rol en curso • Gemini",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (chatBgSeed == "Slate" || chatBgSeed == "Cyber") Color.LightGray else MaterialTheme.colorScheme.primary
                        )
                    }

                    // Settings & Clear
                    Box {
                        IconButton(onClick = { showSettings = true }) {
                            Icon(Icons.Default.Settings, contentDescription = "Ajustes", tint = onBgColor)
                        }
                        DropdownMenu(
                            expanded = showSettings,
                            onDismissRequest = { showSettings = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Reiniciar Conversación") },
                                onClick = {
                                    showSettings = false
                                    viewModel.clearActiveChatHistory()
                                },
                                leadingIcon = { Icon(Icons.Default.Refresh, contentDescription = null) }
                            )
                            DropdownMenuSeparator()
                            DropdownMenuItem(
                                text = { Text("Fondo: Clásico") },
                                onClick = {
                                    viewModel.setChatBackground(null)
                                    showSettings = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Fondo: Slate") },
                                onClick = {
                                    viewModel.setChatBackground("Slate")
                                    showSettings = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Fondo: Cyber") },
                                onClick = {
                                    viewModel.setChatBackground("Cyber")
                                    showSettings = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Fondo: Lavender") },
                                onClick = {
                                    viewModel.setChatBackground("Lavender")
                                    showSettings = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Fondo: Rose") },
                                onClick = {
                                    viewModel.setChatBackground("Rose")
                                    showSettings = false
                                }
                            )
                        }
                    }
                }
            }

            // Message Area
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(top = 12.dp, bottom = 12.dp)
            ) {
                if (messages.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Card(
                                modifier = Modifier.padding(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (chatBgSeed == "Slate" || chatBgSeed == "Cyber") Color(0xFF1E293B) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                )
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "Reglas de Rol Inmersivo",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = if (chatBgSeed == "Slate" || chatBgSeed == "Cyber") Color.Yellow else MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "1. Escribe con normalidad para dialogar.\n2. Usa *asteriscos* para describir tus acciones y gestos.\n3. ¡Diviértete con la inmersión sin censura!",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (chatBgSeed == "Slate" || chatBgSeed == "Cyber") Color.LightGray else Color.Unspecified
                                    )
                                }
                            }
                        }
                    }
                }

                items(messages, key = { it.id }) { msg ->
                    ChatBubble(
                        message = msg,
                        char = activeChar,
                        isDarkBg = chatBgSeed == "Slate" || chatBgSeed == "Cyber"
                    )
                }

                if (isTyping) {
                    item {
                        TypingIndicatorBubble(
                            char = activeChar,
                            isDarkBg = chatBgSeed == "Slate" || chatBgSeed == "Cyber"
                        )
                    }
                }
            }

            // Chat Input
            Surface(
                tonalElevation = 8.dp,
                color = if (chatBgSeed == "Slate" || chatBgSeed == "Cyber") Color(0xFF1E293B) else MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .navigationBarsPadding(),
                    verticalAlignment = Alignment.Bottom
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = { Text("Escribe o usa *asteriscos*...", color = if (chatBgSeed == "Slate" || chatBgSeed == "Cyber") Color.LightGray else Color.Unspecified) },
                        maxLines = 4,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("chat_text_input"),
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = onBgColor,
                            unfocusedTextColor = onBgColor,
                            focusedContainerColor = if (chatBgSeed == "Slate" || chatBgSeed == "Cyber") Color(0xFF0F172A) else Color.Transparent,
                            unfocusedContainerColor = if (chatBgSeed == "Slate" || chatBgSeed == "Cyber") Color(0xFF0F172A) else Color.Transparent
                        )
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                            .clickable(enabled = inputText.isNotBlank() && !isTyping) {
                                viewModel.sendChatMessage(inputText)
                                inputText = ""
                            }
                            .testTag("chat_send_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Send,
                            contentDescription = "Enviar",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ChatBubble(
    message: ChatMessage,
    char: CharacterProfile,
    isDarkBg: Boolean
) {
    val isUser = message.sender == "user"
    val alignment = if (isUser) Alignment.End else Alignment.Start

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalAlignment = alignment
    ) {
        Row(
            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
            verticalAlignment = Alignment.Top,
            modifier = Modifier.fillMaxWidth(0.85f)
        ) {
            if (!isUser) {
                AsyncImage(
                    model = char.avatar,
                    contentDescription = null,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }

            Surface(
                shape = RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = if (isUser) 16.dp else 4.dp,
                    bottomEnd = if (isUser) 4.dp else 16.dp
                ),
                color = if (isUser) {
                    MaterialTheme.colorScheme.primary
                } else {
                    if (isDarkBg) Color(0xFF334155) else MaterialTheme.colorScheme.surfaceVariant
                },
                modifier = Modifier.padding(vertical = 2.dp)
            ) {
                Box(modifier = Modifier.padding(12.dp)) {
                    FormattedText(
                        text = message.text,
                        isUser = isUser,
                        isDarkBg = isDarkBg
                    )
                }
            }
        }
    }
}

@Composable
fun TypingIndicatorBubble(
    char: CharacterProfile,
    isDarkBg: Boolean
) {
    Row(
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth(0.8f)
            .padding(vertical = 4.dp)
    ) {
        AsyncImage(
            model = char.avatar,
            contentDescription = null,
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer)
        )
        Spacer(modifier = Modifier.width(8.dp))

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = if (isDarkBg) Color(0xFF334155) else MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.padding(vertical = 2.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val infiniteTransition = rememberInfiniteTransition()
                val dot1Alpha by infiniteTransition.animateFloat(
                    initialValue = 0.2f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(600, delayMillis = 0),
                        repeatMode = RepeatMode.Reverse
                    )
                )
                val dot2Alpha by infiniteTransition.animateFloat(
                    initialValue = 0.2f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(600, delayMillis = 200),
                        repeatMode = RepeatMode.Reverse
                    )
                )
                val dot3Alpha by infiniteTransition.animateFloat(
                    initialValue = 0.2f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(600, delayMillis = 400),
                        repeatMode = RepeatMode.Reverse
                    )
                )

                Box(modifier = Modifier.size(6.dp).alpha(dot1Alpha).background(if (isDarkBg) Color.White else Color.Gray, CircleShape))
                Box(modifier = Modifier.size(6.dp).alpha(dot2Alpha).background(if (isDarkBg) Color.White else Color.Gray, CircleShape))
                Box(modifier = Modifier.size(6.dp).alpha(dot3Alpha).background(if (isDarkBg) Color.White else Color.Gray, CircleShape))
            }
        }
    }
}

@Composable
fun FormattedText(text: String, isUser: Boolean, isDarkBg: Boolean) {
    val parts = text.split("*")
    val annotatedString = buildAnnotatedString {
        parts.forEachIndexed { index, part ->
            if (index % 2 == 1) {
                withStyle(
                    style = SpanStyle(
                        fontStyle = FontStyle.Italic,
                        fontWeight = FontWeight.Light,
                        color = if (isUser) {
                            Color(0xFFE0E7FF)
                        } else {
                            if (isDarkBg) Color(0xFFCBD5E1) else MaterialTheme.colorScheme.primary
                        }
                    )
                ) {
                    append("*$part*")
                }
            } else {
                append(part)
            }
        }
    }

    Text(
        text = annotatedString,
        style = MaterialTheme.typography.bodyMedium,
        color = if (isUser) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            if (isDarkBg) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
        }
    )
}

@Composable
fun SocialScreen(viewModel: LuminaViewModel) {
    val globalMessages by viewModel.globalMessages.collectAsState()
    val listState = rememberLazyListState()
    var textInput by remember { mutableStateOf("") }

    LaunchedEffect(globalMessages.size) {
        if (globalMessages.isNotEmpty()) {
            listState.animateScrollToItem(globalMessages.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.Public,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = "Foro Global",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Comparte ideas, prompts y charla con otros creadores",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Global messages list
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .testTag("global_chat_list"),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(globalMessages, key = { it.id }) { msg ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = msg.senderName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = msg.text,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Chat Input
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = textInput,
                onValueChange = { textInput = it },
                placeholder = { Text("Escribe una idea...") },
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .testTag("global_text_input"),
                shape = RoundedCornerShape(24.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape)
                    .clickable(enabled = textInput.isNotBlank()) {
                        viewModel.sendGlobalMessage(textInput)
                        textInput = ""
                    }
                    .testTag("global_send_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Send,
                    contentDescription = "Enviar",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun CreateScreen(
    onCreate: (String, String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }

    val avatarSeeds = listOf("Bakugou", "Sherlock", "Elena", "Mia", "Rocky", "Felix", "Princess", "Panda", "Cosmic", "Ghost")
    var selectedAvatarSeed by remember { mutableStateOf(avatarSeeds.first()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Forjar Entidad",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Text(
            text = "Crea tu propio personaje de rol y dale una personalidad única",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Form Fields
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nombre del Personaje") },
                placeholder = { Text("Ej. Goku, Elena, Arthur...") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("create_char_name"),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = desc,
                onValueChange = { desc = it },
                label = { Text("Personalidad, escenario e instrucciones de Rol") },
                placeholder = { Text("Describe detalladamente cómo debe actuar, qué lenguaje usa, sus gestos favoritos y la situación inicial de encuentro...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .testTag("create_char_desc"),
                shape = RoundedCornerShape(12.dp)
            )

            // Avatar Seed selector
            Column {
                Text(
                    text = "Selecciona un Avatar",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = "https://api.dicebear.com/7.x/avataaars/svg?seed=$selectedAvatarSeed",
                        contentDescription = "Avatar de vista previa",
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer)
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(text = "Seed actual: $selectedAvatarSeed", style = MaterialTheme.typography.bodyMedium)
                        Button(
                            onClick = {
                                val currentIdx = avatarSeeds.indexOf(selectedAvatarSeed)
                                val nextIdx = (currentIdx + 1) % avatarSeeds.size
                                selectedAvatarSeed = avatarSeeds[nextIdx]
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
                        ) {
                            Text("Cambiar Estilo")
                        }
                    }
                }
            }
        }

        Button(
            onClick = {
                if (name.isNotBlank() && desc.isNotBlank()) {
                    onCreate(name, desc, selectedAvatarSeed)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("create_char_submit"),
            shape = RoundedCornerShape(16.dp),
            enabled = name.isNotBlank() && desc.isNotBlank()
        ) {
            Text(
                text = "Forjar Personaje",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
    }
}

@Composable
fun DropdownMenuSeparator() {
    Divider(
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f),
        thickness = 1.dp,
        modifier = Modifier.padding(vertical = 4.dp)
    )
}
