# Política de Seguridad — NexusChat

## Versiones soportadas

| Versión | Soportada |
|---------|-----------|
| 3.0.x   | ✅        |
| < 3.0   | ❌        |

## Reportar una vulnerabilidad

Si encuentras una vulnerabilidad de seguridad, por favor **no abras un issue público**.

En su lugar, repórtala de forma privada:

- Telegram: [t.me/AzelModsx7779](https://t.me/AzelModsx7779)
- O abre un *security advisory* privado en GitHub.

Incluye, si es posible:
- Descripción de la vulnerabilidad
- Pasos para reproducirla
- Impacto potencial
- Versión afectada

Intentaremos responder en un plazo razonable y publicar un parche tan pronto como sea posible.

## Buenas prácticas en este proyecto

- **Cifrado E2EE** (Signal Protocol) para mensajes privados.
- **Claves y secretos** nunca se versionan: `google-services.json`, API keys y `local.properties` deben mantenerse fuera del control público.
- **Navegación anónima** vía Tor/Orbot con resolución DNS protegida contra fugas.
- Las dependencias se mantienen actualizadas y fijadas por versión.

## Aviso

Las funciones de investigación de seguridad incluidas (AzelAI) están destinadas a fines educativos y de pruebas autorizadas. El uso indebido es responsabilidad del usuario final.
