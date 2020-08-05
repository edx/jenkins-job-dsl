package analytics

import static org.edx.jenkins.dsl.AnalyticsConstants.common_publishers


class TestHassan {
    public static def job = { dslFactory, allVars ->
        dslFactory.job("test-hassan-param") {
            parameters {
                stringParam('NOTIFY', 'hassan@edx.org', '')
            }
            publishers common_publishers(allVars)
        }
    }