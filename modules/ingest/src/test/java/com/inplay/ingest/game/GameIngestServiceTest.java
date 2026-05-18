package com.inplay.ingest.game;

import static org.assertj.core.api.Assertions.assertThat;

import com.inplay.core.domain.game.Game;
import com.inplay.core.domain.game.GameStatus;
import com.inplay.core.domain.game.Score;
import com.inplay.core.domain.id.GameId;
import com.inplay.core.domain.team.KboTeam;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GameIngestServiceTest {

    @Mock GameRepository repository;

    private static Game game(String id, int home, int away, GameStatus status) {
        return new Game(
                new GameId(id),
                LocalDate.of(2026, 5, 12),
                KboTeam.HH,
                KboTeam.LG,
                status,
                new Score(home, away));
    }

    @Test
    void insertWhenGameIdNotFound() {
        var service = new GameIngestService(repository);
        Mockito.when(repository.findByGameId("g1")).thenReturn(Optional.empty());
        Mockito.when(repository.save(Mockito.any(GameDocument.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        var saved = service.upsert(game("g1", 0, 0, GameStatus.SCHEDULED));
        assertThat(saved.id()).isNull();
        assertThat(saved.gameId()).isEqualTo("g1");

        ArgumentCaptor<GameDocument> captor = ArgumentCaptor.forClass(GameDocument.class);
        Mockito.verify(repository).save(captor.capture());
        assertThat(captor.getValue().id()).isNull();
    }

    @Test
    void updatePreservesExistingMongoId() {
        var service = new GameIngestService(repository);
        var existing = GameMapper.toDocument(game("g1", 0, 0, GameStatus.SCHEDULED)).withId("oid-1");
        Mockito.when(repository.findByGameId("g1")).thenReturn(Optional.of(existing));
        Mockito.when(repository.save(Mockito.any(GameDocument.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        var updated = service.upsert(game("g1", 4, 3, GameStatus.FINAL));
        assertThat(updated.id()).isEqualTo("oid-1");
        assertThat(updated.status()).isEqualTo("FINAL");
        assertThat(updated.score().home()).isEqualTo(4);
    }

    @Test
    void upsertAllReturnsCount() {
        var service = new GameIngestService(repository);
        Mockito.when(repository.findByGameId(Mockito.anyString())).thenReturn(Optional.empty());
        Mockito.when(repository.save(Mockito.any(GameDocument.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        int n = service.upsertAll(List.of(
                game("g1", 0, 0, GameStatus.SCHEDULED),
                game("g2", 0, 0, GameStatus.SCHEDULED)));
        assertThat(n).isEqualTo(2);
        Mockito.verify(repository, Mockito.times(2)).save(Mockito.any(GameDocument.class));
    }

    @Test
    void emptyListReturnsZero() {
        var service = new GameIngestService(repository);
        assertThat(service.upsertAll(List.of())).isZero();
        Mockito.verifyNoInteractions(repository);
    }
}
