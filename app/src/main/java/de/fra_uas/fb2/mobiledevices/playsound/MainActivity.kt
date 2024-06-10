package de.fra_uas.fb2.mobiledevices.playsound

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {

    private lateinit var playSound1: ImageButton
    private lateinit var playSound2: ImageButton
    private lateinit var playSound3: ImageButton

    private lateinit var soundTitle1: TextView
    private lateinit var soundTitle2: TextView
    private lateinit var soundTitle3: TextView

    private lateinit var chooseSound1: Button
    private lateinit var chooseSound2: Button
    private lateinit var chooseSound3: Button

    private lateinit var mixSound: Switch

    private var currentButtonId: Int = 0
    private var mediaPlayer: MediaPlayer? = null
    private var soundUri1: Uri? = null
    private var soundUri2: Uri? = null
    private var soundUri3: Uri? = null

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        playSound1 = findViewById(R.id.playSound1)
        playSound2 = findViewById(R.id.playSound2)
        playSound3 = findViewById(R.id.playSound3)

        soundTitle1 = findViewById(R.id.soundTitle1)
        soundTitle2 = findViewById(R.id.soundTitle2)
        soundTitle3 = findViewById(R.id.soundTitle3)

        chooseSound1 = findViewById(R.id.chooseSound1)
        chooseSound2 = findViewById(R.id.chooseSound2)
        chooseSound3 = findViewById(R.id.chooseSound3)

        mixSound = findViewById(R.id.mixSound)

        chooseSound1.setOnClickListener { chooseSound(1) }
        chooseSound2.setOnClickListener { chooseSound(2) }
        chooseSound3.setOnClickListener { chooseSound(3) }

        playSound1.setOnClickListener { playSound(soundUri1) }
        playSound2.setOnClickListener { playSound(soundUri2) }
        playSound3.setOnClickListener { playSound(soundUri3) }

        checkPermissions()
    }

    private val chooseSoundLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                when (currentButtonId) {
                    1 -> {
                        soundUri1 = uri
                        soundTitle1.text = uri.lastPathSegment
                    }
                    2 -> {
                        soundUri2 = uri
                        soundTitle2.text = uri.lastPathSegment
                    }
                    3 -> {
                        soundUri3 = uri
                        soundTitle3.text = uri.lastPathSegment
                    }
                }
            }
        }
    }

    private fun chooseSound(buttonId: Int) {
        currentButtonId = buttonId
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            type = "audio/*"
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        chooseSoundLauncher.launch(intent)
    }

    private fun playSound(uri: Uri?) {
        if (uri == null) return

        if (!mixSound.isChecked) {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
        }

        mediaPlayer = MediaPlayer().apply {
            setDataSource(this@MainActivity, uri)
            prepare()
            start()
            setOnCompletionListener {
                it.release()
                if (it == mediaPlayer) {
                    mediaPlayer = null
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    private fun checkPermissions() {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.READ_MEDIA_AUDIO)
        } else {
            arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        if (!permissions.all { ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED }) {
            ActivityCompat.requestPermissions(this, permissions, 1)
            Toast.makeText(this, "No permissions granted", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
    }
}
