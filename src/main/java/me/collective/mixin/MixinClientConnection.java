package me.collective.mixin;

import io.netty.bootstrap.AbstractBootstrap;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.handler.proxy.HttpProxyHandler;
import io.netty.handler.proxy.Socks5ProxyHandler;
import me.collective.proxy.ProxyEntry;
import me.collective.proxy.ProxyManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.net.InetAddress;
import java.net.InetSocketAddress;

/**
 * Подключение через выбранный прокси (SOCKS5/HTTP).
 *
 * <p>Перехватывает финальный connect: вместо сервера цепляемся к прокси,
 * а целевой адрес отдаём в рукопожатии (без локального DNS-резолва).
 */
@Mixin(targets = "net.minecraft.network.ClientConnection")
public abstract class MixinClientConnection {

    private static final String CONNECT_METHOD = "connect(Ljava/lang/String;ILnet/minecraft/network/state/NetworkState;"
        + "Lnet/minecraft/network/state/NetworkState;"
        + "Lnet/minecraft/network/listener/ClientPacketListener;"
        + "Lnet/minecraft/network/packet/c2s/handshake/ConnectionIntent;)V";

    /** Ванильный хендлер пайплайна, перехваченный перед подменой. */
    @Unique
    private static ChannelHandler collective$capturedHandler;

    @Redirect(
        method = CONNECT_METHOD,
        at = @At(value = "INVOKE",
            target = "Lio/netty/bootstrap/Bootstrap;handler(Lio/netty/channel/ChannelHandler;)Lio/netty/bootstrap/AbstractBootstrap;")
    )
    private AbstractBootstrap<?, ?> collective$captureHandler(Bootstrap bootstrap, ChannelHandler handler) {
        collective$capturedHandler = handler;
        return bootstrap.handler(handler);
    }

    @Redirect(
        method = CONNECT_METHOD,
        at = @At(value = "INVOKE",
            target = "Lio/netty/bootstrap/Bootstrap;connect(Ljava/net/InetAddress;I)Lio/netty/channel/ChannelFuture;")
    )
    private ChannelFuture collective$proxyConnect(Bootstrap bootstrap, InetAddress address, int port) {
        ProxyEntry proxy = ProxyManager.INSTANCE.getActive();
        if (proxy == null) {
            return bootstrap.connect(address, port);
        }

        ChannelHandler original = collective$capturedHandler;
        collective$capturedHandler = null;
        if (original == null) {
            return bootstrap.connect(address, port);
        }
        InetSocketAddress proxyAddr = new InetSocketAddress(proxy.host(), proxy.port());
        bootstrap.handler(new ChannelInitializer<Channel>() {
            @Override
            protected void initChannel(Channel ch) {
                // Хендлер сам цепляется к прокси и пробрасывает цель из connect()
                if (proxy.type() == ProxyEntry.ProxyType.SOCKS5) {
                    if (proxy.hasAuth()) {
                        ch.pipeline().addLast(new Socks5ProxyHandler(proxyAddr, proxy.user(), proxy.pass()));
                    } else {
                        ch.pipeline().addLast(new Socks5ProxyHandler(proxyAddr));
                    }
                } else {
                    if (proxy.hasAuth()) {
                        ch.pipeline().addLast(new HttpProxyHandler(proxyAddr, proxy.user(), proxy.pass()));
                    } else {
                        ch.pipeline().addLast(new HttpProxyHandler(proxyAddr));
                    }
                }
                ch.pipeline().addLast(original);
            }
        });
        // Цель остаётся серверной — хендлер завернёт TCP на прокси сам
        return bootstrap.connect(address, port);
    }
}
