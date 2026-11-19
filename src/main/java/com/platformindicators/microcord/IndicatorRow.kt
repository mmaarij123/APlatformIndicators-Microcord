package com.platformindicators.microcord

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import dev.mantikafasi.manticord.plugins.api.PluginSettings
import rx.Observable
import rx.Subscription
import rx.schedulers.Schedulers
import kotlin.math.roundToInt

@SuppressLint("ViewConstructor") // Microcord creates this view through a contribution factory.
internal class IndicatorRow(
    context: Context,
    private val pluginContext: Context,
    private val settings: PluginSettings,
    private val presence: PlatformPresence,
    private val surface: Surface,
) : LinearLayout(context) {
    private data class Target(val userId: Long, val isBot: Boolean)

    private val profileBadges = if (surface == Surface.PROFILE) ProfileBadgeSlot(this) else null
    private var target: Target? = null
    private var subscription: Subscription? = null

    init {
        gravity = Gravity.CENTER_VERTICAL
        visibility = View.GONE
    }

    fun bind(userId: Long?, isBot: Boolean) {
        val next = userId?.let { Target(it, isBot) }
        if (target == next) {
            // Microcord can rebuild the native badges before rebinding the same profile.
            profileBadges?.refresh()
            return
        }
        unsubscribe()
        target = next
        if (isAttachedToWindow) subscribe()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        profileBadges?.attach()
        subscribe()
    }

    override fun onDetachedFromWindow() {
        unsubscribe()
        profileBadges?.detach()
        super.onDetachedFromWindow()
    }

    private fun subscribe() {
        val user = target ?: return
        val changes = settings.observeChanges().startWith(surface.enabledKey)
        subscription = Observable.combineLatest(presence.observe(user.userId), changes) { badges, _ ->
            if (!settings.getBoolean(surface.enabledKey, true) ||
                (user.isBot && settings.getBoolean(IGNORE_BOTS, true))
            ) {
                emptyList()
            } else {
                badges.filter { settings.getBoolean(surface.platformKey(it.platform), true) }
            }
        }.distinctUntilChanged()
            .takeUntil(presence.onStop)
            .observeOn(mainScheduler)
            .subscribe(
                ::render,
                { error ->
                    Log.w("PlatformIndicators", "Could not observe platform presence", error)
                    render(emptyList())
                },
                { render(emptyList()) },
            )
    }

    private fun unsubscribe() {
        subscription?.unsubscribe()
        subscription = null
        render(emptyList())
    }

    private fun render(badges: List<PlatformBadge>) {
        val views = badges.map { (platform, status) ->
            val description = "${platform.label}, ${status.label}"
            ImageView(context).apply {
                setImageDrawable(pluginContext.getDrawable(platform.icon)?.mutate())
                setColorFilter(status.color)
                contentDescription = description
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
                if (surface == Surface.PROFILE) {
                    val inset = dp(2)
                    setPadding(inset, inset, inset, inset)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) tooltipText = description
            }
        }
        if (profileBadges != null) {
            profileBadges.setIcons(views)
            return
        }
        removeAllViews()
        visibility = if (views.isEmpty()) View.GONE else View.VISIBLE
        val size = dp(14)
        views.forEach { addView(it, LayoutParams(size, size).apply { marginStart = dp(4) }) }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).roundToInt()

    companion object {
        private val handler = Handler(Looper.getMainLooper())
        private val mainScheduler = Schedulers.from { handler.post(it) }
    }
}
