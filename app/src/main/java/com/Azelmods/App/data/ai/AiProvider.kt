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
    val hint: String = "",
    /**
     * Modelos sugeridos para elegir de un toque, sin teclear el id a mano.
     *
     * NO es una lista cerrada: el campo "Modelo" sigue siendo texto libre y la
     * pantalla de IA puede pedir el catálogo real al proveedor (`GET /models`).
     * Así, cuando un proveedor publica una versión nueva (DeepSeek V4, GLM 5,
     * Qwen Coder más reciente, Kimi…), aparece sola SIN actualizar la app.
     */
    val suggestedModels: List<String> = emptyList()
) {
    GEMINI(
        id = "gemini",
        displayName = "Google Gemini",
        defaultBaseUrl = "",
        defaultModel = "gemini-2.5-flash",
        isOpenAiCompatible = false,
        hint = "Clave desde Google AI Studio.",
        suggestedModels = listOf(
            "gemini-2.5-flash",
            "gemini-2.5-pro",
            "gemini-2.0-flash"
        )
    ),
    OPENAI(
        id = "openai",
        displayName = "OpenAI",
        defaultBaseUrl = "https://api.openai.com/v1",
        defaultModel = "gpt-4o-mini",
        isOpenAiCompatible = true,
        hint = "Clave desde platform.openai.com.",
        suggestedModels = listOf("gpt-4o-mini", "gpt-4o", "gpt-4.1-mini", "o4-mini")
    ),
    OPENROUTER(
        id = "openrouter",
        displayName = "OpenRouter",
        defaultBaseUrl = "https://openrouter.ai/api/v1",
        defaultModel = "deepseek/deepseek-chat",
        isOpenAiCompatible = true,
        hint = "Una sola clave para cientos de modelos: DeepSeek, Qwen Coder, GLM, Kimi, Llama… " +
            "Pulsa «Buscar modelos» para ver el catálogo actualizado.",
        suggestedModels = listOf(
            "deepseek/deepseek-chat",
            "deepseek/deepseek-r1",
            "qwen/qwen3-coder",
            "z-ai/glm-4.6",
            "moonshotai/kimi-k2",
            "meta-llama/llama-3.3-70b-instruct"
        )
    ),
    DEEPSEEK(
        id = "deepseek",
        displayName = "DeepSeek",
        defaultBaseUrl = "https://api.deepseek.com/v1",
        defaultModel = "deepseek-chat",
        isOpenAiCompatible = true,
        hint = "«deepseek-chat» apunta siempre al modelo de chat más reciente de DeepSeek.",
        suggestedModels = listOf("deepseek-chat", "deepseek-reasoner")
    ),
    QWEN(
        id = "qwen",
        displayName = "Qwen (Alibaba)",
        // Endpoint internacional compatible con OpenAI de DashScope.
        defaultBaseUrl = "https://dashscope-intl.aliyuncs.com/compatible-mode/v1",
        defaultModel = "qwen3-coder-plus",
        isOpenAiCompatible = true,
        hint = "Familia Qwen, incluidos los Coder. Clave desde DashScope (Alibaba Cloud).",
        suggestedModels = listOf("qwen3-coder-plus", "qwen-plus", "qwen-max", "qwen-turbo")
    ),
    ZAI(
        id = "zai",
        displayName = "Z.ai / GLM (Zhipu)",
        defaultBaseUrl = "https://api.z.ai/api/paas/v4",
        defaultModel = "glm-4.6",
        isOpenAiCompatible = true,
        hint = "Modelos GLM. Si ya salió una versión superior, pulsa «Buscar modelos».",
        suggestedModels = listOf("glm-4.6", "glm-4.5", "glm-4.5-air")
    ),
    MOONSHOT(
        id = "moonshot",
        displayName = "Kimi (Moonshot AI)",
        defaultBaseUrl = "https://api.moonshot.ai/v1",
        defaultModel = "kimi-latest",
        isOpenAiCompatible = true,
        hint = "«kimi-latest» sigue siempre al Kimi más nuevo. Clave desde platform.moonshot.ai.",
        suggestedModels = listOf("kimi-latest", "kimi-k2-turbo-preview", "moonshot-v1-128k")
    ),
    MISTRAL(
        id = "mistral",
        displayName = "Mistral",
        defaultBaseUrl = "https://api.mistral.ai/v1",
        defaultModel = "mistral-small-latest",
        isOpenAiCompatible = true,
        suggestedModels = listOf("mistral-small-latest", "mistral-large-latest", "codestral-latest")
    ),
    GROQ(
        id = "groq",
        displayName = "Groq",
        defaultBaseUrl = "https://api.groq.com/openai/v1",
        defaultModel = "llama-3.3-70b-versatile",
        isOpenAiCompatible = true,
        suggestedModels = listOf("llama-3.3-70b-versatile", "llama-3.1-8b-instant")
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
        hint = "Sin clave. En móvil: adb reverse tcp:11434 tcp:11434. El modelo que elijas decide si hay filtros.",
        // Pulsando «Buscar modelos» se lista lo que realmente tengas descargado.
        suggestedModels = listOf("llama3.2", "qwen2.5-coder", "deepseek-r1", "mistral")
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
