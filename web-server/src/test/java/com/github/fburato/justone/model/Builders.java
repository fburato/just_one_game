package com.github.fburato.justone.model;

import com.github.fburato.functionalutils.utils.Builder;

import java.util.List;
import java.util.Optional;

public class Builders {

    public static TurnBuilder turnBuilder() {
        return new TurnBuilder();
    }

    public static TurnBuilder turnBuilder(Turn turn) {
        return turnBuilder().with(tb -> {
            tb.phase = turn.phase();
            tb.providedHints = turn.providedHints();
            tb.hintsToFilter = turn.hintsToFilter();
            tb.hintsToRemove = turn.hintsToRemove();
            tb.wordGuessed = turn.wordGuessed();
            tb.players = turn.players();
        });
    }

    public static GameStateBuilder gameStateBuilder() {
        return new GameStateBuilder();
    }

    public static GameStateBuilder gameStateBuilder(GameState gameState) {
        return gameStateBuilder().with(gsb -> {
            gsb.id = gameState.id();
            gsb.status = gameState.status();
            gsb.players = gameState.players();
            gsb.turns = gameState.turns();
            gsb.wordsToGuess = gameState.wordsToGuess();
            gsb.currentTurn = gameState.currentTurn();
        });
    }

    public static class TurnBuilder extends Builder<Turn, TurnBuilder> {

        public TurnPhase phase;
        public List<PlayerWord> providedHints;
        public List<String> hintsToFilter;
        public List<PlayerWord> hintsToRemove;
        public Optional<PlayerWord> wordGuessed;
        public List<TurnPlayer> players;

        private TurnBuilder() {
            super(TurnBuilder::new);
        }

        @Override
        protected Turn makeValue() {
            return new Turn(phase, providedHints, hintsToFilter, hintsToRemove, wordGuessed, players);
        }
    }

    public static class GameStateBuilder extends Builder<GameState, GameStateBuilder> {

        public String id;
        public GameStatus status;
        public List<Player> players;
        public List<Turn> turns;
        public List<String> wordsToGuess;
        public int currentTurn;

        private GameStateBuilder() {
            super(GameStateBuilder::new);
        }

        @Override
        protected GameState makeValue() {
            return new GameState(id, status, players, turns, wordsToGuess, currentTurn);
        }
    }
}
