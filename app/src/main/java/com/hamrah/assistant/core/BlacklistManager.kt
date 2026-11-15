package com.hamrah.assistant.core

object BlacklistManager {
    private val blockedPackages = setOf(
        // نمونه — نام بسته‌های اپ‌های بانکی/صرافی خودت را اضافه کن
        "com.bank.example",
        "com.exchange.example"
    )
    fun isPackageAllowed(packageName: String?): Boolean =
        packageName != null && !blockedPackages.contains(packageName)
}
