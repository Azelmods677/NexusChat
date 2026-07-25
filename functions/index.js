/**
 * 🔔 Nexus Chat — Firebase Cloud Functions
 *
 * ## 📦 Instalación
 *
 * 1. Instala Firebase CLI:
 *    npm install -g firebase-tools
 *
 * 2. Inicia sesión:
 *    firebase login
 *
 * 3. Desde la raíz del proyecto:
 *    cd functions
 *    npm install
 *
 * 4. Despliega:
 *    firebase deploy --only functions
 *
 * ## ⚙️ Requisitos
 * - Plan Firebase Blaze (pay-as-you-go) — necesario para Cloud Functions
 * - Firebase Admin SDK configurado automáticamente al hacer deploy
 */

// Import explícito de /v1: todas las funciones de este archivo usan la API v1
// (database.ref().onCreate(), pubsub.schedule().onRun()). A partir de
// firebase-functions v6 la raíz del paquete exporta v2, así que importar la raíz
// rompería estos handlers. La ruta /v1 existe desde v3 y funciona igual en v4, v5
// y v6, de modo que este import es estable pase lo que pase con la versión.
const functions = require("firebase-functions/v1");
const admin = require("firebase-admin");

admin.initializeApp();

const db = admin.database();

/**
 * 🔑 Obtiene los FCM tokens de un usuario.
 *
 * Los tokens se guardan en el nodo raíz `fcmTokens/{uid}`, FUERA de `/users`: la
 * lista de contactos necesita leer la colección `/users` completa y en Firebase el
 * permiso de lectura cascadea hacia abajo, así que tener ahí los tokens los habría
 * dejado al alcance de cualquier usuario autenticado.
 *
 * Se sigue leyendo también la ruta antigua (`users/{uid}/fcmTokens`) para que los
 * dispositivos registrados antes de la migración no se queden sin notificaciones:
 * cada app reescribe su token en la ruta nueva al arrancar, así que la ruta vieja
 * se vacía sola con el tiempo.
 *
 * @param {string} uid Identificador del usuario destinatario.
 * @return {Promise<string[]>} Lista de tokens sin duplicados.
 */
async function getFcmTokens(uid) {
    const paths = [`/fcmTokens/${uid}`, `/users/${uid}/fcmTokens`];
    const tokens = [];

    for (const path of paths) {
        try {
            const snapshot = await db.ref(path).once("value");
            const data = snapshot.val();
            if (data) {
                tokens.push(...Object.values(data).filter((t) => typeof t === "string"));
            }
        } catch (e) {
            console.error(`❌ Error reading FCM tokens at ${path}:`, e);
        }
    }

    return [...new Set(tokens)];
}

/**
 * 🆕 onMessageCreate — Se dispara cuando se escribe un nuevo mensaje en
 * `chats/{chatId}/messages/{messageId}`.
 *
 * Lee los datos del mensaje, obtiene el FCM token del destinatario,
 * y envía una notificación push.
 */
