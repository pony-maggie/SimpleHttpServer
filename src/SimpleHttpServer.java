import jdk.jfr.events.SocketReadEvent;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class SimpleHttpServer {

    //处理http request的线程
    static ThreadPool<HttpRequestHandler> threadPool = new DefaultThreadPool<>(1);
    static String basePath;
    static ServerSocket serverSocket;

    //服务监听端口
    static int port = 8080;
    public static void setPort(int port){
        if(port > 0){
            SimpleHttpServer.port = port;
        }
    }

    public static void setBasePath(String basePath){
        if(basePath != null && new File(basePath).exists() && new File(basePath).isDirectory()){
            SimpleHttpServer.basePath = basePath;
        }
    }

    public static void start() throws Exception {
        serverSocket = new ServerSocket(port);
        Socket socket = null;

        while ((socket = serverSocket.accept()) != null){
            threadPool.execute(new HttpRequestHandler(socket));
        }
        serverSocket.close();//这里显示的关闭感觉没有意义
    }


    static class HttpRequestHandler implements Runnable {

        private Socket socket;
        public HttpRequestHandler(Socket socket){
            this.socket = socket;
        }

        @Override
        public void run() {
            String line = null;
            byte[] buff = new byte[2048];
            BufferedReader br = null;
            BufferedReader reader = null;
            PrintWriter out = null;
            InputStream in = null;
            File directory = new File("");
            basePath = directory.getAbsolutePath();
            System.out.println("base path:" + basePath);

            try {
                reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                String header = reader.readLine();
                //由相对路径计算绝对路径
                String filePath = header.split(" ")[1];
                System.out.println("file path: " + filePath);
                out = new PrintWriter(socket.getOutputStream());
                if(filePath.endsWith("jpg")){
                    //简化一下，只处理这一种情况
                    in = new FileInputStream(new File(basePath, filePath));
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    int i = 0;
                    while ((i = in.read()) != -1){
                        baos.write(i);
                    }

                    byte[] array = baos.toByteArray();
                    out.println("HTTP/1.1 200 OK");
                    out.println("Server: Molly");
                    out.println("Content-Type: image/jpeg");
                    out.println("Content-Length: " + array.length);
                    out.println("");
                    socket.getOutputStream().write(array, 0, array.length);
                } else if(filePath.endsWith("html")){
                    br = new BufferedReader(new InputStreamReader(new FileInputStream(new File(basePath, filePath))));
                    out = new PrintWriter(socket.getOutputStream());
                    out.println("HTTP/1.1 200 OK");
                    out.println("Server: Molly");
                    out.println("Content-Type: text/html; charset=UTF-8");
                    out.println("");
                    while ((line = br.readLine()) != null) {
                        out.println(line);
                    }
                } else {
                    out = new PrintWriter(socket.getOutputStream());
                    out.println("HTTP/1.1 200 OK");
                    out.println("Server: Molly");
                    out.println("Content-Type: text/html; charset=UTF-8");
                    out.println("");
                    out.println("404 not found");
                }
                out.flush();

            } catch (Exception ex){
                out.println("HTTP/1.1 500");
                out.println("");
                out.flush();

            } finally {

                close(br, in, reader, out, socket);
            }
        }
    }

    //关闭流
    private static void close(Closeable... closeables){
        if(closeables != null){
            for (Closeable closeable:closeables){
                try {
                    if(closeable != null){
                        closeable.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
