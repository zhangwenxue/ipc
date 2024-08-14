package android.boot.ipc

import android.boot.ipc.client.Envoy
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.io.File

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }



        if ("com.wwk.provider.ecg" == intent.action) {
            Log.i("__", "com.wwk.provider.ecg")
            val file = File("${getExternalFilesDir("ecg")}/a.xml")
            val uri = FileProvider.getUriForFile(this, "com.wwk.ecg.fileprovider", file)
            // val path = "${getExternalFilesDir("ecg")}/a.xml"
            file.run {
                createNewFile()
                writeText("Hello,Thank you")

                val contentUri =
                    FileProvider.getUriForFile(this@MainActivity, "com.wwk.ecg.fileprovider", this)


                // Toast.makeText(this@MainActivity,readText(), Toast.LENGTH_SHORT).show()

                contentResolver.openInputStream(contentUri)?.readBytes()?.let {
                    runOnUiThread {
                        Toast.makeText(
                            this@MainActivity,
                            contentUri.toString() + String(it),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                Intent(Intent.ACTION_SENDTO).apply {
                    data = contentUri
                    flags =
                        Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION

                }.let {
                    setResult(RESULT_OK, it)
                    finish()
                }
            }
        }


        Envoy(packageName).let {
            lifecycleScope.launch {
                it.get(this@MainActivity, "test").onFailure { Log.e("__", "${it.message}") }
                    .onSuccess { Log.i("__", it) }
            }

        }
    }
}