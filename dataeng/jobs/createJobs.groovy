import static analytics.AggregateDailyTrackingLogs.job as AggregateDailyTrackingLogsJob
import static analytics.AnalyticsEmailOptin.job as AnalyticsEmailOptinJob
import static analytics.AnalyticsExporter.job as AnalyticsExporterJob
import static analytics.AnswerDistribution.job as AnswerDistributionJob
import static analytics.BackupVertica.job as BackupVerticaJob
import static analytics.BigqueryReplicaImport.job as BigqueryReplicaImportJob
import static analytics.CoursewareLinksClicked.job as CoursewareLinksClickedJob
import static analytics.DBTDocs.job as DBTDocsJob
import static analytics.DBTManual.job as DBTManualJob
import static analytics.DBTSourceFreshness.job as DBTSourceFreshnessJob
import static analytics.DatabaseExportCoursewareStudentmodule.job as DatabaseExportCoursewareStudentmoduleJob
import static analytics.Enrollment.job as EnrollmentJob
import static analytics.EnrollmentValidationEvents.job as EnrollmentValidationEventsJob
import static analytics.Enterprise.job as EnterpriseJob
import static analytics.EventExportIncremental.job as EventExportIncrementalJob
import static analytics.EventExportIncrementalLarge.job as EventExportIncrementalLargeJob
import static analytics.EventTypeDistribution.job as EventTypeDistributionJob
import static analytics.FinanceReport.cybersource_pull_job as CybersourcePullJob
import static analytics.FinanceReport.finance_report_job as FinanceReportJob
import static analytics.FinanceReport.payments_validation_job as PaymentsValidationJob
import static analytics.GenerateWarehouseDocs.job as GenerateWarehouseDocsJob
import static analytics.JenkinsBackup.job as JenkinsBackupJob
import static analytics.LoadAffiliateWindowToWarehouse.job as LoadAffiliateWindowWarehouseJob
import static analytics.LoadCourseStructure.job as LoadCourseStructureJob
import static analytics.LoadEvents.load_events_to_s3_job as LoadEventsToS3Job
import static analytics.LoadEvents.load_events_to_vertica_job as LoadEventsToVerticaJob
import static analytics.LoadEvents.load_json_events_to_bigquery_job as LoadJsonEventsToBigqueryJob
import static analytics.LoadEvents.load_json_events_to_s3_job as LoadJsonEventsToS3Job
import static analytics.LoadGoogleAnalyticsPermissions.job as LoadGoogleAnalyticsPermissionsJob
import static analytics.LoadGoogleSpreadsheetToSnowflake.job as LoadGoogleSpreadsheetToSnowflakeJob
import static analytics.LoadInsightsToVertica.job as LoadInsightsToVerticaJob
import static analytics.LoadPaypalCaseReportToVertica.job as PayPalCaseReportLoadJob
import static analytics.LoadVerticaSchemaToBigquery.job as LoadVerticaSchemaToBigqueryJob
import static analytics.LoadVerticaSchemaToSnowflake.job as LoadVerticaSchemaToSnowflakeJob
import static analytics.LoadWarehouse.bigquery_job as LoadWarehouseBigQueryJob
import static analytics.LoadWarehouse.snowflake_job as LoadWarehouseSnowflakeJob
import static analytics.LoadWarehouse.vertica_job as LoadWarehouseVerticaJob
import static analytics.ModuleEngagement.job as ModuleEngagementJob
import static analytics.MonitorBigqueryEventLoading.job as MonitorBigqueryEventLoadingJob
import static analytics.ProgramEnrollmentReports.job as ProgramEnrollmentReportsJob
import static analytics.ReadReplicaExportToS3.job as ReadReplicaExportToS3Job
import static analytics.SnowflakeMicrobachelorsITK.job as SnowflakeMicrobachelorsITKJob
import static analytics.SnowflakePublicGrantsCleaner.job as SnowflakePublicGrantsCleanerJob
import static analytics.SnowflakeRefreshSnowpipe.job as SnowflakeRefreshSnowpipeJob
import static analytics.SnowflakeReplicaImportFromS3.job as SnowflakeReplicaImportFromS3Job
import static analytics.SqlScripts.multiple_scripts_job as SqlScriptsJob
import static analytics.SqlScripts.single_script_job as SingleSqlScriptJob
import static analytics.StitchSnowflakeLagMonitor.job as StitchSnowflakeLagMonitorJob
import static analytics.TotalEventsDailyReport.job as TotalEventsDailyReportJob
import static analytics.UserActivity.job as UserActivityJob
import static analytics.UserLocationByCourse.job as UserLocationByCourseJob
import static analytics.VerticaReplicaImportFromS3.job as VerticaReplicaImportFromS3Job
import static analytics.VerticaSchemaToS3.job as VerticaSchemaToS3Job
import static analytics.VideoTimeline.job as VideoTimelineJob
import static analytics.WarehouseTransforms.job as WarehouseTransformsJob
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
    ANALYTICS_EMAIL_OPTIN_JOB: AnalyticsEmailOptinJob,
    ANALYTICS_EXPORTER_JOB: AnalyticsExporterJob,
    ANSWER_DISTRIBUTION_JOB: AnswerDistributionJob,
    BACKUP_VERTICA_JOB: BackupVerticaJob,
    BIGQUERY_REPLICA_IMPORT_JOB: BigqueryReplicaImportJob,
    COURSEWARE_LINKS_CLICKED_JOB: CoursewareLinksClickedJob,
    CYBERSOURCE_PULL_JOB: CybersourcePullJob,
    DATABASE_EXPORT_COURSEWARE_STUDENTMODULE_JOB: DatabaseExportCoursewareStudentmoduleJob,
    DBT_DOCS_JOB: DBTDocsJob,
    DBT_MANUAL_JOB: DBTManualJob,
    DBT_SOURCE_FRESHNESS_JOB: DBTSourceFreshnessJob,
    ENROLLMENT_JOB: EnrollmentJob,
    ENROLLMENT_VALIDATION_EVENTS_JOB: EnrollmentValidationEventsJob,
    ENTERPRISE_JOB: EnterpriseJob,
    EVENT_EXPORT_INCREMENTAL_JOB: EventExportIncrementalJob,
    EVENT_EXPORT_INCREMENTAL_LARGE_JOB: EventExportIncrementalLargeJob,
    EVENT_TYPE_DISTRIBUTION_JOB: EventTypeDistributionJob,
    FINANCE_REPORT_JOB: FinanceReportJob,
    GENERATE_WAREHOUSE_DOCS_JOB: GenerateWarehouseDocsJob,
    JENKINS_BACKUP_JOB: JenkinsBackupJob,
    LOAD_AFFILIATE_WINDOW_JOB: LoadAffiliateWindowWarehouseJob,
    LOAD_COURSE_STRUCTURE_JOB: LoadCourseStructureJob,
    LOAD_EVENTS_TO_S3_JOB: LoadEventsToS3Job,
    LOAD_EVENTS_TO_VERTICA_JOB: LoadEventsToVerticaJob,
    LOAD_GOOGLE_ANALYTICS_PERMISSIONS_JOB: LoadGoogleAnalyticsPermissionsJob,
    LOAD_GOOGLE_SPREADSHEET_TO_SNOWFLAKE_JOB: LoadGoogleSpreadsheetToSnowflakeJob,
    LOAD_INSIGHTS_TO_VERTICA_JOB: LoadInsightsToVerticaJob,
    LOAD_JSON_EVENTS_TO_BIGQUERY_JOB: LoadJsonEventsToBigqueryJob,
    LOAD_JSON_EVENTS_TO_S3_JOB: LoadJsonEventsToS3Job,
    LOAD_PAYPAL_CASEREPORT_TO_VERTICA_JOB: PayPalCaseReportLoadJob,
    LOAD_VERTICA_SCHEMA_TO_BIGQUERY_JOB: LoadVerticaSchemaToBigqueryJob,
    LOAD_VERTICA_SCHEMA_TO_SNOWFLAKE_JOB: LoadVerticaSchemaToSnowflakeJob,
    LOAD_WAREHOUSE_BIGQUERY_JOB: LoadWarehouseBigQueryJob,
    LOAD_WAREHOUSE_SNOWFLAKE_JOB: LoadWarehouseSnowflakeJob,
    LOAD_WAREHOUSE_VERTICA_JOB: LoadWarehouseVerticaJob,
    MODULE_ENGAGEMENT_JOB: ModuleEngagementJob,
    MONITOR_BIGQUERY_EVENT_LOADING_JOB: MonitorBigqueryEventLoadingJob,
    PAYMENTS_VALIDATION_JOB: PaymentsValidationJob,
    PROGRAM_ENROLLMENT_REPORTS_JOB: ProgramEnrollmentReportsJob,
    READ_REPLICA_EXPORT_TO_S3_JOB: ReadReplicaExportToS3Job,
    SINGLE_SQL_SCRIPT_JOB: SingleSqlScriptJob,
    SNOWFLAKE_MICROBACHELORS_ITK_JOB: SnowflakeMicrobachelorsITKJob,
    SNOWFLAKE_PUBLIC_GRANTS_CLEANER_JOB: SnowflakePublicGrantsCleanerJob,
    SNOWFLAKE_REFRESH_SNOWPIPE_JOB: SnowflakeRefreshSnowpipeJob,
    SNOWFLAKE_REPLICA_IMPORT_FROM_S3_JOB: SnowflakeReplicaImportFromS3Job,
    SQL_SCRIPTS_JOB: SqlScriptsJob,
    STITCH_SNOWFLAKE_LAG_MONITOR_JOB: StitchSnowflakeLagMonitorJob,
    TOTAL_EVENTS_DAILY_REPORT_JOB: TotalEventsDailyReportJob,
    USER_ACTIVITY_JOB: UserActivityJob,
    USER_LOCATION_BY_COURSE_JOB: UserLocationByCourseJob,
    VERTICA_REPLICA_IMPORT_FROM_S3_JOB: VerticaReplicaImportFromS3Job,
    VERTICA_SCHEMA_TO_S3_JOB: VerticaSchemaToS3Job,
    VIDEO_TIMELINE_JOB: VideoTimelineJob,
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
        regex('.+read-replica-(import|export)-(to|from)-s3|load-.+|vertica-schema-to.+|.*sql-script.*')
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

listView('Deprecated') {
    jobs {
        regex('DEPRECATED-.*')
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
