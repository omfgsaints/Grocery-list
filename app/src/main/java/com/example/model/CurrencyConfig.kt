package com.example.model

data class CurrencyConfig(
    val code: String,
    val symbol: String,
    val name: String,
    val flag: String,
    val approximatePhpRate: Double = 1.0 // for reference conversions
) {
    companion object {
        val PHP = CurrencyConfig("PHP", "₱", "Philippine Peso", "🇵🇭", 1.0)
        val USD = CurrencyConfig("USD", "$", "US Dollar", "🇺🇸", 58.5)
        val EUR = CurrencyConfig("EUR", "€", "Euro", "🇪🇺", 63.2)
        val GBP = CurrencyConfig("GBP", "£", "British Pound", "🇬🇧", 74.8)
        val JPY = CurrencyConfig("JPY", "¥", "Japanese Yen", "🇯🇵", 0.39)
        val CAD = CurrencyConfig("CAD", "CA$", "Canadian Dollar", "🇨🇦", 42.5)
        val AUD = CurrencyConfig("AUD", "AU$", "Australian Dollar", "🇦🇺", 38.0)
        val SGD = CurrencyConfig("SGD", "S$", "Singapore Dollar", "🇸🇬", 44.5)

        val ALL_CURRENCIES = listOf(PHP, USD, EUR, GBP, JPY, CAD, AUD, SGD)

        fun detectCurrency(latitude: Double?, longitude: Double?, address: String?): CurrencyConfig {
            // Check if coordinates fall within Philippine bounding box roughly:
            // Lat: 4.5 to 21.5, Long: 116.5 to 127.0
            if (latitude != null && longitude != null) {
                if (latitude in 4.5..21.5 && longitude in 116.5..127.0) {
                    return PHP
                }
            }

            // Check address keywords for Philippines
            val lower = address?.lowercase() ?: ""
            if (lower.contains("philippines") ||
                lower.contains("manila") ||
                lower.contains("cebu") ||
                lower.contains("davao") ||
                lower.contains("quezon") ||
                lower.contains("makati") ||
                lower.contains("taguig") ||
                lower.contains("pasig") ||
                lower.contains("pampanga") ||
                lower.contains("baguio") ||
                lower.contains("ilocos") ||
                lower.contains("cavite") ||
                lower.contains("laguna") ||
                lower.contains("batangas") ||
                lower.contains("boracay") ||
                lower.contains("palawan")
            ) {
                return PHP
            }

            return USD
        }
    }
}
