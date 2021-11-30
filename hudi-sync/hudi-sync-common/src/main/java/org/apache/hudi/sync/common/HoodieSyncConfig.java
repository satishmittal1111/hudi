package org.apache.hudi.sync.common;

import org.apache.hudi.common.config.ConfigProperty;
import org.apache.hudi.common.config.HoodieConfig;
import org.apache.hudi.common.table.HoodieTableConfig;
import org.apache.hudi.common.util.Option;
import org.apache.hudi.keygen.constant.KeyGeneratorOptions;

import com.beust.jcommander.Parameter;

import java.util.ArrayList;
import java.util.List;

/**
 * Configs needed to sync data into Hive.
 */
public class HoodieSyncConfig extends HoodieConfig {

  @Parameter(names = {"--database"}, description = "name of the target database in Hive", required = true)
  public String databaseName;

  @Parameter(names = {"--table"}, description = "name of the target table in Hive", required = true)
  public String tableName;

  @Parameter(names = {"--base-path"}, description = "Basepath of hoodie table to sync", required = true)
  public String basePath;

  @Parameter(names = {"--base-file-format"}, description = "Format of the base files (PARQUET (or) HFILE)")
  public String baseFileFormat = "PARQUET";

  @Parameter(names = "--partitioned-by", description = "Fields in the schema partitioned by")
  public List<String> partitionFields = new ArrayList<>();

  @Parameter(names = "--partition-value-extractor", description = "Class which implements PartitionValueExtractor "
      + "to extract the partition values from HDFS path")
  public String partitionValueExtractorClass = SlashEncodedDayPartitionValueExtractor.class.getName();

  @Parameter(names = {"--assume-date-partitioning"}, description = "Assume standard yyyy/mm/dd partitioning, this"
      + " exists to support backward compatibility. If you use hoodie 0.3.x, do not set this parameter")
  public Boolean assumeDatePartitioning = false;

  public static final ConfigProperty<String> HIVE_SYNC_ENABLED = ConfigProperty
      .key("hoodie.datasource.hive_sync.enable")
      .defaultValue("false")
      .withDocumentation("When set to true, register/sync the table to Apache Hive metastore.");

  public static final ConfigProperty<String> META_SYNC_ENABLED = ConfigProperty
      .key("hoodie.datasource.meta.sync.enable")
      .defaultValue("false")
      .withDocumentation("Enable Syncing the Hudi Table with an external meta store or data catalog.");

  public static final ConfigProperty<String> META_SYNC_DATABASE = ConfigProperty
      .key("hoodie.datasource.hive_sync.database")
      .defaultValue("default")
      .withDocumentation("The name of the destination database that we should sync the hudi table to.");

  // If the table name for the metastore destination is not provided, pick it up from write or table configs.
  public static final ConfigProperty<String> META_SYNC_TABLE = ConfigProperty
      .key("hoodie.datasource.hive_sync.table")
      .defaultValue("unknown")
      .withInferFunction(cfg -> {
        if (cfg.contains(HoodieTableConfig.HOODIE_WRITE_TABLE_NAME_KEY)) {
          return Option.of(cfg.getString(HoodieTableConfig.HOODIE_WRITE_TABLE_NAME_KEY));
        } else if (cfg.contains(HoodieTableConfig.HOODIE_TABLE_NAME_KEY)) {
          return Option.of(cfg.getString(HoodieTableConfig.HOODIE_TABLE_NAME_KEY));
        } else {
          return Option.empty();
        }
      })
      .withDocumentation("The name of the destination table that we should sync the hudi table to.");

  public static final ConfigProperty<String> META_SYNC_BASE_FILE_FORMAT = ConfigProperty
      .key("hoodie.datasource.hive_sync.base_file_format")
      .defaultValue("PARQUET")
      .withDocumentation("Base file format for the sync.");

  // If partition fields are not explicitly provided, obtain from the KeyGeneration Configs
  public static final ConfigProperty<String> META_SYNC_PARTITION_FIELDS = ConfigProperty
      .key("hoodie.datasource.hive_sync.partition_fields")
      .defaultValue("")
      .withInferFunction(cfg -> {
        if (cfg.contains(KeyGeneratorOptions.PARTITIONPATH_FIELD_NAME)) {
          return Option.of(cfg.getString(KeyGeneratorOptions.PARTITIONPATH_FIELD_NAME));
        } else {
          return Option.empty();
        }
      })
      .withDocumentation("Field in the table to use for determining hive partition columns.");

  // If partition value extraction class is not explicitly provided, configure based on the partition fields.
  public static final ConfigProperty<String> META_SYNC_PARTITION_EXTRACTOR_CLASS = ConfigProperty
      .key("hoodie.datasource.hive_sync.partition_extractor_class")
      .defaultValue(SlashEncodedDayPartitionValueExtractor.class.getName())
      .withInferFunction(cfg -> {
        if (!cfg.contains(KeyGeneratorOptions.PARTITIONPATH_FIELD_NAME)) {
          return Option.of(NonPartitionedExtractor.class.getName());
        } else {
          int numOfPartFields = cfg.getString(KeyGeneratorOptions.PARTITIONPATH_FIELD_NAME).split(",").length
          if (numOfPartFields == 1
              && cfg.contains(KeyGeneratorOptions.HIVE_STYLE_PARTITIONING_ENABLE)
              && cfg.getString(KeyGeneratorOptions.HIVE_STYLE_PARTITIONING_ENABLE).equals("true")) {
            return Option.of(HiveStylePartitionValueExtractor.class.getName());
          } else {
            return Option.of(MultiPartKeysValueExtractor.class.getName());
          }
        }
      })
      .withDocumentation("Class which implements PartitionValueExtractor to extract the partition values, "
          + "default 'SlashEncodedDayPartitionValueExtractor'.");

  public static final ConfigProperty<String> META_SYNC_ASSUME_DATE_PARTITION = ConfigProperty
      .key("hoodie.datasource.hive_sync.assume_date_partitioning")
      .defaultValue("false")
      .withDocumentation("Assume partitioning is yyyy/mm/dd");
}
