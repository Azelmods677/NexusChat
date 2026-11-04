package com.Azelmods.App.data.repository

import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/** Suscripción de una persona. La escribe solo el servidor, tras confirmar el pago. */
data class PremiumStatus(
    val active: Boolean = false,
    val plan: String = "",
    /** null = para siempre. */
    val expiresAt: Long? = null
) {
    fun isActiveNow(now: Long = System.currentTimeMillis()): Boolean =
        active && (expiresAt == null || expiresAt > now)
}

/** Un plan a la venta. Precio y moneda los dicta el servidor: la app no cobra nada por su cuenta. */
data class PremiumPlan(
    val id: String,
    val title: String,
    val priceLabel: String,
    val period: String,
    val badge: String?
)

/** Fallo que se le puede explicar a la persona tal cual. */
class PaymentsUnavailableException(message: String) : IOException(message)

/**
 * Premium de verdad: el pago lo cobra Mercado Pago y lo confirma el servidor.
 *
 * La app nunca decide que alguien ha pagado. Pide al servidor un enlace de pago, lo abre, y
 * después se limita a escuchar `premium/{uid}`, que las reglas de la base de datos solo dejan
 * escribir a las Cloud Functions. Así un APK modificado no puede regalarse Premium.
 */
@Singleton
class PremiumRepository @Inject constructor() {

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val baseUrl: String
        get() = "https://us-central1-${FirebaseApp.getInstance().options.projectId}.cloudfunctions.net/premiumApi"

    fun observe(uid: String): Flow<PremiumStatus> = callbackFlow {
        val ref = FirebaseDatabase.getInstance().getReference("premium/$uid")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                trySend(
                    PremiumStatus(
                        active = snapshot.child("active").getValue(Boolean::class.java) ?: false,
                        plan = snapshot.child("plan").getValue(String::class.java).orEmpty(),
                        expiresAt = snapshot.child("expiresAt").getValue(Long::class.java)
                    )
                )
            }

            override fun onCancelled(error: DatabaseError) {
                trySend(PremiumStatus())
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    /** Planes y precios vigentes, según el servidor. */
    suspend fun loadPlans(): List<PremiumPlan> = withContext(Dispatchers.IO) {
        val json = call(Request.Builder().url("$baseUrl/plans").get())
        val plans = json.getJSONArray("plans")
        (0 until plans.length()).map { i ->
            val plan = plans.getJSONObject(i)
            PremiumPlan(
                id = plan.getString("id"),
                title = plan.getString("title"),
                priceLabel = plan.getString("priceLabel"),
                period = plan.optString("period"),
                badge = plan.optString("badge").takeIf { it.isNotBlank() }
            )
        }
    }

    /** Crea el cobro en el servidor y devuelve el enlace de pago de Mercado Pago. */
    suspend fun createCheckout(planId: String): String = withContext(Dispatchers.IO) {
        val user = FirebaseAuth.getInstance().currentUser
            ?: throw PaymentsUnavailableException("Inicia sesión para continuar.")
        val token = user.getIdToken(false).await().token
            ?: throw PaymentsUnavailableException("No se pudo verificar tu sesión.")
        val body = JSONObject().put("plan", planId).toString()
        val request = Request.Builder()
            .url("$baseUrl/checkout")
            .header("Authorization", "Bearer $token")
            .post(body.toRequestBody("application/json".toMediaType()))
        call(request).getString("url")
    }

    private fun call(builder: Request.Builder): JSONObject {
        val response = try {
            client.newCall(builder.build()).execute()
        } catch (e: IOException) {
            throw PaymentsUnavailableException("Sin conexión con el servidor de pagos. Revisa tu internet.")
        }
        response.use {
            val text = it.body?.string().orEmpty()
            if (it.code == 404) {
                throw PaymentsUnavailableException("Los pagos todavía no están activados en el servidor.")
            }
            val json = runCatching { JSONObject(text) }.getOrNull()
            if (!it.isSuccessful || json == null) {
                throw PaymentsUnavailableException(
                    json?.optString("error")?.takeIf { message -> message.isNotBlank() }
                        ?: "El servidor de pagos no respondió bien. Inténtalo en un rato."
                )
            }
            return json
        }
    }
}
