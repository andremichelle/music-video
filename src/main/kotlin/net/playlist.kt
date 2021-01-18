@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package net

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import java.io.File
import java.net.URL

@Serializable
class Playlist(val tracks: List<Track>, val bpm: Double, val duration: Double) {
    @Serializable
    class Track(
        val key: String,
        val name: String,
        @SerialName("cover-url")
        val coverUrl: String,
        val artists: List<Artist>,
        val position: Double
    ) {
        fun cover(): URL {
            return if (coverUrl == "") URL("https://www.audiotool.com/images/no-avatar-512.gif") else URL("https:$coverUrl")
        }

        fun authors(): String {
            val regex = Regex("[^A-Za-z0-9 ]")
            val string = artists.joinToString { it.name }
            val authors = regex.replace(string, "").trim()
            return if (authors.length > 80) {
                authors.substring(0, 80) + "..."
            } else {
                authors
            }
        }

        override fun toString(): String {
            return "Track(key='$key', name='$name', coverUrl='$coverUrl', artists=$artists)"
        }
    }

    @Serializable
    class Artist(
        var key: String,
        val name: String,
        @SerialName("avatar-url")
        val avatarUrl: String
    ) {
        override fun toString(): String {
            return "Artist(key='$key', name='$name', avatarUrl='$avatarUrl')"
        }
    }

    companion object {
        fun fetch(folder: String): Playlist {
            val path = "/Users/andre.michelle/Documents/Audiotool/Mixes/${folder}/playlist.json"
            return Json.decodeFromString(serializer(), File(path).readText())
        }
    }

    override fun toString(): String {
        return "Playlist(tracks=$tracks, bpm=$bpm, duration=$duration)"
    }
}