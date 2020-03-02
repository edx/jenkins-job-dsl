import static analytics.AnalyticsEmailOptin.job as AnalyticsEmailOptinJob
import static analytics.AnalyticsExporter.job as AnalyticsExporterJob
import static analytics.UserActivity.job as UserActivityJob
import static analytics.VideoTimeline.job as VideoTimelineJob
import static analytics.UserLocationByCourse.job as UserLocationByCourseJob
import static analytics.Enrollment.job as EnrollmentJob
import static analytics.ModuleEngagement.job as ModuleEngagementJob
import static analytics.DatabaseExportCoursewareStudentmodule.job as DatabaseExportCoursewareStudentmoduleJob
import static analytics.AnswerDistribution.job as AnswerDistributionJob
import static analytics.FinanceReport.cybersource_pull_job as CybersourcePullJob
import static analytics.FinanceReport.payments_validation_job as PaymentsValidationJob
import static analytics.FinanceReport.finance_report_job as FinanceReportJob
import static analytics.BigqueryReplicaImport.job as BigqueryReplicaImportJob
import static analytics.EventExportIncremental.job as EventExportIncrementalJob
import static analytics.EventExportIncrementalLarge.job as EventExportIncrementalLargeJob
import static analytics.CoursewareLinksClicked.job as CoursewareLinksClickedJob
import static analytics.EventTypeDistribution.job as EventTypeDistributionJob
import static analytics.GenerateWarehouseDocs.job as GenerateWarehouseDocsJob
import static analytics.SqlScripts.multiple_scripts_job as SqlScriptsJob
import static analytics.SqlScripts.single_script_job as SingleSqlScriptJob
import static analytics.LoadAffiliateWindowToWarehouse.job as LoadAffiliateWindowWarehouseJob
import static analytics.SnowflakeSchemaBuilder.job as SnowflakeSchemaBuilderJob
import static analytics.LoadWarehouse.vertica_job as LoadWarehouseVerticaJob
import static analytics.LoadWarehouse.bigquery_job as LoadWarehouseBigQueryJob
import static analytics.LoadWarehouse.snowflake_job as LoadWarehouseSnowflakeJob
import static analytics.LoadEvents.load_events_to_s3_job as LoadEventsToS3Job
import static analytics.LoadEvents.load_events_to_vertica_job as LoadEventsToVerticaJob
import static analytics.LoadEvents.load_json_events_to_s3_job as LoadJsonEventsToS3Job
import static analytics.LoadEvents.load_json_events_to_bigquery_job as LoadJsonEventsToBigqueryJob
import static analytics.VerticaReplicaImport.job as VerticaReplicaImportJob
import static analytics.BackupVertica.job as BackupVerticaJob
import static analytics.TotalEventsDailyReport.job as TotalEventsDailyReportJob
import static analytics.JenkinsBackup.job as JenkinsBackupJob
import static analytics.LoadCourseStructure.job as LoadCourseStructureJob
import static analytics.Enterprise.job as EnterpriseJob
import static analytics.EmrCostReporter.job as EmrCostReporterJob
import static analytics.VerticaDiskUsageMonitor.job as VerticaDiskUsageMonitorJob
import static analytics.UpdateUsers.job as UpdateUsersJob
import static analytics.TerminateCluster.job as TerminateClusterJob
import static analytics.EnrollmentValidationEvents.job as EnrollmentValidationEventsJob
import static analytics.DeployCluster.job as DeployClusterJob
import static analytics.LoadInsightsToVertica.job as LoadInsightsToVerticaJob
import static analytics.LoadGoogleAnalyticsPermissions.job as LoadGoogleAnalyticsPermissionsJob
import static analytics.AggregateDailyTrackingLogs.job as AggregateDailyTrackingLogsJob
import static analytics.MonitorBigqueryEventLoading.job as MonitorBigqueryEventLoadingJob
import static analytics.VerticaSchemaToS3.job as VerticaSchemaToS3Job
import static analytics.LoadVerticaSchemaToSnowflake.job as LoadVerticaSchemaToSnowflakeJob
import static analytics.LoadVerticaSchemaToBigquery.job as LoadVerticaSchemaToBigqueryJob
import static analytics.LoadGoogleSpreadsheetToSnowflake.job as LoadGoogleSpreadsheetToSnowflakeJob
import static analytics.LoadGoogleSpreadsheetToVertica.job as LoadGoogleSpreadsheetToVerticaJob
import static analytics.SnowflakeReplicaImport.job as SnowflakeReplicaImportJob
import static analytics.LoadPaypalCaseReportToVertica.job as PayPalCaseReportLoadJob
import static analytics.WarehouseTransforms.job as WarehouseTransformsJob
import static analytics.DBTSourceFreshness.job as DBTSourceFreshnessJob
import static analytics.DBTManual.job as DBTManualJob
import static analytics.SnowflakeRefreshSnowpipe.job as SnowflakeRefreshSnowpipeJob
import static analytics.ExpireVerticaPassword.job as ExpireVerticaPasswordJob
import static analytics.SnowflakeExpirePasswords.job as SnowflakeExpirePasswordsJob
import static analytics.ProgramEnrollmentReports.job as ProgramEnrollmentReportsJob
import static analytics.SnowflakeMicrobachelorsITK.job as SnowflakeMicrobachelorsITKJob
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
    ANALYTICS_EMAIL_OPTIN_JOB: AnalyticsEmailOptinJob,
    ANALYTICS_EXPORTER_JOB: AnalyticsExporterJob,
    USER_ACTIVITY_JOB: UserActivityJob,
    VIDEO_TIMELINE_JOB: VideoTimelineJob,
    USER_LOCATION_BY_COURSE_JOB: UserLocationByCourseJob,
    ENROLLMENT_JOB: EnrollmentJob,
    MODULE_ENGAGEMENT_JOB: ModuleEngagementJob,
    DATABASE_EXPORT_COURSEWARE_STUDENTMODULE_JOB: DatabaseExportCoursewareStudentmoduleJob,
    ANSWER_DISTRIBUTION_JOB: AnswerDistributionJob,
    CYBERSOURCE_PULL_JOB: CybersourcePullJob,
    PAYMENTS_VALIDATION_JOB: PaymentsValidationJob,
    FINANCE_REPORT_JOB: FinanceReportJob,
    BIGQUERY_REPLICA_IMPORT_JOB: BigqueryReplicaImportJob,
    EVENT_EXPORT_INCREMENTAL_JOB: EventExportIncrementalJob,
    COURSEWARE_LINKS_CLICKED_JOB: CoursewareLinksClickedJob,
    EVENT_TYPE_DISTRIBUTION_JOB: EventTypeDistributionJob,
    GENERATE_WAREHOUSE_DOCS_JOB: GenerateWarehouseDocsJob,
    SQL_SCRIPTS_JOB: SqlScriptsJob,
    SINGLE_SQL_SCRIPT_JOB: SingleSqlScriptJob,
    LOAD_AFFILIATE_WINDOW_JOB: LoadAffiliateWindowWarehouseJob,
    LOAD_WAREHOUSE_VERTICA_JOB: LoadWarehouseVerticaJob,
    LOAD_WAREHOUSE_SNOWFLAKE_JOB: LoadWarehouseSnowflakeJob,
    LOAD_WAREHOUSE_BIGQUERY_JOB: LoadWarehouseBigQueryJob,
    LOAD_EVENTS_TO_S3_JOB: LoadEventsToS3Job,
    LOAD_EVENTS_TO_VERTICA_JOB: LoadEventsToVerticaJob,
    LOAD_JSON_EVENTS_TO_S3_JOB: LoadJsonEventsToS3Job,
    LOAD_JSON_EVENTS_TO_BIGQUERY_JOB: LoadJsonEventsToBigqueryJob,
    SNOWFLAKE_SCHEMA_BUILDER_JOB: SnowflakeSchemaBuilderJob,
    VERTICA_REPLICA_IMPORT_JOB: VerticaReplicaImportJob,
    BACKUP_VERTICA_JOB: BackupVerticaJob,
    TOTAL_EVENTS_DAILY_REPORT_JOB: TotalEventsDailyReportJob,
    JENKINS_BACKUP_JOB: JenkinsBackupJob,
    EVENT_EXPORT_INCREMENTAL_LARGE_JOB: EventExportIncrementalLargeJob,
    LOAD_COURSE_STRUCTURE_JOB: LoadCourseStructureJob,
    ENTERPRISE_JOB: EnterpriseJob,
    TERMINATE_CLUSTER_JOB: TerminateClusterJob,
    EMR_COST_REPORTER_JOB: EmrCostReporterJob,
    VERTICA_DISK_USAGE_MONITOR_JOB: VerticaDiskUsageMonitorJob,
    UPDATE_USERS_JOB: UpdateUsersJob,
    ENROLLMENT_VALIDATION_EVENTS_JOB: EnrollmentValidationEventsJob,
    DEPLOY_CLUSTER_JOB: DeployClusterJob,
    LOAD_INSIGHTS_TO_VERTICA_JOB: LoadInsightsToVerticaJob,
    LOAD_GOOGLE_ANALYTICS_PERMISSIONS_JOB: LoadGoogleAnalyticsPermissionsJob,
    AGGREGATE_DAILY_TRACKING_LOGS_JOB: AggregateDailyTrackingLogsJob,
    MONITOR_BIGQUERY_EVENT_LOADING_JOB: MonitorBigqueryEventLoadingJob,
    VERTICA_SCHEMA_TO_S3_JOB: VerticaSchemaToS3Job,
    LOAD_VERTICA_SCHEMA_TO_SNOWFLAKE_JOB: LoadVerticaSchemaToSnowflakeJob,
    LOAD_VERTICA_SCHEMA_TO_BIGQUERY_JOB: LoadVerticaSchemaToBigqueryJob,
    LOAD_GOOGLE_SPREADSHEET_TO_SNOWFLAKE_JOB: LoadGoogleSpreadsheetToSnowflakeJob,
    LOAD_GOOGLE_SPREADSHEET_TO_VERTICA_JOB: LoadGoogleSpreadsheetToVerticaJob,
    SNOWFLAKE_REPLICA_IMPORT_JOB: SnowflakeReplicaImportJob,
    LOAD_PAYPAL_CASEREPORT_TO_VERTICA_JOB: PayPalCaseReportLoadJob,
    WAREHOUSE_TRANSFORMS_JOB: WarehouseTransformsJob,
    DBT_SOURCE_FRESHNESS_JOB: DBTSourceFreshnessJob,
    DBT_MANUAL_JOB: DBTManualJob,
    SNOWFLAKE_REFRESH_SNOWPIPE_JOB: SnowflakeRefreshSnowpipeJob,
    EXPIRE_VERTICA_PASSWORD_JOB: ExpireVerticaPasswordJob,
    SNOWFLAKE_EXPIRE_PASSWORDS_JOB: SnowflakeExpirePasswordsJob,
    PROGRAM_ENROLLMENT_REPORTS_JOB: ProgramEnrollmentReportsJob,
    SNOWFLAKE_MICROBACHELORS_ITK_JOB: SnowflakeMicrobachelorsITKJob,
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
