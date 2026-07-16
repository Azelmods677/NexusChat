package com.Azelmods.App.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.Azelmods.App.ui.theme.Pink
import com.Azelmods.App.ui.theme.Purple
import com.Azelmods.App.ui.theme.Teal
import com.Azelmods.App.ui.theme.ErrorRed
import com.Azelmods.App.ui.theme.DarkSurface
import com.Azelmods.App.ui.theme.Info
import com.Azelmods.App.ui.theme.Success

enum class AttachmentType {
    GALLERY,
    CAMERA,
    VIDEO,
    DOCUMENT,
    AUDIO,
    LOCATION,
    CONTACT
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttachmentBottomSheet(
    onAttachmentSelected: (AttachmentType) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        color = DarkSurface,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            // Handle bar
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.Gray.copy(alpha = 0.3f))
                    .align(Alignment.CenterHorizontally)
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "Adjuntar archivo",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Grid of attachment options
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                AttachmentOption(
                    icon = Icons.Default.PhotoLibrary,
                    label = "Galería",
                    color = Purple,
                    onClick = {
                        onAttachmentSelected(AttachmentType.GALLERY)
                        onDismiss()
                    }
                )
                
                AttachmentOption(
                    icon = Icons.Default.CameraAlt,
                    label = "Cámara",
                    color = Pink,
                    onClick = {
                        onAttachmentSelected(AttachmentType.CAMERA)
                        onDismiss()
                    }
                )
                
                AttachmentOption(
                    icon = Icons.Default.Description,
                    label = "Documento",
                    color = Teal,
                    onClick = {
                        onAttachmentSelected(AttachmentType.DOCUMENT)
                        onDismiss()
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                AttachmentOption(
                    icon = Icons.Default.Videocam,
                    label = "Video",
                    color = ErrorRed,
                    onClick = {
                        onAttachmentSelected(AttachmentType.VIDEO)
                        onDismiss()
                    }
                )

                AttachmentOption(
                    icon = Icons.Default.Mic,
                    label = "Audio",
                    color = Color(0xFFFFB020),
                    onClick = {
                        onAttachmentSelected(AttachmentType.AUDIO)
                        onDismiss()
                    }
                )
                
                AttachmentOption(
                    icon = Icons.Default.LocationOn,
                    label = "Ubicación",
                    color = Success,
                    onClick = {
                        onAttachmentSelected(AttachmentType.LOCATION)
                        onDismiss()
                    }
                )
                
                AttachmentOption(
                    icon = Icons.Default.Person,
                    label = "Contacto",
                    color = Info,
                    onClick = {
                        onAttachmentSelected(AttachmentType.CONTACT)
                        onDismiss()
                    }
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun AttachmentOption(
    icon: ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = color,
                modifier = Modifier.size(32.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color.White,
            fontWeight = FontWeight.Medium
        )
    }
}
