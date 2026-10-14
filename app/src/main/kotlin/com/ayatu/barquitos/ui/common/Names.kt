package com.ayatu.barquitos.ui.common

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.ayatu.barquitos.R
import com.ayatu.barquitos.core.Difficulty
import com.ayatu.barquitos.core.GameMode
import com.ayatu.barquitos.core.ShipKind
import com.ayatu.barquitos.core.ShotRule

/**
 * Nombres y descripciones que ve el usuario. El motor (paquete `core`) no sabe nada de recursos
 * de Android, así que la traducción de sus enumerados a texto vive aquí.
 */

@get:StringRes
val ShipKind.labelRes: Int
    get() = when (this) {
        ShipKind.PORTAAVIONES -> R.string.ship_carrier
        ShipKind.ACORAZADO -> R.string.ship_battleship
        ShipKind.DESTRUCTOR -> R.string.ship_destroyer
        ShipKind.SUBMARINO -> R.string.ship_submarine
    }

@get:StringRes
val ShotRule.labelRes: Int
    get() = when (this) {
        ShotRule.CONTINUO -> R.string.rule_continuous
        ShotRule.UN_DISPARO -> R.string.rule_one_shot
    }

@get:StringRes
val ShotRule.descriptionRes: Int
    get() = when (this) {
        ShotRule.CONTINUO -> R.string.rule_continuous_desc
        ShotRule.UN_DISPARO -> R.string.rule_one_shot_desc
    }

@get:StringRes
val Difficulty.labelRes: Int
    get() = when (this) {
        Difficulty.FACIL -> R.string.difficulty_easy
        Difficulty.NORMAL -> R.string.difficulty_normal
        Difficulty.DIFICIL -> R.string.difficulty_hard
    }

@get:StringRes
val Difficulty.descriptionRes: Int
    get() = when (this) {
        Difficulty.FACIL -> R.string.difficulty_easy_desc
        Difficulty.NORMAL -> R.string.difficulty_normal_desc
        Difficulty.DIFICIL -> R.string.difficulty_hard_desc
    }

@get:StringRes
val GameMode.labelRes: Int
    get() = when (this) {
        GameMode.UN_JUGADOR -> R.string.mode_single
        GameMode.WIFI_LOCAL -> R.string.mode_wifi
    }

@Composable fun ShipKind.label(): String = stringResource(labelRes)

@Composable fun ShotRule.label(): String = stringResource(labelRes)

@Composable fun Difficulty.label(): String = stringResource(labelRes)

@Composable fun GameMode.label(): String = stringResource(labelRes)
