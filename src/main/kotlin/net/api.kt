package net

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UnstableDefault
import kotlinx.serialization.json.Json
import java.net.URL

@Suppress("unused")
@OptIn(UnstableDefault::class)
class TrackApi {
    @Serializable
    class User(
        var key: String,
        val name: String,
        val avatar: String
    )

    @Serializable
    class Track(
        var key: String,
        val name: String,
        val id: Int,
        val created: Long,
        val modified: Long,
        val bpm: Double,
        var user: User,
        val template: Boolean,
        val published: Boolean,
        val snapshotUrl: String,
        val coverUrl: String = "",
        val collaborators: List<User>,
        val genreKey: String,
        val genreName: String,
        val duration: Double,
        val isNextTrack: Boolean,
        val joinPolicy: Int,
        val license: Int
    ) {
        fun cover(): URL {
            return URL("https:$coverUrl")
        }

        fun authors(): String {
            val regex = Regex("[^A-Za-z0-9 ]")
            val string = collaborators.joinToString { it.name }
            return regex.replace(string, "").trim()
        }
    }

    @Serializable
    class Response(
        val track: Track,
        @SerialName("author-role")
        val authorRole: Int,
        val privileges: Int,
        val pending: Boolean
    )

    companion object {
        fun fetch(key: String): Response {
            return Json.parse(Response.serializer(), URL("https://api.audiotool.com/track/$key.json").readText())
        }
    }
}