package com.example.background.workers

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.text.TextUtils
import android.util.Log
import androidx.work.Data
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.background.KEY_IMAGE_URI
import com.example.background.R


private const val TAG = "BlurWorker"

class BlurWorker(
    val ctx: Context,
    val params: WorkerParameters
) : Worker(ctx, params) {

    override fun doWork(): Result {

        //CONTEXTO DE LA APLICACIÓN
        val appContext: Context = applicationContext

        //OBTENEMOS LA URI DE LA IMAGEN PASADA
        //inputData ES LA FORMA EN COMO RECIBIMOS DATOS AL
        //CREAR UN WORKER
        val resourceUri = inputData.getString( KEY_IMAGE_URI )

        //CREA UNA NOTIFICACIÓN PARA MOSTRAR UN MENSAJE
        makeStatusNotification("Desenfoque de la imagen", appContext)

        sleep()

        return try {

            //CREA UN RECURSO BITMAP, A PARTIR DE LA IMAGEN DE DRAWABLE
/*            val picture: Bitmap = BitmapFactory.decodeResource(
                appContext.resources,
                R.drawable.android_cupcake
            )*/

            //VALIDAMOS QUE LA DATA ENVIADA NO ESTE VACIA
            //SI ES EL CASO DEVOLVEMOS UN ERROR
            if( TextUtils.isEmpty( resourceUri ) ){
                Log.e(TAG, "Invalid input uri")
                throw IllegalArgumentException("Invalid input URI")
            }

            //NOS RETORNA UNA INSTANCIA DEL RESOLVER PARA EL PAQUETE DE NUESTRA APP
            val resolver = appContext.contentResolver

            val picture = BitmapFactory.decodeStream(
                //ABRIMOS EL ARCHIVO/LIENZO DONDE VA A IR NUESTRA IMAGEN
                //GRACIASL A RESOLVER Y LA RUTA PASADA CUANDO SE CREO EL WORKER
                //DESDE EL WORKREQUEST EN EL VIEWMODEL
                resolver.openInputStream( Uri.parse(resourceUri) )
            )

            //VERSIÓN DESENFOCADA DEL BITMAP, LE PASA LA IMAGEN EN BITMAP Y EL CONTEXT
            val output: Bitmap = blurBitmap(
                picture,
                appContext
            )

            //CREA EL DIRECTORIO LE DA NOMBRE AL ARCHIVO Y CREAN EN EL ARCHIVO
            //LA IMAGEN DESENFOCADA, POR ULTIMO RETORNA LA URI DONDE SE GUARDO
            val outputUri: Uri = writeBitmapToFile(
                appContext,
                output
            )

            //SE MUESTRA OTRA NOTIFICACIÓN INDICANDO LA URI DEL ARCHIVO
            makeStatusNotification(
                "Output is $outputUri",
                appContext
            )

            //SE CREA UN OBJETO DATA DE RETORNO QUE NOS DEVOLVERA EL URI DONDE ESTA LA IMAGEN BORROSA
            val outputData: Data = workDataOf(KEY_IMAGE_URI to outputUri.toString())

            //TRABAJO HECHO EXITOSAMENTE
            Result.success( outputData )

        } catch (e: Throwable) {

            Log.e(TAG, "Error applying blur")

            e.printStackTrace()

            //ERROR AL REALIZAR EL TRABAJO
            Result.failure()
        }

    }

}