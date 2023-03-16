package com.example.background.workers

import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.background.OUTPUT_PATH
import java.io.File


private const val TAG = "CleanupWorker"

class CleanupWorker(
    ctx: Context,
    params: WorkerParameters
) : Worker(ctx, params){

    override fun doWork(): Result {

        //SE MUESTRA UNA NOTIFICACIÓN
        makeStatusNotification(
            "Limpiando archivos temporales",
            applicationContext
        )

        sleep()

        return try {

            //SE CREA UN OBJETO FILE
            val outputDirectory = File(
                applicationContext.filesDir,
                OUTPUT_PATH
            )

            //SI EL FILE EXISTE
            if( outputDirectory.exists() ){

                //LISTAMOS LOS ARCHIVOS
                val entries = outputDirectory.listFiles()

                //VERIFICAMOS SI NO ES NULL
                if(entries != null){

                    //RECORREMOS LOS ARCHIVOS SI EXISTEN
                    for (entry in entries){
                        //RECUPERAMOS EL NOMBRE DE CADA ARCHIVO
                        val name = entry.name
                        //SI EL NOMBRE NO ES VACIO Y EL ARCHIVO TERMINA EN EXTENSIÓN PNG
                        if(name.isNotEmpty() && name.endsWith(".png")){
                            //SE ELIMINA EL ARCHIVO
                            val deleted = entry.delete()
                            Log.i(TAG, "Deleted $name - $deleted")
                        }

                    }

                }

            }

            Result.success()

        } catch (e: java.lang.Exception){
            e.printStackTrace()
            Result.failure()
        }

    }

}