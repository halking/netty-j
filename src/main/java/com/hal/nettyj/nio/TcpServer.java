package com.hal.nettyj.nio;

import com.google.common.collect.Lists;
import com.hal.nettyj.util.CodecUtil;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

/**
 * @Author: Steven HUANG
 * @Date: 2019/5/13
 */
@Slf4j
public class TcpServer {

  private ServerSocketChannel serverSocketChannel;
  private Selector selector;
  private Integer port;
  private Integer size = 0;

  public TcpServer() {
    try {
      //创建一个new serverChannel
      serverSocketChannel = ServerSocketChannel.open();
      //配置为非阻塞
      serverSocketChannel.configureBlocking(false);
      //绑定Server port
      serverSocketChannel.socket().bind(new InetSocketAddress(InetAddress.getLocalHost(), 5454));

      //创建选择器
      selector = Selector.open();
      serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

      handleKeys();
    } catch (Exception e) {
      throw new RuntimeException(e.getMessage(), e);
    }
  }

  private void handleKeys() {
    try {
      while (true) {
        int selectNums = selector.select(3 * 1000);
        if (selectNums == 0) {
          continue;
        }

        Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
        while (iterator.hasNext()) {
          SelectionKey key = iterator.next();
          //移除已经处理的事件key
          iterator.remove();
          if (!key.isValid()) {
            continue;
          }
          //处理事件
          handleKey(key);

        }
      }
    } catch (IOException e) {
      throw new RuntimeException(e.getMessage(), e);
    }
  }

  private void handleKey(SelectionKey key) {
    //接受就绪
    if (key.isAcceptable()) {
      handleAcceptable(key);
    }

    //读就绪
    if (key.isReadable()) {
      handleReadable(key);
    }

    //写就绪
    if (key.isWritable()) {
      handleWritable(key);
    }
  }

  private void handleAcceptable(SelectionKey key) {
    try {
      ServerSocketChannel serverRegistered = (ServerSocketChannel) key.channel();
      SocketChannel socketChannel = serverRegistered.accept();
      socketChannel.configureBlocking(false);
      log.info("Server 接受新的client: {}", socketChannel);

      socketChannel.register(selector, SelectionKey.OP_READ, Lists.newArrayList());
    } catch (Exception e) {
      throw new RuntimeException(e.getMessage(), e);
    }
  }

  private void handleReadable(SelectionKey key) {
    try {
      //Get client channel registered
      SocketChannel clientRegistered = (SocketChannel) key.channel();
      if (clientRegistered == null) {
        key.cancel();
        clientRegistered.close();
      }

      ByteBuffer readBuffer = CodecUtil.read(clientRegistered);
      if (readBuffer == null) {
        log.info("断开连接");
        clientRegistered.register(selector, 0);
      }

      if (readBuffer.position() > 0) {
        String content = CodecUtil.newString(readBuffer);
        log.info("Server 读取的数据：{}", content);

        List<String> response = (List<String>) key.attachment();
        response.add(content);

        clientRegistered.register(selector, SelectionKey.OP_WRITE, key.attachment());
      }
    } catch (IOException e) {
      throw new RuntimeException(e.getMessage(), e);
    }
  }

  private void handleWritable(SelectionKey key) {
    //Get client channel registered
    SocketChannel clientRegistered = (SocketChannel) key.channel();
    try {
      // Get response data
      List<String> response = (List<String>) key.attachment();
      response.stream().forEach(content -> {
        log.info("Server 写入数据：{}", content);
        CodecUtil.write(clientRegistered, content);
      });

      response.clear();

      //注册 client socket channel to Selector
      clientRegistered.register(selector, SelectionKey.OP_READ, response);
    } catch (Exception e) {
      throw new RuntimeException(e.getMessage(), e);
    }
  }

}
