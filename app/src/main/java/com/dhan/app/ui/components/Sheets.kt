package com.dhan.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dhan.app.ui.theme.DhanColor
import com.dhan.app.ui.theme.DhanIcon
import com.dhan.app.ui.theme.Poppins

/** Ports components.jsx `BottomSheet`. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DhanBottomSheet(
    open: Boolean,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    sheetState: SheetState = rememberModalBottomSheetState(),
    content: @Composable () -> Unit,
) {
    if (!open) return
    ModalBottomSheet(
        onDismissRequest = onClose,
        sheetState = sheetState,
        containerColor = DhanColor.bgElevated,
        modifier = modifier,
    ) {
        if (title != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(title, fontFamily = Poppins, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = DhanColor.fg1)
                IconButton(onClick = onClose) {
                    Icon(DhanIcon.of("x"), contentDescription = "Close", tint = DhanColor.fg1)
                }
            }
        }
        androidx.compose.foundation.layout.Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = if (title != null) 8.dp else 20.dp).padding(bottom = 28.dp),
        ) {
            content()
        }
    }
}
