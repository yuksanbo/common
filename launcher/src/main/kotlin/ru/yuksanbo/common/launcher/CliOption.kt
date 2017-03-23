package ru.yuksanbo.common.launcher

data class CliOption(
        val shortName: String,
        val longName: String,
        val hasArg: Boolean,
        val description: String
)