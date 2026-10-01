package com.example.model

enum class VehicleType(
    val title: String,
    val subtitle: String,
    val iconEmoji: String
) {
    HEAVY_VEHICLE(
        title = "Ağır Vasıta",
        subtitle = "Tır • Kamyon • Çekici • Otobüs",
        iconEmoji = "🚛"
    ),
    AUTOMOBILE(
        title = "Otomobil",
        subtitle = "Otomobil • SUV • Hafif Ticari",
        iconEmoji = "🚗"
    )
}
