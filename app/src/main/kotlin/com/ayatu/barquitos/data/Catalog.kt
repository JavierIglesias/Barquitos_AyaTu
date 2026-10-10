package com.ayatu.barquitos.data

import androidx.compose.ui.graphics.Color

/**
 * Catálogo de todo lo que se puede desbloquear y elegir en el Astillero: armadas, pinturas,
 * emblemas, planos de buques históricos y medallas.
 *
 * Nada de esto es un recurso binario: las banderas, los emblemas y los barcos se **dibujan**
 * con Canvas a partir de estos datos, así que la app no lleva ni una imagen de barco y pesa lo
 * que pesa el código. Ver `ui/common/ShipPainter.kt`, `FlagPainter.kt` y `EmblemPainter.kt`.
 */

// --------------------------------------------------------------------------- armadas

enum class FlagOverlay { NINGUNO, CIRCULO, MEDIA_LUNA, UNION, ROMBO, CANTON, ASPA }

/** Descripción geométrica simplificada de una bandera, suficiente para reconocerla de un vistazo. */
data class FlagDesign(
    val stripes: List<Color>,
    val vertical: Boolean = false,
    val overlay: FlagOverlay = FlagOverlay.NINGUNO,
    val overlayColor: Color = Color.White,
    val overlayColor2: Color = Color.White,
)

/** Una armada: su bandera y los colores con los que pinta sus barcos de serie. */
data class Nation(
    val id: String,
    val label: String,
    val flag: FlagDesign,
    val hull: Color,
    val deck: Color,
    val accent: Color,
)

object Nations {

    private val gris = Color(0xFF8D99A6)
    private val grisAzul = Color(0xFF6E8294)
    private val cubierta = Color(0xFFBCC7D1)

    val all: List<Nation> = listOf(
        Nation(
            "neutral", "Neutral",
            FlagDesign(listOf(Color(0xFF9AA7B4), Color(0xFFD7E6F2), Color(0xFF9AA7B4))),
            gris, cubierta, Color(0xFFF2B33D),
        ),
        Nation(
            "espana", "España",
            FlagDesign(
                listOf(Color(0xFFC60B1E), Color(0xFFFFC400), Color(0xFFC60B1E)),
                overlay = FlagOverlay.ASPA, overlayColor = Color(0x00000000),
            ),
            Color(0xFF8A94A0), cubierta, Color(0xFFC60B1E),
        ),
        Nation(
            "usa", "Estados Unidos",
            FlagDesign(
                listOf(
                    Color(0xFFB22234), Color.White, Color(0xFFB22234), Color.White,
                    Color(0xFFB22234), Color.White, Color(0xFFB22234),
                ),
                overlay = FlagOverlay.CANTON,
                overlayColor = Color(0xFF3C3B6E), overlayColor2 = Color.White,
            ),
            Color(0xFF7C8794), cubierta, Color(0xFF3C3B6E),
        ),
        Nation(
            "alemania", "Alemania",
            FlagDesign(listOf(Color(0xFF111111), Color(0xFFDD0000), Color(0xFFFFCE00))),
            Color(0xFF9EA7AF), Color(0xFFCBD3DA), Color(0xFFDD0000),
        ),
        Nation(
            "francia", "Francia",
            FlagDesign(listOf(Color(0xFF002395), Color.White, Color(0xFFED2939)), vertical = true),
            Color(0xFF7F8B99), cubierta, Color(0xFF002395),
        ),
        Nation(
            "reinounido", "Reino Unido",
            FlagDesign(
                listOf(Color(0xFF012169)),
                overlay = FlagOverlay.UNION,
                overlayColor = Color.White, overlayColor2 = Color(0xFFC8102E),
            ),
            Color(0xFF69757F), cubierta, Color(0xFFC8102E),
        ),
        Nation(
            "mexico", "México",
            FlagDesign(
                listOf(Color(0xFF006847), Color.White, Color(0xFFCE1126)),
                vertical = true,
                overlay = FlagOverlay.CIRCULO, overlayColor = Color(0xFF8A5A2B),
            ),
            Color(0xFF808C96), cubierta, Color(0xFF006847),
        ),
        Nation(
            "turquia", "Turquía",
            FlagDesign(
                listOf(Color(0xFFE30A17)),
                overlay = FlagOverlay.MEDIA_LUNA, overlayColor = Color.White,
            ),
            Color(0xFF75808A), cubierta, Color(0xFFE30A17),
        ),
        Nation(
            "italia", "Italia",
            FlagDesign(listOf(Color(0xFF008C45), Color.White, Color(0xFFCD212A)), vertical = true),
            Color(0xFF93A0AA), cubierta, Color(0xFF008C45),
        ),
        Nation(
            "polonia", "Polonia",
            FlagDesign(listOf(Color.White, Color(0xFFDC143C))),
            Color(0xFF8893A0), cubierta, Color(0xFFDC143C),
        ),
        Nation(
            "rusia", "Rusia",
            FlagDesign(listOf(Color.White, Color(0xFF0039A6), Color(0xFFD52B1E))),
            grisAzul, cubierta, Color(0xFF0039A6),
        ),
        Nation(
            "japon", "Japón",
            FlagDesign(
                listOf(Color.White),
                overlay = FlagOverlay.CIRCULO, overlayColor = Color(0xFFBC002D),
            ),
            Color(0xFF7D8894), cubierta, Color(0xFFBC002D),
        ),
        Nation(
            "india", "India",
            FlagDesign(
                listOf(Color(0xFFFF9933), Color.White, Color(0xFF138808)),
                overlay = FlagOverlay.CIRCULO, overlayColor = Color(0xFF000080),
            ),
            Color(0xFF8C97A1), cubierta, Color(0xFFFF9933),
        ),
        Nation(
            "brasil", "Brasil",
            FlagDesign(
                listOf(Color(0xFF009C3B)),
                overlay = FlagOverlay.ROMBO,
                overlayColor = Color(0xFFFFDF00), overlayColor2 = Color(0xFF002776),
            ),
            Color(0xFF7E8A94), cubierta, Color(0xFF009C3B),
        ),
    )

