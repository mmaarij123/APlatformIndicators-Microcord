package com.platformindicators.microcord

import com.discord.api.activity.ActivityType
import com.discord.api.presence.ClientStatus
import com.discord.models.domain.ModelSession
import com.discord.models.presence.Presence
import com.discord.stores.StoreGatewayConnection
import com.discord.stores.StoreUser
import com.discord.stores.StoreUserPresence
import rx.Observable
import rx.subjects.ReplaySubject

internal enum class Platform(val key: String, val label: String, val icon: Int) {
    DESKTOP("desktop", "Desktop", R.drawable.ic_platform_desktop),
    MOBILE("mobile", "Mobile", R.drawable.ic_platform_mobile),
    WEB("web", "Web", R.drawable.ic_platform_web),
    CONSOLE("embedded", "Console", R.drawable.ic_platform_console),
    VR("vr", "VR", R.drawable.ic_platform_vr),
}

internal enum class BadgeStatus(val label: String, val color: Int) {
    ONLINE("Online", 0xFF23A55A.toInt()),
    IDLE("Idle", 0xFFF0B232.toInt()),
    DND("Do Not Disturb", 0xFFF23F43.toInt()),
    STREAMING("Streaming", 0xFFA275F5.toInt()),
    ;

    companion object {
        fun from(status: ClientStatus?) = when (status) {
            ClientStatus.ONLINE -> ONLINE
            ClientStatus.IDLE -> IDLE
            ClientStatus.DND -> DND
            else -> null
        }

        fun fromSession(status: String?) = entries.firstOrNull { it.name.equals(status, ignoreCase = true) }
    }
}

internal data class PlatformBadge(val platform: Platform, val status: BadgeStatus)

internal fun remoteBadges(presence: Presence?): List<PlatformBadge> {
    if (BadgeStatus.from(presence?.status) == null) return emptyList()
    val clients = presence?.clientStatuses ?: return emptyList()
    return listOf(
        Platform.DESKTOP to clients.desktopStatus(),
        Platform.MOBILE to clients.mobileStatus(),
        Platform.WEB to clients.webStatus(),
    ).mapNotNull { (platform, status) -> BadgeStatus.from(status)?.let { PlatformBadge(platform, it) } }
}

internal fun sessionBadges(sessions: List<ModelSession>): List<PlatformBadge> {
    return Platform.entries.mapNotNull { platform ->
        sessions.asSequence()
            .filter { it.clientInfo?.client.equals(platform.key, ignoreCase = true) }
            .mapNotNull { session -> BadgeStatus.fromSession(session.status)?.let { session.lastModified to it } }
            .maxByOrNull { (lastModified, _) -> lastModified }
            ?.let { (_, status) -> PlatformBadge(platform, status) }
    }
}

internal class PlatformPresence(
    private val users: StoreUser,
    private val presences: StoreUserPresence,
    gateway: StoreGatewayConnection,
) {
    private data class OwnBadges(val userId: Long?, val badges: List<PlatformBadge>)

    private val stopped = ReplaySubject.createWithSize<Unit>(1)
    val onStop: Observable<Unit> = stopped.asObservable()

    private val ownBadges = gateway.ready.switchMap { ready ->
        val sessionId = ready.sessionId
        val userId = ready.me?.id
        gateway.sessionsReplace
            // Microcord replays the last replacement even after reconnecting or switching accounts.
            .filter { sessions -> sessionId != null && sessions.any { it.sessionId == sessionId } }
            .startWith(ready.sessions.orEmpty())
            .map { sessions -> OwnBadges(userId, sessionBadges(sessions)) }
    }.startWith(OwnBadges(null, emptyList()))
        .distinctUntilChanged()
        .replay(1)
        .refCount()

    fun observe(userId: Long): Observable<List<PlatformBadge>> =
        users.observeMeId().switchMap { currentUserId ->
            if (currentUserId == userId) {
                Observable.combineLatest(ownBadges, presences.observeLocalPresence()) { own, local ->
                    if (own.userId != userId) {
                        emptyList()
                    } else if (local.activities.orEmpty().any { it.type == ActivityType.STREAMING }) {
                        own.badges.map { it.copy(status = BadgeStatus.STREAMING) }
                    } else {
                        own.badges
                    }
                }
            } else {
                presences.observePresenceForUser(userId).map(::remoteBadges)
            }
        }.distinctUntilChanged()

    fun stop() {
        stopped.onNext(Unit)
        stopped.onCompleted()
    }
}
