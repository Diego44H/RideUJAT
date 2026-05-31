package mx.ujat.dacyti.rideujat.navigation

import kotlinx.serialization.Serializable

@Serializable object Login
@Serializable object Register
@Serializable object Home
@Serializable object Profile
@Serializable object Vehicles
@Serializable object NewTrip
@Serializable data class EditTrip(val tripId: String)
@Serializable object MyTrips
@Serializable object SearchTrips
@Serializable data class TripDetail(val tripId: String)
@Serializable data class ManageRequests(val tripId: String)
@Serializable data class ActiveTrip(val tripId: String, val isConductor: Boolean)
@Serializable data class Chat(val tripId: String)
@Serializable data class Rating(val tripId: String, val isConductor: Boolean)
