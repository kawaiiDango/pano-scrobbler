package com.arn.scrobble

import android.app.ActivityManager
import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Process

class KeepNLSAliveJob: JobService() {

    override fun onStartJob(jp: JobParameters): Boolean {
        ensureServiceRunning(applicationContext)
        return false
    }

    override fun onStopJob(params: JobParameters): Boolean {
        return true
    }

    companion object {
        fun ensureServiceRunning(context: Context):Boolean {
            val serviceComponent = ComponentName(context, NLService::class.java)
            val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            var serviceRunning = false
            val runningServices = manager.getRunningServices(Integer.MAX_VALUE)
            if (runningServices == null) {
                Stuff.log("ensureServiceRunning() runningServices is NULL")
                return true //just assume true for now. this throws SecurityException, might not work in future
            }
            for (service in runningServices) {
                if (service.service == serviceComponent) {
                    Stuff.log("ensureServiceRunning service - pid: " + service.pid + ", currentPID: " +
                            Process.myPid() + ", clientPackage: " + service.clientPackage + ", clientCount: " +
                            service.clientCount + " process:" + service.process + ", clientLabel: " +
                            if (service.clientLabel == 0) "0" else "(" + context.resources.getString(service.clientLabel) + ")")
                    if (service.process == BuildConfig.APPLICATION_ID + ":bgScrobbler" /*&& service.clientCount > 0 */) {
                        serviceRunning = true
                        break
                    }
                }
            }
            if (serviceRunning)
                return true

            Stuff.log("ensureServiceRunning: service not running, reviving...")
            toggleNLS(context)
            return false
        }

        private fun toggleNLS(context: Context) {
            val thisComponent = ComponentName(context, NLService::class.java)
            val pm = context.packageManager
            pm.setComponentEnabledSetting(thisComponent, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP)
            pm.setComponentEnabledSetting(thisComponent, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP)
        }


        const val JOB_ID = 8

        fun checkAndSchedule(context: Context) {
            val js = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
            val jobs = js.allPendingJobs ?: listOf()

            if (jobs.any { it.id == JOB_ID })
                return

            val job = JobInfo.Builder(JOB_ID, ComponentName(context, KeepNLSAliveJob::class.java))
                    .setPeriodic(Stuff.KEEPALIVE_JOB_INTERVAL)
                    .setPersisted(true)
                    .build()
            js.schedule(job)
            Stuff.log("scheduling KeepNLSAliveJob")
        }
    }
}