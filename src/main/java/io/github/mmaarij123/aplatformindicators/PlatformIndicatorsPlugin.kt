package io.github.mmaarij123.aplatformindicators

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.discord.stores.StoreStream
import dev.mantikafasi.manticord.plugins.api.ManticordPlugin
import dev.mantikafasi.manticord.plugins.api.PluginMessagePlacement
import dev.mantikafasi.manticord.plugins.api.PluginScope
import dev.mantikafasi.manticord.plugins.api.pluginMessageContribution
import dev.mantikafasi.manticord.plugins.api.pluginProfileContribution
import dev.mantikafasi.manticord.plugins.api.pluginSettings
import rx.Completable

internal const val IGNORE_BOTS = "ignoreBots"

internal enum class Surface(val enabledKey: String, val title: String) {
    CHAT("showInChat", "Chat headers"),
    PROFILE("showInBadges", "Profile badges"),
    ;

    fun platformKey(platform: Platform) = "${enabledKey}.${platform.key}"
}

class PlatformIndicatorsPlugin : ManticordPlugin {
    private var presence: PlatformPresence? = null

    override val settingsSchema = pluginSettings {
        section("Display")
        toggle(Surface.CHAT.enabledKey, "Show in chat headers", default = true)
        toggle(Surface.PROFILE.enabledKey, "Show in profile badges", default = true)
        toggle(IGNORE_BOTS, "Ignore bots", default = true)

        for (surface in Surface.entries) {
            section(surface.title)
            for (platform in Platform.entries) {
                toggle(
                    surface.platformKey(platform),
                    platform.label,
                    description = if (platform == Platform.CONSOLE || platform == Platform.VR) {
                        "Available for your own sessions when Discord reports this platform."
                    } else {
                        ""
                    },
                    default = true,
                )
            }
        }
    }

    override fun start(scope: PluginScope): Completable = Completable.fromAction {
        val resources = requireNotNull(scope.resources.context)
        val platformPresence = PlatformPresence(
            StoreStream.getUsers(),
            StoreStream.getPresences(),
            StoreStream.getGatewayConnection(),
        )
        presence = platformPresence

        scope.ui.registerMessage(
            pluginMessageContribution(
                "platform_indicators_chat",
                "Platform indicators",
                PluginMessagePlacement.HEADER,
                { context -> IndicatorRow(context, resources, scope.settings, platformPresence, Surface.CHAT) },
            ) {
                isVisible = { it.authorId != null }
                bind = { view, message ->
                    (view as IndicatorRow).bind(message.authorId, message.isAuthorBot)
                }
            },
        )

        scope.ui.registerProfile(
            pluginProfileContribution(
                "platform_indicators_profile",
                "Platform badges",
                { context -> IndicatorRow(context, resources, scope.settings, platformPresence, Surface.PROFILE) },
            ) {
                priority = 550
                bind = { view, profile ->
                    (view as IndicatorRow).bind(profile.userId, profile.isBot)
                }
            },
        )
    }

    override fun stop(): Completable = Completable.fromAction {
        presence?.stop()
        presence = null
    }
}

class PlatformIndicatorsDiscoveryService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null
}
