package cn.edaijia.task.container.job;

import akka.actor.Cancellable;
import cn.edaijia.task.container.TaskContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 定时执行任务
 * <li>
 *     <ol>可以在指定时间后触发执行</ol>
 *     <ol>在指定时间后每隔固定时间触发</ol>
 * </li>
 *
 * @author WangJun <wangjuntytl@163.com>
 * @version 1.0 15/6/8
 * @since 1.6
 */

public abstract class TimingJob {

    public static Logger logger = LoggerFactory.getLogger(TimingJob.class);
    private static Map<String, Job> jobContainer = new ConcurrentHashMap<String, Job>();
    private static Map<String, Cancellable> startContainer = new ConcurrentHashMap<String, Cancellable>();


    public static synchronized void putJob(String name, Job job) {
        if (jobContainer.containsKey(name))
            throw new RuntimeException("this job  " + name + " is exist");
        else jobContainer.put(name, job);
    }


    public static boolean contain(String jobName) {
        return jobContainer.containsKey(jobName);
    }


    public static boolean cancel(String name) {
        if (!startContainer.containsKey(name))
            throw new RuntimeException("this job  " + name + " is exist");
        Cancellable cancellable = startContainer.get(name);
        boolean flag = cancellable.cancel();
        if (flag)
            logger.info("cancel job {} suc", name);
        else {
            startContainer.remove(name);
            logger.info("cancel job {} fail", name);
        }
        return flag;
    }

    public static void scheduleOnce(long delay, TimeUnit timeUnit, final String jobName) {
        TaskContainer.getSystem().scheduler().scheduleOnce(Duration.create(delay, timeUnit),
                new Runnable() {
                    @Override
                    public void run() {
                        try {
                            logger.info("start job {}-{}", jobName, System.currentTimeMillis());
                            jobContainer.get(jobName).logic();
                            logger.info("complete job {}-{}", jobName, System.currentTimeMillis());
                        } catch (Exception e) {
                            logger.error("job error ", e);
                        }
                    }
                }, TaskContainer.getSystem().dispatcher());
    }

    public static void scheduleOnce(long delay, TimeUnit timeUnit, final Job job) {
        TaskContainer.getSystem().scheduler().scheduleOnce(Duration.create(delay, timeUnit),
                new Runnable() {
                    @Override
                    public void run() {
                        try {
                            job.logic();
                        } catch (Exception e) {
                            logger.error("job error ", e);
                        }
                    }
                }, TaskContainer.getSystem().dispatcher());
    }

    public static void schedule(FiniteDuration startTime, long delay, TimeUnit timeUnit, final String jobName) {
        if (startContainer.containsKey(jobName))
            throw new RuntimeException("this job  " + jobName + " has start");
        Cancellable cancellable =
                TaskContainer.getSystem().scheduler().schedule(startTime, Duration.create(delay, timeUnit),
                        new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    logger.info("start job {}-{}", jobName, System.currentTimeMillis());
                                    jobContainer.get(jobName).logic();
                                    logger.info("complete job {}-{}", jobName, System.currentTimeMillis());
                                } catch (Exception e) {
                                    logger.error("job error ", e);
                                }
                            }
                        }, TaskContainer.getSystem().dispatcher());
        if (startContainer.containsKey(jobName))
            throw new RuntimeException("this job  " + jobName + " is exist");
        startContainer.put(jobName, cancellable);
    }


}
