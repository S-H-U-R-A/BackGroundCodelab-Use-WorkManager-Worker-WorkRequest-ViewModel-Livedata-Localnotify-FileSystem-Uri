package com.example.background

import android.app.Application
import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.work.*
import com.example.background.workers.BlurWorker
import com.example.background.workers.CleanupWorker
import com.example.background.workers.SaveImageToFileWorker


class BlurViewModel( application: Application ) : ViewModel() {

    //SE OBTIENE UNA INSTANCIA DE WORKMANAGER
    private val workManager: WorkManager = WorkManager.getInstance(application)

    internal var imageUri: Uri? = null
    internal var outputUri: Uri? = null

    //VARIABLE QUE VA A ESCUCHAR LOS CAMBIOS EN EL WORKREQUEST ACTUAL
    internal val outputWorkInfos: LiveData< List< WorkInfo> >

    //APENAS INICIA LA APP OBTENEMOS LA RUTA DE LA IMAGEN
    //QUE PRESENTAMOS EN PANTALLA
    init {
        imageUri = getImageUri( application.applicationContext )
        outputWorkInfos = workManager.getWorkInfosByTagLiveData(TAG_OUTPUT)
    }

    /**
     * Create the WorkRequest to apply the blur and save the resulting image
     * @param blurLevel The amount to blur the image
     */
    internal fun applyBlur(blurLevel: Int) {
        //EJEMPLO DE COMO SE LLAMA AL WORK MANAGER Y SE LE ENCOLA LA SOLICITUD
        //workManager.enqueue( OneTimeWorkRequest.from( BlurWorker::class.java ) )

        //OTRO EJEMPLO CREAMOS LA SOLICITUD PARA UNA VEZ
        //LE ENVIAMOS LA DATA  Y DESPUES SI ENCOLAMOS EN EL WORKMANAGER
/*        val blurRequest = OneTimeWorkRequestBuilder<BlurWorker>()
            .setInputData( createInputDataForUri() )
            .build()

        workManager.enqueue( blurRequest )*/

        //OBJETO DE ENCADENAMIENTO DE WORKERS
/*        var continuation = workManager
            .beginWith(
                //SE CREA EL PRIMER WORKREQUEST
                OneTimeWorkRequest.Companion.from(
                    CleanupWorker::class.java
                )
            )*/

        //OBJETO DE ENCADENAMIENTO DE TRABAJO UNICO
        var continuation = workManager.beginUniqueWork(
            //NOMBRE PARA LA CADENA DE TRABAJO
            IMAGE_MANIPULATION_WORK_NAME,
            //PARA LA TAREA SI ESTA EN EJECUCIÓN Y EJECUTA LA NUEVA
            ExistingWorkPolicy.REPLACE,
            //WORKRERQUEST
            OneTimeWorkRequest.from(
                CleanupWorker::class.java
            )


        )


/*        val blurRequest = OneTimeWorkRequest.Builder(
            //SE CREA EL SEGUNDO WORKER y SE LE PASA EL OBJETO DATA
            BlurWorker::class.java
        ).setInputData(
            createInputDataForUri()
        ).build()

        //SE AGREGA AL ENCADENAMIENTO
        continuation = continuation.then(
            blurRequest
        )*/

        //EJEMPLO USANDO 3 VECES EL WORKER DE DESENFOQUE
        for (i in 0 until blurLevel) {
            //SE CREA EL WORKREQUEST
            val blurBuilder = OneTimeWorkRequestBuilder<BlurWorker>()

            //LA PRIMERA VEZ EL OBJETO DATA SERÁ EL URI  imageUri QUE SE CREA AL INICIO DE ESTA CLASE
            //EN LAS SIGUIENTES ITERACIONES SE USARÁ EL URI RETORNANDO POR EL MISMO WORKER, Y ASI SUCESIVAMENTE
            if (i == 0) {
                blurBuilder.setInputData(createInputDataForUri())
            }

            //SE AGREGA A LA CADENA DE TRABAJO
            continuation = continuation.then(blurBuilder.build())
        }

        //SE CREA EL OBJETO DE LAS RESTRICIONES
        val constraints: Constraints = Constraints.Builder()
            .setRequiresCharging(true)
            .build()

        val save = OneTimeWorkRequest.Builder(
            //SE CREA EL TERCER WORKER
            SaveImageToFileWorker::class.java
        )
            .setConstraints( constraints )
            .addTag(TAG_OUTPUT)//ETIQUETA DE REFERENCIA PARA WORKREQUEST DE GUARDADO DE IMAGEN
            .build()

        //SE AGREGA AL ENCADENAMIENTO
        continuation = continuation.then(
            save
        )

        //POR ÚLTIMO SE ENVIA A LA COLA LOS TRABAJOS ENCADENADOS
        continuation.enqueue()

    }

    private fun uriOrNull(uriString: String?): Uri? {
        return if (!uriString.isNullOrEmpty()) {
            Uri.parse(uriString)
        } else {
            null
        }
    }

    //MÉTODO QUE RETORNA UN URI QUE REPRESENTA LA UBICACIÓN DE
    //UNA IMAGEN EN LA APP
    private fun getImageUri(context: Context): Uri {

        //OBTENEMOS REFERENCIA A LOS RECURSOS MEDIANTE EL CONTEXTO
        val resources = context.resources

        //CONSTRUYE LA URI USANDO EL FORMATO PREDEFINIDO EN ANDROID
        //PARA IDENTIFICAR LOS RECURSOS DE LA APLICACIÓN
        val imageUri = Uri.Builder()
            .scheme( ContentResolver.SCHEME_ANDROID_RESOURCE )
            //AUTHORITY SE USA PARA DEFINIR EL SERVIDOR O EL SISTEMA QUE OFRECE EL RECURSO
            //EN ESTE CASO ES NUESTRA PROPIA APP
            //SE CONCATENA /nombre.de.paquete
            .authority( resources.getResourcePackageName(R.drawable.android_cupcake) )
            //SE CONCATENA CON EL TIPO DE RECURSO /drawable
            .appendPath( resources.getResourceTypeName(R.drawable.android_cupcake))
            //SE CONCATENA CON EL NOMBRE DEL ARCHIVO /android_cupcake sin la extensión
            .appendPath( resources.getResourceEntryName(R.drawable.android_cupcake))
            .build()

        //GENERARÍA COMO SALIDA
        // : android.resource://com.example.myapp/drawable/android_cupcake

        return imageUri
    }

    internal fun setOutputUri(outputImageUri: String?) {
        outputUri = uriOrNull(outputImageUri)
    }

    //ESTE METODO CREA Y RETORNA UN OBJETO DATA,
    //QUE ES ALGO ASI COMO UN MAPA CLAVE VALOR
    //EN EL CUAL VAMOS A ALMCANAR LA URI DE LA IMAGEN
    private fun createInputDataForUri(): Data{

        val builder = Data.Builder()

        imageUri.let {
            builder.putString(KEY_IMAGE_URI, imageUri.toString())
        }

        return builder.build()

    }

    internal fun cancelWork(){
        //SE CANCELA EL TRABAJO ENCADENADO, SE PASA EL NOMBRE DEL TRABAJO ENCADENADO
        workManager.cancelUniqueWork( IMAGE_MANIPULATION_WORK_NAME )
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
