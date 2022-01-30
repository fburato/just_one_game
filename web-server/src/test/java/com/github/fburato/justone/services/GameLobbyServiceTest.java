package com.github.fburato.justone.services;

import com.github.fburato.justone.internals.CrudRepository;
import com.github.fburato.justone.io.InMemoryCrudRepository;
import com.github.fburato.justone.model.Lobby;
import com.github.fburato.justone.model.PlayerConnection;
import com.github.fburato.justone.model.PlayerConnectionStatus;
import com.github.fburato.justone.services.errors.ConflictException;
import com.github.fburato.justone.services.errors.EntityIdMismatchException;
import com.github.fburato.justone.services.errors.UnauthorisedException;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.github.fburato.justone.RandomUtils.randomEnum;
import static com.github.fburato.justone.RandomUtils.randomString;
import static org.apache.commons.lang3.RandomUtils.nextInt;
import static org.assertj.core.api.Assertions.assertThat;

class GameLobbyServiceTest {

    private final CrudRepository<String, Lobby> lobbyRepository = new InMemoryCrudRepository<>(Lobby::gameId);
    private final String gameId = randomString();
    private final String host = randomString();
    private final List<PlayerConnection> playerConnections = List.of(randomPlayerConnection(), randomPlayerConnection());
    private final Lobby lobby = new Lobby(gameId, host, playerConnections);

    private final GameLobbyService testee = new GameLobbyService(lobbyRepository);

    private PlayerConnection randomPlayerConnection() {
        return new PlayerConnection(randomString(), randomEnum(PlayerConnectionStatus.class));
    }

    @Nested
    @DisplayName("createLobby should")
    class CreateLobbyTests {


        @Test
        @DisplayName("fail with ConflictException if lobby with provided id already defined")
        void failOnAlreadyExisting() {
            lobbyRepository.save(lobby);

            StepVerifier.create(testee.createLobby(lobby))
                    .verifyErrorSatisfies(error ->
                            assertThat(error)
                                    .isInstanceOfSatisfying(ConflictException.class, c -> {
                                        assertThat(c.entityType()).isEqualTo("Lobby");
                                        assertThat(c.entityId()).isEqualTo(gameId);
                                    }));
        }

        @Test
        @DisplayName("define lobby on repository if previously undefined")
        void defineLobby() {
            StepVerifier.create(testee.createLobby(lobby))
                    .expectNextCount(1)
                    .verifyComplete();

            assertThat(lobbyRepository.findById(gameId))
                    .contains(lobby);
        }
    }

    @Nested
    @DisplayName("updateLobby should")
    class UpdateLobbyTests {

        @Test
        @DisplayName("return empty if lobby is not defined")
        void emptyOnNotDefined() {
            StepVerifier.create(testee.updateLobby(gameId, lobby))
                    .expectNext(Optional.empty())
                    .verifyComplete();
        }

        @Test
        @DisplayName("fail with EntityIdMismatchException if gameId does not match lobby id")
        void failOnIdMismatch() {
            final var otherGameId = randomString();
            lobbyRepository.save(lobby);

            StepVerifier.create(testee.updateLobby(gameId, new Lobby(otherGameId, host, playerConnections)))
                    .verifyErrorSatisfies(t -> assertThat(t)
                            .isInstanceOfSatisfying(EntityIdMismatchException.class, e -> {
                                assertThat(e.resourceId()).isEqualTo(gameId);
                                assertThat(e.entityId()).isEqualTo(otherGameId);
                            }));
        }

        @Test
        @DisplayName("update the lobby in repository")
        void updateLobby() {
            final var otherLobby = new Lobby(gameId, randomString(), List.of(randomPlayerConnection()));
            lobbyRepository.save(lobby);

            StepVerifier.create(testee.updateLobby(gameId, otherLobby))
                    .expectNextCount(1)
                    .verifyComplete();

            assertThat(lobbyRepository.findById(gameId))
                    .contains(otherLobby);
        }

        @Test
        @DisplayName("return the updated lobby")
        void returnUpdatedLobby() {
            final var otherLobby = new Lobby(gameId, randomString(), List.of(randomPlayerConnection()));
            lobbyRepository.save(lobby);

            StepVerifier.create(testee.updateLobby(gameId, otherLobby))
                    .expectNext(Optional.of(otherLobby))
                    .verifyComplete();
        }
    }

    @Nested
    @DisplayName("getLobby should")
    class GetLobbyTests {

