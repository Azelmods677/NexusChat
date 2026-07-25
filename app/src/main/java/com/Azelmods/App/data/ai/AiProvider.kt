package com.Azelmods.App.data.ai

/**
 * Proveedores de IA que el usuario puede elegir.
 *
 * Salvo Gemini, todos hablan el mismo dialecto: `POST {baseUrl}/chat/completions`
 * con `Authorization: Bearer <key>`, el estándar de facto que implementan OpenAI,
 * Ollama, OpenRouter, DeepSeek, Mistral, Groq, LM Studio y prácticamente cualquier
 * servidor local. Por eso basta un único cliente para todos ellos: no hace falta
 * una integración por proveedor.
 *
 * Gemini es la excepción porque usa `:generateContent` y un esquema propio
 * (`contents`/`parts` en vez de `messages`), así que mantiene su ruta aparte.
 *
 * La censura no la decide la app: depende del modelo que el usuario elija. Un
 * modelo local vía Ollama no aplica los filtros de un proveedor comercial. La app
 * solo enruta a donde el usuario le dice.
 */
enum class AiProvider(
    val id: String,
    val displayName: String,
    /** Base OpenAI-compatible; vacío en Gemini, que tiene su propia ruta. */
    val defaultBaseUrl: String,
    val defaultModel: String,
    /** true si habla `/chat/completions` en lugar del esquema de Gemini. */
    val isOpenAiCompatible: Boolean,
    /** true si el endpoint corre en el propio dispositivo o la red local. */
    val isLocal: Boolean = false,
    /** true si el endpoint no necesita API key (servidores locales). */
    val allowsEmptyKey: Boolean = false,
    val hint: String = ""
) {
    GEMINI(
        id = "gemini",
        displayName = "Google Gemini",
        defaultBaseUrl = "",
        defaultModel = "gemini-2.5-flash",
        isOpenAiCompatible = false,
        hint = "Clave desde Google AI Studio."
    ),
    OPENAI(
        id = "openai",
        displayName = "OpenAI",
        defaultBaseUrl = "https://api.openai.com/v1",
        defaultModel = "gpt-4o-mini",
        isOpenAiCompatible = true,
        hint = "Clave desde platform.openai.com."
    ),
    OPENROUTER(
        id = "openrouter",
        displayName = "OpenRouter",
        defaultBaseUrl = "https://openrouter.ai/api/v1",
        defaultModel = "meta-llama/llama-3.3-70b-instruct",
        isOpenAiCompatible = true,
        hint = "Un único acceso a cientos de modelos, con y sin filtros."
    ),
    DEEPSEEK(
        id = "deepseek",
        displayName = "DeepSeek",
        defaultBaseUrl = "https://api.deepseek.com/v1",
        defaultModel = "deepseek-chat",
        isOpenAiCompatible = true
    ),
    MISTRAL(
        id = "mistral",
        displayName = "Mistral",
        defaultBaseUrl = "https://api.mistral.ai/v1",
        defaultModel = "mistral-small-latest",
        isOpenAiCompatible = true
    ),
    GROQ(
        id = "groq",
        displayName = "Groq",
        defaultBaseUrl = "https://api.groq.com/openai/v1",
        defaultModel = "llama-3.3-70b-versatile",
        isOpenAiCompatible = true
    ),
    OLLAMA(
        id = "ollama",
        displayName = "Ollama (local)",
        // 127.0.0.1 por defecto porque es la única ruta que funciona sin editar la
        // network security config: en móvil físico se redirige el puerto con
        //   adb reverse tcp:11434 tcp:11434
        // En el emulador, 10.0.2.2 apunta al equipo anfitrión (también permitido).
        // Una IP de la LAN (192.168.x.x) exige añadirla a network_security_config.xml,
        // porque Android no acepta rangos CIDR ahí.
        defaultBaseUrl = "http://127.0.0.1:11434/v1",
        defaultModel = "llama3.2",
        isOpenAiCompatible = true,
        isLocal = true,
        allowsEmptyKey = true,
        hint = "Sin clave. En móvil: adb reverse tcp:11434 tcp:11434. El modelo que elijas decide si hay filtros."
    ),
    CUSTOM(
        id = "custom",
        displayName = "Personalizado (OpenAI-compatible)",
        defaultBaseUrl = "",
        defaultModel = "",
        isOpenAiCompatible = true,
        allowsEmptyKey = true,
        hint = "Cualquier servidor que exponga /chat/completions: LM Studio, vLLM, llama.cpp…"
    );

    companion object {
        val DEFAULT = GEMINI

        fun fromId(id: String?): AiProvider =
            entries.firstOrNull { it.id == id } ?: DEFAULT
    }
}
