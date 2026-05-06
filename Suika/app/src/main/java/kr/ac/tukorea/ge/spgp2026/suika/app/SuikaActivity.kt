package kr.ac.tukorea.ge.spgp2026.suika.app

import kr.ac.tukorea.ge.spgp2026.a2dg.activity.BaseGameActivity
import kr.ac.tukorea.ge.spgp2026.a2dg.scene.Scene
import kr.ac.tukorea.ge.spgp2026.a2dg.view.GameContext
import kr.ac.tukorea.ge.spgp2026.suika.BuildConfig
import kr.ac.tukorea.ge.spgp2026.suika.game.main.MainScene

class SuikaActivity : BaseGameActivity() {
    override val drawsDebugGrid: Boolean = BuildConfig.DEBUG
    override val drawsDebugInfo: Boolean = BuildConfig.DEBUG
    override val drawsFpsGraph: Boolean = BuildConfig.DEBUG

    override fun createRootScene(gctx: GameContext): Scene {
        gctx.metrics.setSize(900f, 1600f)
        val stage = intent.getIntExtra(KEY_STAGE, 1)
        return MainScene(gctx)
    }

    companion object {
        const val KEY_STAGE = "stage"
    }
}
