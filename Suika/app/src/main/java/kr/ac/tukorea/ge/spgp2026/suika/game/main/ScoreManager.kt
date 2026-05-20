package kr.ac.tukorea.ge.spgp2026.suika.game.main

import android.content.Context
import android.content.SharedPreferences

class ScoreManager(context: Context) {

    // 최고 점수를 기기에 영구 저장하기 위한 SharedPreferences 설정
    private val prefs: SharedPreferences = context.getSharedPreferences("suika_score_prefs", Context.MODE_PRIVATE)

    // 현재 게임 점수 (외부에서는 읽기만 가능)
    var currentScore: Int = 0
        private set

    // 역대 최고 점수 (초기화 시 기존 저장된 점수를 불러옴)
    var bestScore: Int = prefs.getInt("key_best_score", 0)
        private set

    /**
     * 과일이 합성되었을 때 호출하는 함수
     * @param nextGrade 새롭게 만들어진 과일의 등급 (0부터 시작)
     */
    fun addScoreForMerge(nextGrade: Int) {
        // 등급별 점수 부여 규칙 (0단계 합쳐져서 1단계가 되었을 때 등)
        val scoreGain = when (nextGrade) {
            1 -> 10
            2 -> 20
            3 -> 40
            4 -> 100
            5 -> 200
            6 -> 350
            7 -> 500 // 최대 등급 수박 달성 시
            else -> 10 // 예외 방지용 기본값
        }

        currentScore += scoreGain

        // 최고 점수 갱신 및 기기 저장을 동시에 수행
        if (currentScore > bestScore) {
            bestScore = currentScore
            prefs.edit().putInt("key_best_score", bestScore).apply()
        }
    }

    /**
     * 새로운 게임을 시작할 때 현재 점수를 초기화하는 함수
     */
    fun resetCurrentScore() {
        currentScore = 0
    }

    /**
     * [추후 확장용] 점수를 숫자 이미지로 그리기 쉽도록 각 자릿수를 리스트로 변환하는 함수
     * 예: 1230 -> [1, 2, 3, 0] 반환
     */
    fun getScoreDigits(score: Int): List<Int> {
        if (score == 0) return listOf(0)
        val digits = mutableListOf<Int>()
        var temp = score
        while (temp > 0) {
            digits.add(0, temp % 10) // 일의 자리부터 추출하여 리스트 앞에 삽입
            temp /= 10
        }
        return digits
    }
}