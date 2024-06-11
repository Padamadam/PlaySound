package de.fra_uas.fb2.mobiledevices.playsound

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
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
import java.io.File
import java.io.IOException

class MainActivity : AppCompatActivity() {

    private lateinit var playButtons: Array<ImageButton>
    private lateinit var soundTitles: Array<EditText>
    private lateinit var chooseButtons: Array<ImageButton>
    private lateinit var mediaPlayers: Array<MediaPlayer?>
    private lateinit var soundUris: Array<Uri?>
    private lateinit var recordButtons: Array<ImageButton>

    private lateinit var mixSound: Switch

    private var currentButtonId: Int = 0
    private var isOnPlayArray: BooleanArray = booleanArrayOf(false, false, false)
    private var isRecordingArray: BooleanArray = booleanArrayOf(false, false, false)

    private var mediaRecorder: MediaRecorder? = null
    private var recordedFilePaths: Array<String?> = arrayOfNulls(3)

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
        recordButtons = arrayOf(findViewById(R.id.recordSound1), findViewById(R.id.recordSound2), findViewById(R.id.recordSound3))
        mediaPlayers = arrayOfNulls(3)
        soundUris = arrayOfNulls(3)

        mixSound = findViewById(R.id.mixSound)

        chooseButtons.forEachIndexed { index, button ->
            button.setOnClickListener { chooseSound(index) }
        }

        playButtons.forEachIndexed { index, button ->
            button.setOnClickListener { playSound(soundUris[index], button, index) }
        }

        recordButtons.forEachIndexed { index, button ->
            button.setOnClickListener { toggleRecording(index) }
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
        val uriToPlay = uri ?: recordedFilePaths[index]?.let { Uri.fromFile(File(it)) } ?: return

        if (isOnPlayArray[index]) {
            stopIndividualSound(mediaPlayers[index], playButton, index)
        } else {
            if (!mixSound.isChecked) {
                stopSound()
            }
            playIndividualSound(uriToPlay, playButton, index)
        }
    }

    private fun playIndividualSound(uri: Uri, playButton: ImageButton, index: Int) {
        val mediaPlayer = MediaPlayer().apply {
            try {
                setDataSource(this@MainActivity, uri)
                prepare()
                start()
                setOnCompletionListener {
                    it.release()
                    mediaPlayers[index] = null
                    isOnPlayArray[index] = false
                    playButton.setImageResource(android.R.drawable.ic_media_play)
                }
            } catch (e: IOException) {
                e.printStackTrace()
                Toast.makeText(this@MainActivity, "Error playing sound", Toast.LENGTH_SHORT).show()
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

    private fun toggleRecording(index: Int) {
        if (isRecordingArray[index]) {
            stopRecording(index)
        } else {
            startRecording(index)
        }
    }

    private fun startRecording(index: Int) {
        val fileName = "${externalCacheDir?.absolutePath}/audiorecordtest${index}.3gp"
        recordedFilePaths[index] = fileName

        mediaRecorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
            setOutputFile(fileName)
            setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
            try {
                prepare()
                start()
                isRecordingArray[index] = true
                recordButtons[index].setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
                Toast.makeText(this@MainActivity, "Recording started", Toast.LENGTH_SHORT).show()
            } catch (e: IOException) {
                e.printStackTrace()
                Toast.makeText(this@MainActivity, "Error starting recording", Toast.LENGTH_SHORT).show()
                mediaRecorder?.release()
                mediaRecorder = null
            }
        }
    }

    private fun stopRecording(index: Int) {
        mediaRecorder?.apply {
            stop()
            release()
        }
        mediaRecorder = null
        isRecordingArray[index] = false
        recordButtons[index].setImageResource(android.R.drawable.ic_btn_speak_now)
        soundTitles[index].setText("Recorded Sound ${index + 1}")
        Toast.makeText(this, "Recording stopped", Toast.LENGTH_SHORT).show()
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
