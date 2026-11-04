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
        // `members` es un mapa { uid: true }: los ids son las CLAVES. Con
        // Object.values() el "destinatario" salía `true`, se buscaban los tokens en
        // /fcmTokens/true y ningún mensaje llegaba a notificarse. Se admite también
        // el formato antiguo en array.
        let members = [];
        try {
            const chatSnapshot = await db.ref(`/chats/${chatId}/members`).once("value");
            const raw = chatSnapshot.val() || {};
            members = Array.isArray(raw) ? raw.filter(Boolean) : Object.keys(raw);
        } catch (e) {
            console.error("❌ Error reading chat members:", e);
            return null;
        }

        // ── 3. Destinatarios: todos los miembros menos quien envía ──
        const recipientIds = members.filter((uid) => uid !== senderId);
        if (recipientIds.length === 0) {
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
                    displayText = "📷 Foto";
                    break;
                case "VIDEO":
                    displayText = "🎥 Vídeo";
                    break;
                case "AUDIO":
                    displayText = "🎤 Mensaje de voz";
                    break;
                case "DOCUMENT":
                    displayText = "📄 Documento";
                    break;
                case "LOCATION":
                    displayText = "📍 Ubicación";
                    break;
                case "STICKER":
                    displayText = "Sticker";
                    break;
                case "GIF":
                    displayText = "GIF";
                    break;
                case "SYSTEM":
                    // Aviso interno del chat (p. ej. cambio del temporizador): sin push.
                    return null;
            }
        }

        // Un mensaje temporal no debe dejar su contenido en la bandeja de avisos del
        // otro teléfono, donde sobreviviría al borrado del propio mensaje.
        if (messageData.isEphemeral === true) {
            displayText = messageData.isViewOnce === true ? "Mensaje de una sola vista" : "Mensaje temporal";
        }

        // ── 7. Obtener FCM tokens de los destinatarios ──
        const tokenLists = await Promise.all(recipientIds.map((uid) => getFcmTokens(uid)));
        const fcmTokens = [...new Set(tokenLists.flat())];

        if (fcmTokens.length === 0) {
            console.log(`⚠️ No FCM tokens for chat ${chatId}`);
            return null;
        }

        console.log(`📱 Sending push to ${fcmTokens.length} device(s) in chat ${chatId}`);

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
 * 🆕 onStoryCreate — Avisa a quienes tienen una conversación con el autor cuando publica
 * una historia. Las historias viven en /stories/{userId}/{storyId}: la ruta anterior,
 * /stories/{storyId}, se disparaba con el nodo equivocado y nunca avisaba a nadie.
 */
