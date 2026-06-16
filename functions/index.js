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

const functions = require("firebase-functions");
const admin = require("firebase-admin");

admin.initializeApp();

const db = admin.database();

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
        let fcmTokens = [];
        try {
            const tokensSnapshot = await db.ref(`/users/${recipientId}/fcmTokens`).once("value");
            const tokensData = tokensSnapshot.val();
            if (tokensData) {
                fcmTokens = Object.values(tokensData).filter((t) => typeof t === "string");
            }
        } catch (e) {
            console.error("❌ Error reading FCM tokens:", e);
        }

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
 * 🆕 onCallEnded — Notifica llamadas perdidas.
 */
exports.onCallEnded = functions.database
    .ref("/calls/{callId}")
    .onUpdate(async (change, context) => {
        const { callId } = context.params;
        const before = change.before.val();
        const after = change.after.val();

        // Solo notificar si la llamada pasó a estado ENDED sin responder
        if (before?.status !== "RINGING" || after?.status !== "ENDED") return null;

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
        let fcmTokens = [];
        try {
            const tokensSnapshot = await db.ref(`/users/${missedUserId}/fcmTokens`).once("value");
            const tokensData = tokensSnapshot.val();
            if (tokensData) {
                fcmTokens = Object.values(tokensData).filter((t) => typeof t === "string");
            }
        } catch (e) {
            console.error("❌ Error reading FCM tokens:", e);
        }

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
        let fcmTokens = [];
        try {
            const tokensSnapshot = await db.ref(`/users/${receiverId}/fcmTokens`).once("value");
            const tokensData = tokensSnapshot.val();
            if (tokensData) {
                fcmTokens = Object.values(tokensData).filter((t) => typeof t === "string");
            }
        } catch (e) {
            console.error("❌ Error reading FCM tokens:", e);
        }

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
