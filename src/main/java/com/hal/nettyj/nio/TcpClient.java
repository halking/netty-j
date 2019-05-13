package com.hal.nettyj.nio;

import com.google.common.collect.Lists;
import com.hal.nettyj.util.CodecUtil;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import lombok.extern.slf4j.Slf4j;

/**
 * @Author: Steven HUANG
 * @Date: 2019/5/13
 */
@Slf4j
public class TcpClient {

  private SocketChannel socketChannel;
  private Selector selector;
  private final List<String> datum = Lists.newArrayList();

  private CountDownLatch connected = new CountDownLatch(1);

  public TcpClient() {
    try {
      socketChannel = SocketChannel.open();
      socketChannel.configureBlocking(false);

      selector = Selector.open();

      socketChannel.register(selector, SelectionKey.OP_CONNECT);
      socketChannel.connect(new InetSocketAddress(InetAddress.getLocalHost(), 5454));

      new Thread(() -> handleKeys(), "client").start();

      if (connected.getCount() != 0) {
        connected.await();
      }
    } catch (IOException e) {
      throw new RuntimeException(e.getMessage(), e);
    } catch (InterruptedException ex) {
      throw new RuntimeException(ex.getMessage(), ex);
    }


  }

  private void handleKeys() {
    try {
      while (true) {
        int selectNums = selector.select(3 * 1000L);
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
    //连接就绪
    if (key.isConnectable()) {
      handleConnectable(key);
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

  private void handleConnectable(SelectionKey key) {
    try {
      //完成连接
      if (!socketChannel.isConnectionPending()) {
        return;
      }

      socketChannel.finishConnect();

      log.info("Client开始新的连接");

      socketChannel.register(selector, SelectionKey.OP_READ, datum);
    } catch (IOException e) {
      throw new RuntimeException(e.getMessage(), e);
    } finally {
      connected.countDown();
    }
  }

  private void handleReadable(SelectionKey key) {
    SocketChannel clientRegistered = (SocketChannel) key.channel();

    ByteBuffer readBuffer = CodecUtil.read(clientRegistered);

    while (readBuffer.hasRemaining()) {
      String content = CodecUtil.newString(readBuffer);
      log.info("Client 读取数据：{}", content);
    }
  }

  private void handleWritable(SelectionKey key) {
    SocketChannel clientRegistered = (SocketChannel) key.channel();

    List<String> response = (List<String>) key.attachment();
    response.stream().forEach(content -> {
      log.info("Client 写入数据: {}", content);
      CodecUtil.write(clientRegistered, content);
    });

    response.clear();

    try {
      clientRegistered.register(selector, SelectionKey.OP_READ, response);
    } catch (IOException e) {
      log.error(e.getMessage(), e);
      throw new RuntimeException(e.getMessage(), e);
    }
  }

  public synchronized void send(String content) {
    datum.add(content);
    log.info("Client 写入数据 datum：{}", content);
    try {
      socketChannel.register(selector, SelectionKey.OP_WRITE, datum);
      selector.wakeup();
    } catch (IOException e) {
      log.error(e.getMessage(), e);
      throw new RuntimeException(e.getMessage(), e);
    }
  }
}