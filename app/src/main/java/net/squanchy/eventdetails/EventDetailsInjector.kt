package net.squanchy.eventdetails

import net.squanchy.injection.ActivityContextModule
import net.squanchy.injection.applicationComponent
import net.squanchy.navigation.NavigationModule

internal fun eventDetailsComponent(activity: EventDetailsActivity) : EventDetailsComponent {
    return DaggerEventDetailsComponent.builder()
        .applicationComponent(activity.applicationComponent)
        .eventDetailsModule(EventDetailsModule())
        .activityContextModule(ActivityContextModule(activity))
        .navigationModule(NavigationModule())
        .build()
}