package com.dhan.app.capture.parser

/**
 * Signed rupee amount (positive = money in, negative = money out) extracted from a bank
 * or UPI SMS/notification, plus best-effort merchant name, category guess, and account hint.
 */
data class ParsedTransaction(
    val amountRupees: Double,
    val merchant: String,
    val category: String,
    val accountHint: String?,
)

/**
 * Regex-based, offline transaction detector for common Indian bank & UPI app SMS and
 * notification text (SBI/HDFC/ICICI/Axis/Kotak-style bank alerts, GPay/PhonePe/Paytm
 * payment confirmations). This is a best-effort heuristic, not a bank-format parser —
 * unfamiliar phrasing will simply not match and the message is skipped rather than
 * guessed at. Tune [MERCHANT_CATEGORY_HINTS] and the regexes below as real message
 * samples surface false negatives/positives.
 */
object TransactionParser {

    private val amountRegex = Regex(
        """(?:rs\.?|inr|₹)\s?([0-9]{1,3}(?:,[0-9]{2,3})*(?:\.[0-9]{1,2})?|[0-9]+(?:\.[0-9]{1,2})?)""",
        RegexOption.IGNORE_CASE,
    )

    private val debitKeywords = listOf(
        "debited", "spent", "paid to", "you paid", "purchase of", "withdrawn",
        "txn of", "transaction of", "debit alert",
    )
    private val creditKeywords = listOf(
        "credited", "received", "deposited", "refunded", "refund of", "credit alert",
        "money sent to you", "you received",
    )

    // Promotional / non-transactional bank SMS that would otherwise false-positive on
    // an amount + "credited/debited"-adjacent word (loan offers, cashback teasers, OTPs).
    private val exclusionKeywords = listOf(
        "otp", "one time password", "do not share", "loan of", "pre-approved",
        "eligible for", "cashback offer", "click here", "download", "apply now",
        "will expire", "expires on", "limited period", "install", "t&c apply",
    )

    private val vpaRegex = Regex("""(?:to|from)\s+VPA\s+([\w.\-]+)@[\w.\-]+""", RegexOption.IGNORE_CASE)
    private val toMerchantRegex = Regex("""(?:paid to|to|at)\s+([A-Za-z0-9&.'\- ]{2,40}?)\s+(?:on|using|via|for|\.|,|$)""", RegexOption.IGNORE_CASE)
    private val fromPersonRegex = Regex("""(?:received from|from)\s+([A-Za-z0-9&.'\- ]{2,40}?)\s+(?:via|on|using|\.|,|$)""", RegexOption.IGNORE_CASE)
    private val accountRegex = Regex("""(?:a/?c(?:count)?|card)\D{0,10}([Xx*]{2,}\d{2,6}|\d{4,6})""", RegexOption.IGNORE_CASE)

    private val MERCHANT_CATEGORY_HINTS: List<Pair<Regex, String>> = listOf(
        Regex("swiggy|zomato|dominos|pizza|restaurant|cafe|starbucks|eatery", RegexOption.IGNORE_CASE) to "food",
        Regex("uber|ola|rapido|irctc|metro|petrol|fuel|indianoil|hpcl|bpcl", RegexOption.IGNORE_CASE) to "transport",
        Regex("amazon|flipkart|myntra|ajio|meesho|nykaa", RegexOption.IGNORE_CASE) to "shopping",
        Regex("electricity|water bill|broadband|airtel|jio|vodafone|vi\\b|gas bill|dth", RegexOption.IGNORE_CASE) to "bills",
        Regex("netflix|prime video|hotstar|spotify|bookmyshow|pvr|inox", RegexOption.IGNORE_CASE) to "ent",
        Regex("pharmacy|hospital|clinic|apollo|practo|medplus|diagnostic", RegexOption.IGNORE_CASE) to "health",
        Regex("byju|udemy|coursera|school|college|tuition", RegexOption.IGNORE_CASE) to "edu",
        Regex("bigbasket|blinkit|zepto|dmart|grocery|grofers|instamart", RegexOption.IGNORE_CASE) to "groceries",
        Regex("rent\\b|landlord", RegexOption.IGNORE_CASE) to "rent",
        Regex("indigo|spicejet|makemytrip|goibibo|airbnb|oyo|airline|flight", RegexOption.IGNORE_CASE) to "travel",
    )

    fun looksLikeTransaction(text: String): Boolean {
        val lower = text.lowercase()
        if (exclusionKeywords.any { lower.contains(it) }) return false
        if (!amountRegex.containsMatchIn(text)) return false
        return debitKeywords.any { lower.contains(it) } || creditKeywords.any { lower.contains(it) }
    }

    fun parse(text: String, sourceApp: String? = null): ParsedTransaction? {
        if (!looksLikeTransaction(text)) return null

        val amountMatch = amountRegex.find(text) ?: return null
        val amount = amountMatch.groupValues[1].replace(",", "").toDoubleOrNull() ?: return null
        if (amount <= 0.0) return null

        val lower = text.lowercase()
        val isDebit = debitKeywords.any { lower.contains(it) }
        val isCredit = creditKeywords.any { lower.contains(it) }
        // Ambiguous or unmatched direction defaults to expense: undercounting income is
        // safer for a budget app than inflating the user's apparent balance.
        val signedAmount = if (isCredit && !isDebit) amount else -amount

        val merchantRaw = extractMerchant(text, isCredit = signedAmount > 0) ?: sourceApp ?: "Transaction"
        val merchant = merchantRaw.trim().trim('.', ',').take(40).ifBlank { sourceApp ?: "Transaction" }
        val category = if (signedAmount > 0) "income" else inferCategory(merchant)
        val accountHint = accountRegex.find(text)?.groupValues?.get(1)

        return ParsedTransaction(
            amountRupees = signedAmount,
            merchant = merchant,
            category = category,
            accountHint = accountHint,
        )
    }

    private fun extractMerchant(text: String, isCredit: Boolean): String? {
        vpaRegex.find(text)?.let { return prettifyHandle(it.groupValues[1]) }
        if (isCredit) {
            fromPersonRegex.find(text)?.let { return it.groupValues[1] }
        }
        toMerchantRegex.find(text)?.let { return it.groupValues[1] }
        fromPersonRegex.find(text)?.let { return it.groupValues[1] }
        return null
    }

    private fun prettifyHandle(handle: String): String =
        handle.replace(Regex("[._-]"), " ").split(" ")
            .filter { it.isNotBlank() }
            .joinToString(" ") { it.replaceFirstChar(Char::uppercase) }

    private fun inferCategory(merchant: String): String {
        for ((pattern, category) in MERCHANT_CATEGORY_HINTS) {
            if (pattern.containsMatchIn(merchant)) return category
        }
        return "other"
    }
}