    fun byId(id: String): Nation = all.firstOrNull { it.id == id } ?: all.first()
}

// --------------------------------------------------------------------------- pinturas

/** Cómo se pinta el casco. */
enum class SkinPattern(val label: String) {
    LISO("Liso"),
    CAMUFLAJE("Camuflaje"),
    DAZZLE("Rayas de guerra"),
    DESGASTADO("Desgastado"),
    ANIVERSARIO("Aniversario"),
}

/** Una pintura concreta: patrón, colores y cuántos barcos hay que hundir para tenerla. */
data class Skin(
    val id: String,
    val label: String,
    val pattern: SkinPattern,
    val hull: Color,
    val deck: Color,
    val accent: Color,
    /** Barcos hundidos en total que hacen falta para desbloquearla. */
    val unlockAt: Int,
)

object Skins {

    /** Las bases de color de las que salen todas las pinturas. */
    private val bases: List<Triple<String, Color, Color>> = listOf(
        Triple("Gris armada", Color(0xFF8D99A6), Color(0xFFBCC7D1)),
        Triple("Gris niebla", Color(0xFFA9B4BE), Color(0xFFD5DDE4)),
        Triple("Azul océano", Color(0xFF4E6E8C), Color(0xFF8FA9BF)),
        Triple("Azul noche", Color(0xFF2E4057), Color(0xFF61788F)),
        Triple("Verde abisal", Color(0xFF3E5E53), Color(0xFF7D9A8E)),
        Triple("Verde oliva", Color(0xFF5E6B45), Color(0xFF95A177)),
        Triple("Arena", Color(0xFFBBA37A), Color(0xFFE0D2B6)),
        Triple("Óxido", Color(0xFF8A5A3C), Color(0xFFBC8C6C)),
        Triple("Carbón", Color(0xFF3A3F47), Color(0xFF6E757E)),
        Triple("Marfil", Color(0xFFD9D2C3), Color(0xFFF2EEE4)),
        Triple("Grafito", Color(0xFF4A4F55), Color(0xFF80868D)),
        Triple("Cobalto", Color(0xFF2C4E8A), Color(0xFF6B8FC9)),
        Triple("Burdeos", Color(0xFF6E2434), Color(0xFFA85F6E)),
        Triple("Violeta profundo", Color(0xFF4A3B6B), Color(0xFF8577A8)),
        Triple("Turquesa", Color(0xFF2A6E6E), Color(0xFF6FAAAA)),
        Triple("Bronce", Color(0xFF8C6A3F), Color(0xFFC19B6C)),
        Triple("Acero blanco", Color(0xFFC3CBD3), Color(0xFFE9EEF2)),
        Triple("Negro mar", Color(0xFF1E232A), Color(0xFF515962)),
        Triple("Coral", Color(0xFFB4593F), Color(0xFFDC9179)),
        Triple("Esmeralda", Color(0xFF1F6B45), Color(0xFF5FA37C)),
    )

