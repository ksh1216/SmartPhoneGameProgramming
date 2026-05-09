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
        color = Color.DKGRAY
        style = Paint.Style.STROKE
        strokeWidth = 10f
    }

    // 화면 경계 값 (900x1600 가상 해상도 기준)
    private val gameWidth = 900f
    private val gameHeight = 1600f
    private val groundY = 1500f // 바닥 높이

    init {
        prepareNextFruit()
    }

    private fun prepareNextFruit() {
        // 0~2단계 사이의 과일 랜덤 생성
        val grade = (0..2).random()
        val fruit = Fruit(grade)
        fruit.setCenter(gameWidth / 2, 150f) // 상단 대기 위치
        currentFruit = fruit
        world.add(fruit, Layer.TOP_FRUIT)
    }

    override fun update(gctx: GameContext) {
        super.update(gctx)

        solvePhysics() // 물리 연산 (밀어내기 및 벽 충돌)
        checkMerge()    // 합성 체크
    }

    // MainScene.kt 내부의 solvePhysics() 함수 전체 교체

    private fun solvePhysics() {
        val fruits = world.objectsAt(Layer.FRUIT).filterIsInstance<Fruit>()

        for (i in fruits.indices) {
            val f1 = fruits[i]

            // 1. 벽 및 바닥 충돌 처리 (동일)
            if (f1.x - f1.radius < 0) { f1.x = f1.radius; f1.dx *= -0.5f } // 벽에 닿으면 살짝 튕김
            if (f1.x + f1.radius > gameWidth) { f1.x = gameWidth - f1.radius; f1.dx *= -0.5f }

            if (f1.y + f1.radius > groundY) {
                f1.y = groundY - f1.radius
                f1.stopVertical()
                f1.dx *= 0.8f // 바닥 마찰력
            }

            // 2. 과일 간 밀어내기 및 굴러떨어지기 로직
            for (j in i + 1 until fruits.size) {
                val f2 = fruits[j]
                val distX = f2.x - f1.x
                val distY = f2.y - f1.y
                val dist = sqrt(distX * distX + distY * distY)
                val minDist = f1.radius + f2.radius

                if (dist < minDist && dist > 0) {
                    // [해결법 A] 겹침 즉시 해소 (파고들기 방지)
                    val overlap = minDist - dist
                    val nx = distX / dist
                    val ny = distY / dist

                    f1.x -= nx * overlap * 0.5f
                    f1.y -= ny * overlap * 0.5f
                    f2.x += nx * overlap * 0.5f
                    f2.y += ny * overlap * 0.5f

                    // [해결법 B] 수직 낙하 힘을 가로 미끄러짐으로 변환
                    // 누가 위에 있는지 판별
                    val top = if (f1.y < f2.y) f1 else f2
                    val bottom = if (f1.y < f2.y) f2 else f1

                    // 중심축을 기준으로 위쪽 과일이 어느 쪽에 치우쳐 있는지 확인 (좌/우 방향)
                    val slideDir = if (top.x < bottom.x) -1f else 1f

                    // 위쪽 과일이 아래로 떨어지려는 힘(dy)이 있다면, 이를 가로 힘(dx)으로 전환
                    if (top.dy > 0) {
                        val slideForce = top.dy * 0.3f // 떨어지는 힘의 30%를 가로 구르기에 사용
                        top.dx += slideDir * slideForce

                        // 아래 과일에 부딪혔으므로 수직 낙하 속도 크게 감소 (끈적임 원인 제거)
                        top.dy *= 0.4f
                    }

                    // 두 과일 사이의 기본적인 반발력 (살짝 밀어냄)
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
                    // 동일 등급 충돌 시 합성 (최대 6단계)
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
                    return // 리스트 변형 보호를 위해 즉시 종료
                }
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                currentFruit?.let {
                    // 벽 안쪽에서만 움직이도록 제한
                    it.x = event.x.coerceIn(it.radius, gameWidth - it.radius)
                }
            }
            MotionEvent.ACTION_UP -> {
                currentFruit?.let {
                    world.remove(it, Layer.TOP_FRUIT)
                    it.startPhysics()
                    world.add(it, Layer.FRUIT)
                    currentFruit = null

                    // 1초 뒤 다음 과일 생성
                    gctx.view.postDelayed({ prepareNextFruit() }, 1000)
                }
            }
        }
        return true
    }

    override fun draw(canvas: Canvas) {
        // 배경 상자 그리기 (시각적 가이드)
        canvas.drawRect(0f, 0f, gameWidth, groundY, wallPaint)

        super.draw(canvas) // World의 레이어 순서대로 그리기
    }

    override fun touchObjects(): List<IGameObject>? = null
}