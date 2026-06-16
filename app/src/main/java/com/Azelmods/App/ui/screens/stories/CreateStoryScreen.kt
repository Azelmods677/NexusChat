package com.Azelmods.App.ui.screens.stories

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil3.compose.rememberAsyncImagePainter
import com.Azelmods.App.ui.components.DraggableText
import com.Azelmods.App.ui.components.DraggableEmoji
import com.Azelmods.App.ui.components.EmojiOverlay
import com.Azelmods.App.ui.components.PhotoAdjuster
import com.Azelmods.App.ui.theme.*
import com.Azelmods.App.utils.PermissionHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.draw.drawWithContent
import com.Azelmods.App.utils.VideoThumbnailExtractor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateStoryScreen(
    navController: NavController,
    viewModel: CreateStoryViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var caption by remember { mutableStateOf("") }
    var textOverlay by remember { mutableStateOf("") }
    var textPosition by remember { mutableStateOf(Offset(100f, 300f)) }
    var showTextDialog by remember { mutableStateOf(false) }
    var showDrawMode by remember { mutableStateOf(false) }
    var showStickerPicker by remember { mutableStateOf(false) }
    var selectedSticker by remember { mutableStateOf("") }
    var emojiOverlays by remember { mutableStateOf<List<EmojiOverlay>>(emptyList()) }
    var selectedMusicUri by remember { mutableStateOf<Uri?>(null) }
    var selectedMusicName by remember { mutableStateOf("") }
    var showPhotoAdjuster by remember { mutableStateOf(false) }
    var photoVerticalPosition by remember { mutableStateOf(0f) }
    var videoThumbnail by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var isVideoSelected by remember { mutableStateOf(false) }

    // GraphicsLayer used to capture the editable preview area (media + text/sticker/emoji
    // overlays) into a Bitmap so the overlays are RENDERED into the published file.
    val captureLayer = rememberGraphicsLayer()
    val captureScope = rememberCoroutineScope()
    
    // Show error toast
    LaunchedEffect(state.error) {
        state.error?.let { error ->
            android.widget.Toast.makeText(context, error, android.widget.Toast.LENGTH_SHORT).show()
            viewModel.clearError()
        }
    }
    
    // Show success and navigate back
    LaunchedEffect(state.uploadSuccess) {
        if (state.uploadSuccess) {
            android.widget.Toast.makeText(context, "Story uploaded!", android.widget.Toast.LENGTH_SHORT).show()
            viewModel.clearSuccess()
            navController.popBackStack()
        }
    }
    
    // Media permission launcher
    val mediaPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            // Permissions granted, can proceed
        }
    }
    
    // Image picker launcher with error handling
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        try {
            selectedImageUri = uri
            isVideoSelected = false
            videoThumbnail = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    // Video picker launcher with error handling and thumbnail extraction
    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        try {
            selectedImageUri = uri
            if (uri != null) {
                isVideoSelected = true
                // Extract video thumbnail in background
                videoThumbnail = VideoThumbnailExtractor.extractThumbnail(context, uri)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            isVideoSelected = false
            videoThumbnail = null
        }
    }

    // Music picker launcher â€” lets the user attach an audio track to the story.
    val musicPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedMusicUri = uri
            // Resolve a friendly file name for the picked audio.
            val name = try {
                context.contentResolver.query(uri, null, null, null, null)?.use { c ->
                    val idx = c.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0 && c.moveToFirst()) c.getString(idx) else null
                }
            } catch (e: Exception) { null }
            selectedMusicName = name ?: "Audio seleccionado"
            android.widget.Toast.makeText(
                context,
                "ðŸŽµ $selectedMusicName",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    }
    
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .navigationBarsPadding(),
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Create Story",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.Default.Close, 
                            contentDescription = "Close",
                            modifier = Modifier.size(28.dp)
                        )
                    }
                },
                actions = {
                    if (selectedImageUri != null) {
                        // Animated share button
                        val infiniteTransition = rememberInfiniteTransition(label = "share_pulse")
                        val scale by infiniteTransition.animateFloat(
                            initialValue = 1f,
                            targetValue = 1.05f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1000),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "pulse"
                        )
                        
                        Surface(
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .scale(if (state.isUploading) 1f else scale),
                            shape = RoundedCornerShape(20.dp),
                            color = if (state.isUploading) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.primary,
                            onClick = {
                                if (!state.isUploading && selectedImageUri != null) {
                                    // Detect media type and upload accordingly
                                    when {
                                        selectedImageUri.toString() == "text_only" -> {
                                            // Text story
                                            viewModel.createTextStory(textOverlay, "#7C3AED")
                                        }
                                        isVideoSelected -> {
                                            // Video story: burn the text/sticker/emoji
                                            // overlays into the frames with Media3
                                            // Transformer. The capture layer records the
                                            // editable area (the video surface punches a
                                            // transparent hole, so we get an overlay-only
                                            // bitmap). If there are no overlays, or the
                                            // capture fails, we upload the original clip.
                                            val original = selectedImageUri ?: return@Surface
                                            val hasOverlays = textOverlay.isNotBlank() ||
                                                emojiOverlays.isNotEmpty() ||
                                                selectedSticker.isNotBlank()
                                            if (!hasOverlays) {
                                                viewModel.uploadVideoStory(original, caption)
                                            } else {
                                                captureScope.launch {
                                                    try {
                                                        val overlay = captureLayer
                                                            .toImageBitmap()
                                                            .asAndroidBitmap()
                                                        viewModel.uploadComposedVideoStory(
                                                            overlayBitmap = overlay,
                                                            caption = caption,
                                                            originalUri = original
                                                        )
                                                    } catch (e: Exception) {
                                                        e.printStackTrace()
                                                        // Fallback: upload the untouched video.
                                                        viewModel.uploadVideoStory(original, caption)
                                                    }
                                                }
                                            }
                                        }
                                        else -> {
                                            // Image story: capture the editable preview area
                                            // (photo + overlays) to a Bitmap and upload THAT,
                                            // so text/stickers/emojis are rendered into the file.
                                            val original = selectedImageUri ?: return@Surface
                                            captureScope.launch {
                                                try {
                                                    val composed = captureLayer
                                                        .toImageBitmap()
                                                        .asAndroidBitmap()
                                                    viewModel.uploadComposedImageStory(
                                                        composedBitmap = composed,
                                                        caption = caption,
                                                        originalUri = original
                                                    )
                                                } catch (e: Exception) {
                                                    e.printStackTrace()
                                                    // Fallback: upload the untouched image.
                                                    viewModel.uploadImageStory(original, caption)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (state.isUploading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        color = Color.White,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(
                                        Icons.AutoMirrored.Filled.Send,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    if (state.isUploading) "Uploading..." else "Share",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1A1A2E),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        containerColor = Color(0xFF0F0F1A)
    ) { paddingValues ->
        if (selectedImageUri == null) {
            // Media selection with modern design
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Animated icon
                    val infiniteTransition = rememberInfiniteTransition(label = "icon_pulse")
                    val iconScale by infiniteTransition.animateFloat(
                        initialValue = 1f,
                        targetValue = 1.1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1500),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "scale"
                    )
                    
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .scale(iconScale)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.primary,
                                        MaterialTheme.colorScheme.secondary
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.AddPhotoAlternate,
                            contentDescription = null,
                            modifier = Modifier.size(60.dp),
                            tint = Color.White
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    Text(
                        text = "Create Your Story",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Text(
                        text = "Share a moment that disappears after 24 hours",
                        fontSize = 15.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(48.dp))
                    
                    // Modern glassmorphism buttons
                    StoryCreationButton(
                        icon = Icons.Default.Photo,
                        text = "Choose Photo",
                        gradient = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary),
                        onClick = { 
                            if (PermissionHelper.hasMediaPermissions(context)) {
                                imagePickerLauncher.launch("image/*")
                            } else {
                                mediaPermissionLauncher.launch(PermissionHelper.mediaPermissions)
                            }
                        }
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    StoryCreationButton(
                        icon = Icons.Default.Videocam,
                        text = "Choose Video",
                        gradient = listOf(Color(0xFFFF6B9D), Color(0xFFFF8E53)),
                        onClick = { 
                            if (PermissionHelper.hasMediaPermissions(context)) {
                                videoPickerLauncher.launch("video/*")
                            } else {
                                mediaPermissionLauncher.launch(PermissionHelper.mediaPermissions)
                            }
                        }
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    StoryCreationButton(
                        icon = Icons.Default.TextFields,
                        text = "Text Story",
                        gradient = listOf(Color(0xFF00BFA6), Color(0xFF00D9FF)),
                        onClick = { 
                            selectedImageUri = Uri.parse("text_only")
                            showTextDialog = true
                        }
                    )
                }
            }
        } else {
            // Preview with modern overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Background
                if (selectedImageUri.toString() == "text_only") {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black) // Fondo negro como pediste
                    ) {
                        // Draggable text overlay - can be moved anywhere
                        if (textOverlay.isNotEmpty()) {
                            DraggableText(
                                text = textOverlay,
                                initialOffset = textPosition,
                                onPositionChange = { newPosition ->
                                    textPosition = newPosition
                                },
                                modifier = Modifier.fillMaxSize(),
                                backgroundColor = Color.Transparent // Sin fondo para que se vea el negro
                            )
                        } else {
                            // Placeholder text in center
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Tap 'Text' to add your message",
                                    fontSize = 18.sp,
                                    color = Color.White.copy(alpha = 0.6f),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                        
                        // Draggable emoji overlays on black background
                        emojiOverlays.forEach { emojiOverlay ->
                            DraggableEmoji(
                                emoji = emojiOverlay.emoji,
                                initialOffset = Offset(emojiOverlay.x, emojiOverlay.y),
                                size = emojiOverlay.size,
                                onPositionChange = { newPosition ->
                                    emojiOverlays = emojiOverlays.map {
                                        if (it.id == emojiOverlay.id) {
                                            it.copy(x = newPosition.x, y = newPosition.y)
                                        } else {
                                            it
                                        }
                                    }
                                },
                                onRemove = {
                                    emojiOverlays = emojiOverlays.filter { it.id != emojiOverlay.id }
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black)
                            .drawWithContent {
                                // Record this subtree (media + overlays) into the layer so it
                                // can be exported to a Bitmap on publish, then draw it normally.
                                captureLayer.record { this@drawWithContent.drawContent() }
                                drawContent()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        // Show video preview with ExoPlayer if video selected, otherwise show image
                        if (isVideoSelected && selectedImageUri != null) {
                            // Video preview using ExoPlayer
                            val previewPlayer = remember(selectedImageUri) {
                                androidx.media3.exoplayer.ExoPlayer.Builder(context).build().apply {
                                    setMediaItem(androidx.media3.common.MediaItem.fromUri(selectedImageUri ?: return@apply))
                                    prepare()
                                    playWhenReady = false // paused preview
                                    repeatMode = androidx.media3.common.Player.REPEAT_MODE_ONE
                                }
                            }
                            
                            DisposableEffect(selectedImageUri) {
                                onDispose { previewPlayer.release() }
                            }
                            
                            androidx.compose.ui.viewinterop.AndroidView(
                                factory = { ctx ->
                                    androidx.media3.ui.PlayerView(ctx).apply {
                                        player = previewPlayer
                                        useController = true
                                        resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer {
                                        translationY = photoVerticalPosition
                                    }
                            )
                        } else {
                            Image(
                                painter = rememberAsyncImagePainter(selectedImageUri),
                                contentDescription = "Selected media",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer {
                                        translationY = photoVerticalPosition
                                    },
                                contentScale = ContentScale.Crop
                            )
                        }
                        
                        // Draggable text overlay
                        if (textOverlay.isNotEmpty()) {
                            DraggableText(
                                text = textOverlay,
                                initialOffset = textPosition,
                                onPositionChange = { newPosition ->
                                    textPosition = newPosition
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        
                        // Draggable emoji overlays
                        emojiOverlays.forEach { emojiOverlay ->
                            DraggableEmoji(
                                emoji = emojiOverlay.emoji,
                                initialOffset = Offset(emojiOverlay.x, emojiOverlay.y),
                                size = emojiOverlay.size,
                                onPositionChange = { newPosition ->
                                    emojiOverlays = emojiOverlays.map {
                                        if (it.id == emojiOverlay.id) {
                                            it.copy(x = newPosition.x, y = newPosition.y)
                                        } else {
                                            it
                                        }
                                    }
                                },
                                onRemove = {
                                    emojiOverlays = emojiOverlays.filter { it.id != emojiOverlay.id }
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        
                        // Sticker overlay (deprecated - use emoji overlays instead)
                        if (selectedSticker.isNotEmpty()) {
                            Text(
                                text = selectedSticker,
                                fontSize = 64.sp,
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }
                    }
                }
                
                // Modern bottom toolbar with glassmorphism
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .imePadding(),
                    color = Color(0xFF1A1A2E).copy(alpha = 0.95f),
                    shadowElevation = 8.dp
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {
                        // Caption input with modern design
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .imePadding(),
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFF2D2D44)
                        ) {
                            BasicTextField(
                                value = caption,
                                onValueChange = { caption = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                textStyle = LocalTextStyle.current.copy(
                                    color = Color.White,
                                    fontSize = 15.sp
                                ),
                                decorationBox = { innerTextField ->
                                    if (caption.isEmpty()) {
                                        Text(
                                            "Add a caption...",
                                            color = Color.Gray,
                                            fontSize = 15.sp
                                        )
                                    }
                                    innerTextField()
                                }
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(20.dp))
                        
                        // Edit options with better design
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            ModernStoryEditOption(
                                icon = Icons.Default.CropRotate,
                                label = "Ajustar",
                                color = Color(0xFF10B981),
                                onClick = { showPhotoAdjuster = true }
                            )
                            ModernStoryEditOption(
                                icon = Icons.Default.Draw,
                                label = "Draw",
                                color = Color(0xFFFF6B9D),
                                onClick = { showDrawMode = true }
                            )
                            ModernStoryEditOption(
                                icon = Icons.Default.TextFields,
                                label = "Text",
                                color = MaterialTheme.colorScheme.primary,
                                onClick = { showTextDialog = true }
                            )
                            ModernStoryEditOption(
                                icon = Icons.Default.EmojiEmotions,
                                label = "Sticker",
                                color = Color(0xFFFFB020),
                                onClick = { showStickerPicker = true }
                            )
                            ModernStoryEditOption(
                                icon = Icons.Default.MusicNote,
                                label = if (selectedMusicName.isNotBlank()) "MÃºsica âœ“" else "Music",
                                color = Color(0xFF00BFA6),
                                onClick = {
                                    musicPickerLauncher.launch("audio/*")
                                }
                            )
                        }
                    }
                }
            }
        }
    }
    
    // Modern text dialog with proper keyboard handling for Android 16
    if (showTextDialog) {
        var dialogText by remember { mutableStateOf(textOverlay) }
        
        AlertDialog(
            onDismissRequest = { showTextDialog = false },
            title = { 
                Text(
                    "Add Text",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                ) 
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .imePadding() // Critical for Android 16 keyboard handling
                ) {
                    OutlinedTextField(
                        value = dialogText,
                        onValueChange = { dialogText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 120.dp),
                        placeholder = { 
                            Text(
                                "Enter your text...",
                                color = Color.Gray
                            ) 
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = Color.Gray.copy(alpha = 0.3f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            cursorColor = MaterialTheme.colorScheme.primary
                        ),
                        textStyle = LocalTextStyle.current.copy(
                            fontSize = 16.sp
                        ),
                        maxLines = 5
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { 
                        textOverlay = dialogText
                        showTextDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("Done", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { 
                        dialogText = ""
                        textOverlay = ""
                        showTextDialog = false
                    }
                ) {
                    Text("Clear", color = Color.Gray)
                }
            },
            containerColor = Color(0xFF1A1A2E),
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false // Critical for Android 16
            ),
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .wrapContentHeight()
        )
    }
    
    // Sticker picker dialog - FULL EMOJI PICKER
    if (showStickerPicker) {
        var selectedCategory by remember { mutableStateOf("Caritas") }
        val emojiCategories = mapOf(
            "Caritas" to listOf("ðŸ˜€","ðŸ˜‚","ðŸ˜","ðŸ˜Ž","ðŸ˜­","ðŸ˜…","ðŸ¤£","ðŸ˜Š","ðŸ˜‡","ðŸ¥°","ðŸ˜˜","ðŸ˜œ","ðŸ¤”","ðŸ˜","ðŸ˜’","ðŸ˜¡","ðŸ¤¯","ðŸ¥³","ðŸ˜´","ðŸ¤®","ðŸ¤’","ðŸ‘»","ðŸ’€","ðŸ¤–","ðŸ‘½","ðŸ˜º","ðŸ˜¸"),
            "Animales" to listOf("ðŸ¶","ðŸ±","ðŸ­","ðŸ¹","ðŸ°","ðŸ¦Š","ðŸ»","ðŸ¼","ðŸ¨","ðŸ¯","ðŸ¦","ðŸ®","ðŸ·","ðŸ¸","ðŸµ","ðŸ”","ðŸ§","ðŸ¦†","ðŸ¦…","ðŸ¦‰","ðŸ¦‡","ðŸº","ðŸ—","ðŸ´","ðŸ¦„","ðŸ","ðŸ¦‹","ðŸŒ","ðŸž","ðŸ¢","ðŸ","ðŸ¦Ž","ðŸ¦–","ðŸ¦•","ðŸ™","ðŸ¦‘","ðŸ¦","ðŸ¦ž","ðŸ¦€","ðŸ¡","ðŸ ","ðŸŸ","ðŸ¬","ðŸ³","ðŸ‹","ðŸ¦ˆ"),
            "Comida" to listOf("ðŸŽ","ðŸŠ","ðŸ‹","ðŸ‡","ðŸ“","ðŸ’","ðŸ‘","ðŸ¥­","ðŸ","ðŸ¥¥","ðŸ¥","ðŸ…","ðŸ¥‘","ðŸ†","ðŸ¥”","ðŸ¥•","ðŸŒ½","ðŸŒ¶ï¸","ðŸ¥’","ðŸ¥¬","ðŸ¥¦","ðŸ„","ðŸ¥œ","ðŸŒ°","ðŸž","ðŸ¥","ðŸ¥–","ðŸ¥¨","ðŸ¥¯","ðŸ¥ž","ðŸ§‡","ðŸ§€","ðŸ–","ðŸ—","ðŸ¥©","ðŸ¥“","ðŸ”","ðŸŸ","ðŸ•","ðŸŒ­","ðŸ¥ª","ðŸŒ®","ðŸŒ¯","ðŸ¥™","ðŸ§†","ðŸ¥š","ðŸ³","ðŸ¥˜","ðŸ²","ðŸ¥£","ðŸ¥—","ðŸ¿","ðŸ§ˆ","ðŸ§‚","ðŸ¥«","ðŸ±","ðŸ˜","ðŸ™","ðŸš","ðŸ›","ðŸœ","ðŸ","ðŸ ","ðŸ¢","ðŸ£","ðŸ¤","ðŸ¥","ðŸ¥®","ðŸ¡","ðŸ¥Ÿ","ðŸ¥ ","ðŸ¥¡","ðŸ¦€","ðŸ¦ž","ðŸ¦","ðŸ¦‘","ðŸ¦ª","ðŸ¦","ðŸ§","ðŸ¨","ðŸ©","ðŸª","ðŸŽ‚","ðŸ°","ðŸ§","ðŸ¥§","ðŸ«","ðŸ¬","ðŸ­","ðŸ®","ðŸ¯","ðŸ¼","ðŸ¥›","â˜•","ðŸµ","ðŸ¶","ðŸ¾","ðŸ·","ðŸ¸","ðŸ¹","ðŸº","ðŸ»","ðŸ¥‚","ðŸ¥ƒ","ðŸ¥¤","ðŸ§ƒ","ðŸ§‰","ðŸ§Š"),
            "Deportes" to listOf("âš½","ðŸ€","ðŸˆ","âš¾","ðŸ¥Ž","ðŸŽ¾","ðŸ","ðŸ‰","ðŸ¥","ðŸŽ±","ðŸª€","ðŸ“","ðŸ¸","ðŸ’","ðŸ‘","ðŸ¥","ðŸ","ðŸ¥…","â›³","ðŸª","ðŸ¹","ðŸŽ£","ðŸ¤¿","ðŸ¥Š","ðŸ¥‹","ðŸŽ½","ðŸ›¹","ðŸ›¼","ðŸ›·","â›¸ï¸","ðŸ¥Œ","ðŸŽ¿","â›·ï¸","ðŸ‚","ðŸª‚","ðŸ‹ï¸","ðŸ¤¼","ðŸ¤¸","ðŸ¤º","ðŸ¤¾","ðŸŒï¸","ðŸ‡","ðŸ§˜","ðŸ„","ðŸŠ","ðŸ¤½","ðŸš£","ðŸ§—","ðŸšµ","ðŸš´","ðŸ†","ðŸ¥‡","ðŸ¥ˆ","ðŸ¥‰","ðŸ…","ðŸŽ–ï¸","ðŸŽ—ï¸","ðŸŽ«","ðŸŽŸï¸","ðŸŽª","ðŸŽ­","ðŸŽ¨","ðŸŽ¬","ðŸŽ¤","ðŸŽ§","ðŸŽ¼","ðŸŽ¹","ðŸ¥","ðŸŽ·","ðŸŽº","ðŸŽ¸","ðŸª•","ðŸŽ»","ðŸŽ²","â™Ÿï¸","ðŸŽ¯","ðŸŽ®","ðŸ•¹ï¸","ðŸŽ°","ðŸ§©")
        )
        
        ModalBottomSheet(
            onDismissRequest = { showStickerPicker = false },
            containerColor = Color(0xFF1A1A2E),
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp)
            ) {
                // Category tabs
                ScrollableTabRow(
                    selectedTabIndex = emojiCategories.keys.indexOf(selectedCategory),
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.primary,
                    edgePadding = 8.dp
                ) {
                    emojiCategories.keys.forEach { cat ->
                        Tab(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            text = { 
                                Text(
                                    cat, 
                                    fontSize = 12.sp,
                                    fontWeight = if (selectedCategory == cat) FontWeight.Bold else FontWeight.Normal
                                ) 
                            }
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Emoji grid
                androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                    columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(8),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val emojis = emojiCategories[selectedCategory] ?: emptyList()
                    items(emojis.size) { index ->
                        val emoji = emojis[index]
                        Text(
                            text = emoji,
                            fontSize = 28.sp,
                            modifier = Modifier
                                .padding(4.dp)
                                .clickable {
                                    // Add emoji as draggable overlay
                                    emojiOverlays = emojiOverlays + EmojiOverlay(
                                        emoji = emoji,
                                        x = 150f,
                                        y = 300f,
                                        size = 64
                                    )
                                    showStickerPicker = false
                                },
                            textAlign = TextAlign.Center
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Tip: MantÃ©n presionado un emoji para eliminarlo",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                )
            }
        }
    }
    
    // Draw mode â€” functional full-screen drawing canvas
    if (showDrawMode) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { showDrawMode = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            val strokes = remember { mutableStateListOf<DrawnStroke>() }
            var current by remember { mutableStateOf<DrawnStroke?>(null) }
            var color by remember { mutableStateOf(Color(0xFFFF6B9D)) }
            val palette = listOf(
                Color.White, Color.Black, Color(0xFF7C6FE0), Color(0xFFFF6B9D),
                Color(0xFF00D4FF), Color(0xFF00E676), Color(0xFFFFD700), Color(0xFFFF5252)
            )

            Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0D0D1E))) {
                androidx.compose.foundation.Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(color) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    current = DrawnStroke(mutableStateListOf(offset), color, 10f)
                                },
                                onDrag = { change, _ ->
                                    current?.points?.add(change.position)
                                    // Force recomposition by reassigning
                                    current = current?.copy()
                                },
                                onDragEnd = {
                                    current?.let { strokes.add(it) }
                                    current = null
                                }
                            )
                        }
                ) {
                    (strokes + listOfNotNull(current)).forEach { stroke ->
                        val pts = stroke.points
                        for (i in 1 until pts.size) {
                            drawLine(
                                color = stroke.color,
                                start = pts[i - 1],
                                end = pts[i],
                                strokeWidth = stroke.width,
                                cap = androidx.compose.ui.graphics.StrokeCap.Round
                            )
                        }
                    }
                }

                // Top bar: close + undo + clear
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { showDrawMode = false }) {
                        Icon(Icons.Default.Close, "Cerrar", tint = Color.White)
                    }
                    Row {
                        IconButton(onClick = { if (strokes.isNotEmpty()) strokes.removeAt(strokes.size - 1) }) {
                            Icon(Icons.Default.Undo, "Deshacer", tint = Color.White)
                        }
                        IconButton(onClick = { strokes.clear() }) {
                            Icon(Icons.Default.Delete, "Limpiar", tint = Color(0xFFFF5252))
                        }
                        TextButton(onClick = { showDrawMode = false }) {
                            Text("Listo", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Color palette at bottom
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .background(Color(0xFF141428))
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    palette.forEach { c ->
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(c)
                                .border(
                                    if (color == c) 3.dp else 1.dp,
                                    if (color == c) Color.White else Color.Transparent,
                                    CircleShape
                                )
                                .clickable { color = c }
                        )
                    }
                }
            }
        }
    }
    
    // Photo adjuster overlay
    if (showPhotoAdjuster && selectedImageUri != null && selectedImageUri.toString() != "text_only") {
        PhotoAdjuster(
            imageUri = selectedImageUri,
            onPositionChange = { newPosition ->
                photoVerticalPosition = newPosition
            },
            onConfirm = {
                showPhotoAdjuster = false
                android.widget.Toast.makeText(
                    context,
                    "PosiciÃ³n ajustada",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            },
            onCancel = {
                showPhotoAdjuster = false
                photoVerticalPosition = 0f
            },
            initialPosition = photoVerticalPosition,
            title = "Ajustar Foto de Story"
        )
    }
}

@Composable
fun StoryCreationButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    gradient: List<Color>,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp),
        shape = RoundedCornerShape(16.dp),
        color = Color.Transparent,
        onClick = onClick
    ) {
        Box(
            modifier = Modifier.background(
                Brush.linearGradient(gradient)
            ),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text,
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun ModernStoryEditOption(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Surface(
            modifier = Modifier.size(56.dp),
            shape = RoundedCornerShape(16.dp),
            color = color.copy(alpha = 0.15f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    icon,
                    contentDescription = label,
                    tint = color,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color.White,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun StoryEditOption(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(8.dp)
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
        ) {
            Icon(
                icon,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color.White
        )
    }
}

/**
 * A single freehand stroke drawn on the story canvas.
 * [points] are screen-space positions; [color] and [width] define its style.
 */
data class DrawnStroke(
    val points: MutableList<Offset>,
    val color: androidx.compose.ui.graphics.Color,
    val width: Float
)
