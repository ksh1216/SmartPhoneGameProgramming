package kr.ac.tukorea.ge.spgp2026.suika.game.main

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import kr.ac.tukorea.ge.spgp2026.a2dg.objects.IGameObject
import kr.ac.tukorea.ge.spgp2026.a2dg.view.GameContext

class Fruit(
    val grade: Int // 0 ~ 6
) : IGameObject {

    // 원을 그리기 위한 도구(Paint) 설정
    private val paint = Paint().apply {
        isAntiAlias = true
        color = getGradeColor(grade)
        style = Paint.Style.FILL
    }

    // 테두리용 Paint
    private val strokePaint = Paint().apply {
        isAntiAlias = true
        color = Color.BLACK
        style = Paint.Style.STROKE
        strokeWidth = 5f
    }

    var x = 0f
    var y = 0f
    val radius = 40f + (grade * 30f) // 등급별로 반지름 증가
    val width: Float get() = radius * 2

    private var dy = 0f
    private var dx = 0f
    private val gravity = 1.2f
    private val friction = 0.98f
    private var isPhysicsEnabled = false

    private fun getGradeColor(grade: Int): Int {
        return when (grade) {
            0 -> Color.parseColor("#FFCDD2") // 연분홍 (체리)
            1 -> Color.parseColor("#F48FB1") // 분홍 (딸기)
            2 -> Color.parseColor("#CE93D8") // 보라 (포도)
            3 -> Color.parseColor("#FFE082") // 노랑 (귤)
            4 -> Color.parseColor("#FFAB91") // 주황 (감)
            5 -> Color.parseColor("#EF5350") // 빨강 (사과)
            6 -> Color.parseColor("#4CAF50") // 초록 (수박)
            else -> Color.WHITE
        }
    }

    fun setCenter(cx: Float, cy: Float) {
        x = cx
        y = cy
    }

    fun startPhysics() { isPhysicsEnabled = true }
    fun stopVertical() { dy = 0f }
    fun roll(force: Float) { dx += force * 0.4f }

    override fun update(gctx: GameContext) {
        if (!isPhysicsEnabled) return
        dy += gravity
        x += dx
        y += dy
        dx *= friction
    }

    override fun draw(canvas: Canvas) {
        // 1. 채워진 원 그리기
        canvas.drawCircle(x, y, radius, paint)
        // 2. 테두리 그리기 (구분감 유도)
        canvas.drawCircle(x, y, radius, strokePaint)
    }

    fun isCollidingWith(other: Fruit): Boolean {
        val dx = x - other.x
        val dy = y - other.y
        val distSq = dx * dx + dy * dy
        val rSum = radius + other.radius
        return distSq <= rSum * rSum
    }

    // Sprite를 쓰지 않으므로 syncDstRect는 필요 없지만,
    // 기존 MainScene 로직과의 호환을 위해 빈 함수로 둡니다.
    fun syncDstRect() {}
}