package com.bibintomj.echoscholar.util

import android.content.Context
import java.util.UUID

fun getOrCreateLocalUserId(ctx: Context): String {
    val prefs = ctx.getSharedPreferences("chat_prefs", Context.MODE_PRIVATE)
    val existing = prefs.getString("local_user_id", null)
    if (existing != null) return existing
    val newId = UUID.randomUUID().toString()
    prefs.edit().putString("local_user_id", newId).apply()
    return newId
}
