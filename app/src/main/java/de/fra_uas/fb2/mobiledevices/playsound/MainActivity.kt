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
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.io.File
import java.io.IOException

class MainActivity : AppCompatActivity() {

    private lateinit var playButton1: ImageButton
    private lateinit var playButton2: ImageButton
    private lateinit var playButton3: ImageButton

    private lateinit var soundTitle1: EditText
    private lateinit var soundTitle2: EditText
    private lateinit var soundTitle3: EditText

    private lateinit var chooseButton1: ImageButton
    private lateinit var chooseButton2: ImageButton
    private lateinit var chooseButton3: ImageButton

    private lateinit var recordButton1: ImageButton
    private lateinit var recordButton2: ImageButton
    private lateinit var recordButton3: ImageButton

    private var mediaPlayer1: MediaPlayer? = null
    private var mediaPlayer2: MediaPlayer? = null
    private var mediaPlayer3: MediaPlayer? = null

    private var soundUri1: Uri? = null
    private var soundUri2: Uri? = null
    private var soundUri3: Uri? = null

    private lateinit var mixSound: Switch

    private var isOnPlay1 = false
    private var isOnPlay2 = false
    private var isOnPlay3 = false

    private var isRecording1 = false
    private var isRecording2 = false
    private var isRecording3 = false

    private var mediaRecorder: MediaRecorder? = null
    private var recordedFilePath1: String? = null
    private var recordedFilePath2: String? = null
    private var recordedFilePath3: String? = null

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        playButton1 = findViewById(R.id.playSound1)
        playButton2 = findViewById(R.id.playSound2)
        playButton3 = findViewById(R.id.playSound3)

        soundTitle1 = findViewById(R.id.soundTitle1)
        soundTitle2 = findViewById(R.id.soundTitle2)
        soundTitle3 = findViewById(R.id.soundTitle3)

        chooseButton1 = findViewById(R.id.chooseSound1)
        chooseButton2 = findViewById(R.id.chooseSound2)
        chooseButton3 = findViewById(R.id.chooseSound3)

        recordButton1 = findViewById(R.id.recordSound1)
        recordButton2 = findViewById(R.id.recordSound2)
        recordButton3 = findViewById(R.id.recordSound3)

        mixSound = findViewById(R.id.mixSound)

        chooseButton1.setOnClickListener { chooseSound(1) }
        chooseButton2.setOnClickListener { chooseSound(2) }
        chooseButton3.setOnClickListener { chooseSound(3) }

        playButton1.setOnClickListener { playSound(soundUri1, playButton1, 1) }
        playButton2.setOnClickListener { playSound(soundUri2, playButton2, 2) }
        playButton3.setOnClickListener { playSound(soundUri3, playButton3, 3) }

        recordButton1.setOnClickListener { toggleRecording(1) }
        recordButton2.setOnClickListener { toggleRecording(2) }
        recordButton3.setOnClickListener { toggleRecording(3) }

        mixSound.setOnClickListener { handleSwitchChange() }

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

    private var currentButtonId: Int = 0

    private fun chooseSound(buttonId: Int) {
        currentButtonId = buttonId
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            type = "audio/*"
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        chooseSoundLauncher.launch(intent)
    }

