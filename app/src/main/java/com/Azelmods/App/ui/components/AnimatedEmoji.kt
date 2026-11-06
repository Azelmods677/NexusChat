package com.Azelmods.App.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import com.Azelmods.App.data.security.tor.AnonymityPolicy
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import java.text.BreakIterator

/** Prefijo de los stickers animados: el resto del texto es el emoji que representan. */
const val ANIMATED_STICKER_PREFIX = "noto:"

/**
 * Emoji animado (Noto Animated Emoji de Google, licencia CC BY 4.0).
 *
 * Mientras se descarga, si no existe animación para ese emoji o sin conexión se ve el emoji normal.
 * En modo anónimo no se piden: la descarga saldría fuera de Tor y revelaría la IP a Google.
 */
@Composable
fun AnimatedEmoji(
    emoji: String,
    size: Dp,
    modifier: Modifier = Modifier,
    iterations: Int = LottieConstants.IterateForever
) {
    val url = remember(emoji) { notoAnimationUrl(emoji) }
    val anonymous = AnonymityPolicy.active
    val composition by rememberLottieComposition(
        if (anonymous) LottieCompositionSpec.JsonString(EMPTY_ANIMATION) else LottieCompositionSpec.Url(url)
    )
    val progress by animateLottieCompositionAsState(composition, iterations = iterations)

    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        if (composition != null && !anonymous) {
            LottieAnimation(composition, { progress }, Modifier.size(size))
        } else {
            val fontSize = with(LocalDensity.current) { (size * 0.8f).toSp() }
            Text(emoji, fontSize = fontSize)
        }
    }
}

private const val EMPTY_ANIMATION = "{}"

/** "😀" → .../1f600/lottie.json; las secuencias se unen con guion bajo. */
internal fun notoAnimationUrl(emoji: String): String {
    val code = emoji.trim().codePoints().toArray().joinToString("_") { Integer.toHexString(it) }
    return "https://fonts.gstatic.com/s/e/notoemoji/latest/$code/lottie.json"
}

/** Separa un texto en emojis completos: una familia o una bandera cuentan como uno solo. */
internal fun splitGraphemes(text: String): List<String> {
    val iterator = BreakIterator.getCharacterInstance()
    iterator.setText(text)
    val parts = mutableListOf<String>()
    var start = iterator.first()
    var end = iterator.next()
    while (end != BreakIterator.DONE) {
        text.substring(start, end).takeIf { it.isNotBlank() }?.let(parts::add)
        start = end
        end = iterator.next()
    }
    return parts
}
