package fr.mandarine.tarotcounter

import android.provider.Settings
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.round
import androidx.compose.foundation.layout.offset
import kotlinx.coroutines.launch

// ─────────────────────────────────────────────────────────────────────────────
// Motion helpers (issue #204): screen transitions, animated scores and list
// re-ordering — all switched off when the user asked Android to remove
// animations (Settings → Accessibility → Remove animations).
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Whether the UI should skip animations. Provided once in MainActivity from the
 * system setting; `false` by default so previews and tests animate normally.
 */
val LocalReducedMotion = staticCompositionLocalOf { false }

/** Reads the system animator duration scale and converts it with [isReducedMotion]. */
@Composable
fun rememberSystemReducedMotion(): Boolean {
    val resolver = LocalContext.current.contentResolver
    return remember(resolver) {
        isReducedMotion(
            Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
        )
    }
}

/**
 * "Fade through" transition between two screens (Material motion): the old screen
 * fades out quickly, then the new one fades in while growing slightly from 92 %.
 * With reduced motion the screens swap instantly.
 *
 * Use it as the `transitionSpec` of an `AnimatedContent`.
 */
fun <S> AnimatedContentTransitionScope<S>.fadeThrough(reducedMotion: Boolean): ContentTransform {
    val total = screenTransitionMillis(reducedMotion)
    if (total == 0) return EnterTransition.None togetherWith ExitTransition.None
    val out = total * 3 / 10          // 30 % of the time fading the old screen out
    return (fadeIn(tween(total - out, delayMillis = out)) +
            scaleIn(tween(total - out, delayMillis = out), initialScale = 0.92f)) togetherWith
        fadeOut(tween(out))
}

/**
 * Animates a composable to its new position when its parent re-orders it (e.g. a
 * standings row moving up after a round). Each row must be wrapped in `key(...)`
 * so Compose knows it is the *same* row that moved.
 *
 * How it works: `onPlaced` reports where the layout put the row; `offset` then
 * draws it shifted back to where it *was*, and an [Animatable] slides that shift
 * to zero with a spring.
 */
fun Modifier.animatePlacement(enabled: Boolean = true): Modifier = if (!enabled) this else composed {
    val scope = rememberCoroutineScope()
    var target by remember { mutableStateOf(IntOffset.Zero) }
    var animatable by remember { mutableStateOf<Animatable<IntOffset, *>?>(null) }
    this
        .onPlaced { target = it.positionInParent().round() }
        .offset {
            val anim = animatable ?: Animatable(target, IntOffset.VectorConverter).also { animatable = it }
            if (anim.targetValue != target) {
                scope.launch {
                    anim.animateTo(target, spring(stiffness = Spring.StiffnessMediumLow))
                }
            }
            anim.value - target
        }
}
