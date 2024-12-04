import static analytics.AnalyticsEmailOptin.job as AnalyticsEmailOptinJob
import static analytics.AnalyticsExporter.job as AnalyticsExporterJob
import static analytics.DBTDocs.job as DBTDocsJob
import static analytics.DBTRun.job as DBTRunJob
import static analytics.DeployCluster.job as DeployClusterJob
import static analytics.EmrCostReporter.job as EmrCostReporterJob
import static analytics.ModelTransfers.job as ModelTransfersJob
import static analytics.RetirementJobEdxTriggers.job as RetirementJobEdxTriggersJob
import static analytics.RetirementJobs.job as RetirementJobsJob
import static analytics.SnowflakeCollectMetrics.job as SnowflakeCollectMetricsJob
import static analytics.SnowflakeSchemaBuilder.job as SnowflakeSchemaBuilderJob
import static analytics.SnowflakeUserRetirementStatusCleanup.job as SnowflakeUserRetirementStatusCleanupJob
import static analytics.PrefectFlowsDeployment.job as PrefectFlowsDeploymentJob
import static analytics.TerminateCluster.job as TerminateClusterJob
import static analytics.UpdateUsers.job as UpdateUsersJob
import static analytics.WarehouseTransforms.job as WarehouseTransformsJob
import static analytics.WarehouseTransformsCI.job as WarehouseTransformsCIJob
import static analytics.WarehouseTransformsCIMasterMerges.job as WarehouseTransformsCIMasterMergesJob
import static analytics.WarehouseTransformsCIManual.job as WarehouseTransformsCIManualJob
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
    ANALYTICS_EMAIL_OPTIN_JOB: AnalyticsEmailOptinJob,
    ANALYTICS_EXPORTER_JOB: AnalyticsExporterJob,
    DBT_DOCS_JOB: DBTDocsJob,
    DBT_RUN_JOB: DBTRunJob,
    DEPLOY_CLUSTER_JOB: DeployClusterJob,
    EMR_COST_REPORTER_JOB: EmrCostReporterJob,
    MODEL_TRANSFERS_JOB: ModelTransfersJob,
    RETIREMENT_JOB_EDX_TRIGGERS_JOB: RetirementJobEdxTriggersJob,
    RETIREMENT_JOBS_JOB: RetirementJobsJob,
    SNOWFLAKE_COLLECT_METRICS_JOB: SnowflakeCollectMetricsJob,
    SNOWFLAKE_SCHEMA_BUILDER_JOB: SnowflakeSchemaBuilderJob,
    SNOWFLAKE_USER_RETIREMENT_STATUS_CLEANUP_JOB: SnowflakeUserRetirementStatusCleanupJob,
    PREFECT_FLOWS_DEPLOYMENT_JOB: PrefectFlowsDeploymentJob,
    TERMINATE_CLUSTER_JOB: TerminateClusterJob,
    UPDATE_USERS_JOB: UpdateUsersJob,
    WAREHOUSE_TRANSFORMS_CI_JOB: WarehouseTransformsCIJob,
    WAREHOUSE_TRANSFORMS_CI_MANUAL_JOB: WarehouseTransformsCIManualJob,
    WAREHOUSE_TRANSFORMS_CI_MASTER_MERGES_JOB: WarehouseTransformsCIMasterMergesJob,
    WAREHOUSE_TRANSFORMS_JOB: WarehouseTransformsJob,
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
        name('courseware-links-clicked')
        name('generate-warehouse-docs')
        name('snowflake-schema-builder')
        regex('refresh-snowpipe-.*')
        regex('.+read-replica-import|load-.+')
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
listView('prefect') {
    description('Jobs that are used for prefect flows deployment')
    jobs {
        regex('prefect-flows-.*')
    }
    columns DEFAULT_VIEW.call()
}
listView('retirement') {
    description('Jobs that are used for user retirement and retirement partner reporting')
    jobs {
        regex('.*retirement.*')
    }
    columns DEFAULT_VIEW.call()
}
