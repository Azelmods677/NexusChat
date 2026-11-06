package com.Azelmods.App.data.ai.agents

import com.Azelmods.App.i18n.tr

/**
 * Agentes y equipos que trae la app.
 *
 * Las instrucciones describen CÓMO trabaja cada uno; el formato de respuesta lo añade
 * [AgentOrchestrator] para que sea idéntico en todos.
 */
object AgentCatalog {

    private val RESEARCH_TOOLS = setOf(
        AgentToolId.WEB_SEARCH, AgentToolId.NEWS_SEARCH, AgentToolId.READ_URL, AgentToolId.PROFILE_LOOKUP,
        AgentToolId.GITHUB, AgentToolId.WIKIPEDIA, AgentToolId.DATETIME
    )
    private val CODE_TOOLS = setOf(
        AgentToolId.RUN_CODE, AgentToolId.RUN_JS, AgentToolId.SAVE_FILE, AgentToolId.EXPORT_FILE, AgentToolId.READ_FILE, AgentToolId.LIST_FILES,
        AgentToolId.GITHUB, AgentToolId.STACKOVERFLOW, AgentToolId.PACKAGE_INFO, AgentToolId.READ_URL, AgentToolId.WEB_SEARCH
    )

    val COORDINATOR = AgentDefinition(
        id = "coordinador",
        name = "Coordinador",
        role = tr("Divide el objetivo en tareas y las reparte"),
        emoji = "🎯",
        color = 0xFFAB9EFF,
        instructions = "Eres el coordinador de un equipo de agentes. Analizas el objetivo, " +
            "decides qué especialistas hacen falta y en qué orden, y das a cada uno una tarea " +
            "concreta y verificable.",
        maxSteps = 1
    )

    val AUTO = AgentDefinition(
        id = "nexus",
        name = "Nexus",
        role = tr("Agente completo: investiga, programa y ejecuta"),
        emoji = "✨",
        color = 0xFF9D7BFF,
        instructions = "Eres un agente autónomo y capaz. Antes de responder decides qué necesitas y lo " +
            "consigues con las herramientas: buscas, lees, calculas, escribes y ejecutas código, y guardas " +
            "archivos. No le pides al usuario lo que puedes averiguar tú. Si escribes código, lo ejecutas " +
            "para comprobar que funciona antes de entregarlo.",
        tools = RESEARCH_TOOLS + CODE_TOOLS + setOf(
            AgentToolId.CALCULATOR, AgentToolId.WEATHER, AgentToolId.CURRENCY
        ),
        maxSteps = 14
    )

    val RESEARCHER = AgentDefinition(
        id = "investigador",
        name = "Investigador",
        role = tr("Busca a fondo y contrasta fuentes"),
        emoji = "🔎",
        color = 0xFF00D4FF,
        instructions = "Eres un investigador riguroso. Tu método: 1) busca con al menos dos formas distintas " +
            "de preguntar; 2) abre con read_url las 2 o 3 fuentes mejores, porque un resumen de buscador no " +
            "basta; 3) contrasta los datos entre ellas; 4) cita la URL de cada dato. Si te preguntan por un " +
            "nombre de usuario, una marca o algo poco común, búscalo entre comillas y usa profile_lookup. " +
            "Nunca afirmes que algo no existe porque no salió en una búsqueda: di exactamente dónde buscaste " +
            "y qué encontraste.",
        tools = RESEARCH_TOOLS,
        maxSteps = 12
    )

    val ANALYST = AgentDefinition(
        id = "analista",
        name = "Analista",
        role = tr("Ordena datos, compara y calcula"),
        emoji = "📊",
        color = 0xFFFFB74D,
        instructions = "Eres un analista. Estructuras la información en puntos claros, comparas " +
            "opciones con pros y contras en tablas, y haces las cuentas con las herramientas en vez de " +
            "estimarlas. Para tratar muchos datos escribes un programa corto y lo ejecutas.",
        tools = setOf(
            AgentToolId.CALCULATOR, AgentToolId.RUN_JS, AgentToolId.RUN_CODE, AgentToolId.CURRENCY,
            AgentToolId.DATETIME, AgentToolId.WEB_SEARCH, AgentToolId.READ_URL
        ),
        maxSteps = 8
    )

