package fr.insset.projectloge2

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.location.LocationProvider
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityCompat.requestPermissions
import androidx.core.content.FileProvider
import androidx.databinding.DataBindingUtil
import fr.insset.projectloge2.databinding.ActivityMainBinding
import net.gotev.uploadservice.protocols.multipart.MultipartUploadRequest
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList


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
        const val photoChoosePicker = 1002
        const val photoCameraAction = 1001
        const val locationPermission = 2001
        const val oneMinute: Long = 60000
    }

    private lateinit var layout: ActivityMainBinding;
    private lateinit var photoHandler: PhotoHandler;
    private var pictureUrl = "";
    private lateinit var locationManager: LocationManager;
    private val providers : ArrayList<LocationProvider> = ArrayList<LocationProvider>()
    private var location : Location? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        layout = DataBindingUtil.setContentView(this, R.layout.activity_main);
        photoHandler = PhotoHandler();
        layout.photoHandler = photoHandler;
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        initGeoloc()
    }

    fun initGeoloc() {
        val names = locationManager.getProviders(true)

        for (name : String in names) {
            providers.add(locationManager.getProvider(name)!!)
        }

        initRequestLocationUpdate()
    }

    fun initRequestLocationUpdate() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            askForPermission()
            return
        }

        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, oneMinute, 150.0f, object: LocationListener {
            override fun onLocationChanged(location: Location?) {
                this@MainActivity.location = location
            }

            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {

            }

            override fun onProviderEnabled(provider: String?) {

            }

            override fun onProviderDisabled(provider: String?) {

            }

        })
    }

    fun askForPermission() {
        ActivityCompat.requestPermissions(this,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION), locationPermission);
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

    fun getLocation(): Location? {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            askForPermission()
            return locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        }
        return locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
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
                    val tt = getLocation()
                    Log.d("LOCATION!!!!!", String.format("%s", tt?.longitude))
                }
            } else if(requestCode == locationPermission){
                initRequestLocationUpdate()
            } else {
                super.onActivityResult(requestCode, resultCode, data)
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    @SuppressLint("SimpleDateFormat")
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
