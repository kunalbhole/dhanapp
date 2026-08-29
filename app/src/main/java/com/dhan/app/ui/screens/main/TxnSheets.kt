package com.dhan.app.ui.screens.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dhan.app.data.db.BillEntity
import com.dhan.app.data.db.BillStatus
import com.dhan.app.data.db.TransactionEntity
import com.dhan.app.data.db.TxnSource
import com.dhan.app.data.repo.LocalRepository
import com.dhan.app.ui.components.DhanBottomSheet
import com.dhan.app.ui.components.DhanButton
import com.dhan.app.ui.components.DhanButtonVariant
import com.dhan.app.ui.components.DhanField
import com.dhan.app.ui.components.CategoryIcon
import com.dhan.app.ui.components.TxnCategory
import com.dhan.app.ui.theme.DhanColor
import com.dhan.app.ui.theme.Poppins
import kotlinx.coroutines.launch

/** Expense/Income toggle, amount, category grid, note — ported from the design brief
 *  described in chats/chat1.md (app.jsx's AddTxnSheet; the JSX source for this sheet
 *  wasn't present in the handoff bundle's screens-*.jsx files, so this is a from-spec
 *  rebuild wired to the real Room-backed transaction store). */
@Composable
fun AddTxnSheet(open: Boolean, onClose: () -> Unit, defaultIsIncome: Boolean = false) {
    val repo = LocalRepository.current
    val scope = rememberCoroutineScope()
    var isIncome by remember(open) { mutableStateOf(defaultIsIncome) }
    var amountText by remember(open) { mutableStateOf("") }
    var note by remember(open) { mutableStateOf("") }
    var category by remember(open) { mutableStateOf(TxnCategory.OTHER) }

    DhanBottomSheet(open = open, onClose = onClose, title = "Add transaction") {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(DhanColor.bgSurface, RoundedCornerShape(12.dp))
                .padding(3.dp),
        ) {
            listOf(false to "Expense", true to "Income").forEach { (value, label) ->
                val selected = isIncome == value
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { isIncome = value }
                        .background(if (selected) DhanColor.bgElevated else androidx.compose.ui.graphics.Color.Transparent, RoundedCornerShape(9.dp))
                        .padding(vertical = 9.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(label, fontFamily = Poppins, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = if (selected) DhanColor.navy else DhanColor.fg3)
                }
            }
        }

        Box(Modifier.height(16.dp))

        DhanField(
            value = amountText,
            onValueChange = { new -> amountText = new.filter { it.isDigit() || it == '.' } },
            label = "Amount",
            placeholder = "0",
            prefix = "₹",
            keyboardType = KeyboardType.Decimal,
        )

        if (!isIncome) {
            Text("Category", fontFamily = Poppins, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = DhanColor.fg2, modifier = Modifier.padding(bottom = 8.dp))
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth().height(160.dp),
            ) {
                items(TxnCategory.entries.filter { it != TxnCategory.INCOME }) { cat ->
                    val selected = category == cat
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clickable { category = cat }
                            .background(if (selected) DhanColor.navy05 else DhanColor.bgSurface, RoundedCornerShape(12.dp))
                            .padding(6.dp),
                        verticalArrangement = Arrangement.Center,
                    ) {
                        CategoryIcon(cat, size = 28.dp, tint = true)
                        Text(cat.displayName, fontFamily = Poppins, fontWeight = FontWeight.SemiBold, fontSize = 10.sp, color = DhanColor.fg2, maxLines = 1)
                    }
                }
            }
            Box(Modifier.height(8.dp))
        }

        DhanField(value = note, onValueChange = { note = it }, label = "Note (optional)", placeholder = "Add a note")

        Box(Modifier.height(4.dp))

        DhanButton(
            text = if (isIncome) "Save income" else "Save expense",
            full = true,
            onClick = {
                val amount = amountText.toDoubleOrNull() ?: return@DhanButton
                val signed = if (isIncome) amount else -amount
                val resolvedCategory = if (isIncome) "income" else category.name.lowercase()
                val merchant = note.ifBlank { if (isIncome) "Income" else category.displayName }
                scope.launch {
                    repo.addTransaction(
                        TransactionEntity(
                            merchant = merchant,
                            note = note.ifBlank { null },
                            amountRupees = signed,
                            category = resolvedCategory,
                            timestampMillis = System.currentTimeMillis(),
                            source = TxnSource.MANUAL,
                        ),
                    )
                    onClose()
                }
            },
        )
    }
}

/** Add-bill sheet — name, amount, due-in-days, repeat-monthly toggle. */
@Composable
fun AddBillSheet(open: Boolean, onClose: () -> Unit) {
    val repo = LocalRepository.current
    val scope = rememberCoroutineScope()
    var name by remember(open) { mutableStateOf("") }
    var amountText by remember(open) { mutableStateOf("") }
    var dueInDaysText by remember(open) { mutableStateOf("7") }
    var repeatMonthly by remember(open) { mutableStateOf(true) }

    DhanBottomSheet(open = open, onClose = onClose, title = "Add bill") {
        DhanField(value = name, onValueChange = { name = it }, label = "Bill name", placeholder = "e.g. Netflix")
        DhanField(
            value = amountText,
            onValueChange = { new -> amountText = new.filter { it.isDigit() || it == '.' } },
            label = "Amount",
            placeholder = "0",
            prefix = "₹",
            keyboardType = KeyboardType.Decimal,
        )
        DhanField(
            value = dueInDaysText,
            onValueChange = { new -> dueInDaysText = new.filter { it.isDigit() } },
            label = "Due in (days)",
            placeholder = "7",
            keyboardType = KeyboardType.Number,
        )
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 16.dp)) {
            Text("Repeats monthly", fontFamily = Poppins, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = DhanColor.fg2, modifier = Modifier.weight(1f))
            com.dhan.app.ui.components.DhanChip(
                text = if (repeatMonthly) "On" else "Off",
                active = repeatMonthly,
                onClick = { repeatMonthly = !repeatMonthly },
            )
        }
        DhanButton(
            text = "Save bill",
            full = true,
            onClick = {
                val amount = amountText.toDoubleOrNull() ?: return@DhanButton
                val dueInDays = dueInDaysText.toIntOrNull() ?: 0
                val dueMillis = System.currentTimeMillis() + dueInDays * 24L * 60 * 60 * 1000
                val status = if (dueInDays <= 3) BillStatus.DUE_SOON else BillStatus.UPCOMING
                scope.launch {
                    repo.addBill(
                        BillEntity(
                            name = name.ifBlank { "Bill" },
                            amountRupees = amount,
                            dueDateMillis = dueMillis,
                            status = status,
                            repeatMonthly = repeatMonthly,
                        ),
                    )
                    onClose()
                }
            },
        )
    }
}
