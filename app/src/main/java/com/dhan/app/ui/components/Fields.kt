package com.dhan.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dhan.app.ui.theme.DhanColor
import com.dhan.app.ui.theme.DhanRadius
import com.dhan.app.ui.theme.Poppins

/** Ports components.jsx `Field` — labeled text input with optional prefix/trailing slot. */
@Composable
fun DhanField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    prefix: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    trailing: @Composable (() -> Unit)? = null,
    enabled: Boolean = true,
) {
    Column(modifier = Modifier.padding(bottom = 14.dp)) {
        if (label != null) {
            Text(
                label,
                fontFamily = Poppins,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
                color = DhanColor.fg2,
                modifier = Modifier.padding(bottom = 6.dp),
            )
        }
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            enabled = enabled,
            modifier = modifier.fillMaxWidth().height(52.dp),
            placeholder = placeholder?.let { { Text(it, color = DhanColor.fg3) } },
            leadingIcon = prefix?.let {
                {
                    Text(it, fontFamily = Poppins, fontWeight = FontWeight.SemiBold, color = DhanColor.fg2)
                }
            },
            trailingIcon = trailing,
            singleLine = true,
            shape = RoundedCornerShape(DhanRadius.input),
            textStyle = androidx.compose.ui.text.TextStyle(fontFamily = Poppins, fontWeight = FontWeight.Medium, fontSize = 15.sp),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = DhanColor.navy,
                unfocusedBorderColor = DhanColor.borderDefault,
                focusedContainerColor = androidx.compose.ui.graphics.Color.White,
                unfocusedContainerColor = androidx.compose.ui.graphics.Color.White,
            ),
        )
    }
}
