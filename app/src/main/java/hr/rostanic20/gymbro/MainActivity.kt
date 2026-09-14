package hr.rostanic20.gymbro

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import hr.rostanic20.gymbro.navigation.AppNavController
import hr.rostanic20.gymbro.ui.GymBroApp
import hr.rostanic20.gymbro.ui.theme.GymBroTheme
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {

    private val navController: AppNavController by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GymBroTheme {
                GymBroApp(navController = navController)
            }
        }
    }
}
