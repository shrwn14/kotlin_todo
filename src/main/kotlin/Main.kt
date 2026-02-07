import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import database.DatabaseHelper
import ui.MainScreen

fun main() = application {
    val database = DatabaseHelper()

    Window(
        onCloseRequest = {
            database.close()
            exitApplication()
        },
        title = "Todo App",
        state = rememberWindowState(width = 800.dp, height = 900.dp)
    ) {
        MainScreen(database)
    }
}
