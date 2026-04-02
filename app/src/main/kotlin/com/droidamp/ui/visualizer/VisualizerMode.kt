package com.droidamp.ui.visualizer

enum class VisualizerMode(val label: String) {
    RADIAL       ("Radial"),
    SCATTER      ("Scatter"),
    BARS_REBORN  ("Bars"),
    OSCILLOSCOPE ("Oscilloscope"),
    PLASMA       ("Plasma"),
    VU_METERS    ("VU Meters"),
    ;
    fun next() = entries[(ordinal + 1) % entries.size]
    fun prev() = entries[(ordinal - 1 + entries.size) % entries.size]
    companion object { val DEFAULT = RADIAL }
}
