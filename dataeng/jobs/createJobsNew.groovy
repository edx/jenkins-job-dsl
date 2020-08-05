import static analytics.EmrCostReporter.job as EmrCostReporterJob
import static analytics.ExpireVerticaPassword.job as ExpireVerticaPasswordJob
import static analytics.SnowflakeExpirePasswords.job as SnowflakeExpirePasswordsJob
import static analytics.SnowflakeCollectMetrics.job as SnowflakeCollectMetricsJob
import static analytics.DeployCluster.job as DeployClusterJob
import static analytics.TerminateCluster.job as TerminateClusterJob
import static analytics.UpdateUsers.job as UpdateUsersJob
import static analytics.VerticaDiskUsageMonitor.job as VerticaDiskUsageMonitorJob
import static analytics.SnowflakeSchemaBuilder.job as SnowflakeSchemaBuilderJob
import static analytics.TestHassan.job as TestHassanJob
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
    // Add jobs here as they are ported from the old analytics Jenkins server.
/*    EMR_COST_REPORTER_JOB: EmrCostReporterJob,
    EXPIRE_VERTICA_PASSWORD_JOB: ExpireVerticaPasswordJob,
    SNOWFLAKE_EXPIRE_PASSWORDS_JOB: SnowflakeExpirePasswordsJob,
    SNOWFLAKE_COLLECT_METRICS_JOB: SnowflakeCollectMetricsJob,
    DEPLOY_CLUSTER_JOB: DeployClusterJob,
    TERMINATE_CLUSTER_JOB: TerminateClusterJob,
    UPDATE_USERS_JOB: UpdateUsersJob,
    VERTICA_DISK_USAGE_MONITOR_JOB: VerticaDiskUsageMonitorJob,
    SNOWFLAKE_SCHEMA_BUILDER_JOB: SnowflakeSchemaBuilderJob,*/
    TEST_HASSAN_JOB: TestHassanJob
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
        out.println("File $extraVarsFileName does not exist.")
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

listView('Release') {
    description('Jobs that are used for testing release candidates.')
    jobs {
        regex('.+release')
    }
    columns DEFAULT_VIEW.call()
}

listView('Exporter') {
    description('Jobs that are used for exporting course data.')
    jobs {
        regex('analytics-.+')
    }
    columns DEFAULT_VIEW.call()
}

listView('Warehouse') {
    jobs {
        name('event-type-distribution')
        name('courseware-links-clicked')
        name('finance-report')
        name('payments-validation')
        name('generate-warehouse-docs')
        name('affiliate-window')
        name('snowflake-schema-builder')
        regex('refresh-snowpipe-.*')
        regex('.+read-replica-import|load-.+|vertica-schema-to.+|.*sql-script.*')
    }
    columns DEFAULT_VIEW.call()
}

listView('Tools') {
    jobs {
        name('data_engineering_seed_job')
        name('deploy-cluster')
        name('terminate-cluster')
        name('emr-cost-reporter')
        name('update-users')
        name('vertica-disk-usage-monitor')
        name('monitor-bigquery-loading')
        name('stitch-snowflake-lag-monitor')
        name('snowflake-public-grants-cleaner')
    }
    columns DEFAULT_VIEW.call()
}

listView('Stage') {
    jobs {
        regex('.+stage')
    }
    columns DEFAULT_VIEW.call()
}

listView('Enterprise') {
    jobs {
        regex('enterprise.+')
    }
    columns DEFAULT_VIEW.call()
}

listView('Backups') {
    jobs {
        regex('.*backup.*')
    }
    columns DEFAULT_VIEW.call()
}

listView('dbt') {
    jobs {
        name('snowflake-schema-builder')
        regex('dbt-.*|warehouse-transforms-.*')
    }
    columns DEFAULT_VIEW.call()
}