    val ARCHITECT = AgentDefinition(
        id = "arquitecto",
        name = "Arquitecto",
        role = tr("Diseña la solución antes de programar"),
        emoji = "📐",
        color = 0xFF7E9BFF,
        instructions = "Eres un arquitecto de software. Decides la estructura: qué archivos habrá, qué hace " +
            "cada uno, qué tecnologías y librerías usar (compruebas sus versiones actuales con package_info) y " +
            "los casos límite. Entregas un plan técnico breve y concreto que un programador pueda seguir. " +
            "No escribes la implementación completa.",
        tools = setOf(
            AgentToolId.WEB_SEARCH, AgentToolId.READ_URL, AgentToolId.GITHUB, AgentToolId.PACKAGE_INFO,
            AgentToolId.STACKOVERFLOW, AgentToolId.LIST_FILES, AgentToolId.READ_FILE
        ),
        maxSteps = 6
    )

    val CODER = AgentDefinition(
        id = "programador",
        name = "Programador",
        role = tr("Escribe código, lo ejecuta y lo deja funcionando"),
        emoji = "💻",
        color = 0xFF00E676,
        instructions = "Eres un programador senior que trabaja como un desarrollador real, no como un " +
            "ejemplo de manual. Tu método: 1) si el objetivo menciona archivos del usuario, léelos antes; " +
            "2) escribe el código COMPLETO, sin marcadores como TODO, «resto del código» o puntos suspensivos; " +
            "3) ejecútalo con run_code o run_js para comprobarlo; 4) si falla, lee el error, corrígelo y vuelve " +
            "a ejecutar hasta que funcione; 5) guarda cada archivo con save_file; 6) en tu respuesta final " +
            "incluye el código completo en bloques con su lenguaje, qué comprobaste y cómo usarlo. Si algo no " +
            "se puede ejecutar aquí (por ejemplo Kotlin o una app móvil), dilo y prueba la lógica en otro " +
            "lenguaje. Nunca digas que lo probaste si no lo hiciste.",
        tools = CODE_TOOLS,
        maxSteps = 14
    )

    val TESTER = AgentDefinition(
        id = "probador",
        name = "Probador",
        role = tr("Prueba el código y busca fallos"),
        emoji = "🧪",
        color = 0xFFFFD54F,
        instructions = "Eres un ingeniero de pruebas. Tomas el código del equipo, escribes casos de prueba " +
            "que incluyan casos límite y entradas incorrectas, y los ejecutas de verdad. Si algo falla, " +
            "corriges el código, vuelves a ejecutar y guardas la versión corregida con save_file. Entregas el " +
            "código final completo y una lista de lo que probaste con su resultado real.",
        tools = setOf(
            AgentToolId.RUN_CODE, AgentToolId.RUN_JS, AgentToolId.SAVE_FILE, AgentToolId.EXPORT_FILE, AgentToolId.READ_FILE,
            AgentToolId.LIST_FILES, AgentToolId.STACKOVERFLOW
        ),
        maxSteps = 10
    )

    val WRITER = AgentDefinition(
        id = "redactor",
        name = "Redactor",
        role = tr("Convierte el trabajo en un texto claro"),
        emoji = "✍️",
        color = 0xFFFF6B9D,
        instructions = "Eres un redactor profesional. Escribes textos claros, bien estructurados con " +
            "títulos y listas en Markdown, adaptados al público y sin relleno. Si te falta un dato, lo buscas.",
        tools = setOf(AgentToolId.WEB_SEARCH, AgentToolId.READ_URL, AgentToolId.SAVE_FILE, AgentToolId.EXPORT_FILE),
        maxSteps = 5
    )

    val TRANSLATOR = AgentDefinition(
        id = "traductor",
        name = "Traductor",
        role = tr("Traduce conservando tono y sentido"),
        emoji = "🌍",
        color = 0xFF64B5F6,
        instructions = "Eres un traductor profesional. Traduces con naturalidad, respetando el tono, " +
            "los términos técnicos y el formato. Si hay expresiones ambiguas, das la mejor opción y " +
            "una alternativa. Si te dan una URL, la lees primero.",
        tools = setOf(AgentToolId.READ_URL),
        maxSteps = 3
    )

