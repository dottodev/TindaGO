package com.tindahan.tracker.ui.navigation

object Routes {
    const val STOCK = "stock"
    const val TRACKER = "tracker"
    const val MORE = "more"
    const val DASHBOARD = "dashboard"
    const val PRODUCT_DETAILS = "product/{productId}"
    const val SALES_HISTORY = "sales_history"
    const val ONBOARDING = "onboarding"
    const val CALCULATOR = "calculator"
    const val NOTES = "notes"

    fun productDetails(id: Long) = "product/$id"
}
