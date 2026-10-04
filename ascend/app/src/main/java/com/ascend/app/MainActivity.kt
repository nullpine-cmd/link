package com.ascend.app

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.ascend.app.di.LocalAppContainer
import com.ascend.app.di.LocalNotificationPermission
import com.ascend.app.di.NotificationPermissionRequester
import com.ascend.app.ui.navigation.AscendRoot
import com.ascend.app.ui.theme.AscendTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        val container = (application as AscendApplication).container
        setContent {
            val permissionRequester = rememberNotificationPermissionRequester()
            AscendTheme {
                CompositionLocalProvider(
                    LocalAppContainer provides container,
                    LocalNotificationPermission provides permissionRequester,
                ) {
                    AscendRoot()
                }
            }
        }
    }
}

@Composable
private fun rememberNotificationPermissionRequester(): NotificationPermissionRequester {
    val context = LocalContext.current
    var pending by remember { mutableStateOf<((Boolean) -> Unit)?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        pending?.invoke(granted)
        pending = null
    }
    return remember(launcher) {
        NotificationPermissionRequester { onResult ->
            val granted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
            if (granted) {
                onResult(true)
            } else {
                pending = onResult
                launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}