    private val accents = listOf(
        Color(0xFFF2B33D), Color(0xFFE8562A), Color(0xFF7FC4E8), Color(0xFF4CAF7D),
        Color(0xFFD9D2C3),
    )

    /** Las 100 pinturas: cinco patrones por cada una de las veinte bases de color. */
    val all: List<Skin> = buildList {
        var index = 0
        SkinPattern.entries.forEachIndexed { patternIndex, pattern ->
            bases.forEachIndexed { baseIndex, (name, hull, deck) ->
                val id = "${pattern.name.lowercase()}-$baseIndex"
                // Las diez primeras vienen de serie; a partir de ahí, una cada dos hundimientos.
                val unlockAt = ((index - 9).coerceAtLeast(0)) * 2
                add(
                    Skin(
                        id = id,
                        label = "$name · ${pattern.label}",
                        pattern = pattern,
                        hull = hull,
                        deck = deck,
                        accent = accents[(patternIndex + baseIndex) % accents.size],
                        unlockAt = unlockAt,
                    ),
                )
                index++
            }
        }
    }

    /** La pintura de serie, la que se usa si la guardada ya no existe. */
    val default: Skin get() = all.first()

    fun byId(id: String): Skin = all.firstOrNull { it.id == id } ?: default

    fun unlocked(sunkShips: Int): List<Skin> = all.filter { it.unlockAt <= sunkShips }
}

// --------------------------------------------------------------------------- emblemas

/** Emblema del jugador, dibujado a mano con Canvas. */
data class Emblem(val id: String, val label: String, val unlockAtWins: Int)

object Emblems {
    val all = listOf(
        Emblem("ancla", "Ancla", 0),
        Emblem("timon", "Timón", 0),
        Emblem("estrella", "Estrella", 0),
        Emblem("brujula", "Brújula", 0),
        Emblem("ola", "Ola", 1),
        Emblem("tridente", "Tridente", 3),
        Emblem("rayo", "Rayo", 5),
        Emblem("canon", "Cañón", 8),
        Emblem("calavera", "Calavera", 12),
        Emblem("corona", "Corona", 18),
        Emblem("nudo", "Nudo", 25),
        Emblem("kraken", "Kraken", 40),
    )

    fun byId(id: String): Emblem = all.firstOrNull { it.id == id } ?: all.first()
    fun unlocked(wins: Int): List<Emblem> = all.filter { it.unlockAtWins <= wins }
}

// --------------------------------------------------------------------------- planos

/**
 * Plano de un buque histórico. Se desbloquean hundiendo barcos, igual que en el juego de
 * referencia: cada hundimiento acerca el siguiente plano. Los datos son los reales del buque.
 */
data class Blueprint(
    val id: String,
    val name: String,
    val nationId: String,
    val year: Int,
    val note: String,
    val unlockAt: Int,
)

