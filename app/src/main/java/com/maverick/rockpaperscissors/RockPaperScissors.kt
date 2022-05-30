package com.maverick.rockpaperscissors

class RockPaperScissors {
    enum class Result {
        WIN,
        LOSE,
        TIE;

        operator fun not(): Result {
            return when (this) {
                WIN -> LOSE
                LOSE -> WIN
                TIE -> TIE
            }
        }

        companion object {
            fun summary(results: List<Result>): Map<Result, Int> {
                return results.groupingBy { it }.aggregate { _, accumulator: Int?, _, first ->
                    if (first) 1 else accumulator!!.plus(1)
                }
            }
        }
    }

    enum class Gesture(val string: String) {
        ROCK("rock"),
        PAPER("paper"),
        SCISSORS("scissors");

        companion object {
            fun fromName(name: String): Gesture =
                values().find { it.string.equals(name, true) } ?: throw Exception("format error")
        }

        fun beats(other: Gesture): Result {
            return when (this) {
                ROCK -> when (other) {
                    ROCK -> Result.TIE
                    PAPER -> Result.LOSE
                    SCISSORS -> Result.WIN
                }
                PAPER -> when (other) {
                    ROCK -> Result.WIN
                    PAPER -> Result.TIE
                    SCISSORS -> Result.LOSE
                }
                SCISSORS -> when (other) {
                    ROCK -> Result.LOSE
                    PAPER -> Result.WIN
                    SCISSORS -> Result.TIE
                }
            }
        }
    }

    companion object {
        fun getResults(gestures: List<Gesture>): List<Result> {
            if (gestures.isEmpty()) {
                return emptyList()
            }
            if (gestures.size == 1) {
                return listOf(Result.TIE)
            }
            val distinctGestures = gestures.distinct()
            return when (distinctGestures.size) {
                3, 1 -> MutableList(gestures.size) { Result.TIE }
                else -> {
                    val first = distinctGestures.first()
                    val firstResult = first.beats(distinctGestures[1])
                    gestures.map {
                        if (it == first)
                            firstResult
                        else
                            firstResult.not()
                    }
                }
            }
        }
    }
}

