package com.safeflow.app.features.monitoring.domain
import com.safeflow.app.core.domain.Records

enum class ThermalCondition(val label: String) {
    NORMAL("Normal"), WARNING("Advertencia"), RISK("Riesgo"), CRITICAL("Crítico"),
    DISCONNECTED("Desconectado"), UNKNOWN("Sin lecturas")
}
data class Sensor(val id: String, val productId: String, val location: String, val connected: Boolean = true)
data class Reading(val id: String, val sensorId: String, val at: Long, val temperature: Double,
    val condition: ThermalCondition, val shipmentId: String? = null)
data class RejectedReading(val id: String, val sensorId: String, val at: Long, val raw: String, val reason: String)
interface SensorRepository : Records<Sensor>
interface ReadingRepository : Records<Reading>
interface RejectionRepository : Records<RejectedReading>
object ThermalRules {
    fun evaluate(value: Double, minimum: Double, maximum: Double): ThermalCondition {
        require(value.isFinite() && minimum.isFinite() && maximum.isFinite() && minimum <= maximum)
        val deviation = maxOf(minimum - value, value - maximum)
        if (deviation > 2.0) return ThermalCondition.CRITICAL
        if (deviation > 0.0) return ThermalCondition.RISK
        val width = maximum - minimum
        if (width > 0 && (value - minimum <= width * .1 || maximum - value <= width * .1))
            return ThermalCondition.WARNING
        return ThermalCondition.NORMAL
    }
}
