package it.radioteateonair.app

import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.google.android.material.edge.EdgeToEdge // ✅ IMPORTANTE

class MainActivity : AppCompatActivity() {
    private lateinit var webView: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ✅ 1. Abilita la modalità edge-to-edge compatibile
        EdgeToEdge.enable(this)

        // ✅ 2. Crea la WebView e la mostra
        webView = WebView(this)
        setContentView(webView)

        // ✅ 3. Applica padding per evitare che la nav bar copra la WebView
        ViewCompat.setOnApplyWindowInsetsListener(webView) { view, insets ->
            val navBarInsets = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            view.updatePadding(bottom = navBarInsets.bottom)
            insets
        }

        // ✅ 4. Configura la WebView
        webView.webViewClient = WebViewClient()
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.loadUrl("https://radioteateonair.it")
    }

    @Deprecated("Deprecated in Android API 33")
    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }
}
