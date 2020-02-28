package fr.insset.projectloge2

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import androidx.core.content.FileProvider
import androidx.databinding.DataBindingUtil
import androidx.documentfile.provider.DocumentFile
import fr.insset.projectloge2.databinding.ActivityMainBinding
import net.gotev.uploadservice.protocols.multipart.MultipartUploadRequest
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    inner class PhotoHandler {
        fun onClickSelectPhoto(v: View) {
            pickPhoto()
        }

        fun onClickUpload(v: View) {
            upload()
        }

        fun onClickTakePhoto(c: View) {
            takePhoto()
        }
    }

    companion object {
        const val photoChoosePicker = 2;
        const val photoCameraAction = 1;
    }

    private lateinit var layout: ActivityMainBinding;
    private lateinit var photoHandler: PhotoHandler;
    private var pictureUrl = "";

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        layout = DataBindingUtil.setContentView(this, R.layout.activity_main);
        photoHandler = PhotoHandler();
        layout.photoHandler = photoHandler;
    }

    fun pickPhoto() {
        val i = Intent(Intent.ACTION_PICK)
        i.type = "image/*"
        val mimeTypes = arrayOf("image/jpeg", "image/png")
        i.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes)
        startActivityForResult(i, photoChoosePicker)
    }

    fun takePhoto() {
        val i = Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            takePictureIntent.resolveActivity(packageManager)?.also {
                val photoFile: File? = try {
                    createImageFile()
                } catch (ex: IOException) {
                    null
                }
                photoFile?.also {
                    val photoURI: Uri = FileProvider.getUriForFile(
                        this,
                        "fr.insset.projectloge2.fileprovider",
                        it
                    )
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                    startActivityForResult(takePictureIntent, photoChoosePicker)
                }
            }
        }
    }

    private fun galleryAddPic() {
        Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE).also { mediaScanIntent ->
            val f = File(pictureUrl)
            mediaScanIntent.data = Uri.fromFile(f)
            sendBroadcast(mediaScanIntent)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == photoChoosePicker) {
                data?.let {
                    layout.imageView2.setImageURI(it.data)
                    pictureUrl = it.data.toString()
                    layout.uploadButton.isEnabled = true
                    layout.uploadButton.isClickable = true
                }
            } else {
                super.onActivityResult(requestCode, resultCode, data)
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "JPEG_${timeStamp}_", /* prefix */
            ".jpg", /* suffix */
            storageDir /* directory */
        ).apply {
            pictureUrl = absolutePath
        }
    }

    private fun upload() {
        if (pictureUrl.isNotEmpty()) {
            MultipartUploadRequest(this, serverUrl = "https://ptsv2.com/t/ji5a9-1582895613/post")
                .setMethod("POST")
                .addFileToUpload(
                    filePath = pictureUrl,
                    parameterName = "photo"
                ).startUpload()
        }
    }
}
