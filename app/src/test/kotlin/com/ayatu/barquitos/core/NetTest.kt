package com.ayatu.barquitos.core

import com.ayatu.barquitos.net.Hello
import com.ayatu.barquitos.net.NetCodec
import com.ayatu.barquitos.net.NetMessage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/**
 * Pruebas del WiFi local que se pueden hacer sin dos móviles: el protocolo de texto y, sobre
 * todo, que los dos móviles acaban de acuerdo en **quién empieza**.
 */
class NetTest {

    @Test
    fun `el anfitrion sortea quien empieza antes de publicar la partida`() {
        val alAzar = GameConfig(mode = GameMode.WIFI_LOCAL, startingPlayer = null)
        repeat(20) { seed ->
            val acordado = alAzar.resolvedForHosting(Random(seed))
            assertNotNull(
                "Por WiFi no puede salir a la red un «quién empieza» sin decidir",
                acordado.startingPlayer,
            )
            assertTrue(acordado.startingPlayer in 0..1)
        }
    }

    @Test
    fun `si ya se eligio quien empieza el anfitrion no lo toca`() {
        for (elegido in 0..1) {
            val config = GameConfig(mode = GameMode.WIFI_LOCAL, startingPlayer = elegido)
            assertEquals(elegido, config.resolvedForHosting(Random(1)).startingPlayer)
        }
    }

    @Test
    fun `el saludo lleva las reglas y quien empieza de ida y vuelta`() {
        val config = GameConfig(
            mode = GameMode.WIFI_LOCAL,
            shotRule = ShotRule.UN_DISPARO,
            startingPlayer = 1,
        )
        val linea = NetCodec.encodeHello(Hello("Javier", config))
        val vuelta = NetCodec.decodeHello(linea)
        assertNotNull(vuelta)
        assertEquals("Javier", vuelta!!.name)
        assertEquals(ShotRule.UN_DISPARO, vuelta.config.shotRule)
        assertEquals(1, vuelta.config.startingPlayer)
        assertEquals(GameMode.WIFI_LOCAL, vuelta.config.mode)
    }

    @Test
    fun `los dos moviles montan la misma partida a partir del saludo`() {
        // Lo que de verdad fallaba: el anfitrión sortea, manda, y los dos tienen que coincidir.
        val elegidoPorJavier = GameConfig(mode = GameMode.WIFI_LOCAL, startingPlayer = null)
        val delAnfitrion = elegidoPorJavier.resolvedForHosting(Random(7))
        val delInvitado = NetCodec.decodeHello(
            NetCodec.encodeHello(Hello("Anfitrión", delAnfitrion)),
        )!!.config
        assertEquals(
            "Los dos móviles tienen que creer lo mismo sobre quién abre fuego",
            delAnfitrion.startingPlayer,
            delInvitado.startingPlayer,
        )
        assertEquals(delAnfitrion.shotRule, delInvitado.shotRule)
    }

    @Test
    fun `un saludo roto no revienta`() {
        assertNull(NetCodec.decodeHello("cualquier cosa"))
        assertNull(NetCodec.decodeHello(""))
        assertNull(NetCodec.decodeHello("HELLO|solo|dos"))
    }

    @Test
    fun `los disparos y la flota viajan de ida y vuelta`() {
        val disparo = NetCodec.decode(NetCodec.encode(NetMessage.Shot(Coord(4, 7))))
        assertEquals(NetMessage.Shot(Coord(4, 7)), disparo)

        val flota = Placement.randomLayout(Random(3))
        val vuelta = NetCodec.decode(NetCodec.encode(NetMessage.Layout(flota)))
        assertTrue(vuelta is NetMessage.Layout)
        val recibida = (vuelta as NetMessage.Layout).ships
        assertEquals(flota.size, recibida.size)
        assertEquals(
            flota.map { Triple(it.id, it.anchor, it.orientation) },
            recibida.map { Triple(it.id, it.anchor, it.orientation) },
        )
    }

    @Test
    fun `rendirse e irse viajan de ida y vuelta`() {
        assertEquals(NetMessage.Surrender, NetCodec.decode(NetCodec.encode(NetMessage.Surrender)))
        assertEquals(NetMessage.Bye, NetCodec.decode(NetCodec.encode(NetMessage.Bye)))
    }
}
