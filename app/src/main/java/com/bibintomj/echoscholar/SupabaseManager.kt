package com.bibintomj.echoscholar
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.AuthConfig
import io.ktor.client.engine.okhttp.OkHttp

object SupabaseManager {
    val supabase: SupabaseClient = createSupabaseClient(
        supabaseUrl = BuildConfig.SUPABASE_URL,
        supabaseKey = BuildConfig.SUPABASE_ANON_KEY
    ) {
        install(Auth) {
            // Optional config like autoLoadFromStorage = true
        }
    }
}