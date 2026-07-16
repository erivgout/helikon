package dev.helikon.client.command;

import dev.helikon.client.friend.FriendManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FriendCommandTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void managesOnlyLocalFriendsAndColors() {
        FriendManager friends = new FriendManager(temporaryDirectory.resolve("helikon"));
        CommandDispatcher dispatcher = new CommandDispatcher();
        dispatcher.register(new FriendCommand(friends));
        RecordingFeedback feedback = new RecordingFeedback();

        dispatcher.dispatch(".friend add Alice_1", feedback);
        dispatcher.dispatch(".friend color alice_1 #123456", feedback);
        dispatcher.dispatch(".friend list", feedback);
        assertEquals(0xFF123456, friends.find("alice_1").orElseThrow().color());
        dispatcher.dispatch(".friend remove ALICE_1", feedback);

        assertTrue(feedback.infos.stream().anyMatch(message -> message.contains("Friends: Alice_1")));
        assertTrue(feedback.infos.stream().anyMatch(message -> message.contains("Removed local friend")));
    }
}
