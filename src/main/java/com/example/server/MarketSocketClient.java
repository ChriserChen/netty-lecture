package com.example.server;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.example.server.handler.MarketSocketHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshakerFactory;
import io.netty.handler.codec.http.websocketx.WebSocketVersion;
import io.netty.handler.proxy.Socks4ProxyHandler;
import io.netty.handler.proxy.Socks5ProxyHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import org.apache.commons.lang3.StringUtils;

import java.net.InetSocketAddress;
import java.net.URI;

public class MarketSocketClient {

    /***
     * ws url
     */
    public static final String url = "wss://api.huobi.pro/ws";
    private ChannelFuture future;
    private Channel channel;

    public void run() throws Exception {

        //1.检查uri地址
        if (StringUtils.isEmpty(url)) {
            throw new IllegalArgumentException("url is empty!");
        }
        //2.构建地址
        URI uri = new URI(url);

        //3.构建ssl
        final SslContext sslCtx;
        if (uri.getScheme().equals("wss")) {
            sslCtx = SslContextBuilder.forClient().build();
        } else {
            sslCtx = null;
        }

        //4.构建socket
        EventLoopGroup group = new NioEventLoopGroup(1);
        try {
            Bootstrap bootstrap = new Bootstrap();

            WebSocketClientHandshaker handshaker = WebSocketClientHandshakerFactory
                    .newHandshaker(uri, WebSocketVersion.V13, null, false, new DefaultHttpHeaders(), Integer.MAX_VALUE);

            MarketSocketHandler handler = new MarketSocketHandler(handshaker);

            bootstrap.group(group);
            bootstrap.option(ChannelOption.TCP_NODELAY, true)
                    .channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel ch) throws Exception {
                    ChannelPipeline pipeline = ch.pipeline();
//                            pipeline.addFirst(new Socks5ProxyHandler(new InetSocketAddress("127.0.0.1", 1080)));
                    if (sslCtx != null) {
                        pipeline.addLast(sslCtx.newHandler(ch.alloc(), uri.getHost(), -1));
                    }
                    pipeline.addLast(new HttpClientCodec(), new HttpObjectAggregator(8192), handler);
                }
            });
            int port = uri.getScheme().equalsIgnoreCase("wss") ? 443 : 80;
            future = bootstrap.connect(uri.getHost(), 443);
            future.addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(final ChannelFuture future) throws Exception {
                }
            });
            channel = future.sync().channel();
            // 等待握手成功
            handler.handshakeFuture().sync();

            if (channel.isActive()) {
                subject();
            }

            channel.closeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            group.shutdownGracefully();
        }
    }

    private void subject() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("sub", "market.ethbtc.kline.1min");
        jsonObject.put("id", "client1");
        String message = jsonObject.toJSONString();
        channel.writeAndFlush(new TextWebSocketFrame(message));
    }


    public static class Subject {
        private String sub;
        private String id;


        public Subject() {
        }

        public Subject(String id, String sub) {
            this.id = id;
            this.sub = sub;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getSub() {
            return sub;
        }

        public void setSub(String sub) {
            this.sub = sub;
        }

        @Override
        public String toString() {
            return "Subject{" +
                    "id='" + id + '\'' +
                    ", sub='" + sub + '\'' +
                    '}';
        }
    }

}
