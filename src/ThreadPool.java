
public interface ThreadPool<Job extends Runnable> {
    //执行job
    void execute(Job job);
    //关闭线程池
    void shutdown();
    //增加工作者线程
    void addWorkers(int num);
    //减少工作者线程
    void removeWorker(int num);
    //得到正在执行的任务数量
    int getJobSize();
}
