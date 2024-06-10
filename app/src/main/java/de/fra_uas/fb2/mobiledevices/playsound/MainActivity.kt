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

    private lateinit var playButtons: Array<ImageButton>
    private lateinit var soundTitles: Array<EditText>
    private lateinit var chooseButtons: Array<Button>
    private lateinit var mediaPlayers: Array<MediaPlayer?>
    private lateinit var soundUris: Array<Uri?>

    private lateinit var mixSound: Switch

    private var currentButtonId: Int = 0
    private var isOnPlayArray: BooleanArray = booleanArrayOf(false, false, false)

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

        playButtons = arrayOf(findViewById(R.id.playSound1), findViewById(R.id.playSound2), findViewById(R.id.playSound3))
        soundTitles = arrayOf(findViewById(R.id.soundTitle1), findViewById(R.id.soundTitle2), findViewById(R.id.soundTitle3))
        chooseButtons = arrayOf(findViewById(R.id.chooseSound1), findViewById(R.id.chooseSound2), findViewById(R.id.chooseSound3))
        mediaPlayers = arrayOfNulls(3)
        soundUris = arrayOfNulls(3)

        mixSound = findViewById(R.id.mixSound)

        chooseButtons.forEachIndexed { index, button ->
            button.setOnClickListener { chooseSound(index) }
        }

        playButtons.forEachIndexed { index, button ->
            button.setOnClickListener { playSound(soundUris[index], button, index) }
        }

        checkPermissions()
    }

    private val chooseSoundLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                soundUris[currentButtonId] = uri
                soundTitles[currentButtonId].setText(uri.lastPathSegment)
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

    private fun playSound(uri: Uri?, playButton: ImageButton, index: Int) {
        if (uri == null) return

        if (isOnPlayArray[index]) {
            stopIndividualSound(mediaPlayers[index], playButton, index)
        } else {
            if (!mixSound.isChecked) {
                stopSound()
            }
            playIndividualSound(uri, playButton, index)
        }
    }

    private fun playIndividualSound(uri: Uri, playButton: ImageButton, index: Int) {
        val mediaPlayer = MediaPlayer().apply {
            setDataSource(this@MainActivity, uri)
            prepare()
            start()
            setOnCompletionListener {
                it.release()
                mediaPlayers[index] = null
                isOnPlayArray[index] = false
                playButton.setImageResource(android.R.drawable.ic_media_play)
            }
        }

        mediaPlayers[index]?.release()
        mediaPlayers[index] = mediaPlayer
        isOnPlayArray[index] = true
        playButton.setImageResource(android.R.drawable.ic_media_pause)
    }

    private fun stopIndividualSound(mediaPlayer: MediaPlayer?, playButton: ImageButton, index: Int) {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayers[index] = null
        isOnPlayArray[index] = false
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
        for (i in mediaPlayers.indices) {
            mediaPlayers[i]?.stop()
            mediaPlayers[i]?.release()
            mediaPlayers[i] = null
            playButtons[i].setImageResource(android.R.drawable.ic_media_play)
            isOnPlayArray[i] = false
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayers.forEach { it?.release() }
    }
}