exports.onMessageCreate = functions.database
    .ref("/chats/{chatId}/messages/{messageId}")
    .onCreate(async (snapshot, context) => {
        const { chatId, messageId } = context.params;
        const messageData = snapshot.val();

        console.log(`📨 New message: ${chatId}/${messageId}`);

        // ── 1. Obtener remitente ──
        const senderId = messageData.senderId;
        if (!senderId) {
            console.log("⚠️ No senderId, skipping");
            return null;
        }

        // ── 2. Obtener miembros del chat ──
        let members = [];
        try {
            const chatSnapshot = await db.ref(`/chats/${chatId}/members`).once("value");
            members = Object.values(chatSnapshot.val() || {});
        } catch (e) {
            console.error("❌ Error reading chat members:", e);
            return null;
        }

        // ── 3. Encontrar destinatario (el que NO es el remitente) ──
        const recipientId = members.find((uid) => uid !== senderId);
        if (!recipientId) {
            console.log("⚠️ No recipient found, skipping");
            return null;
        }

        // ── 4. Obtener nombre del remitente ──
        let senderName = "Unknown";
        try {
            const userSnapshot = await db.ref(`/users/${senderId}/displayName`).once("value");
            senderName = userSnapshot.val() || senderName;
        } catch (e) {
            console.warn("⚠️ Could not fetch sender name:", e);
        }

        // ── 5. Obtener foto del remitente ──
        let senderPhotoUrl = null;
        try {
            const photoSnapshot = await db.ref(`/users/${senderId}/photoUrl`).once("value");
            senderPhotoUrl = photoSnapshot.val();
        } catch (e) {
            // Silently ignore
        }

        // ── 6. Construir texto de notificación ──
        const mediaType = messageData.mediaType;
        const content = messageData.content || "";

        let displayText = content;
        if (mediaType) {
            switch (mediaType) {
                case "IMAGE":
                    displayText = "📷 Photo";
                    break;
                case "VIDEO":
                    displayText = "🎥 Video";
                    break;
                case "AUDIO":
                    displayText = "🎤 Voice message";
                    break;
                case "DOCUMENT":
                    displayText = "📄 Document";
                    break;
                case "LOCATION":
                    displayText = "📍 Location";
                    break;
                case "STICKER":
                    displayText = "Sticker";
                    break;
            }
        }

        // ── 7. Obtener FCM tokens del destinatario ──
        const fcmTokens = await getFcmTokens(recipientId);

        if (fcmTokens.length === 0) {
            console.log(`⚠️ No FCM tokens for user ${recipientId}`);
            return null;
        }

        console.log(`📱 Sending push to ${fcmTokens.length} device(s) for user ${recipientId}`);

        // ── 8. Construir payload de datos ──
        const dataPayload = {
            type: "message",
            chatId: chatId,
            senderId: senderId,
            senderName: senderName,
            body: displayText,
        };
        if (senderPhotoUrl) dataPayload.senderPhotoUrl = senderPhotoUrl;
        if (mediaType) dataPayload.mediaType = mediaType;

        // ── 9. Enviar FCM a cada token ──
        const results = await Promise.allSettled(
            fcmTokens.map((token) =>
                admin.messaging().send({
                    token: token,
                    notification: {
                        title: senderName,
                        body: displayText,
                    },
                    data: dataPayload,
                    android: {
                        priority: "high",
                        notification: {
                            channelId: "nexus_messages",
                            icon: "ic_notification",
                            color: "#7B5CFA",
                            sound: "default",
                            priority: "high",
                            clickAction: "OPEN_CHAT",
                        },
                    },
                    apns: {
                        payload: {
                            aps: {
                                sound: "default",
                                badge: 1,
                                "content-available": 1,
                            },
                        },
                    },
                })
            )
        );

        const successCount = results.filter((r) => r.status === "fulfilled").length;
        const failCount = results.filter((r) => r.status === "rejected").length;

        console.log(`✅ Push sent: ${successCount} success, ${failCount} failed`);

        return { successCount, failCount };
    });

/**
 * 🆕 onStoryCreate — Notifica contactos cuando se publica una historia nueva.
 */
exports.onStoryCreate = functions.database
    .ref("/stories/{storyId}")
    .onCreate(async (snapshot, context) => {
        const { storyId } = context.params;
        const storyData = snapshot.val();

        const userId = storyData.userId;
        if (!userId) return null;

        // Obtener nombre del usuario
        let userName = "Someone";
        try {
            const userSnapshot = await db.ref(`/users/${userId}/displayName`).once("value");
            userName = userSnapshot.val() || userName;
        } catch (e) {
            // ignore
        }

        // Notificar a todos los contactos (simplificado)
        // En producción, filtrar solo contactos del usuario
        console.log(`📖 New story by ${userName}: ${storyId}`);

        return null; // Implementación completa según necesidades
    });

/**
 * 🆕 onStoryReaction — Push al autor cuando alguien reacciona a su historia.
 * Ruta: stories_reactions/{ownerId}/{storyId}/{reactorId} = { emoji, timestamp }
 * onCreate: solo la primera reacción de cada usuario notifica (cambiar el
 * emoji sobrescribe el nodo y no dispara onCreate → sin spam).
 */
