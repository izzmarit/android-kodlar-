package com.example.kuluckakontrolu.utils

import com.example.kuluckakontrolu.model.IncubationCycle

object ProfileUtils {
    // Profil tiplerine göre toplam gün sayıları - ESP32'deki GlobalDefinitions.h ile uyumlu
    fun getTotalDaysForProfile(animalType: String): Int {
        return when (animalType) {
            "TAVUK" -> 21
            "ÖRDEK" -> 28
            "BILDIRCIN" -> 17 // ESP32'deki değerle uyumlu olarak düzeltildi
            "KAZ" -> 30
            "HİNDİ" -> 28
            "KEKLIK" -> 24
            "GUVERCIN" -> 18
            "SULUN" -> 25
            else -> 21
        }
    }

    // Profil tipine ve güne göre sıcaklık/nem ayarlarını hesapla
    // ESP32'deki ProfileModule.cpp ile uyumlu olarak düzeltildi
    fun getSettingsForDay(animalType: String, day: Int): Pair<Double, Int> {
        return when (animalType) {
            "TAVUK" -> {
                when {
                    day <= 17 -> Pair(37.8, 55)  // Erken ve orta aşama
                    day <= 19 -> Pair(37.5, 60)  // Son aşama
                    else -> Pair(37.2, 70)      // Çıkış aşaması
                }
            }
            "ÖRDEK" -> {
                when {
                    day <= 24 -> Pair(37.7, 65)  // Erken ve orta aşama
                    day <= 25 -> Pair(37.5, 70)  // Son aşama
                    else -> Pair(37.2, 80)      // Çıkış aşaması
                }
            }
            "BILDIRCIN" -> {
                when {
                    day <= 14 -> Pair(37.7, 60)  // Erken ve orta aşama
                    day <= 15 -> Pair(37.5, 65)  // Son aşama
                    else -> Pair(37.2, 75)      // Çıkış aşaması
                }
            }
            "KAZ" -> {
                when {
                    day <= 27 -> Pair(37.7, 60)  // Erken ve orta aşama
                    day <= 28 -> Pair(37.5, 65)  // Son aşama
                    else -> Pair(37.2, 75)      // Çıkış aşaması
                }
            }
            "HİNDİ" -> {
                when {
                    day <= 24 -> Pair(37.8, 58)  // Erken ve orta aşama
                    day <= 26 -> Pair(37.5, 63)  // Son aşama
                    else -> Pair(37.2, 75)      // Çıkış aşaması
                }
            }
            "KEKLIK" -> {
                when {
                    day <= 20 -> Pair(37.7, 62)  // Erken ve orta aşama
                    day <= 22 -> Pair(37.5, 65)  // Son aşama
                    else -> Pair(37.2, 72)      // Çıkış aşaması
                }
            }
            "GUVERCIN" -> {
                when {
                    day <= 14 -> Pair(37.8, 65)  // Erken ve orta aşama
                    day <= 16 -> Pair(37.5, 60)  // Son aşama
                    else -> Pair(37.2, 70)      // Çıkış aşaması
                }
            }
            "SULUN" -> {
                when {
                    day <= 21 -> Pair(37.7, 60)  // Erken ve orta aşama
                    day <= 23 -> Pair(37.5, 55)  // Son aşama
                    else -> Pair(37.2, 70)      // Çıkış aşaması
                }
            }
            else -> Pair(37.5, 65) // Manuel/varsayılan değerler
        }
    }

    // Motor durumunu profil ve gün bazında değerlendir
    // ESP32'deki ProfileModule.cpp ile uyumlu
    fun shouldMotorBeActive(animalType: String, day: Int): Boolean {
        val lastMotorDay = when (animalType) {
            "TAVUK" -> 18
            "ÖRDEK" -> 25
            "BILDIRCIN" -> 15
            "KAZ" -> 25
            "HİNDİ" -> 25
            "KEKLIK" -> 21
            "GUVERCIN" -> 15
            "SULUN" -> 22
            else -> 18
        }

        return day < lastMotorDay
    }

    // Profil tipi adını enum değerine çevir (ESP32 ile uyumlu)
    fun getProfileTypeForAnimal(animalType: String): Int {
        return when (animalType) {
            "TAVUK" -> 0    // PROFILE_CHICKEN
            "KAZ" -> 1      // PROFILE_GOOSE
            "BILDIRCIN" -> 2 // PROFILE_QUAIL
            "ÖRDEK" -> 3    // PROFILE_DUCK
            "HİNDİ" -> 5    // PROFILE_TURKEY
            "KEKLIK" -> 6   // PROFILE_PARTRIDGE
            "GUVERCIN" -> 7 // PROFILE_PIGEON
            "SULUN" -> 8    // PROFILE_PHEASANT
            else -> 4       // PROFILE_MANUAL
        }
    }

    // Enum değerinden profil tipi adı elde et (ESP32 ile uyumlu)
    fun getAnimalTypeForProfile(profileType: Int): String {
        return when (profileType) {
            0 -> "TAVUK"    // PROFILE_CHICKEN
            1 -> "KAZ"      // PROFILE_GOOSE
            2 -> "BILDIRCIN" // PROFILE_QUAIL
            3 -> "ÖRDEK"    // PROFILE_DUCK
            5 -> "HİNDİ"    // PROFILE_TURKEY
            6 -> "KEKLIK"   // PROFILE_PARTRIDGE
            7 -> "GUVERCIN" // PROFILE_PIGEON
            8 -> "SULUN"    // PROFILE_PHEASANT
            else -> "MANUEL"       // PROFILE_MANUAL
        }
    }

    // Kuluçka profili için aşama sınırlarını hesapla
    fun calculateStages(animalType: String): List<Pair<Int, Int>> {
        val totalDays = getTotalDaysForProfile(animalType)

        // Her hayvan türü için farklı aşamalar - ESP32'deki aşama yapısıyla uyumlu
        return when (animalType) {
            "TAVUK" -> listOf(
                Pair(0, 7),     // Erken aşama
                Pair(8, 17),    // Orta aşama
                Pair(18, 19),   // Son aşama
                Pair(20, 21)    // Çıkış aşaması
            )
            "ÖRDEK" -> listOf(
                Pair(0, 9),     // Erken aşama
                Pair(10, 24),   // Orta aşama
                Pair(25, 26),   // Son aşama
                Pair(27, 28)    // Çıkış aşaması
            )
            "BILDIRCIN" -> listOf(
                Pair(0, 5),     // Erken aşama
                Pair(6, 14),    // Orta aşama
                Pair(15, 16),   // Son aşama
                Pair(17, 17)    // Çıkış aşaması
            )
            "KAZ" -> listOf(
                Pair(0, 10),    // Erken aşama
                Pair(11, 27),   // Orta aşama
                Pair(28, 29),   // Son aşama
                Pair(30, 30)    // Çıkış aşaması
            )
            else -> listOf(
                Pair(0, totalDays / 3),                      // Erken aşama
                Pair(totalDays / 3 + 1, totalDays * 2 / 3),  // Orta aşama
                Pair(totalDays * 2 / 3 + 1, totalDays - 1),  // Son aşama
                Pair(totalDays, totalDays)                   // Çıkış aşaması
            )
        }
    }
}