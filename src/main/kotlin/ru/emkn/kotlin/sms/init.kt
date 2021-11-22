package ru.emkn.kotlin.sms

import com.sksamuel.hoplite.ConfigLoader

data class GroupData(
    val group: String,
    val distance: String
)
data class CriteriaData(
    val distance: String,
    val checkpoints: List<String>
)
data class ConfigData(
    val eventName: String, val eventDate: String, val eventSport: String,
    val ranks: List<String>,
    val groups: List<GroupData>,
    val criteria: List<CriteriaData>)

val config = ConfigLoader().loadConfigOrThrow<ConfigData>("/config.yaml")
val RANKS = config.ranks
val GROUP_NAMES = config.groups.map { it.group }
