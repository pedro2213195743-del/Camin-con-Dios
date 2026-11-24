 package com.caminocondios.app

import android.annotation.SuppressLint
import android.content.ContentValues
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Base64
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.webkit.WebViewAssetLoader
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var webView: WebView
    private var tts: TextToSpeech? = null
    private var ttsReady = false

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        tts = TextToSpeech(this, this)

        webView = WebView(this)
        setContentView(webView)

        val assetLoader = WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(this))
            .build()

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            allowFileAccess = false
            allowContentAccess = true
            mediaPlaybackRequiresUserGesture = false
            cacheMode = WebSettings.LOAD_DEFAULT
            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
        }

        webView.addJavascriptInterface(AndroidTtsBridge(), "AndroidTTS")
        webView.addJavascriptInterface(AndroidFilesBridge(), "AndroidFiles")
        webView.webChromeClient = WebChromeClient()

        webView.setDownloadListener { url, _, _, _, _ ->
            if (url.startsWith("data:image/")) {
                saveDataUrlToGallery(url, "versiculo_${System.currentTimeMillis()}.png")
            }
        }

        webView.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(
                view: WebView,
                request: WebResourceRequest
            ): WebResourceResponse? {
                return assetLoader.shouldInterceptRequest(request.url)
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                val hook = """
                    (function(){
                      if (window.__ccdSaveHook) return;
                      window.__ccdSaveHook = true;
                      var origClick = HTMLAnchorElement.prototype.click;
                      HTMLAnchorElement.prototype.click = function() {
                        if (this.href && this.href.indexOf('data:image/') === 0 && window.AndroidFiles) {
                          window.AndroidFiles.saveImage(this.href, this.download || 'versiculo.png');
                          return;
                        }
                        return origClick.apply(this, arguments);
                      };
                    })();
                """.trimIndent()
                view?.evaluateJavascript(hook, null)
            }
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (webView.canGoBack()) {
                    webView.goBack()
                } else {
                    finish()
                }
            }
        })

        webView.loadUrl("https://appassets.androidplatform.net/assets/www/index.html")
    }

    private fun saveDataUrlToGallery(dataUrl: String, rawName: String): Boolean {
        return try {
            val base64Part = dataUrl.substringAfter("base64,", "")
            if (base64Part.isEmpty()) return false
            val bytes = Base64.decode(base64Part, Base64.DEFAULT)
            val safeName = rawName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
            val fileName = if (safeName.endsWith(".png", true)) safeName else "$safeName.png"

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                    put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/CaminoConDios")
                }
                val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                if (uri != null) {
                    contentResolver.openOutputStream(uri)?.use { it.write(bytes) }
                }
            } else {
                val dir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
                val file = File(dir, fileName)
                FileOutputStream(file).use { it.write(bytes) }
            }

            runOnUiThread {
                Toast.makeText(this, "Guardada en Galería (Pictures/CaminoConDios)", Toast.LENGTH_LONG).show()
            }
            true
        } catch (e: Exception) {
            runOnUiThread {
                Toast.makeText(this, "No se pudo guardar la imagen", Toast.LENGTH_SHORT).show()
            }
            false
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val esMx = Locale("es", "MX")
            val res = tts?.setLanguage(esMx)
            if (res == TextToSpeech.LANG_MISSING_DATA || res == TextToSpeech.LANG_NOT_SUPPORTED) {
                tts?.setLanguage(Locale("es", "ES"))
            }
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {}
                override fun onDone(utteranceId: String?) {
                    runOnUiThread {
                        webView.evaluateJavascript("if(window.__androidTtsDone) window.__androidTtsDone();", null)
                    }
                }
                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    runOnUiThread {
                        webView.evaluateJavascript("if(window.__androidTtsDone) window.__androidTtsDone();", null)
                    }
                }
            })
            ttsReady = true
        }
    }

    inner class AndroidTtsBridge {
        @JavascriptInterface
        fun speak(text: String, rate: Float) {
            if (!ttsReady) return
            tts?.setSpeechRate(rate.coerceIn(0.75f, 1.4f))
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "ccd_${System.currentTimeMillis()}")
        }

        @JavascriptInterface
        fun stop() {
            tts?.stop()
        }
    }

    inner class AndroidFilesBridge {
        @JavascriptInterface
        fun saveImage(dataUrl: String, fileName: String): Boolean {
            return saveDataUrlToGallery(dataUrl, fileName)
        }
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }
}
   