exports.onStoryCreate = functions.database
    .ref("/stories/{userId}/{storyId}")
    .onCreate(async (snapshot, context) => {
        const { userId, storyId } = context.params;

        let userName = "Alguien";
        try {
            const userSnapshot = await db.ref(`/users/${userId}/displayName`).once("value");
            userName = userSnapshot.val() || userName;
        } catch (e) {
            console.warn("⚠️ Could not fetch story author name:", e);
        }

        // Los contactos son los otros miembros de sus conversaciones 1:1.
        const chatsSnapshot = await db.ref(`/userChats/${userId}`).once("value");
        const chatIds = Object.keys(chatsSnapshot.val() || {}).filter((id) => !id.startsWith("group_"));
        const contacts = [...new Set(
            chatIds.flatMap((id) => id.split("_")).filter((uid) => uid && uid !== userId)
        )];
        if (contacts.length === 0) return null;

        const tokenLists = await Promise.all(contacts.map((uid) => getFcmTokens(uid)));
        const fcmTokens = [...new Set(tokenLists.flat())];

        const results = await Promise.allSettled(
            fcmTokens.map((token) =>
                admin.messaging().send({
                    token: token,
                    notification: {
                        title: userName,
                        body: "Ha publicado una historia",
                    },
                    data: { type: "story", storyId: storyId, userId: userId },
                    android: {
                        notification: {
                            channelId: "nexus_messages",
                            icon: "ic_notification",
                            color: "#7B5CFA",
                        },
                    },
                })
            )
        );
        const failCount = results.filter((r) => r.status === "rejected").length;
        if (failCount > 0) console.warn(`⚠️ ${failCount} story push(es) failed for ${storyId}`);
        return null;
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

        // Desde CALLING/RINGING solo se llega a ENDED si el EMISOR colgó o se agotó
        // el tiempo de timbre (si el receptor rechaza, el estado es DECLINED). Así
        // que quien se pierde la llamada es siempre el receptor.
        //
        // Antes se calculaba con `after.endedBy`, que el cliente no escribía nunca:
        // el resultado era que el aviso de "llamada perdida" le llegaba AL QUE
        // LLAMABA y nunca al que no la cogió.
        const missedUserId = receiverId;
        if (!missedUserId) return null;

        let callerName = after.callerName || "Alguien";
        try {
            const userSnapshot = await db.ref(`/users/${callerId}/displayName`).once("value");
            callerName = userSnapshot.val() || callerName;
        } catch (e) {
            // ignore
        }

        const fcmTokens = await getFcmTokens(missedUserId);

        // DATA-ONLY: la app construye la notificación con el mismo tag/id que usa
        // cuando detecta la perdida en primer plano (sin duplicados) y, sobre todo,
        // retira la notificación de llamada entrante, que si no seguiría sonando.
        const results = await Promise.allSettled(
            fcmTokens.map((token) =>
                admin.messaging().send({
                    token: token,
                    data: {
                        type: "missed_call",
                        callId: callId,
                        callerId: callerId || "",
                        callerName: callerName,
                        callerPhotoUrl: after.callerPhotoUrl || "",
                        callType: after.callType || "AUDIO",
                    },
                    android: {
                        priority: "high",
                    },
                })
            )
        );

        const failCount = results.filter((r) => r.status === "rejected").length;
        if (failCount > 0) {
            console.error(`❌ ${failCount} missed-call push(es) failed for ${callId}`);
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
                        // Una llamada que llega tarde ya no se puede contestar: si el
                        // dispositivo estaba sin red, que FCM la descarte en vez de
                        // hacerlo sonar minutos después.
                        ttl: 45 * 1000,
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
 * Margen máximo de un mensaje temporal que nadie llega a abrir. Coincide con
 * RealtimeDatabaseRepository.EPHEMERAL_MAX_UNREAD_MS: el cliente lo escribe en
 * `expireAt`, y aquí se usa como respaldo si el campo falta.
 */
const EPHEMERAL_MAX_UNREAD_MS = 7 * 24 * 60 * 60 * 1000;

/**
 * Calcula cuándo debe borrarse un mensaje temporal, o null si no lo es.
 *
 * La cuenta atrás empieza cuando el destinatario lo abre (`viewedAt`, marca del
 * servidor), no al enviarlo: si no, alguien sin conexión perdería el mensaje sin
 * llegar a verlo. Mientras nadie lo abra rige `expireAt`.
 *
 * @param {object} message Mensaje guardado en la base de datos.
 * @return {number|null} Marca de tiempo en milisegundos.
 */
function ephemeralDeadline(message) {
    if (!message || message.isEphemeral !== true) return null;
    const seconds = Number(message.selfDestructDuration) || 0;
    const viewedAt = Number(message.viewedAt) || 0;
    if (viewedAt > 0 && seconds > 0) return viewedAt + seconds * 1000;
    const expireAt = Number(message.expireAt) || 0;
    if (expireAt > 0) return expireAt;
    return (Number(message.timestamp) || Date.now()) + EPHEMERAL_MAX_UNREAD_MS;
}

/**
 * 🕒 indexEphemeralMessage — Mantiene el índice `expiringMessages`, ordenado por
 * fecha de borrado. Sin él, la limpieza programada tendría que recorrer todos los
 * mensajes de todos los chats cada minuto.
 *
 * El nodo solo lo toca el Admin SDK: las reglas lo dejan cerrado a los clientes.
 */
exports.indexEphemeralMessage = functions.database
    .ref("/chats/{chatId}/messages/{messageId}")
    .onWrite(async (change, context) => {
        const { chatId, messageId } = context.params;
        const key = `${chatId}__${messageId}`;
        const message = change.after.val();
        const deadline = ephemeralDeadline(message);

        if (deadline === null) {
            // Ya no existe o no es temporal: quitar la entrada si la había.
            if (change.before.exists() && change.before.val()?.isEphemeral === true) {
                await db.ref(`/expiringMessages/${key}`).remove();
            }
            return null;
        }

        await db.ref(`/expiringMessages/${key}`).set({
            chatId,
            messageId,
            at: deadline,
            mediaUrl: message.mediaUrl || null,
        });
        return null;
    });

/**
 * 🧨 cleanupExpiredMessages — Borra en el servidor los mensajes temporales vencidos,
 * y su archivo en Storage si llevaban foto, vídeo o GIF.
 *
 * Es lo que hace cierta la autodestrucción: la app borra el mensaje cuando el tiempo
 * se acaba, pero solo si alguno de los participantes tiene el chat abierto. Con esta
 * función el mensaje desaparece aunque todos hayan cerrado la app.
 *
 * Frecuencia: cada minuto. Requiere plan Blaze (Cloud Scheduler).
 */
exports.cleanupExpiredMessages = functions.pubsub
    .schedule("every 1 minutes")
    .onRun(async () => {
        const snapshot = await db.ref("expiringMessages")
            .orderByChild("at")
            .endAt(Date.now())
            .limitToFirst(500)
            .once("value");
        if (!snapshot.exists()) return null;

        const updates = {};
        const storageUrls = [];
        snapshot.forEach((entry) => {
            const { chatId, messageId, mediaUrl } = entry.val() || {};
            updates[`expiringMessages/${entry.key}`] = null;
            if (chatId && messageId) {
                updates[`chats/${chatId}/messages/${messageId}`] = null;
            }
            if (typeof mediaUrl === "string" && mediaUrl.startsWith("http")) {
                storageUrls.push(mediaUrl);
            }
        });
        await db.ref().update(updates);

        const bucket = admin.storage().bucket();
        await Promise.all(storageUrls.map(async (url) => {
            try {
                const match = url.match(/\/o\/([^?]+)/);
                if (match) await bucket.file(decodeURIComponent(match[1])).delete();
            } catch (e) {
                console.warn(`No se pudo borrar de Storage: ${e.message}`);
            }
        }));

        console.log(`🧨 ${snapshot.numChildren()} mensajes temporales eliminados`);
        return null;
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


// ─────────────────────────────────────────────────────────────────────────────
// 💳 Premium con Mercado Pago
//
// El token de Mercado Pago es un secreto de servidor: vive en Secret Manager, nunca en el
// APK. Se configura una sola vez con:
//   npx firebase-tools functions:secrets:set MERCADOPAGO_ACCESS_TOKEN
//
// Precios: los de PREMIUM_PLANS, salvo que se sobrescriban sin redesplegar en la base de
// datos, en config/premium/plans/{plan}/amount y config/premium/currency.
// ─────────────────────────────────────────────────────────────────────────────

const MP_API = "https://api.mercadopago.com";
const DAY_MS = 24 * 60 * 60 * 1000;

const PREMIUM_PLANS = {
    monthly: { title: "Mensual", amount: 19900, period: "Se paga cada mes", days: 30 },
    annual: { title: "Anual", amount: 159900, period: "Un año completo", days: 365, badge: "Ahorra 33 %" },
    lifetime: { title: "De por vida", amount: 399900, period: "Pago único", days: null, badge: "Para siempre" },
};

/**
 * Planes vigentes, con los cambios de config/premium aplicados.
 *
 * @return {Promise<{plans: object, currency: string}>} Planes por id y moneda.
 */
async function loadPremiumConfig() {
    const snapshot = await db.ref("config/premium").once("value");
    const config = snapshot.val() || {};
    const plans = {};
    for (const [id, plan] of Object.entries(PREMIUM_PLANS)) {
        const amount = Number(config.plans?.[id]?.amount);
        plans[id] = { ...plan, amount: amount > 0 ? amount : plan.amount };
    }
    return { plans, currency: config.currency || "COP" };
}

/**
 * Dirección pública de esta función.
 *
 * @return {string} URL base, sin barra final.
 */
function premiumBaseUrl() {
    const projectId = JSON.parse(process.env.FIREBASE_CONFIG || "{}").projectId || process.env.GCLOUD_PROJECT;
    return `https://us-central1-${projectId}.cloudfunctions.net/premiumApi`;
}

/**
 * Crea el cobro en Mercado Pago para el usuario autenticado.
 *
 * @param {object} req Petición: `Authorization: Bearer <idToken>` y `{ plan }`.
 * @param {object} res Respuesta: `{ url }` con el enlace de pago.
 * @return {Promise<void>}
 */
async function handleCheckout(req, res) {
    const header = req.get("Authorization") || "";
    const idToken = header.startsWith("Bearer ") ? header.slice(7) : "";
    let user;
    try {
        user = await admin.auth().verifyIdToken(idToken);
    } catch (e) {
        res.status(401).json({ error: "Tu sesión no es válida. Vuelve a entrar." });
        return;
    }

    const { plans, currency } = await loadPremiumConfig();
    const planId = String(req.body?.plan || "");
    const plan = plans[planId];
    if (!plan) {
        res.status(400).json({ error: "Ese plan no existe." });
        return;
    }

    const token = process.env.MERCADOPAGO_ACCESS_TOKEN;
    const base = premiumBaseUrl();
    const response = await fetch(`${MP_API}/checkout/preferences`, {
        method: "POST",
        headers: { "Authorization": `Bearer ${token}`, "Content-Type": "application/json" },
        body: JSON.stringify({
            items: [{
                id: planId,
                title: `NexusChat Premium · ${plan.title}`,
                quantity: 1,
                currency_id: currency,
                unit_price: plan.amount,
            }],
            payer: user.email ? { email: user.email } : undefined,
            // Lo único que une el pago con la cuenta: se recibe de vuelta en el aviso.
            external_reference: `${user.uid}|${planId}|${Date.now()}`,
            notification_url: `${base}/webhook`,
            back_urls: { success: `${base}/return`, pending: `${base}/return`, failure: `${base}/return` },
            auto_return: "approved",
            statement_descriptor: "NEXUSCHAT",
        }),
    });

    if (!response.ok) {
        console.error(`❌ Mercado Pago respondió ${response.status}: ${await response.text()}`);
        res.status(502).json({ error: "Mercado Pago no pudo crear el cobro. Inténtalo en un rato." });
        return;
    }
    const preference = await response.json();
    const url = token.startsWith("TEST-") ? preference.sandbox_init_point : preference.init_point;
    res.json({ url });
}

/**
 * Aviso de Mercado Pago: un pago ha cambiado.
 *
 * El contenido del aviso no se cree: cualquiera podría enviar uno inventado a esta dirección.
 * Solo se usa su id para preguntarle el pago a Mercado Pago con nuestro token, y esa respuesta
 * es lo único que puede activar Premium.
 *
 * @param {object} req Petición de Mercado Pago.
 * @param {object} res Respuesta; 200 para que no reintente lo ya procesado.
 * @return {Promise<void>}
 */
async function handleWebhook(req, res) {
    const type = req.body?.type || req.query.type || req.query.topic;
    const paymentId = req.body?.data?.id || req.query["data.id"] || req.query.id;
    if (type !== "payment" || !paymentId) {
        res.status(200).send("ignorado");
        return;
    }

    const response = await fetch(`${MP_API}/v1/payments/${encodeURIComponent(paymentId)}`, {
        headers: { Authorization: `Bearer ${process.env.MERCADOPAGO_ACCESS_TOKEN}` },
    });
    if (!response.ok) {
        // 404: id inventado o de otra cuenta. Otro error: que Mercado Pago lo reintente.
        res.status(response.status === 404 ? 200 : 500).send("sin confirmar");
        return;
    }
    const payment = await response.json();

    const [uid, planId] = String(payment.external_reference || "").split("|");
    const { plans } = await loadPremiumConfig();
    const plan = plans[planId];
    if (!uid || !plan) {
        res.status(200).send("sin referencia");
        return;
    }

    const receiptRef = db.ref(`payments/${uid}/${payment.id}`);
    const previous = (await receiptRef.once("value")).val();
    await receiptRef.set({
        plan: planId,
        status: payment.status,
        amount: payment.transaction_amount,
        currency: payment.currency_id,
        createdAt: previous?.createdAt || Date.now(),
        updatedAt: Date.now(),
    });

    const premiumRef = db.ref(`premium/${uid}`);
    if (payment.status === "approved" && previous?.status !== "approved") {
        if (Number(payment.transaction_amount) < plan.amount) {
            console.error(`❌ Pago ${payment.id} por debajo del precio del plan ${planId}`);
            res.status(200).send("importe incorrecto");
            return;
        }
        // Renovar antes de que venza suma el tiempo, no lo pisa.
        await premiumRef.transaction((current) => {
            if (plan.days === null || (current?.active && !current.expiresAt)) {
                return { active: true, plan: plan.days === null ? planId : current.plan, updatedAt: Date.now() };
            }
            const from = Math.max(Date.now(), Number(current?.expiresAt) || 0);
            return { active: true, plan: planId, expiresAt: from + plan.days * DAY_MS, updatedAt: Date.now() };
        });
        console.log(`✅ Premium ${planId} activado para ${uid}`);
    } else if (["refunded", "charged_back", "cancelled"].includes(payment.status) && previous?.status === "approved") {
        await premiumRef.update({ active: false, updatedAt: Date.now() });
        console.log(`↩️ Premium retirado a ${uid}: pago ${payment.status}`);
    }

    res.status(200).send("ok");
}

/**
 * 💳 premiumApi — Planes, cobro y avisos de Mercado Pago.
 *   GET  /plans     planes y precios para la app
 *   POST /checkout  crea el enlace de pago
 *   POST /webhook   aviso de Mercado Pago
 *   GET  /return    página a la que vuelve quien paga
 */
exports.premiumApi = functions
    .runWith({ secrets: ["MERCADOPAGO_ACCESS_TOKEN"] })
    .https.onRequest(async (req, res) => {
        try {
            if (req.path === "/plans" && req.method === "GET") {
                const { plans, currency } = await loadPremiumConfig();
                const format = new Intl.NumberFormat("es-CO", {
                    style: "currency",
                    currency,
                    maximumFractionDigits: 0,
                });
                res.json({
                    plans: Object.entries(plans).map(([id, plan]) => ({
                        id,
                        title: plan.title,
                        priceLabel: format.format(plan.amount),
                        period: plan.period,
                        badge: plan.badge || "",
                    })),
                });
            } else if (req.path === "/checkout" && req.method === "POST") {
                await handleCheckout(req, res);
            } else if (req.path === "/webhook") {
                await handleWebhook(req, res);
            } else if (req.path === "/return") {
                res.set("Content-Type", "text/html; charset=utf-8").send(
                    "<!doctype html><meta name=viewport content='width=device-width,initial-scale=1'>" +
                    "<body style='background:#0b0c0e;color:#eceef0;font-family:sans-serif;text-align:center;padding:64px 24px'>" +
                    "<h2>Gracias</h2><p>Ya puedes volver a NexusChat. Premium se activa solo en cuanto " +
                    "Mercado Pago confirme el pago.</p></body>"
                );
            } else {
                res.status(404).json({ error: "No encontrado" });
            }
        } catch (e) {
            console.error("❌ premiumApi:", e);
            res.status(500).json({ error: "Error del servidor de pagos." });
        }
    });
