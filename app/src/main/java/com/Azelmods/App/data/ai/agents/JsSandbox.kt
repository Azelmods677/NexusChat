package com.Azelmods.App.data.ai.agents

import android.annotation.SuppressLint
import android.content.Context
import android.webkit.WebView
import android.webkit.WebViewClient
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import org.json.JSONTokener
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * Ejecuta JavaScript de los agentes en un WebView aislado.
 *
 * Un agente lee páginas de internet y esas páginas pueden intentar darle órdenes, así que lo
 * que ejecuta no puede tocar nada del usuario: sin red (CSP y bloqueo de cargas), sin archivos,
 * sin almacenamiento y sin ningún puente hacia la app. Cada ejecución usa un WebView nuevo que
 * se destruye al terminar, de modo que tampoco queda estado entre una y otra.
 */
@Singleton
class JsSandbox @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private companion object {
        const val TIMEOUT_MS = 12_000L
        const val POLL_MS = 80L
        const val PAGE = "<!doctype html><html><head><meta http-equiv=\"Content-Security-Policy\" " +
            "content=\"default-src 'none'; script-src 'unsafe-inline' 'unsafe-eval'\"></head><body></body></html>"
    }

    @SuppressLint("SetJavaScriptEnabled")
    suspend fun run(code: String): ToolOutcome = withContext(Dispatchers.Main) {
        val web = WebView(context)
        try {
            web.settings.apply {
                javaScriptEnabled = true
                blockNetworkLoads = true
                allowFileAccess = false
                allowContentAccess = false
                domStorageEnabled = false
            }
            val loaded = CompletableDeferred<Unit>()
            web.webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    loaded.complete(Unit)
                }
            }
            web.loadDataWithBaseURL(null, PAGE, "text/html", "utf-8", null)
            withTimeoutOrNull(5_000) { loaded.await() }
                ?: return@withContext ToolOutcome("No se pudo preparar el entorno de ejecución.", false)

            web.evaluateJavascript(wrap(code), null)

            val raw = withTimeoutOrNull(TIMEOUT_MS) {
                var value = ""
                while (value.isEmpty()) {
                    delay(POLL_MS)
                    value = evaluate(web, "window.__nexusResult||''")
                }
                value
            } ?: return@withContext ToolOutcome(
                "El código superó los ${TIMEOUT_MS / 1000} s y se detuvo. Revisa si hay un bucle infinito.",
                false
            )

            val result = JSONObject(raw)
            val logs = result.optJSONArray("logs")
            val output = buildString {
                if (logs != null) for (i in 0 until logs.length()) appendLine(logs.optString(i))
                val value = result.optString("result")
                if (value.isNotBlank() && value != "null") appendLine("Valor devuelto: $value")
            }.trim()
            val error = result.optString("error")
            if (error.isNotBlank() && error != "null") {
                ToolOutcome(listOf(output, "Error: $error").filter { it.isNotBlank() }.joinToString("\n"), false)
            } else {
                ToolOutcome(output.ifBlank { "El código terminó sin imprimir nada. Usa console.log para ver resultados." }, true)
            }
        } finally {
            web.destroy()
        }
    }

    private suspend fun evaluate(web: WebView, script: String): String = suspendCoroutine { continuation ->
        web.evaluateJavascript(script) { value ->
            // evaluateJavascript devuelve el resultado codificado como literal JSON.
            val decoded = runCatching { JSONTokener(value).nextValue() as? String }.getOrNull()
            continuation.resume(decoded.orEmpty())
        }
    }

    private fun wrap(code: String): String = """
        (function(){
          var logs=[];
          var fmt=function(v){try{return (typeof v==='object'&&v!==null)?JSON.stringify(v):String(v)}catch(e){return String(v)}};
          ['log','info','warn','error','debug'].forEach(function(k){
            console[k]=function(){if(logs.length<500)logs.push(Array.prototype.map.call(arguments,fmt).join(' '))};
          });
          var done=function(r,e){window.__nexusResult=JSON.stringify({logs:logs,result:r===undefined?null:fmt(r),error:e?String(e&&e.stack||e):null})};
          try{
            var AF=Object.getPrototypeOf(async function(){}).constructor;
            new AF(${JSONObject.quote(code)})().then(function(r){done(r,null)},function(e){done(undefined,e)});
          }catch(e){done(undefined,e)}
        })();
    """.trimIndent()
}
