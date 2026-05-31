package mx.ujat.dacyti.rideujat.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import mx.ujat.dacyti.rideujat.ui.active.ActiveTripScreen
import mx.ujat.dacyti.rideujat.ui.auth.LoginScreen
import mx.ujat.dacyti.rideujat.ui.chat.ChatScreen
import mx.ujat.dacyti.rideujat.ui.rating.RatingScreen
import mx.ujat.dacyti.rideujat.ui.auth.RegisterScreen
import mx.ujat.dacyti.rideujat.ui.home.HomeScreen
import mx.ujat.dacyti.rideujat.ui.profile.ProfileScreen
import mx.ujat.dacyti.rideujat.ui.requests.ManageRequestsScreen
import mx.ujat.dacyti.rideujat.ui.search.SearchTripsScreen
import mx.ujat.dacyti.rideujat.ui.search.TripDetailScreen
import mx.ujat.dacyti.rideujat.ui.trips.MyTripsScreen
import mx.ujat.dacyti.rideujat.ui.trips.PublishTripScreen
import mx.ujat.dacyti.rideujat.ui.vehicles.VehiclesScreen

@Composable
fun NavGraph(
    navController: NavHostController,
    startDestination: Any = Login
) {
    NavHost(navController = navController, startDestination = startDestination) {

        composable<Login> {
            LoginScreen(
                onLoginSuccess = { navController.navigate(Home) { popUpTo<Login> { inclusive = true } } },
                onRegisterClick = { navController.navigate(Register) }
            )
        }
        composable<Register> {
            RegisterScreen(
                onRegisterSuccess = { navController.navigate(Home) { popUpTo<Login> { inclusive = true } } },
                onLoginClick = { navController.popBackStack() }
            )
        }
        composable<Home> {
            HomeScreen(
                onProfileClick = { navController.navigate(Profile) },
                onPublishTripClick = { navController.navigate(NewTrip) },
                onMyTripsClick = { navController.navigate(MyTrips) },
                onSearchTripsClick = { navController.navigate(SearchTrips) }
            )
        }
        composable<Profile> {
            ProfileScreen(
                onBack = { navController.popBackStack() },
                onVehiclesClick = { navController.navigate(Vehicles) }
            )
        }
        composable<Vehicles> {
            VehiclesScreen(onBack = { navController.popBackStack() })
        }
        composable<NewTrip> {
            PublishTripScreen(
                tripId = null,
                onBack = { navController.popBackStack() },
                onSuccess = { navController.navigate(MyTrips) { popUpTo<NewTrip> { inclusive = true } } }
            )
        }
        composable<EditTrip> { back ->
            val route = back.toRoute<EditTrip>()
            PublishTripScreen(tripId = route.tripId, onBack = { navController.popBackStack() }, onSuccess = { navController.popBackStack() })
        }
        composable<MyTrips> {
            MyTripsScreen(
                onBack = { navController.popBackStack() },
                onPublishTrip = { navController.navigate(NewTrip) },
                onEditTrip = { id -> navController.navigate(EditTrip(id)) },
                onManageRequests = { id -> navController.navigate(ManageRequests(id)) },
                onActiveTrip = { id -> navController.navigate(ActiveTrip(id, isConductor = true)) }
            )
        }
        composable<SearchTrips> {
            SearchTripsScreen(
                onBack = { navController.popBackStack() },
                onTripClick = { id -> navController.navigate(TripDetail(id)) }
            )
        }
        composable<TripDetail> { back ->
            val route = back.toRoute<TripDetail>()
            TripDetailScreen(
                tripId = route.tripId,
                onBack = { navController.popBackStack() },
                onActiveTrip = { id -> navController.navigate(ActiveTrip(id, isConductor = false)) },
                onChatClick = { id -> navController.navigate(Chat(id)) }
            )
        }
        composable<ManageRequests> { back ->
            val route = back.toRoute<ManageRequests>()
            ManageRequestsScreen(tripId = route.tripId, onBack = { navController.popBackStack() })
        }
        composable<ActiveTrip> { back ->
            val route = back.toRoute<ActiveTrip>()
            ActiveTripScreen(
                tripId = route.tripId,
                isConductor = route.isConductor,
                onBack = { navController.popBackStack() },
                onTripFinished = {
                    navController.navigate(Rating(route.tripId, route.isConductor)) {
                        popUpTo<ActiveTrip> { inclusive = true }
                    }
                },
                onChatClick = { id -> navController.navigate(Chat(id)) }
            )
        }
        composable<Chat> { back ->
            val route = back.toRoute<Chat>()
            ChatScreen(tripId = route.tripId, onBack = { navController.popBackStack() })
        }
        composable<Rating> { back ->
            val route = back.toRoute<Rating>()
            RatingScreen(
                tripId = route.tripId,
                isConductor = route.isConductor,
                onRatingComplete = {
                    navController.navigate(Home) { popUpTo<Rating> { inclusive = true } }
                }
            )
        }
    }
}
