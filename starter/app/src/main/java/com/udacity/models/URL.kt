package com.udacity.models

enum class URL(val uri: String, val title: String, val text: String) {
    GLIDE_URI(
        "https://github.com/bumptech/glide/archive/master.zip",
        "Glide: Image Loading Library By BumpTech",
        "Glide repository is downloaded"
    ),
    UDACITY_URI(
        "https://github.com/udacity/nd940-c3-advanced-android-programming-project-starter/archive/master.zip",
        "Udacity: Android Kotlin Nanodegree",
        "The Project 3 repository is downloaded"
    ),
    RETROFIT_URI(
        "https://github.com/square/retrofit/archive/master.zip",
        "Retrofit: Type-safe HTTP client by Square, Inc",
        "Retrofit repository is downloaded"
    ),
}