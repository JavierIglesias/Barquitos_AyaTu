package com.ayatu.barquitos.core

/**
 * Guardado y recuperación de una partida a medias, en texto plano y sin dependencias (ni JSON ni
 * Android), para que se pueda probar con pruebas unitarias y para que el fichero se pueda leer de
 * un vistazo si hay que depurar algo.
 *
 * Una partida contra la máquina dura varios minutos, y Android puede matar la app en cualquier
 * momento mientras está en segundo plano; con esto, al volver se recupera exactamente donde
 * estaba.
 *
 * Formato: una línea por clave, `clave=valor`. Si cambia el formato se sube [VERSION] y los
 * guardados viejos se descartan en silencio (una partida perdida no es un drama; un fallo al
 * arrancar sí).
 */
object GameSave {

    const val VERSION = 2

    private const val NO_WINNER = -99

    fun serialize(game: Game): String = buildString {
        val (fired, hits) = game.snapshotCounters()
        line("v", VERSION)
        line("mode", game.config.mode.name)
        line("rule", game.config.shotRule.name)
        line("diff", game.config.difficulty.name)
        line("start", game.config.startingPlayer ?: -1)
        line("current", game.current)
        line("round", game.round)
        line("winner", game.winner ?: NO_WINNER)
        line("fired", fired.joinToString(","))
        line("hits", hits.joinToString(","))
        for (p in 0..1) {
            line(
                "ships$p",
                game.boards[p].ships.joinToString(";") { s ->
                    "${s.id}:${s.anchor.x},${s.anchor.y}:${s.orientation.name}"
                },
            )
            line("shots$p", game.boards[p].shots.joinToString(";") { "${it.x},${it.y}" })
        }
        line(
            "hist",
            game.history.joinToString(";") { r ->
                val kind = r.sunkKind?.name ?: ""
                "${r.round}:${r.player}:${r.coord.x},${r.coord.y}:${if (r.hit) 1 else 0}:$kind"
            },
        )
    }

    /** Devuelve la partida guardada, o null si el texto no vale (versión vieja, truncado...). */
    fun deserialize(text: String): Game? = runCatching {
        val map = text.lineSequence()
            .filter { it.contains('=') }
            .associate { it.substringBefore('=') to it.substringAfter('=') }
        if (map["v"]?.toIntOrNull() != VERSION) return@runCatching null

        val config = GameConfig(
            mode = GameMode.valueOf(map.getValue("mode")),
            shotRule = ShotRule.valueOf(map.getValue("rule")),
            difficulty = Difficulty.valueOf(map.getValue("diff")),
            startingPlayer = map.getValue("start").toInt().takeIf { it >= 0 },
        )
        val boards = (0..1).map { p ->
            val ships = map.getValue("ships$p").split(";").filter { it.isNotBlank() }.map { spec ->
                val parts = spec.split(":")
                val def = Fleet.shipById(parts[0].toInt()) ?: error("Barco ${parts[0]} desconocido")
                val anchor = parts[1].split(",").map(String::toInt)
                PlacedShip(def, Coord(anchor[0], anchor[1]), Orientation.valueOf(parts[2]))
            }
            require(ships.size == Fleet.shipCount) { "Flota incompleta en el guardado" }
            PlayerBoard(Fleet.BOARD_SIZE, ships).apply { restoreShots(parseCoords(map["shots$p"])) }
        }
        val game = Game(config, boards, startingPlayer = config.startingPlayer ?: 0)
        val fired = map.getValue("fired").split(",").map(String::toInt)
        val hits = map.getValue("hits").split(",").map(String::toInt)
        val winnerRaw = map.getValue("winner").toInt()
        game.restore(
            current = map.getValue("current").toInt(),
            round = map.getValue("round").toInt(),
            winner = winnerRaw.takeIf { it != NO_WINNER },
            history = parseHistory(map["hist"]),
            shotsFired = intArrayOf(fired[0], fired[1]),
            hitsScored = intArrayOf(hits[0], hits[1]),
        )
        game
    }.getOrNull()

    private fun parseCoords(spec: String?): List<Coord> =
        spec?.split(";")?.filter { it.isNotBlank() }?.map {
            val parts = it.split(",").map(String::toInt)
            Coord(parts[0], parts[1])
        } ?: emptyList()

    private fun parseHistory(spec: String?): List<ShotRecord> =
        spec?.split(";")?.filter { it.isNotBlank() }?.map {
            val parts = it.split(":")
            val xy = parts[2].split(",").map(String::toInt)
            ShotRecord(
                round = parts[0].toInt(),
                player = parts[1].toInt(),
                coord = Coord(xy[0], xy[1]),
                hit = parts[3] == "1",
                sunkKind = parts.getOrNull(4)?.takeIf { k -> k.isNotBlank() }?.let(ShipKind::valueOf),
            )
        } ?: emptyList()

    private fun StringBuilder.line(key: String, value: Any) {
        append(key).append('=').append(value).append('\n')
    }
}
