package com.android.sample.screenrecorder

import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import com.android.sample.screenrecorder.ui.theme.ScreenRecorderTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ScreenRecorderTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Greeting("Android")
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    var shouldStart by remember {
        mutableStateOf(false)
    }
    val context = LocalContext.current
    val service = remember {
        context.getSystemService(MediaProjectionManager::class.java)
    }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()) {
        if (it.data == null || it.resultCode != ComponentActivity.RESULT_OK) {
            return@rememberLauncherForActivityResult
        }
        context.startForegroundService(Intent(
            context,
            MainService::class.java
        ).apply {
            action = MainService.START
            putExtras(it.data!!)
        })
    }

    Column {
        Text("Hello World")
        Button(onClick = {
            shouldStart = !shouldStart
            if (shouldStart) {
                launcher.launch(service.createScreenCaptureIntent())
            } else {
                context.startForegroundService(Intent(
                    context,
                    MainService::class.java
                ).apply {
                    action = MainService.STOP
                })
            }
        }) {
            Text("Click me")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    ScreenRecorderTheme {
        Greeting("Android")
    }
}