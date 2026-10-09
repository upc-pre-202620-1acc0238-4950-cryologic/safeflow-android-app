package com.safeflow.app.features.reporting.domain

data class Summary(val products: Int, val units: Int, val risks: Int, val activeIncidents: Int,
    val inTransit: Int, val critical: Int = 0, val warnings: Int = 0,
    val shipments: Int = 0, val preparation: Int = 0, val delivered: Int = 0)
