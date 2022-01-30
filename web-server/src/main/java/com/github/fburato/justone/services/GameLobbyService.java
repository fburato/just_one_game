package com.github.fburato.justone.services;

import com.github.fburato.justone.internals.CrudRepository;
import com.github.fburato.justone.model.Lobby;
import com.github.fburato.justone.model.PlayerConnection;
import com.github.fburato.justone.model.PlayerConnectionStatus;
import com.github.fburato.justone.services.errors.ConflictException;
import com.github.fburato.justone.services.errors.EntityIdMismatchException;
import com.github.fburato.justone.services.errors.UnauthorisedException;
import org.apache.commons.lang3.StringUtils;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Optional;

public class GameLobbyService {

    private final CrudRepository<String, Lobby> lobbyRepository;

    public GameLobbyService(CrudRepository<String, Lobby> lobbyRepository) {
        this.lobbyRepository = lobbyRepository;
    }

    public Mono<Lobby> createLobby(Lobby lobby) {
        final var maybeLobby = lobbyRepository.findById(lobby.gameId());
        if (maybeLobby.isPresent()) {
            return Mono.error(new ConflictException(Lobby.class.getSimpleName(), lobby.gameId()));
        }
        return Mono.just(lobbyRepository.save(lobby));
    }

    public Mono<Optional<Lobby>> updateLobby(String gameId, Lobby lobby) {
        final var maybeLobby = lobbyRepository.findById(gameId);
        if (maybeLobby.isEmpty()) {
            return Mono.just(Optional.empty());
        }
        if (!StringUtils.equals(lobby.gameId(), gameId)) {
            return Mono.error(new EntityIdMismatchException(gameId, lobby.gameId()));
        }
        return Mono.just(Optional.of(lobbyRepository.save(lobby)));
    }

    public Mono<Optional<Lobby>> getLobby(String gameId) {
        return Mono.just(lobbyRepository.findById(gameId));
    }

    public Mono<Optional<Lobby>> deleteLobby(String gameId) {
        final var maybeLobby = lobbyRepository.findById(gameId);
        if (maybeLobby.isEmpty()) {
            return Mono.just(Optional.empty());
        }
        lobbyRepository.delete(gameId);
        return Mono.just(maybeLobby);
    }

    public Mono<Optional<Lobby>> joinLobby(String gameId, String playerId) {
        final var maybeLobby = lobbyRepository.findById(gameId);
        if (maybeLobby.isEmpty()) {
            return Mono.just(Optional.empty());
        }
        final var lobby = maybeLobby.get();
        if (lobby.playerConnections().stream()
                .map(PlayerConnection::playerId)
                .anyMatch(p -> StringUtils.equals(playerId, p))) {
            return Mono.error(new ConflictException("playerId", playerId));
        }
        final var newPlayers = new ArrayList<>(lobby.playerConnections());
        newPlayers.add(new PlayerConnection(playerId, PlayerConnectionStatus.REQUESTED_TO_JOIN));
        return Mono.just(Optional.of(lobbyRepository.save(new Lobby(
                lobby.gameId(),
                lobby.host(),
                newPlayers
        ))));
    }

    public Mono<Optional<Lobby>> updatePlayerState(String gameId, String host, String playerId, PlayerConnectionStatus playerConnectionStatus) {
        final var maybeLobby = lobbyRepository.findById(gameId);
        if (maybeLobby.isEmpty()) {
            return Mono.just(Optional.empty());
        }
        final var lobby = maybeLobby.get();
        if (!StringUtils.equals(host, lobby.host())) {
            return Mono.error(new UnauthorisedException(String.format("host='%s' is not host of gameId='%s'", host, gameId)));
        }
        final var newConnections = lobby.playerConnections().stream()
                .map(playerConnection -> {
                    if (StringUtils.equals(playerId, playerConnection.playerId())) {
                        return new PlayerConnection(playerId, playerConnectionStatus);
                    }
                    return playerConnection;
                }).toList();
        return Mono.just(Optional.of(lobbyRepository.save(new Lobby(gameId, host, newConnections))));
    }
}
