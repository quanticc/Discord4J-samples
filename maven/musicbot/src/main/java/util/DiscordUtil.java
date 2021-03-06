package util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.util.DiscordException;
import sx.blah.discord.util.MissingPermissionsException;
import sx.blah.discord.util.RequestBuffer;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class DiscordUtil {

    private static final Logger log = LoggerFactory.getLogger(DiscordUtil.class);
    private static final int LENGTH_LIMIT = 2000;

    public static RequestBuffer.RequestFuture<IMessage> sendMessage(final IChannel channel, final String content) {
        RequestBuffer.RequestFuture<IMessage> response = null;
        if (content.length() > LENGTH_LIMIT) {
            SplitMessage splitMessage = new SplitMessage(content);
            List<String> splits = splitMessage.split(LENGTH_LIMIT);
            for (String split : splits) {
                response = sendMessage0(channel, split);
            }
        } else {
            response = sendMessage0(channel, content);
        }
        return response;
    }

    private static RequestBuffer.RequestFuture<IMessage> sendMessage0(final IChannel channel, final String content) {
        return RequestBuffer.request(() -> {
            try {
                return channel.sendMessage(content);
            } catch (MissingPermissionsException | DiscordException ex) {
                log.warn("Could not send message", ex);
            }
            return null;
        });
    }

    public static void deleteMessage(IMessage message) {
        RequestBuffer.request(() -> {
            try {
                message.delete();
            } catch (MissingPermissionsException | DiscordException e) {
                log.warn("Failed to delete message", e);
            }
            return null;
        });
    }

    public static void deleteMessage(IMessage message, long timeout, TimeUnit unit) {
        CompletableFuture.runAsync(() -> {
            try {
                unit.sleep(timeout);
            } catch (InterruptedException ex) {
                log.warn("Could not perform cleanup: {}", ex.toString());
            }
        }).thenRun(() -> RequestBuffer.request(() -> {
            try {
                message.delete();
            } catch (MissingPermissionsException | DiscordException e) {
                log.warn("Failed to delete message", e);
            }
            return null;
        }));
    }

    public static CompletableFuture<Void> processCommand(Runnable runnable) {
        return CompletableFuture.runAsync(runnable)
            .exceptionally(t -> {
                log.warn("Could not complete command", t);
                return null;
            });
    }
}
