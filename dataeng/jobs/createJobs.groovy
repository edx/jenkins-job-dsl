import static analytics.AggregateDailyTrackingLogs.job as AggregateDailyTrackingLogsJob
import static analytics.DatabaseExportCoursewareStudentmodule.job as DatabaseExportCoursewareStudentmoduleJob
import static analytics.EventExportIncremental.job as EventExportIncrementalJob
import static analytics.EventExportIncrementalLarge.job as EventExportIncrementalLargeJob
import static analytics.JenkinsBackup.job as JenkinsBackupJob
import static analytics.PipelineAcceptanceTestManual.job as PipelineAcceptanceTestManualJob
import static analytics.PipelineAcceptanceTestMaster.job as PipelineAcceptanceTestMasterJob
import static analytics.ReadReplicaExportToS3.job as ReadReplicaExportToS3Job
import static analytics.SnowflakePublicGrantsCleaner.job as SnowflakePublicGrantsCleanerJob
import static analytics.SnowflakeRefreshSnowpipe.job as SnowflakeRefreshSnowpipeJob
import static analytics.SnowflakeReplicaImportFromS3.job as SnowflakeReplicaImportFromS3Job
import static org.edx.jenkins.dsl.JenkinsPublicConstants.DEFAULT_VIEW
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.error.YAMLException

def globals = binding.variables
String commonVarsDir = globals.get('COMMON_VARS_DIR')
String commonVarsFilePath = commonVarsDir + 'common.yaml'
Map commonConfigMap = [:]

try {
    out.println('Parsing secret YAML file')
    String commonConfigContents = readFileFromWorkspace(commonVarsFilePath)
    Yaml yaml = new Yaml()
    commonConfigMap = yaml.load(commonConfigContents)
    out.println('Successfully parsed secret YAML file')

}  catch (YAMLException e) {
    throw new IllegalArgumentException("Unable to parse ${commonVarsFilePath}: ${e.message}")
}

def taskMap = [
    AGGREGATE_DAILY_TRACKING_LOGS_JOB: AggregateDailyTrackingLogsJob,
    DATABASE_EXPORT_COURSEWARE_STUDENTMODULE_JOB: DatabaseExportCoursewareStudentmoduleJob,
    EVENT_EXPORT_INCREMENTAL_JOB: EventExportIncrementalJob,
    EVENT_EXPORT_INCREMENTAL_LARGE_JOB: EventExportIncrementalLargeJob,
    JENKINS_BACKUP_JOB: JenkinsBackupJob,
    PIPELINE_ACCEPTANCE_TEST_MANUAL_JOB: PipelineAcceptanceTestManualJob,
    PIPELINE_ACCEPTANCE_TEST_MASTER_JOB: PipelineAcceptanceTestMasterJob,
    READ_REPLICA_EXPORT_TO_S3_JOB: ReadReplicaExportToS3Job,
    SNOWFLAKE_PUBLIC_GRANTS_CLEANER_JOB: SnowflakePublicGrantsCleanerJob,
    SNOWFLAKE_REFRESH_SNOWPIPE_JOB: SnowflakeRefreshSnowpipeJob,
    SNOWFLAKE_REPLICA_IMPORT_FROM_S3_JOB: SnowflakeReplicaImportFromS3Job,
]

for (task in taskMap) {
    def extraVarsFileName = task.key + '_EXTRA_VARS.yaml'
    def extraVarsFilePath = commonVarsDir + extraVarsFileName
    Map extraVars = [:]
    try {
        String extraVarsContents = readFileFromWorkspace(extraVarsFilePath)
        Yaml yaml = new Yaml()
        extraVars = yaml.load(extraVarsContents)
    }
    catch (Exception e) {
        out.println("Unable to parse the extra variables file ${extraVarsFileName}: ${e.message}")
    }

    task.value(this, commonConfigMap + extraVars)
}

listView('Production') {
    description('Only for production jobs.')
    jobs {
        regex('.+production')
    }
    columns DEFAULT_VIEW.call()
}

listView('Edge') {
    description('Only for Edge jobs.')
    jobs {
        regex('.+edge')
    }
    columns DEFAULT_VIEW.call()
}

listView('Warehouse') {
    jobs {
        name('snowflake-schema-builder')
        regex('refresh-snowpipe-.*')
        regex('.+read-replica-(import|export)-(to|from)-s3|load-.+')
    }
    columns DEFAULT_VIEW.call()
}

listView('Tools') {
    jobs {
        name('data_engineering_seed_job')
        name('stitch-snowflake-lag-monitor')
        name('snowflake-public-grants-cleaner')
    }
    columns DEFAULT_VIEW.call()
}

listView('Backups') {
    jobs {
        regex('.*backup.*')
    }
    columns DEFAULT_VIEW.call()
}

listView('Current') {
    jobs {
        regex('(?!DEPRECATED-).*')
    }
    columns DEFAULT_VIEW.call()
}

// Manually add jobs that are not captured in DSL to this view
listView('Custom') {
    jobs {
    }
    columns DEFAULT_VIEW.call()
}
