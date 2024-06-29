package moe.sekiu.minilpa.ui.component

import javax.swing.JProgressBar
import kotlin.math.abs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.swing.Swing
import moe.sekiu.minilpa.logger

class MiniProgressBar : JProgressBar()
{
    private val log = logger()

    var freeze = false

    private var targetValue = 0

    private var lastProgressJob : Job = Job()

    var stage = 1
        set(value)
        {
            step = 1000 / value
            field = value
        }

    private var step = 0

    init
    {
        isStringPainted = true
        maximum = 1000
    }

    fun reset() {
        value = 0
        targetValue = 0
        stage = 1
        freeze = false
    }

    private var count = 0

    suspend fun stageCount(block : suspend () -> Unit)
    {
        count = 0
        block()
        log.info("StageCount: $count")
    }

    fun swipeTo(value : Int) : Job
    {
        count++
        targetValue = value
        lastProgressJob.cancel()
        val job = GlobalScope.launch(Dispatchers.Swing) {
            val value0 = if (value > maximum) maximum else if (value < minimum) minimum else value
            if (value0 == this@MiniProgressBar.value) return@launch
            val totalSteps = abs(value0 - this@MiniProgressBar.value)
            if (totalSteps == 0) return@launch
            val totalDuration = 500L * 10
            val delay = totalDuration / totalSteps
            val step = if (value0 > this@MiniProgressBar.value) 10 else -10
            while (true)
            {
                if (abs(this@MiniProgressBar.value - value0) <= abs(step))
                {
                    this@MiniProgressBar.value = value0
                    break
                } else
                {
                    this@MiniProgressBar.value += step
                    delay(delay)
                }
            }
        }
        lastProgressJob = job
        return job
    }

    fun swipePlus(value : Int) = swipeTo(targetValue + value)

    fun swipePlusAuto() = swipePlus(step)
}