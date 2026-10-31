package com.rxplayer.pro.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "video_metadata",
    foreignKeys = [
        ForeignKey(
            entity = MediaFileEntity::class,
            parentColumns = ["id"],
            childColumns = ["mediaId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["mediaId"], unique = true)]
)
data class VideoMetadataEntity(
    @PrimaryKey
    val mediaId: Long,
    val videoCodec: String = "",
    val audioCodec: String = "",
    val containerFormat: String = "",
    val frameRate: Float = 0f,
    val colorStandard: String = "",
    val isHdr: Boolean = false,
    val hwDecoderSupported: Boolean = true
)

@Entity(
    tableName = "audio_metadata",
    foreignKeys = [
        ForeignKey(
            entity = MediaFileEntity::class,
            parentColumns = ["id"],
            childColumns = ["mediaId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["mediaId"], unique = true)]
)
data class AudioMetadataEntity(
    @PrimaryKey
    val mediaId: Long,
    val album: String = "",
    val artist: String = "",
    val albumArtist: String = "",
    val composer: String = "",
    val genre: String = "",
    val year: Int = 0,
    val trackNumber: Int = 0,
    val sampleRate: Int = 44100,
    val channelCount: Int = 2
)
