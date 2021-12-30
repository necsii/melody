package audioCore.slash.component;

import audioCore.slash.SlashCommand;
import utils.Logging;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class AnonymousComponentManager {
  //public static final int MAX_CACHE = 50;

  private final HashMap<String, SlashCommand> idCache;

  public AnonymousComponentManager(List<SlashCommand> commands) {
    this.idCache = new HashMap<>();
    this.cache(commands);
    Logging.debug(getClass(), null, null, "Cached anonymous IDs:" + Arrays.toString(idCache.keySet().toArray(String[]::new)));
  }

  private void cache(List<SlashCommand> commands){
    for (SlashCommand command : commands){
      List<String> ids = command.allowAnonymousComponentCall();
      if (ids == null || ids.isEmpty()) continue;
      for (String id : ids){
        cache(id, command);
      }
    }
  }

  private void cache(String id, SlashCommand slashCommand){
    if (idCache.containsKey(id))
      throw new RuntimeException("No duplicate Component ID allowed: " + id);
    idCache.put(id, slashCommand);
  }

  public SlashCommand request(String component){
    return idCache.getOrDefault(component, null);
  }
}
