package com.droidamp.ui.theme

import androidx.compose.ui.graphics.Color

// ─────────────────────────────────────────────────────────────
//  DroidTheme — single source of truth for all 17 color themes
//  Each theme drives every pixel: background, panels, accents,
//  viz bars, EQ, volume, playlist highlight, source tags, nav.
// ─────────────────────────────────────────────────────────────

data class DroidTheme(
    val id: String,
    val displayName: String,
    val bg: Color,          // deepest background
    val panel: Color,       // card / header surfaces
    val surface: Color,     // elevated surfaces, tabs
    val border: Color,      // dividers and outlines
    val accent: Color,      // primary — logo, active tab, now-playing, viz bars
    val fg: Color,          // primary text
    val fg2: Color,         // secondary / muted text
    val green: Color,       // LOCAL source tag
    val yellow: Color,      // SC source tag + EQ label
    val red: Color,         // BC tag + close dot + fav heart
    val vizBar: Color,      // spectrum visualizer bars (often = accent)
    val eqBar: Color,       // 10-band EQ bar color
    val volBar: Color,      // volume slider fill
    val playBg: Color,      // play button background
    val playBorder: Color,  // play button border
    val isLight: Boolean = false,
)

// ─────────────────────────────────────────────────────────────
//  All 17 themes — faithful to cliamp.stream palette
// ─────────────────────────────────────────────────────────────
object DroidThemes {

    val Catppuccin = DroidTheme(
        id = "catppuccin", displayName = "Catppuccin",
        bg = Color(0xFF1E1E2E), panel = Color(0xFF313244), surface = Color(0xFF45475A),
        border = Color(0xFF585B70), accent = Color(0xFFCBA6F7), fg = Color(0xFFCDD6F4),
        fg2 = Color(0xFFA6ADC8), green = Color(0xFFA6E3A1), yellow = Color(0xFFF9E2AF),
        red = Color(0xFFEB6F92), vizBar = Color(0xFFCBA6F7), eqBar = Color(0xFF89DCEB),
        volBar = Color(0xFF89B4FA), playBg = Color(0xFF313244), playBorder = Color(0xFFCBA6F7),
    )

    val CatppuccinLatte = DroidTheme(
        id = "catppuccin_latte", displayName = "Catppuccin Latte",
        bg = Color(0xFFEFF1F5), panel = Color(0xFFE6E9EF), surface = Color(0xFFDCE0E8),
        border = Color(0xFFBCC0CC), accent = Color(0xFF8839EF), fg = Color(0xFF4C4F69),
        fg2 = Color(0xFF8C8FA1), green = Color(0xFF40A02B), yellow = Color(0xFFDF8E1D),
        red = Color(0xFFD20F39), vizBar = Color(0xFF8839EF), eqBar = Color(0xFF209FB5),
        volBar = Color(0xFF1E66F5), playBg = Color(0xFFE8D5FF), playBorder = Color(0xFF8839EF),
        isLight = true,
    )

    val AyuMirage = DroidTheme(
        id = "ayu_mirage", displayName = "Ayu Mirage Dark",
        bg = Color(0xFF1F2430), panel = Color(0xFF232A38), surface = Color(0xFF2D3444),
        border = Color(0xFF374059), accent = Color(0xFFFFCC66), fg = Color(0xFFCBCCC6),
        fg2 = Color(0xFF5C6773), green = Color(0xFFBAE67E), yellow = Color(0xFFFFCC66),
        red = Color(0xFFF28779), vizBar = Color(0xFFFFCC66), eqBar = Color(0xFF73D0FF),
        volBar = Color(0xFF73D0FF), playBg = Color(0xFF3A2E00), playBorder = Color(0xFFFFCC66),
    )

    val Ethereal = DroidTheme(
        id = "ethereal", displayName = "Ethereal",
        bg = Color(0xFF0D0E14), panel = Color(0xFF13141D), surface = Color(0xFF1A1B27),
        border = Color(0xFF252639), accent = Color(0xFFC5A8FF), fg = Color(0xFFB8C0CC),
        fg2 = Color(0xFF454660), green = Color(0xFF7CF5A0), yellow = Color(0xFFF5D08C),
        red = Color(0xFFF57C7C), vizBar = Color(0xFFC5A8FF), eqBar = Color(0xFF7CD8F5),
        volBar = Color(0xFFC5A8FF), playBg = Color(0xFF1E1530), playBorder = Color(0xFFC5A8FF),
    )

    val Everforest = DroidTheme(
        id = "everforest", displayName = "Everforest",
        bg = Color(0xFF2D353B), panel = Color(0xFF272E33), surface = Color(0xFF3D484D),
        border = Color(0xFF4A555B), accent = Color(0xFFA7C080), fg = Color(0xFFD3C6AA),
        fg2 = Color(0xFF7A8478), green = Color(0xFFA7C080), yellow = Color(0xFFDBBC7F),
        red = Color(0xFFE67E80), vizBar = Color(0xFFA7C080), eqBar = Color(0xFF83C092),
        volBar = Color(0xFF83C092), playBg = Color(0xFF1E2A1E), playBorder = Color(0xFFA7C080),
    )

