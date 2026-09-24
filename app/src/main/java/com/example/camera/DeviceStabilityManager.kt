package com.example.camera

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.sqrt

/**
 * Gestor del sensor de movimiento y aceleración para calcular en tiempo real
 * la estabilidad del dispositivo.
 * 
 * Se utiliza para avisar al usuario si la mano tiembla mientras se toman
 * capturas fotográficas en el modo de resolución nativa máxima (High-Res / Ultra MP),
 * evitando fotos movidas o trepidadas.
 *
 * @param context Contexto de la aplicación Android.
 */
class DeviceStabilityManager(context: Context) : SensorEventListener, DefaultLifecycleObserver {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private val _isDeviceSteady = MutableStateFlow(true)
    val isDeviceSteady: StateFlow<Boolean> = _isDeviceSteady.asStateFlow()

    private var lastX = 0f
    private var lastY = 0f
    private var lastZ = 0f
    private var isFirstReading = true

    // Umbral de aceleración diferencial para considerar que el dispositivo está quieto
    private val stabilityThreshold = 0.45f

    /**
     * Inicia el registro del acelerómetro.
     */
    fun startListening() {
        accelerometer?.let {
            sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    /**
     * Detiene el sensor para conservar batería cuando no se use.
     */
    fun stopListening() {
        sensorManager?.unregisterListener(this)
        isFirstReading = true
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || event.sensor.type != Sensor.TYPE_ACCELEROMETER) return

        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]

        if (isFirstReading) {
            lastX = x
            lastY = y
            lastZ = z
            isFirstReading = false
            return
        }

        val deltaX = x - lastX
        val deltaY = y - lastY
        val deltaZ = z - lastZ

        lastX = x
        lastY = y
        lastZ = z

        // Magnitud del delta de movimiento
        val movementMagnitude = sqrt((deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ).toDouble()).toFloat()

        // Si el delta es menor que el umbral, el teléfono se mantiene quieto
        _isDeviceSteady.value = movementMagnitude < stabilityThreshold
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // No requiere implementación para acelerómetro general
    }

    override fun onResume(owner: LifecycleOwner) {
        startListening()
    }

    override fun onPause(owner: LifecycleOwner) {
        stopListening()
    }
}
