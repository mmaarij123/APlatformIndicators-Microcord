package io.github.mmaarij123.aplatformindicators

import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import com.discord.R as HostR
import com.discord.widgets.user.profile.UserProfileHeaderView

/**
 * The profile contribution owns the lifetime. The icons themselves are children of Microcord's
 * native badge rows, which the profile header can replace on each bind.
 */
internal class ProfileBadgeSlot(private val anchor: View) {
    private var header: UserProfileHeaderView? = null
    private var badgeGroup: LinearLayout? = null
    private var icons = emptyList<ImageView>()
    private val extraRows = mutableListOf<LinearLayout>()
    private var addedBackground: GradientDrawable? = null
    private var laidOutWidth = 0

    private val widthListener = View.OnLayoutChangeListener { _, left, _, right, _, oldLeft, _, oldRight, _ ->
        if (right - left != oldRight - oldLeft) refresh()
    }

    fun attach() {
        try {
            val sheet = generateSequence(anchor.parent as? View) { it.parent as? View }
                .firstOrNull { it.id == HostR.id.user_sheet_content }
            val profileHeader = sheet?.findViewById<View>(HostR.id.user_sheet_profile_header_view)
                as? UserProfileHeaderView ?: return
            val group = profileHeader.findViewById<View>(HostR.id.user_profile_header_badges)
                as? LinearLayout ?: return
            if (group.orientation != LinearLayout.VERTICAL) return
            header = profileHeader
            badgeGroup = group
            (group.parent as? View)?.addOnLayoutChangeListener(widthListener)
        } catch (_: LinkageError) {
            // Older or changed host builds may lack these internal profile views.
            return
        }
        refresh()
    }

    fun detach() {
        clearLayout()
        (badgeGroup?.parent as? View)?.removeOnLayoutChangeListener(widthListener)
        badgeGroup = null
        header = null
    }

    fun setIcons(next: List<ImageView>) {
        clearLayout()
        icons = next
        refresh()
    }

    fun refresh() {
        val group = badgeGroup ?: return
        if (icons.isEmpty()) return
        val width = (group.parent as? View)?.width?.takeIf { it > 0 }
            ?: group.resources.displayMetrics.widthPixels
        if (laidOutWidth == width && icons.all { (it.parent as? View)?.parent === group }) return

        clearLayout()
        if (group.background == null) {
            val color = try {
                header?.badgeRowColor
            } catch (_: LinkageError) {
                null
            }
            if (color != null) {
                addedBackground = GradientDrawable().apply {
                    cornerRadius = dp(8).toFloat()
                    setColor(color)
                }
                group.background = addedBackground
                group.setPadding(dp(7), dp(2), dp(7), dp(2))
            }
        }

        val size = dp(20)
        val gap = dp(4)
        val capacity = ((width - group.paddingLeft - group.paddingRight + gap) / (size + gap))
            .coerceAtLeast(1)
        var row = group.getChildAt(group.childCount - 1) as? LinearLayout
        for (icon in icons) {
            val target = row?.takeIf { it.orientation == LinearLayout.HORIZONTAL && it.childCount < capacity }
                ?: addRow(group)
            target.addView(icon, LinearLayout.LayoutParams(size, size).apply {
                if (target.childCount > 0) marginStart = gap
            })
            row = target
        }
        laidOutWidth = width
    }

    private fun addRow(group: LinearLayout): LinearLayout = LinearLayout(group.context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        val params = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
        )
        if (group.childCount > 0) params.topMargin = dp(12)
        group.addView(this, params)
        extraRows.add(this)
    }

    private fun clearLayout() {
        icons.forEach { (it.parent as? ViewGroup)?.removeView(it) }
        val group = badgeGroup
        if (group != null) {
            extraRows.forEach(group::removeView)
            if (addedBackground != null && group.background === addedBackground) {
                group.background = null
                group.setPadding(0, 0, 0, 0)
            }
        }
        extraRows.clear()
        addedBackground = null
        laidOutWidth = 0
    }

    // Use the same rounding as Microcord's native badge layout.
    private fun dp(value: Int) = (value * anchor.resources.displayMetrics.density).toInt()
}
