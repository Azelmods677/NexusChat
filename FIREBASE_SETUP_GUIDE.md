# 🔥 Firebase Setup Guide - Nexus Chat

## 📋 Tabla de Contenidos
1. [Reglas de Firebase Realtime Database](#reglas-de-firebase-realtime-database)
2. [Configuración de Google Sign-In](#configuración-de-google-sign-in)
3. [Troubleshooting](#troubleshooting)

---

## 🗄️ Reglas de Firebase Realtime Database

### ✅ Reglas Actualizadas (Copiar y Pegar)

Ve a **Firebase Console** → **Realtime Database** → **Rules** y pega esto:

```json
{
  "rules": {
    ".read": "auth != null",
    ".write": "auth != null",
    
    "users": {
      "$uid": {
        ".read": "auth != null",
        ".write": "auth != null && auth.uid == $uid"
      }
    },
    
    "chats": {
      "$chatId": {
        ".read": "auth != null",
        ".write": "auth != null"
      }
    },
    
    "messages": {
      "$chatId": {
        ".read": "auth != null",
        ".write": "auth != null"
      }
    },
    
    "stories": {
      ".read": "auth != null",
      "$storyId": {
        ".write": "auth != null && auth.uid == data.child('userId').val()",
        "views": {
          ".write": "auth != null"
        },
        "reactions": {
          ".write": "auth != null"
        }
      }
    },
    
    "storyReplies": {
      "$storyOwnerId": {
        ".read": "auth != null",
        "$storyId": {
          ".write": "auth != null"
        }
      }
    },
    
    "aiChats": {
      "$userId": {
        ".read": "auth != null && auth.uid == $userId",
        ".write": "auth != null && auth.uid == $userId"
      }
    },
    
    "terminal": {
      "$userId": {
        ".read": "auth != null && auth.uid == $userId",
        ".write": "auth != null && auth.uid == $userId"
      }
    },
    
    "codeFiles": {
      "$userId": {
        ".read": "auth != null && auth.uid == $userId",
        ".write": "auth != null && auth.uid == $userId"
      }
    }
  }
}
```

### 📝 Explicación de las Reglas

| Nodo | Lectura | Escritura | Descripción |
|------|---------|-----------|-------------|
| `users/$uid` | Cualquier usuario autenticado | Solo el dueño | Perfiles de usuario |
| `chats/$chatId` | Cualquier usuario autenticado | Cualquier usuario autenticado | Conversaciones |
| `messages/$chatId` | Cualquier usuario autenticado | Cualquier usuario autenticado | Mensajes de chat |
| `stories/$storyId` | Cualquier usuario autenticado | Solo el creador | Historias |
| `stories/$storyId/views` | Cualquier usuario autenticado | Cualquier usuario autenticado | Vistas de historias |
| `stories/$storyId/reactions` | Cualquier usuario autenticado | Cualquier usuario autenticado | Reacciones a historias |
| `storyReplies/$ownerId/$storyId` | Cualquier usuario autenticado | Cualquier usuario autenticado | **Respuestas a historias** |
| `aiChats/$userId` | Solo el dueño | Solo el dueño | Chats con IA |
| `terminal/$userId` | Solo el dueño | Solo el dueño | Historial de terminal |
| `codeFiles/$userId` | Solo el dueño | Solo el dueño | Archivos de código |

---

## 🔐 Configuración de Google Sign-In

### ✅ Configuración Actual

**Web Client ID configurado:**
```
341817574378-68483linucnkehkl4b6aiafba94vs0f5.apps.googleusercontent.com
```

**Ubicación en el código:**
- `app/src/main/res/values/strings.xml` → `default_web_client_id`

### 🔧 Pasos para Verificar/Corregir

#### 1️⃣ Obtener el SHA-1 de tu keystore

Abre terminal en la raíz del proyecto y ejecuta:

```bash
./gradlew signingReport
```

**Busca esta sección:**
```
Variant: debug
Config: debug
Store: C:\Users\...\.android\debug.keystore
Alias: AndroidDebugKey
MD5: ...
SHA1: 1B:01:96:24:47:26:3E:53:C0:B3:08:7F:B9:2C:C8:75:F6:AD:84:2D
SHA-256: ...
```

**Copia el SHA-1** (ejemplo: `1B:01:96:24:47:26:3E:53:C0:B3:08:7F:B9:2C:C8:75:F6:AD:84:2D`)

#### 2️⃣ Verificar SHA-1 en Firebase Console

1. Ve a [Firebase Console](https://console.firebase.google.com/)
2. Selecciona tu proyecto **nexuschat-d3191**
3. Click en **⚙️ Project Settings**
4. Scroll down a **Your apps** → Android app
5. Verifica que el **SHA certificate fingerprints** incluya el SHA-1 del paso 1

**Si NO está o es diferente:**
- Click en **Add fingerprint**
- Pega el SHA-1 del paso 1
- Click **Save**

#### 3️⃣ Descargar google-services.json actualizado

1. En la misma página de **Project Settings**
2. Click en **Download google-services.json**
3. Reemplaza el archivo en: `app/google-services.json`

#### 4️⃣ Verificar Web Client ID

1. Firebase Console → **Authentication** → **Sign-in method**
2. Click en **Google** (debe estar habilitado)
3. En **Web SDK configuration**, verifica el **Web client ID**

**Si es diferente al configurado:**

Edita `app/src/main/res/values/strings.xml`:

```xml
<string name="default_web_client_id">TU_WEB_CLIENT_ID_AQUI</string>
```

#### 5️⃣ Rebuild el proyecto

```bash
./gradlew clean
./gradlew assembleDebug
```

---

## 🐛 Troubleshooting

### ❌ Error: "Invalid credential type"

**Causa:** SHA-1 no coincide o Web Client ID incorrecto

**Solución:**
1. Verifica SHA-1 (pasos 1-2 arriba)
2. Descarga nuevo `google-services.json` (paso 3)
3. Verifica Web Client ID (paso 4)
4. Rebuild (paso 5)

### ❌ Error: "Permission denied" en historias

**Causa:** Reglas de Firebase incorrectas

**Solución:**
1. Copia las reglas de la sección [Reglas de Firebase](#reglas-de-firebase-realtime-database)
2. Pégalas en Firebase Console → Realtime Database → Rules
3. Click **Publish**

### ❌ Error: "User not authenticated"

**Causa:** Usuario no ha iniciado sesión correctamente

**Solución:**
1. Verifica que Google Sign-In esté configurado correctamente
2. Revisa los logs de Android Studio para ver el error específico
3. Asegúrate de que el usuario aparezca en Firebase Console → Authentication → Users

### ❌ Error: "No registered users found"

**Causa:** No hay usuarios en la base de datos o el usuario actual no está autenticado

**Solución:**
1. Verifica que `FirebaseAuth.getInstance().currentUser` no sea null
2. Revisa Firebase Console → Realtime Database → Data → users
3. Asegúrate de que las reglas permitan lectura: `"users": { ".read": "auth != null" }`

---

## 📞 Soporte

Si sigues teniendo problemas:

1. **Revisa los logs de Android Studio** (Logcat)
2. **Busca errores específicos** en Firebase Console → Functions → Logs
3. **Verifica la estructura de datos** en Firebase Console → Realtime Database → Data

---

## ✅ Checklist de Configuración

- [ ] Reglas de Firebase Realtime Database actualizadas
- [ ] SHA-1 agregado en Firebase Console
- [ ] `google-services.json` descargado y actualizado
- [ ] Web Client ID verificado en `strings.xml`
- [ ] Proyecto rebuildeado (`./gradlew clean assembleDebug`)
- [ ] Google Sign-In habilitado en Firebase Console → Authentication
- [ ] Usuario de prueba creado y visible en Firebase Console → Authentication → Users

---

**Última actualización:** 2025-01-20
**Versión de Firebase:** 33.7.0
**Versión de la app:** 1.0.0
