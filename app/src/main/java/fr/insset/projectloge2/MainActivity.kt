package fr.insset.projectloge2

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.databinding.DataBindingUtil
import fr.insset.projectloge2.databinding.ActivityMainBinding
import net.gotev.uploadservice.protocols.multipart.MultipartUploadRequest

class MainActivity : AppCompatActivity() {

    inner class PhotoHandler {
        fun onClickSelectPhoto(v: View) {
            pickFile()
        }
    }

    val photoChoosePicker = 2;

    private lateinit var layout: ActivityMainBinding;
    private lateinit var photoHandler: PhotoHandler;

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        layout = DataBindingUtil.setContentView(this, R.layout.activity_main);
        photoHandler = PhotoHandler();
        layout.photoHandler = photoHandler;
    }

    fun pickFile() {
        val i = Intent(Intent.ACTION_PICK)
        i.type = "image/*"
        val mimeTypes = arrayOf("image/jpeg", "image/png")
        i.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes)
        startActivityForResult(i, photoChoosePicker)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == photoChoosePicker && resultCode == Activity.RESULT_OK) {
            data?.let {
                onFilePicked(it.data.toString())
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun onFilePicked(filePath: String) {
        MultipartUploadRequest(this, serverUrl = "https://ptsv2.com/t/ji5a9-1582895613/post")
            .setMethod("POST")
            .addFileToUpload(
                filePath = filePath,
                parameterName = "photo"
            ).startUpload()
    }
}