object Blueprints {
    val all = listOf(
        Blueprint("dreadnought", "HMS Dreadnought", "reinounido", 1906,
            "El acorazado que dio nombre a toda una generación de buques.", 0),
        Blueprint("mikasa", "Mikasa", "japon", 1900,
            "Buque insignia en Tsushima; hoy se conserva en seco en Yokosuka.", 2),
        Blueprint("espana", "España", "espana", 1913,
            "Primero de los tres acorazados de la clase España, los más pequeños de su tipo.", 4),
        Blueprint("minas", "Minas Geraes", "brasil", 1908,
            "Construido en Inglaterra, desató una carrera naval en Sudamérica.", 6),
        Blueprint("marat", "Marat", "rusia", 1911,
            "Botado como Petropávlovsk; resistió el sitio de Leningrado.", 9),
        Blueprint("yavuz", "Yavuz", "turquia", 1911,
            "Antes el alemán Goeben; sirvió hasta 1950 como buque insignia turco.", 12),
        Blueprint("texas", "USS Texas", "usa", 1912,
            "Único acorazado superviviente de los anteriores a la Gran Guerra.", 15),
        Blueprint("hood", "HMS Hood", "reinounido", 1918,
            "El crucero de batalla más grande jamás construido.", 19),
        Blueprint("nagato", "Nagato", "japon", 1919,
            "Primer acorazado con cañones de 410 mm.", 23),
        Blueprint("cervera", "Almirante Cervera", "espana", 1925,
            "Crucero ligero español de larga vida, en servicio hasta 1965.", 27),
        Blueprint("spee", "Admiral Graf Spee", "alemania", 1934,
            "«Acorazado de bolsillo» hundido por su tripulación en el Río de la Plata.", 32),
        Blueprint("dunkerque", "Dunkerque", "francia", 1935,
            "Torres cuádruples a proa, una silueta inconfundible.", 37),
        Blueprint("littorio", "Littorio", "italia", 1937,
            "Acorazado rápido italiano, protagonista en Tarento.", 42),
        Blueprint("scharnhorst", "Scharnhorst", "alemania", 1936,
            "Rápido y bien protegido; hundido en Cabo Norte.", 48),
        Blueprint("richelieu", "Richelieu", "francia", 1939,
            "El acorazado francés más potente de su tiempo.", 54),
        Blueprint("bismarck", "Bismarck", "alemania", 1939,
            "Hundió al Hood y fue cazado tres días después.", 60),
        Blueprint("pow", "HMS Prince of Wales", "reinounido", 1939,
            "Peleó contra el Bismarck recién salido del astillero.", 67),
        Blueprint("tirpitz", "Tirpitz", "alemania", 1939,
            "La «Bestia Solitaria» de los fiordos noruegos.", 74),
        Blueprint("iowa", "USS Iowa", "usa", 1942,
            "Acorazado rápido de 33 nudos, en servicio hasta 1990.", 82),
        Blueprint("missouri", "USS Missouri", "usa", 1944,
            "En su cubierta se firmó la rendición de Japón.", 90),
        Blueprint("musashi", "Musashi", "japon", 1940,
            "Gemelo del Yamato; cañones de 460 mm.", 100),
        Blueprint("yamato", "Yamato", "japon", 1940,
            "El acorazado más grande y mejor armado de la historia.", 110),
    )

    fun unlocked(sunkShips: Int): List<Blueprint> = all.filter { it.unlockAt <= sunkShips }

    /** El siguiente plano por desbloquear, o null si ya están todos. */
    fun next(sunkShips: Int): Blueprint? = all.firstOrNull { it.unlockAt > sunkShips }
}

// --------------------------------------------------------------------------- medallas

/** Una medalla y la condición que la gana. */
data class Medal(
    val id: String,
    val label: String,
    val description: String,
    val earned: (Stats) -> Boolean,
)

object Medals {
    val all = listOf(
        Medal("primer_hundimiento", "Primera sangre", "Hunde tu primer barco") { it.sunkByMe >= 1 },
        Medal("primera_victoria", "Primera victoria", "Gana una partida") { it.won >= 1 },
        Medal("artillero", "Artillero", "100 impactos en total") { it.hits >= 100 },
        Medal("maestro_artillero", "Maestro artillero", "500 impactos en total") { it.hits >= 500 },
        Medal("francotirador", "Francotirador", "Gana con un 50 % de acierto o más") { it.bestAccuracy >= 50 },
        Medal("certero", "Puntería de almirante", "Gana con un 65 % de acierto o más") { it.bestAccuracy >= 65 },
        Medal("racha3", "Tres seguidas", "Gana tres partidas seguidas") { it.bestStreak >= 3 },
        Medal("racha10", "Invicto", "Gana diez partidas seguidas") { it.bestStreak >= 10 },
        Medal("almirante", "Almirante", "Gana 50 partidas") { it.won >= 50 },
        Medal("tres_flotas", "Las tres flotas", "Gana con la Estándar, la Clásica y la Rusa") { it.fleetsWon >= 3 },
        Medal("cinco_reglas", "Todas las reglas", "Gana con las cinco reglas de disparo") { it.rulesWon >= 5 },
        Medal("contra_la_maquina", "A pulso", "Gana a la máquina en Difícil") { it.hardWins >= 1 },
        Medal("veterano", "Veterano", "Juega 100 partidas") { it.played >= 100 },
        Medal("limpia", "Victoria limpia", "Gana en 40 disparos o menos") { it.bestWinShots in 1..40 },
    )

    fun earned(stats: Stats): List<Medal> = all.filter { it.earned(stats) }
}
