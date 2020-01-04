package net.time4tea.oidn

import kotlin.math.sqrt

/*
    https://www.johndcook.com/blog/standard_deviation/
 */
class RunningStat {
    private var n = 0
    private var oldM = 0.0
    private var newM = 0.0
    private var oldS = 0.0
    private var newS = 0.0

    fun push(x: Double) {
        n++

        // See Knuth TAOCP vol 2, 3rd edition, page 232
        if (n == 1) {
            oldM = x
            newM = x
            oldS = 0.0
        } else {
            newM = oldM + (x - oldM) / n
            newS = oldS + (x - oldM) * (x - newM)

            // set up for next iteration
            oldM = newM
            oldS = newS
        }
    }

    fun count(): Int {
        return n
    }

    fun mean(): Double {
        return if (n > 0) newM else 0.0
    }

    fun variance(): Double {
        return if (n > 1) newS / (n - 1) else 0.0
    }

    fun stddev(): Double {
        return sqrt(variance())
    }
}