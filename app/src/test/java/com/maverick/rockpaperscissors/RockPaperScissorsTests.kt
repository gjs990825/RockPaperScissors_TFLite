package com.maverick.rockpaperscissors


import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class RockPaperScissorsTests {
    @Test
    fun rockPaperScissorsTests() {
        assertEquals(RockPaperScissors.Gesture.fromName("Rock"), RockPaperScissors.Gesture.ROCK)
        assertEquals(RockPaperScissors.Gesture.fromName("Paper"), RockPaperScissors.Gesture.PAPER)
        assertEquals(
            RockPaperScissors.Gesture.fromName("Scissors"),
            RockPaperScissors.Gesture.SCISSORS
        )

        assertEquals(
            RockPaperScissors.Gesture.ROCK.beats(RockPaperScissors.Gesture.SCISSORS),
            RockPaperScissors.Result.WIN
        )

        assertArrayEquals(
            arrayOf(
                RockPaperScissors.getResults(
                    listOf(
                        RockPaperScissors.Gesture.ROCK,
                        RockPaperScissors.Gesture.PAPER,
                        RockPaperScissors.Gesture.SCISSORS
                    )
                )
            ), arrayOf(MutableList(3) { RockPaperScissors.Result.TIE })
        )
        assertArrayEquals(
            arrayOf(
                RockPaperScissors.getResults(
                    listOf(
                        RockPaperScissors.Gesture.ROCK,
                        RockPaperScissors.Gesture.ROCK,
                        RockPaperScissors.Gesture.ROCK
                    )
                )
            ), arrayOf(MutableList(3) { RockPaperScissors.Result.TIE })
        )

        print(
            RockPaperScissors.Result.summary(
                RockPaperScissors.getResults(
                    listOf(
                        RockPaperScissors.Gesture.ROCK,
                        RockPaperScissors.Gesture.PAPER,
                        RockPaperScissors.Gesture.PAPER
                    )
                )
            )
        )
    }
}