    private fun playSound(uri: Uri?, playButton: ImageButton, index: Int) {
        val uriToPlay = uri ?: when (index) {
            1 -> recordedFilePath1?.let { Uri.fromFile(File(it)) }
            2 -> recordedFilePath2?.let { Uri.fromFile(File(it)) }
            3 -> recordedFilePath3?.let { Uri.fromFile(File(it)) }
            else -> null
        } ?: return

        val isOnPlay = when (index) {
            1 -> isOnPlay1
            2 -> isOnPlay2
            3 -> isOnPlay3
            else -> false
        }

        val mediaPlayer = when (index) {
            1 -> mediaPlayer1
            2 -> mediaPlayer2
            3 -> mediaPlayer3
            else -> null
        }

        if (isOnPlay) {
            stopIndividualSound(mediaPlayer, playButton, index)
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
                    when (index) {
                        1 -> {
                            mediaPlayer1 = null
                            isOnPlay1 = false
                        }
                        2 -> {
                            mediaPlayer2 = null
                            isOnPlay2 = false
                        }
                        3 -> {
                            mediaPlayer3 = null
                            isOnPlay3 = false
                        }
                    }
                    playButton.setImageResource(android.R.drawable.ic_media_play)
                }
            } catch (e: IOException) {
                e.printStackTrace()
                Toast.makeText(this@MainActivity, getString(R.string.error_playing), Toast.LENGTH_SHORT).show()
            }
        }

        when (index) {
            1 -> {
                mediaPlayer1?.release()
                mediaPlayer1 = mediaPlayer
                isOnPlay1 = true
            }
            2 -> {
                mediaPlayer2?.release()
                mediaPlayer2 = mediaPlayer
                isOnPlay2 = true
            }
            3 -> {
                mediaPlayer3?.release()
                mediaPlayer3 = mediaPlayer
                isOnPlay3 = true
            }
        }
        playButton.setImageResource(android.R.drawable.ic_media_pause)
    }

    private fun stopIndividualSound(mediaPlayer: MediaPlayer?, playButton: ImageButton, index: Int) {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        when (index) {
            1 -> {
                mediaPlayer1 = null
                isOnPlay1 = false
            }
            2 -> {
                mediaPlayer2 = null
                isOnPlay2 = false
            }
            3 -> {
                mediaPlayer3 = null
                isOnPlay3 = false
            }
        }
        playButton.setImageResource(android.R.drawable.ic_media_play)
    }

    private fun toggleRecording(index: Int) {
        when (index) {
            1 -> if (isRecording1) stopRecording(1) else startRecording(1)
            2 -> if (isRecording2) stopRecording(2) else startRecording(2)
            3 -> if (isRecording3) stopRecording(3) else startRecording(3)
        }
    }

    private fun startRecording(index: Int) {
        var fileName: String? = null
        when (index) {
            1 -> {
                fileName = "${externalCacheDir?.absolutePath}/audiorecordtest${index}.mp3"
                recordedFilePath1 = fileName
            }
            2 -> {
                fileName = "${externalCacheDir?.absolutePath}/audiorecordtest${index}.amr"
                recordedFilePath2 = fileName
            }
            3 -> {
                fileName = "${externalCacheDir?.absolutePath}/audiorecordtest${index}.3gp"
                recordedFilePath3 = fileName
            }
        }

        mediaRecorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFile(fileName)
            try {
                when (index) {
                    1 -> {
//                      Mono/Stereo 8-320Kbps constant (CBR) or variable bit-rate (VBR)
                        setOutputFormat(MediaRecorder.OutputFormat.MPEG_2_TS)
                        setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                    }
                    2 -> {
//                        9 rates from 6.60 kbit/s to 23.85 kbit/s sampled @ 16kHz
                        setOutputFormat(MediaRecorder.OutputFormat.AMR_NB)
                        setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
                    }
                    3 -> {
//                        Support for mono/stereo/5.0/5.1 content
//                        with standard sampling rates from 8 to 48 kHz.
                        setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                        setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                    }
                }
                prepare()
                start()
                when (index) {
                    1 -> {
                        isRecording1 = true
                        recordButton1.setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
                    }
                    2 -> {
                        isRecording2 = true
                        recordButton2.setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
                    }
                    3 -> {
                        isRecording3 = true
                        recordButton3.setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
                    }
                }
                Toast.makeText(this@MainActivity, getString(R.string.recording_started), Toast.LENGTH_SHORT).show()
            } catch (e: IOException) {
                e.printStackTrace()
                Toast.makeText(this@MainActivity, getString(R.string.error_starting_recording), Toast.LENGTH_SHORT).show()
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
        when (index) {
            1 -> {
                isRecording1 = false
                recordButton1.setImageResource(android.R.drawable.ic_btn_speak_now)
                soundTitle1.setText(".mp3 format")
            }
            2 -> {
                isRecording2 = false
                recordButton2.setImageResource(android.R.drawable.ic_btn_speak_now)
                soundTitle2.setText(".amr format")
            }
            3 -> {
                isRecording3 = false
                recordButton3.setImageResource(android.R.drawable.ic_btn_speak_now)
                soundTitle3.setText(".3gp format")
            }
        }
        Toast.makeText(this, getString(R.string.recording_stopped), Toast.LENGTH_SHORT).show()
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
        }
    }

    private fun stopSound() {
        listOf(mediaPlayer1, mediaPlayer2, mediaPlayer3).forEachIndexed { index, mediaPlayer ->
            mediaPlayer?.stop()
            mediaPlayer?.release()
            when (index) {
                0 -> {
                    mediaPlayer1 = null
                    isOnPlay1 = false
                    playButton1.setImageResource(android.R.drawable.ic_media_play)
                }
                1 -> {
                    mediaPlayer2 = null
                    isOnPlay2 = false
                    playButton2.setImageResource(android.R.drawable.ic_media_play)
                }
                2 -> {
                    mediaPlayer3 = null
                    isOnPlay3 = false
                    playButton3.setImageResource(android.R.drawable.ic_media_play)
                }
            }
        }
    }

    private fun handleSwitchChange() {
        stopSound()
        mixSound.text = if (mixSound.isChecked) {
            getString(R.string.mix_sound_on)
        } else {
            getString(R.string.mix_sound_off)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        listOf(mediaPlayer1, mediaPlayer2, mediaPlayer3).forEach { it?.release() }
    }
}
