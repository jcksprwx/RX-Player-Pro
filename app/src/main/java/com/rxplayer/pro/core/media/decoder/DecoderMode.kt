package com.rxplayer.pro.core.media.decoder

enum class DecoderMode(val displayName: String, val description: String) {
    AUTO("Auto", "System-optimized hardware and software selection"),
    HW("HW", "Hardware accelerated decoding for smooth playback and low battery usage"),
    SW("SW", "Software decoding for complex or unsupported formats")
}
