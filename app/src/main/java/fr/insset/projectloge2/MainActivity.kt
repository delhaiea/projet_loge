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
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import androidx.databinding.DataBindingUtil
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import com.google.android.gms.location.*
import fr.insset.projectloge2.databinding.ActivityMainBinding
import net.gotev.uploadservice.protocols.multipart.MultipartUploadRequest
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit


class MainActivity : AppCompatActivity() {

    private var periodicNotificationRequest = PeriodicWorkRequest.Builder(
        NotificationWorker::class.java, 15, TimeUnit.MINUTES
    ).build()


    inner class PhotoHandler {
        fun onClickSelectPhoto(v: View) {
            pickPhoto()
        }

        fun onClickUpload(v: View) {
            upload()
        }

        fun onClickTakePhoto(v: View) {
            takePhoto()
        }
    }

    companion object {
        const val photoChoosePicker = 1002
        const val serverAddress = "https://wildtortoise-api.appspot.com"
    }

    private lateinit var layout: ActivityMainBinding
    private lateinit var photoHandler: PhotoHandler
    private var pictureUrl = ""

    private lateinit var locationCallback: LocationCallback
    private lateinit var locationRequest: LocationRequest
    private var requestingLocationUpdates: Boolean = true
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationManager: LocationManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        layout = DataBindingUtil.setContentView(this, R.layout.activity_main);
        photoHandler = PhotoHandler();
        layout.photoHandler = photoHandler;

        callNotificationWorker()
        layout = DataBindingUtil.setContentView(this, R.layout.activity_main)
        photoHandler = PhotoHandler()
        layout.photoHandler = photoHandler
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        locationRequest = LocationRequest.create();
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult?) {
                locationResult ?: return
                for (location in locationResult.locations){
                    // Update UI with location data
                    // ...
                }
            }
        }

        callNotificationWorker()
    }

    override fun onResume() {
        super.onResume()
        if (requestingLocationUpdates) startLocationUpdates()
    }

    private fun startLocationUpdates() {
        fusedLocationClient.requestLocationUpdates(locationRequest,
            locationCallback,
            Looper.getMainLooper())
    }

    fun pickPhoto() {
        val i = Intent(Intent.ACTION_PICK)
        i.type = "image/*"
        val mimeTypes = arrayOf("image/jpeg", "image/png")
        i.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes)
        startActivityForResult(i, photoChoosePicker)
    }

    fun takePhoto() {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
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

    private fun getLastKnownLocation(): Location? {
        val providers: List<String> = locationManager.getProviders(true)
        var bestLocation: Location? = null
        for (provider in providers) {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return null;
            }
            val l: Location? = locationManager.getLastKnownLocation(provider)
            Log.d("TAG", String.format("last known location, provider: %s, location: %s", provider,l))
            if (l == null) {
                continue
            }
            if (bestLocation == null
                || l.accuracy < bestLocation.accuracy
            ) {
                Log.d("TAG", String.format("found best last known location: %s", l))
                bestLocation = l
            }
        }
        return bestLocation
    }

    private fun findLocation(c: Context) {
        Log.d("Find Location", "in findLocation")
        val providers: List<String>  = locationManager.getProviders(true)
        for (provider: String in providers) {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
            locationManager.requestLocationUpdates(provider, 60000, 150.0f, object:
                LocationListener {
                override fun onLocationChanged(location: Location?) {

                }

                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {

                }

                override fun onProviderEnabled(provider: String?) {

                }

                override fun onProviderDisabled(provider: String?) {

                }

            })
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
                    getLastKnownLocation().also { location: Location? ->
                        Log.d("TAG", String.format("%d, %d", location?.latitude, location?.longitude)) }
                }
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
            MultipartUploadRequest(this, serverUrl = String.format("%s/photo", serverAddress))
                .setMethod("POST")
                .addFileToUpload(
                    filePath = pictureUrl,
                    parameterName = "photo"
                ).startUpload()
        }
    }

    private fun callNotificationWorker(){
        /*
        * will create a unique request to send a notification at the start of the activity
        * This will create a notification when we launch the application which is intended
        * */
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "periodicNotification",
            ExistingPeriodicWorkPolicy.KEEP,
            periodicNotificationRequest
        )
    }
}
