package mx.ujat.dacyti.rideujat.data.model

data class TripWithDetails(
    val trip: Trip,
    val conductor: Profile?,
    val vehicle: Vehicle?
)

data class RequestWithPassenger(
    val request: TripRequest,
    val pasajero: Profile?
)
