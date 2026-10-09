package com.safeflow.app

import com.safeflow.app.core.domain.*
import com.safeflow.app.features.iam.domain.*
import com.safeflow.app.features.monitoring.domain.*
import org.junit.Assert.*
import org.junit.Test

class BusinessRulesTest {
    @Test fun temperatureBoundariesAndSeverity() {
        val cases = listOf(2.0 to ThermalCondition.WARNING, 8.0 to ThermalCondition.WARNING,
            5.0 to ThermalCondition.NORMAL, 1.9 to ThermalCondition.RISK, 8.1 to ThermalCondition.RISK,
            0.0 to ThermalCondition.RISK, 10.0 to ThermalCondition.RISK,
            -.01 to ThermalCondition.CRITICAL, 10.01 to ThermalCondition.CRITICAL)
        cases.forEach { (value, condition) -> assertEquals("Temperature $value", condition, ThermalRules.evaluate(value, 2.0, 8.0)) }
    }
    @Test fun zeroWidthHasNoWarningBand() { assertEquals(ThermalCondition.NORMAL, ThermalRules.evaluate(5.0, 5.0, 5.0)) }
    @Test fun invalidTemperatureRejected() {
        listOf(Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY).forEach {
            assertThrows(IllegalArgumentException::class.java) { ThermalRules.evaluate(it, 2.0, 8.0) }
        }
        assertThrows(IllegalArgumentException::class.java) { ThermalRules.evaluate(5.0, 8.0, 2.0) }
    }
    @Test fun requestedPasswordsDoNotRequireUppercase() {
        listOf("@operadortest1", "@supervisortest1", "@admintest1").forEach { assertTrue(IdentityRules.validPassword(it)) }
        listOf("abc", "password123", "@abcdefghi", "12345678@").forEach { assertFalse(IdentityRules.validPassword(it)) }
    }
    @Test fun validatesEmail() {
        assertTrue(IdentityRules.validEmail("operador.test1@gmail.com"))
        listOf("bad", "a@", "a b@gmail.com", "a@gmail").forEach { assertFalse(IdentityRules.validEmail(it)) }
    }
    @Test fun quantitiesAreFinitePositiveIntegers() {
        assertEquals(10, positiveQuantity("10"))
        listOf("0", "-1", "1.5", "NaN", "Infinity", "2147483648", "").forEach {
            assertThrows(BusinessException::class.java) { positiveQuantity(it) }
        }
    }
    @Test fun administrationIsNotAnAssignablePermission() {
        assertEquals(emptySet<Permission>(), IdentityRules.defaults(Role.ADMIN))
        assertTrue(Permission.WRITE_INVENTORY in IdentityRules.defaults(Role.OPERADOR))
        assertTrue(Permission.RESOLVE in IdentityRules.defaults(Role.SUPERVISOR))
        assertFalse(Permission.RESOLVE in IdentityRules.defaults(Role.OPERADOR))
    }
}
