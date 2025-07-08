package com.bibintomj.echoscholar
import android.util.Log
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.AuthConfig
import io.github.jan.supabase.postgrest.Postgrest
import io.ktor.client.engine.okhttp.OkHttp
//import io.github.jan.supabase.plugins.UserManagement


object SupabaseManager {
    val supabase: SupabaseClient = createSupabaseClient(
        supabaseUrl = BuildConfig.SUPABASE_URL,
        supabaseKey = BuildConfig.SUPABASE_ANON_KEY
    ) {
        install(Auth) {
            // Optional config like autoLoadFromStorage = true
            autoLoadFromStorage = true
        }
        install(Postgrest)
//        install(UserManagement)
        Log.d("SupabaseDebug", "Using Supabase URL: ${BuildConfig.SUPABASE_URL}")
    }
}