exports.onStoryReaction = functions.database
    .ref("/stories_reactions/{ownerId}/{storyId}/{reactorId}")
    .onCreate(async (snapshot, context) => {
        const { ownerId, storyId, reactorId } = context.params;
        const emoji = (snapshot.val() || {}).emoji || "❤️";

        if (ownerId === reactorId) return null; // reacción propia: sin push

        // Nombre de quien reacciona
        let reactorName = "Alguien";
        try {
            const nameSnap = await db.ref(`/users/${reactorId}/displayName`).once("value");
            reactorName = nameSnap.val() || reactorName;
        } catch (e) {
            console.warn("⚠️ Could not fetch reactor name:", e);
        }

        // Tokens FCM del autor de la historia
        const fcmTokens = await getFcmTokens(ownerId);
        if (fcmTokens.length === 0) {
            console.log(`⚠️ No FCM tokens for story owner ${ownerId}`);
            return null;
        }

        const body = `${reactorName} reaccionó ${emoji} a tu historia`;
        const results = await Promise.allSettled(
            fcmTokens.map((token) =>
                admin.messaging().send({
                    token: token,
                    notification: {
                        title: "Tu historia",
                        body: body,
                    },
                    data: {
                        type: "story_reaction",
                        storyId: storyId,
                        reactorId: reactorId,
                        emoji: emoji,
                    },
                    android: {
                        priority: "high",
                        notification: {
                            channelId: "nexus_messages",
                            icon: "ic_notification",
                            color: "#7B5CFA",
                            sound: "default",
                        },
                    },
                })
            )
        );

        const successCount = results.filter((r) => r.status === "fulfilled").length;
        const failCount = results.filter((r) => r.status === "rejected").length;
        console.log(`✅ Story-reaction push: ${successCount} success, ${failCount} failed`);
        return { successCount, failCount };
    });

/**
 * 🆕 onCallEnded — Notifica llamadas perdidas.
 */
exports.onCallEnded = functions.database
    .ref("/calls/{callId}")
    .onUpdate(async (change, context) => {
        const { callId } = context.params;
        const before = change.before.val();
        const after = change.after.val();

        // Solo notificar si la llamada pasó a estado ENDED sin responder.
        // El cliente usa "CALLING" (y a veces "RINGING") mientras suena; antes
        // solo se comprobaba "RINGING", por lo que la notificación de llamada
        // perdida nunca se enviaba.
        const wasRinging = before?.status === "CALLING" || before?.status === "RINGING";
        if (!wasRinging || after?.status !== "ENDED") return null;

        const callerId = after.callerId;
        const receiverId = after.receiverId;

        // Encontrar quién no respondió (el que no colgó)
        const missedUserId = after.endedBy === callerId ? receiverId : callerId;

        let callerName = "Someone";
        try {
            const userSnapshot = await db.ref(`/users/${callerId}/displayName`).once("value");
            callerName = userSnapshot.val() || callerName;
        } catch (e) {
            // ignore
        }

        // Obtener FCM tokens del usuario que perdió la llamada
        const fcmTokens = await getFcmTokens(missedUserId);

        for (const token of fcmTokens) {
            try {
                await admin.messaging().send({
                    token: token,
                    notification: {
                        title: "Missed call",
                        body: `From ${callerName}`,
                    },
                    data: {
                        type: "missed_call",
                        callId: callId,
                        callerId: callerId,
                        callerName: callerName,
                    },
                    android: {
                        priority: "high",
                        notification: {
                            channelId: "nexus_missed_calls",
                        },
                    },
                });
            } catch (e) {
                console.error(`❌ Failed to send missed call notification:`, e);
            }
        }

        return null;
    });

/**
 * 🆕 onCallCreate — Se dispara cuando se crea una llamada nueva en
 * `calls/{callId}`. Envía al RECEPTOR una notificación push de tipo
 * "incoming_call" para que su dispositivo muestre la pantalla de llamada
 * entrante (full-screen) aunque la app esté en segundo plano.
 *
 * IMPORTANTE: se envía como mensaje DATA-ONLY (sin bloque `notification`)
 * y con prioridad alta, para que `onMessageReceived` se ejecute siempre y
 * la app construya la notificación de llamada (full-screen intent).
 * Si se incluyera un bloque `notification`, en segundo plano la mostraría
 * el sistema y la app nunca abriría la IncomingCallScreen.
 */
