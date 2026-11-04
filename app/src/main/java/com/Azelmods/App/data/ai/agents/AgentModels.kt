package com.Azelmods.App.data.ai.agents

/**
 * Herramientas que un agente puede usar. Todas hacen algo real: el texto de [usage] es lo
 * que lee el modelo, así que describe la entrada exacta que espera cada una.
 */
enum class AgentToolId(val id: String, val label: String, val emoji: String, val usage: String) {
    WEB_SEARCH(
        "web_search", "Búsqueda web", "🔎",
        "busca en varios buscadores a la vez (Google cuando hay clave de Gemini, DuckDuckGo, Bing y Yahoo) " +
            "y marca qué resultados contienen literalmente lo buscado. input: la consulta; admite " +
            "\"frase exacta\" y site:dominio.com."
    ),
    NEWS_SEARCH(
        "news_search", "Noticias", "📰",
        "noticias recientes de Google Noticias con fecha y medio. input: el tema."
    ),
    READ_URL(
        "read_url", "Leer página", "🌐",
        "abre una página y devuelve su texto, también en webs hechas con JavaScript. input: la URL completa; " +
            "para seguir leyendo una página larga añade al final: pagina=2"
    ),
    PROFILE_LOOKUP(
        "profile_lookup", "Buscar perfil", "🪪",
        "comprueba si un nombre de usuario existe en GitHub, GitLab, YouTube, X, Reddit, Telegram, TikTok y " +
            "Hacker News. input: el nombre de usuario, sin @."
    ),
    GITHUB(
        "github", "GitHub", "🐙",
        "busca repositorios y perfiles, y lee código. input: texto libre, o repo:dueño/nombre para ver un " +
            "repositorio y su README, o file:dueño/nombre/ruta/al/archivo para leer un archivo."
    ),
    STACKOVERFLOW(
        "stackoverflow", "Stack Overflow", "🧩",
        "busca preguntas y soluciones de programación. input: el problema o el mensaje de error."
    ),
    PACKAGE_INFO(
        "package_info", "Paquetes", "📦",
        "versión actual y descripción de una librería. input: npm:react, pypi:requests, crates:serde o " +
            "maven:com.squareup.okhttp3:okhttp"
    ),
    WIKIPEDIA(
        "wikipedia", "Wikipedia", "📚",
        "resumen enciclopédico de un tema. input: el nombre del tema."
    ),
    WEATHER(
        "weather", "Clima", "⛅",
        "tiempo actual y pronóstico de 3 días. input: la ciudad."
    ),
    CURRENCY(
        "currency", "Divisas", "💱",
        "convierte moneda con el cambio de hoy. input: 100 USD a COP"
    ),
    CALCULATOR(
        "calculator", "Calculadora", "🧮",
        "calcula una expresión exacta. input: la expresión, p. ej. (1200*0.21)+15 o sqrt(2)."
    ),
    DATETIME(
        "datetime", "Fecha y hora", "🕒",
        "fecha y hora actuales del dispositivo. input: vacío."
    ),
    RUN_JS(
        "run_js", "Ejecutar JavaScript", "⚡",
        "ejecuta JavaScript en un entorno aislado del teléfono, sin red ni acceso a datos, y devuelve lo que " +
            "imprima console.log. Ideal para probar lógica y hacer cálculos con datos. input: el código."
    ),
    RUN_CODE(
        "run_code", "Ejecutar código", "▶️",
        "compila y ejecuta código en un servidor externo y devuelve la salida y los errores. Lenguajes: python, " +
            "javascript, typescript, c, cpp, java, rust, go, php, ruby, bash, csharp, lua, perl, swift. " +
            "input: el lenguaje en la primera línea y el código debajo. No incluyas contraseñas ni datos privados."
    ),
    SAVE_FILE(
        "save_file", "Guardar archivo", "💾",
        "guarda un archivo en el Editor de código del usuario, donde puede abrirlo y ejecutarlo. input: el " +
            "nombre del archivo con extensión en la primera línea y el contenido completo debajo."
    ),
    READ_FILE(
        "read_file", "Leer archivo", "📄",
        "lee un archivo del Editor de código del usuario. input: el nombre del archivo."
    ),
    LIST_FILES(
        "list_files", "Mis archivos", "🗂️",
        "lista los archivos del Editor de código del usuario. input: vacío."
    );

