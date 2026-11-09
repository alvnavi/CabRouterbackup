package com.example

import com.example.location.LocationHelper
import com.example.navigation.WazeLauncher
import org.junit.Assert.assertEquals
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun wazeDestinationLabel_formatsCorrectlyWithFare() {
        val label = WazeLauncher.formatDestinationLabel("terminal 1", 50.0)
        assertEquals("terminal 1 - $50", label)
    }

    @Test
    fun wazeDestinationLabel_formatsWithDecimalFare() {
        val label = WazeLauncher.formatDestinationLabel("aeropuerto", 75.50)
        assertEquals("aeropuerto - $75.50", label)
    }

    @Test
    fun wazeDestinationLabel_formatsWithoutFareWhenZero() {
        val label = WazeLauncher.formatDestinationLabel("centro historico", 0.0)
        assertEquals("centro historico", label)
    }

    @Test
    fun wazeDestinationLabel_formatsWithoutFareWhenNegative() {
        val label = WazeLauncher.formatDestinationLabel("plaza central", -10.0)
        assertEquals("plaza central", label)
    }

    @Test
    fun wazeDestinationLabel_handlesBlankName() {
        val label = WazeLauncher.formatDestinationLabel("   ", 30.0)
        assertEquals("Destino - $30", label)
    }

    @Test
    fun coordinateFormatting_positiveCoordinates() {
        val formatted = LocationHelper.formatCoordinates(19.4326, 99.1332)
        assertEquals("19.43260°N, 99.13320°E", formatted)
    }

    @Test
    fun coordinateFormatting_negativeCoordinates() {
        val formatted = LocationHelper.formatCoordinates(-34.6037, -58.3816)
        assertEquals("34.60370°S, 58.38160°O", formatted)
    }
}