    val FlexokiLight = DroidTheme(
        id = "flexoki_light", displayName = "Flexoki Light",
        bg = Color(0xFFFFFCF0), panel = Color(0xFFF2F0E5), surface = Color(0xFFE6E4D9),
        border = Color(0xFFCECDC3), accent = Color(0xFF24837B), fg = Color(0xFF100F0F),
        fg2 = Color(0xFF6F6E69), green = Color(0xFF3AA99F), yellow = Color(0xFFAD8301),
        red = Color(0xFFAF3029), vizBar = Color(0xFF24837B), eqBar = Color(0xFF205EA6),
        volBar = Color(0xFF24837B), playBg = Color(0xFFD5F0EE), playBorder = Color(0xFF24837B),
        isLight = true,
    )

    val Gruvbox = DroidTheme(
        id = "gruvbox", displayName = "Gruvbox",
        bg = Color(0xFF282828), panel = Color(0xFF1D2021), surface = Color(0xFF3C3836),
        border = Color(0xFF504945), accent = Color(0xFFFABD2F), fg = Color(0xFFEBDBB2),
        fg2 = Color(0xFF928374), green = Color(0xFFB8BB26), yellow = Color(0xFFFABD2F),
        red = Color(0xFFFB4934), vizBar = Color(0xFFFABD2F), eqBar = Color(0xFF83A598),
        volBar = Color(0xFF83A598), playBg = Color(0xFF3A2E00), playBorder = Color(0xFFFABD2F),
    )

    val Hackerman = DroidTheme(
        id = "hackerman", displayName = "Hackerman",
        bg = Color(0xFF0A0A0A), panel = Color(0xFF111111), surface = Color(0xFF1A1A1A),
        border = Color(0xFF00FF41), accent = Color(0xFF00FF41), fg = Color(0xFF00FF41),
        fg2 = Color(0xFF006614), green = Color(0xFF00FF41), yellow = Color(0xFFFFFF00),
        red = Color(0xFFFF0000), vizBar = Color(0xFF00FF41), eqBar = Color(0xFF00CCFF),
        volBar = Color(0xFF00CCFF), playBg = Color(0xFF001A00), playBorder = Color(0xFF00FF41),
    )

    val Kanagawa = DroidTheme(
        id = "kanagawa", displayName = "Kanagawa",
        bg = Color(0xFF1F1F28), panel = Color(0xFF16161D), surface = Color(0xFF2A2A37),
        border = Color(0xFF363646), accent = Color(0xFFE6C384), fg = Color(0xFFDCD7BA),
        fg2 = Color(0xFF717C7C), green = Color(0xFF98BB6C), yellow = Color(0xFFE6C384),
        red = Color(0xFFE46876), vizBar = Color(0xFFE6C384), eqBar = Color(0xFF7FB4CA),
        volBar = Color(0xFF7FB4CA), playBg = Color(0xFF2A2000), playBorder = Color(0xFFE6C384),
    )

    val MatteBlack = DroidTheme(
        id = "matte_black", displayName = "Matte Black",
        bg = Color(0xFF0A0A0A), panel = Color(0xFF141414), surface = Color(0xFF1C1C1C),
        border = Color(0xFF2A2A2A), accent = Color(0xFFFFFFFF), fg = Color(0xFFC8C8C8),
        fg2 = Color(0xFF555555), green = Color(0xFF88FF88), yellow = Color(0xFFFFEE88),
        red = Color(0xFFFF6666), vizBar = Color(0xFFCCCCCC), eqBar = Color(0xFFAAAAAA),
        volBar = Color(0xFFCCCCCC), playBg = Color(0xFF222222), playBorder = Color(0xFF888888),
    )

    val Miasma = DroidTheme(
        id = "miasma", displayName = "Miasma",
        bg = Color(0xFF0E0F14), panel = Color(0xFF13141A), surface = Color(0xFF1A1C24),
        border = Color(0xFF252830), accent = Color(0xFFD4A0FF), fg = Color(0xFFB0AFC4),
        fg2 = Color(0xFF454560), green = Color(0xFF96E08C), yellow = Color(0xFFE8CC7A),
        red = Color(0xFFF07070), vizBar = Color(0xFFD4A0FF), eqBar = Color(0xFF80D4F0),
        volBar = Color(0xFFD4A0FF), playBg = Color(0xFF1E1030), playBorder = Color(0xFFD4A0FF),
    )

