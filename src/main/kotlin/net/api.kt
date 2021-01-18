package net

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.URL

@Suppress("unused")
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
            return if (coverUrl == "") URL("https://www.audiotool.com/images/no-avatar-512.gif") else URL("https:$coverUrl")
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
            return try {
                Json.decodeFromString(Response.serializer(), URL("https://api.audiotool.com/track/$key.json").readText())
            } catch (throwable: Throwable) {
                Response(
                    Track(
                        key,
                        "N/A",
                        -1,
                        0,
                        0,
                        120.0,
                        User("foo", "foo", ""),
                        template = false,
                        published = false,
                        snapshotUrl = "",
                        coverUrl = "",
                        collaborators = emptyList(),
                        genreKey = "",
                        genreName = "",
                        duration = 235.0,
                        isNextTrack = false, joinPolicy = 0, license = 0
                    ), 0, 0, true
                )
            }
        }
    }
}