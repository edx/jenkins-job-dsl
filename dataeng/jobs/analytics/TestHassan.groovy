package analytics

import static org.edx.jenkins.dsl.AnalyticsConstants.common_publishers


class TestHassan {
    public static def job = { dslFactory, allVars ->
        dslFactory.job("test-hassan-param") {
          parameters {
            stringParam('NOTIFY', '$PAGER_NOTIFY', 'Space separated list of emails to send notifications to.')
          }
          publishers {
            mailer(system.getenv("NOTIFY") ?: allVars.get('PAGER_NOTIFY'))
          }
        }
    }
}