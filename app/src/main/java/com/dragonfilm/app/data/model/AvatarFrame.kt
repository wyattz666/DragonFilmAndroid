package com.dragonfilm.app.data.model

import com.dragonfilm.app.R

data class AvatarFrame(
    val id: String,
    val name: String,
    val description: String,
    val drawableRes: Int,
    val isVipOnly: Boolean = true
)

object AvatarFrameCatalog {
    val NONE = AvatarFrame(
        id = "",
        name = "Mặc định",
        description = "Không dùng khung",
        drawableRes = 0,
        isVipOnly = false
    )

    val frames: List<AvatarFrame> = listOf(
        NONE,
        AvatarFrame(
            id = "frame-dragon-spirit",
            name = "Hồn Rồng Thiêng",
            description = "Biểu tượng hoàng kim tối thượng",
            drawableRes = R.drawable.frame_dragon_spirit
        ),
        AvatarFrame(
            id = "frame-golden-royale",
            name = "Hoàng Kim Quý Tộc",
            description = "Vương giả kim sắc rực rỡ",
            drawableRes = R.drawable.frame_golden_royale
        ),
        AvatarFrame(
            id = "frame-dreamweaver",
            name = "Kẻ Dệt Mộng (Genshin)",
            description = "Sắc màu huyền ảo của giấc mơ",
            drawableRes = R.drawable.frame_dreamweaver
        ),
        AvatarFrame(
            id = "frame-celestial-night",
            name = "Dạ Nguyệt Tinh Cầu",
            description = "Ánh sáng tinh hà huyền bí",
            drawableRes = R.drawable.frame_celestial_night
        ),
        AvatarFrame(
            id = "frame-cyber-neon",
            name = "Cyber Neon",
            description = "Phong cách công nghệ tương lai",
            drawableRes = R.drawable.frame_cyber_neon
        ),
        AvatarFrame(
            id = "frame-tech-shadow",
            name = "Chiến Binh Bóng Đêm",
            description = "Sức mạnh hắc ám uy vũ",
            drawableRes = R.drawable.frame_tech_shadow
        ),
        AvatarFrame(
            id = "frame-crystal-wings",
            name = "Đôi Cánh Pha Lê",
            description = "Cánh thần tiên thuần khiết",
            drawableRes = R.drawable.frame_crystal_wings
        ),
        AvatarFrame(
            id = "frame-cute-ribbon",
            name = "Mèo Con Đáng Yêu",
            description = "Khung nơ mèo 3D dễ thương",
            drawableRes = R.drawable.frame_cute_ribbon
        ),
        AvatarFrame(
            id = "frame-ruby-rose",
            name = "Hồng Ngọc Hoàng Gia",
            description = "Hồng ngọc quý phái sang trọng",
            drawableRes = R.drawable.frame_ruby_rose
        )
    )

    fun normalizeId(rawId: String?): String {
        val trimmed = (rawId ?: "").trim()
        if (trimmed.isEmpty() || trimmed.equals("none", ignoreCase = true)) return ""
        return trimmed.replace("_", "-")
    }

    fun getFrame(id: String?): AvatarFrame {
        val normalized = normalizeId(id)
        if (normalized.isEmpty()) return NONE
        val safeRaw = (id ?: "").trim()
        return frames.firstOrNull {
            it.id.equals(normalized, ignoreCase = true) ||
            it.id.replace("-", "_").equals(safeRaw, ignoreCase = true)
        } ?: NONE
    }

    fun getFrameDrawable(id: String?): Int? {
        val frame = getFrame(id)
        return if (frame.drawableRes != 0) frame.drawableRes else null
    }
}
