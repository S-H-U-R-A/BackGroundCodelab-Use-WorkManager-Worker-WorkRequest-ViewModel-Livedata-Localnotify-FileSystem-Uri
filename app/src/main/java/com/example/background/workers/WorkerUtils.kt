@file:JvmName("WorkerUtils")

package com.example.background.workers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.annotation.WorkerThread
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.renderscript.Allocation
import androidx.renderscript.Element
import androidx.renderscript.RenderScript
import androidx.renderscript.ScriptIntrinsicBlur
import com.example.background.CHANNEL_ID
import com.example.background.DELAY_TIME_MILLIS
import com.example.background.NOTIFICATION_ID
import com.example.background.NOTIFICATION_TITLE
import com.example.background.OUTPUT_PATH
import com.example.background.R
import com.example.background.VERBOSE_NOTIFICATION_CHANNEL_DESCRIPTION
import com.example.background.VERBOSE_NOTIFICATION_CHANNEL_NAME
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.util.UUID

private const val TAG = "WorkerUtils"

/**
 * Create a Notification that is shown as a heads-up notification if possible.
 *
 * For this codelab, this is used to show a notification so that you know when different steps
 * of the background work chain are starting
 *
 * @param message Message shown on the notification
 * @param context Context needed to create Toast
 */
fun makeStatusNotification(message: String, context: Context) {

    // Make a channel if necessary
    //SI EL SDK ES MAYOR O IGUAL A ANDROID OREO
    if ( Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ) {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library

        //SE CONFIGURA EL CANAL
        val name = VERBOSE_NOTIFICATION_CHANNEL_NAME
        val description = VERBOSE_NOTIFICATION_CHANNEL_DESCRIPTION
        val importance = NotificationManager.IMPORTANCE_HIGH

        //SE CREA EL CANAL
        val channel = NotificationChannel(CHANNEL_ID, name, importance)
        channel.description = description

        // Add the channel
        //SE OBTIENE EL NOTIFICATION MANAGER
        val notificationManager = context.getSystemService( Context.NOTIFICATION_SERVICE ) as NotificationManager?

        //SE CREA LA NOTIFICACIÓN EN EL CANAL
        notificationManager?.createNotificationChannel( channel )

    }

    // SE CREA Y CONFIGURA LA NOTIFICACIÓN, QUE ES EL MENSAJE QUE ENTRA DE ARRIBA EN ANDROID
    val builder = NotificationCompat.Builder(
        context,
        CHANNEL_ID
    )
        .setSmallIcon(R.drawable.ic_launcher_foreground)
        .setContentTitle(NOTIFICATION_TITLE)
        .setContentText(message)
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setVibrate(LongArray(0))

    // SE MUESTRA LA NOTIFICACIÓN
    NotificationManagerCompat.from( context ).notify( NOTIFICATION_ID, builder.build() )

}

/**
 * Method for sleeping for a fixed amount of time to emulate slower work
 */
fun sleep() {
    try {
        Thread.sleep(DELAY_TIME_MILLIS, 0)
    } catch (e: InterruptedException) {
        Log.e(TAG, e.message.toString())
    }

}

/**
 * Blurs the given Bitmap image
 * @param bitmap Image to blur
 * @param applicationContext Application context
 * @return Blurred bitmap image
 */
@WorkerThread
fun blurBitmap(bitmap: Bitmap, applicationContext: Context): Bitmap {

    //RENDERSCRIPT AUN NO SE QUE HACE
    lateinit var rsContext: RenderScript

    try {

        // Create the output bitmap
        val output = Bitmap.createBitmap(
            //SE TOMAN LAS MEDIDAS Y CONFIGURACIONES DEL BITMAP RECIBIDO
            bitmap.width,
            bitmap.height,
            bitmap.config
        )

        // Blur the image
        rsContext = RenderScript.create(
            applicationContext,
            RenderScript.ContextType.DEBUG
        )

        val inAlloc = Allocation.createFromBitmap(rsContext, bitmap)
        val outAlloc = Allocation.createTyped(rsContext, inAlloc.type)

        val theIntrinsic = ScriptIntrinsicBlur.create(rsContext, Element.U8_4(rsContext))

        theIntrinsic.apply {
            setRadius(10f)
            theIntrinsic.setInput(inAlloc)
            theIntrinsic.forEach(outAlloc)
        }

        outAlloc.copyTo( output )

        return output

    } finally {
        rsContext.finish()
    }
}

/**
 * Writes bitmap to a temporary file and returns the Uri for the file
 * @param applicationContext Application context
 * @param bitmap Bitmap to write to temp file
 * @return Uri for temp file with bitmap
 * @throws FileNotFoundException Throws if bitmap file cannot be found
 */

@Throws(FileNotFoundException::class)
fun writeBitmapToFile(applicationContext: Context, bitmap: Bitmap): Uri {

    //SE CREA UN NOMBRE PARA EL ARCHIVO
    val name = String.format("blur-filter-output-%s.png", UUID.randomUUID().toString() )


    //EL DIRECTORIO INDICADO POR fileDir
    //QUE ES UNA CARPETA CON EL NOMBRE DE FILES QUE SE ENCUENTRA EN LA CARPETA
    //DEL PAQUETE DE LA APLICACIÓN
    val outputDir = File(
        applicationContext.filesDir, //CREA LA CARPETA FILES
        OUTPUT_PATH// NOMBRE DE LO QUE QUEREMOS CREAR
    )

    //SI EL DIRECTORIO/CARPETA NO EXISTE LO CREA
    if ( !outputDir.exists() ) {
        // ACA LE DECIMOS QUE CON ESE NOMBRE GUARDADO EN OUTPUT_PATH CREE UN DIRECTORIO
        outputDir.mkdirs()
    }

    //SE CONFIGURA EL ARCHIVO DE SALIDA, CON SU NOMBRE Y EN QUE DIRECTORIO DEBE ESTAR
    val outputFile = File(
        //DIRECTORIO DONDE SE VA A ALMACENAR EL ARCHIVO
        outputDir,
        //NOMBRE DEL ARCHIVO
        name
    )

    //ARCHIVO
    var out: FileOutputStream? = null

    try {
        //ABRIMOS EL ARCHIVO PARA EDICIÓN
        out = FileOutputStream(outputFile)

        //ACÁ SE CREA EL ARCHIVO EN FISICO COMO TAL Y
        //SE PINTA LA IMAGEN EN EL ARCHIVO
        bitmap.compress(
            Bitmap.CompressFormat.PNG,
            0 /* ignored for PNG */,
            out
        )

    } finally {
        //CERRAMOS EL ARCHIVO EDITABLE
        out?.let {
            try {
                it.close()
            } catch (ignore: IOException) {
            }

        }
    }

    //OBTENEMOS EL URI DEL ARCHIVO Y LO RETORNAMOS
    return Uri.fromFile(outputFile)
}
