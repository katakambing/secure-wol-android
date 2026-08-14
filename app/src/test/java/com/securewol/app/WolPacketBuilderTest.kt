package com.securewol.app

import com.securewol.app.core.network.WolPacketBuilder
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WolPacketBuilderTest {

    @Test
    fun testParseMacAddress_colonFormatted() {
        val macBytes = WolPacketBuilder.parseMacAddress("00:11:22:33:44:55")
        val expected = byteArrayOf(0x00, 0x11, 0x22, 0x33, 0x44, 0x55)
        assertArrayEquals(expected, macBytes)
    }

    @Test
    fun testParseMacAddress_dashFormatted() {
        val macBytes = WolPacketBuilder.parseMacAddress("AA-BB-CC-DD-EE-FF")
        val expected = byteArrayOf(
            0xAA.toByte(), 0xBB.toByte(), 0xCC.toByte(),
            0xDD.toByte(), 0xEE.toByte(), 0xFF.toByte()
        )
        assertArrayEquals(expected, macBytes)
    }

    @Test
    fun testParseMacAddress_rawHex() {
        val macBytes = WolPacketBuilder.parseMacAddress("0123456789ab")
        val expected = byteArrayOf(0x01, 0x23, 0x45, 0x67, 0x89.toByte(), 0xab.toByte())
        assertArrayEquals(expected, macBytes)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testParseMacAddress_invalidLength() {
        WolPacketBuilder.parseMacAddress("00:11:22:33")
    }

    @Test(expected = IllegalArgumentException::class)
    fun testParseMacAddress_invalidChars() {
        WolPacketBuilder.parseMacAddress("00:11:22:33:44:GG")
    }

    @Test
    fun testBuildMagicPacket_standardPayload() {
        val mac = "00:11:22:33:44:55"
        val packet = WolPacketBuilder.buildMagicPacket(mac)

        // 1. Total length must be exactly 102 bytes
        assertEquals(102, packet.size)

        // 2. First 6 bytes must be 0xFF
        for (i in 0 until 6) {
            assertEquals(0xFF.toByte(), packet[i])
        }

        // 3. Followed by 16 repetitions of 6-byte MAC
        val macBytes = byteArrayOf(0x00, 0x11, 0x22, 0x33, 0x44, 0x55)
        for (rep in 0 until 16) {
            val offset = 6 + (rep * 6)
            for (byteIndex in 0 until 6) {
                assertEquals(macBytes[byteIndex], packet[offset + byteIndex])
            }
        }
    }

    @Test
    fun testBuildMagicPacket_withSecureOnPassword() {
        val mac = "00:11:22:33:44:55"
        val passwordHex = "AA:BB:CC:DD:EE:FF"
        val packet = WolPacketBuilder.buildMagicPacket(mac, passwordHex)

        // Total length must be 108 bytes (102 + 6)
        assertEquals(108, packet.size)

        // Last 6 bytes must match password
        val expectedPwd = byteArrayOf(
            0xAA.toByte(), 0xBB.toByte(), 0xCC.toByte(),
            0xDD.toByte(), 0xEE.toByte(), 0xFF.toByte()
        )
        for (i in 0 until 6) {
            assertEquals(expectedPwd[i], packet[102 + i])
        }
    }

    @Test
    fun testIsValidMac() {
        assertTrue(WolPacketBuilder.isValidMac("00:11:22:33:44:55"))
        assertTrue(WolPacketBuilder.isValidMac("aa-bb-cc-dd-ee-ff"))
        assertFalse(WolPacketBuilder.isValidMac("invalid-mac"))
        assertFalse(WolPacketBuilder.isValidMac(""))
    }
}
