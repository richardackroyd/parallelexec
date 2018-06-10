import com.sun.deploy.util.SessionState;
import org.apache.tomcat.jni.Time;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.CountDownLatch;

public class execservc {

        public static void main (String[] args) {
            ApplicationContext context =
                    new AnnotationConfigApplicationContext(MyConfig.class);
            MyBean bean = context.getBean(MyBean.class);
            int processLoops = 10000;
            bean.runTasks(processLoops);
            ThreadPoolTaskExecutor t = context.getBean(ThreadPoolTaskExecutor.class);
            t.shutdown();
        }

        @Configuration
        public static class MyConfig {

            @Bean
            MyBean myBean () {
                return new MyBean();
            }

            @Bean
            ThreadPoolTaskExecutor taskExecutor () {
                ThreadPoolTaskExecutor t = new ThreadPoolTaskExecutor();
                t.setCorePoolSize(10);
                t.setMaxPoolSize(100);
//xxxxxxxx                t.setQueueCapacity(60);
                t.setAllowCoreThreadTimeOut(true);
                t.setKeepAliveSeconds(120);
                return t;
            }
        }

/*    private static class MyBean {
        @Autowired
        private ThreadPoolTaskExecutor executor;

        private CountDownLatch countDownLatch;

        public void runTasks (int processLoops) {

            float startTime = System.nanoTime();

            for (int i = 0; i < processLoops; i++) {

                try {

                    URL url = new URL("http://localhost:9000/myendpoint");

                    HttpURLConnection con = (HttpURLConnection) url.openConnection();

                    BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream()));
                    String lineRead;

                    while ((lineRead = br.readLine()) != null) {

                        System.out.println("Output: " + i + " is " + lineRead + " from " + Thread.currentThread().getName());

                    }

                    con.disconnect();

                } catch (Exception e) {System.out.println("Exception: " + e);}

            }

            float finishTime = System.nanoTime();
            System.out.println("Time passed: " + (finishTime - startTime)/1000000000);

        }*/

        private static class MyBean {
            @Autowired
            private ThreadPoolTaskExecutor executor;

            private CountDownLatch countDownLatch;

            public void runTasks (int processLoops) {

                countDownLatch = new CountDownLatch(processLoops);

//                System.out.println("queue: " + executor.getThreadPoolExecutor().getQueue().size());
//                System.out.println("queue: " + executor.getThreadPoolExecutor().getQueue().remainingCapacity());
                executor.setQueueCapacity(100);
//                System.out.println("queue: " + executor.getThreadPoolExecutor().getQueue().remainingCapacity());

//                executor.setMaxPoolSize(processLoops - 60);

                float startTime = System.nanoTime();

                for (int i = 0; i < processLoops; i++) {
                    executor.execute(getTask(i));
                }
                try {
                    countDownLatch.await();
                    float finishTime = System.nanoTime();
                    System.out.println("Time passed: " + (finishTime - startTime)/1000000000);
                } catch (Exception e) {System.out.println(e);}

            }

            private Runnable getTask (int i) {
                return () -> {

                    try {

                        URL url = new URL("http://localhost:9000/myendpoint");

                        HttpURLConnection con = (HttpURLConnection) url.openConnection();

                        BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream()));
                        String lineRead;

                        while ((lineRead = br.readLine()) != null) {

                            System.out.println("Output: " + i + " is " + lineRead + " from " + Thread.currentThread().getName());

                        }

                        con.disconnect();

                    } catch (Exception e) {System.out.println("Exception: " + e);}

                    countDownLatch.countDown();

                };
            }


        }

}
