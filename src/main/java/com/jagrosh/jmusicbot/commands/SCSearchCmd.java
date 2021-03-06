/*
 * Copyright 2016 John Grosh <john.a.grosh@gmail.com>.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jagrosh.jmusicbot.commands;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException.Severity;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import java.util.concurrent.TimeUnit;
import com.jagrosh.jdautilities.commandclient.CommandEvent;
import com.jagrosh.jdautilities.menu.orderedmenu.OrderedMenuBuilder;
import com.jagrosh.jmusicbot.Bot;
import com.jagrosh.jmusicbot.utils.FormatUtil;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Message;

/**
 *
 * @author John Grosh <john.a.grosh@gmail.com>
 */
public class SCSearchCmd extends MusicCommand {

    private final OrderedMenuBuilder builder;
    public SCSearchCmd(Bot bot)
    {
        super(bot);
        this.name = "scsearch";
        this.arguments = "<query>";
        this.help = "searches Soundcloud for a provided query";
        this.beListening = true;
        this.bePlaying = false;
        this.botPermissions = new Permission[]{Permission.MESSAGE_EMBED_LINKS};
        builder = new OrderedMenuBuilder()
                .allowTextInput(true)
                .useNumbers()
                .useCancelButton(true)
                .setEventWaiter(bot.getWaiter())
                .setTimeout(1, TimeUnit.MINUTES)
                ;
    }
    @Override
    public void doCommand(CommandEvent event) {
        if(event.getArgs().isEmpty())
        {
            event.reply(event.getClient().getError()+" Please include a query.");
            return;
        }
        event.getChannel().sendMessage("\uD83D\uDD0E Searching... `["+event.getArgs()+"]`").queue(m -> {
            bot.getAudioManager().loadItemOrdered(event.getGuild(), "scsearch:"+event.getArgs(), new ResultHandler(m,event));
        });
    }
    
    private class ResultHandler implements AudioLoadResultHandler {
        final Message m;
        final CommandEvent event;
        private ResultHandler(Message m, CommandEvent event)
        {
            this.m = m;
            this.event = event;
        }
        
        @Override
        public void trackLoaded(AudioTrack track) {
            int pos = bot.queueTrack(event, track)+1;
            m.editMessage(event.getClient().getSuccess()+" Added **"+track.getInfo().title
                    +"** (`"+FormatUtil.formatTime(track.getDuration())+"`) "+(pos==0 ? "to begin playing" 
                        : " to the queue at position "+pos)).queue();
        }

        @Override
        public void playlistLoaded(AudioPlaylist playlist) {
            builder.setColor(event.getSelfMember().getColor())
                    .setText(event.getClient().getSuccess()+" Search results for `"+event.getArgs()+"`:")
                    .setChoices(new String[0])
                    .setAction(i -> {
                        AudioTrack track = playlist.getTracks().get(i-1);
                        int pos = bot.queueTrack(event, track)+1;
                        event.getChannel().sendMessage(event.getClient().getSuccess()+" Added **"+track.getInfo().title
                                +"** (`"+FormatUtil.formatTime(track.getDuration())+"`) "+(pos==0 ? "to begin playing" 
                                    : " to the queue at position "+pos)).queue();
                    })
                    .setCancel(() -> m.delete().queue())
                    .setUsers(event.getAuthor())
                    ;
            for(int i=0; i<4&&i<playlist.getTracks().size(); i++)
            {
                AudioTrack track = playlist.getTracks().get(i);
                builder.addChoices("`["+FormatUtil.formatTime(track.getDuration())+"]` [**"
                        +track.getInfo().title+"**]("+track.getInfo().uri+")");
            }
            builder.build().display(m);
        }

        @Override
        public void noMatches() {
            m.editMessage(event.getClient().getWarning()+" No results found for `"+event.getArgs()+"`.").queue();
        }

        @Override
        public void loadFailed(FriendlyException throwable) {
            if(throwable.severity==Severity.COMMON)
                m.editMessage(event.getClient().getError()+" Error loading: "+throwable.getMessage()).queue();
            else
                m.editMessage(event.getClient().getError()+" Error loading track.").queue();
        }
    }
}
