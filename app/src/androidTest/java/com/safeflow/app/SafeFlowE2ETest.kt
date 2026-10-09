package com.safeflow.app

import androidx.compose.ui.test.*
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.test.platform.app.InstrumentationRegistry
import com.safeflow.app.core.domain.*
import com.safeflow.app.core.infrastructure.local.LocalDatabase
import com.safeflow.app.features.iam.application.IdentityService
import com.safeflow.app.features.iam.domain.*
import com.safeflow.app.features.iam.infrastructure.local.LocalSession
import com.safeflow.app.features.inventory.application.InventoryService
import com.safeflow.app.features.inventory.domain.*
import com.safeflow.app.features.inventory.infrastructure.repository.*
import com.safeflow.app.features.monitoring.application.MonitoringService
import com.safeflow.app.features.monitoring.domain.*
import com.safeflow.app.features.alerts.application.AlertService
import com.safeflow.app.features.logistics.application.LogisticsService
import com.safeflow.app.features.logistics.domain.*
import dagger.hilt.android.testing.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.*
import org.junit.Assert.*
import javax.inject.Inject

@HiltAndroidTest
class SafeFlowE2ETest {
    @get:Rule(order = 0) val hilt = HiltAndroidRule(this)
    @get:Rule(order = 1) val ui = createAndroidComposeRule<MainActivity>()
    @Inject lateinit var identity: IdentityService
    @Inject lateinit var inventory: InventoryService
    @Inject lateinit var monitoring: MonitoringService
    @Inject lateinit var alerts: AlertService
    @Inject lateinit var logistics: LogisticsService
    @Inject lateinit var workspace: com.safeflow.app.main.application.ObserveWorkspace
    @Inject lateinit var reporting: com.safeflow.app.features.reporting.application.ReportingService
    @Inject lateinit var clock: TestClock
    @Inject lateinit var storage: TestStorage
    @Before fun ready() { hilt.inject(); waitTag("screen-login") }
    @After fun dispose() { ui.activityRule.scenario.close(); storage.dispose() }
    private fun capture(name: String) {
        ui.runOnIdle { ui.activity.window.insetsController?.hide(android.view.WindowInsets.Type.ime()) }
        ui.mainClock.advanceTimeBy(600)
        ui.waitForIdle()
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val directory = java.io.File(InstrumentationRegistry.getArguments().getString("additionalTestOutputDir")
            ?: context.getExternalFilesDir(null)!!.absolutePath).resolve("e2e-captures").also { it.mkdirs() }
        directory.resolve("$name.png").outputStream().use {
            ui.onNodeWithTag("prototype-viewport").captureToImage().asAndroidBitmap().compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
        }
    }
    private fun waitTag(tag: String) { ui.waitUntil(15000) { ui.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty() } }
    private fun waitText(text: String) { ui.waitUntil(15000) { ui.onAllNodesWithText(text, substring = true).fetchSemanticsNodes().isNotEmpty() } }
    private fun click(text: String) { ui.onNode(hasText(text) and hasClickAction()).performScrollTo().performClick() }
    private fun input(tag: String, value: String) { ui.onNodeWithTag(tag).performScrollTo().performTextReplacement(value) }
    private fun login(role: String) {
        waitTag("login-email"); input("login-email", "$role.test1@gmail.com"); input("login-password", "@${role}test1")
        ui.onNodeWithTag("login-submit").performScrollTo().performClick()
        waitTag("screen-${if (role == "operador") "inventory" else if (role == "supervisor") "summary" else "permissions"}")
    }
    private fun logout() {
        ui.onNodeWithText("Cuenta").performClick(); waitTag("logout"); ui.onNodeWithTag("logout").performScrollTo().performClick(); waitTag("screen-login")
    }
    private fun createProduct(name: String = "Vacuna de prueba"): String {
        click("Registrar producto"); waitTag("product-name")
        input("product-name", name); input("product-min", "8"); input("product-max", "2"); ui.onNodeWithTag("product-submit").assertIsNotEnabled()
        waitText("La temperatura mínima no puede superar")
        input("product-min", "2"); input("product-max", "8"); click("Registrar producto"); waitTag("screen-product")
        waitText(name)
        click("Registrar lote"); input("lot-code", "LOT-001"); click("Registrar lote"); waitTag("screen-lot-success"); waitText("Lote registrado")
        click("Registrar ingreso"); input("income-quantity", "0"); ui.onNodeWithTag("income-submit").assertIsNotEnabled(); waitText("entero mayor que cero")
        input("income-quantity", "10"); click("Registrar ingreso"); waitTag("screen-lot-success"); waitText("10 unidades"); click("Volver al producto"); waitTag("screen-product")
        return runBlocking { inventory.products.all().single().id }
    }
    @Test fun emptyInventoryRegistrationAndPersistenceWithoutSensors() {
        login("operador"); waitText("Sin productos")
        val productId = createProduct()
        capture("product-detail")
        ui.onNodeWithText("Vincular sensor").performScrollTo().assertIsNotEnabled()
        ui.onNodeWithText("Revisar conectividad").performScrollTo().assertIsNotEnabled()
        click("Registrar lote"); input("lot-code", "LOT-001"); ui.onNodeWithTag("lot-submit").assertIsNotEnabled(); waitText("ya existe")
        ui.onNodeWithContentDescription("Volver").performClick()
        ui.onNodeWithText("Despachos").performClick(); click("Registrar despacho")
        click("Seleccionar producto"); waitTag("screen-shipment-product"); click("Vacuna de prueba"); click("Seleccionar lote"); waitTag("screen-shipment-lot"); click("Lote LOT-001"); input("shipment-quantity", "3"); input("shipment-destination", "Clínica Lima")
        ui.onNodeWithTag("shipment-submit").performScrollTo().assertIsNotEnabled(); click("Seleccionar sensor"); waitText("Sin sensores disponibles"); click("Cancelar")
        ui.onNodeWithTag("shipment-destination").assertTextEquals("Clínica Lima")
        runBlocking { assertTrue(logistics.shipments.all().isEmpty()); assertEquals(0, inventory.products.find(productId)!!.reserved) }
        ui.activityRule.scenario.recreate(); waitTag("screen-shipment-form")
        ui.onNodeWithTag("shipment-destination").assertTextEquals("Clínica Lima")
        ui.activityRule.scenario.close()
        val restarted = androidx.test.core.app.ActivityScenario.launch(MainActivity::class.java)
        try { waitTag("screen-inventory"); waitText("Vacuna de prueba") } finally { restarted.close() }
        ui.activityRule.scenario.close(); storage.scope.cancel(); storage.db.close()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val reopened = LocalDatabase.open(context, storage.dbFile.absolutePath)
        runBlocking {
            val product = LocalProductRepository(reopened.records()).find(productId)!!
            assertEquals(10, product.stock); assertEquals(0, product.reserved)
            assertEquals(1, LocalLotRepository(reopened.records()).all().size)
        }
        reopened.close()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val store = PreferenceDataStoreFactory.create(scope = scope, produceFile = { storage.prefFile })
        runBlocking { assertEquals("test-operador-1", LocalSession(store).current()!!.accountId) }
        scope.cancel()
    }
    @Test fun thermalDispatchAlertsResolutionAndDelivery() {
        login("operador"); val productId = createProduct()
        // Exclusive fixture; production starts with zero sensors and exposes no ingestion controls.
        runBlocking { monitoring.sensors.save("E2E-S1", Sensor("E2E-S1", productId, "Almacén de prueba")) }
        ui.onNodeWithText("Despachos").performClick(); click("Registrar despacho"); click("Seleccionar producto"); click("Vacuna de prueba"); click("Seleccionar lote"); click("Lote LOT-001")
        input("shipment-quantity", "3"); input("shipment-destination", "Clínica Lima"); click("Seleccionar sensor"); waitText("E2E-S1"); click("Sensor E2E-S1"); click("Registrar despacho")
        waitTag("screen-shipment"); waitText("En preparación")
        click("Validar temperatura"); ui.onNodeWithText("Continuar con el despacho").performScrollTo().assertIsNotEnabled()
        runBlocking {
            assertFalse(monitoring.ingest("E2E-S1", "NaN"))
            assertTrue(monitoring.ingest("E2E-S1", "11", clock.now() - 1000))
            assertTrue(monitoring.ingest("E2E-S1", "11", clock.now()))
            assertEquals(1, alerts.incidents.all().size); assertEquals(2, alerts.incidents.all().single().repetitions)
        }
        waitText("Crítico"); click("Revisar y confirmar"); waitTag("screen-risk-confirm"); click("Confirmar continuación"); waitTag("screen-shipment"); waitText("En tránsito")
        capture("shipment-transit")
        runBlocking { assertEquals(3, inventory.products.find(productId)!!.reserved) }
        click("Consultar historial térmico"); waitTag("screen-history"); waitText("Mediciones rechazadas"); waitText("11.0 °C")
        logout(); login("supervisor"); waitTag("alert-notice"); ui.onNodeWithTag("alert-notice").performScrollTo().performClick(); waitTag("screen-incident")
        ui.onNodeWithText("Resolver alerta").performScrollTo().assertIsNotEnabled()
        input("incident-action", "Trasladar el producto a una cámara controlada."); click("Resolver alerta"); waitText("Acción correctiva registrada")
        capture("incident-resolved")
        runBlocking {
            val incident = alerts.incidents.all().single(); assertFalse(incident.active); assertEquals("supervisor.test1@gmail.com", incident.author)
            monitoring.ingest("E2E-S1", "5")
        }
        logout(); login("operador"); ui.onNodeWithText("Despachos").performClick(); waitText("Clínica Lima")
        ui.onNodeWithText("Vacuna de prueba · Clínica Lima").performScrollTo().performClick(); waitTag("screen-shipment")
        click("Confirmar entrega"); waitTag("screen-delivery"); capture("14-delivery-confirmation"); click("Confirmar entrega"); waitText("Entrega confirmada")
        runBlocking {
            val shipment = logistics.shipments.all().single(); assertEquals(ShipmentStatus.DELIVERED, shipment.status)
            assertEquals(7, inventory.products.find(productId)!!.stock); assertEquals(0, inventory.products.find(productId)!!.reserved)
            val timestamp = shipment.deliveredAt
            try { logistics.deliver(shipment.id); fail("Duplicate delivery") } catch (_: BusinessException) { }
            assertEquals(timestamp, logistics.shipments.find(shipment.id)!!.deliveredAt)
        }
        ui.onAllNodesWithText("Confirmar entrega").assertCountEquals(0)
    }
    @Test fun visitorRegistrationLockExpiryAndNondelegableAdmin() {
        click("Crear cuenta"); input("register-email", "nuevo.test1@gmail.com"); input("register-password", "@operadortest1")
        ui.onNodeWithTag("register-terms").performScrollTo().performClick(); click("Crear cuenta"); waitTag("screen-verify")
        runBlocking { assertNull(identity.sessions.current()); assertEquals(Role.OPERADOR, identity.identities.all().find { it.account.email == "nuevo.test1@gmail.com" }!!.account.role) }
        click("Volver a iniciar sesión")
        input("login-email", "operador.test1@gmail.com"); input("login-password", "wrong")
        repeat(5) { ui.onNodeWithTag("login-submit").performScrollTo().performClick(); ui.waitUntil(15000) { !runBlocking { identity.identities.find("test-operador-1")!!.failures < it + 1 } } }
        waitText("bloqueado temporalmente")
        input("login-password", "@operadortest1"); ui.onNodeWithTag("login-submit").performScrollTo().performClick(); waitText("bloqueado temporalmente")
        clock.value += 300_001; login("operador")
        clock.value += 1_800_001; waitTag("screen-login")
        runBlocking { assertNull(identity.sessions.current()) }
        login("admin")
        capture("admin-permissions")
        ui.onAllNodesWithText("Controlar permisos por rol").assertCountEquals(0)
        runBlocking {
            try { identity.update(Role.ADMIN, setOf(Permission.RESOLVE)); fail("ADMIN mutable") } catch (_: BusinessException) { }
            assertTrue(identity.roles.find(Role.ADMIN.name)!!.granted.isEmpty())
            assertEquals(Role.ADMIN, identity.requireAdmin().role)
        }
        ui.onNodeWithTag("permission-OPERADOR-WRITE_INVENTORY").performScrollTo().performClick()
        click("Guardar permisos"); waitText("Permisos guardados")
        logout(); login("operador"); ui.onAllNodesWithText("Registrar producto").assertCountEquals(0)
        runBlocking {
            try { inventory.create("Sin permiso", "Farmacéutico", "2", "8"); fail("Unauthorized create") } catch (_: BusinessException) { }
            try { identity.requireAdmin(); fail("Unauthorized admin") } catch (_: BusinessException) { }
            assertTrue(inventory.products.all().isEmpty())
        }
    }
    private suspend fun denied(block: suspend () -> Unit) {
        try { block(); fail("Expected business denial") } catch (_: BusinessException) { }
    }
    @Test fun validationAuthorizationAndReportContracts() = runBlocking {
        assertEquals(3, identity.identities.all().size)
        denied { inventory.create("No autorizado", "Alimenticio", "2", "8") }
        denied { identity.register("invalid", "@operadortest1", true) }
        denied { identity.register("usuario.test1@gmail.com", "short", true) }
        denied { identity.register("usuario.test1@gmail.com", "@operadortest1", false) }
        denied { identity.register("operador.test1@gmail.com", "@operadortest1", true) }
        identity.login("operador.test1@gmail.com", "@operadortest1")
        denied { identity.update(Role.SUPERVISOR, emptySet()) }
        denied { inventory.create("", "Alimenticio", "2", "8") }
        // Product type is optional metadata, absent from the Figma form.
        denied { inventory.create("Producto", "Alimenticio", "NaN", "8") }
        val product = inventory.create("Producto", "Alimenticio", "2", "8")
        denied { inventory.addLot(product, "") }; denied { inventory.addLot("missing", "L-1") }
        val lot = inventory.addLot(product, "L-1")
        denied { inventory.addLot(product, "l-1") }
        denied { inventory.income(product, "-1") }; denied { inventory.income("missing", "10") }
        inventory.income(product, "10")
        monitoring.sensors.save("E2E-C", Sensor("E2E-C", product, "Almacén"))
        denied { logistics.create(product, lot, "11", "Destino", "E2E-C") }
        try { logistics.create(product, lot, "11", "Destino", ""); fail("Overstock without sensor") }
        catch (error: BusinessException) { assertTrue(error.message!!.contains("stock disponible")) }
        assertEquals(0, inventory.products.find(product)!!.reserved)
        denied { logistics.create(product, lot, "0", "Destino", "E2E-C") }
        denied { logistics.create(product, lot, "1", "", "E2E-C") }
        val other = inventory.create("Otro", "Alimenticio", "2", "8")
        val otherLot = inventory.addLot(other, "L-2")
        denied { logistics.create(product, otherLot, "1", "Destino", "E2E-C") }
        denied { logistics.create("missing", lot, "1", "Destino", "E2E-C") }
        val shipment = logistics.create(product, lot, "3", "Destino", "E2E-C")
        assertEquals(7, inventory.products.find(product)!!.available)
        denied { logistics.depart(shipment, false) }; denied { logistics.deliver(shipment) }
        listOf("", "absent", "Infinity", "-Infinity", "NaN").forEach { assertFalse(monitoring.ingest("E2E-C", it)) }
        assertTrue(monitoring.readings.all().isEmpty()); assertEquals(5, monitoring.rejections.all().size)
        monitoring.ingest("E2E-C", "5"); logistics.depart(shipment, false)
        monitoring.ingest("E2E-C", "8"); assertEquals(ThermalCondition.WARNING, alerts.incidents.all().single().severity)
        identity.logout(); assertNull(identity.account())
        denied { logistics.deliver(shipment) }; assertEquals(ShipmentStatus.TRANSIT, logistics.shipments.find(shipment)!!.status)
        identity.login("supervisor.test1@gmail.com", "@supervisortest1")
        val restricted = reporting.summary(); assertEquals(10, restricted.units); assertEquals(1, restricted.activeIncidents)
        denied { logistics.deliver(shipment) }; denied { alerts.resolve(alerts.incidents.all().single().id, "  ") }
        identity.logout(); identity.login("admin.test1@gmail.com", "@admintest1")
        denied { identity.update(Role.SUPERVISOR, IdentityRules.defaults(Role.SUPERVISOR) + Permission.READ_INVENTORY) }
        denied { identity.update(Role.OPERADOR, IdentityRules.defaults(Role.OPERADOR) + Permission.RESOLVE) }
        identity.update(Role.SUPERVISOR, setOf(Permission.SUMMARY))
        identity.logout(); identity.login("supervisor.test1@gmail.com", "@supervisortest1")
        assertEquals(10, reporting.summary().units); assertEquals(2, reporting.summary().products)
        assertEquals(1, reporting.summary().inTransit)
        val delegated = workspace().first()
        assertTrue(delegated.products.isEmpty()); assertTrue(delegated.shipments.isEmpty()); assertTrue(delegated.incidents.isEmpty())
        denied { logistics.deliver(shipment) }; denied { inventory.income(product, "10") }
        identity.logout(); identity.login("admin.test1@gmail.com", "@admintest1")
        // Existing local permissions are normalized without resetting operational records.
        identity.roles.save(Role.SUPERVISOR.name, RolePermissions(Role.SUPERVISOR, setOf(Permission.SUMMARY, Permission.DELIVER)))
        identity.initialize()
        assertEquals(setOf(Permission.SUMMARY), identity.roles.find(Role.SUPERVISOR.name)!!.granted)
        assertEquals(10, inventory.products.find(product)!!.stock)

    }
    @Test fun inventoryRiskAndHistoryFilters() {
        login("operador"); val product = createProduct()
        runBlocking {
            monitoring.sensors.save("E2E-H", Sensor("E2E-H", product, "Almacén"))
            monitoring.ingest("E2E-H", "1", clock.now() - 31L * 86_400_000)
            monitoring.ingest("E2E-H", "5")
        }
        click("Consultar historial térmico"); waitText("5.0 °C"); click("Periodo"); click("Consultar periodo"); waitText("1.0 °C"); click("Hoy")
        ui.onAllNodesWithText("1.0 °C").assertCountEquals(0); waitText("5.0 °C")
        ui.onNodeWithText("Inventario").performClick(); click("Normal"); waitText("Vacuna de prueba")
        click("En riesgo"); waitText("No hay productos que coincidan")
        logout(); login("supervisor"); ui.onAllNodesWithText("Unidades en stock").assertCountEquals(0)
        ui.onNodeWithText("Riesgos").performClick(); waitText("Sin productos en riesgo")
        click("Consultar historial térmico"); click("Vacuna de prueba · Normal"); click("Periodo"); click("Consultar periodo"); waitText("1.0 °C")
        waitText("Historial de anomalías")
    }
    @Test fun screenByScreenFidelityAndMinimumValidations() {
        clock.value = java.util.GregorianCalendar(java.util.TimeZone.getTimeZone("America/Lima")).apply { clear(); set(2026, 9, 8, 10, 30) }.timeInMillis
        capture("01-login")
        click("Crear cuenta"); capture("02-register")
        input("register-email", "invalid"); input("register-password", "short")
        ui.onNodeWithTag("register-submit").assertIsNotEnabled(); capture("02-register-invalid")
        ui.onNodeWithText("Ya tengo cuenta · Iniciar sesión").performScrollTo().performClick()
        click("Recuperar acceso"); capture("01-recovery")
        input("recovery-email", "operador.test1@gmail.com"); click("Solicitar recuperación")
        waitText("tu contraseña no ha cambiado"); capture("01-recovery-request")
        click("Volver a iniciar sesión"); login("operador")
        capture("04-inventory-empty")
        ui.onNodeWithContentDescription("Cuenta").performClick(); waitText("Tu sesión"); capture("04-account-operator")
        click("Volver"); waitTag("screen-inventory")
        click("Registrar producto"); capture("05-product-form")
        input("product-name", "Vacuna A"); input("product-min", "8"); input("product-max", "2")
        ui.onNodeWithTag("product-submit").assertIsNotEnabled(); capture("05-product-inconsistent-range")
        input("product-min", "2"); input("product-max", "8"); capture("05-product-complete")
        click("Registrar producto"); waitTag("screen-product"); waitText("Vacuna A"); capture("06-product-no-sensor")
        click("Registrar lote"); capture("07-lot-form"); input("lot-code", "L-001"); capture("07-lot-complete")
        click("Registrar lote"); waitText("Lote registrado"); capture("07-lot-success")
        runBlocking { assertEquals(0, inventory.products.all().single().stock) }
        click("Registrar ingreso"); capture("08-income-form")
        listOf("0", "-1", "1.5", "2147483648").forEach { invalid -> input("income-quantity", invalid); ui.onNodeWithTag("income-submit").assertIsNotEnabled() }
        capture("08-income-invalid"); input("income-quantity", "120"); click("Registrar ingreso"); waitText("120 unidades"); capture("07-lot-stock")
        click("Volver al producto"); waitTag("screen-product")
        click("Registrar lote"); input("lot-code", "l-001"); waitText("ya existe"); capture("07-lot-duplicate")
        click("Consultar lote existente"); waitTag("screen-lot"); capture("07-lot-detail"); click("Volver al producto")
        val product = runBlocking { inventory.products.all().single().id }
        val clockBefore = clock.now()
        runBlocking {
            monitoring.sensors.save("S-001", Sensor("S-001", product, "Cámara 1"))
            monitoring.ingest("S-001", "5.4", clockBefore - 1_800_000)
            monitoring.ingest("S-001", "5.1", clockBefore)
        }
        waitText("5.1 °C"); capture("06-product-current")
        click("Consultar historial térmico"); waitText("5.1 °C"); capture("09-history")
        click("Periodo"); capture("09-period-picker")
        input("history-from", "10/10/2026 10:00"); input("history-to", "09/10/2026 10:00")
        ui.onNodeWithText("Consultar periodo").performScrollTo().assertIsNotEnabled(); capture("09-period-invalid")
        input("history-from", com.safeflow.app.features.monitoring.presentation.periodLabel(clockBefore - 1_800_000))
        input("history-to", com.safeflow.app.features.monitoring.presentation.periodLabel(clockBefore)); click("Consultar periodo"); waitText("5.4 °C"); capture("09-period-applied")
        ui.onNodeWithText("Despachos").performClick(); capture("13-shipments-empty"); click("Registrar despacho"); capture("17-dispatch-form")
        click("Seleccionar producto"); capture("17-select-product"); click("Vacuna A")
        click("Seleccionar lote"); capture("17-select-lot"); click("Lote L-001")
        input("shipment-destination", "Centro Norte")
        listOf("0", "-1", "1.5", "121").forEach { invalid -> input("shipment-quantity", invalid); ui.onNodeWithTag("shipment-submit").performScrollTo().assertIsNotEnabled() }
        waitText("supera el stock disponible"); capture("17-dispatch-over-stock")
        input("shipment-quantity", "120"); click("Seleccionar sensor"); capture("17-select-sensor"); click("Sensor S-001")
        ui.onNodeWithTag("shipment-destination").assertTextEquals("Centro Norte"); ui.onNodeWithTag("shipment-quantity").assertTextEquals("120")
        ui.onNodeWithTag("shipment-submit").performScrollTo().assertIsEnabled(); capture("17-dispatch-complete")
        click("Registrar despacho"); waitTag("screen-shipment"); waitText("En preparación"); capture("14-shipment-preparation")
        runBlocking {
            assertEquals(0, inventory.products.find(product)!!.available)
            monitoring.ingest("S-001", "5.1")
        }
        click("Validar temperatura"); waitText("Condición térmica dentro del rango"); capture("15-validation-normal")
        runBlocking { clock.value += 1_000; monitoring.ingest("S-001", "11") }
        waitText("Crítico"); capture("15-validation-risk")
        click("Revisar y confirmar"); waitTag("screen-risk-confirm"); capture("15-risk-confirmation"); click("Cancelar"); waitTag("screen-validation"); runBlocking { assertEquals(ShipmentStatus.PREPARATION, logistics.shipments.all().single().status) }; click("Revisar y confirmar"); click("Confirmar continuación"); waitText("En tránsito"); capture("14-shipment-transit-chart")
        logout(); login("supervisor"); waitText("Situación operativa"); capture("03-summary")
        ui.onNodeWithContentDescription("Cuenta").performClick(); capture("03-account-supervisor"); click("Volver"); waitTag("screen-summary")
        ui.onNodeWithText("Riesgos").performClick(); waitText("11.0 °C"); capture("10-risks-critical")
        runBlocking { clock.value += 1_000; monitoring.ingest("S-001", "7.8") }; waitText("7.8 °C"); capture("10-risks-warning")
        ui.onNodeWithText("Alertas").performClick(); waitText("Incidencias pendientes"); capture("11-alerts-active")
        val incidentId = runBlocking { alerts.incidents.all().single().id }
        ui.onNode(hasText(incidentId.take(8), substring = true) and hasClickAction()).performScrollTo().performClick()
        waitTag("screen-incident"); capture("12-incident")
        input("incident-action", " "); ui.onNodeWithText("Resolver alerta").performScrollTo().assertIsNotEnabled(); capture("12-incident-action-required")
        input("incident-action", "Restablecer la temperatura de la cámara."); click("Resolver alerta"); waitText("Acción correctiva registrada"); capture("12-incident-resolved")
        ui.onNodeWithText("Alertas").performClick(); click("Historial"); waitText("Historial de incidencias"); capture("11-alerts-history")
        logout(); login("admin"); capture("16-permissions-operator")
        ui.onNodeWithTag("permission-OPERADOR-WRITE_INVENTORY").performScrollTo().performClick(); capture("16-permissions-pending")
        click("SUPERVISOR"); ui.onAllNodesWithTag("permission-SUPERVISOR-DELIVER").assertCountEquals(0); capture("16-permissions-supervisor")
        click("OPERADOR"); ui.onNodeWithTag("permission-OPERADOR-WRITE_INVENTORY").assertIsOff()
        click("Descartar cambios"); ui.onNodeWithTag("permission-OPERADOR-WRITE_INVENTORY").assertIsOn()
        ui.onNodeWithTag("permission-OPERADOR-READ_INVENTORY").performScrollTo().performClick(); click("Guardar permisos"); waitText("Permisos guardados"); capture("16-permissions-saved")
        ui.onNodeWithContentDescription("Cuenta").performClick(); capture("16-account-admin"); click("Volver"); waitTag("screen-permissions")
        logout(); login("operador"); ui.onNodeWithText("Despachos").performClick(); waitText("Centro Norte")
        ui.onNode(hasText("Centro Norte", substring = true) and hasClickAction()).performScrollTo().performClick()
        waitTag("screen-shipment"); click("Confirmar entrega"); waitTag("screen-delivery"); capture("14-delivery-confirmation"); click("Cancelar"); waitTag("screen-shipment"); runBlocking { assertEquals(ShipmentStatus.TRANSIT, logistics.shipments.all().single().status) }; click("Confirmar entrega"); click("Confirmar entrega"); waitText("Entrega confirmada"); capture("14-shipment-delivered")
        runBlocking { assertEquals(0, inventory.products.find(product)!!.stock); assertEquals(0, inventory.products.find(product)!!.reserved) }
    }
    @Test fun transactionsAnomalyRecoveryAndReadingsAssociation() = runBlocking {
        identity.login("operador.test1@gmail.com", "@operadortest1")
        val product = inventory.create("Producto transaccional", "Alimenticio", "2", "8")
        val lot = inventory.addLot(product, "L-1"); inventory.income(product, "10")
        monitoring.sensors.save("E2E-A", Sensor("E2E-A", product, "Almacén"))
        monitoring.sensors.save("E2E-B", Sensor("E2E-B", product, "Almacén"))
        val outcomes = coroutineScope {
            listOf("E2E-A", "E2E-B").map { sensor -> async(Dispatchers.IO) {
                try { logistics.create(product, lot, "7", "Destino", sensor); true } catch (_: BusinessException) { false }
            } }.awaitAll()
        }
        assertEquals(1, outcomes.count { it }); assertEquals(7, inventory.products.find(product)!!.reserved)
        val shipment = logistics.shipments.all().single()
        monitoring.ingest(shipment.sensorId, "1"); monitoring.ingest(shipment.sensorId, "5")
        assertTrue(alerts.incidents.all().single().active) // recovery does not resolve an incident
        assertEquals(shipment.id, monitoring.readings.all().first().shipmentId)
        monitoring.disconnect(shipment.sensorId); assertEquals(2, alerts.incidents.all().size)
        try { logistics.depart(shipment.id, true); fail("Disconnected departure") } catch (_: BusinessException) { }
        assertEquals(ShipmentStatus.PREPARATION, logistics.shipments.find(shipment.id)!!.status)
        monitoring.ingest(shipment.sensorId, "5")
        clock.value += 300_001
        try { logistics.depart(shipment.id, true); fail("Stale departure") } catch (_: BusinessException) { }
        // An intentional failure proves Room rolls the reservation back across namespaces.
        val before = inventory.products.find(product)!!.stock
        try { com.safeflow.app.core.infrastructure.local.RoomTransaction(storage.db).run {
            inventory.products.save(product, inventory.products.find(product)!!.copy(stock = 999))
            throw BusinessException("rollback")
        } } catch (_: BusinessException) { }
        assertEquals(before, inventory.products.find(product)!!.stock)
    }
}
