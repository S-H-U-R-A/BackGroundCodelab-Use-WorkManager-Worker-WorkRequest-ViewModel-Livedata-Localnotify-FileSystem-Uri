package com.example.background.workers

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.background.KEY_IMAGE_URI
import java.text.SimpleDateFormat
import java.util.*

private const val TAG = "SaveImageToFileWorker"

class SaveImageToFileWorker(
    ctx: Context,
    params: WorkerParameters
) : Worker(ctx, params) {

    //NOMBRE DE LA IMAGEN
    private val title: String = "Blurred Image"
    //SE CREA UN FORMATO PARA FECHAS
    private val dateFormatter: SimpleDateFormat = SimpleDateFormat(
        "yyyy.MM.dd 'at' HH:mm:ss z",
        Locale.getDefault()
    )

    override fun doWork(): Result {

        //SE MUESTRA UNA NOTIFICACIÓN
        makeStatusNotification(
            "Guandando Imagen",
            applicationContext
        )
        //SE MUESTRA UN RETRASO PARA PODER VER LA NOTIFICACIÓN
        sleep()

        //SE OBTIENE ACCESO AL PAQUETE DE LA APLICACIÓN
        val resolver = applicationContext.contentResolver

        return try {
            //SE RECUPERA EL VALOR ENVIADO AL WORKER
            val resourceUri = inputData.getString( KEY_IMAGE_URI )

            //SE CREA UNA IMAGEN BITMAP, A PARTIR DE LA URI PASADA AL WORKER
            val bitmap = BitmapFactory.decodeStream(
                resolver.openInputStream( Uri.parse(resourceUri) )
            )

            //SE CREA LA IMAGEN
            val imageUrl = MediaStore.Images.Media.insertImage(
                resolver,
                bitmap,
                title,
                dateFormatter.format(Date())
            )

            //SI LA IMÁGEN NO ES NULA CREAMOS UN OBJETO DATA
            //COMO SALIDA DEL WORKER
            if ( !imageUrl.isNullOrEmpty() ) {

                val output = workDataOf(KEY_IMAGE_URI to imageUrl)

                Result.success(output)

            } else {
                Log.e(TAG, "Writing to MediaStore failed")
                Result.failure()
            }

        }catch (e: Exception){
            e.printStackTrace()
            Result.failure()
        }

    }

}