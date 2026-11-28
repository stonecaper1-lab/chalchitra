package io.chalchitra

import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent

object Auto {
    /** Picks one new wallpaper from the enabled sources and applies it. */
    fun runOnce(ctx: Context): Boolean {
        val pr = Prefs(ctx)
        Feed.pixabayKey = pr.pixabayKey
        val srcs = pr.sources.map { Sources.byId(it) }
            .filter { pr.showReddit || it.kind != Kind.REDDIT }
            .filter { pr.pixabayKey.isNotBlank() || it.kind != Kind.PIXABAY }
            .ifEmpty { listOf(Sources.all[0]) }
            .shuffled()
        for (s in srcs) {
            try {
                val cursor = Feed.startCursor(s)
                val items = Feed.load(s, Sort.WEEK, cursor).items
                if (items.isEmpty()) continue
                val pick = items.filter { !pr.isSeen(it.id) }.ifEmpty { items }.random()
                val bmp = Engine.render(ctx, pick.full)
                try {
                    Engine.apply(ctx, bmp, pr.autoTarget, pr.dimOn, pr.dimStrength)
                } finally {
                    bmp.recycle()
                }
                pr.markSeen(pick.id)
                Engine.clearCache(ctx)
                return true
            } catch (_: Exception) {
                // try the next source
            }
        }
        return false
    }

    /** Re-creates the job if the system dropped it (app update, etc). */
    fun ensure(ctx: Context) {
        val pr = Prefs(ctx)
        if (!pr.autoOn) return
        val js = ctx.getSystemService(JobScheduler::class.java)
        if (js.getPendingJob(AutoJob.ID) == null) AutoJob.schedule(ctx, pr.interval)
    }
}

class AutoJob : JobService() {
    @Volatile
    private var worker: Thread? = null

    override fun onStartJob(p: JobParameters): Boolean {
        worker = Thread {
            val ok = try {
                Auto.runOnce(applicationContext)
            } catch (_: Throwable) {
                false
            }
            jobFinished(p, !ok)
        }.also { it.start() }
        return true
    }

    override fun onStopJob(p: JobParameters): Boolean {
        worker?.interrupt()
        return true
    }

    companion object {
        const val ID = 1

        /** Returns false instead of crashing if the system refuses the job. */
        fun schedule(ctx: Context, minutes: Int): Boolean = try {
            val info = JobInfo.Builder(ID, ComponentName(ctx, AutoJob::class.java))
                .setPeriodic(minutes * 60_000L)
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setRequiresBatteryNotLow(true)
                .setPersisted(true)
                .build()
            ctx.getSystemService(JobScheduler::class.java).schedule(info) == JobScheduler.RESULT_SUCCESS
        } catch (_: Exception) {
            false
        }

        fun cancel(ctx: Context) {
            ctx.getSystemService(JobScheduler::class.java).cancel(ID)
        }
    }
}

class Boot : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        Auto.ensure(ctx)
    }
}
