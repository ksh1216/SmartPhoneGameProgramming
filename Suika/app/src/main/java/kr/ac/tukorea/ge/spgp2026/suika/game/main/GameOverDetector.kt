package kr.ac.tukorea.ge.spgp2026.suika.game.main

class GameOverDetector {

    // 현재 게임의 오버 여부 상태
    var isGameOver: Boolean = false
        private set

    // 과일이 선을 넘었을 때 누적되는 타이머 (초 단위)
    private var overflowTimer = 0f

    // 허용 시간 한계치 (2초)
    private val timeThreshold = 2.0f

    /**
     * 매 프레임마다 과일들의 위치를 넘겨받아 게임오버 상태를 업데이트하는 함수
     * @param fruits 현재 월드에 존재하는 과일 리스트
     * @param deadLineY 기준선 높이 (playBoxTop)
     * @param frameTime 현재 프레임의 경과 시간
     */
    fun update(fruits: List<Fruit>, deadLineY: Float, frameTime: Float) {
        // 게임이 이미 끝난 상태라면 추가 연산을 하지 않음
        if (isGameOver) return

        // 과일 중 하나라도 중심점이 기준선(deadLineY)보다 위로 올라갔는지 검사
        val isOverflowing = fruits.any { fruit -> fruit.y < deadLineY }

        if (isOverflowing) {
            overflowTimer += frameTime
            if (overflowTimer >= timeThreshold) {
                isGameOver = true
            }
        } else {
            // 과일이 다시 선 아래로 내려가면 타이머를 즉시 초기화 (일시적 도약 허용)
            overflowTimer = 0f
        }
    }

    /**
     * 게임을 다시 시작하거나 리셋할 때 상태를 초기화하는 함수
     */
    fun reset() {
        isGameOver = false
        overflowTimer = 0f
    }
}