exports.onCallCreate = functions.database
    .ref("/calls/{callId}")
    .onCreate(async (snapshot, context) => {
        const { callId } = context.params;
        const callData = snapshot.val();

        if (!callData) {
            console.log("⚠️ No call data, skipping");
            return null;
        }

        // Solo notificar llamadas que están iniciando.
        const status = callData.status;
        if (status && status !== "CALLING" && status !== "RINGING") {
            console.log(`⚠️ Call ${callId} status is ${status}, skipping incoming push`);
            return null;
        }

        const receiverId = callData.receiverId;
        const callerId = callData.callerId;
        if (!receiverId) {
            console.log("⚠️ No receiverId, skipping");
            return null;
        }

        const callerName = callData.callerName || "Unknown";
        const callerPhotoUrl = callData.callerPhotoUrl || "";
        // El cliente guarda callType como "AUDIO" / "VIDEO".
        const callType = callData.callType || "AUDIO";

        // ── Obtener FCM tokens del receptor ──
        const fcmTokens = await getFcmTokens(receiverId);

        if (fcmTokens.length === 0) {
            console.log(`⚠️ No FCM tokens for receiver ${receiverId}`);
            return null;
        }

        console.log(`📞 Sending incoming-call push to ${fcmTokens.length} device(s) for ${receiverId}`);

        const dataPayload = {
            type: "incoming_call",
            callId: callId,
            callerId: callerId || "",
            callerName: callerName,
            callerPhotoUrl: callerPhotoUrl,
            callType: callType,
        };

        const results = await Promise.allSettled(
            fcmTokens.map((token) =>
                admin.messaging().send({
                    token: token,
                    // DATA-ONLY (sin notification) para que la app maneje el full-screen intent.
                    data: dataPayload,
                    android: {
                        priority: "high",
                    },
                    apns: {
                        headers: {
                            "apns-priority": "10",
                            "apns-push-type": "voip",
                        },
                        payload: {
                            aps: {
                                "content-available": 1,
                            },
                        },
                    },
                })
            )
        );

        const successCount = results.filter((r) => r.status === "fulfilled").length;
        const failCount = results.filter((r) => r.status === "rejected").length;

        console.log(`✅ Incoming-call push sent: ${successCount} success, ${failCount} failed`);

        return { successCount, failCount };
    });

/**
 * 🧹 cleanupExpiredStories — Función programada que elimina las historias que ya
 * superaron su ventana de 24 h. Completa la "expiración" de Stories: el cliente
 * ya las ocultaba al leer, pero sin esto los nodos y archivos se acumulaban para
 * siempre. Borra el nodo en Realtime Database, su nodo de vistas y, si tenía
 * media (imagen/video), también el archivo en Storage.
 *
 * Frecuencia: cada 6 horas. Requiere plan Blaze (Cloud Scheduler).
 */
exports.cleanupExpiredStories = functions.pubsub
    .schedule("every 6 hours")
    .onRun(async () => {
        const MAX_AGE_MS = 24 * 60 * 60 * 1000;
        const now = Date.now();

        const storiesSnap = await db.ref("stories").once("value");
        if (!storiesSnap.exists()) {
            console.log("🧹 cleanupExpiredStories: no hay historias.");
            return null;
        }

        const dbUpdates = {};
        const storageUrls = [];
        let expiredCount = 0;

        storiesSnap.forEach((userNode) => {
            const userId = userNode.key;
            userNode.forEach((storyNode) => {
                const story = storyNode.val() || {};
                const ts = typeof story.timestamp === "number" ? story.timestamp : 0;
                if (now - ts >= MAX_AGE_MS) {
                    expiredCount++;
                    dbUpdates[`stories/${userId}/${storyNode.key}`] = null;
                    dbUpdates[`stories_views/${storyNode.key}`] = null;
                    if (typeof story.mediaUrl === "string" && story.mediaUrl.startsWith("http")) {
                        storageUrls.push(story.mediaUrl);
                    }
                }
            });
        });

        if (expiredCount === 0) {
            console.log("🧹 cleanupExpiredStories: nada que borrar.");
            return null;
        }

        // 1. Borra los nodos de Database en una sola escritura atómica.
        await db.ref().update(dbUpdates);

        // 2. Borra los archivos de Storage (best-effort; los errores no bloquean).
        const bucket = admin.storage().bucket();
        await Promise.all(storageUrls.map(async (url) => {
            try {
                const match = url.match(/\/o\/([^?]+)/);
                if (match) {
                    const objectPath = decodeURIComponent(match[1]);
                    await bucket.file(objectPath).delete();
                }
            } catch (e) {
                console.warn(`No se pudo borrar de Storage: ${url} — ${e.message}`);
            }
        }));

        console.log(`🧹 cleanupExpiredStories: ${expiredCount} historias expiradas eliminadas.`);
        return null;
    });
