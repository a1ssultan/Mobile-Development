package kz.example.splitbill

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import kz.example.splitbill.ui.navigation.SplitBillNavHost
import kz.example.splitbill.ui.theme.SplitBillTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            SplitBillTheme {
                SplitBillNavHost()
            }
        }
    }
}