        @Test
        @DisplayName("return empty if lobby not defined")
        void emptyOnUndefined() {
            StepVerifier.create(testee.getLobby(gameId))
                    .expectNext(Optional.empty())
                    .verifyComplete();
        }

        @Test
        @DisplayName("return the lobby from the repository")
        void lobbyFromRepository() {
            lobbyRepository.save(lobby);

            StepVerifier.create(testee.getLobby(gameId))
                    .expectNext(Optional.of(lobby))
                    .verifyComplete();
        }
    }

    @Nested
    @DisplayName("deleteLobby should")
    class DeleteLobbyTests {

        @Test
        @DisplayName("return empty if lobby not defined")
        void emptyOnUndefined() {
            StepVerifier.create(testee.deleteLobby(gameId))
                    .expectNext(Optional.empty())
                    .verifyComplete();
        }

        @Test
        @DisplayName("remove existing lobby from repository")
        void deleteLobbyFromRepo() {
            lobbyRepository.save(lobby);

            StepVerifier.create(testee.deleteLobby(gameId))
                    .expectNextCount(1)
                    .verifyComplete();

            assertThat(lobbyRepository.findById(gameId))
                    .isEmpty();
        }

        @Test
        @DisplayName("return removed lobby")
        void returnRemoved() {
            lobbyRepository.save(lobby);

            StepVerifier.create(testee.deleteLobby(gameId))
                    .expectNext(Optional.of(lobby))
                    .verifyComplete();
        }

    }

    @Nested
    @DisplayName("joinLobby should")
    class JoinLobbyTests {

        @Test
        @DisplayName("return empty if lobby is not defined")
        void emptyOnUndefined() {
            StepVerifier.create(testee.joinLobby(gameId, randomString()))
                    .expectNext(Optional.empty())
                    .verifyComplete();
        }

        @Test
        @DisplayName("fail with ConflictException if player is already in lobby")
        void failWithConflict() {
            lobbyRepository.save(lobby);

            StepVerifier.create(testee.joinLobby(gameId, playerConnections.get(0).playerId()))
                    .verifyErrorSatisfies(t -> assertThat(t)
                            .isInstanceOfSatisfying(ConflictException.class, c -> {
                                assertThat(c.entityType()).isEqualTo("playerId");
                                assertThat(c.entityId()).isEqualTo(playerConnections.get(0).playerId());
                            }));
        }

        @Test
        @DisplayName("add player to lobby with REQUESTED_TO_JOIN status if it doesn't already exists")
        void addPlayer() {
            final var newPlayer = randomString();
            lobbyRepository.save(lobby);

            StepVerifier.create(testee.joinLobby(gameId, newPlayer))
                    .expectNextCount(1)
                    .verifyComplete();

            final var updatedLobby = lobbyRepository.findById(gameId).orElseThrow();
            assertThat(updatedLobby.gameId()).isEqualTo(lobby.gameId());
            assertThat(updatedLobby.host()).isEqualTo(lobby.host());
            assertThat(updatedLobby.playerConnections()).containsExactlyInAnyOrderElementsOf(Stream.concat(
                    playerConnections.stream(), Stream.of(new PlayerConnection(newPlayer, PlayerConnectionStatus.REQUESTED_TO_JOIN))
            ).collect(Collectors.toList()));
        }

        @Test
        @DisplayName("return updated lobby if player added")
        void returnUpdated() {
            final var newPlayer = randomString();
            lobbyRepository.save(lobby);
            final AtomicReference<Lobby> returnedLobby = new AtomicReference<>();

            StepVerifier.create(testee.joinLobby(gameId, newPlayer))
                    .expectNextMatches(maybeLobby -> {
                        returnedLobby.set(maybeLobby.orElseThrow());
                        return true;
                    })
                    .verifyComplete();

            assertThat(returnedLobby.get()).isEqualTo(lobbyRepository.findById(gameId).orElseThrow());
        }
    }

    @Nested
    @DisplayName("updatePlayerState should")
    class UpdatePlayerStateTests {

        private final PlayerConnection firstPlayerConnection = playerConnections.get(0);

        @Test
        @DisplayName("return empty if lobby does not exists")
        void emptyOnUndefined() {
            StepVerifier.create(testee.updatePlayerState(gameId, host, firstPlayerConnection.playerId(), randomEnum(PlayerConnectionStatus.class)))
                    .expectNext(Optional.empty())
                    .verifyComplete();
        }

