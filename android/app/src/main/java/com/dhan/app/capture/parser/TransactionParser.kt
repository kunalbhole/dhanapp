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

    // First alternative requires at least one comma group (`+`, not `*`) so it only
    // matches genuinely comma-formatted numbers ("5,000", "1,00,000"). With `*` it could
    // match zero comma groups and "succeed" on just the first 1-3 digits of a comma-less
    // number (e.g. "500" out of "5000"), which won since alternation never backtracks
    // into the second branch once the first "succeeds" — silently truncating amounts.
    private val amountRegex = Regex(
        """(?:rs\.?|inr|₹)\s?([0-9]{1,3}(?:,[0-9]{2,3})+(?:\.[0-9]{1,2})?|[0-9]+(?:\.[0-9]{1,2})?)""",
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

    // Indian DLT SMS headers look like "AX-AXISBK-S" or "VM-HDFCBK" — telecom-operator
    // prefix, registered entity code, optional service-type suffix. Strips both so the
    // remaining code can be looked up (or at least title-cased) into something readable.
    private val dltHeaderRegex = Regex("""^[A-Za-z]{2}-([A-Za-z0-9]+)(?:-[A-Za-z])?$""")

    private val KNOWN_SENDER_CODES: Map<String, String> = mapOf(
        "AXISBK" to "Axis Bank", "AXISB" to "Axis Bank",
        "SBIINB" to "SBI", "SBIUPI" to "SBI UPI", "SBICRD" to "SBI Card", "ATMSBI" to "SBI", "SBIPSG" to "SBI",
        "HDFCBK" to "HDFC Bank", "HDFCBN" to "HDFC Bank",
        "ICICIB" to "ICICI Bank", "ICICIT" to "ICICI Bank",
        "KOTAKB" to "Kotak Bank", "KMBL" to "Kotak Bank",
        "PNBSMS" to "PNB", "PUNBNK" to "PNB",
        "BOIIND" to "Bank of India",
        "BOBTXN" to "Bank of Baroda", "BOBIBK" to "Bank of Baroda",
        "UNIONB" to "Union Bank", "UBOI" to "Union Bank",
        "CANBNK" to "Canara Bank",
        "IDFCFB" to "IDFC First Bank",
        "YESBNK" to "Yes Bank",
        "INDBNK" to "IndusInd Bank", "INDUSB" to "IndusInd Bank",
        "IDBIBK" to "IDBI Bank",
        "JIOPAY" to "JioPay",
        "GOOGLP" to "Google Pay", "GPAY" to "Google Pay",
        "PHONPE" to "PhonePe",
        "PAYTM" to "Paytm",
        "AMAZNP" to "Amazon Pay", "AMZNPY" to "Amazon Pay",
        "BHIMUP" to "BHIM UPI",
        "NPCIUP" to "UPI",
    )

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

        val merchantRaw = extractMerchant(text, isCredit = signedAmount > 0) ?: humanizeSender(sourceApp) ?: "Transaction"
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

    /**
     * Last-resort fallback when no merchant/payee could be extracted from the message body
     * — cleans a raw SMS sender ID into a human-readable bank/service name instead of
     * showing e.g. "AX-AXISBK-S" as the transaction title. Notification-derived sourceApp
     * values (already human names like "Google Pay", set by TxnNotificationListenerService)
     * simply don't match the DLT header pattern and pass through unchanged.
     */
    private fun humanizeSender(sourceApp: String?): String? {
        if (sourceApp.isNullOrBlank()) return null
        val trimmed = sourceApp.trim()
        val match = dltHeaderRegex.find(trimmed) ?: return trimmed
        val code = match.groupValues[1].uppercase()
        KNOWN_SENDER_CODES[code]?.let { return it }
        // Unknown DLT code: still strictly better than the raw "XX-CODE-S" header — best
        // effort title-case, turning a trailing BK/BNK/BANK into " Bank".
        val cleaned = code.replace(Regex("(BK|BNK|BANK)$"), " Bank")
        return cleaned.split(" ", "_").filter { it.isNotBlank() }
            .joinToString(" ") { it.lowercase().replaceFirstChar(Char::uppercase) }
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
