package com.example.pravaahan.di

import com.example.pravaahan.BuildConfig
import com.example.pravaahan.core.error.SupabaseConnectionException
import com.example.pravaahan.core.logging.Logger
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
// import io.github.jan.supabase.gotrue.GoTrue // TODO: Fix import issue
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.serializer.KotlinXSerializer
import javax.inject.Singleton
import kotlin.time.Duration.Companion.seconds

/**
 * Hilt module for providing Supabase dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
object SupabaseModule {
    
    private const val TAG = "SupabaseModule"
    
    @Provides
    @Singleton
    fun provideSupabaseClient(logger: Logger): SupabaseClient {
        logger.info(TAG, "Initializing Supabase client")
        
        return try {
            createSupabaseClient(
                supabaseUrl = BuildConfig.SUPABASE_URL,
                supabaseKey = BuildConfig.SUPABASE_ANON_KEY
            ) {
                install(Postgrest) {
                    serializer = KotlinXSerializer()
                }
                install(Realtime) {
                    reconnectDelay = 5.seconds
                    heartbeatInterval = 30.seconds
                }
                // TODO: Add GoTrue when import issue is resolved
                // install(GoTrue)
            }.also {
                logger.info(TAG, "Supabase client initialized successfully")
            }
        } catch (e: Exception) {
            logger.error(TAG, "Failed to initialize Supabase client", e)
            throw SupabaseConnectionException("Failed to connect to Supabase: ${e.message}", e)
        }
    }
}