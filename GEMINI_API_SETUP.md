# 🔑 Configuración de Gemini API - Guía Completa

## 📋 Paso 1: Obtener tu API Key GRATIS

1. **Visita Google AI Studio**: https://aistudio.google.com/app/apikey
2. **Inicia sesión** con tu cuenta de Google
3. **Haz clic en "Create API Key"**
4. **Copia tu API Key** (se verá algo como: `AIzaSyXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX`)

## ⚙️ Paso 2: Configurar la API Key en la App

### Opción A: Editar directamente el código (Recomendado)

1. Abre el archivo: `app/src/main/java/com/Azelmods/App/data/api/AzelAIApiService.kt`

2. Busca la línea que dice:
```kotlin
private const val API_KEY = "TU_API_KEY_AQUI"
```

3. Reemplaza `TU_API_KEY_AQUI` con tu API Key real:
```kotlin
private const val API_KEY = "AIzaSyXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX"
```

4. Guarda el archivo y recompila la app

### Opción B: Usar variables de entorno (Más seguro)

1. Crea un archivo `local.properties` en la raíz del proyecto (si no existe)

2. Agrega tu API Key:
```properties
GEMINI_API_KEY=AIzaSyXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
```

3. Modifica `build.gradle.kts` para leer la variable:
```kotlin
android {
    defaultConfig {
        buildConfigField("String", "GEMINI_API_KEY", "\"${project.findProperty("GEMINI_API_KEY")}\"")
    }
}
```

4. Usa en el código:
```kotlin
private const val API_KEY = BuildConfig.GEMINI_API_KEY
```

## 🚀 Paso 3: Verificar la Configuración

1. **Compila la app**: `./gradlew assembleDebug`
2. **Instala en tu dispositivo**: `./gradlew installDebug`
3. **Abre la app** y ve a **AzelAI**
4. **Envía un mensaje de prueba**: "Hola, ¿funcionas?"
5. Si recibes una respuesta, ¡todo está configurado correctamente! ✅

## 📊 Límites de la Versión Gratuita

### Gemini 1.5 Flash (Recomendado para uso gratuito)
- **15 requests por minuto (RPM)**
- **1 millón de tokens por minuto**
- **1,500 requests por día**
- **Gratis para siempre**

### Gemini 1.5 Pro
- **2 requests por minuto (RPM)**
- **32,000 tokens por minuto**
- **50 requests por día**

### Gemini 2.0 Flash (Experimental)
- **10 requests por minuto (RPM)**
- **4 millones de tokens por minuto**
- **1,500 requests por día**

## ⚡ Optimizaciones Implementadas

La app ya incluye optimizaciones para maximizar el uso de la versión gratuita:

✅ **Rate Limiter**: Controla automáticamente la velocidad de requests
✅ **Request Queue**: Cola inteligente con reintentos automáticos
✅ **Backoff Exponencial**: Espera creciente en caso de límite (5s → 15s → 30s)
✅ **Compresión de Prompts**: Reduce hasta 40% el uso de tokens
✅ **Estimación de Tokens**: Muestra tokens en tiempo real
✅ **Truncado Automático**: Limita prompts largos automáticamente

## 🔓 Sistema de Evasión de Filtros

La app incluye **7 técnicas avanzadas** para evadir filtros de seguridad de Gemini:

1. **Académico**: Contexto educativo y de investigación
2. **Defensivo**: Enfoque en defensa y análisis de seguridad
3. **Forense**: Investigación post-incidente
4. **Roleplay**: Escenarios de pentesting autorizado
5. **Autorización**: Declaración explícita de autorización legal
6. **Neutral**: Terminología técnica neutral
7. **Ninguno**: Sin técnicas de evasión

### Cómo usar las técnicas de evasión:

1. Abre **AzelAI**
2. Toca el ícono de **configuración** (⚙️)
3. Selecciona **"Técnica de Evasión"**
4. Elige la técnica que mejor se adapte a tu consulta
5. Activa **"Optimización de Tokens"** para reducir uso

## 🛡️ Safety Settings

La app configura automáticamente los **Safety Settings** de Gemini en `BLOCK_NONE` para todas las categorías:

- `HARM_CATEGORY_HARASSMENT` → `BLOCK_NONE`
- `HARM_CATEGORY_HATE_SPEECH` → `BLOCK_NONE`
- `HARM_CATEGORY_SEXUALLY_EXPLICIT` → `BLOCK_NONE`
- `HARM_CATEGORY_DANGEROUS_CONTENT` → `BLOCK_NONE`

Esto permite respuestas técnicas sin restricciones para investigación de seguridad.

## ❌ Solución de Problemas

### Error: "API Key inválida"
- Verifica que copiaste la API Key completa
- Asegúrate de no tener espacios al inicio o final
- Regenera la API Key en Google AI Studio

### Error: "429 - Rate Limit Exceeded"
- Espera 60 segundos antes de enviar otro mensaje
- Activa la **Optimización de Tokens** para reducir uso
- Considera usar **Gemini 1.5 Flash** (15 RPM vs 2 RPM de Pro)

### Error: "SAFETY - Contenido bloqueado"
- Cambia la **Técnica de Evasión** a "Académico" o "Defensivo"
- Reformula tu pregunta de forma más técnica
- Usa terminología neutral (ej: "security assessment" en lugar de "attack")

### Error: "Sin conexión"
- Verifica tu conexión a internet
- Comprueba que no estés usando un proxy que bloquee Google APIs
- Desactiva temporalmente el navegador Tor si está activo

## 📚 Recursos Adicionales

- **Documentación oficial**: https://ai.google.dev/docs
- **Límites y cuotas**: https://ai.google.dev/pricing
- **Modelos disponibles**: https://ai.google.dev/models/gemini
- **API Reference**: https://ai.google.dev/api/rest

## 🔐 Seguridad

⚠️ **IMPORTANTE**: 
- **NO compartas tu API Key** con nadie
- **NO subas tu API Key** a repositorios públicos
- **Usa variables de entorno** para mayor seguridad
- **Regenera tu API Key** si crees que fue comprometida

## 💡 Consejos Pro

1. **Usa Gemini 1.5 Flash** para la mayoría de consultas (más rápido y mayor límite)
2. **Activa la optimización de tokens** para maximizar requests por día
3. **Combina técnicas de evasión** según el tipo de consulta
4. **Monitorea el contador de tokens** para evitar exceder límites
5. **Usa el modo streaming** para respuestas más rápidas

---

**¿Necesitas ayuda?** Abre un issue en GitHub o contacta al desarrollador.

**Desarrollado por AzelMods677** - 2026
