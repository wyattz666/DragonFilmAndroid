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
        id = "none",
        name = "Mặc định",
        description = "Không dùng khung",
        drawableRes = 0,
        isVipOnly = false
    )

    val frames: List<AvatarFrame> = listOf(
        NONE,
        AvatarFrame(
            id = "frame_dragon_gold",
            name = "Rồng Vàng Hoàng Kim",
            description = "Biểu tượng quyền lực tối thượng",
            drawableRes = R.drawable.frame_dragon_gold
        ),
        AvatarFrame(
            id = "frame_golden_ring",
            name = "Vòng Tròn Hoàng Gia",
            description = "Kim sắc rạng ngời tinh xảo",
            drawableRes = R.drawable.frame_golden_ring
        ),
        AvatarFrame(
            id = "frame_fire_phoenix",
            name = "Hỏa Phụng Hoàng",
            description = "Ngọn lửa quyền năng tái sinh",
            drawableRes = R.drawable.frame_fire_phoenix
        ),
        AvatarFrame(
            id = "frame_royal_crest",
            name = "Vương Miện Quý Tộc",
            description = "Đẳng cấp thành viên danh dự",
            drawableRes = R.drawable.frame_royal_crest
        ),
        AvatarFrame(
            id = "frame_dreamweaver",
            name = "Huyễn Mộng Dạ Khúc",
            description = "Sắc màu huyền ảo của giấc mơ",
            drawableRes = R.drawable.frame_dreamweaver
        ),
        AvatarFrame(
            id = "frame_magic_circle",
            name = "Ma Pháp Trận",
            description = "Trận pháp ma thuật huyền bí",
            drawableRes = R.drawable.frame_magic_circle
        ),
        AvatarFrame(
            id = "frame_crystal_bloom",
            name = "Băng Tinh Thiên Giới",
            description = "Hoa tuyết pha lê thuần khiết",
            drawableRes = R.drawable.frame_crystal_bloom
        ),
        AvatarFrame(
            id = "frame_celestial_star",
            name = "Ngân Hà Tinh Tú",
            description = "Ánh sáng tinh hà rực rỡ",
            drawableRes = R.drawable.frame_celestial_star
        ),
        AvatarFrame(
            id = "frame_3d_cute",
            name = "Thỏ Ngọc 3D",
            description = "Khung 3D đáng yêu cao cấp",
            drawableRes = R.drawable.frame_3d_cute
        )
    )

    fun getFrame(id: String?): AvatarFrame {
        if (id.isNullOrEmpty() || id == "none") return NONE
        return frames.firstOrNull { it.id == id } ?: NONE
    }

    fun getFrameDrawable(id: String?): Int? {
        val frame = getFrame(id)
        return if (frame.drawableRes != 0) frame.drawableRes else null
    }
}
