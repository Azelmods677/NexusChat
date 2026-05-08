# 🤝 Guía de Contribución

¡Gracias por tu interés en contribuir a NexusChat! Este documento proporciona pautas para contribuir al proyecto.

## 📋 Tabla de Contenidos

- [Código de Conducta](#código-de-conducta)
- [Cómo Contribuir](#cómo-contribuir)
- [Reportar Bugs](#reportar-bugs)
- [Sugerir Mejoras](#sugerir-mejoras)
- [Pull Requests](#pull-requests)
- [Estilo de Código](#estilo-de-código)
- [Commits](#commits)

## 📜 Código de Conducta

Este proyecto se adhiere a un código de conducta. Al participar, se espera que mantengas este código. Por favor reporta comportamientos inaceptables.

## 🚀 Cómo Contribuir

### Reportar Bugs

Si encuentras un bug, por favor crea un issue con:

- **Título claro y descriptivo**
- **Pasos para reproducir** el problema
- **Comportamiento esperado** vs **comportamiento actual**
- **Capturas de pantalla** si es aplicable
- **Información del dispositivo**: modelo, versión de Android
- **Logs** relevantes

### Sugerir Mejoras

Para sugerir una mejora:

1. Verifica que no exista un issue similar
2. Crea un nuevo issue con la etiqueta `enhancement`
3. Describe claramente la mejora propuesta
4. Explica por qué sería útil para el proyecto

### Pull Requests

1. **Fork** el repositorio
2. **Crea una rama** desde `main`:
   ```bash
   git checkout -b feature/mi-nueva-funcionalidad
   ```
3. **Realiza tus cambios** siguiendo el estilo de código
4. **Escribe tests** si es aplicable
5. **Commit** tus cambios con mensajes descriptivos
6. **Push** a tu fork:
   ```bash
   git push origin feature/mi-nueva-funcionalidad
   ```
7. **Abre un Pull Request** con:
   - Descripción clara de los cambios
   - Referencias a issues relacionados
   - Capturas de pantalla si hay cambios visuales

## 💻 Estilo de Código

### Kotlin

- Sigue las [convenciones de Kotlin](https://kotlinlang.org/docs/coding-conventions.html)
- Usa nombres descriptivos para variables y funciones
- Documenta funciones públicas con KDoc
- Mantén funciones pequeñas y enfocadas
- Usa `val` en lugar de `var` cuando sea posible

### Jetpack Compose

- Componentes reutilizables en archivos separados
- Usa `remember` y `rememberSaveable` apropiadamente
- Evita side effects en composables
- Usa `LaunchedEffect` para operaciones asíncronas
- Mantén el estado elevado cuando sea necesario

### Arquitectura

- Respeta la Clean Architecture del proyecto
- Separa lógica de negocio de la UI
- Usa ViewModels para gestionar estado
- Implementa repositorios para acceso a datos
- Usa casos de uso para lógica compleja

## 📝 Commits

Usa mensajes de commit descriptivos siguiendo este formato:

```
tipo(alcance): descripción breve

Descripción detallada opcional

Fixes #123
```

**Tipos de commit:**
- `feat`: Nueva funcionalidad
- `fix`: Corrección de bug
- `docs`: Cambios en documentación
- `style`: Cambios de formato (no afectan el código)
- `refactor`: Refactorización de código
- `test`: Agregar o modificar tests
- `chore`: Tareas de mantenimiento

**Ejemplos:**
```
feat(chat): agregar soporte para mensajes de voz

fix(stories): corregir crash al cargar imágenes grandes

docs(readme): actualizar instrucciones de instalación
```

## 🧪 Testing

- Escribe tests unitarios para nueva lógica de negocio
- Verifica que los tests existentes pasen
- Prueba en diferentes dispositivos y versiones de Android

## 📦 Dependencias

- Justifica la adición de nuevas dependencias
- Usa versiones estables cuando sea posible
- Actualiza el README si agregas dependencias importantes

## 🔍 Code Review

- Sé respetuoso y constructivo
- Explica el "por qué" de tus sugerencias
- Acepta feedback de manera positiva
- Responde a comentarios en tiempo razonable

## ❓ Preguntas

Si tienes preguntas, puedes:

- Abrir un issue con la etiqueta `question`
- Contactar al mantenedor del proyecto

---

¡Gracias por contribuir a NexusChat! 🎉
