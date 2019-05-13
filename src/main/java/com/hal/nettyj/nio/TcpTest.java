package com.hal.nettyj.nio;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * @Author: Steven HUANG
 * @Date: 2019/5/13
 */
@SpringBootTest
@RunWith(SpringRunner.class)
public class TcpTest {

  @Test
  public void runServer() throws Exception {
    TcpServer server = new TcpServer();
  }

  @Test
  public void runClient() throws Exception {
    TcpClient client = new TcpClient();

//    Scanner scanner = new Scanner(System.in);

    for (int i = 0; i < 30; i++) {
      client.send("Hello world>>>>" + i);
      Thread.sleep(1000L);
    }
  }

}
