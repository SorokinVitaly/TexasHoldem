package com.example.texasholdem

fun String.splitItems(delimiter: Char = ','): List<String> =
    substring(1, length - 1).split(delimiter).map { it.trim() }