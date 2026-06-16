package com.Azelmods.App.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil3.compose.AsyncImage
import coil3.request.ImageRequest

data class StickerPack(
    val id: String,
    val name: String,
    val icon: String,
    val stickers: List<String>
)

enum class StickerCategory(val label: String) {
    RECENT("Recientes"),
    FAVORITES("Favoritos"),
    PACK1("Pack 1"),
    PACK2("Pack 2"),
    PACK3("Pack 3")
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun StickerPicker(
    onStickerSelected: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedCategory by remember { mutableStateOf(StickerCategory.PACK1) }
    var recentStickers by remember { mutableStateOf(listOf<String>()) }
    var favoriteStickers by remember { mutableStateOf(listOf<String>()) }
    var previewSticker by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    // Demo stickers (en producción, cargar desde Firebase Storage)
    val stickerPacks = remember {
        mapOf(
            StickerCategory.RECENT to recentStickers,
            StickerCategory.FAVORITES to favoriteStickers,
            StickerCategory.PACK1 to listOf(
                "😀", "😃", "😄", "😁", "😆", "😅", "🤣", "😂",
                "🙂", "🙃", "😉", "😊", "😇", "🥰", "😍", "🤩"
            ),
            StickerCategory.PACK2 to listOf(
                "🐶", "🐱", "🐭", "🐹", "🐰", "🦊", "🐻", "🐼",
                "🐨", "🐯", "🦁", "🐮", "🐷", "🐽", "🐸", "🐵"
            ),
            StickerCategory.PACK3 to listOf(
                "❤️", "🧡", "💛", "💚", "💙", "💜", "🖤", "🤍",
                "🤎", "💔", "❤️‍🔥", "❤️‍🩹", "💕", "💞", "💓", "💗"
            )
        )
    }

    val currentStickers = stickerPacks[selectedCategory] ?: emptyList()

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(400.dp),
        color = Color(0xFF1A1A2E),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Stickers",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                }
            }

            // Category tabs
            ScrollableTabRow(
                selectedTabIndex = StickerCategory.values().indexOf(selectedCategory),
                containerColor = Color.Transparent,
                contentColor = Color.White,
                edgePadding = 8.dp
            ) {
                StickerCategory.values().forEach { category ->
                    Tab(
                        selected = selectedCategory == category,
                        onClick = { selectedCategory = category },
                        text = {
                            Text(
                                text = category.label,
                                fontSize = 14.sp,
                                fontWeight = if (selectedCategory == category) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            HorizontalDivider(color = Color.Gray.copy(alpha = 0.2f))

            // Sticker grid
            if (currentStickers.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.EmojiEmotions,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = Color.Gray
                        )
                        Text(
                            text = when (selectedCategory) {
                                StickerCategory.RECENT -> "No hay stickers recientes"
                                StickerCategory.FAVORITES -> "No hay stickers favoritos"
                                else -> "Pack vacío"
                            },
                            color = Color.Gray,
                            fontSize = 14.sp
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(12.dp)
                ) {
                    items(currentStickers) { sticker ->
                        Text(
                            text = sticker,
                            fontSize = 48.sp,
                            modifier = Modifier
                                .padding(8.dp)
                                .combinedClickable(
                                    onClick = {
                                        // Add to recent
                                        recentStickers = (listOf(sticker) + recentStickers)
                                            .distinct()
                                            .take(16)
                                        onStickerSelected(sticker)
                                    },
                                    onLongClick = {
                                        previewSticker = sticker
                                    }
                                )
                                .padding(8.dp)
                        )
                    }
                }
            }
        }
    }

    // Preview dialog
    previewSticker?.let { sticker ->
        Dialog(onDismissRequest = { previewSticker = null }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF1A1A2E)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = sticker,
                        fontSize = 120.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Button(
                            onClick = {
                                favoriteStickers = if (favoriteStickers.contains(sticker)) {
                                    favoriteStickers - sticker
                                } else {
                                    favoriteStickers + sticker
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(
                                if (favoriteStickers.contains(sticker)) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Favorite"
                            )
                        }

                        Button(
                            onClick = {
                                onStickerSelected(sticker)
                                previewSticker = null
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Text("Enviar")
                        }
                    }
                }
            }
        }
    }
}
