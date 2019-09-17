import javafx.concurrent.Worker;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class DefaultThreadPool<Job extends Runnable> implements ThreadPool<Job> {

    //线程池最大工作者线程数
    private static final int MAX_WORKER_NUMBERS = 10;
    //线程池默认工作者线程数
    private static final int DEFAULT_WORKER_NUMBERS = 5;
    //线程池最小数量
    private static final int MIN_WORKERS_NUMBERS = 1;

    private int workerNum = DEFAULT_WORKER_NUMBERS;

    //任务队列
    private final LinkedList<Job> jobs = new LinkedList<Job>();
    //工作者列表
    private final List<Worker> workers = Collections.synchronizedList(new ArrayList<Worker>());

    //线程编号
    private AtomicLong threadNumber = new AtomicLong();

    public DefaultThreadPool(int num){
        if(num > MAX_WORKER_NUMBERS){
            workerNum = MAX_WORKER_NUMBERS;
        }else if(num < MIN_WORKERS_NUMBERS){
            workerNum = MIN_WORKERS_NUMBERS;
        }
        initializeWorkers(workerNum);
    }

    //初始化工作者线程
    private void initializeWorkers(int num){
        for(int i = 0; i < num; i++){
            Worker worker = new Worker();
            workers.add(worker);
            Thread thread = new Thread(worker, "threadpool-worker-" + threadNumber.incrementAndGet());
            thread.start();
        }

    }

    //工作者，负责消费任务
    class Worker implements Runnable {
        private volatile boolean running = true;

        @Override
        public void run() {
            while (running){
                Job job = null;
                synchronized (jobs){
                    while (jobs.isEmpty()){
                        try {
                            jobs.wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                            Thread.currentThread().interrupt();
                            return;
                        }
                    }

                    job = jobs.removeFirst();
                }

                if(job != null){
                    job.run();
                }
            }

        }

        public void shutdown(){
            running = false;
        }
    }


    @Override
    public void execute(Job job) {
        if(job != null){
            synchronized (jobs){
                jobs.addLast(job);
                jobs.notify();
            }
        }

    }

    @Override
    public void shutdown() {

        for(Worker worker : workers){
            worker.shutdown();
        }
    }

    @Override
    public void addWorkers(int num) {
        synchronized (jobs){
            if(num + this.workerNum > MAX_WORKER_NUMBERS){
                num = MAX_WORKER_NUMBERS - this.workerNum;
            }
            initializeWorkers(num);
            this.workerNum += num;
        }

    }

    @Override
    public void removeWorker(int num) {
        synchronized (jobs){
            if(num >= this.workerNum){
                throw new IllegalArgumentException("beyond worker number");
            }

            int count = 0;
            while(count < num){
                Worker worker = workers.get(count);
                if(workers.remove(worker)){
                    count++;
                }
            }

            this.workerNum -= count;
        }

    }

    @Override
    public int getJobSize() {
        return jobs.size();
    }
}
