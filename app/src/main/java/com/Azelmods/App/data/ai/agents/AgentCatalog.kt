package com.Azelmods.App.data.ai.agents

/**
 * Agentes y equipos que trae la app.
 *
 * Las instrucciones describen CÓMO trabaja cada uno; el formato de respuesta
 * (JSON con herramientas) lo añade [AgentOrchestrator] para que sea idéntico en
 * todos.
 */
object AgentCatalog {

    val COORDINATOR = AgentDefinition(
        id = "coordinador",
        name = "Coordinador",
        role = "Divide el objetivo en tareas y las reparte",
        emoji = "🎯",
        color = 0xFFAB9EFF,
        instructions = "Eres el coordinador de un equipo de agentes. Analizas el objetivo, " +
            "decides qué especialistas hacen falta y en qué orden, y das a cada uno una tarea " +
            "concreta y verificable.",
        maxSteps = 1
    )

    val RESEARCHER = AgentDefinition(
        id = "investigador",
        name = "Investigador",
        role = "Busca y contrasta información actual",
        emoji = "🔎",
        color = 0xFF00D4FF,
        instructions = "Eres un investigador riguroso. Buscas información, la contrastas en más de " +
            "una fuente cuando es posible y citas de dónde sale cada dato (nombre de la fuente o URL). " +
            "Si algo no se puede verificar, lo dices.",
        tools = setOf(AgentToolId.WEB_SEARCH, AgentToolId.WIKIPEDIA, AgentToolId.READ_URL, AgentToolId.DATETIME),
        maxSteps = 5
    )

    val ANALYST = AgentDefinition(
        id = "analista",
        name = "Analista",
        role = "Ordena datos, compara y calcula",
        emoji = "📊",
        color = 0xFFFFB74D,
        instructions = "Eres un analista. Estructuras la información en puntos claros, comparas " +
            "opciones con pros y contras, y haces las cuentas con la calculadora en vez de estimarlas.",
        tools = setOf(AgentToolId.CALCULATOR, AgentToolId.DATETIME),
        maxSteps = 4
    )

    val CODER = AgentDefinition(
        id = "programador",
        name = "Programador",
        role = "Escribe y explica código",
        emoji = "💻",
        color = 0xFF00E676,
        instructions = "Eres un programador senior. Escribes código correcto, legible y completo, en " +
            "bloques de código con el lenguaje indicado, y explicas brevemente las decisiones. " +
            "Señalas casos límite y cómo probar el código.",
        tools = setOf(AgentToolId.CALCULATOR, AgentToolId.READ_URL),
        maxSteps = 3
    )

    val WRITER = AgentDefinition(
        id = "redactor",
        name = "Redactor",
        role = "Convierte el trabajo en un texto claro",
        emoji = "✍️",
        color = 0xFFFF6B9D,
        instructions = "Eres un redactor profesional. Escribes textos claros, bien estructurados con " +
            "títulos y listas en Markdown, adaptados al público y sin relleno.",
        maxSteps = 1
    )

    val TRANSLATOR = AgentDefinition(
        id = "traductor",
        name = "Traductor",
        role = "Traduce conservando tono y sentido",
        emoji = "🌍",
        color = 0xFF64B5F6,
        instructions = "Eres un traductor profesional. Traduces con naturalidad, respetando el tono, " +
            "los términos técnicos y el formato. Si hay expresiones ambiguas, das la mejor opción y " +
            "una alternativa.",
        maxSteps = 1
    )

    val PLANNER = AgentDefinition(
        id = "planificador",
        name = "Planificador",
        role = "Crea planes, horarios y presupuestos",
        emoji = "🧭",
        color = 0xFF4DD0E1,
        instructions = "Eres un planificador. Conviertes objetivos en planes paso a paso con fechas, " +
            "tiempos y presupuesto. Usas la fecha actual para calcular plazos y la calculadora para " +
            "los importes.",
        tools = setOf(AgentToolId.DATETIME, AgentToolId.CALCULATOR, AgentToolId.WEB_SEARCH),
        maxSteps = 4
    )

    val REVIEWER = AgentDefinition(
        id = "revisor",
        name = "Revisor",
        role = "Revisa, corrige y entrega el resultado final",
        emoji = "🧐",
        color = 0xFFE57373,
        instructions = "Eres el revisor final. Recibes el trabajo del equipo, compruebas que responde " +
            "al objetivo, corriges errores e incoherencias, eliminas repeticiones y entregas UNA " +
            "respuesta final completa y bien formateada en Markdown. No menciones al equipo ni el proceso.",
        maxSteps = 1
    )

    /** Agentes que el usuario puede lanzar en solitario. */
    val singles: List<AgentDefinition> = listOf(RESEARCHER, CODER, ANALYST, PLANNER, WRITER, TRANSLATOR)

    private val all: List<AgentDefinition> = singles + listOf(COORDINATOR, REVIEWER)

    fun agent(id: String): AgentDefinition? = all.firstOrNull { it.id == id }

    val teams: List<AgentTeam> = listOf(
        AgentTeam(
            id = "research_team",
            name = "Equipo de investigación",
            description = "Investiga en la web, analiza los datos y entrega un informe revisado.",
            emoji = "🛰️",
            memberIds = listOf(RESEARCHER.id, ANALYST.id, WRITER.id),
            finisherId = REVIEWER.id
        ),
        AgentTeam(
            id = "dev_team",
            name = "Equipo de desarrollo",
            description = "Diseña la solución, escribe el código y lo revisa antes de entregarlo.",
            emoji = "🛠️",
            memberIds = listOf(RESEARCHER.id, CODER.id),
            finisherId = REVIEWER.id
        ),
        AgentTeam(
            id = "plan_team",
            name = "Equipo de planificación",
            description = "Viajes, estudios o proyectos: busca opciones, calcula y arma el plan.",
            emoji = "🗺️",
            memberIds = listOf(RESEARCHER.id, PLANNER.id, ANALYST.id),
            finisherId = REVIEWER.id
        ),
        AgentTeam(
            id = "content_team",
            name = "Equipo de contenido",
            description = "Documenta el tema, redacta el texto y lo pule para publicarlo.",
            emoji = "📰",
            memberIds = listOf(RESEARCHER.id, WRITER.id),
            finisherId = REVIEWER.id
        )
    )

    /** Ejemplos de objetivo para la pantalla vacía. */
    val examples: List<String> = listOf(
        "Compara los 3 móviles gama media más recomendados este año y dime cuál comprar",
        "Planifica un viaje de 4 días a Lisboa con 600 € de presupuesto",
        "Escribe una función en Kotlin que valide un IBAN español y explícala",
        "Resume qué es Tor, cómo funciona y sus límites de privacidad",
        "Calcula la cuota mensual de un préstamo de 12.000 € al 7 % a 4 años"
    )
}