    companion object {
        /**
         * Los modelos inventan nombres parecidos ("google_search", "python"...). Antes cada
         * uno se contaba como herramienta inexistente y gastaba un paso de trabajo.
         */
        private val aliases = mapOf(
            "search" to WEB_SEARCH, "google" to WEB_SEARCH, "google_search" to WEB_SEARCH,
            "bing" to WEB_SEARCH, "duckduckgo" to WEB_SEARCH, "internet" to WEB_SEARCH, "websearch" to WEB_SEARCH,
            "news" to NEWS_SEARCH, "noticias" to NEWS_SEARCH,
            "browser" to READ_URL, "browse" to READ_URL, "open_url" to READ_URL, "fetch" to READ_URL,
            "fetch_url" to READ_URL, "visit" to READ_URL, "read_page" to READ_URL,
            "username_lookup" to PROFILE_LOOKUP, "user_lookup" to PROFILE_LOOKUP, "profile" to PROFILE_LOOKUP,
            "github_search" to GITHUB, "stack_overflow" to STACKOVERFLOW,
            "npm" to PACKAGE_INFO, "pypi" to PACKAGE_INFO, "package" to PACKAGE_INFO,
            "clima" to WEATHER, "convert_currency" to CURRENCY, "exchange" to CURRENCY,
            "calc" to CALCULATOR, "math" to CALCULATOR, "calculate" to CALCULATOR,
            "date" to DATETIME, "time" to DATETIME, "now" to DATETIME,
            "javascript" to RUN_JS, "js" to RUN_JS, "node" to RUN_JS,
            "python" to RUN_CODE, "code" to RUN_CODE, "execute" to RUN_CODE, "execute_code" to RUN_CODE,
            "code_interpreter" to RUN_CODE, "run" to RUN_CODE, "run_python" to RUN_CODE,
            "write_file" to SAVE_FILE, "create_file" to SAVE_FILE, "write" to SAVE_FILE, "save" to SAVE_FILE,
            "open_file" to READ_FILE, "cat" to READ_FILE, "ls" to LIST_FILES, "files" to LIST_FILES
        )

        fun fromId(id: String): AgentToolId? {
            val key = id.trim().lowercase().replace('-', '_').replace(' ', '_')
            return entries.firstOrNull { it.id == key || it.name.lowercase() == key } ?: aliases[key]
        }
    }
}

/** Un agente: identidad, estilo de trabajo y herramientas permitidas. */
data class AgentDefinition(
    val id: String,
    val name: String,
    val role: String,
    val emoji: String,
    /** Color ARGB del agente en la interfaz. */
    val color: Long,
    val instructions: String,
    val tools: Set<AgentToolId> = emptySet(),
    val maxSteps: Int = 4
)

/** Un equipo: coordinador + especialistas que trabajan en cadena. */
data class AgentTeam(
    val id: String,
    val name: String,
    val description: String,
    val emoji: String,
    /** Especialistas disponibles para el plan, en su orden natural. */
    val memberIds: List<String>,
    /** Agente que entrega la respuesta final (revisa y une el trabajo). */
    val finisherId: String
)

enum class AgentNodeStatus { WAITING, THINKING, USING_TOOL, DONE, ERROR }

/** Estado visible de un agente dentro de una ejecución. */
data class AgentNode(
    val agent: AgentDefinition,
    val status: AgentNodeStatus = AgentNodeStatus.WAITING,
    val activity: String = "En espera",
    val activeTool: AgentToolId? = null,
    val steps: Int = 0
)

/** Lo que va pasando, para el registro en vivo. */
sealed class AgentEvent {
    abstract val id: Long
    abstract val agentId: String

    data class Plan(override val id: Long, override val agentId: String, val steps: List<PlanStep>) : AgentEvent()
    data class Handoff(override val id: Long, override val agentId: String, val task: String) : AgentEvent()
    data class Thought(override val id: Long, override val agentId: String, val text: String) : AgentEvent()
    data class ToolCall(override val id: Long, override val agentId: String, val tool: AgentToolId?, val toolName: String, val input: String) : AgentEvent()
    data class ToolResult(override val id: Long, override val agentId: String, val tool: AgentToolId?, val output: String, val ok: Boolean) : AgentEvent()
    data class Output(override val id: Long, override val agentId: String, val text: String) : AgentEvent()
    data class Failure(override val id: Long, override val agentId: String, val message: String) : AgentEvent()
}

enum class AgentRunStatus { RUNNING, DONE, FAILED, CANCELLED }

data class AgentRunState(
    val runId: String,
    val goal: String,
    val title: String,
    val isTeam: Boolean,
    val nodes: List<AgentNode>,
    val events: List<AgentEvent> = emptyList(),
    val plan: List<PlanStep> = emptyList(),
    /** Índice del paso del plan en curso (-1 = planificando). */
    val currentStep: Int = -1,
    val status: AgentRunStatus = AgentRunStatus.RUNNING,
    val finalAnswer: String = "",
    val error: String? = null,
    val startedAt: Long = System.currentTimeMillis(),
    val finishedAt: Long? = null,
    val llmCalls: Int = 0,
    val toolCalls: Int = 0,
    /** Lo que el modelo está escribiendo ahora mismo; vacío si no hay nada que mostrar. */
    val liveText: String = "",
    /** Archivos que los agentes han dejado en el Editor de código durante esta ejecución. */
    val savedFiles: List<String> = emptyList()
) {
    val isRunning: Boolean get() = status == AgentRunStatus.RUNNING
}
