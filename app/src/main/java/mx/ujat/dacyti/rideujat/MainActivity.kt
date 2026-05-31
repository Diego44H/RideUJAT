package mx.ujat.dacyti.rideujat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController
import mx.ujat.dacyti.rideujat.navigation.NavGraph
import mx.ujat.dacyti.rideujat.ui.theme.RideUJATTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RideUJATTheme {
                val navController = rememberNavController()
                NavGraph(navController = navController)
            }
        }
    }
}
