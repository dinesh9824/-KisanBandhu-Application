package com.example.kisanbandhuai_basedcroprecommendationanddecisionsupportmobileapplication

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import java.nio.FloatBuffer
import java.nio.DoubleBuffer

class CropRecommendationViewModel(application: Application) : AndroidViewModel(application) {

    private val useOfflineModel = true 

    private val _predictionResult = MutableLiveData<CropResponse?>()
    val predictionResult: LiveData<CropResponse?> = _predictionResult

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val cropLabels = listOf(
        "rice", "maize", "chickpea", "kidneybeans", "pigeonpeas",
        "mothbeans", "mungbean", "blackgram", "lentil", "pomegranate",
        "banana", "mango", "grapes", "watermelon", "muskmelon",
        "apple", "orange", "papaya", "coconut", "cotton", "jute", "coffee"
    )

    fun predictCrop(request: CropRequest) {
        _isLoading.value = true
        if (useOfflineModel) {
            predictCropOffline(request)
        } else {
            predictCropOnline(request)
        }
    }

    private fun predictCropOnline(request: CropRequest) {
        RetrofitClient.api.getCrop(request).enqueue(object : Callback<CropResponse> {
            override fun onResponse(call: Call<CropResponse>, response: Response<CropResponse>) {
                _isLoading.value = false
                if (response.isSuccessful) {
                    _predictionResult.value = response.body()
                } else {
                    _error.value = "Server Error: ${response.code()}"
                }
            }
            override fun onFailure(call: Call<CropResponse>, t: Throwable) {
                _isLoading.value = false
                _error.value = "Connection Failed: ${t.message}"
            }
        })
    }

    private fun predictCropOffline(request: CropRequest) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val env = OrtEnvironment.getEnvironment()
                val assetManager = getApplication<Application>().assets
                val modelBytes = assetManager.open("crop_model.onnx").readBytes()
                val session = env.createSession(modelBytes)

                val inputName = session.inputNames.iterator().next()
                
                // Prepare input data
                val floatArray = floatArrayOf(
                    request.N, request.P, request.K,
                    request.temperature, request.humidity, request.ph, request.rainfall
                )
                
                val inputTensor = OnnxTensor.createTensor(env, FloatBuffer.wrap(floatArray), longArrayOf(1, 7))

                val results = session.run(mapOf(inputName to inputTensor))
                
                // results[0] -> Label array (e.g., ["jute"])
                // results[1] -> Probability map/array
                
                val predictedLabelOutput = results[0].value
                val probabilitiesOutput = results[1].value
                
                var top3Predictions = mutableListOf<CropPrediction>()
                var recommendedCrop = "Unknown"

                // 1. Extract Recommended Crop Name
                recommendedCrop = when (predictedLabelOutput) {
                    is Array<*> -> {
                        val first = predictedLabelOutput[0]
                        if (first is Array<*>) first[0].toString() else first.toString()
                    }
                    is LongArray -> predictedLabelOutput[0].toString()
                    else -> predictedLabelOutput.toString().replace(Regex("[\\[\\]]"), "")
                }

                // 2. Extract Probabilities and Calculate Top 3
                if (probabilitiesOutput is Array<*>) {
                    val firstRow = probabilitiesOutput[0]
                    if (firstRow is FloatArray) {
                        val probs = firstRow
                        top3Predictions = probs.mapIndexed { index, prob ->
                            CropPrediction(cropLabels.getOrElse(index) { "Unknown" }, prob)
                        }.sortedByDescending { it.probability }.take(3).toMutableList()
                    } else if (firstRow is Map<*, *>) {
                        @Suppress("UNCHECKED_CAST")
                        val probMap = firstRow as Map<String, Float>
                        top3Predictions = probMap.map { CropPrediction(it.key, it.value) }
                            .sortedByDescending { it.probability }.take(3).toMutableList()
                    }
                }

                results.close()
                inputTensor.close()
                session.close()

                withContext(Dispatchers.Main) {
                    _isLoading.value = false
                    _predictionResult.value = CropResponse(
                        success = true,
                        recommended_crop = recommendedCrop,
                        top_predictions = top3Predictions
                    )
                }

            } catch (e: Exception) {
                Log.e("ONNX_ERROR", "Error running model", e)
                withContext(Dispatchers.Main) {
                    _isLoading.value = false
                    _error.value = "Model Error: ${e.message}"
                }
            }
        }
    }
}