        @Test
        @DisplayName("fail with UnauthorisedException if host is not the lobby host")
        void failOnUpdateAsNotHost() {
            final var otherHost = randomString();

            lobbyRepository.save(lobby);

            StepVerifier.create(testee.updatePlayerState(gameId, otherHost, firstPlayerConnection.playerId(), randomEnum(PlayerConnectionStatus.class)))
                    .verifyErrorSatisfies(t ->
                            assertThat(t)
                                    .isInstanceOf(UnauthorisedException.class)
                                    .hasMessageContaining(String.format("host='%s' is not host of gameId='%s'", otherHost, gameId)));
        }

        @Test
        @DisplayName("do nothing if playerId is not in lobby")
        void doNothingOnUndefinedPlayer() {
            lobbyRepository.save(lobby);

            StepVerifier.create(testee.updatePlayerState(gameId, host, randomString(), randomEnum(PlayerConnectionStatus.class)))
                    .expectNextCount(1)
                    .verifyComplete();

            assertThat(lobbyRepository.findById(gameId))
                    .contains(new Lobby(gameId, host, playerConnections));
        }

        @Test
        @DisplayName("return the lobby status if playerId is not in lobby")
        void returnLobbyOnUndefinedPlayer() {
            lobbyRepository.save(lobby);

            StepVerifier.create(testee.updatePlayerState(gameId, host, randomString(), randomEnum(PlayerConnectionStatus.class)))
                    .expectNext(Optional.of(lobby))
                    .verifyComplete();
        }

        @ParameterizedTest
        @EnumSource(PlayerConnectionStatus.class)
        @DisplayName("update the player status if it is in the lobby")
        void updateExistingPlayerStatus(PlayerConnectionStatus status) {
            lobbyRepository.save(lobby);

            StepVerifier.create(testee.updatePlayerState(gameId, host, firstPlayerConnection.playerId(), status))
                    .expectNextCount(1)
                    .verifyComplete();

            final var firstPlayerStatus = lobbyRepository.findById(gameId).orElseThrow().playerConnections().stream()
                    .filter(p -> StringUtils.equals(p.playerId(), firstPlayerConnection.playerId()))
                    .findFirst().orElseThrow()
                    .status();
            assertThat(firstPlayerStatus).isEqualTo(status);
        }

        @ParameterizedTest
        @EnumSource(PlayerConnectionStatus.class)
        @DisplayName("update only the selected player status if it is in the lobby")
        void updateOnlyPlayerId(PlayerConnectionStatus status) {
            final var playerIndexToUpdate = nextInt(0, playerConnections.size());
            lobbyRepository.save(lobby);
            final var otherPlayersBefore = playerConnections.stream()
                    .filter(p -> !StringUtils.equals(p.playerId(), playerConnections.get(playerIndexToUpdate).playerId()))
                    .toList();

            StepVerifier.create(testee.updatePlayerState(gameId, host, playerConnections.get(playerIndexToUpdate).playerId(), status))
                    .expectNextCount(1)
                    .verifyComplete();

            final var otherPlayersAfter = lobbyRepository.findById(gameId).orElseThrow().playerConnections().stream()
                    .filter(p -> !StringUtils.equals(p.playerId(), playerConnections.get(playerIndexToUpdate).playerId()))
                    .toList();
            assertThat(otherPlayersBefore).containsExactlyInAnyOrderElementsOf(otherPlayersAfter);
        }

        @ParameterizedTest
        @EnumSource(PlayerConnectionStatus.class)
        @DisplayName("return the updated lobby if player is in the lobby")
        void returnLobbyOnExistingPlayer(PlayerConnectionStatus status) {
            lobbyRepository.save(lobby);

            StepVerifier.create(testee.updatePlayerState(gameId, host, firstPlayerConnection.playerId(), status))
                    .expectNextMatches(result -> {
                        assertThat(result).isPresent();
                        final var resultLobby = result.orElseThrow();
                        assertThat(resultLobby.gameId()).isEqualTo(lobby.gameId());
                        assertThat(resultLobby.host()).isEqualTo(lobby.host());
                        final var expectedConnections = List.of(
                                new PlayerConnection(firstPlayerConnection.playerId(), status),
                                playerConnections.get(1)
                        );
                        assertThat(resultLobby.playerConnections()).containsExactlyInAnyOrderElementsOf(expectedConnections);
                        return true;
                    })
                    .verifyComplete();
        }
    }
}