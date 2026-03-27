package com.droidamp.ui.visualizer

enum class VisualizerMode(val label: String) {
    RETRO       ("Retro"),
    BARS        ("Bars"),
    BRICKS      ("Bricks"),
    COLUMNS     ("Columns"),
    WAVE        ("Wave"),
    FLAME       ("Flame"),
    SCATTER     ("Scatter"),
    RADIAL      ("Radial"),
    OSCILLOSCOPE("Oscilloscope"),
    VU_METERS   ("VU Meters"),
    NONE        ("None"),
    ;
    fun next() = entries[(ordinal + 1) % entries.size]
    fun prev() = entries[(ordinal - 1 + entries.size) % entries.size]
    companion object { val DEFAULT = RETRO }
}
