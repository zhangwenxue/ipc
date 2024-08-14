package android.boot.client

import android.boot.ipc.client.Envoy
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {
    private val envoy by lazy {
        Envoy("android.boot.ipc")
    }

    //注意：此方法需要提前调用，和launch方法同时调用时可能会提示未注册该监听

    private val launcher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            if (result.resultCode == RESULT_OK) {
                val data = result.data?.data
                val uri = Uri.parse("$data")
                contentResolver.openInputStream(uri)?.readBytes()?.let {
                    runOnUiThread {
                        Toast.makeText(
                            this@MainActivity,
                            "Client:" + String(it),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
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
            launcher.launch(Intent("com.wwk.provider.ecg").setPackage("android.boot.ipc"))
//            lifecycleScope.launch {
//                envoy.set(this@MainActivity, "sn", "${System.currentTimeMillis()}")
//            }
        }
    }
}