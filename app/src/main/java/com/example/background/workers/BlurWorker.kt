package com.example.background.workers

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.background.R


private const val TAG = "BlurWorker"

class BlurWorker(
    val ctx: Context,
    val params: WorkerParameters
) : Worker(ctx, params) {

    override fun doWork(): Result {

        //CONTEXTO DE LA APLICACIÓN
        val appContext: Context = applicationContext

        //CREA UNA NOTIFICACIÓN PARA MOSTRAR UN MENSAJE
        makeStatusNotification("Desenfoque de la imagen", appContext)

        return try {

            //CREA UN RECURSO BITMAP, A PARTIR DE LA IMAGEN DE DRAWABLE
            val picture: Bitmap = BitmapFactory.decodeResource(
                appContext.resources,
                R.drawable.android_cupcake
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

            //TRABAJO HECHO EXITOSAMENTE
            Result.success()

        } catch (e: Throwable) {

            Log.e(TAG, "Error applying blur")

            //ERROR AL REALIZAR EL TRABAJO
            Result.failure()
        }

    }

}