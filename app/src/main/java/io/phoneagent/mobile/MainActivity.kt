package io.phoneagent.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import io.phoneagent.mobile.data.TaskStore
import io.phoneagent.mobile.ui.PhoneAgentApp
import io.phoneagent.mobile.ui.PhoneAgentTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val taskStore = TaskStore(applicationContext)

        setContent {
            PhoneAgentTheme {
                PhoneAgentApp(taskStore = taskStore)
            }
        }
    }
}
