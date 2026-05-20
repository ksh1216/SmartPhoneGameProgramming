package kr.ac.tukorea.ge.spgp2026.suika.game.main

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.MotionEvent
import kr.ac.tukorea.ge.spgp2026.a2dg.objects.IGameObject
import kr.ac.tukorea.ge.spgp2026.a2dg.scene.Scene
import kr.ac.tukorea.ge.spgp2026.a2dg.scene.World
import kr.ac.tukorea.ge.spgp2026.a2dg.view.GameContext
import kotlin.math.sqrt

class MainScene(gctx: GameContext) : Scene(gctx) {

    // 1. 레이어 정의
    enum class Layer {
        BACKGROUND, FRUIT, TOP_FRUIT, UI, COUNT
    }

    override val world = World(Layer.entries.toTypedArray())

    private var currentFruit: Fruit? = null
    private val wallPaint = Paint().apply {
        color = Color.BLACK
        style = Paint.Style.STROKE
        strokeWidth = 10f
    }

    // 화면 전체 크기 및 실제 플레이 박스 경계 정의
    private val gameWidth = 900f
    private val gameHeight = 1600f

    private val playBoxTop = 250f     // UI 공간 경계선
    private val playBoxLeft = 20f     // 좌측 벽 여백
    private val playBoxRight = 880f   // 우측 벽 여백
    private val groundY = 1550f       // 바닥 높이

    init {
        prepareNextFruit()
    }

    private fun prepareNextFruit() {
        // 0~2단계 사이의 과일 랜덤 생성
        val grade = (0..2).random()
        val fruit = Fruit(grade)

        // [수정] 과일의 중앙(Center Y)이 검정 사각형 상단 선(playBoxTop = 250f)에 정확히 일치하도록 설정
        fruit.setCenter(gameWidth / 2, playBoxTop)
        currentFruit = fruit
        world.add(fruit, Layer.TOP_FRUIT)
    }

    override fun update(gctx: GameContext) {
        super.update(gctx)

        solvePhysics() // 물리 연산 (밀어내기 및 벽 충돌)
        checkMerge()    // 합성 체크
    }

    private fun solvePhysics() {
        val fruits = world.objectsAt(Layer.FRUIT).filterIsInstance<Fruit>()

        for (i in fruits.indices) {
            val f1 = fruits[i]

            // 1. 플레이 박스 사방 벽 및 바닥 충돌 처리
            if (f1.x - f1.radius < playBoxLeft) {
                f1.x = playBoxLeft + f1.radius
                f1.dx *= -0.5f
            }
            if (f1.x + f1.radius > playBoxRight) {
                f1.x = playBoxRight - f1.radius
                f1.dx *= -0.5f
            }

            if (f1.y + f1.radius > groundY) {
                f1.y = groundY - f1.radius
                f1.stopVertical()
                f1.dx *= 0.8f // 바닥 마찰력
            }

            // 과일이 위쪽 UI 영역으로 튀어 올라가는 것을 방지
            if (f1.y - f1.radius < playBoxTop && f1.dy < 0) {
                f1.y = playBoxTop + f1.radius
                f1.dy *= -0.2f
            }

            // 2. 과일 간 밀어내기 및 굴러떨어지기 로직
            for (j in i + 1 until fruits.size) {
                val f2 = fruits[j]
                val distX = f2.x - f1.x
                val distY = f2.y - f1.y
                val dist = sqrt(distX * distX + distY * distY)
                val minDist = f1.radius + f2.radius

                if (dist < minDist && dist > 0) {
                    val overlap = minDist - dist
                    val nx = distX / dist
                    val ny = distY / dist

                    f1.x -= nx * overlap * 0.5f
                    f1.y -= ny * overlap * 0.5f
                    f2.x += nx * overlap * 0.5f
                    f2.y += ny * overlap * 0.5f

                    val top = if (f1.y < f2.y) f1 else f2
                    val bottom = if (f1.y < f2.y) f2 else f1
                    val slideDir = if (top.x < bottom.x) -1f else 1f

                    if (top.dy > 0) {
                        val slideForce = top.dy * 0.3f
                        top.dx += slideDir * slideForce
                        top.dy *= 0.4f
                    }

                    f1.dx -= nx * 0.5f
                    f2.dx += nx * 0.5f
                }
            }
        }
    }

    private fun checkMerge() {
        val fruits = world.objectsAt(Layer.FRUIT).filterIsInstance<Fruit>()
        for (i in fruits.indices) {
            for (j in i + 1 until fruits.size) {
                val f1 = fruits[i]
                val f2 = fruits[j]

                if (f1.grade == f2.grade && f1.isCollidingWith(f2)) {
                    if (f1.grade < 6) {
                        val nextGrade = f1.grade + 1
                        val spawnX = (f1.x + f2.x) / 2
                        val spawnY = (f1.y + f2.y) / 2

                        world.remove(f1, Layer.FRUIT)
                        world.remove(f2, Layer.FRUIT)

                        val newFruit = Fruit(nextGrade)
                        newFruit.setCenter(spawnX, spawnY)
                        newFruit.startPhysics()
                        world.add(newFruit, Layer.FRUIT)
                    }
                    return
                }
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                currentFruit?.let {
                    // 좌우 벽 경계 제한
                    it.x = event.x.coerceIn(playBoxLeft + it.radius, playBoxRight - it.radius)
                    // [추가] 드래그하는 도중 터치 오차로 Y축이 흔들리지 않도록 검정 선(250f)에 완전히 고정
                    it.y = playBoxTop
                }
            }
            MotionEvent.ACTION_UP -> {
                currentFruit?.let {
                    world.remove(it, Layer.TOP_FRUIT)
                    it.startPhysics()
                    world.add(it, Layer.FRUIT)
                    currentFruit = null

                    gctx.view.postDelayed({ prepareNextFruit() }, 1000)
                }
            }
        }
        return true
    }

    override fun draw(canvas: Canvas) {
        // 검정색 사각형 플레이 상자 그리기
        canvas.drawRect(playBoxLeft, playBoxTop, playBoxRight, groundY, wallPaint)

        super.draw(canvas)
    }

    override fun touchObjects(): List<IGameObject>? = null
}