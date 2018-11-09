import static analytics.AnalyticsEmailOptin.job as AnalyticsEmailOptinJob
import static analytics.AnalyticsExporter.job as AnalyticsExporterJob
import static analytics.UserActivity.job as UserActivityJob
import static analytics.VideoTimeline.job as VideoTimelineJob
import static analytics.UserLocationByCourse.job as UserLocationByCourseJob
import static analytics.Enrollment.job as EnrollmentJob
import static analytics.ModuleEngagement.job as ModuleEngagementJob
import static analytics.DatabaseExportCoursewareStudentmodule.job as DatabaseExportCoursewareStudentmoduleJob
import static analytics.AnswerDistribution.job as AnswerDistributionJob
import static analytics.FinanceReport.payments_validation_job as PaymentsValidationJob
import static analytics.FinanceReport.finance_report_job as FinanceReportJob
import static analytics.BigqueryReplicaImport.job as BigqueryReplicaImportJob
import static analytics.EventExportIncremental.job as EventExportIncrementalJob
import static analytics.CoursewareLinksClicked.job as CoursewareLinksClickedJob
import static analytics.EventTypeDistribution.job as EventTypeDistributionJob
import static analytics.GenerateWarehouseDocs.job as GenerateWarehouseDocsJob
import static analytics.SqlScripts.job as SqlScriptsJob
import static analytics.LoadWarehouse.vertica_job as LoadWarehouseVerticaJob
import static analytics.LoadWarehouse.bigquery_job as LoadWarehouseBigQueryJob
import static analytics.LoadEvents.load_events_to_s3_job as LoadEventsToS3Job
import static analytics.LoadEvents.load_events_to_vertica_job as LoadEventsToVerticaJob
import static analytics.LoadEvents.load_json_events_to_s3_job as LoadJsonEventsToS3Job
import static analytics.LoadEvents.load_json_events_to_bigquery_job as LoadJsonEventsToBigqueryJob
import static analytics.VerticaReplicaImport.job as VerticaReplicaImportJob
import static analytics.VerticaToBigquerySchemaCopy.job as VerticaToBigquerySchemaCopyJob
import static analytics.BackupVertica.job as BackupVerticaJob
import static analytics.TotalEventsDailyReport.job as TotalEventsDailyReportJob
import static analytics.EnterpriseUser.job as EnterpriseUserJob
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
    PAYMENTS_VALIDATION_JOB: PaymentsValidationJob,
    FINANCE_REPORT_JOB: FinanceReportJob,
    BIGQUERY_REPLICA_IMPORT_JOB: BigqueryReplicaImportJob,
    EVENT_EXPORT_INCREMENTAL_JOB: EventExportIncrementalJob,
    COURSEWARE_LINKS_CLICKED_JOB: CoursewareLinksClickedJob,
    EVENT_TYPE_DISTRIBUTION_JOB: EventTypeDistributionJob,
    GENERATE_WAREHOUSE_DOCS_JOB: GenerateWarehouseDocsJob,
    SQL_SCRIPTS_JOB: SqlScriptsJob,
    LOAD_WAREHOUSE_VERTICA_JOB: LoadWarehouseVerticaJob,
    LOAD_WAREHOUSE_BIGQUERY_JOB: LoadWarehouseBigQueryJob,
    LOAD_EVENTS_TO_S3_JOB: LoadEventsToS3Job,
    LOAD_EVENTS_TO_VERTICA_JOB: LoadEventsToVerticaJob,
    LOAD_JSON_EVENTS_TO_S3_JOB: LoadJsonEventsToS3Job,
    LOAD_JSON_EVENTS_TO_BIGQUERY_JOB: LoadJsonEventsToBigqueryJob,
    VERTICA_REPLICA_IMPORT_JOB: VerticaReplicaImportJob,
    VERTICA_TO_BIGQUERY_SCHEMA_COPY_JOB: VerticaToBigquerySchemaCopyJob,
    BACKUP_VERTICA_JOB: BackupVerticaJob,
    TOTAL_EVENTS_DAILY_REPORT_JOB: TotalEventsDailyReportJob,
    ENTERPRISE_USER_JOB: EnterpriseUserJob,
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
    columns {
        status()
        weather()
        name()
        lastSuccess()
        lastFailure()
        lastDuration()
        buildButton()
    }
}

listView('Edge') {
    description('Only for Edge jobs.')
    jobs {
        regex('.+edge')
    }
    columns {
        status()
        weather()
        name()
        lastSuccess()
        lastFailure()
        lastDuration()
        buildButton()
    }
}

listView('Release') {
    description('Jobs that are used for testing release candidates.')
    jobs {
        regex('.+release')
    }
    columns {
        status()
        weather()
        name()
        lastSuccess()
        lastFailure()
        lastDuration()
        buildButton()
    }
}

listView('Exporter') {
    description('Jobs that are used for exporting course data.')
    jobs {
        regex('analytics-.+')
    }
    columns {
        status()
        weather()
        name()
        lastSuccess()
        lastFailure()
        lastDuration()
        buildButton()
    }
}

listView('Warehouse') {
    jobs {
        name('event-type-distribution')
        name('courseware-links-clicked')
        name('finance-report')
        name('payments-validation')
        name('generate-warehouse-docs')
        regex('.+read-replica-import')
        regex('load-.+')
    }
    columns {
        status()
        weather()
        name()
        lastSuccess()
        lastFailure()
        lastDuration()
        buildButton()
    }
}

listView('Tools') {
    jobs {
        name('data_engineering_seed_job')
    }
    columns {
        status()
        weather()
        name()
        lastSuccess()
        lastFailure()
        lastDuration()
        buildButton()
    }
}
