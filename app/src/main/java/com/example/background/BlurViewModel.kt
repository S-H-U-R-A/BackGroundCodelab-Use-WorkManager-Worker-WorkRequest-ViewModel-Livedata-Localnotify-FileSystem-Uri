package com.example.background

import android.app.Application
import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.work.Data
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.example.background.workers.BlurWorker


class BlurViewModel( application: Application ) : ViewModel() {

    //SE OBTIENE UNA INSTANCIA DE WORKMANAGER
    private val workManager: WorkManager = WorkManager.getInstance(application)

    internal var imageUri: Uri? = null
    internal var outputUri: Uri? = null

    init {
        imageUri = getImageUri( application.applicationContext )
    }

    /**
     * Create the WorkRequest to apply the blur and save the resulting image
     * @param blurLevel The amount to blur the image
     */
    internal fun applyBlur(blurLevel: Int) {
        //SE LLAMA AL WORK MANAGER Y SE LE ENCOLA LA SOLICITUD
        workManager.enqueue( OneTimeWorkRequest.from( BlurWorker::class.java ) )
    }

    private fun uriOrNull(uriString: String?): Uri? {
        return if (!uriString.isNullOrEmpty()) {
            Uri.parse(uriString)
        } else {
            null
        }
    }

    private fun getImageUri(context: Context): Uri {
        val resources = context.resources

        val imageUri = Uri.Builder()
            .scheme( ContentResolver.SCHEME_ANDROID_RESOURCE )
            .authority( resources.getResourcePackageName(R.drawable.android_cupcake) )
            .appendPath(resources.getResourceTypeName(R.drawable.android_cupcake))
            .appendPath(resources.getResourceEntryName(R.drawable.android_cupcake))
            .build()

        return imageUri
    }

    internal fun setOutputUri(outputImageUri: String?) {
        outputUri = uriOrNull(outputImageUri)
    }

    private fun createInputDataForUri(): Data{

        val builder = Data.Builder()

        imageUri.let {
            builder.putString(KEY_IMAGE_URI, imageUri.toString())
        }

        return builder.build()

    }

    //FABRICA DEL VIEWMODEL
    class BlurViewModelFactory(private val application: Application) :
        ViewModelProvider.Factory {

        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return if ( modelClass.isAssignableFrom( BlurViewModel::class.java ) ) {
                @Suppress("UNCHECKED_CAST")
                BlurViewModel(application) as T
            } else {
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }

    }

}
