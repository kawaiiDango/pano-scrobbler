package com.arn.scrobble.billing

data class MyProductDetails(
    val productId: String,
    val title: String,
    val name: String,
    val description: String,
    val formattedPrice: String,
)