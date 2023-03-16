/*
 * Copyright (C) 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.background

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.work.WorkInfo
import com.example.background.databinding.ActivityBlurBinding

class BlurActivity : AppCompatActivity() {

    //SE OBTIENE EL VIEWMODEL
    private val viewModel: BlurViewModel by viewModels {
        BlurViewModel.BlurViewModelFactory(
            application
        )
    }

    //VARIABLE DE VINCULACIÓN
    private lateinit var binding: ActivityBlurBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //SE INFLA LA VISTA
        binding = ActivityBlurBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //SE CONFIGURA EL CLICK()
        binding.goButton.setOnClickListener {
            viewModel.applyBlur(blurLevel)
        }

        //SE OBSERVA EL OBJETO WORK INFO ASOCIADO AL WORKREQUEST QUE EJECUTA EL WORKER DE SAVE-IMAGE
        viewModel.outputWorkInfos.observe(this, workInfosObserver() )

        //EVENTO CLICK DEL BOTON DE VER ARCHIVO
        binding.seeFileButton.setOnClickListener {
            //SI EL VALOR DE OUTPUT URI NO ES NULO
            viewModel.outputUri?.let { currentUri ->
                //SI EL OUTPUT-URI TIENE UN VALOR INTENTAMOS ABRIR LA URI
                Intent(Intent.ACTION_VIEW, currentUri).apply {
                    resolveActivity(packageManager)?.run {
                        startActivity(this@apply)
                    }
                }
            }
        }

        //EVENTO CLICK DE CANCELAR EL TRABAJO
        binding.cancelButton.setOnClickListener { viewModel.cancelWork() }
    }

    // Define the observer function
    private fun workInfosObserver(): Observer< List<WorkInfo> > {
        return Observer { listOfWorkInfo ->

            // Note that these next few lines grab a single WorkInfo if it exists
            // This code could be in a Transformation in the ViewModel; they are included here
            // so that the entire process of displaying a WorkInfo is in one location.

            //SI NO HAY INFO EN LA LISTA ENVUELTA POR EL LIVEDATA
            //NO HAGA NADA Y SALGA DE LA FUNCIÓN
            if (listOfWorkInfo.isNullOrEmpty()) {
                return@Observer
            }

            //OBTENEMOS EL PRIMER ELEMENTO WORKINFO DE LA LISTA
            val workInfo: WorkInfo = listOfWorkInfo[0]

            //SI EL ESTADO DE ESTE WORKRWUEST ES FINALIZADO,
            //DE LO CONTRARIO SE SUPONE QUE ESTA EN EJECUCIÓN
            if ( workInfo.state.isFinished )  {
                showWorkFinished()

                //RECUPERAMOS LA SALIDA DEL WORKREQUEST, OSEA EL OBJETO DATA
                val outputImageUri = workInfo.outputData.getString( KEY_IMAGE_URI )

                //SI ESTA SALIDA NO ES NULA O VACIA
                //SETEAMOS EL VALOR DE LA VARIABLE DEL VIEMODEL
                //Y HABILITAMOS EL BOTON PARA LA VISUALIZACIÓN DE LA IMAGEN
                if (!outputImageUri.isNullOrEmpty() ){
                    viewModel.setOutputUri( outputImageUri )
                    binding.seeFileButton.visibility = View.VISIBLE
                }

            } else {
                showWorkInProgress()
            }
        }
    }

    /**
     * Shows and hides views for when the Activity is processing an image
     * MUESTRA Y OCULTA VISTAS CUANDO LA ACTIVIDAD ESTA PROCESANDO UNA IMAGEN
     */
    private fun showWorkInProgress() {
        with(binding) {
            progressBar.visibility = View.VISIBLE
            cancelButton.visibility = View.VISIBLE
            goButton.visibility = View.GONE
            seeFileButton.visibility = View.GONE
        }
    }

    /**
     * Shows and hides views for when the Activity is done processing an image
     * OCULTA VISTAS CUANDO EL ACTIVITY HA TERMINADO DE PROCESAR UNA IMAGEN
     */
    private fun showWorkFinished() {
        with(binding) {
            progressBar.visibility = View.GONE
            cancelButton.visibility = View.GONE
            goButton.visibility = View.VISIBLE
        }
    }

    //SEGUN EL RADIOGROUP SELECCIONADO SE CONFIGURA LA VARIABLE
    private val blurLevel: Int
        get() =
            when (binding.radioBlurGroup.checkedRadioButtonId) {
                R.id.radio_blur_lv_1 -> 1
                R.id.radio_blur_lv_2 -> 2
                R.id.radio_blur_lv_3 -> 3
                else -> 1
            }
}