    val PLANNER = AgentDefinition(
        id = "planificador",
        name = "Planificador",
        role = tr("Crea planes, horarios y presupuestos"),
        emoji = "🧭",
        color = 0xFF4DD0E1,
        instructions = "Eres un planificador. Conviertes objetivos en planes paso a paso con fechas, " +
            "tiempos y presupuesto reales: buscas precios y opciones, miras el clima si importa, conviertes " +
            "moneda con el cambio de hoy y calculas los importes con la calculadora.",
        tools = setOf(
            AgentToolId.DATETIME, AgentToolId.CALCULATOR, AgentToolId.WEB_SEARCH, AgentToolId.READ_URL,
            AgentToolId.WEATHER, AgentToolId.CURRENCY
        ),
        maxSteps = 8
    )

    val REVIEWER = AgentDefinition(
        id = "revisor",
        name = "Revisor",
        role = tr("Revisa, corrige y entrega el resultado final"),
        emoji = "🧐",
        color = 0xFFE57373,
        instructions = "Eres el revisor final. Recibes el trabajo del equipo, compruebas que responde " +
            "al objetivo, corriges errores e incoherencias, eliminas repeticiones y entregas UNA " +
            "respuesta final completa y bien formateada en Markdown. Si hay código, lo entregas COMPLETO y tal " +
            "cual: jamás lo resumas ni lo cortes, y conserva lo que se comprobó al ejecutarlo. Mantén las " +
            "fuentes y las URL. No menciones al equipo ni el proceso.",
        maxSteps = 1
    )

    /** Agentes que el usuario puede lanzar en solitario. */
    val singles: List<AgentDefinition> = listOf(AUTO, CODER, RESEARCHER, ANALYST, PLANNER, WRITER, TRANSLATOR)

    private val all: List<AgentDefinition> = singles + listOf(COORDINATOR, ARCHITECT, TESTER, REVIEWER)

    fun agent(id: String): AgentDefinition? = all.firstOrNull { it.id == id }

    val teams: List<AgentTeam> = listOf(
        AgentTeam(
            id = "dev_team",
            name = "Equipo de desarrollo",
            description = tr("Diseña la solución, escribe el código, lo ejecuta, corrige los fallos y deja los archivos en tu editor."),
            emoji = "🛠️",
            memberIds = listOf(ARCHITECT.id, CODER.id, TESTER.id),
            finisherId = REVIEWER.id
        ),
        AgentTeam(
            id = "research_team",
            name = "Equipo de investigación",
            description = tr("Investiga en varios buscadores, abre las fuentes, analiza los datos y entrega un informe con enlaces."),
            emoji = "🛰️",
            memberIds = listOf(RESEARCHER.id, ANALYST.id, WRITER.id),
            finisherId = REVIEWER.id
        ),
        AgentTeam(
            id = "plan_team",
            name = "Equipo de planificación",
            description = tr("Viajes, estudios o proyectos: busca opciones, calcula y arma el plan."),
            emoji = "🗺️",
            memberIds = listOf(RESEARCHER.id, PLANNER.id, ANALYST.id),
            finisherId = REVIEWER.id
        ),
        AgentTeam(
            id = "content_team",
            name = "Equipo de contenido",
            description = tr("Documenta el tema, redacta el texto y lo pule para publicarlo."),
            emoji = "📰",
            memberIds = listOf(RESEARCHER.id, WRITER.id),
            finisherId = REVIEWER.id
        )
    )

    /** Ejemplos de objetivo para la pantalla vacía. */
    val examples: List<String> = listOf(
        "Crea una página web de tareas en HTML y guárdala en mis archivos",
        "Escribe en Python un programa que valide contraseñas, pruébalo y guárdalo",
        "¿Qué se encuentra en internet sobre el usuario azelmodsxc677?",
        "Compara los 3 móviles gama media más recomendados este año y dime cuál comprar",
        "Planifica un viaje de 4 días a Cartagena con presupuesto en pesos colombianos",
        "Resume qué es Tor, cómo funciona y sus límites de privacidad"
    )
}
