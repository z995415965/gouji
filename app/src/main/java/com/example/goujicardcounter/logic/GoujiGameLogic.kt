// GoujiGameLogic.kt: Core logic for Goju card game rules and statistics
package com.example.goujicardcounter.logic

/**
 * Implements Goju card game rules:
 * - 5-deck and 6-deck variants
 * - Player positions: North, NE, SE, South, SW, NW
 * - Federal partners: SW & NE (for South player)
 * - Card counting and remaining calculation
 * - Special card detection (烧牌, 点牌, 够级牌)
 */
object GoujiGameLogic {

    // Card values in Goju order (low to high)
    val CARD_VALUES = listOf("3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K", "A", "2", "小王", "大王")

    /** Total cards per rank based on deck count */
    fun getTotalCards(deckCount: Int, cardValue: String): Int {
        return when (cardValue) {
            "小王", "大王" -> deckCount  // One per deck
            else -> deckCount * 4        // Four suits per deck
        }
    }

    /** Initialize empty card count map */
    fun initCardCounts(deckCount: Int): MutableMap<String, Int> {
        return mutableMapOf<String, Int>().apply {
            CARD_VALUES.forEach { card ->
                put(card, getTotalCards(deckCount, card))
            }
        }
    }

    /** Record a played card and decrease count */
    fun recordPlayedCard(cardCounts: MutableMap<String, Int>, cardValue: String) {
        if (cardCounts.containsKey(cardValue)) {
            val current = cardCounts[cardValue] ?: 0
            if (current > 0) {
                cardCounts[cardValue] = current - 1
            }
        }
    }

    /** Reset all card counts (new game) */
    fun resetCardCounts(deckCount: Int): MutableMap<String, Int> {
        return initCardCounts(deckCount)
    }

    /** Detect if a card is a "够级牌" (Goju card - usually 6 or 7) */
    fun isGoujiCard(cardValue: String): Boolean {
        return cardValue == "6" || cardValue == "7"
    }

    /** Detect federal partner position relative to South player */
    fun isFederalPartner(position: PlayerPosition, myPosition: PlayerPosition): Boolean {
        return (myPosition == PlayerPosition.SOUTH && position == PlayerPosition.SW) ||
               (myPosition == PlayerPosition.SOUTH && position == PlayerPosition.NE) ||
               (myPosition == PlayerPosition.SW && position == PlayerPosition.SOUTH) ||
               (myPosition == PlayerPosition.SW && position == PlayerPosition.NE) ||
               (myPosition == PlayerPosition.NE && position == PlayerPosition.SOUTH) ||
               (myPosition == PlayerPosition.NE && position == PlayerPosition.SW)
    }

    /** Calculate remaining cards for display */
    fun getRemainingStats(cardCounts: Map<String, Int>): String {
        return CARD_VALUES.joinToString("\n") { card ->
            val remaining = cardCounts[card] ?: 0
            val total = getTotalCards(6, card)  // Assume 6-deck for display
            "$card: $remaining/$total"
        }
    }

    /** Player positions in Goju */
    enum class PlayerPosition {
        NORTH,      // 上家
        NORTHEAST,  // 右上/联邦
        SOUTHEAST,  // 下家
        SOUTH,      // 自己
        SOUTHWEST,  // 右下/联邦
        NORTHWEST   // 左上/对手
    }

    /** Single played card result */
    data class PlayedCard(
        val player: PlayerPosition,
        val cardValue: String,
        val cardCount: Int,  // How many of this card
        val timestamp: Long = System.currentTimeMillis()
    )

    /** Track play history for deduplication */
    private val playHistory = mutableListOf<PlayedCard>()
    private var lastPlayTime = 0L
    private val DEBOUNCE_MS = 2000L  // 2 second debounce

    /**
     * Record a play with debouncing
     * Returns true if the play was actually recorded (not duplicate)
     */
    fun recordPlay(cardCounts: MutableMap<String, Int>, play: PlayedCard): Boolean {
        // Debounce: ignore if same card played within 2 seconds
        if (System.currentTimeMillis() - lastPlayTime < DEBOUNCE_MS) {
            return false
        }

        // Validate card value
        if (!CARD_VALUES.contains(play.cardValue)) {
            return false
        }

        // Record the play
        playHistory.add(play)
        lastPlayTime = play.timestamp

        // Deduct from counts
        for (i in 1..play.cardCount) {
            recordPlayedCard(cardCounts, play.cardValue)
        }

        return true
    }

    /** Clear play history (new game) */
    fun clearHistory() {
        playHistory.clear()
        lastPlayTime = 0
    }

    /** Get play history for debugging */
    fun getPlayHistory(): List<PlayedCard> {
        return playHistory.toList()
    }
}
