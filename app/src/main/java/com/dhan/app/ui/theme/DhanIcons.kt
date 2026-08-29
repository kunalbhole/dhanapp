package com.dhan.app.ui.theme

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.AirplanemodeActive
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowOutward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Laptop
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RemoveCircle
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ShoppingBasket
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.outlined.CalendarToday as CalendarTodayOutlined
import androidx.compose.material.icons.outlined.Home as HomeOutlined
import androidx.compose.material.icons.outlined.List as ListOutlined
import androidx.compose.material.icons.outlined.PieChart as PieChartOutlined
import androidx.compose.material.icons.outlined.Receipt as ReceiptOutlined
import androidx.compose.material.icons.filled.Circle
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Phosphor-icon-name -> Material Icon lookup. The source design (components.jsx /
 * screens-*.jsx) references Phosphor icon slugs (e.g. "fork-knife", "chart-pie-slice");
 * Phosphor's web font isn't available offline in this build environment, so every slug
 * used anywhere in the ported screens must have an entry here. Add new entries here
 * rather than inlining ad-hoc Icons.Filled.* calls in screen code, so icon choices for
 * a given concept stay consistent across all 26 screens.
 */
object DhanIcon {
    fun of(slug: String, filled: Boolean = true): ImageVector = when (slug) {
        "house", "home" -> if (filled) Icons.Filled.Home else HomeOutlined
        "list-bullets" -> if (filled) Icons.Filled.List else ListOutlined
        "chart-pie-slice" -> if (filled) Icons.Filled.PieChart else PieChartOutlined
        "receipt" -> if (filled) Icons.Filled.Receipt else ReceiptOutlined
        "list" -> Icons.Filled.List
        "plus" -> Icons.Filled.Add
        "plus-circle" -> Icons.Filled.AddCircle
        "minus-circle" -> Icons.Filled.RemoveCircle
        "arrow-left" -> Icons.Filled.ArrowBack
        "arrow-right" -> Icons.Filled.ArrowForward
        "arrow-up-right" -> Icons.Filled.ArrowOutward
        "arrow-down-left" -> Icons.Filled.ArrowDownward
        "arrow-up" -> Icons.Filled.ArrowUpward
        "arrow-down" -> Icons.Filled.ArrowDownward
        "x", "close" -> Icons.Filled.Close
        "check" -> Icons.Filled.Check
        "check-circle" -> Icons.Filled.CheckCircle
        "bell" -> Icons.Filled.Notifications
        "bell-ringing" -> Icons.Filled.NotificationsActive
        "fork-knife" -> Icons.Filled.Restaurant
        "car-simple" -> Icons.Filled.DirectionsCar
        "shopping-bag" -> Icons.Filled.ShoppingBag
        "film-strip" -> Icons.Filled.Movie
        "heartbeat" -> Icons.Filled.MonitorHeart
        "graduation-cap" -> Icons.Filled.School
        "basket" -> Icons.Filled.ShoppingBasket
        "airplane-takeoff" -> Icons.Filled.Flight
        "dots-three" -> Icons.Filled.MoreHoriz
        "funnel" -> Icons.Filled.Tune
        "caret-right" -> Icons.Filled.ChevronRight
        "pencil" -> Icons.Filled.Edit
        "gear" -> Icons.Filled.Settings
        "sign-out" -> Icons.Filled.Logout
        "user", "user-circle" -> Icons.Filled.Person
        "users" -> Icons.Filled.Group
        "question" -> Icons.Filled.HelpOutline
        "star" -> Icons.Filled.Star
        "calendar" -> if (filled) Icons.Filled.CalendarToday else CalendarTodayOutlined
        "repeat" -> Icons.Filled.Repeat
        "lock", "lock-key" -> Icons.Filled.Lock
        "eye" -> Icons.Filled.Visibility
        "eye-slash" -> Icons.Filled.VisibilityOff
        "shield-check" -> Icons.Filled.Shield
        "bank" -> Icons.Filled.AccountBalance
        "credit-card" -> Icons.Filled.CreditCard
        "wallet" -> Icons.Filled.Wallet
        "trend-up", "chart-line-up" -> Icons.Filled.TrendingUp
        "swap" -> Icons.Filled.SwapHoriz
        "link" -> Icons.Filled.Link
        "search" -> Icons.Filled.Search
        "share" -> Icons.Filled.Share
        "info" -> Icons.Filled.Info
        "lightning" -> Icons.Filled.Bolt
        "fingerprint" -> Icons.Filled.Fingerprint
        "device-mobile" -> Icons.Filled.PhoneAndroid
        "cell-signal-high" -> Icons.Filled.SignalCellularAlt
        "wifi-high" -> Icons.Filled.Wifi
        "battery-full" -> Icons.Filled.BatteryFull
        "chat-centered-text" -> Icons.Filled.Sms
        "address-book" -> Icons.Filled.Contacts
        "scan" -> Icons.Filled.Face
        "backspace" -> Icons.Filled.Backspace
        "google-logo" -> Icons.Filled.Public
        "pencil-simple" -> Icons.Filled.Edit
        "trash" -> Icons.Filled.Delete
        "users-three" -> Icons.Filled.Group
        "key" -> Icons.Filled.VpnKey
        "camera" -> Icons.Filled.PhotoCamera
        "magnifying-glass" -> Icons.Filled.Search
        "chat-circle-text", "chats" -> Icons.Filled.Forum
        "envelope" -> Icons.Filled.Email
        "download-simple" -> Icons.Filled.Download
        "paper-plane-tilt" -> Icons.Filled.Send
        "sparkle" -> Icons.Filled.AutoAwesome
        "stack" -> Icons.Filled.Layers
        "arrow-clockwise" -> Icons.Filled.Refresh
        "warning", "warning-octagon" -> Icons.Filled.WarningAmber
        "wifi-slash" -> Icons.Filled.WifiOff
        "trend-down" -> Icons.Filled.TrendingDown
        "lightbulb" -> Icons.Filled.Lightbulb
        "calendar-check" -> Icons.Filled.EventAvailable
        "airplane-tilt" -> Icons.Filled.Flight
        "laptop" -> Icons.Filled.Laptop
        "gift" -> Icons.Filled.CardGiftcard
        "receipt-x" -> Icons.Filled.Receipt
        else -> Icons.Filled.Circle
    }
}
