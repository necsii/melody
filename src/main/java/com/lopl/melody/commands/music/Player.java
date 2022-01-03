package com.lopl.melody.commands.music;

import com.jagrosh.jdautilities.command.Command;
import com.lopl.melody.audio.handler.PlayerManager;
import com.lopl.melody.audio.handler.TrackScheduler;
import com.lopl.melody.slash.SlashCommand;
import com.lopl.melody.audio.util.AudioStateChecks;
import com.lopl.melody.utils.Logging;
import com.lopl.melody.utils.embed.EmbedColor;
import com.lopl.melody.utils.embed.EmbedError;
import com.lopl.melody.utils.embed.ReactionEmoji;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.Button;

import javax.annotation.Nullable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class Player extends SlashCommand {

  public static final String PlayPause = "play_pause";
  public static final String SkipForward = "skip";
  public static final String SkipBackwards = "skip_reverse";
  public static final String Stop = "stop";

  public Player() {
    super.name = "player";
    super.category = new Command.Category("Sound");
    super.help = "/player : Shows some clickable buttons to control the best of all music bots.";
    super.description = "Shows some clickable buttons to control the best of all music bots.";
  }

//    @Override
//    public void action(SlashCommandEvent event) {
//        event.replyEmbeds(new EmbedBuilder()
//                .setDescription("[Song Name](https://www.youtube.com/watch?v=dQw4w9WgXcQ)" + "   4:12")
//                .build())
//        .addActionRow(Button.secondary("0", "⏮"), Button.secondary("0", "▶️"), Button.secondary("0", "⏹"), Button.secondary("0", "⏭"), Button.secondary("0", "\uD83D\uDD00"))//▶⏹
//                .addActionRow(Button.secondary("0", "back"), Button.secondary("0", "pause"), Button.secondary("0", "stop"), Button.secondary("0", "skip"), Button.secondary("0", "shuffle"))
//                .addActionRow(Button.secondary("0", "ᐅ"), Button.secondary("0", "□"), Button.secondary("0", "»"), Button.secondary("0", "↝"))
//        .queue();
//    }


  @Nullable
  @Override
  public List<String> allowAnonymousComponentCall() {
    return List.of(PlayPause, SkipBackwards, SkipForward, Stop);
  }

  @Override
  protected void execute(SlashCommandEvent event) {
    Logging.slashCommand(getClass(), event);

    if (event.getGuild() == null) {
      event.replyEmbeds(EmbedError.with("This command can only be executed in a server textchannel")).queue();
      return;
    }

    if (!AudioStateChecks.isMelodyInVC(event)) {
      event.replyEmbeds(EmbedError.with("This Command requires **Melody** to be **connected to a voice channel**")).queue();
      return;
    }

    if (!AudioStateChecks.isMemberAndMelodyInSameVC(event)) {
      GuildVoiceState voiceState = event.getGuild().getSelfMember().getVoiceState();
      if (voiceState == null) return; // should be covered by the check above
      VoiceChannel channel = voiceState.getChannel();
      if (channel == null) return; // should be covered by the check above
      event.replyEmbeds(EmbedError.withFormat("This Command requires **you** to be **connected to Melody's voice channel** <#%S>", channel.getId())).queue();
      return;
    }

    PlayerManager manager = PlayerManager.getInstance();
    AudioPlayer player = manager.getGuildAudioManager(event.getGuild()).player;
    ActionRow buttons = getActionRow(event.getGuild(), null);
    for (Button button : buttons.getButtons())
      registerButton(button);

    event.replyEmbeds(getSongEmbed(player.getPlayingTrack()))
        .addActionRow(buttons.getButtons())
        .queue();
  }

  private ActionRow getActionRow(Guild guild, @Nullable Boolean playing) {
    PlayerManager manager = PlayerManager.getInstance();
    AudioPlayer player = manager.getGuildAudioManager(guild).player;
    TrackScheduler scheduler = manager.getGuildAudioManager(guild).scheduler;

    boolean isPlaying = !player.isPaused() && player.getPlayingTrack() != null;
    if (playing != null) isPlaying = playing;
    boolean hasTrack = player.getPlayingTrack() != null;
    boolean hasNextTrack = !scheduler.getQueue().isEmpty();

    Button bPlayPause = Button.primary(PlayPause, Emoji.fromMarkdown(isPlaying ? ReactionEmoji.PAUSE : ReactionEmoji.RESUME)).withDisabled(!hasTrack);
    Button bSkip = Button.secondary(SkipForward, Emoji.fromMarkdown(ReactionEmoji.SKIP)).withDisabled(!hasNextTrack);
    Button bPrevious = Button.danger(SkipBackwards, Emoji.fromMarkdown(ReactionEmoji.BACKWARDS)).asDisabled();
    Button bStop = Button.secondary(Stop, Emoji.fromMarkdown(ReactionEmoji.STOP)).withDisabled(!hasTrack);

    return ActionRow.of(bPrevious, bPlayPause, bSkip, bStop);
  }

  private MessageEmbed getSongEmbed(AudioTrack audioTrack) {
    if (audioTrack == null)
      return new EmbedBuilder().setDescription("No track currently playing.").build();

    AudioTrackInfo trackInfo = audioTrack.getInfo();
    EmbedBuilder eb = new EmbedBuilder()
        .setColor(EmbedColor.BLUE)
        .setTitle(trackInfo.title)
        .setAuthor(trackInfo.author)
        .setThumbnail(getThumbnail(trackInfo.uri))
        .setDescription(
            new SimpleDateFormat("mm:ss").format(new Date(audioTrack.getPosition())) + " / " +
                new SimpleDateFormat("mm:ss").format(new Date(trackInfo.length)) + "\n");
    return eb.build();
  }

  private String getThumbnail(String uri) {
    // i.e. https://www.youtube.com/watch?v=dQw4w9WgXcQ
    uri = uri.replaceAll("//youtube", "//img.youtube");
    uri = uri.replaceAll("www", "img");
    uri = uri.replaceAll("watch\\?v=", "vi/");
    uri = uri + "/hqdefault.jpg";
    // i.e. https://img.youtube.com/vi/dQw4w9WgXcQ/hqdefault.jpg
    return uri;
  }

  @Override
  protected void clicked(ButtonClickEvent event, boolean anonymous) {
    Logging.button(getClass(), event);

    if (event.getGuild() == null) {
      event.replyEmbeds(EmbedError.with("This command can only be executed in a server textchannel")).queue();
      return;
    }

    if (!AudioStateChecks.isMelodyInVC(event)) {
      event.replyEmbeds(EmbedError.with("This Command requires **Melody** to be **connected to a voice channel**")).queue();
      return;
    }

    if (!AudioStateChecks.isMemberAndMelodyInSameVC(event)) {
      GuildVoiceState voiceState = event.getGuild().getSelfMember().getVoiceState();
      if (voiceState == null) return; // should be covered by the check above
      VoiceChannel channel = voiceState.getChannel();
      if (channel == null) return; // should be covered by the check above
      event.replyEmbeds(EmbedError.withFormat("This Command requires **you** to be **connected to Melody's voice channel** <#%S>", channel.getId())).queue();
      return;
    }

    if (event.getButton() == null || event.getButton().getId() == null || event.getButton().getEmoji() == null) {
      event.replyEmbeds(EmbedError.with("Something is wrong with this button...")).queue();
      return;
    }

    PlayerManager manager = PlayerManager.getInstance();
    AudioPlayer player = manager.getGuildAudioManager(event.getGuild()).player;
    boolean paused = player.isPaused();

    switch (event.getButton().getId()) {
      case PlayPause:
        if (paused)
          new Resume().resume(event.getGuild());
        else
          new Pause().pause(event.getGuild());
        break;
      case SkipForward:
        new Skip().skip(event.getGuild());
        break;
      case SkipBackwards:
        // TODO Not implemented
        break;
      case Stop:
        new Stop().stop(event.getGuild());
        new Disconnect().disconnect(event.getGuild());
        break;
      default:
        return;
    }

    // reload Message
    Message message = event.getMessage();

    MessageEmbed embed = getSongEmbed(player.getPlayingTrack());
    ActionRow buttons = getActionRow(event.getGuild(), paused);
    message.editMessageEmbeds(embed).queue();
    event.editComponents(buttons).queue();

    for (Button button : buttons.getButtons())
      registerButton(button);
  }
}