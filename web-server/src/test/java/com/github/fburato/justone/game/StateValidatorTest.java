package com.github.fburato.justone.game;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class StateValidatorTest {

    @Test
    @DisplayName("should return NULL_ID if id is null")
    void nullId() {

    }

    @Test
    @DisplayName("should return NULL_STATUS if status is null")
    void nullStatus() {

    }

    @Test
    @DisplayName("should return PLAYER_NUM_INVALID if players are null")
    void nullPlayers() {

    }

    @Test
    @DisplayName("should return PLAYER_NUM_INVALID if players are empty")
    void emptyPlayers() {

    }

    @Test
    @DisplayName("should return NO_HOST if players does not contain a host")
    void noHost() {

    }

    @Test
    @DisplayName("should return WORDS_NUM_INVALID if words are null")
    void nullWordsToGuess() {

    }

    @Test
    @DisplayName("should return WORDS_NUM_INVALID if words are empty")
    void emptyWords() {

    }

    @Test
    @DisplayName("should return CURRENT_TURN_INVALID if currentTurn is negative")
    void negativeCurrentTurn() {

    }

    @Test
    @DisplayName("should return CURRENT_TURN_INVALID if currentTurn bigger than wordsToGuess.size()")
    void bigCurrentTurn() {

    }
}