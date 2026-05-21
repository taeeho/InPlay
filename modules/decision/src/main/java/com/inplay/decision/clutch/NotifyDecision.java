package com.inplay.decision.clutch;

/**
 * 알림 발사 여부 + 사유. PLAN.md §6 4단계 필터 결과.
 *
 * @param shouldNotify      발사할지 여부
 * @param importance        계산된 importance score
 * @param suppressionReason 차단되었다면 사유 ({@code BELOW_THRESHOLD} / {@code COOLDOWN} /
 *                          {@code DUPLICATE} / {@code MUTED}). 발사면 null.
 */
public record NotifyDecision(
        boolean shouldNotify,
        ImportanceScore importance,
        String suppressionReason
) {
    public static NotifyDecision allow(ImportanceScore importance) {
        return new NotifyDecision(true, importance, null);
    }

    public static NotifyDecision suppress(ImportanceScore importance, String reason) {
        return new NotifyDecision(false, importance, reason);
    }
}
