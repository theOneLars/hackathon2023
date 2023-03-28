package ch.zuehlke.fullstack.hackathon.model;

import ch.zuehlke.common.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.*;

import static ch.zuehlke.common.GameStatus.CREATED;

@RequiredArgsConstructor
@Getter
public class Game {

    public static final int REQUIRED_NUMBER_OF_PLAYERS = 2;
    public static final int MIN_PLAYERS = 2;

    private Map<String, Board> boardsByPlayerId = new HashMap<>();
    private final String gameId = UUID.randomUUID().toString();
    private final List<Player> players = new ArrayList<>();

    private GameStatus status = CREATED;

    private final GameState state = new GameState();

    private List<Round> rounds = new ArrayList<>();

    public boolean addPlayer(Player player) {
        if (status != CREATED) {
            throw new IllegalArgumentException("cannot add players in current phase: " + status);
        }
        if (players.size() > REQUIRED_NUMBER_OF_PLAYERS) {
            throw new IllegalArgumentException("A game cannot have more than " + REQUIRED_NUMBER_OF_PLAYERS + " players");
        }
        players.add(player);
        if (players.size() == REQUIRED_NUMBER_OF_PLAYERS) {
            status = GameStatus.PLACE_SHIPS;
        }
        return true;
    }

    public void placeShips(Player player, List<Ship> ships) {

        // ToDo: validate player token
        var board = new Board(ships);

        if (!board.shipsValid()) {
            throw new IllegalArgumentException("Board is not valid");
        }

        boardsByPlayerId.put(player.getId(), board);

        if (boardsByPlayerId.size() == REQUIRED_NUMBER_OF_PLAYERS) {
            this.status = GameStatus.SHOOT;
        }
    }

    public void shoot(Player player, int x, int y) {

        var currentRound = getCurrentRound();

        if (!currentRound.isPlayerAllowedToShoot(player.getId())) {
            throw new IllegalArgumentException("Player did already shoot this round!");
        }

        var shoot = new Shoot(player.getId(), x, y);
        currentRound.addShoot(shoot);

        if (currentRound.receivedBothMoves()) {
            currentRound.getShoots().forEach(it -> executeShoot(it.playerId(), x, y));
            currentRound.finishRound();
        }

        // finish game, if one player wins


        if (state.currentRequests().isEmpty()) {
            finishRound();
        }
    }

    private void executeShoot(String playerId, int x, int y) {
        Player enemy = players.stream().filter(p -> !p.getId().equals(playerId)).findFirst().orElseThrow(() -> new RuntimeException("No enemy found"));
        Board enemyBoard = boardsByPlayerId.get(enemy.getId());
        enemyBoard.executeShot(x, y);
    }

    public boolean isPlayerAllowedToShoot(Player player) {
        return getCurrentRound().isPlayerAllowedToShoot(player.getId());
    }

    public boolean canStartGame() {
        return players.size() == 2 &&
                status == CREATED;
    }

    public void startGame() {
        if (!canStartGame()) {
            return;
        }
        status = GameStatus.PLACE_SHIPS;
    }

    private void startRound() {
        rounds = new ArrayList<>();
        //players.forEach(player -> state.currentRequests().add(new PlayRequest(player.id())));
    }

    public void finishGame() {
        status = GameStatus.FINISHED;
    }

    public void deleteGame() {
        status = GameStatus.DELETED;
    }

    private void finishRound() {
        Round currentRound = getCurrentRound();
        state.rounds().add(currentRound);

        if (getWinner().isPresent()) {
            finishGame();
        }
    }

    public Optional<PlayerId> getWinner() {
        if (status != GameStatus.FINISHED || state.rounds().isEmpty()) {
            return Optional.empty();
        }
        // Improve: Handle multiple rounds
        return Optional.empty();
    }

    public Player getPlayerById(String playerId) {
        return players.stream().filter(player -> player.getId().equals(playerId)).findFirst().orElseThrow(() -> new RuntimeException("Player not found"));
    }

    private Round getCurrentRound() {
        if (rounds.isEmpty()) {
            var round = new Round();
            rounds.add(round);
            return round;
        }
        return rounds.get(rounds.size() - 1);
    }
}
