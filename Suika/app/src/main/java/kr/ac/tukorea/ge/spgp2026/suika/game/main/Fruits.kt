package kr.ac.tukorea.ge.spgp2026.suika.game.main

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import kr.ac.tukorea.ge.spgp2026.a2dg.objects.IGameObject
import kr.ac.tukorea.ge.spgp2026.a2dg.view.GameContext

class Fruit(
    val grade: Int
) : IGameObject {

    private val paint = Paint().apply {
        isAntiAlias = true
        color = getGradeColor(grade)
        style = Paint.Style.FILL
    }
    private val strokePaint = Paint().apply {
        isAntiAlias = true
        color = Color.BLACK
        style = Paint.Style.STROKE
        strokeWidth = 5f
    }

    var x = 0f
    var y = 0f
    val radius = 40f + (grade * 30f)

    // 외부(MainScene)에서 충돌 시 속도를 제어할 수 있도록 public으로 변경
    var dx = 0f
    var dy = 0f

    private val gravity = 0.8f   // 기존 1.2f에서 감소시켜 부드러운 낙하 유도
    private val friction = 0.96f // 마찰력을 살짝 줄여 더 잘 미끄러지게 함
    private var isPhysicsEnabled = false

    private fun getGradeColor(grade: Int): Int {
        return when (grade) {
            0 -> Color.parseColor("#FFCDD2")
            1 -> Color.parseColor("#F48FB1")
            2 -> Color.parseColor("#CE93D8")
            3 -> Color.parseColor("#FFE082")
            4 -> Color.parseColor("#FFAB91")
            5 -> Color.parseColor("#EF5350")
            6 -> Color.parseColor("#4CAF50")
            else -> Color.WHITE
        }
    }

    fun setCenter(cx: Float, cy: Float) {
        x = cx
        y = cy
    }

    fun startPhysics() { isPhysicsEnabled = true }
    fun stopVertical() { dy = 0f }

    override fun update(gctx: GameContext) {
        if (!isPhysicsEnabled) return

        dy += gravity
        x += dx
        y += dy

        // 바닥이나 벽에 닿아있을 때의 감속 처리
        dx *= friction
    }

    override fun draw(canvas: Canvas) {
        canvas.drawCircle(x, y, radius, paint)
        canvas.drawCircle(x, y, radius, strokePaint)
    }

    fun isCollidingWith(other: Fruit): Boolean {
        val dx = x - other.x
        val dy = y - other.y
        return (dx * dx + dy * dy) <= (radius + other.radius) * (radius + other.radius)
    }
}