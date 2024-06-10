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
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Switch
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

    private lateinit var soundTitle1: EditText
    private lateinit var soundTitle2: EditText
    private lateinit var soundTitle3: EditText

    private lateinit var chooseSound1: Button
    private lateinit var chooseSound2: Button
    private lateinit var chooseSound3: Button

    private lateinit var mixSound: Switch

    private var currentButtonId: Int = 0

    private var mediaPlayer1: MediaPlayer? = null
    private var mediaPlayer2: MediaPlayer? = null
    private var mediaPlayer3: MediaPlayer? = null
    private var soundUri1: Uri? = null
    private var soundUri2: Uri? = null
    private var soundUri3: Uri? = null

    private var isPlaying1 = false
    private var isPlaying2 = false
    private var isPlaying3 = false

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

        playSound1.setOnClickListener { playSound(soundUri1, playSound1, 1) }
        playSound2.setOnClickListener { playSound(soundUri2, playSound2, 2) }
        playSound3.setOnClickListener { playSound(soundUri3, playSound3, 3) }

        checkPermissions()
    }

    private val chooseSoundLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                when (currentButtonId) {
                    1 -> {
                        soundUri1 = uri
                        soundTitle1.setText(uri.lastPathSegment)
                    }
                    2 -> {
                        soundUri2 = uri
                        soundTitle2.setText(uri.lastPathSegment)
                    }
                    3 -> {
                        soundUri3 = uri
                        soundTitle3.setText(uri.lastPathSegment)
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

    private fun playSound(uri: Uri?, playButton: ImageButton, soundId: Int) {
        if (uri == null) return

        when (soundId) {
            1 -> {
                if (isPlaying1) {
                    stopIndividualSound(mediaPlayer1, playButton, soundId)
                } else {
                    playIndividualSound(uri, playButton, soundId)
                }
            }
            2 -> {
                if (isPlaying2) {
                    stopIndividualSound(mediaPlayer2, playButton, soundId)
                } else {
                    playIndividualSound(uri, playButton, soundId)
                }
            }
            3 -> {
                if (isPlaying3) {
                    stopIndividualSound(mediaPlayer3, playButton, soundId)
                } else {
                    playIndividualSound(uri, playButton, soundId)
                }
            }
        }
    }

    private fun playIndividualSound(uri: Uri, playButton: ImageButton, soundId: Int) {
        val mediaPlayer = MediaPlayer().apply {
            setDataSource(this@MainActivity, uri)
            prepare()
            start()
            setOnCompletionListener {
                it.release()
                when (soundId) {
                    1 -> {
                        mediaPlayer1 = null
                        isPlaying1 = false
                    }
                    2 -> {
                        mediaPlayer2 = null
                        isPlaying2 = false
                    }
                    3 -> {
                        mediaPlayer3 = null
                        isPlaying3 = false
                    }
                }
                playButton.setImageResource(android.R.drawable.ic_media_play)
            }
        }

        when (soundId) {
            1 -> {
                mediaPlayer1?.release()
                mediaPlayer1 = mediaPlayer
                isPlaying1 = true
            }
            2 -> {
                mediaPlayer2?.release()
                mediaPlayer2 = mediaPlayer
                isPlaying2 = true
            }
            3 -> {
                mediaPlayer3?.release()
                mediaPlayer3 = mediaPlayer
                isPlaying3 = true
            }
        }

        playButton.setImageResource(android.R.drawable.ic_media_pause)
    }

    private fun stopIndividualSound(mediaPlayer: MediaPlayer?, playButton: ImageButton, soundId: Int) {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        when (soundId) {
            1 -> {
                mediaPlayer1 = null
                isPlaying1 = false
            }
            2 -> {
                mediaPlayer2 = null
                isPlaying2 = false
            }
            3 -> {
                mediaPlayer3 = null
                isPlaying3 = false
            }
        }
        playButton.setImageResource(android.R.drawable.ic_media_play)
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

    private fun stopSound() {
        mediaPlayer1?.stop()
        mediaPlayer1?.release()
        mediaPlayer1 = null
        mediaPlayer2?.stop()
        mediaPlayer2?.release()
        mediaPlayer2 = null
        mediaPlayer3?.stop()
        mediaPlayer3?.release()
        mediaPlayer3 = null

        playSound1.setImageResource(android.R.drawable.ic_media_play)
        playSound2.setImageResource(android.R.drawable.ic_media_play)
        playSound3.setImageResource(android.R.drawable.ic_media_play)

        isPlaying1 = false
        isPlaying2 = false
        isPlaying3 = false
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer1?.release()
        mediaPlayer2?.release()
        mediaPlayer3?.release()
    }
}
