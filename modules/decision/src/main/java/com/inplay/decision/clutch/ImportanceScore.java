package com.inplay.decision.clutch;

/**
 * Clutch 알림 우선순위. PLAN.md §6 formula 매핑.
 *
 * @param value                  0~10 정규화 점수. notify policy threshold 입력.
 * @param wpaContribution        |ΔWE| ∈ [0,1]. 변동 클수록 ↑.
 * @param rivalryWeight          팀 라이벌리 보너스 ∈ [0.5, 1.5] 정도.
 * @param leverageContribution   leverage_proxy / 1.5 정규화 ∈ [0,1].
 */
public record ImportanceScore(
        double value,
        double wpaContribution,
        double rivalryWeight,
        double leverageContribution
) {
    public static ImportanceScore zero() {
        return new ImportanceScore(0.0, 0.0, 1.0, 0.0);
    }
}
