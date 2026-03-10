package com.ntros.config.reader;

import com.ntros.config.data.CfgWorld;
import com.ntros.config.data.WorldConfig;
import java.io.InputStream;
import java.util.List;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

public class WorldConfigReader implements ConfigReader<CfgWorld> {

  private static final String WORLDS_RESOURCE_PATH = "cfg/worlds.yml";

  private final Yaml reader;
  private final InputStream inputStream;

  public WorldConfigReader() {
    reader = new Yaml(new Constructor(WorldConfig.class, new LoaderOptions()));
    inputStream = this.getClass()
        .getClassLoader()
        .getResourceAsStream(WORLDS_RESOURCE_PATH);
  }

  @Override
  public CfgWorld read() {
    throw new UnsupportedOperationException("Use readAll()");
  }

  @Override
  public List<CfgWorld> readAll() {
    WorldConfig config = reader.load(inputStream);
    return config.getWorlds();
  }
}