    val Nord = DroidTheme(
        id = "nord", displayName = "Nord",
        bg = Color(0xFF2E3440), panel = Color(0xFF242933), surface = Color(0xFF3B4252),
        border = Color(0xFF434C5E), accent = Color(0xFF88C0D0), fg = Color(0xFFECEFF4),
        fg2 = Color(0xFF616E88), green = Color(0xFFA3BE8C), yellow = Color(0xFFEBCB8B),
        red = Color(0xFFBF616A), vizBar = Color(0xFF88C0D0), eqBar = Color(0xFF81A1C1),
        volBar = Color(0xFF88C0D0), playBg = Color(0xFF1A2A30), playBorder = Color(0xFF88C0D0),
    )

    val OsakaJade = DroidTheme(
        id = "osaka_jade", displayName = "Osaka Jade",
        bg = Color(0xFF0D1117), panel = Color(0xFF161B22), surface = Color(0xFF1F2937),
        border = Color(0xFF2D3748), accent = Color(0xFF4EC9A0), fg = Color(0xFFC9D1D9),
        fg2 = Color(0xFF484F58), green = Color(0xFF4EC9A0), yellow = Color(0xFFF0C050),
        red = Color(0xFFF07070), vizBar = Color(0xFF4EC9A0), eqBar = Color(0xFF56D4E4),
        volBar = Color(0xFF4EC9A0), playBg = Color(0xFF0A2018), playBorder = Color(0xFF4EC9A0),
    )

    val Ristretto = DroidTheme(
        id = "ristretto", displayName = "Ristretto",
        bg = Color(0xFF2A1F1F), panel = Color(0xFF1E1515), surface = Color(0xFF3A2828),
        border = Color(0xFF4A3535), accent = Color(0xFFFD6883), fg = Color(0xFFF0E0D0),
        fg2 = Color(0xFF8A6060), green = Color(0xFFADDA78), yellow = Color(0xFFF9CC6C),
        red = Color(0xFFFD6883), vizBar = Color(0xFFFD6883), eqBar = Color(0xFF85DACC),
        volBar = Color(0xFFFD6883), playBg = Color(0xFF3A1020), playBorder = Color(0xFFFD6883),
    )

    val RosePine = DroidTheme(
        id = "rose_pine", displayName = "Rosé Pine",
        bg = Color(0xFF191724), panel = Color(0xFF1F1D2E), surface = Color(0xFF26233A),
        border = Color(0xFF403D52), accent = Color(0xFFEBBCBA), fg = Color(0xFFE0DEF4),
        fg2 = Color(0xFF6E6A86), green = Color(0xFF9CCFD8), yellow = Color(0xFFF6C177),
        red = Color(0xFFEB6F92), vizBar = Color(0xFFEBBCBA), eqBar = Color(0xFF9CCFD8),
        volBar = Color(0xFFC4A7E7), playBg = Color(0xFF2A1A20), playBorder = Color(0xFFEBBCBA),
    )

    val TokyoNight = DroidTheme(
        id = "tokyo_night", displayName = "Tokyo Night",
        bg = Color(0xFF1A1B2E), panel = Color(0xFF16161F), surface = Color(0xFF1F2335),
        border = Color(0xFF292E42), accent = Color(0xFF7AA2F7), fg = Color(0xFFC0CAF5),
        fg2 = Color(0xFF565F89), green = Color(0xFF9ECE6A), yellow = Color(0xFFE0AF68),
        red = Color(0xFFF7768E), vizBar = Color(0xFF7AA2F7), eqBar = Color(0xFF7DCFFF),
        volBar = Color(0xFF7AA2F7), playBg = Color(0xFF1A2060), playBorder = Color(0xFF7AA2F7),
    )

    val Vantablack = DroidTheme(
        id = "vantablack", displayName = "Vantablack",
        bg = Color(0xFF000000), panel = Color(0xFF050505), surface = Color(0xFF0A0A0A),
        border = Color(0xFF151515), accent = Color(0xFFFF3C00), fg = Color(0xFFC0C0C0),
        fg2 = Color(0xFF333333), green = Color(0xFF00FF88), yellow = Color(0xFFFFAA00),
        red = Color(0xFFFF3C00), vizBar = Color(0xFFFF3C00), eqBar = Color(0xFF00CCFF),
        volBar = Color(0xFFFF3C00), playBg = Color(0xFF1A0800), playBorder = Color(0xFFFF3C00),
    )

    val all: List<DroidTheme> = listOf(
        AyuMirage, Catppuccin, CatppuccinLatte, Ethereal, Everforest,
        FlexokiLight, Gruvbox, Hackerman, Kanagawa, MatteBlack,
        Miasma, Nord, OsakaJade, Ristretto, RosePine, TokyoNight, Vantablack,
    )

    fun fromId(id: String): DroidTheme = all.find { it.id == id } ?: Catppuccin
}
