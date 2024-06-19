package android.boot.client

import android.boot.ipc.client.Envoy
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private val envoy by lazy {
        Envoy("android.boot.ipc")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        envoy.observe(this, "sn", null) {
            Log.i("_Client1", "client1 set :${it.getOrNull()}")
        }
        findViewById<View>(R.id.random).setOnClickListener {
            lifecycleScope.launch {
                envoy.set(this@MainActivity, "sn", "${System.currentTimeMillis()}")
            }
        }
    }
}