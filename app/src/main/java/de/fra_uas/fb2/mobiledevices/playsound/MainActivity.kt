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
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.size
import java.io.File
import java.io.IOException

class MainActivity : AppCompatActivity() {

    private lateinit var playButtons: ArrayList<ImageButton>
    private lateinit var soundTitles: ArrayList<EditText>
    private lateinit var chooseButtons: ArrayList<ImageButton>
    private lateinit var deleteButtons: ArrayList<ImageButton>
    private lateinit var mediaPlayers: ArrayList<MediaPlayer?>
    private lateinit var soundUris: ArrayList<Uri?>
    private lateinit var recordButtons: ArrayList<ImageButton>

    private lateinit var soundsContainer: LinearLayout


    private lateinit var mixSound: Switch

    private val MAX_SOUND_NUM: Int = 6

    private var currentButtonId: Int = 0
    private var isOnPlayArray: ArrayList<Boolean> = arrayListOf(false, false, false)
    private var isRecordingArray: ArrayList<Boolean> = arrayListOf(false, false, false)

    private var mediaRecorder: MediaRecorder? = null
    private var recordedFilePaths: ArrayList<String?> = arrayListOf(null, null, null)

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

        playButtons = arrayListOf(findViewById(R.id.playSound1), findViewById(R.id.playSound2), findViewById(R.id.playSound3))
        soundTitles = arrayListOf(findViewById(R.id.soundTitle1), findViewById(R.id.soundTitle2), findViewById(R.id.soundTitle3))
        chooseButtons = arrayListOf(findViewById(R.id.chooseSound1), findViewById(R.id.chooseSound2), findViewById(R.id.chooseSound3))
        recordButtons = arrayListOf(findViewById(R.id.recordSound1), findViewById(R.id.recordSound2), findViewById(R.id.recordSound3))
        deleteButtons = arrayListOf(findViewById(R.id.deleteSound1), findViewById(R.id.deleteSound2), findViewById(R.id.deleteSound3))
        mediaPlayers = arrayListOf(null, null, null)
        soundUris = arrayListOf(null, null, null)

        soundsContainer = findViewById(R.id.soundsContainer)

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

        deleteButtons.forEachIndexed { index, button ->
            button.setOnClickListener { deleteSound(index) }
        }

        mixSound.setOnClickListener { handleSwitchChange() }

        checkPermissions()

        findViewById<Button>(R.id.addBarButton).setOnClickListener {
            if(soundsContainer.size < MAX_SOUND_NUM) {
                addNewBar()
            }else{
                Toast.makeText(this, "Cannot add more than $MAX_SOUND_NUM", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private val chooseSoundLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                soundUris[currentButtonId] = uri
                val title = getDisplayNameFromUri(uri)
                soundTitles[currentButtonId].setText(title)
            }
        }
    }

    private fun getDisplayNameFromUri(uri: Uri): String? {
        var displayName: String? = null
        val projection = arrayOf(MediaStore.MediaColumns.DISPLAY_NAME)
        contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                displayName = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME))
            }
        }
        return displayName
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
                Toast.makeText(this@MainActivity, getString(R.string.error_playing), Toast.LENGTH_SHORT).show()
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
        val fileName = "${externalCacheDir?.absolutePath}/audiorecordtest${index}.aac"
        recordedFilePaths[index] = fileName

        mediaRecorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.AAC_ADTS)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOutputFile(fileName)
            try {
                prepare()
                start()
                isRecordingArray[index] = true
                recordButtons[index].setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
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
        isRecordingArray[index] = false
        recordButtons[index].setImageResource(android.R.drawable.ic_btn_speak_now)
        val recordedSound = getString(R.string.recorded_sound)
        val soundCnt = index + 1
        val title = "$recordedSound $soundCnt"
        soundTitles[index].setText(title)
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

    private fun handleSwitchChange() {
        val isChecked = mixSound.isChecked
        if (!isChecked) {
            stopSound()
        }
        Toast.makeText(this, if (isChecked) "Mix Sound Enabled" else "Mix Sound Disabled", Toast.LENGTH_SHORT).show()
    }

    private fun stopSound() {
        playButtons.forEachIndexed { index, button ->
            if (isOnPlayArray[index]) {
                stopIndividualSound(mediaPlayers[index], button, index)
            }
        }
    }

    private fun addNewBar() {
        val inflater = LayoutInflater.from(this)
        val newBarView = inflater.inflate(R.layout.sound_bar_layout, soundsContainer, false)
        soundsContainer.addView(newBarView)

        val newSoundIndex = soundTitles.size

        val newSoundTitle = newBarView.findViewById<EditText>(R.id.soundTitle)
        val newPlayButton = newBarView.findViewById<ImageButton>(R.id.playSound)
        val newChooseButton = newBarView.findViewById<ImageButton>(R.id.chooseSound)
        val newRecordButton = newBarView.findViewById<ImageButton>(R.id.recordSound)
        val newDeleteButton = newBarView.findViewById<ImageButton>(R.id.deleteSound)

        soundTitles.add(newSoundTitle)
        playButtons.add(newPlayButton)
        chooseButtons.add(newChooseButton)
        recordButtons.add(newRecordButton)
        deleteButtons.add(newDeleteButton)
        mediaPlayers.add(null)
        soundUris.add(null)
        isOnPlayArray.add(false)
        isRecordingArray.add(false)
        recordedFilePaths.add(null)

        newChooseButton.setOnClickListener { chooseSound(newSoundIndex) }
        newPlayButton.setOnClickListener { playSound(soundUris[newSoundIndex], newPlayButton, newSoundIndex) }
        newRecordButton.setOnClickListener { toggleRecording(newSoundIndex) }
        newDeleteButton.setOnClickListener { deleteSound(newSoundIndex) }
    }

    private fun deleteSound(index: Int) {
        if (index < 3) {
            soundUris[index] = null
            soundTitles[index].setText("")
            mediaPlayers[index]?.release()
            mediaPlayers[index] = null
            isOnPlayArray[index] = false
            isRecordingArray[index] = false
            recordedFilePaths[index] = null
            playButtons[index].setImageResource(android.R.drawable.ic_media_play)
            soundTitles[index].setText(getString(R.string.no_sound_selected))
            Toast.makeText(this, "Sound $index deleted", Toast.LENGTH_SHORT).show()
        } else {
            soundTitles.removeAt(index)
            playButtons.removeAt(index)
            chooseButtons.removeAt(index)
            recordButtons.removeAt(index)
            deleteButtons.removeAt(index)
            mediaPlayers.removeAt(index)
            soundUris.removeAt(index)
            isOnPlayArray.removeAt(index)
            isRecordingArray.removeAt(index)
            recordedFilePaths.removeAt(index)
            soundsContainer.removeViewAt(index)
            Toast.makeText(this, "Sound $index deleted", Toast.LENGTH_SHORT).show()
        }
    }
}
