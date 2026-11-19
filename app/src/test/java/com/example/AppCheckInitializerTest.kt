package com.example

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.example.ai.ColorGradingAnalyzer
import com.example.ai.configureAppCheck
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AppCheckInitializerTest {

    @Test
    fun `configureAppCheck initializes FirebaseApp and persists debug token synchronously`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        if (FirebaseApp.getApps(context).isEmpty()) {
            FirebaseApp.initializeApp(
                context,
                FirebaseOptions.Builder()
                    .setApplicationId("1:188037161532:android:a884383e8f0131dcc3d939")
                    .setApiKey("AIzaSyTestKeyForRobolectricVerification")
                    .setProjectId("gen-lang-client-0779898070")
                    .build()
            )
        }

        val expectedToken = "test-debug-token-1234-5678"
        val intent = Intent().apply {
            putExtra("FIREBASE_APPCHECK_DEBUG_TOKEN", expectedToken)
        }

        configureAppCheck(context, intent)

        val persistenceKey = FirebaseApp.getInstance().persistenceKey
        val prefs = context.getSharedPreferences(
            "com.google.firebase.appcheck.debug.store.$persistenceKey",
            Context.MODE_PRIVATE
        )
        assertEquals(
            expectedToken,
            prefs.getString("com.google.firebase.appcheck.debug.DEBUG_SECRET", null)
        )
    }

    @Test
    fun `recaptcha_site_key resource is configured for release App Check`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val resId = context.resources.getIdentifier(
            "recaptcha_site_key",
            "string",
            context.packageName
        )
        assertTrue("recaptcha_site_key string resource must exist", resId != 0)
        assertTrue(context.getString(resId).isNotBlank())
    }

    @Test
    fun `ColorGradingAnalyzer constructs lazily without eager Firebase evaluation`() {
        val analyzer = ColorGradingAnalyzer()
        assertNotNull(analyzer)
    }
}
