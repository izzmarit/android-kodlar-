package com.example.kuluckakontrolu.utils

import com.example.kuluckakontrolu.model.IncubationCycle
import com.example.kuluckakontrolu.model.ProfileStageSettings

object ProfileUtils {
    // Profil tiplerine göre toplam gün sayıları
    fun getTotalDaysForProfile(animalType: String): Int {
        return when (animalType) {
            "TAVUK" -> 21
            "ÖRDEK" -> 28
            "BILDIRCIN" -> 18
            "KAZ" -> 30
            "HİNDİ" -> 28
            "KEKLIK" -> 24
            "GUVERCIN" -> 18
            "SULUN" -> 25
            else -> 21
        }
    }

    // Profil tipine ve güne göre sıcaklık/nem ayarlarını hesapla
    fun getSettingsForDay(animalType: String, day: Int): Pair<Double, Int> {
        return when (animalType) {
            "TAVUK" -> {
                when {
                    day <= 9 -> Pair(37.8, 55)
                    day <= 17 -> Pair(37.5, 60)
                    else -> Pair(37.2, 70)
                }
            }
            "ÖRDEK" -> {
                when {
                    day <= 9 -> Pair(37.7, 65)
                    day <= 24 -> Pair(37.5, 70)
                    else -> Pair(37.2, 80)
                }
            }
            "BILDIRCIN" -> {
                when {
                    day <= 6 -> Pair(37.7, 60)
                    day <= 14 -> Pair(37.5, 65)
                    else -> Pair(37.2, 75)
                }
            }
            "KAZ" -> {
                when {
                    day <= 14 -> Pair(37.7, 60)
                    day <= 24 -> Pair(37.5, 65)
                    else -> Pair(37.2, 75)
                }
            }
            "HİNDİ" -> {
                when {
                    day <= 10 -> Pair(37.8, 58)
                    day <= 24 -> Pair(37.5, 63)
                    else -> Pair(37.2, 75)
                }
            }
            "KEKLIK" -> {
                when {
                    day <= 8 -> Pair(37.7, 62)
                    day <= 20 -> Pair(37.5, 65)
                    else -> Pair(37.2, 72)
                }
            }
            "GUVERCIN" -> {
                when {
                    day <= 5 -> Pair(37.8, 65)
                    day <= 14 -> Pair(37.5, 60)
                    else -> Pair(37.2, 70)
                }
            }
            "SULUN" -> {
                when {
                    day <= 8 -> Pair(37.7, 60)
                    day <= 21 -> Pair(37.5, 55)
                    else -> Pair(37.2, 70)
                }
            }
            else -> Pair(37.5, 65) // Manuel/varsayılan değerler
        }
    }

    // Motor durumunu profil ve gün bazında değerlendir
    fun shouldMotorBeActive(animalType: String, day: Int): Boolean {
        return when (animalType) {
            "TAVUK" -> day < 18
            "ÖRDEK" -> day < 25
            "BILDIRCIN" -> day < 15
            "KAZ" -> day < 25
            "HİNDİ" -> day < 25
            "KEKLIK" -> day < 21
            "GUVERCIN" -> day < 15
            "SULUN" -> day < 22
            else -> true
        }
    }
}