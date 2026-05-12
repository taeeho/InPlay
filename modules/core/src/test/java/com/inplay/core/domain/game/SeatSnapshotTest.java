package com.inplay.core.domain.game;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class SeatSnapshotTest {

    private static final Inning FIFTH_BOTTOM = new Inning(5, InningHalf.BOTTOM);

    @Test
    void validSnapshotAccepted() {
        SeatSnapshot snap = new SeatSnapshot(FIFTH_BOTTOM, 2, true, true, true, new Count(3, 2));
        assertThat(snap.basesLoaded()).isTrue();
        assertThat(snap.runnersOnBase()).isEqualTo(3);
        assertThat(snap.outs()).isEqualTo(2);
    }

    @Test
    void emptyBasesGivesZeroRunners() {
        SeatSnapshot snap = new SeatSnapshot(FIFTH_BOTTOM, 0, false, false, false, Count.fresh());
        assertThat(snap.basesLoaded()).isFalse();
        assertThat(snap.runnersOnBase()).isZero();
    }

    @Test
    void negativeOutsRejected() {
        assertThatThrownBy(
                () -> new SeatSnapshot(FIFTH_BOTTOM, -1, false, false, false, Count.fresh()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void threeOutsRejected() {
        assertThatThrownBy(
                () -> new SeatSnapshot(FIFTH_BOTTOM, 3, false, false, false, Count.fresh()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void nullInningRejected() {
        assertThatThrownBy(() -> new SeatSnapshot(null, 0, false, false, false, Count.fresh()))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void nullCountRejected() {
        assertThatThrownBy(() -> new SeatSnapshot(FIFTH_BOTTOM, 0, false, false, false, null))
                .isInstanceOf(NullPointerException.class);
    }
}
