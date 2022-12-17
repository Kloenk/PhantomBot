/*
 * Copyright (C) 2016-2022 phantombot.github.io/PhantomBot
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package tv.phantombot.event.irc.message;

import java.util.HashMap;
import java.util.Map;
import tv.phantombot.event.irc.IrcEvent;
import tv.phantombot.twitch.irc.TwitchSession;

public abstract class IrcMessageEvent extends IrcEvent {

    protected final String sender;
    protected final String message;
    protected final Map<String, String> tags;

    /**
     * Class constructor.
     *
     * @param session
     * @param sender
     * @param message
     */
    protected IrcMessageEvent(TwitchSession session, String sender, String message) {
        super(session);

        this.sender = sender;
        this.message = message;
        this.tags = new HashMap<>();
    }

    /**
     * Class constructor.
     *
     * @param session
     * @param sender
     * @param message
     * @param tags
     */
    protected IrcMessageEvent(TwitchSession session, String sender, String message, Map<String, String> tags) {
        super(session);

        this.sender = sender;
        this.message = message;
        this.tags = (tags == null ? new HashMap<>() : tags);
    }

    /**
     * Method that returns the sender.
     *
     * @return sender
     */
    public String getSender() {
        return this.sender;
    }

    /**
     * Method that returns the message.
     *
     * @return sender
     */
    public String getMessage() {
        return this.message;
    }

    /**
     * Method that returns the IRCv3 tags.
     *
     * @return tags
     */
    public Map<String, String> getTags() {
        return this.tags;
    }
}
