package net.squanchy.schedule.domain.view

import net.squanchy.eventdetails.domain.view.ExperienceLevel
import net.squanchy.speaker.domain.view.Speaker
import net.squanchy.support.lang.Optional
import org.joda.time.DateTimeZone
import org.joda.time.LocalDateTime

@Suppress("LongParameterList") // This is just a big model - TODO refactor this to split it up
data class Event(
        val id: String,
        val numericId: Long,
        val startTime: LocalDateTime,
        val endTime: LocalDateTime,
        val title: String,
        val place: Optional<Place>,
        val track: Optional<Track>,
        val speakers: List<Speaker>,
        val experienceLevel: Optional<ExperienceLevel>,
        val dayId: String,
        val type: Type,
        val favorited: Boolean,
        val description: Optional<String>,
        val timeZone: DateTimeZone
) {

    fun isHappeningAt(time: LocalDateTime) = time.isAfter(startTime) && time.isBefore(endTime)

    enum class Type constructor(private val rawType: String) {
        REGISTRATION("registration"),
        TALK("talk"),
        KEYNOTE("keynote"),
        COFFEE_BREAK("coffee_break"),
        LUNCH("lunch"),
        SOCIAL("social"),
        OTHER("other");

        companion object {

            fun fromRawType(rawType: String): Type {
                return values().find { it.rawType.equals(rawType, ignoreCase = true) }
                        ?: throw IllegalArgumentException("Unsupported raw event type: $rawType")
            }
        }
    }

    companion object {

        fun create(
                eventId: String,
                numericEventId: Long,
                dayId: String,
                startTime: LocalDateTime,
                endTime: LocalDateTime,
                title: String,
                place: Optional<Place>,
                experienceLevel: Optional<ExperienceLevel>,
                speakers: List<Speaker>,
                type: Type,
                favorited: Boolean,
                description: Optional<String>,
                track: Optional<Track>,
                timeZone: DateTimeZone
        ) = Event(
                eventId,
                numericEventId,
                startTime,
                endTime,
                title,
                place,
                track,
                speakers,
                experienceLevel,
                dayId,
                type,
                favorited,
                description,
                timeZone
        )
    }
}
